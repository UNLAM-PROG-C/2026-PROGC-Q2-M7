package controller;

import events.DestinoCelda;
import events.Evento;
import events.EventBus;
import events.TipoEvento;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.SwingUtilities;
import model.ConfigPartida;
import model.Direccion;
import model.ResultadoMovimiento;
import network.CanalRed;
import network.CanalServidor;
import network.ClientePartida;
import network.DireccionLocal;
import network.MensajeRecibido;
import network.ServidorPartida;
import utils.Constantes;

/**
 * Motor del juego: único hilo consumidor del {@link EventBus} y único dueño del estado
 * (tablero, sala, marcador, tomate). La red y el timer solo publican eventos; la vista recibe
 * fotos inmutables vía {@code invokeLater}. Lo que difiere entre partida local, host y cliente
 * se delega en un {@link RolPartida} (Strategy). Los eventos se despachan por una tabla
 * tipo de evento a manejador (Command).
 */
public final class MotorJuego {
	private static final String NOMBRE_HILO = "Thread-MotorJuego";
	private static final String SEPARADOR_DIRECCION = ":";
	private static final String MSJ_INICIO = "¡Partida iniciada! Conectá los números en orden.";
	private static final String MSJ_DESHECHO = "Paso deshecho.";
	private static final String MSJ_SIN_DESHACER = "No se puede deshacer más.";
	private static final String MSJ_REINICIO = "Ronda reiniciada.";
	private static final String MSJ_RONDA = "¡Ronda ganada! (%d/%d). Siguiente ronda...";
	private static final String MSJ_ESPERA = "Completaste las rondas. Esperando confirmación…";
	private static final String MSJ_TOMATE = "¡%s completó una ronda! ¡Tomatazo!";
	private static final String MSJ_RIVAL_LLEGO = "%s completó todas sus rondas.";
	private static final String MSJ_TOMATE_LIMPIO = "Se limpió el tomate. ¡Seguí jugando!";
	private static final String MSJ_DESCONEXION = "Un jugador se desconectó.";
	private static final String MSJ_GANASTE = "¡GANASTE LA PARTIDA!";
	private static final String MSJ_GANADOR = "%s ganó la partida.";

	private final EventBus bus;
	private final Consumer<ActualizacionVista> vista;
	private final Consumer<EstadoSala> consumidorSala;
	private final CanalRed red;
	private final SalaPartida sala;
	private final TomateTemporizado tomate;
	private final AtomicBoolean finalizado = new AtomicBoolean();
	private final Map<TipoEvento, Consumer<Object>> manejadores =
			new EnumMap<>(TipoEvento.class);
	private final RolPartida rol;
	private volatile PartidaEnCurso partida;
	private volatile Thread hilo;
	private boolean manchadoPorTomate;
	private boolean viendoSala = true;
	private boolean abortada;

	private MotorJuego(EventBus bus, Consumer<ActualizacionVista> vista,
			Consumer<EstadoSala> consumidorSala, CanalRed red, SalaPartida sala,
			Function<MotorJuego, RolPartida> fabricaRol, long duracionTomateMilis) {
		this.bus = bus;
		this.vista = vista;
		this.consumidorSala = consumidorSala;
		this.red = red;
		this.sala = sala;
		this.tomate = new TomateTemporizado(bus, duracionTomateMilis);
		this.rol = fabricaRol.apply(this);
		registrarManejadores();
	}

	/** Partida individual sin red. */
	public static MotorJuego local(EventBus bus, Consumer<ActualizacionVista> vista) {
		SalaPartida sala = new SalaPartida(false, Constantes.ID_HOST, "");
		return new MotorJuego(bus, vista, null, null, sala, RolLocal::new,
				TomateTemporizado.DURACION_MILIS);
	}

	/** Host de una partida en red: abre el puerto sin bloquear. */
	public static MotorJuego host(ConfigPartida config, String nombre, int puerto, EventBus bus,
			Consumer<ActualizacionVista> vista, Consumer<EstadoSala> sala) throws IOException {
		CanalServidor canal = ServidorPartida.abrir(puerto, config.jugadores() - 1, bus);
		return host(config, nombre, bus, vista, sala, canal, TomateTemporizado.DURACION_MILIS);
	}

	/** Cliente de una partida en red: bloquea hasta conectar. */
	public static MotorJuego cliente(String nombre, String host, int puerto, EventBus bus,
			Consumer<ActualizacionVista> vista, Consumer<EstadoSala> sala) throws IOException {
		CanalRed canal = ClientePartida.conectar(host, puerto, bus);
		String direccion = host + SEPARADOR_DIRECCION + puerto;
		return cliente(nombre, direccion, bus, vista, sala, canal,
				TomateTemporizado.DURACION_MILIS);
	}

	/** Fábrica para pruebas con un canal de servidor falso. */
	static MotorJuego host(ConfigPartida config, String nombre, EventBus bus,
			Consumer<ActualizacionVista> vista, Consumer<EstadoSala> consumidorSala,
			CanalServidor canal, long duracionTomateMilis) {
		String direccion = DireccionLocal.ipParaCompartir() + SEPARADOR_DIRECCION + canal.puerto();
		SalaPartida sala = new SalaPartida(true, Constantes.ID_HOST, direccion);
		sala.config(config);
		sala.agregar(Constantes.ID_HOST, nombre);
		return new MotorJuego(bus, vista, consumidorSala, canal, sala,
				m -> new RolHost(m, canal), duracionTomateMilis);
	}

	/** Fábrica para pruebas con un canal de cliente falso. */
	static MotorJuego cliente(String nombre, String direccion, EventBus bus,
			Consumer<ActualizacionVista> vista, Consumer<EstadoSala> consumidorSala,
			CanalRed canal, long duracionTomateMilis) {
		SalaPartida sala = new SalaPartida(false, SalaPartida.SIN_ID, direccion);
		return new MotorJuego(bus, vista, consumidorSala, canal, sala,
				m -> new RolCliente(m, canal, nombre), duracionTomateMilis);
	}

	private void registrarManejadores() {
		manejadores.put(TipoEvento.INICIAR, p -> rol.alIniciarLocal(p));
		manejadores.put(TipoEvento.MOVIMIENTO, this::procesarMovimiento);
		manejadores.put(TipoEvento.IR_A_CELDA, this::procesarIrACelda);
		manejadores.put(TipoEvento.DESHACER, p -> procesarDeshacer());
		manejadores.put(TipoEvento.REINICIAR, p -> procesarReiniciar());
		manejadores.put(TipoEvento.LISTO, this::procesarListo);
		manejadores.put(TipoEvento.VOLVER_SALA, p -> procesarVolverSala());
		manejadores.put(TipoEvento.LIMPIAR_TOMATE, this::procesarLimpiarTomate);
		manejadores.put(TipoEvento.MENSAJE_RED, this::procesarMensajeRed);
		manejadores.put(TipoEvento.JUGADOR_DESCONECTADO, this::procesarDesconexion);
		manejadores.put(TipoEvento.CONEXION_PERDIDA, p -> procesarConexionPerdida());
	}

	/** Arranca la red y el hilo consumidor; el host notifica la sala y el cliente manda HOLA. */
	public void iniciar() {
		if (hilo != null || finalizado.get()) {
			return;
		}
		if (red != null) {
			red.iniciar();
		}
		rol.alArrancar();
		Thread nuevo = new Thread(this::cicloConsumidor, NOMBRE_HILO);
		nuevo.setDaemon(true);
		hilo = nuevo;
		nuevo.start();
	}

	/** Cierra consumidor, timer, pregenerador y red. Idempotente. */
	public void finalizar() {
		if (!finalizado.compareAndSet(false, true)) {
			return;
		}
		Thread consumidor = hilo;
		if (consumidor != null) {
			consumidor.interrupt();
		}
		tomate.close();
		cerrarPartida();
		if (red != null) {
			red.close();
		}
	}

	private void cicloConsumidor() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				despachar(bus.tomar());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	private void despachar(Evento evento) {
		Consumer<Object> manejador = manejadores.get(evento.tipo());
		if (manejador == null) {
			return;
		}
		try {
			manejador.accept(evento.payload());
		} catch (RuntimeException e) {
			System.err.println("[MOTOR] Evento descartado " + evento.tipo() + ": " + e);
		}
	}

	// ---- Acciones del jugador local ----

	private PartidaEnCurso activa() {
		PartidaEnCurso p = partida;
		return p != null && p.puedeJugar() ? p : null;
	}

	private void procesarMovimiento(Object payload) {
		PartidaEnCurso p = activa();
		if (p == null || !(payload instanceof Direccion direccion)) {
			return;
		}
		notificarVista(p.mover(direccion).mensaje());
		verificarRondaCompleta(p);
	}

	private void procesarIrACelda(Object payload) {
		PartidaEnCurso p = activa();
		if (p == null || !(payload instanceof DestinoCelda destino)) {
			return;
		}
		ResultadoMovimiento resultado = p.irA(destino);
		if (resultado != ResultadoMovimiento.SIN_EFECTO) {
			notificarVista(resultado.mensaje());
			verificarRondaCompleta(p);
		}
	}

	private void procesarDeshacer() {
		PartidaEnCurso p = activa();
		if (p != null) {
			notificarVista(p.deshacer() ? MSJ_DESHECHO : MSJ_SIN_DESHACER);
		}
	}

	private void procesarReiniciar() {
		PartidaEnCurso p = activa();
		if (p != null) {
			p.reiniciar();
			notificarVista(MSJ_REINICIO);
		}
	}

	private void verificarRondaCompleta(PartidaEnCurso p) {
		if (!p.rondaCompleta()) {
			return;
		}
		int rondas = p.completarRondaLocal();
		rol.alCompletarRonda();
		if (!p.finalizada()) {
			notificarVista(p.esperaConfirmacion() ? MSJ_ESPERA
					: String.format(MSJ_RONDA, rondas, p.objetivo()));
		}
	}

	// ---- Eventos de sala y de red ----

	private void procesarListo(Object payload) {
		if (payload instanceof Boolean listo && !abortada && !jugando()) {
			rol.alMarcarListo(listo);
		}
	}

	private void procesarVolverSala() {
		if (jugando()) {
			return;
		}
		viendoSala = true;
		rol.alVolverSala();
		notificarSala();
	}

	private void procesarMensajeRed(Object payload) {
		if (!abortada && payload instanceof MensajeRecibido m && m.mensaje() != null) {
			rol.alRecibir(m);
		}
	}

	private void procesarDesconexion(Object payload) {
		if (!abortada && payload instanceof Integer id) {
			rol.alDesconectarse(id);
		}
	}

	private void procesarConexionPerdida() {
		if (!abortada) {
			rol.alPerderConexion();
		}
	}

	private void procesarLimpiarTomate(Object payload) {
		if (tomate.consumir(payload)) {
			manchadoPorTomate = false;
			notificarVista(MSJ_TOMATE_LIMPIO);
		}
	}

	// ---- API para los roles (siempre en el hilo del motor) ----

	SalaPartida sala() {
		return sala;
	}

	PartidaEnCurso partida() {
		return partida;
	}

	/** Hay una partida empezada y sin ganador todavía. */
	boolean jugando() {
		PartidaEnCurso p = partida;
		return p != null && !p.finalizada();
	}

	long nuevaSemilla() {
		return ThreadLocalRandom.current().nextLong();
	}

	void empezar(long semilla) {
		if (sala.config() == null) {
			return;
		}
		cerrarPartida();
		partida = new PartidaEnCurso(sala.config(), semilla, sala.idLocal(), sala.jugadores());
		tomate.cancelar();
		manchadoPorTomate = false;
		viendoSala = false;
		notificarVista(MSJ_INICIO);
	}

	/** Otro jugador completó una ronda: lo anota y le cae el tomate al local. */
	void rondaDeOtro(int id) {
		PartidaEnCurso p = partida;
		p.sumarRonda(id);
		String nombre = p.marcador().nombre(id);
		if (p.alcanzoObjetivo(id)) {
			notificarVista(String.format(MSJ_RIVAL_LLEGO, nombre));
			return;
		}
		manchadoPorTomate = true;
		tomate.aplicar();
		notificarVista(String.format(MSJ_TOMATE, nombre));
	}

	void terminar(int idGanador) {
		PartidaEnCurso p = partida;
		if (p == null || p.finalizada()) {
			return;
		}
		p.finalizar(idGanador);
		tomate.cancelar();
		manchadoPorTomate = false;
		sala.reiniciarListos();
		notificarVista(idGanador == p.idLocal() ? MSJ_GANASTE
				: String.format(MSJ_GANADOR, p.marcador().nombre(idGanador)));
	}

	void jugadorSeFue(int id, String mensaje) {
		PartidaEnCurso p = partida;
		if (p == null) {
			return;
		}
		p.marcador().desconectar(id);
		if (!p.finalizada()) {
			notificarVista(mensaje);
		}
	}

	void sincronizarConectados(List<JugadorSala> presentes) {
		PartidaEnCurso p = partida;
		if (p != null) {
			List<Integer> ids = presentes.stream().map(JugadorSala::id).toList();
			if (p.marcador().sincronizarConectados(ids)) {
				notificarVista(MSJ_DESCONEXION);
			}
		}
	}

	/** Error irrecuperable de la conexión: cierra todo y avisa a la vista. */
	void abortar(String mensaje) {
		abortada = true;
		tomate.cancelar();
		manchadoPorTomate = false;
		cerrarPartida();
		if (red != null) {
			red.close();
		}
		if (vista != null) {
			SwingUtilities.invokeLater(() -> vista.accept(ActualizacionVista.error(mensaje)));
		}
	}

	void notificarVista(String mensaje) {
		PartidaEnCurso p = partida;
		if (p == null || vista == null || finalizado.get()) {
			return;
		}
		ActualizacionVista foto = p.foto(mensaje, manchadoPorTomate);
		SwingUtilities.invokeLater(() -> vista.accept(foto));
	}

	void notificarSala() {
		if (consumidorSala == null || !viendoSala || finalizado.get()) {
			return;
		}
		EstadoSala foto = sala.fotoEstado();
		SwingUtilities.invokeLater(() -> consumidorSala.accept(foto));
	}

	private void cerrarPartida() {
		PartidaEnCurso p = partida;
		partida = null;
		if (p != null) {
			p.close();
		}
	}
}
