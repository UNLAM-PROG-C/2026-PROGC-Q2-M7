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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import utils.Constantes;

public class GestorRed {
	private static final int TIMEOUT_CONEXION_MS = 5000;

	private record Conexion(Socket socket, PrintWriter salida) {}

	private final int rol, puerto, clientesEsperados;
	private final String host;
	private final EventBus bus;
	private final Object lockEstado = new Object();
	private final Map<Integer, Conexion> conexiones = new HashMap<>();
	private int siguienteId = 1;
	private boolean cerrado, listo;
	private Closeable pendiente;

	public GestorRed(int rol, String host, int puerto, int clientesEsperados, EventBus bus) {
		this.rol = rol;
		this.host = host;
		this.puerto = puerto;
		this.clientesEsperados = Math.max(1, clientesEsperados);
		this.bus = bus;
	}

	public void conectar() throws IOException {
		if (rol == Constantes.ROL_HOST) {
			esperarClientes();
		} else {
			conectarComoCliente();
		}
	}

	public int clientesConectados() {
		synchronized (lockEstado) {
			return conexiones.size();
		}
	}

	private boolean estaCerrado() {
		synchronized (lockEstado) {
			return cerrado;
		}
	}

	private void esperarClientes() throws IOException {
		ServerSocket server = new ServerSocket();
		registrarPendiente(server);
		try {
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(puerto));
			while (!cupoCompleto()) {
				registrarConexion(server.accept(), false);
			}
		} catch (IOException e) {
			cerrarConexionesActivas();
			throw estaCerrado() ? new IOException("Espera de host cancelada.", e) : e;
		} finally {
			limpiarPendiente(server);
			cerrarRecurso(server);
		}
	}

	private boolean cupoCompleto() throws IOException {
		synchronized (lockEstado) {
			if (cerrado) {
				throw new IOException("Espera de host cancelada.");
			}
			listo = conexiones.size() >= clientesEsperados;
			return listo;
		}
	}

	private void conectarComoCliente() throws IOException {
		Socket cliente = new Socket();
		registrarPendiente(cliente);
		try {
			cliente.setReuseAddress(true);
			cliente.connect(new InetSocketAddress(host, puerto), TIMEOUT_CONEXION_MS);
			limpiarPendiente(cliente);
			registrarConexion(cliente, true);
		} catch (IOException e) {
			limpiarPendiente(cliente);
			cerrarRecurso(cliente);
			throw estaCerrado() ? new IOException("Conexión cancelada.", e) : e;
		}
	}

	private void registrarPendiente(Closeable recurso) throws IOException {
		synchronized (lockEstado) {
			if (!cerrado) {
				pendiente = recurso;
				return;
			}
		}
		cerrarRecurso(recurso);
		throw new IOException("Conexión cancelada.");
	}

	private void limpiarPendiente(Closeable recurso) {
		synchronized (lockEstado) {
			if (pendiente == recurso) {
				pendiente = null;
			}
		}
	}

	private void registrarConexion(Socket socket, boolean marcarListo) throws IOException {
		PrintWriter salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
		int id;
		synchronized (lockEstado) {
			if (cerrado) {
				id = -1;
			} else {
				id = siguienteId++;
				conexiones.put(id, new Conexion(socket, salida));
				if (marcarListo) {
					listo = true;
				}
			}
		}
		if (id < 0) {
			salida.close();
			cerrarRecurso(socket);
			throw new IOException("Conexión cancelada.");
		}
		Thread hilo = new Thread(() -> leerMensajes(id, socket), "Thread-Red-" + id);
		hilo.setDaemon(true);
		hilo.start();
	}

	private void leerMensajes(int id, Socket socket) {
		try (BufferedReader entrada = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
			String linea;
			while (!estaCerrado() && (linea = entrada.readLine()) != null) {
				procesarLinea(id, linea);
			}
		} catch (IOException e) {
			if (!estaCerrado()) {
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
		TipoEvento tipo = evento.tipo();
		if (rol == Constantes.ROL_HOST
				&& (tipo == TipoEvento.RIVAL_COMPLETO_GRILLA || tipo == TipoEvento.PARTIDA_FINALIZADA)) {
			salidasDestino(idOrigen).forEach(salida -> salida.println(linea));
		}
		bus.publicar(tipo == TipoEvento.SOLICITAR_REVANCHA
				? new Evento(TipoEvento.SOLICITAR_REVANCHA, idOrigen) : evento);
	}

	public void enviarEvento(TipoEvento tipo) {
		enviarEvento(tipo, null);
	}

	public void enviarEvento(TipoEvento tipo, Object datos) {
		if (tipo == null || estaCerrado()) {
			return;
		}
		String linea = new Evento(tipo, datos).aLineaRed();
		salidasDestino(-1).forEach(salida -> salida.println(linea));
	}

	private List<PrintWriter> salidasDestino(int excluirId) {
		synchronized (lockEstado) {
			if (cerrado) {
				return List.of();
			}
			List<PrintWriter> salidas = new ArrayList<>(conexiones.size());
			conexiones.forEach((id, con) -> {
				if (id != excluirId) {
					salidas.add(con.salida());
				}
			});
			return salidas;
		}
	}

	private void desconectar(int id, Socket socket) {
		boolean notificar;
		boolean sinClientes;
		synchronized (lockEstado) {
			conexiones.remove(id);
			notificar = !cerrado && listo;
			sinClientes = conexiones.isEmpty();
		}
		cerrarRecurso(socket);
		if (notificar) {
			bus.publicar(sinClientes
					? new Evento(TipoEvento.CONEXION_PERDIDA)
					: new Evento(TipoEvento.JUGADOR_DESCONECTADO, id));
		}
	}

	public void cerrar() {
		Closeable porCerrar;
		synchronized (lockEstado) {
			if (cerrado) {
				return;
			}
			cerrado = true;
			porCerrar = pendiente;
			pendiente = null;
		}
		cerrarRecurso(porCerrar);
		cerrarConexionesActivas();
	}

	private void cerrarConexionesActivas() {
		List<Conexion> activas;
		synchronized (lockEstado) {
			activas = new ArrayList<>(conexiones.values());
			conexiones.clear();
		}
		activas.forEach(con -> cerrarRecurso(con.socket()));
	}

	private static void cerrarRecurso(Closeable recurso) {
		if (recurso != null) {
			try {
				recurso.close();
			} catch (IOException e) {
				System.err.println("[RED] Error al cerrar recurso: " + e.getMessage());
			}
		}
	}
}
