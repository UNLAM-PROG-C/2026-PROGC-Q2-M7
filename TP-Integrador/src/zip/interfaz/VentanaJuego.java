package zip.interfaz;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import zip.aplicacion.SesionJuego;
import zip.aplicacion.ModoJuego;
import zip.aplicacion.FabricaSesion;
import zip.aplicacion.ActualizacionSesion;
import zip.dominio.Dificultad;

public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final String PROXIMAMENTE = "proximamente";
	private static final boolean MULTIJUGADOR_HABILITADO = false;
	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion = new PanelConfiguracion(this::iniciar, this::seleccionarModo);
	private final PanelProximamente proximamente = new PanelProximamente(this::jugarSolo, this::restaurarMenu);
	private final PanelJuego juego = new PanelJuego(direccion -> enviar(sesion -> sesion.mover(direccion)),
			() -> enviar(SesionJuego::deshacer), () -> enviar(SesionJuego::reiniciar), this::menu);
	private final CapaVictoria victoria = new CapaVictoria(
			() -> enviar(SesionJuego::reiniciar), this::menu);
	private final FabricaSesion sesiones;
	private Optional<SesionJuego> sesion = Optional.empty();
	private ModoJuego modoActual = ModoJuego.UN_JUGADOR;
	private ModoJuego modoAnterior = ModoJuego.UN_JUGADOR;

	public VentanaJuego(FabricaSesion sesiones) {
		this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
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
		juego.controles(controles);
		SesionJuego siguiente = sesiones.crear(this::recibir);
		sesion = Optional.of(siguiente);
		if (!siguiente.iniciar(dificultad)) {
			configuracion.error("No se pudo iniciar la sesión. Intentá nuevamente.");
		}
	}

	private void seleccionarModo(ModoJuego modo) {
		if (modo == ModoJuego.UN_JUGADOR) {
			modoActual = modo;
			return;
		}
		modoAnterior = modoActual;
		if (MULTIJUGADOR_HABILITADO) {
			iniciarMultijugador();
			return;
		}
		tarjetas.show(contenido, PROXIMAMENTE);
	}

	private void iniciarMultijugador() {
		throw new IllegalStateException("El flujo multijugador todavía no está configurado.");
	}

	private void jugarSolo() {
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void restaurarMenu() {
		modoActual = modoAnterior;
		configuracion.seleccionarModo(modoActual);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void recibir(ActualizacionSesion actualizacion) {
		if (actualizacion.estadoJuego().isEmpty()) {
			menu();
			configuracion.error(actualizacion.mensaje());
			return;
		}
		configuracion.ocupado(false);
		juego.mostrar(actualizacion.estadoJuego().orElseThrow(), actualizacion.mensaje());
		actualizarVictoria(actualizacion.estadoJuego().orElseThrow());
		boolean primeraVisualizacion = !juego.componente().isShowing();
		tarjetas.show(contenido, JUEGO);
		if (primeraVisualizacion) {
			juego.enfocarEntrada();
		}
	}

	private void enviar(Predicate<SesionJuego> accion) {
		if (sesion.isEmpty() || !accion.test(sesion.orElseThrow())) {
			juego.mensaje("Acción no aceptada: sesión ocupada o cerrada. Intentá nuevamente.");
		}
	}

	private void menu() {
		victoria.ocultarSuperposicion();
		abandonar();
		configuracion.ocupado(false);
		modoActual = ModoJuego.UN_JUGADOR;
		configuracion.seleccionarModo(modoActual);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void abandonar() {
		sesion.ifPresent(SesionJuego::close);
		sesion = Optional.empty();
	}

	private void actualizarVictoria(zip.dominio.EstadoJuego estadoJuego) {
		if (estadoJuego.gano()) {
			victoria.mostrarVictoria(estadoJuego);
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
