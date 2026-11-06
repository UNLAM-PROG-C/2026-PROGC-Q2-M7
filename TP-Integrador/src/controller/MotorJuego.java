package controller;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

import events.*;
import model.Dificultad;
import model.Direccion;
import model.EstadoJuego;
import model.GeneradorNivel;
import model.ResultadoMovimiento;
import model.TableroZip;
import network.GestorRed;
import utils.Constantes;

public class MotorJuego {
	private final EventBus bus;
	private final GestorRed red;
	private Thread hiloConsumidor;
	private final ScheduledExecutorService timer =
		Executors.newScheduledThreadPool(Constantes.HILOS_TIMER);
	private TableroZip tablero;
	private final GeneradorNivel generador;
	private Dificultad dificultadActual;
	private final Consumer<ActualizacionVista> vista;
	private boolean manchadoPorTomate;

	public MotorJuego(int rol, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this.bus = bus;
		this.vista = vista;
		this.generador = new GeneradorNivel();
		this.dificultadActual = Dificultad.MEDIA;
		this.manchadoPorTomate = false;
		if (rol == Constantes.ROL_HOST || rol == Constantes.ROL_CLIENTE) {
			this.red = new GestorRed(rol, host, puerto, bus);
		} else {
			this.red = null;
		}
	}

	public MotorJuego(int rol, EventBus bus) throws IOException {
		this(rol, bus, null);
	}

	public void iniciar() {
		if (red != null) {
			red.iniciarEscucha();
		}
		hiloConsumidor = new Thread(this::cicloConsumidor, "Thread-MotorJuego");
		hiloConsumidor.setDaemon(true);
		hiloConsumidor.start();
	}

	private void cicloConsumidor() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Evento event = bus.tomar();
				despacharEvento(event);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void despacharEvento(Evento event) {
		if (event == null || event.tipo() == null) {
			return;
		}
		switch (event.tipo()) {
			case INICIAR -> procesarIniciar(event.payload());
			case MOVIMIENTO -> procesarMovimiento(event.payload());
			case DESHACER -> procesarDeshacer();
			case REINICIAR -> procesarReiniciar();
			case RIVAL_COMPLETO_GRILLA -> procesarRivalCompletoGrilla();
			case LIMPIAR_TOMATE -> procesarLimpiarTomate();
			case CONEXION_PERDIDA -> procesarConexionPerdida();
			case GRILLA_COMPLETA -> procesarGrillaCompleta();
			default -> System.err.println("Evento no conocido: " + event.tipo());
		}
	}

	private void procesarIniciar(Object payload) {
		if (payload instanceof Dificultad d) {
			this.dificultadActual = d;
		} else if (this.dificultadActual == null) {
			this.dificultadActual = Dificultad.MEDIA;
		}
		this.tablero = new TableroZip(generador.generar(dificultadActual));
		this.manchadoPorTomate = false;
		notificarVista("¡Nivel iniciado! Conectá los números en orden.");
	}

	private void procesarDeshacer() {
		if (tablero != null) {
			boolean ok = tablero.deshacer();
			notificarVista(ok ? "Paso deshecho." : "No se puede deshacer más.");
		}
	}

	private void procesarReiniciar() {
		if (tablero != null) {
			tablero.reiniciar();
			notificarVista("Nivel reiniciado.");
		}
	}

	private void procesarMovimiento(Object payload) {
		if (tablero == null || !(payload instanceof Direccion direccion)) {
			return;
		}
		ResultadoMovimiento resultado = tablero.mover(direccion);
		notificarVista(resultado.mensaje());
		if (tablero.estadoJuego().gano()) {
			if (red != null) {
				procesarGrillaCompleta();
			}
		}
	}

	private void procesarGrillaCompleta() {
		System.out.println("[LOCAL] ¡Completaste la grilla! Enviando tomate al rival...");
		if (red != null) {
			red.enviarEvento(TipoEvento.RIVAL_COMPLETO_GRILLA);
		}
		notificarVista("¡Completaste la grilla! Enviando tomate al rival...");
	}

	private void procesarRivalCompletoGrilla() {
		System.out.println("[TOMATE] ¡Rival completó grilla! Pantalla manchada por " + Constantes.SEGUNDOS_TOMATE + "s...");
		manchadoPorTomate = true;
		notificarVista("¡Rival completó la grilla! ¡Tomatazo recibido por " + Constantes.SEGUNDOS_TOMATE + "s!");
		timer.schedule(() -> 
			bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE)),
			Constantes.SEGUNDOS_TOMATE, TimeUnit.SECONDS
		);
	}

	private void procesarLimpiarTomate() {
		System.out.println("[LIMPIO] Pasaron los " + Constantes.SEGUNDOS_TOMATE + "s. Se limpió el tomate.");
		manchadoPorTomate = false;
		notificarVista("Se limpió el tomate. ¡Seguí jugando!");
	}

	private void procesarConexionPerdida() {
		System.err.println("[RED] Conexión perdida.");
		notificarVista("Conexión con el rival perdida.");
	}

	private void notificarVista(String mensaje) {
		if (vista != null) {
			EstadoJuego estado = (tablero != null) ? tablero.estadoJuego() : null;
			ActualizacionVista actualizacion = new ActualizacionVista(estado, mensaje, manchadoPorTomate);
			SwingUtilities.invokeLater(() -> vista.accept(actualizacion));
		}
	}

	public void finalizar() {
		if (hiloConsumidor != null) {
			hiloConsumidor.interrupt();
		}
		timer.shutdownNow();
		if (red != null) {
			try {
				red.cerrar();
			} catch (IOException e) {
				System.err.println("Error al cerrar: " + e.getMessage());
			}
		}
	}
}
