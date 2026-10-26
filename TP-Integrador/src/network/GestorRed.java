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
		this.socket = obtenerSocket(rol);
		this.bus = bus;
		this.salida = new PrintWriter(socket.getOutputStream(), true);
		this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private Socket obtenerSocket(int rol) throws IOException {
		if (rol == Constantes.ROL_HOST) {
			System.out.println("Esperando rival en puerto " + Constantes.PUERTO + "...");
			try (ServerSocket server = new ServerSocket(Constantes.PUERTO)) {
				return server.accept();
			}
		}
		System.out.println("Conectando a " + Constantes.IP_LOCAL + ":" + Constantes.PUERTO + "...");
		return new Socket(Constantes.IP_LOCAL, Constantes.PUERTO);
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
				bus.publicar(new Evento(TipoEvento.desdeTexto(linea)));
			}
		} catch (IOException e) {
			bus.publicar(new Evento(TipoEvento.CONEXION_PERDIDA));
		}
	}

	public void enviarEvento(TipoEvento tipo) {
		salida.println(tipo.name());
	}

	public void cerrar() throws IOException {
		salida.close();
		entrada.close();
		socket.close();
	}
}
