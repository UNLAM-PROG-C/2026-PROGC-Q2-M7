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
	private final int rondasObjetivo;
	private int rondasGanadasLocal = 0;
	private int rondasGanadasRival = 0;
	private boolean partidaFinalizada = false;

	public MotorJuego(int rol, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, host, puerto, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, int rondasObjetivo, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this.bus = bus;
		this.vista = vista;
		this.generador = new GeneradorNivel();
		this.dificultadActual = Dificultad.MEDIA;
		this.manchadoPorTomate = false;
		this.rondasObjetivo = Math.max(Constantes.MIN_RONDAS, Math.min(Constantes.MAX_RONDAS, rondasObjetivo));
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
			case PARTIDA_FINALIZADA -> procesarPartidaFinalizada();
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
		this.rondasGanadasLocal = 0;
		this.rondasGanadasRival = 0;
		this.partidaFinalizada = false;
		this.tablero = new TableroZip(generador.generar(dificultadActual));
		this.manchadoPorTomate = false;
		notificarVista("¡Partida iniciada! Conectá los números en orden.");
	}

	private void procesarDeshacer() {
		if (partidaFinalizada || tablero == null) {
			return;
		}
		boolean ok = tablero.deshacer();
		notificarVista(ok ? "Paso deshecho." : "No se puede deshacer más.");
	}

	private void procesarReiniciar() {
		if (partidaFinalizada || tablero == null) {
			return;
		}
		tablero.reiniciar();
		notificarVista("Ronda reiniciada.");
	}

	private void procesarMovimiento(Object payload) {
		if (partidaFinalizada || tablero == null || !(payload instanceof Direccion direccion)) {
			return;
		}
		ResultadoMovimiento resultado = tablero.mover(direccion);
		notificarVista(resultado.mensaje());
		if (tablero.estadoJuego().gano()) {
			procesarGrillaCompleta();
		}
	}

	private void procesarGrillaCompleta() {
		rondasGanadasLocal++;
		if (rondasGanadasLocal >= rondasObjetivo) {
			partidaFinalizada = true;
			if (red != null) {
				red.enviarEvento(TipoEvento.PARTIDA_FINALIZADA);
			}
			notificarVista("¡GANASTE LA PARTIDA! (" + rondasGanadasLocal + " - " + rondasGanadasRival + ")");
		} else {
			if (red != null) {
				red.enviarEvento(TipoEvento.RIVAL_COMPLETO_GRILLA);
			}
			notificarVista("¡Ronda ganada! (" + rondasGanadasLocal + "/" + rondasObjetivo + "). Siguiente ronda...");
			this.tablero = new TableroZip(generador.generar(dificultadActual));
			this.manchadoPorTomate = false;
			notificarVista("Nueva ronda. Marcador: Vos " + rondasGanadasLocal + " - " + rondasGanadasRival + " Rival");
		}
	}

	private void procesarRivalCompletoGrilla() {
		rondasGanadasRival++;
		if (rondasGanadasRival >= rondasObjetivo) {
			partidaFinalizada = true;
			notificarVista("¡El rival completó " + rondasObjetivo + " rondas y ganó la partida!");
		} else {
			manchadoPorTomate = true;
			notificarVista("¡Rival completó grilla! (" + rondasGanadasLocal + " - " + rondasGanadasRival + ") ¡Tomatazo recibido!");
			timer.schedule(() -> 
				bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE)),
				Constantes.SEGUNDOS_TOMATE, TimeUnit.SECONDS
			);
		}
	}

	private void procesarLimpiarTomate() {
		manchadoPorTomate = false;
		notificarVista("Se limpió el tomate. ¡Seguí jugando!");
	}

	private void procesarPartidaFinalizada() {
		partidaFinalizada = true;
		rondasGanadasRival = rondasObjetivo;
		notificarVista("¡El rival completó " + rondasObjetivo + " rondas y ganó la partida!");
	}

	private void procesarConexionPerdida() {
		System.err.println("[RED] Conexión perdida.");
		notificarVista("Conexión con el rival perdida. Podés volver al menú.");
	}

	private void notificarVista(String mensaje) {
		if (vista != null) {
			EstadoJuego estado = (tablero != null) ? tablero.estadoJuego() : null;
			ActualizacionVista actualizacion = new ActualizacionVista(
					estado,
					mensaje,
					manchadoPorTomate,
					rondasGanadasLocal,
					rondasGanadasRival,
					rondasObjetivo,
					partidaFinalizada,
					partidaFinalizada && rondasGanadasLocal >= rondasObjetivo);
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
