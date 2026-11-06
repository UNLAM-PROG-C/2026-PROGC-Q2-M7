package network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import events.*;
import utils.Constantes;

public class GestorRed {
	private final Socket socket;
	private final EventBus bus;
	private final PrintWriter salida;
	private final BufferedReader entrada;

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
			System.out.println("Esperando rival en puerto " + puerto + "...");
			try (ServerSocket server = new ServerSocket(puerto)) {
				return server.accept();
			}
		}
		System.out.println("Conectando a " + host + ":" + puerto + "...");
		return new Socket(host, puerto);
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
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		}
	}

	public void enviarEvento(TipoEvento tipo) {
		if (salida != null && tipo != null) {
			salida.println(tipo.name());
		}
	}

	public void cerrar() throws IOException {
		if (salida != null) {
			salida.close();
		}
		if (entrada != null) {
			entrada.close();
		}
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}
}
