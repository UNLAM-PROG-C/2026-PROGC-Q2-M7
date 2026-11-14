package ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import controller.ActualizacionVista;
import controller.MotorJuego;
import events.EventBus;
import events.Evento;
import events.TipoEvento;
import model.Dificultad;
import model.ModoJuego;
import network.GestorRed;
import utils.Constantes;

public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final String MULTIJUGADOR = "multijugador";
	private static final int PUERTO_MAXIMO = 65535;

	private final AtomicLong intentoConexion = new AtomicLong();
	private final Object lockConexion = new Object();
	private volatile MotorJuego motorActual;
	private volatile EventBus busActual;
	private volatile Dificultad dificultadActual = Dificultad.MEDIA;
	private ModoJuego modoActual = ModoJuego.UN_JUGADOR;
	private String pantallaActual = CONFIGURACION;

	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion = new PanelConfiguracion(
			this::iniciar, this::seleccionarModo);
	private final PanelMultijugador multijugador = new PanelMultijugador(
			this::iniciarHost, this::iniciarCliente, this::restaurarMenu);
	private final PanelJuego juego = crearPanelJuego();
	private final CapaVictoria victoria = new CapaVictoria(
			() -> publicarEvento(new Evento(TipoEvento.INICIAR, dificultadActual)),
			this::menu);

	public VentanaJuego() {
		contenido.add(configuracion, CONFIGURACION);
		contenido.add(multijugador, MULTIJUGADOR);
		contenido.add(juego, JUEGO);
		configurarVentana();
	}

	private PanelJuego crearPanelJuego() {
		return new PanelJuego(
				direccion -> publicarEvento(new Evento(TipoEvento.MOVIMIENTO, direccion)),
				() -> publicarEvento(new Evento(TipoEvento.DESHACER)),
				() -> publicarEvento(new Evento(TipoEvento.REINICIAR)),
				this::menu);
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
		agregarCierreVentana();
	}

	private void agregarCierreVentana() {
		ventana.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evento) {
				close();
			}
		});
	}

	private void iniciar(Controles controles, Dificultad dificultad) {
		long intento = prepararPartida(ModoJuego.UN_JUGADOR, dificultad, controles);
		configuracion.ocupado(true);
		arrancarMotor(Constantes.ROL_LOCAL, Constantes.IP_LOCAL, Constantes.PUERTO,
				configuracion.cantidadRondas(), intento, "No se pudo iniciar el juego: ");
	}

	private void iniciarHost() {
		int rondas = configuracion.cantidadRondas();
		String estado = "Esperando rival en puerto " + Constantes.PUERTO
				+ " (Rondas: " + rondas + ")...";
		iniciarEnRed(Constantes.ROL_HOST, Constantes.IP_LOCAL, Constantes.PUERTO,
				estado, "Thread-Conexion-Host", "Espera cancelada o error: ");
	}

	private void iniciarCliente() {
		DestinoRed destino = solicitarDestinoCliente();
		if (destino == null) {
			return;
		}
		String estado = "Conectando a " + destino.host() + ":" + destino.puerto() + "...";
		iniciarEnRed(Constantes.ROL_CLIENTE, destino.host(), destino.puerto(),
				estado, "Thread-Conexion-Cliente", "Error al conectar con host: ");
	}

	private DestinoRed solicitarDestinoCliente() {
		String defecto = Constantes.IP_LOCAL + ":" + Constantes.PUERTO;
		String entrada = JOptionPane.showInputDialog(ventana,
				"Ingresá la dirección del Host (IP o IP:puerto):", defecto);
		if (entrada == null || entrada.trim().isEmpty()) {
			return null;
		}
		return parsearDestino(entrada.trim());
	}

	private DestinoRed parsearDestino(String entrada) {
		String limpio = entrada.startsWith("tcp://")
				? entrada.substring("tcp://".length()) : entrada;
		if (!limpio.contains(":")) {
			return new DestinoRed(limpio, Constantes.PUERTO);
		}
		String[] partes = limpio.split(":", 2);
		String host = partes[0].trim().isEmpty() ? Constantes.IP_LOCAL : partes[0].trim();
		return parsearPuerto(host, partes[1].trim());
	}

	private DestinoRed parsearPuerto(String host, String textoPuerto) {
		try {
			int puerto = Integer.parseInt(textoPuerto);
			if (puerto >= 1 && puerto <= PUERTO_MAXIMO) {
				return new DestinoRed(host, puerto);
			}
		} catch (NumberFormatException ignored) {
		}
		JOptionPane.showMessageDialog(ventana, "Puerto inválido: " + textoPuerto,
				"Error", JOptionPane.ERROR_MESSAGE);
		return null;
	}

	private void iniciarEnRed(int rol, String host, int puerto, String estadoInicial,
			String nombreHilo, String prefijoError) {
		long intento = prepararPartida(ModoJuego.MULTIJUGADOR,
				configuracion.dificultadSeleccionada(), configuracion.controlesSeleccionados());
		int rondas = configuracion.cantidadRondas();
		multijugador.ocupado(true);
		multijugador.estado(estadoInicial);
		new Thread(() -> arrancarMotor(rol, host, puerto, rondas, intento, prefijoError),
				nombreHilo).start();
	}

	private long prepararPartida(ModoJuego modo, Dificultad dificultad, Controles controles) {
		victoria.ocultarSuperposicion();
		abandonar();
		this.modoActual = modo;
		this.dificultadActual = dificultad;
		juego.controles(controles);
		return intentoConexion.get();
	}

	private void arrancarMotor(int rol, String host, int puerto, int rondas,
			long intento, String prefijoError) {
		try {
			EventBus bus = new EventBus();
			MotorJuego motor = new MotorJuego(rol, host, puerto, rondas, bus,
					actualizacion -> recibir(intento, actualizacion));
			registrarMotor(bus, motor, intento);
		} catch (IOException e) {
			reportarErrorConexion(intento, rol, prefijoError + e.getMessage());
		}
	}

	private void registrarMotor(EventBus bus, MotorJuego motor, long intento) {
		boolean aceptado = false;
		synchronized (lockConexion) {
			if (intento == intentoConexion.get()) {
				this.busActual = bus;
				this.motorActual = motor;
				motor.iniciar();
				bus.publicar(new Evento(TipoEvento.INICIAR, dificultadActual));
				aceptado = true;
			}
		}
		if (!aceptado) {
			motor.finalizar();
		}
	}

	private void reportarErrorConexion(long intento, int rol, String mensaje) {
		SwingUtilities.invokeLater(() -> {
			if (intento != intentoConexion.get()) {
				return;
			}
			if (rol == Constantes.ROL_LOCAL) {
				configuracion.error(mensaje);
			} else {
				multijugador.ocupado(false);
				multijugador.error(mensaje);
			}
		});
	}

	private void seleccionarModo(ModoJuego modo) {
		this.modoActual = modo;
		if (modo == ModoJuego.UN_JUGADOR) {
			return;
		}
		resetearMultijugador();
		mostrarPantalla(MULTIJUGADOR);
	}

	private void restaurarMenu() {
		abandonar();
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		mostrarPantalla(CONFIGURACION);
	}

	private void recibir(long intento, ActualizacionVista actualizacion) {
		if (intento != intentoConexion.get()) {
			return;
		}
		if (actualizacion.estadoJuego() == null) {
			manejarErrorSinTablero(actualizacion.mensaje());
			return;
		}
		mostrarEnJuego(actualizacion);
	}

	private void mostrarEnJuego(ActualizacionVista actualizacion) {
		boolean entrandoAlJuego = !JUEGO.equals(pantallaActual);
		if (entrandoAlJuego) {
			configuracion.ocupado(false);
			multijugador.ocupado(false);
			mostrarPantalla(JUEGO);
		}
		juego.mostrar(actualizacion);
		actualizarVictoria(actualizacion);
		if (entrandoAlJuego) {
			juego.enfocarEntrada();
		}
	}

	private void manejarErrorSinTablero(String mensaje) {
		if (modoActual == ModoJuego.MULTIJUGADOR) {
			victoria.ocultarSuperposicion();
			abandonar();
			multijugador.ocupado(false);
			multijugador.error(mensaje);
			mostrarPantalla(MULTIJUGADOR);
			return;
		}
		menu();
		configuracion.error(mensaje);
	}

	private void publicarEvento(Evento evento) {
		EventBus bus = busActual;
		if (bus != null) {
			bus.publicar(evento);
		}
	}

	private void menu() {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(false);
		resetearMultijugador();
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		mostrarPantalla(CONFIGURACION);
	}

	private void resetearMultijugador() {
		multijugador.ocupado(false);
		multijugador.estado(" ");
	}

	private void mostrarPantalla(String pantalla) {
		if (!pantalla.equals(pantallaActual)) {
			tarjetas.show(contenido, pantalla);
			pantallaActual = pantalla;
		}
	}

	private void abandonar() {
		intentoConexion.incrementAndGet();
		GestorRed.cancelarEsperaServidor();
		MotorJuego motorCerrar;
		synchronized (lockConexion) {
			motorCerrar = motorActual;
			motorActual = null;
			busActual = null;
		}
		if (motorCerrar != null) {
			motorCerrar.finalizar();
		}
	}

	private void actualizarVictoria(ActualizacionVista act) {
		if (!act.partidaFinalizada()) {
			victoria.ocultarSuperposicion();
			return;
		}
		if (act.ganoLocal()) {
			victoria.mostrarFinPartida("¡VICTORIA!",
					"Ganaste la partida final (" + act.rondasLocal() + " a " + act.rondasRival() + ")");
		} else {
			victoria.mostrarFinPartida("¡DERROTA!",
					"El rival ganó la partida final (" + act.rondasLocal() + " a " + act.rondasRival() + ")");
		}
	}

	public void mostrar() {
		ventana.setVisible(true);
	}

	JFrame ventana() {
		return ventana;
	}

	@Override
	public void close() {
		abandonar();
		ventana.dispose();
	}

	private record DestinoRed(String host, int puerto) {
	}
}
