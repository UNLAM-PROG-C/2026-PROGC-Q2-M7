package ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import controller.ActualizacionVista;
import controller.MotorJuego;
import events.EventBus;
import events.Evento;
import events.TipoEvento;
import model.Dificultad;
import utils.Constantes;

public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final int PUERTO_MAXIMO = 65535;

	private final AtomicLong intentoConexion = new AtomicLong();
	private final Object lockConexion = new Object();
	private volatile MotorJuego motorActual;
	private volatile EventBus busActual;
	private volatile Dificultad dificultadActual = Dificultad.MEDIA;
	private String pantallaActual = CONFIGURACION;

	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion =
			new PanelConfiguracion(this::ejecutarDesdeMenu, this::cancelarEspera);
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

	private void ejecutarDesdeMenu() {
		int rol = configuracion.rolSeleccionado();
		if (rol == Constantes.ROL_HOST) {
			iniciarHost();
		} else if (rol == Constantes.ROL_CLIENTE) {
			iniciarCliente();
		} else {
			iniciarLocal();
		}
	}

	private void iniciarLocal() {
		long intento = prepararPartida(configuracion.dificultadSeleccionada());
		configuracion.ocupado(true, "Generando un tablero con solución única...");
		arrancarMotor(Constantes.ROL_LOCAL, Constantes.IP_LOCAL, Constantes.PUERTO,
				configuracion.cantidadRondas(), 1, intento, "No se pudo iniciar el juego: ");
	}

	private void iniciarHost() {
		int jugadores = configuracion.cantidadJugadores();
		int rivales = jugadores - 1;
		String textoRivales = rivales == 1 ? "1 rival" : rivales + " rivales";
		iniciarEnRed(Constantes.ROL_HOST, Constantes.IP_LOCAL, Constantes.PUERTO, jugadores,
				"Esperando " + textoRivales + " en puerto " + Constantes.PUERTO
						+ " (Rondas: " + configuracion.cantidadRondas() + ")...");
	}

	private void iniciarCliente() {
		String entrada = configuracion.direccionHost();
		if (entrada.isEmpty()) {
			configuracion.error("Ingresá la dirección del Host (IP o IP:puerto).");
			return;
		}
		conectarCliente(entrada);
	}

	private void conectarCliente(String entrada) {
		String limpio = entrada.trim().replaceFirst("^tcp://", "");
		int sep = limpio.indexOf(':');
		String rawHost = sep < 0 ? limpio : limpio.substring(0, sep).trim();
		String host = rawHost.isEmpty() ? Constantes.IP_LOCAL : rawHost;
		int puerto = sep < 0 ? Constantes.PUERTO : leerPuerto(limpio.substring(sep + 1).trim());
		if (puerto > 0) {
			iniciarEnRed(Constantes.ROL_CLIENTE, host, puerto, Constantes.JUGADORES_RED_POR_DEFECTO,
					"Conectando a " + host + ":" + puerto + "...");
		}
	}

	private int leerPuerto(String texto) {
		if (!texto.isEmpty() && texto.chars().allMatch(Character::isDigit)) {
			long valor = Long.parseLong(texto);
			if (valor >= 1 && valor <= PUERTO_MAXIMO) {
				return (int) valor;
			}
		}
		configuracion.error("Puerto inválido: " + texto);
		return -1;
	}

	private void iniciarEnRed(int rol, String host, int puerto, int jugadores, String estadoInicial) {
		long intento = prepararPartida(configuracion.dificultadSeleccionada());
		int rondas = configuracion.cantidadRondas();
		configuracion.ocupado(true, estadoInicial);
		boolean esHost = rol == Constantes.ROL_HOST;
		String prefijo = esHost ? "Espera cancelada o error: " : "Error al conectar con host: ";
		Thread hilo = new Thread(() -> arrancarMotor(rol, host, puerto, rondas, jugadores, intento, prefijo),
				esHost ? "Thread-Conexion-Host" : "Thread-Conexion-Cliente");
		hilo.setDaemon(true);
		hilo.start();
	}

	private void cancelarEspera() {
		abandonar();
		configuracion.ocupado(false, "");
	}

	private long prepararPartida(Dificultad dificultad) {
		victoria.ocultarSuperposicion();
		abandonar();
		this.dificultadActual = dificultad;
		return intentoConexion.get();
	}

	private void arrancarMotor(int rol, String host, int puerto, int rondas, int jugadores,
			long intento, String prefijoError) {
		EventBus bus = new EventBus();
		MotorJuego motor = new MotorJuego(rol, host, puerto, rondas, jugadores, bus,
				act -> recibir(intento, act));
		if (!vincularMotor(bus, motor, intento)) {
			motor.finalizar();
			return;
		}
		conectarEIniciar(rol, bus, motor, intento, prefijoError);
	}

	private boolean vincularMotor(EventBus bus, MotorJuego motor, long intento) {
		synchronized (lockConexion) {
			if (intento != intentoConexion.get()) {
				return false;
			}
			this.busActual = bus;
			this.motorActual = motor;
			return true;
		}
	}

	private void conectarEIniciar(int rol, EventBus bus, MotorJuego motor, long intento, String prefijoError) {
		try {
			motor.conectar();
			activarMotorConectado(rol, bus, motor, intento);
		} catch (IOException e) {
			SwingUtilities.invokeLater(() -> {
				if (intento == intentoConexion.get()) {
					configuracion.error(prefijoError + e.getMessage());
				}
			});
		}
	}

	private void activarMotorConectado(int rol, EventBus bus, MotorJuego motor, long intento) {
		synchronized (lockConexion) {
			if (intento != intentoConexion.get()) {
				return;
			}
			motor.iniciar();
			if (rol != Constantes.ROL_CLIENTE) {
				bus.publicar(new Evento(TipoEvento.INICIAR, dificultadActual));
			} else {
				SwingUtilities.invokeLater(() -> configuracion.estado("Conectado. Esperando inicio del host..."));
			}
		}
	}

	private void recibir(long intento, ActualizacionVista act) {
		if (intento != intentoConexion.get()) { return; }
		if (act.estadoJuego() == null) {
			menu();
			configuracion.error(act.mensaje());
		} else {
			mostrarEnJuego(act);
		}
	}

	private void mostrarEnJuego(ActualizacionVista actualizacion) {
		boolean entrando = !JUEGO.equals(pantallaActual);
		if (entrando) {
			configuracion.ocupado(false, "");
			mostrarPantalla(JUEGO);
		}
		juego.mostrar(actualizacion);
		boolean cerroVictoria = actualizarVictoria(actualizacion);
		if (entrando || cerroVictoria) { juego.enfocarEntrada(); }
	}

	private void jugarDeNuevo() {
		publicar(TipoEvento.INICIAR, dificultadActual);
		juego.enfocarEntrada();
	}

	private void publicar(TipoEvento tipo, Object payload) {
		EventBus bus = busActual;
		if (bus != null) { bus.publicar(payload == null ? new Evento(tipo) : new Evento(tipo, payload)); }
	}

	private void menu() {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(false, "");
		mostrarPantalla(CONFIGURACION);
	}

	private void mostrarPantalla(String pantalla) {
		if (!pantalla.equals(pantallaActual)) { tarjetas.show(contenido, pantalla); pantallaActual = pantalla; }
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
		String titulo = act.ganoLocal() ? "¡VICTORIA!" : "¡DERROTA!";
		String marcador = "(" + act.rondasLocal() + " a " + act.rondasRival() + ")";
		victoria.mostrarFinPartida(titulo, act.mensaje() + " " + marcador);
		return false;
	}

	public void mostrar() { ventana.setVisible(true); }

	JFrame ventana() { return ventana; }

	@Override
	public void close() { abandonar(); ventana.dispose(); }
}
