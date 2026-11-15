package ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import controller.ActualizacionVista;
import events.DestinoCelda;
import model.Direccion;
import model.EstadoJuego;

final class PanelJuego extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String AYUDA_MOUSE = "Arrastrá desde el camino · volvé atrás para deshacer";
	private static final int RELLENO_CHIP_V = 10;
	private static final int RELLENO_CHIP_H = 14;
	private static final int SEPARACION_PIE = 6;
	private static final int PROGRESO_MAXIMO = 100;

	private final PanelTablero tablero = new PanelTablero();
	private final JLabel celdas = crearChip("progreso");
	private final JLabel siguiente = crearChip("siguiente");
	private final JProgressBar progreso = new JProgressBar(0, PROGRESO_MAXIMO);
	private final JLabel leyenda = crearEtiquetaCentrada(" ", null);
	private final JLabel estadoMensaje = crearEtiquetaCentrada(" ", "estado-mensaje");
	private final JButton deshacer = Tema.boton("Deshacer", false);
	private final JButton reiniciar = Tema.boton("Reiniciar", false);
	private final transient AdaptadorMouse adaptadorMouse = new AdaptadorMouse(tablero, this::irA);
	private final transient Consumer<Direccion> mover;
	private final transient Consumer<DestinoCelda> irA;
	private Controles controlesActuales;
	private boolean jugando;

	PanelJuego(Consumer<Direccion> mover, Consumer<DestinoCelda> irA,
			Runnable accionDeshacer, Runnable accionReiniciar, Runnable volver) {
		super(new BorderLayout(0, Tema.ESPACIO));
		this.mover = mover;
		this.irA = irA;
		setName("juego");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		configurarEntradas(accionDeshacer, accionReiniciar);
		add(encabezado(volver), BorderLayout.NORTH);
		add(tablero, BorderLayout.CENTER);
		add(pie(), BorderLayout.SOUTH);
		controles(Controles.FLECHAS);
	}

	private void configurarEntradas(Runnable accionDeshacer, Runnable accionReiniciar) {
		tablero.setFocusable(true);
		adaptadorMouse.instalar();
		progreso.setStringPainted(true);
		vincularBoton(deshacer, "deshacer", accionDeshacer);
		vincularBoton(reiniciar, "reiniciar", () -> {
			adaptadorMouse.habilitar(controlesActuales == Controles.MOUSE);
			accionReiniciar.run();
		});
	}

	private void vincularBoton(JButton boton, String nombre, Runnable accion) {
		boton.setName(nombre);
		boton.addActionListener(evento -> {
			accion.run();
			enfocarEntrada();
		});
	}

	private JPanel encabezado(Runnable accionVolver) {
		JPanel chips = new JPanel(new GridLayout(1, 2, Tema.ESPACIO, 0));
		chips.setOpaque(false);
		chips.add(celdas);
		chips.add(siguiente);
		JPanel encabezado = new JPanel(new BorderLayout(0, Tema.ESPACIO));
		encabezado.setOpaque(false);
		encabezado.add(barraSuperior(accionVolver), BorderLayout.NORTH);
		encabezado.add(chips, BorderLayout.CENTER);
		encabezado.add(progreso, BorderLayout.SOUTH);
		return encabezado;
	}

	private JPanel barraSuperior(Runnable accionVolver) {
		JPanel barra = new JPanel(new BorderLayout());
		barra.setOpaque(false);
		JButton volver = Tema.boton("Desconectar / Menú", false);
		volver.setName("volver");
		volver.addActionListener(evento -> accionVolver.run());
		barra.add(Tema.etiqueta("ZIP", Font.BOLD, Tema.TAMANO_TITULO, Tema.TINTA), BorderLayout.WEST);
		barra.add(volver, BorderLayout.EAST);
		return barra;
	}

	private JPanel pie() {
		JPanel textos = new JPanel(new GridLayout(0, 1, 0, SEPARACION_PIE));
		textos.setOpaque(false);
		textos.add(leyenda);
		textos.add(crearEtiquetaCentrada("Conectá los números en orden y recorré todas las celdas.", null));
		textos.add(estadoMensaje);
		JPanel pie = new JPanel(new BorderLayout(0, Tema.ESPACIO));
		pie.setOpaque(false);
		pie.add(textos, BorderLayout.NORTH);
		pie.add(acciones(), BorderLayout.SOUTH);
		return pie;
	}

	private JPanel acciones() {
		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.CENTER, Tema.ESPACIO, 0));
		acciones.setOpaque(false);
		acciones.add(deshacer);
		acciones.add(reiniciar);
		return acciones;
	}

	void controles(Controles controles) {
		adaptadorMouse.habilitar(controles.usaMouse());
		if (controles == controlesActuales) {
			return;
		}
		this.controlesActuales = controles;
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		getActionMap().clear();
		if (!controles.usaMouse()) {
			vincularDirecciones(controles);
		}
		vincular("deshacer", KeyEvent.VK_BACK_SPACE, deshacer::doClick);
		leyenda.setText((controles.usaMouse() ? AYUDA_MOUSE : controles.leyendaTeclas())
				+ " · Backspace = deshacer");
	}

	private void vincularDirecciones(Controles controles) {
		for (Direccion dir : Direccion.values()) {
			vincular("mover-" + dir, controles.tecla(dir), () -> {
				if (jugando) {
					mover.accept(dir);
				}
			});
		}
	}

	private void vincular(String nombre, int tecla, Runnable accion) {
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nombre);
		getActionMap().put(nombre, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent evento) {
				if (entradaHabilitada()) {
					accion.run();
				}
			}
		});
	}

	private void irA(DestinoCelda destino) {
		if (jugando && entradaHabilitada()) {
			irA.accept(destino);
		}
	}

	private boolean entradaHabilitada() {
		JRootPane raiz = SwingUtilities.getRootPane(this);
		return raiz == null || raiz.getGlassPane() == null || !raiz.getGlassPane().isVisible();
	}

	void mostrar(ActualizacionVista act) {
		EstadoJuego estadoJuego = act.estadoJuego();
		jugando = !act.partidaFinalizada() && !estadoJuego.gano();
		tablero.mostrar(estadoJuego, act.manchadoPorTomate());
		celdas.setText("Celdas: " + estadoJuego.visitadas() + "/" + estadoJuego.nivel().cantidadCeldas());
		siguiente.setText("Próximo: " + textoSiguiente(act, estadoJuego));
		progreso.setValue((int) Math.round(PROGRESO_MAXIMO
				* (double) estadoJuego.visitadas() / estadoJuego.nivel().cantidadCeldas()));
		deshacer.setEnabled(jugando && estadoJuego.visitadas() > 1);
		estadoMensaje.setText(act.mensaje());
	}

	private String textoSiguiente(ActualizacionVista act, EstadoJuego estadoJuego) {
		String texto = estadoJuego.gano() ? "Completado" : Integer.toString(estadoJuego.proximoNumero());
		if (act.rondasObjetivo() > 1) {
			texto += " · " + act.rondasLocal() + "-" + act.rondasRival() + " (" + act.rondasObjetivo() + ")";
		}
		return texto;
	}

	void enfocarEntrada() { tablero.requestFocusInWindow(); }

	private static JLabel crearChip(String nombre) {
		JLabel chip = Tema.etiqueta(" ", Font.BOLD, Tema.TAMANO_TEXTO, Tema.TINTA);
		chip.setHorizontalAlignment(SwingConstants.CENTER);
		chip.setName(nombre);
		chip.setOpaque(true);
		chip.setBackground(Tema.TARJETA);
		chip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDE),
				BorderFactory.createEmptyBorder(RELLENO_CHIP_V, RELLENO_CHIP_H,
						RELLENO_CHIP_V, RELLENO_CHIP_H)));
		return chip;
	}

	private static JLabel crearEtiquetaCentrada(String texto, String nombre) {
		JLabel etiqueta = Tema.etiqueta(texto, Font.PLAIN, Tema.TAMANO_TEXTO, Tema.TINTA_SUAVE);
		etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
		etiqueta.setName(nombre);
		return etiqueta;
	}
}
