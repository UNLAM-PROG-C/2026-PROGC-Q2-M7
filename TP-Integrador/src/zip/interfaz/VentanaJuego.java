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
import zip.aplicacion.FabricaSesion;
import zip.aplicacion.ActualizacionSesion;

public final class VentanaJuego implements AutoCloseable {
	private static final String CONFIGURACION = "configuracion";
	private static final String JUEGO = "juego";
	private static final int ANCHO = 720;
	private static final int ALTO = 800;
	private final JFrame ventana = crearVentana();
	private final CardLayout tarjetas = new CardLayout();
	private final JPanel contenido = new JPanel(tarjetas);
	private final PanelConfiguracion configuracion = new PanelConfiguracion(this::iniciar);
	private final PanelJuego juego = new PanelJuego(direccion -> enviar(sesion -> sesion.mover(direccion)),
			() -> enviar(SesionJuego::deshacer), () -> enviar(SesionJuego::reiniciar), this::menu);
	private final FabricaSesion sesiones;
	private Optional<SesionJuego> sesion = Optional.empty();

	public VentanaJuego(FabricaSesion sesiones) {
		this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
		contenido.add(configuracion.componente(), CONFIGURACION);
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
		ventana.setMinimumSize(new Dimension(ANCHO, ALTO));
		ventana.setSize(ANCHO, ALTO);
		ventana.setLocationRelativeTo(null);
		ventana.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evento) {
				close();
			}
		});
	}

	private void iniciar(Controles controles, int puntos) {
		abandonar();
		configuracion.ocupado(true);
		juego.controles(controles);
		SesionJuego siguiente = sesiones.crear(this::recibir);
		sesion = Optional.of(siguiente);
		if (!siguiente.iniciar(puntos)) {
			configuracion.error("No se pudo iniciar la sesión. Intentá nuevamente.");
		}
	}

	private void recibir(ActualizacionSesion actualizacion) {
		if (actualizacion.estadoJuego().isEmpty()) {
			menu();
			configuracion.error(actualizacion.mensaje());
			return;
		}
		configuracion.ocupado(false);
		juego.mostrar(actualizacion.estadoJuego().orElseThrow(), actualizacion.mensaje());
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
		abandonar();
		configuracion.ocupado(false);
		tarjetas.show(contenido, CONFIGURACION);
	}

	private void abandonar() {
		sesion.ifPresent(SesionJuego::close);
		sesion = Optional.empty();
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
