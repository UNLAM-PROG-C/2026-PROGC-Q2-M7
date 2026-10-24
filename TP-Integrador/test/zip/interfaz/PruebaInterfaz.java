package zip.interfaz;

import static zip.SoportePruebas.verificar;
import static zip.SoportePruebas.esperarExcepcion;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import zip.dominio.Direccion;
import zip.dominio.Nivel;
import zip.infraestructura.SesionJuegoLocal;

public final class PruebaInterfaz {
	private static final int SEGUNDOS_ESPERA = 5;
	private static final int MILISEGUNDOS_CONSULTA = 10;
	private static final int DEMORA_CERO = 0;

	private PruebaInterfaz() {
	}

	public static void main(String[] argumentos) throws Exception {
		verificar(!GraphicsEnvironment.isHeadless(), "Las pruebas de UI requieren un escritorio; no se omiten");
		esperarExcepcion(IllegalStateException.class, () -> new VentanaJuego(escucha -> {
			throw new AssertionError("La fábrica no debe invocarse fuera del EDT");
		}));
		jugar(Controles.FLECHAS);
		jugar(Controles.WASD);
		esperarHasta(PruebaInterfaz::sinTrabajadores);
		System.out.println(
				"PruebaInterfaz: controles, movimientos inválidos, deshacer, reiniciar, victoria y cierre correctos");
	}

	private static Nivel nivel(int puntos) {
		verificar(puntos == Nivel.PUNTOS_MINIMOS, "La UI debe enviar la cantidad de puntos elegida");
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(Nivel.CANTIDAD_CELDAS, 0));
		numeros.set(0, 1);
		numeros.set(Nivel.CANTIDAD_CELDAS - Nivel.TAMANO, 2);
		return new Nivel(numeros);
	}

	private static Escenario abrir() throws Exception {
		return enEdt(() -> {
			VentanaJuego ventana = new VentanaJuego(escucha -> new SesionJuegoLocal(
					PruebaInterfaz::nivel, escucha, SwingUtilities::invokeLater));
			ventana.mostrar();
			verificar(ventana.ventana().isShowing(), "La ventana Swing real está visible");
			return new Escenario(ventana);
		});
	}

	private static void jugar(Controles controles) throws Exception {
		try (Escenario escenario = abrir()) {
			iniciar(escenario, controles);
			verificarEntrada(escenario, controles);
			capturar(escenario, "build/ui-jugando.png");
			completar(escenario, controles);
			capturar(escenario, "build/ui-victoria.png");
			reiniciarYVolver(escenario);
		}
	}

	private static void iniciar(Escenario escenario, Controles controles) throws Exception {
		enEdt(() -> {
			((JComboBox<?>) escenario.buscar("controles")).setSelectedItem(controles);
			JSpinner puntos = (JSpinner) escenario.buscar("puntos");
			puntos.setValue(Nivel.PUNTOS_MINIMOS);
			escenario.hacerClic("iniciar");
			return null;
		});
		esperarCantidad(escenario, 1);
		esperarHasta(() -> escenario.ventana().ventana().getFocusOwner() != null);
	}

	private static void verificarEntrada(Escenario escenario, Controles controles) throws Exception {
		presionar(escenario, controles.tecla(Direccion.ARRIBA));
		esperarTexto(escenario, "estadoMensaje", "tablero");
		presionar(escenario, controles.tecla(Direccion.DERECHA));
		esperarCantidad(escenario, 2);
		presionar(escenario, KeyEvent.VK_BACK_SPACE);
		esperarCantidad(escenario, 1);
		presionar(escenario, controles.tecla(Direccion.DERECHA));
		esperarCantidad(escenario, 2);
		enEdt(() -> { escenario.hacerClic("reiniciar"); return null; });
		esperarCantidad(escenario, 1);
		verificarTeclasInactivas(escenario, controles);
	}

	private static void verificarTeclasInactivas(Escenario escenario, Controles controles) throws Exception {
		enEdt(() -> {
			JComponent panel = (JComponent) escenario.buscar("juego");
			int inactiva = controles == Controles.FLECHAS ? KeyEvent.VK_W : KeyEvent.VK_UP;
			verificar(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.get(KeyStroke.getKeyStroke(inactiva, 0)) == null, "Los controles inactivos se eliminan");
			((JButton) escenario.buscar("reiniciar")).requestFocusInWindow();
			return null;
		});
	}

	private static void completar(Escenario escenario, Controles controles) throws Exception {
		int visitadas = 1;
		for (int fila = 0; fila < Nivel.TAMANO; fila++) {
			Direccion direccion = fila % 2 == 0 ? Direccion.DERECHA : Direccion.IZQUIERDA;
			for (int columna = 1; columna < Nivel.TAMANO; columna++) {
				presionar(escenario, controles.tecla(direccion));
				esperarCantidad(escenario, ++visitadas);
			}
			if (fila < Nivel.TAMANO - 1) {
				presionar(escenario, controles.tecla(Direccion.ABAJO));
				esperarCantidad(escenario, ++visitadas);
			}
		}
		esperarTexto(escenario, "victoria", "100%");
	}

	private static void reiniciarYVolver(Escenario escenario) throws Exception {
		enEdt(() -> {
			verificar(!escenario.buscar("deshacer").isEnabled(), "Deshacer queda deshabilitado tras la victoria");
			escenario.hacerClic("reiniciar");
			return null;
		});
		esperarCantidad(escenario, 1);
		enEdt(() -> { escenario.hacerClic("volver"); return null; });
		esperarHasta(() -> escenario.visible("iniciar"));
	}

	private static void presionar(Escenario escenario, int tecla) throws Exception {
		enEdt(() -> {
			JComponent panel = (JComponent) escenario.buscar("juego");
			Object vinculo = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.get(KeyStroke.getKeyStroke(tecla, 0));
			verificar(vinculo != null, "La tecla debe tener un vínculo activo a nivel de ventana");
			enviarTecla(escenario, tecla);
			return null;
		});
	}

	private static void enviarTecla(Escenario escenario, int tecla) {
		KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		Component destino = escenario.ventana().ventana().getFocusOwner();
		verificar(destino != null, "La ventana de prueba debe tener el foco del teclado");
		focus.redispatchEvent(destino, new KeyEvent(destino, KeyEvent.KEY_PRESSED,
				System.currentTimeMillis(), 0, tecla, KeyEvent.CHAR_UNDEFINED));
	}

	private static void esperarCantidad(Escenario escenario, int cantidad) throws Exception {
		String esperado = "Celdas recorridas: " + cantidad + "/" + Nivel.CANTIDAD_CELDAS;
		esperarHasta(() -> escenario.texto("progreso").equals(esperado));
	}

	private static void esperarTexto(Escenario escenario, String nombre, String texto) throws Exception {
		esperarHasta(() -> escenario.texto(nombre).contains(texto));
	}

	private static void esperarHasta(BooleanSupplier condicion) throws Exception {
		long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(SEGUNDOS_ESPERA);
		while (!enEdt(condicion::getAsBoolean)) {
			verificar(System.nanoTime() < limite, "Se agotó la espera del estado de la UI o del trabajador");
			Thread.sleep(MILISEGUNDOS_CONSULTA);
		}
	}

	private static void capturar(Escenario escenario, String recorrido) throws Exception {
		BufferedImage imagen = enEdt(() -> renderizar(escenario.ventana().ventana().getContentPane()));
		verificar(ImageIO.write(imagen, "png", Path.of(recorrido).toFile()), "El escritor PNG está disponible");
	}

	private static BufferedImage renderizar(Container contenido) {
		BufferedImage imagen = new BufferedImage(contenido.getWidth(), contenido.getHeight(),
				BufferedImage.TYPE_INT_RGB);
		Graphics2D graficos = imagen.createGraphics();
		try {
			contenido.printAll(graficos);
		} finally {
			graficos.dispose();
		}
		return imagen;
	}

	private static <T> T enEdt(Callable<T> accion) throws Exception {
		FutureTask<T> tarea = new FutureTask<>(accion);
		SwingUtilities.invokeAndWait(tarea);
		return tarea.get();
	}

	private static boolean sinTrabajadores() {
		return Thread.getAllStackTraces().keySet().stream()
				.noneMatch(hilo -> hilo.isAlive() && hilo.getName().equals("zip-sesion"));
	}

	private static Component buscar(Container contenedor, String nombre) {
		for (Component hijo : contenedor.getComponents()) {
			if (nombre.equals(hijo.getName())) {
				return hijo;
			}
			if (hijo instanceof Container descendiente && contiene(descendiente, nombre)) {
				return buscar(descendiente, nombre);
			}
		}
		throw new AssertionError("Falta el componente de UI: " + nombre);
	}

	private static boolean contiene(Container contenedor, String nombre) {
		for (Component hijo : contenedor.getComponents()) {
			if (nombre.equals(hijo.getName())
					|| (hijo instanceof Container descendiente && contiene(descendiente, nombre))) {
				return true;
			}
		}
		return false;
	}

	private record Escenario(VentanaJuego ventana) implements AutoCloseable {
		Component buscar(String nombre) {
			return PruebaInterfaz.buscar(ventana.ventana().getContentPane(), nombre);
		}

		void hacerClic(String nombre) {
			((JButton) buscar(nombre)).doClick(DEMORA_CERO);
		}

		String texto(String nombre) {
			return ((JLabel) buscar(nombre)).getText();
		}

		boolean visible(String nombre) {
			return buscar(nombre).isShowing();
		}

		@Override
		public void close() {
			try {
				enEdt(() -> {
					ventana.ventana().dispatchEvent(new WindowEvent(ventana.ventana(), WindowEvent.WINDOW_CLOSING));
					verificar(!ventana.ventana().isDisplayable(), "La ventana debe liberarse al cerrar");
					return null;
				});
			} catch (Exception excepcion) {
				throw new AssertionError("No se pudo cerrar la ventana de prueba", excepcion);
			}
		}
	}
}
