package ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import controller.ActualizacionVista;
import controller.EstadoSala;
import controller.MotorJuego;
import controller.PuntajeJugador;
import events.EventBus;
import events.TipoEvento;
import events.Evento;
import model.ConfigPartida;
import model.Dificultad;
import model.ModoJuego;
import utils.Constantes;

/**
 * Ventana principal: orquesta las pantallas (CardLayout) y el motor de la partida.
 * La UI solo publica eventos y se redibuja con las fotos inmutables que le llegan.
 */
public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final String MULTIJUGADOR = "multijugador";
	private static final String SALA = "sala";
	private static final String SEPARADOR_PUNTAJES = " · ";
	private static final String HILO_CONEXION = "Thread-Conexion-Cliente";

	private final AtomicLong intentoConexion = new AtomicLong();
	private final Object lockConexion = new Object();
	private volatile MotorJuego motorActual;
	private volatile EventBus busActual;
	private volatile ConfigPartida configLocal =
			new ConfigPartida(Dificultad.MEDIA, Constantes.RONDAS_POR_DEFECTO, 1);
	private ModoJuego modoActual = ModoJuego.UN_JUGADOR;
	private String pantallaActual = CONFIGURACION;

	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion =
			new PanelConfiguracion(this::iniciar, this::seleccionarModo);
	private final PanelMultijugador multijugador =
			new PanelMultijugador(this::crearSala, this::conectar, this::menu);
	private final PanelSala sala =
			new PanelSala(listo -> publicar(TipoEvento.LISTO, listo), this::salirDeSala);
	private final PanelJuego juego;
	private final CapaVictoria victoria = new CapaVictoria(this::jugarDeNuevo, this::menu);

	public VentanaJuego() {
		this.juego = new PanelJuego(
				d -> publicar(TipoEvento.MOVIMIENTO, d),
				dest -> publicar(TipoEvento.IR_A_CELDA, dest),
				() -> publicar(TipoEvento.DESHACER, null),
				() -> publicar(TipoEvento.REINICIAR, null),
				this::menu);
		contenido.add(configuracion, CONFIGURACION);
		contenido.add(multijugador, MULTIJUGADOR);
		contenido.add(sala, SALA);
		contenido.add(juego, JUEGO);
		configurarVentana();
	}

	private static JFrame crearVentana() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("La ventana debe crearse en el EDT.");
		}
		Tema.aplicarDefaultsSwing();
		return new JFrame("Zip");
	}

	private void configurarVentana() {
		ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ventana.setContentPane(contenido);
		ventana.setGlassPane(victoria);
		Dimension tamano = new Dimension(Tema.ANCHO_VENTANA, Tema.ALTO_VENTANA);
		ventana.setMinimumSize(tamano);
		ventana.setPreferredSize(tamano);
		ventana.pack();
		ventana.setLocationRelativeTo(null);
		ventana.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent evento) { close(); }
		});
	}

	private void iniciar(Controles controles, Dificultad dificultad) {
		long intento = prepararPartida(ModoJuego.UN_JUGADOR, controles);
		configLocal = new ConfigPartida(dificultad, configuracion.cantidadRondas(), 1);
		configuracion.ocupado(true);
		EventBus bus = new EventBus();
		registrarMotor(bus, MotorJuego.local(bus, act -> recibir(intento, act)), intento);
		publicar(TipoEvento.INICIAR, configLocal);
	}

	private void crearSala(String nombre, int jugadores) {
		long intento = prepararRed();
		ConfigPartida config = new ConfigPartida(configuracion.dificultadSeleccionada(),
				configuracion.cantidadRondas(), jugadores);
		try {
			EventBus bus = new EventBus();
			MotorJuego motor = MotorJuego.host(config, nombre, Constantes.PUERTO, bus,
					act -> recibir(intento, act), estado -> recibirSala(intento, estado));
			registrarMotor(bus, motor, intento);
		} catch (IOException e) {
			multijugador.error("No se pudo crear la sala: " + e.getMessage());
		}
	}

	private void conectar(String nombre, DireccionHost destino) {
		long intento = prepararRed();
		multijugador.ocupado(true);
		multijugador.estado("Conectando a " + destino + "…");
		Thread hilo = new Thread(() -> conectarEnHilo(nombre, destino, intento), HILO_CONEXION);
		hilo.setDaemon(true);
		hilo.start();
	}

	private void conectarEnHilo(String nombre, DireccionHost destino, long intento) {
		if (intento != intentoConexion.get()) { return; }
		try {
			EventBus bus = new EventBus();
			MotorJuego motor = MotorJuego.cliente(nombre, destino.host(), destino.puerto(), bus,
					act -> recibir(intento, act), estado -> recibirSala(intento, estado));
			registrarMotor(bus, motor, intento);
		} catch (IOException e) {
			reportarError(intento, "No se pudo conectar con " + destino + ": " + e.getMessage());
		}
	}

	private long prepararRed() {
		return prepararPartida(ModoJuego.MULTIJUGADOR, configuracion.controlesSeleccionados());
	}

	private long prepararPartida(ModoJuego modo, Controles controles) {
		victoria.ocultarSuperposicion();
		abandonar();
		this.modoActual = modo;
		juego.controles(controles);
		return intentoConexion.get();
	}

	private void registrarMotor(EventBus bus, MotorJuego motor, long intento) {
		boolean aceptado = false;
		synchronized (lockConexion) {
			if (intento == intentoConexion.get()) {
				this.busActual = bus;
				this.motorActual = motor;
				motor.iniciar();
				aceptado = true;
			}
		}
		if (!aceptado) { motor.finalizar(); }
	}

	private void reportarError(long intento, String mensaje) {
		SwingUtilities.invokeLater(() -> {
			if (intento == intentoConexion.get()) { multijugador.error(mensaje); }
		});
	}

	private void seleccionarModo(ModoJuego modo) {
		this.modoActual = modo;
		if (modo != ModoJuego.UN_JUGADOR) {
			multijugador.configuracion(
					configuracion.dificultadSeleccionada(), configuracion.cantidadRondas());
			multijugador.ocupado(false);
			multijugador.estado(" ");
			mostrarPantalla(MULTIJUGADOR);
		}
	}

	private void recibir(long intento, ActualizacionVista act) {
		if (intento != intentoConexion.get()) { return; }
		if (act.estadoJuego() == null) {
			manejarErrorSinTablero(act.mensaje());
		} else {
			mostrarEnJuego(act);
		}
	}

	private void recibirSala(long intento, EstadoSala estado) {
		if (intento != intentoConexion.get()) { return; }
		victoria.ocultarSuperposicion();
		sala.mostrar(estado);
		multijugador.ocupado(false);
		mostrarPantalla(SALA);
	}

	private void mostrarEnJuego(ActualizacionVista actualizacion) {
		boolean entrando = !JUEGO.equals(pantallaActual);
		if (entrando) {
			configuracion.ocupado(false);
			multijugador.ocupado(false);
			mostrarPantalla(JUEGO);
		}
		juego.mostrar(actualizacion);
		boolean cerroVictoria = actualizarVictoria(actualizacion);
		if (entrando || cerroVictoria) { juego.enfocarEntrada(); }
	}

	private void jugarDeNuevo() {
		if (modoActual == ModoJuego.MULTIJUGADOR) {
			publicar(TipoEvento.VOLVER_SALA, null);
			return;
		}
		publicar(TipoEvento.INICIAR, configLocal);
		juego.enfocarEntrada();
	}

	private void manejarErrorSinTablero(String mensaje) {
		if (modoActual == ModoJuego.MULTIJUGADOR) {
			victoria.ocultarSuperposicion();
			abandonar();
			multijugador.error(mensaje);
			mostrarPantalla(MULTIJUGADOR);
			return;
		}
		menu();
		configuracion.error(mensaje);
	}

	private void publicar(TipoEvento tipo, Object payload) {
		EventBus bus = busActual;
		if (bus != null) {
			bus.publicar(payload == null ? new Evento(tipo) : new Evento(tipo, payload));
		}
	}

	private void menu() {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(false);
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		mostrarPantalla(CONFIGURACION);
	}

	private void salirDeSala() {
		abandonar();
		multijugador.ocupado(false);
		multijugador.estado(" ");
		mostrarPantalla(MULTIJUGADOR);
	}

	private void mostrarPantalla(String pantalla) {
		if (!pantalla.equals(pantallaActual)) {
			tarjetas.show(contenido, pantalla);
			pantallaActual = pantalla;
		}
	}

	private void abandonar() {
		intentoConexion.incrementAndGet();
		MotorJuego motorCerrar;
		synchronized (lockConexion) {
			motorCerrar = motorActual;
			motorActual = null;
			busActual = null;
		}
		if (motorCerrar != null) { motorCerrar.finalizar(); }
	}

	private boolean actualizarVictoria(ActualizacionVista act) {
		if (!act.partidaFinalizada()) {
			boolean estabaVisible = victoria.isVisible();
			victoria.ocultarSuperposicion();
			return estabaVisible;
		}
		String titulo = act.ganoLocal() ? "¡VICTORIA!" : "Ganó " + act.nombreGanador();
		victoria.mostrarFinPartida(titulo, detalleFinal(act));
		return false;
	}

	private static String detalleFinal(ActualizacionVista act) {
		String puntajes = act.marcador().stream().map(VentanaJuego::textoPuntaje)
				.collect(Collectors.joining(SEPARADOR_PUNTAJES));
		return "<html><center>Partida a " + act.rondasObjetivo() + "<br>" + puntajes
				+ "</center></html>";
	}

	private static String textoPuntaje(PuntajeJugador puntaje) {
		String nombre = puntaje.nombre().replace("&", "&amp;").replace("<", "&lt;");
		return nombre + " " + puntaje.rondas();
	}

	public void mostrar() { ventana.setVisible(true); }

	JFrame ventana() { return ventana; }

	@Override
	public void close() { abandonar(); ventana.dispose(); }
}
