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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import zip.dominio.Celda;
import zip.dominio.Dificultad;
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
		verificar(!GraphicsEnvironment.isHeadless(),
				"Las pruebas de interfaz requieren escritorio y no se omiten silenciosamente.");
		esperarExcepcion(IllegalStateException.class, () -> new VentanaJuego(escucha -> {
			throw new AssertionError("La fábrica no debe invocarse fuera del EDT.");
		}));
		navegacionMenu();
		jugar(Controles.FLECHAS, Dificultad.FACIL);
		jugar(Controles.WASD, Dificultad.MEDIA);
		jugar(Controles.FLECHAS, Dificultad.DIFICIL);
		esperarHasta(PruebaInterfaz::sinTrabajadores);
		System.out.println("PruebaInterfaz: menú, próximamente, 3 dificultades, "
				+ "ambos controles y cierre superados.");
	}

	private static Nivel nivel(Dificultad dificultad) {
		List<Celda> recorrido = serpiente(dificultad.tamano());
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(dificultad.cantidadCeldas(), 0));
		for (int punto = 0; punto < dificultad.puntos(); punto++) {
			int indice = punto * (dificultad.cantidadCeldas() - 1) / (dificultad.puntos() - 1);
			Celda celda = recorrido.get(indice);
			numeros.set(celda.fila() * dificultad.tamano() + celda.columna(), punto + 1);
		}
		return new Nivel(dificultad.tamano(), numeros);
	}

	private static List<Celda> serpiente(int tamano) {
		List<Celda> recorrido = new ArrayList<>();
		for (int fila = 0; fila < tamano; fila++) {
			for (int desplazamiento = 0; desplazamiento < tamano; desplazamiento++) {
				int columna = fila % 2 == 0 ? desplazamiento : tamano - 1 - desplazamiento;
				recorrido.add(new Celda(fila, columna));
			}
		}
		return recorrido;
	}

	private static Escenario abrir() throws Exception {
		return enEdt(() -> {
			AtomicInteger sesiones = new AtomicInteger();
			VentanaJuego ventana = new VentanaJuego(escucha -> {
				sesiones.incrementAndGet();
				return new SesionJuegoLocal(PruebaInterfaz::nivel, escucha, SwingUtilities::invokeLater);
			});
			ventana.mostrar();
			verificar(ventana.ventana().isShowing(), "La ventana Swing real está visible.");
			return new Escenario(ventana, sesiones);
		});
	}

	private static void navegacionMenu() throws Exception {
		try (Escenario escenario = abrir()) {
			verificarPredeterminados(escenario);
			capturar(escenario, "build/ui-menu.png");
			activar(escenario, "dificultad-dificil");
			esperarTexto(escenario, "resumen-seleccion", "Difícil 8×8");
			activar(escenario, "controles-flechas", KeyEvent.VK_ENTER);
			esperarTexto(escenario, "resumen-seleccion", "Flechas");
			enEdt(() -> { escenario.hacerClic("controles-wasd"); return null; });
			esperarTexto(escenario, "resumen-seleccion", "W A S D");
			visitarProximamente(escenario);
			verificar(escenario.sesiones().get() == 0,
					"Próximamente no debe crear una sesión de juego.");
		}
	}

	private static void verificarPredeterminados(Escenario escenario) throws Exception {
		enEdt(() -> {
			verificar(escenario.seleccionada("modo-un-jugador"),
					"Un jugador está seleccionado de forma predeterminada.");
			verificar(escenario.seleccionada("controles-flechas"),
					"Flechas está seleccionado de forma predeterminada.");
			verificar(escenario.seleccionada("dificultad-media"),
					"Media está seleccionada de forma predeterminada.");
			verificar(escenario.texto("resumen-seleccion").contains("Flechas · Media 6×6"),
					"El resumen predeterminado es correcto.");
			verificarTeclasVisuales(escenario.ventana().ventana().getRootPane());
			return null;
		});
	}

	private static void verificarTeclasVisuales(Container contenedor) {
		for (Component hijo : contenedor.getComponents()) {
			if (hijo instanceof TeclaVisual tecla) {
				verificarTeclaVisual(tecla);
			}
			if (hijo instanceof Container descendiente) {
				verificarTeclasVisuales(descendiente);
			}
		}
	}

	private static void verificarTeclaVisual(TeclaVisual tecla) {
		TarjetaControles tarjeta = (TarjetaControles) SwingUtilities.getAncestorOfClass(TarjetaControles.class, tecla);
		java.awt.Rectangle limites = SwingUtilities.convertRectangle(
				tecla.getParent(), tecla.getBounds(), tarjeta);
		int inferiorCuerpo = tarjeta.getHeight() - Tema.MARGEN_CONTROL - Tema.HALO_TARJETA;
		verificar(tecla.getHeight() >= tecla.getPreferredSize().height,
				"La tecla visual conserva su altura preferida.");
		verificar(limites.y + limites.height <= inferiorCuerpo,
				"La tecla visual queda dentro del borde de la tarjeta.");
	}

	private static void visitarProximamente(Escenario escenario) throws Exception {
		enEdt(() -> { escenario.hacerClic("modo-multijugador"); return null; });
		esperarHasta(() -> escenario.visible("proximamente"));
		capturar(escenario, "build/ui-proximamente.png");
		enEdt(() -> { escenario.hacerClic("proximamente-volver"); return null; });
		esperarHasta(() -> escenario.visible("iniciar"));
		verificar(escenario.seleccionada("modo-un-jugador"),
				"Volver restaura el modo anterior.");
		activar(escenario, "modo-multijugador");
		esperarHasta(() -> escenario.visible("proximamente"));
		enEdt(() -> { escenario.hacerClic("jugar-solo"); return null; });
		esperarHasta(() -> escenario.visible("iniciar"));
		verificar(escenario.seleccionada("modo-un-jugador"),
				"Jugar en solitario selecciona un jugador.");
	}

	private static void jugar(Controles controles, Dificultad dificultad) throws Exception {
		try (Escenario escenario = abrir()) {
			iniciar(escenario, controles, dificultad);
			verificarEntrada(escenario, controles, dificultad.cantidadCeldas());
			if (dificultad == Dificultad.DIFICIL) {
				capturar(escenario, "build/ui-jugando.png");
			}
			completar(escenario, controles, dificultad);
			if (dificultad == Dificultad.DIFICIL) {
				capturar(escenario, "build/ui-victoria.png");
			}
			reiniciarYVolver(escenario, dificultad.cantidadCeldas());
		}
	}

	private static void iniciar(Escenario escenario, Controles controles,
			Dificultad dificultad) throws Exception {
		enEdt(() -> {
			escenario.hacerClic(controles == Controles.FLECHAS
					? "controles-flechas" : "controles-wasd");
			escenario.hacerClic("dificultad-"
					+ dificultad.name().toLowerCase(java.util.Locale.ROOT));
			escenario.hacerClic("iniciar");
			return null;
		});
		esperarCantidad(escenario, 1, dificultad.cantidadCeldas());
		esperarHasta(() -> escenario.ventana().ventana().getFocusOwner() != null);
	}

	private static void verificarEntrada(Escenario escenario, Controles controles,
			int cantidadCeldas) throws Exception {
		pulsar(escenario, controles.tecla(Direccion.ARRIBA));
		esperarTexto(escenario, "estado-mensaje", "tablero");
		pulsar(escenario, controles.tecla(Direccion.DERECHA));
		esperarCantidad(escenario, 2, cantidadCeldas);
		pulsar(escenario, KeyEvent.VK_BACK_SPACE);
		esperarCantidad(escenario, 1, cantidadCeldas);
		pulsar(escenario, controles.tecla(Direccion.DERECHA));
		esperarCantidad(escenario, 2, cantidadCeldas);
		enEdt(() -> { escenario.hacerClic("reiniciar"); return null; });
		esperarCantidad(escenario, 1, cantidadCeldas);
		verificarTeclasInactivas(escenario, controles);
	}

	private static void verificarTeclasInactivas(Escenario escenario, Controles controles) throws Exception {
		enEdt(() -> {
			JComponent panel = (JComponent) escenario.buscar("juego");
			int inactiva = controles == Controles.FLECHAS ? KeyEvent.VK_W : KeyEvent.VK_UP;
			verificar(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.get(KeyStroke.getKeyStroke(inactiva, 0)) == null,
					"Los controles inactivos se eliminan.");
			escenario.buscar("reiniciar").requestFocusInWindow();
			return null;
		});
	}

	private static void completar(Escenario escenario, Controles controles,
			Dificultad dificultad) throws Exception {
		int visitadas = 1;
		for (int fila = 0; fila < dificultad.tamano(); fila++) {
			Direccion direccion = fila % 2 == 0 ? Direccion.DERECHA : Direccion.IZQUIERDA;
			for (int columna = 1; columna < dificultad.tamano(); columna++) {
				pulsar(escenario, controles.tecla(direccion));
				esperarCantidad(escenario, ++visitadas, dificultad.cantidadCeldas());
			}
			if (fila < dificultad.tamano() - 1) {
				pulsar(escenario, controles.tecla(Direccion.ABAJO));
				esperarCantidad(escenario, ++visitadas, dificultad.cantidadCeldas());
			}
		}
		esperarVictoria(escenario);
	}

	private static void esperarVictoria(Escenario escenario) throws Exception {
		esperarTexto(escenario, "victoria", "¡Completado!");
		esperarHasta(() -> escenario.visible("superposicion-victoria"));
		esperarHasta(() -> focoNombrado(escenario, "jugar-de-nuevo"));
	}

	private static boolean focoNombrado(Escenario escenario, String nombre) {
		Component foco = escenario.ventana().ventana().getFocusOwner();
		return foco != null && nombre.equals(foco.getName());
	}

	private static void reiniciarYVolver(Escenario escenario, int cantidadCeldas) throws Exception {
		enEdt(() -> {
			verificar(!escenario.buscar("deshacer").isEnabled(),
					"Deshacer queda deshabilitado después de la victoria.");
			escenario.hacerClic("jugar-de-nuevo");
			return null;
		});
		esperarCantidad(escenario, 1, cantidadCeldas);
		enEdt(() -> { escenario.hacerClic("volver"); return null; });
		esperarHasta(() -> escenario.visible("iniciar"));
	}

	private static void activar(Escenario escenario, String nombre) throws Exception {
		activar(escenario, nombre, KeyEvent.VK_SPACE);
	}

	private static void activar(Escenario escenario, String nombre, int tecla) throws Exception {
		enEdt(() -> {
			escenario.ventana().ventana().toFront();
			escenario.buscar(nombre).requestFocusInWindow();
			return null;
		});
		esperarHasta(() -> solicitarFoco(escenario, nombre));
		enviarTecla(escenario, tecla, KeyEvent.KEY_PRESSED);
		enviarTecla(escenario, tecla, KeyEvent.KEY_RELEASED);
	}

	private static boolean solicitarFoco(Escenario escenario, String nombre) {
		Component destino = escenario.buscar(nombre);
		if (escenario.ventana().ventana().getFocusOwner() == destino) {
			return true;
		}
		destino.requestFocusInWindow();
		return false;
	}

	private static void pulsar(Escenario escenario, int tecla) throws Exception {
		enEdt(() -> {
			JComponent panel = (JComponent) escenario.buscar("juego");
			Object vinculacion = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
					.get(KeyStroke.getKeyStroke(tecla, 0));
			verificar(vinculacion != null,
					"La tecla debe tener una vinculación activa en la ventana.");
			enviarTecla(escenario, tecla, KeyEvent.KEY_PRESSED);
			return null;
		});
	}

	private static void enviarTecla(Escenario escenario, int tecla, int identificador) {
		KeyboardFocusManager administradorFoco =
				KeyboardFocusManager.getCurrentKeyboardFocusManager();
		Component destino = escenario.ventana().ventana().getFocusOwner();
		verificar(destino != null, "La ventana de prueba debe tener el foco del teclado.");
		administradorFoco.redispatchEvent(destino, new KeyEvent(destino, identificador,
				System.currentTimeMillis(), 0, tecla, KeyEvent.CHAR_UNDEFINED));
	}

	private static void esperarCantidad(Escenario escenario, int cantidad, int cantidadCeldas) throws Exception {
		String esperado = cantidad + "/" + cantidadCeldas;
		esperarHasta(() -> escenario.texto("progreso").equals(esperado));
	}

	private static void esperarTexto(Escenario escenario, String nombre, String texto) throws Exception {
		esperarHasta(() -> escenario.texto(nombre).contains(texto));
	}

	private static void esperarHasta(BooleanSupplier condicion) throws Exception {
		long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(SEGUNDOS_ESPERA);
		while (!enEdt(condicion::getAsBoolean)) {
			verificar(System.nanoTime() < limite,
					"Se agotó la espera del estado de la interfaz o del trabajador.");
			Thread.sleep(MILISEGUNDOS_CONSULTA);
		}
	}

	private static void capturar(Escenario escenario, String recorrido) throws Exception {
		BufferedImage imagen = enEdt(() -> renderizar(escenario.ventana().ventana().getRootPane()));
		verificar(ImageIO.write(imagen, "png", Path.of(recorrido).toFile()),
				"El escritor de PNG está disponible.");
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
		throw new AssertionError("Falta el componente de interfaz: " + nombre);
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

	private record Escenario(VentanaJuego ventana, AtomicInteger sesiones) implements AutoCloseable {
		Component buscar(String nombre) {
			return PruebaInterfaz.buscar(ventana.ventana().getRootPane(), nombre);
		}

		void hacerClic(String nombre) {
			Component componente = buscar(nombre);
			if (componente instanceof AbstractButton boton) {
				boton.doClick(DEMORA_CERO);
			} else {
				((ControlAccion) componente).hacerClic();
			}
		}

		boolean seleccionada(String nombre) {
			Component componente = buscar(nombre);
			return componente instanceof AbstractButton boton
					? boton.isSelected() : ((TarjetaSeleccionable) componente).estaSeleccionada();
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
					verificar(!ventana.ventana().isDisplayable(),
							"La ventana debe liberarse al cerrar.");
					return null;
				});
			} catch (Exception excepcion) {
				throw new AssertionError("No se pudo cerrar la ventana de prueba.", excepcion);
			}
		}
	}
}
