package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import utils.Constantes;

public class GestorRed {
	private static final int TIMEOUT_CONEXION_MS = 5000;
	private static final char SEPARADOR_PAYLOAD = ':';
	private static final Object LOCK_ESPERA = new Object();
	private static final Map<Thread, Boolean> ESPERAS_CANCELADAS = new WeakHashMap<>();
	private static ServerSocket serverSocketPendiente;

	private final int rol;
	private final EventBus bus;
	private final List<Socket> sockets = new CopyOnWriteArrayList<>();
	private final List<PrintWriter> salidas = new CopyOnWriteArrayList<>();
	private final AtomicBoolean cerrado = new AtomicBoolean();

	public static void registrarEspera(Thread hilo) {
		synchronized (LOCK_ESPERA) {
			ESPERAS_CANCELADAS.put(hilo, Boolean.FALSE);
		}
	}

	public static void limpiarEspera(Thread hilo) {
		synchronized (LOCK_ESPERA) {
			ESPERAS_CANCELADAS.remove(hilo);
		}
	}

	public static void cancelarEsperaServidor() {
		ServerSocket pendiente;
		synchronized (LOCK_ESPERA) {
			ESPERAS_CANCELADAS.replaceAll((hilo, cancelada) -> Boolean.TRUE);
			pendiente = serverSocketPendiente;
			serverSocketPendiente = null;
		}
		cerrarServidorSilencioso(pendiente);
	}

	public GestorRed(int rol, EventBus bus) throws IOException {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, 1, bus);
	}

	public GestorRed(int rol, String host, int puerto, EventBus bus) throws IOException {
		this(rol, host, puerto, 1, bus);
	}

	public GestorRed(int rol, String host, int puerto, int clientesEsperados, EventBus bus) throws IOException {
		this.rol = rol;
		this.bus = bus;
		if (rol == Constantes.ROL_HOST) {
			esperarClientes(puerto, Math.max(1, clientesEsperados));
		} else {
			registrarSocket(conectarCliente(host, puerto));
		}
	}

	private void esperarClientes(int puerto, int clientesEsperados) throws IOException {
		System.out.println("Esperando " + clientesEsperados + " rival(es) en puerto " + puerto + "...");
		ServerSocket server = abrirServidorPendiente(puerto);
		try {
			aceptarClientes(server, clientesEsperados);
		} catch (IOException e) {
			cerrarSocketsAbiertos();
			throw server.isClosed() ? new IOException("Espera de host cancelada.", e) : e;
		} finally {
			limpiarServidorPendiente(server);
		}
	}

	private void aceptarClientes(ServerSocket server, int clientesEsperados) throws IOException {
		while (sockets.size() < clientesEsperados) {
			registrarSocket(server.accept());
		}
	}

	private void registrarSocket(Socket socket) throws IOException {
		sockets.add(socket);
		salidas.add(new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8));
	}

	private static ServerSocket abrirServidorPendiente(int puerto) throws IOException {
		ServerSocket server = new ServerSocket();
		try {
			vincularServidor(server, puerto);
			return server;
		} catch (IOException e) {
			cerrarServidorSilencioso(server);
			throw e;
		}
	}

	private static void vincularServidor(ServerSocket server, int puerto) throws IOException {
		synchronized (LOCK_ESPERA) {
			if (Boolean.TRUE.equals(ESPERAS_CANCELADAS.remove(Thread.currentThread()))) {
				throw new IOException("Espera de host cancelada.");
			}
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(puerto));
			serverSocketPendiente = server;
		}
	}

	private static void limpiarServidorPendiente(ServerSocket server) {
		synchronized (LOCK_ESPERA) {
			if (serverSocketPendiente == server) {
				serverSocketPendiente = null;
			}
		}
		cerrarServidorSilencioso(server);
	}

	private static void cerrarServidorSilencioso(ServerSocket server) {
		if (server != null && !server.isClosed()) {
			try {
				server.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static void cerrarSocketSilencioso(Socket socket) {
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	private Socket conectarCliente(String host, int puerto) throws IOException {
		System.out.println("Conectando a " + host + ":" + puerto + "...");
		Socket cliente = new Socket();
		cliente.setReuseAddress(true);
		cliente.connect(new InetSocketAddress(host, puerto), TIMEOUT_CONEXION_MS);
		return cliente;
	}

	public void iniciarEscucha() {
		for (int i = 0; i < sockets.size(); i++) {
			Socket socket = sockets.get(i);
			PrintWriter origen = salidas.get(i);
			Thread hilo = new Thread(() -> leerMensajes(socket, origen), "Thread-Red-" + (i + 1));
			hilo.setDaemon(true);
			hilo.start();
		}
	}

	private void leerMensajes(Socket socket, PrintWriter origen) {
		try (BufferedReader entrada = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
			String linea;
			while ((linea = entrada.readLine()) != null) {
				procesarLinea(linea, origen);
			}
		} catch (IOException ignored) {
			// Red cerrada
		} finally {
			desconectar(socket, origen);
		}
	}

	private void procesarLinea(String linea, PrintWriter origen) {
		int sep = linea.indexOf(SEPARADOR_PAYLOAD);
		String nombre = sep < 0 ? linea : linea.substring(0, sep);
		String datos = sep < 0 ? null : linea.substring(sep + 1);
		TipoEvento tipo = TipoEvento.desdeTexto(nombre);
		if (tipo != null) {
			if (rol == Constantes.ROL_HOST && esReenviable(tipo)) {
				reenviarExcepto(origen, linea);
			}
			bus.publicar(new Evento(tipo, datos));
		}
	}

	private static boolean esReenviable(TipoEvento tipo) {
		return tipo == TipoEvento.RIVAL_COMPLETO_GRILLA || tipo == TipoEvento.PARTIDA_FINALIZADA;
	}

	private void reenviarExcepto(PrintWriter origen, String linea) {
		for (PrintWriter salida : salidas) {
			if (salida != origen) {
				salida.println(linea);
			}
		}
	}

	public void enviarEvento(TipoEvento tipo) {
		enviarEvento(tipo, null);
	}

	public void enviarEvento(TipoEvento tipo, Object datos) {
		if (tipo == null) {
			return;
		}
		String linea = datos == null ? tipo.name() : tipo.name() + SEPARADOR_PAYLOAD + datos;
		for (PrintWriter salida : salidas) {
			salida.println(linea);
		}
	}

	private void desconectar(Socket socket, PrintWriter origen) {
		salidas.remove(origen);
		sockets.remove(socket);
		cerrarSocketSilencioso(socket);
		if (!cerrado.get() && salidas.isEmpty()) {
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		}
	}

	public void cerrar() throws IOException {
		cerrado.set(true);
		cancelarEsperaServidor();
		cerrarSocketsAbiertos();
	}

	private void cerrarSocketsAbiertos() {
		for (PrintWriter salida : salidas) {
			salida.close();
		}
		salidas.clear();
		for (Socket socket : sockets) {
			cerrarSocketSilencioso(socket);
		}
		sockets.clear();
	}
}
