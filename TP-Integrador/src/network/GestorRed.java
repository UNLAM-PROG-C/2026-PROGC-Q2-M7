package network;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import utils.Constantes;

public class GestorRed {
	private static final int TIMEOUT_CONEXION_MS = 5000;

	private final int rol;
	private final String host;
	private final int puerto;
	private final int clientesEsperados;
	private final EventBus bus;
	private final Map<Integer, Socket> sockets = new ConcurrentHashMap<>();
	private final Map<Integer, PrintWriter> salidas = new ConcurrentHashMap<>();
	private final AtomicInteger generadorId = new AtomicInteger(1);
	private final AtomicBoolean conectado = new AtomicBoolean();
	private final AtomicBoolean listo = new AtomicBoolean();
	private final AtomicBoolean cerrado = new AtomicBoolean();
	private volatile ServerSocket servidor;

	public GestorRed(int rol, EventBus bus) {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, 1, bus);
	}

	public GestorRed(int rol, String host, int puerto, EventBus bus) {
		this(rol, host, puerto, 1, bus);
	}

	public GestorRed(int rol, String host, int puerto, int clientesEsperados, EventBus bus) {
		this.rol = rol;
		this.host = host;
		this.puerto = puerto;
		this.clientesEsperados = Math.max(1, clientesEsperados);
		this.bus = bus;
	}

	public void conectar() throws IOException {
		if (!conectado.compareAndSet(false, true)) {
			return;
		}
		if (cerrado.get()) {
			throw new IOException("Conexión cancelada.");
		}
		if (rol == Constantes.ROL_HOST) {
			esperarClientes();
		} else {
			conectarComoCliente();
		}
	}

	public int clientesConectados() {
		return salidas.size();
	}

	private void esperarClientes() throws IOException {
		this.servidor = abrirServidor(puerto);
		try {
			aceptarHastaCompletarCupo();
			listo.set(true);
		} catch (IOException e) {
			cerrarConexionesActivas();
			throw cerrado.get() ? new IOException("Espera de host cancelada.", e) : e;
		} finally {
			cerrarRecurso(servidor);
		}
	}

	private void aceptarHastaCompletarCupo() throws IOException {
		while (!cerrado.get() && salidas.size() < clientesEsperados) {
			registrarConexion(servidor.accept());
		}
		if (cerrado.get()) {
			throw new IOException("Espera de host cancelada.");
		}
	}

	private void conectarComoCliente() throws IOException {
		Socket cliente = new Socket();
		try {
			cliente.setReuseAddress(true);
			cliente.connect(new InetSocketAddress(host, puerto), TIMEOUT_CONEXION_MS);
			listo.set(true);
			registrarConexion(cliente);
		} catch (IOException e) {
			cerrarRecurso(cliente);
			throw e;
		}
	}

	private static ServerSocket abrirServidor(int puerto) throws IOException {
		ServerSocket server = new ServerSocket();
		try {
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(puerto));
			return server;
		} catch (IOException e) {
			cerrarRecurso(server);
			throw e;
		}
	}

	private void registrarConexion(Socket socket) throws IOException {
		if (cerrado.get()) {
			cerrarRecurso(socket);
			throw new IOException("Conexión cancelada.");
		}
		int id = generadorId.getAndIncrement();
		PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
		sockets.put(id, socket);
		salidas.put(id, salida);
		iniciarLector(id, socket);
	}

	private void iniciarLector(int id, Socket socket) {
		Thread hilo = new Thread(() -> leerMensajes(id, socket), "Thread-Red-" + id);
		hilo.setDaemon(true);
		hilo.start();
	}

	private void leerMensajes(int id, Socket socket) {
		try (BufferedReader entrada = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
			String linea;
			while (!cerrado.get() && (linea = entrada.readLine()) != null) {
				procesarLinea(id, linea);
			}
		} catch (IOException e) {
			if (!cerrado.get()) {
				System.err.println("[RED] Conexión " + id + " finalizada: " + e.getMessage());
			}
		} finally {
			desconectar(id, socket);
		}
	}

	private void procesarLinea(int idOrigen, String linea) {
		Evento evento = Evento.desdeLineaRed(linea);
		if (evento == null) {
			return;
		}
		if (rol == Constantes.ROL_HOST && esReenviable(evento.tipo())) {
			reenviarExcepto(idOrigen, linea);
		}
		if (evento.tipo() == TipoEvento.SOLICITAR_REVANCHA) {
			bus.publicar(new Evento(TipoEvento.SOLICITAR_REVANCHA, idOrigen));
		} else {
			bus.publicar(evento);
		}
	}

	private static boolean esReenviable(TipoEvento tipo) {
		return tipo == TipoEvento.RIVAL_COMPLETO_GRILLA || tipo == TipoEvento.PARTIDA_FINALIZADA;
	}

	private void reenviarExcepto(int idOrigen, String linea) {
		salidas.forEach((id, salida) -> {
			if (id != idOrigen) {
				salida.println(linea);
			}
		});
	}

	public void enviarEvento(TipoEvento tipo) {
		enviarEvento(tipo, null);
	}

	public void enviarEvento(TipoEvento tipo, Object datos) {
		if (tipo == null || cerrado.get()) {
			return;
		}
		String linea = new Evento(tipo, datos).aLineaRed();
		salidas.values().forEach(salida -> salida.println(linea));
	}

	private void desconectar(int id, Socket socket) {
		salidas.remove(id);
		sockets.remove(id);
		cerrarRecurso(socket);
		if (cerrado.get() || !listo.get()) {
			return;
		}
		if (salidas.isEmpty()) {
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		} else {
			bus.publicar(new Evento(TipoEvento.JUGADOR_DESCONECTADO, id));
		}
	}

	public void cerrar() {
		if (!cerrado.compareAndSet(false, true)) {
			return;
		}
		cerrarRecurso(servidor);
		cerrarConexionesActivas();
	}

	private void cerrarConexionesActivas() {
		salidas.values().forEach(PrintWriter::close);
		salidas.clear();
		sockets.values().forEach(GestorRed::cerrarRecurso);
		sockets.clear();
	}

	private static void cerrarRecurso(Closeable recurso) {
		if (recurso == null) {
			return;
		}
		try {
			recurso.close();
		} catch (IOException e) {
			System.err.println("[RED] Error al cerrar recurso: " + e.getMessage());
		}
	}
}
