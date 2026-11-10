package ui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
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
import model.EstadoJuego;
import model.ModoJuego;
import network.GestorRed;
import utils.Constantes;

public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final String PROXIMAMENTE = "proximamente";

	private MotorJuego motorActual;
	private EventBus busActual;
	private ModoJuego modoActual = ModoJuego.UN_JUGADOR;
	private ModoJuego modoAnterior = ModoJuego.UN_JUGADOR;
	private Dificultad dificultadActual = Dificultad.MEDIA;

	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion = new PanelConfiguracion(this::iniciar, this::seleccionarModo);
	private final PanelProximamente proximamente = new PanelProximamente(this::iniciarHost, this::iniciarCliente, this::restaurarMenu);
	private final PanelJuego juego = new PanelJuego(
			direccion -> publicarEvento(new Evento(TipoEvento.MOVIMIENTO, direccion)),
			() -> publicarEvento(new Evento(TipoEvento.DESHACER)),
			() -> publicarEvento(new Evento(TipoEvento.REINICIAR)),
			this::menu);
	private final CapaVictoria victoria = new CapaVictoria(
			() -> publicarEvento(new Evento(TipoEvento.INICIAR, dificultadActual)),
			this::menu);

	public VentanaJuego() {
		contenido.add(configuracion.componente(), CONFIGURACION);
		contenido.add(proximamente.componente(), PROXIMAMENTE);
		contenido.add(juego.componente(), JUEGO);
		configurarVentana();
	}

	private static JFrame crearVentana() {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("La ventana debe crearse en el EDT.");
		}
		return new JFrame("Zip");
	}

	private void configurarVentana() {
		ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ventana.setContentPane(contenido);
		ventana.setGlassPane(victoria);
		ventana.setMinimumSize(new Dimension(Tema.ANCHO_MINIMO_VENTANA, Tema.ALTO_MINIMO_VENTANA));
		ventana.setPreferredSize(new Dimension(Tema.ANCHO_VENTANA, Tema.ALTO_VENTANA));
		ventana.pack();
		ventana.setLocationRelativeTo(null);
		ventana.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evento) {
				close();
			}
		});
	}

	private void iniciar(Controles controles, Dificultad dificultad) {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(true);
		this.dificultadActual = dificultad;
		juego.controles(controles);

		try {
			EventBus bus = new EventBus();
			this.busActual = bus;
			int rondas = configuracion.rondasSeleccionadas();
			MotorJuego motor = new MotorJuego(Constantes.ROL_LOCAL, Constantes.IP_LOCAL, Constantes.PUERTO, rondas, bus, this::recibir);
			this.motorActual = motor;
			motor.iniciar();
			bus.publicar(new Evento(TipoEvento.INICIAR, dificultad));
		} catch (IOException e) {
			configuracion.error("No se pudo iniciar el juego: " + e.getMessage());
		}
	}

	private void iniciarHost() {
		victoria.ocultarSuperposicion();
		abandonar();
		this.dificultadActual = configuracion.dificultadSeleccionada();
		int rondas = configuracion.rondasSeleccionadas();
		juego.controles(configuracion.controlesSeleccionados());
		proximamente.ocupado(true);
		proximamente.mostrarEstado("Esperando rival en puerto " + Constantes.PUERTO + " (Rondas: " + rondas + ")...");

		new Thread(() -> {
			try {
				EventBus bus = new EventBus();
				MotorJuego motor = new MotorJuego(Constantes.ROL_HOST, Constantes.IP_LOCAL, Constantes.PUERTO, rondas, bus, this::recibir);
				this.busActual = bus;
				this.motorActual = motor;
				motor.iniciar();
				bus.publicar(new Evento(TipoEvento.INICIAR, dificultadActual));
			} catch (IOException e) {
				SwingUtilities.invokeLater(() -> {
					proximamente.ocupado(false);
					proximamente.mostrarError("Espera cancelada o error: " + e.getMessage());
				});
			}
		}, "Thread-Conexion-Host").start();
	}

	private void iniciarCliente() {
		String defecto = Constantes.IP_LOCAL + ":" + Constantes.PUERTO;
		String entrada = JOptionPane.showInputDialog(
				ventana,
				"Ingresá la dirección del Host (IP o IP:puerto):",
				defecto);
		if (entrada == null || entrada.trim().isEmpty()) {
			return;
		}
		entrada = entrada.trim();
		if (entrada.startsWith("tcp://")) {
			entrada = entrada.substring("tcp://".length());
		}

		String host = Constantes.IP_LOCAL;
		int puerto = Constantes.PUERTO;
		if (entrada.contains(":")) {
			String[] partes = entrada.split(":", 2);
			host = partes[0].trim();
			try {
				puerto = Integer.parseInt(partes[1].trim());
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(ventana, "Puerto inválido: " + partes[1], "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
		} else {
			host = entrada;
		}

		final String destinoHost = host;
		final int destinoPuerto = puerto;

		victoria.ocultarSuperposicion();
		abandonar();
		this.dificultadActual = configuracion.dificultadSeleccionada();
		int rondas = configuracion.rondasSeleccionadas();
		juego.controles(configuracion.controlesSeleccionados());
		proximamente.ocupado(true);
		proximamente.mostrarEstado("Conectando a " + destinoHost + ":" + destinoPuerto + "...");

		new Thread(() -> {
			try {
				EventBus bus = new EventBus();
				MotorJuego motor = new MotorJuego(Constantes.ROL_CLIENTE, destinoHost, destinoPuerto, rondas, bus, this::recibir);
				this.busActual = bus;
				this.motorActual = motor;
				motor.iniciar();
				bus.publicar(new Evento(TipoEvento.INICIAR, dificultadActual));
			} catch (IOException e) {
				SwingUtilities.invokeLater(() -> {
					proximamente.ocupado(false);
					proximamente.mostrarError("Error al conectar con host: " + e.getMessage());
				});
			}
		}, "Thread-Conexion-Cliente").start();
	}

	private void seleccionarModo(ModoJuego modo) {
		if (modo == ModoJuego.UN_JUGADOR) {
			modoActual = modo;
			return;
		}
		modoAnterior = modoActual;
		proximamente.resetear();
		tarjetas.show(contenido, PROXIMAMENTE);
	}

	private void restaurarMenu() {
		abandonar();
		modoActual = modoAnterior;
		configuracion.seleccionarModo(modoActual);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void recibir(ActualizacionVista actualizacion) {
		if (actualizacion.estadoJuego() == null) {
			menu();
			configuracion.error(actualizacion.mensaje());
			return;
		}
		configuracion.ocupado(false);
		proximamente.ocupado(false);
		juego.mostrar(actualizacion);
		actualizarVictoria(actualizacion);
		boolean primeraVisualizacion = !juego.componente().isShowing();
		tarjetas.show(contenido, JUEGO);
		if (primeraVisualizacion) {
			juego.enfocarEntrada();
		}
	}

	private void publicarEvento(Evento evento) {
		if (busActual != null) {
			busActual.publicar(evento);
		}
	}

	private void menu() {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(false);
		proximamente.resetear();
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void abandonar() {
		GestorRed.cancelarEsperaServidor();
		if (motorActual != null) {
			motorActual.finalizar();
			motorActual = null;
		}
		busActual = null;
	}

	private void actualizarVictoria(ActualizacionVista act) {
		if (act.partidaFinalizada()) {
			if (act.ganoLocal()) {
				victoria.mostrarFinPartida("¡VICTORIA!",
						"Ganaste la partida final (" + act.rondasLocal() + " a " + act.rondasRival() + ")");
			} else {
				victoria.mostrarFinPartida("¡DERROTA!",
						"El rival ganó la partida final (" + act.rondasLocal() + " a " + act.rondasRival() + ")");
			}
		} else {
			victoria.ocultarSuperposicion();
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
}
