package controller;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

import events.DestinoCelda;
import events.EventBus;
import events.Evento;
import events.TipoEvento;
import model.Dificultad;
import model.Direccion;
import model.EstadoJuego;
import model.GeneradorNivel;
import model.ResultadoMovimiento;
import model.TableroZip;
import network.GestorRed;
import utils.Constantes;

public class MotorJuego {
	private static final String SEPARADOR_DATOS = ":";
	private static final int PARTES_INICIO = 3;

	private final int rol;
	private final EventBus bus;
	private final GestorRed red;
	private Thread hiloConsumidor;
	private final ScheduledExecutorService timer =
			Executors.newScheduledThreadPool(Constantes.HILOS_TIMER);
	private TableroZip tablero;
	private GeneradorNivel generador;
	private Dificultad dificultadActual;
	private final Consumer<ActualizacionVista> vista;
	private boolean manchadoPorTomate;
	private int rondasObjetivo;
	private int rondasGanadasLocal;
	private int rondasGanadasRival;
	private boolean partidaFinalizada;

	public MotorJuego(int rol, EventBus bus) throws IOException {
		this(rol, bus, null);
	}

	public MotorJuego(int rol, EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, EventBus bus,
			Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, host, puerto, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, int rondasObjetivo, EventBus bus,
			Consumer<ActualizacionVista> vista) throws IOException {
		this(rol, host, puerto, rondasObjetivo, Constantes.JUGADORES_RED_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, int rondasObjetivo, int jugadores,
			EventBus bus, Consumer<ActualizacionVista> vista) throws IOException {
		this.rol = rol;
		this.bus = bus;
		this.vista = vista;
		this.generador = new GeneradorNivel();
		this.dificultadActual = Dificultad.MEDIA;
		this.rondasObjetivo = acotarRondas(rondasObjetivo);
		boolean enRed = rol == Constantes.ROL_HOST || rol == Constantes.ROL_CLIENTE;
		this.red = enRed ? new GestorRed(rol, host, puerto, Math.max(1, jugadores - 1), bus) : null;
	}

	private static int acotarRondas(int rondas) {
		return Math.max(Constantes.MIN_RONDAS, Math.min(Constantes.MAX_RONDAS, rondas));
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
				if (event != null && event.tipo() != null) {
					despacharEvento(event);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void despacharEvento(Evento event) {
		switch (event.tipo()) {
			case INICIAR -> procesarIniciar(event.payload());
			case MOVIMIENTO -> procesarMovimiento(event.payload());
			case IR_A_CELDA -> procesarIrACelda(event.payload());
			case DESHACER -> procesarDeshacer();
			case REINICIAR -> procesarReiniciar();
			case RIVAL_COMPLETO_GRILLA -> procesarRivalCompletoGrilla(event.payload());
			case LIMPIAR_TOMATE -> procesarLimpiarTomate();
			case PARTIDA_FINALIZADA -> procesarPartidaFinalizada();
			case CONEXION_PERDIDA -> procesarConexionPerdida();
			case GRILLA_COMPLETA -> procesarGrillaCompleta();
			default -> System.err.println("Evento no conocido: " + event.tipo());
		}
	}

	private void procesarIniciar(Object payload) {
		if (payload instanceof String datosInicio) {
			iniciarDesdeRed(datosInicio);
		} else if (rol == Constantes.ROL_CLIENTE && red != null) {
			red.enviarEvento(TipoEvento.INICIAR);
		} else {
			iniciarComoAutoridad(payload);
		}
	}

	private void iniciarComoAutoridad(Object payload) {
		if (payload instanceof Dificultad d) {
			this.dificultadActual = d;
		}
		long semilla = ThreadLocalRandom.current().nextLong();
		if (rol == Constantes.ROL_HOST && red != null) {
			red.enviarEvento(TipoEvento.INICIAR, armarDatosInicio(semilla));
		}
		iniciarPartidaConSemilla(semilla);
	}

	private String armarDatosInicio(long semilla) {
		return dificultadActual.name() + SEPARADOR_DATOS + rondasObjetivo + SEPARADOR_DATOS + semilla;
	}

	private void iniciarDesdeRed(String datos) {
		String[] partes = datos.split(SEPARADOR_DATOS);
		if (partes.length != PARTES_INICIO) {
			return;
		}
		try {
			this.dificultadActual = Dificultad.valueOf(partes[0]);
			this.rondasObjetivo = acotarRondas(Integer.parseInt(partes[1]));
			iniciarPartidaConSemilla(Long.parseLong(partes[2]));
		} catch (IllegalArgumentException ignored) {
			// Mensaje de inicio inválido
		}
	}

	private void iniciarPartidaConSemilla(long semilla) {
		this.generador = new GeneradorNivel(semilla);
		this.rondasGanadasLocal = 0;
		this.rondasGanadasRival = 0;
		this.partidaFinalizada = false;
		this.manchadoPorTomate = false;
		this.tablero = new TableroZip(generador.generar(dificultadActual));
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

	private void procesarIrACelda(Object payload) {
		if (partidaFinalizada || tablero == null || !(payload instanceof DestinoCelda destino)
				|| destino.nivel() != tablero.nivel()) {
			return;
		}
		ResultadoMovimiento resultado = tablero.irA(destino.celda());
		if (resultado == ResultadoMovimiento.SIN_EFECTO) {
			return;
		}
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
			return;
		}
		if (red != null) {
			red.enviarEvento(TipoEvento.RIVAL_COMPLETO_GRILLA, rondasGanadasLocal);
		}
		avanzarSiguienteRonda();
	}

	private void avanzarSiguienteRonda() {
		notificarVista("¡Ronda ganada! (" + rondasGanadasLocal + "/" + rondasObjetivo + "). Siguiente ronda...");
		this.tablero = new TableroZip(generador.generar(dificultadActual));
		this.manchadoPorTomate = false;
		notificarVista("Nueva ronda. Marcador: Vos " + rondasGanadasLocal + " - " + rondasGanadasRival + " Rival");
	}

	private void procesarRivalCompletoGrilla(Object payload) {
		if (partidaFinalizada) {
			return;
		}
		actualizarRondasRival(payload);
		if (rondasGanadasRival >= rondasObjetivo) {
			procesarPartidaFinalizada();
			return;
		}
		manchadoPorTomate = true;
		notificarVista("¡Rival completó grilla! (" + rondasGanadasLocal + " - "
				+ rondasGanadasRival + ") ¡Tomatazo recibido!");
		timer.schedule(() -> bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE)),
				Constantes.SEGUNDOS_TOMATE, TimeUnit.SECONDS);
	}

	private void actualizarRondasRival(Object payload) {
		if (payload instanceof String texto) {
			try {
				rondasGanadasRival = Math.max(rondasGanadasRival, Integer.parseInt(texto.trim()));
				return;
			} catch (NumberFormatException ignored) {
				// Usa incremento simple si no viene número
			}
		}
		rondasGanadasRival++;
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
