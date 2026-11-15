package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Un socket con lectura y escritura de líneas UTF-8 (uso interno del paquete).
 *
 * <p>La escritura está sincronizada (varios hilos pueden enviar); la lectura la hace un único
 * hilo dueño del enlace. {@link #close()} es idempotente y puede llamarse desde cualquier hilo:
 * destraba la lectura bloqueada.
 */
final class Enlace implements AutoCloseable {
	private final Socket socket;
	private final BufferedReader entrada;
	private final PrintWriter salida;

	Enlace(Socket socket) throws IOException {
		this.socket = socket;
		this.entrada = new BufferedReader(
				new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		this.salida = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
	}

	/** Manda una línea; si el socket está muerto se ignora (PrintWriter no lanza). */
	synchronized void enviar(MensajeRed mensaje) {
		salida.println(mensaje.aLinea());
	}

	/** Lee hasta el fin del flujo o un error, entregando solo los mensajes válidos. */
	void leer(Consumer<MensajeRed> receptor) {
		try {
			String linea;
			while ((linea = entrada.readLine()) != null) {
				MensajeRed.desdeLinea(linea).ifPresent(receptor);
			}
		} catch (IOException e) {
			// corte de la conexión: el llamador lo informa al terminar
		}
	}

	/** Cierra el socket sin lanzar. */
	@Override
	public void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// ya cerrado o inutilizable
		}
	}

	/** Cierra solo la escritura (el par recibe EOF tras leer lo enviado). */
	void cerrarEscritura() {
		try {
			socket.shutdownOutput();
		} catch (IOException e) {
			// socket ya cerrado
		}
	}

	Socket socket() {
		return socket;
	}
}
