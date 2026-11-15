package controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
	private final ScheduledExecutorService timer =
			Executors.newScheduledThreadPool(Constantes.HILOS_TIMER);
	private final Consumer<ActualizacionVista> vista;
	private final Set<Integer> revanchasClientes = new HashSet<>();
	private Thread hiloConsumidor;
	private ScheduledFuture<?> tareaTomate;
	private TableroZip tablero;
	private GeneradorNivel generador;
	private Dificultad dificultadActual;
	private boolean manchadoPorTomate;
	private boolean revanchaLocal;
	private int rondasObjetivo;
	private int rondasGanadasLocal;
	private int rondasGanadasRival;
	private boolean partidaFinalizada;

	public MotorJuego(int rol, EventBus bus) {
		this(rol, bus, null);
	}

	public MotorJuego(int rol, EventBus bus, Consumer<ActualizacionVista> vista) {
		this(rol, Constantes.IP_LOCAL, Constantes.PUERTO, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, EventBus bus, Consumer<ActualizacionVista> vista) {
		this(rol, host, puerto, Constantes.RONDAS_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, int rondasObjetivo, EventBus bus,
			Consumer<ActualizacionVista> vista) {
		this(rol, host, puerto, rondasObjetivo, Constantes.JUGADORES_RED_POR_DEFECTO, bus, vista);
	}

	public MotorJuego(int rol, String host, int puerto, int rondasObjetivo, int jugadores,
			EventBus bus, Consumer<ActualizacionVista> vista) {
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

	public void conectar() throws IOException {
		if (red != null) {
			red.conectar();
		}
	}

	public void iniciar() {
		if (hiloConsumidor != null) {
			return;
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
			case GRILLA_COMPLETA -> procesarGrillaCompleta();
			case LIMPIAR_TOMATE -> procesarLimpiarTomate();
			default -> despacharEventoRed(event);
		}
	}

	private void despacharEventoRed(Evento event) {
		switch (event.tipo()) {
			case PARTIDA_INICIADA -> procesarPartidaIniciada(event.payload());
			case SOLICITAR_REVANCHA -> procesarSolicitarRevancha(event.payload());
			case RIVAL_COMPLETO_GRILLA -> procesarRivalCompletoGrilla(event.payload());
			case PARTIDA_FINALIZADA -> procesarPartidaFinalizada();
			case JUGADOR_DESCONECTADO -> procesarJugadorDesconectado(event.payload());
			case CONEXION_PERDIDA -> procesarConexionPerdida();
			default -> System.err.println("[MOTOR] Evento no conocido: " + event.tipo());
		}
	}

	private void procesarIniciar(Object payload) {
		if (payload instanceof Dificultad d) {
			this.dificultadActual = d;
		}
		if (rol == Constantes.ROL_LOCAL) {
			iniciarPartidaConSemilla(ThreadLocalRandom.current().nextLong());
		} else if (tablero == null) {
			lanzarPartidaAutoridad();
		} else {
			solicitarRevanchaLocal();
		}
	}

	private void solicitarRevanchaLocal() {
		if (!hayRivalesConectados()) {
			procesarConexionPerdida();
			return;
		}
		revanchaLocal = true;
		if (rol == Constantes.ROL_CLIENTE) {
			red.enviarEvento(TipoEvento.SOLICITAR_REVANCHA);
			notificarVista("Esperando que el rival acepte jugar de nuevo...");
		} else if (revanchaLista()) {
			lanzarPartidaAutoridad();
		} else {
			notificarVista("Esperando que el rival acepte jugar de nuevo...");
		}
	}

	private void procesarSolicitarRevancha(Object payload) {
		if (rol != Constantes.ROL_HOST || !partidaFinalizada || !(payload instanceof Integer idCliente)) {
			return;
		}
		revanchasClientes.add(idCliente);
		if (revanchaLista()) {
			lanzarPartidaAutoridad();
		} else {
			notificarVista("El rival quiere la revancha. Tocá 'Jugar de nuevo' para aceptar.");
		}
	}

	private boolean revanchaLista() {
		return revanchaLocal && hayRivalesConectados()
				&& revanchasClientes.size() >= red.clientesConectados();
	}

	private boolean hayRivalesConectados() {
		return red != null && red.clientesConectados() > 0;
	}

	private void lanzarPartidaAutoridad() {
		if (rol != Constantes.ROL_HOST) {
			return;
		}
		if (!hayRivalesConectados()) {
			procesarConexionPerdida();
			return;
		}
		long semilla = ThreadLocalRandom.current().nextLong();
		red.enviarEvento(TipoEvento.PARTIDA_INICIADA, armarDatosInicio(semilla));
		iniciarPartidaConSemilla(semilla);
	}

	private String armarDatosInicio(long semilla) {
		return dificultadActual.name() + SEPARADOR_DATOS + rondasObjetivo + SEPARADOR_DATOS + semilla;
	}

	private void procesarPartidaIniciada(Object payload) {
		if (!(payload instanceof String datos)) { return; }
		String[] partes = datos.split(SEPARADOR_DATOS);
		if (partes.length != PARTES_INICIO) { return; }
		try {
			this.dificultadActual = Dificultad.valueOf(partes[0]);
			this.rondasObjetivo = acotarRondas(Integer.parseInt(partes[1]));
			iniciarPartidaConSemilla(Long.parseLong(partes[2]));
		} catch (IllegalArgumentException e) {
			System.err.println("[MOTOR] Datos de inicio inválidos: " + e.getMessage());
		}
	}

	private void iniciarPartidaConSemilla(long semilla) {
		cancelarTomate();
		this.generador = new GeneradorNivel(semilla);
		this.rondasGanadasLocal = 0;
		this.rondasGanadasRival = 0;
		this.partidaFinalizada = false;
		this.manchadoPorTomate = false;
		this.revanchaLocal = false;
		this.revanchasClientes.clear();
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
			declararVictoriaLocal();
			return;
		}
		if (red != null) {
			red.enviarEvento(TipoEvento.RIVAL_COMPLETO_GRILLA, rondasGanadasLocal);
		}
		avanzarSiguienteRonda();
	}

	private void declararVictoriaLocal() {
		cancelarTomate();
		partidaFinalizada = true;
		manchadoPorTomate = false;
		if (red != null) {
			red.enviarEvento(TipoEvento.PARTIDA_FINALIZADA);
		}
		notificarVista("¡GANASTE LA PARTIDA! (" + rondasGanadasLocal + " - " + rondasGanadasRival + ")");
	}

	private void avanzarSiguienteRonda() {
		cancelarTomate();
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
		programarLimpiezaTomate();
	}

	private void programarLimpiezaTomate() {
		cancelarTomate();
		tareaTomate = timer.schedule(
				() -> bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE)),
				Constantes.SEGUNDOS_TOMATE,
				TimeUnit.SECONDS);
	}

	private void cancelarTomate() {
		if (tareaTomate != null) {
			tareaTomate.cancel(false);
			tareaTomate = null;
		}
	}

	private void actualizarRondasRival(Object payload) {
		if (payload == null) {
			rondasGanadasRival++;
			return;
		}
		try {
			int rondasRemotas = Integer.parseInt(payload.toString().trim());
			rondasGanadasRival = Math.max(rondasGanadasRival, rondasRemotas);
		} catch (NumberFormatException e) {
			System.err.println("[MOTOR] Puntaje de rival inválido: " + e.getMessage());
		}
	}

	private void procesarLimpiarTomate() {
		if (!partidaFinalizada && manchadoPorTomate) {
			manchadoPorTomate = false;
			notificarVista("Se limpió el tomate. ¡Seguí jugando!");
		}
	}

	private void procesarPartidaFinalizada() {
		cancelarTomate();
		partidaFinalizada = true;
		manchadoPorTomate = false;
		rondasGanadasRival = rondasObjetivo;
		notificarVista("¡El rival completó " + rondasObjetivo + " rondas y ganó la partida!");
	}

	private void procesarJugadorDesconectado(Object payload) {
		if (payload instanceof Integer idCliente) {
			revanchasClientes.remove(idCliente);
		}
		if (partidaFinalizada && revanchaLista()) {
			lanzarPartidaAutoridad();
		} else if (!partidaFinalizada && tablero != null) {
			notificarVista("Un rival se desconectó de la partida.");
		}
	}

	private void procesarConexionPerdida() {
		cancelarTomate();
		this.tablero = null;
		this.partidaFinalizada = false;
		this.manchadoPorTomate = false;
		this.revanchaLocal = false;
		this.revanchasClientes.clear();
		notificarVista("Conexión cerrada: el rival abandonó la partida.");
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
		cancelarTomate();
		if (hiloConsumidor != null) {
			hiloConsumidor.interrupt();
		}
		timer.shutdownNow();
		if (red != null) {
			red.cerrar();
		}
	}
}
