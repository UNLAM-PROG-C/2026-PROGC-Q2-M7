package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.WeakHashMap;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import utils.Constantes;

public class GestorRed {
	private static final int TIMEOUT_CONEXION_MS = 5000;
	private static final Object LOCK_ESPERA = new Object();
	private static final Map<Thread, Boolean> ESPERAS_CANCELADAS = new WeakHashMap<>();
	private static ServerSocket serverSocketPendiente;

	private final Socket socket;
	private final EventBus bus;
	private final PrintWriter salida;
	private final BufferedReader entrada;

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
		cerrarSilencioso(pendiente);
	}

	public GestorRed(int rol, EventBus bus) throws IOException {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, bus);
	}

	public GestorRed(int rol, String host, int puerto, EventBus bus) throws IOException {
		this.socket = obtenerSocket(rol, host, puerto);
		this.bus = bus;
		this.salida = new PrintWriter(socket.getOutputStream(), true);
		this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private Socket obtenerSocket(int rol, String host, int puerto) throws IOException {
		if (rol == Constantes.ROL_HOST) {
			return esperarCliente(puerto);
		}
		return conectarCliente(host, puerto);
	}

	private Socket esperarCliente(int puerto) throws IOException {
		System.out.println("Esperando rival en puerto " + puerto + "...");
		ServerSocket server = abrirServidorPendiente(puerto);
		try {
			return server.accept();
		} catch (SocketException e) {
			if (server.isClosed()) {
				throw new IOException("Espera de host cancelada.", e);
			}
			throw e;
		} finally {
			limpiarServidorPendiente(server);
		}
	}

	private static ServerSocket abrirServidorPendiente(int puerto) throws IOException {
		ServerSocket server = new ServerSocket();
		try {
			vincularServidor(server, puerto);
			return server;
		} catch (IOException e) {
			cerrarSilencioso(server);
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
		cerrarSilencioso(server);
	}

	private static void cerrarSilencioso(ServerSocket server) {
		if (server != null && !server.isClosed()) {
			try {
				server.close();
			} catch (IOException ignored) {}
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
		Thread hiloDeRed = new Thread(this::leerMensajes, "Thread-Red");
		hiloDeRed.setDaemon(true);
		hiloDeRed.start();
	}

	private void leerMensajes() {
		try {
			String linea;
			while ((linea = entrada.readLine()) != null) {
				TipoEvento tipo = TipoEvento.desdeTexto(linea);
				if (tipo != null) {
					bus.publicar(new Evento(tipo));
				}
			}
		} catch (IOException e) {
			// Red cerrada
		} finally {
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		}
	}

	public void enviarEvento(TipoEvento tipo) {
		if (salida != null && tipo != null) {
			salida.println(tipo.name());
		}
	}

	public void cerrar() throws IOException {
		cancelarEsperaServidor();
		if (salida != null) {
			salida.close();
		}
		if (entrada != null) {
			try {
				entrada.close();
			} catch (IOException ignored) {}
		}
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}
}
