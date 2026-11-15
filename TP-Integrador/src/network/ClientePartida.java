package network;

import events.EventBus;
import events.Evento;
import events.TipoEvento;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import utils.Constantes;

/**
 * Conexión TCP de un jugador hacia el host.
 *
 * <p>Modelo de hilos: {@link #conectar} bloquea al llamador (máx. {@link #TIMEOUT_CONEXION_MS}).
 * {@link #iniciar} lanza un hilo lector daemon que publica en el EventBus. {@code enviar} y
 * {@code close} pueden llamarse desde cualquier hilo.
 */
public final class ClientePartida implements CanalRed {
	/** Tiempo máximo para establecer la conexión. */
	public static final int TIMEOUT_CONEXION_MS = 5000;
	private static final String NOMBRE_HILO = "Cliente-Lector";

	private final Enlace enlace;
	private final EventBus bus;
	private final AtomicBoolean cerrado = new AtomicBoolean();
	private final AtomicBoolean iniciado = new AtomicBoolean();

	private ClientePartida(Enlace enlace, EventBus bus) {
		this.enlace = enlace;
		this.bus = bus;
	}

	/** Conecta (bloquea hasta conectar o agotar el timeout). */
	public static ClientePartida conectar(String host, int puerto, EventBus bus)
			throws IOException {
		Socket socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, puerto), TIMEOUT_CONEXION_MS);
			return new ClientePartida(new Enlace(socket), bus);
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
		Thread lector = new Thread(this::leer, NOMBRE_HILO);
		lector.setDaemon(true);
		lector.start();
	}

	private void leer() {
		enlace.leer(m -> bus.publicar(
				new Evento(TipoEvento.MENSAJE_RED, new MensajeRecibido(Constantes.ID_HOST, m))));
		if (!cerrado.get()) {
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		}
	}

	@Override
	public void enviar(MensajeRed mensaje) {
		if (!cerrado.get()) {
			enlace.enviar(mensaje);
		}
	}

	@Override
	public void close() {
		if (cerrado.compareAndSet(false, true)) {
			enlace.close();
		}
	}
}
