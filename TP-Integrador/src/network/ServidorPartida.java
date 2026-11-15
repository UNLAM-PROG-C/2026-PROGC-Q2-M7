package network;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import utils.Constantes;

/**
 * Servidor TCP del host: acepta hasta {@code maximoClientes} jugadores.
 *
 * <p>Modelo de hilos: un hilo aceptador daemon (único que agrega conexiones) y un hilo lector
 * daemon por cliente (único que quita su propia conexión y publica su desconexión). El motor
 * (u otro hilo) llama {@code enviar*} y {@code desconectar} concurrentemente: el mapa de
 * conexiones es un {@link ConcurrentHashMap} y cada {@link Enlace} sincroniza su escritura, de
 * modo que un socket muerto solo pierde sus propios mensajes sin afectar a los demás.
 * Una desconexión se publica exactamente una vez: la publica el lector de ese cliente cuando
 * logra quitarlo del mapa (también tras {@link #desconectar}); tras {@link #close()} no se
 * publica nada.
 */
public final class ServidorPartida implements CanalServidor {
	/** Motivo del RECHAZADO cuando la sala ya tiene el máximo de jugadores. */
	public static final String MOTIVO_SALA_LLENA = "SALA_LLENA";
	private static final int PRIMER_ID_CLIENTE = Constantes.ID_HOST + 1;
	private static final int ESPERA_RECHAZO_MS = 1000;
	private static final String HILO_ACEPTADOR = "Servidor-Aceptador";
	private static final String HILO_LECTOR = "Servidor-Lector-";
	private static final String HILO_RECHAZO = "Servidor-Rechazo";

	private final ServerSocket servidor;
	private final int maximoClientes;
	private final EventBus bus;
	private final Map<Integer, Enlace> conexiones = new ConcurrentHashMap<>();
	private final AtomicInteger siguienteId = new AtomicInteger(PRIMER_ID_CLIENTE);
	private final AtomicBoolean cerrado = new AtomicBoolean();
	private final AtomicBoolean iniciado = new AtomicBoolean();

	private ServidorPartida(ServerSocket servidor, int maximoClientes, EventBus bus) {
		this.servidor = servidor;
		this.maximoClientes = maximoClientes;
		this.bus = bus;
	}

	/** Abre el puerto (sin bloquear). Los clientes se aceptan en un hilo propio tras iniciar. */
	public static ServidorPartida abrir(int puerto, int maximoClientes, EventBus bus)
			throws IOException {
		ServerSocket socket = new ServerSocket();
		try {
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(puerto));
			return new ServidorPartida(socket, maximoClientes, bus);
		} catch (IOException | RuntimeException e) {
			socket.close();
			throw e;
		}
	}

	@Override
	public void iniciar() {
		if (cerrado.get() || !iniciado.compareAndSet(false, true)) {
			return;
		}
		lanzar(this::aceptar, HILO_ACEPTADOR);
	}

	private static void lanzar(Runnable tarea, String nombre) {
		Thread hilo = new Thread(tarea, nombre);
		hilo.setDaemon(true);
		hilo.start();
	}

	private void aceptar() {
		while (!cerrado.get()) {
			try {
				atender(servidor.accept());
			} catch (SocketException e) {
				return;
			} catch (IOException e) {
				continue;
			}
		}
	}

	private void atender(Socket socket) {
		try {
			Enlace enlace = new Enlace(socket);
			if (conexiones.size() >= maximoClientes) {
				rechazar(enlace);
			} else {
				registrar(enlace);
			}
		} catch (IOException e) {
			cerrarSocket(socket);
		}
	}

	private void registrar(Enlace enlace) {
		int id = siguienteId.getAndIncrement();
		conexiones.put(id, enlace);
		bus.publicar(new Evento(TipoEvento.JUGADOR_CONECTADO, id));
		lanzar(() -> leerCliente(id, enlace), HILO_LECTOR + id);
	}

	private void leerCliente(int id, Enlace enlace) {
		enlace.leer(m -> bus.publicar(
				new Evento(TipoEvento.MENSAJE_RED, new MensajeRecibido(id, m))));
		enlace.close();
		if (conexiones.remove(id, enlace) && !cerrado.get()) {
			bus.publicar(new Evento(TipoEvento.JUGADOR_DESCONECTADO, id));
		}
	}

	/** Avisa RECHAZADO en un hilo aparte, para no frenar al aceptador con un cliente lento. */
	private void rechazar(Enlace enlace) {
		lanzar(() -> avisarRechazo(enlace), HILO_RECHAZO);
	}

	private void avisarRechazo(Enlace enlace) {
		try {
			enlace.enviar(MensajeRed.de(TipoMensaje.RECHAZADO, MOTIVO_SALA_LLENA));
			enlace.cerrarEscritura();
			enlace.socket().setSoTimeout(ESPERA_RECHAZO_MS);
			enlace.leer(m -> { });
		} catch (IOException e) {
			// el rechazado ya se fue
		} finally {
			enlace.close();
		}
	}

	private static void cerrarSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			// nada que hacer
		}
	}

	@Override
	public void enviar(MensajeRed mensaje) {
		conexiones.values().forEach(enlace -> enlace.enviar(mensaje));
	}

	@Override
	public void enviarA(int idJugador, MensajeRed mensaje) {
		Enlace enlace = conexiones.get(idJugador);
		if (enlace != null) {
			enlace.enviar(mensaje);
		}
	}

	@Override
	public void enviarATodosMenos(int idExcluido, MensajeRed mensaje) {
		conexiones.forEach((id, enlace) -> {
			if (id != idExcluido) {
				enlace.enviar(mensaje);
			}
		});
	}

	/** Corta al jugador; su lector detecta el cierre y publica JUGADOR_DESCONECTADO una vez. */
	@Override
	public void desconectar(int idJugador) {
		Enlace enlace = conexiones.get(idJugador);
		if (enlace != null) {
			enlace.close();
		}
	}

	@Override
	public int puerto() {
		return servidor.getLocalPort();
	}

	@Override
	public void close() {
		if (!cerrado.compareAndSet(false, true)) {
			return;
		}
		try {
			servidor.close();
		} catch (IOException e) {
			// ya cerrado
		}
		conexiones.values().forEach(Enlace::close);
		conexiones.clear();
	}
}
