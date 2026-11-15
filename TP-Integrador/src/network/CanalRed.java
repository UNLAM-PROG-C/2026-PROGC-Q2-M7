package network;

/**
 * Conexión de red de una partida. Lo recibido se publica en el EventBus como eventos
 * MENSAJE_RED (y JUGADOR_CONECTADO / JUGADOR_DESCONECTADO / CONEXION_PERDIDA).
 */
public interface CanalRed extends AutoCloseable {
	/** Arranca los hilos de lectura (y de aceptación, en el servidor). */
	void iniciar();

	/** Cliente: manda al host. Servidor: manda a todos los clientes conectados. */
	void enviar(MensajeRed mensaje);

	/** Cierra todas las conexiones. No lanza excepciones. */
	@Override
	void close();
}
