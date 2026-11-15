package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import model.ConfigPartida;
import model.Dificultad;
import utils.Constantes;

/**
 * Pantalla multijugador: crear sala (host) o unirse a una (cliente).
 * Valida los datos y delega en la ventana mediante callbacks (patrón Observer).
 */
final class PanelMultijugador extends JPanel {
	static final int MAX_NOMBRE = 16;
	private static final long serialVersionUID = 1L;
	private static final int COLUMNAS_NOMBRE = 14;
	private static final int COLUMNAS_DIRECCION = 18;
	private static final int ESPACIO_CHICO = 6;
	private static final int ANCHO_ETIQUETA = 150;
	private static final int ANCHO_SPINNER = 70;
	private static final double PESO_CAMPO = 1.0;
	private static final String TEXTO_CREAR = "Crear sala";
	private static final String TEXTO_CONECTAR = "Conectar";
	private static final String NOMBRE_VACIO = "Escribí tu nombre para entrar.";
	private static final String NOMBRE_LARGO = "El nombre admite hasta " + MAX_NOMBRE + " letras.";
	private static final String DIRECCION_VACIA = "Escribí la dirección del host (ip:puerto).";
	private static final String AVISO_MENU =
			"Para cambiar la dificultad o las rondas volvé al menú.";

	private final JTextField nombreHost = campo("host-nombre", "", COLUMNAS_NOMBRE);
	private final JTextField nombreCliente = campo("unirse-nombre", "", COLUMNAS_NOMBRE);
	private final JTextField direccion = campo("unirse-direccion",
			Constantes.IP_LOCAL + ":" + Constantes.PUERTO, COLUMNAS_DIRECCION);
	private final JSpinner jugadores = new JSpinner(new SpinnerNumberModel(
			ConfigPartida.MIN_JUGADORES_RED, ConfigPartida.MIN_JUGADORES_RED,
			ConfigPartida.MAX_JUGADORES, 1));
	private final JLabel resumen = Tema.texto(" ", Font.PLAIN, Tema.TINTA);
	private final JButton crear = Tema.boton(TEXTO_CREAR, true);
	private final JButton conectar = Tema.boton(TEXTO_CONECTAR, true);
	private final JLabel estadoMensaje = Tema.texto(" ", Font.PLAIN, Tema.TINTA);
	private final transient BiConsumer<String, Integer> alCrear;
	private final transient BiConsumer<String, DireccionHost> alUnirse;

	PanelMultijugador(BiConsumer<String, Integer> alCrear,
			BiConsumer<String, DireccionHost> alUnirse, Runnable alVolver) {
		super();
		this.alCrear = alCrear;
		this.alUnirse = alUnirse;
		setName("multijugador");
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(
				Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		construirContenido(alVolver);
	}

	/** Muestra la dificultad y rondas elegidas en el menú (las define el host). */
	void configuracion(Dificultad dificultad, int rondas) {
		resumen.setText(Resumen.configuracion(dificultad, rondas));
	}

	void ocupado(boolean ocupado) {
		crear.setEnabled(!ocupado);
		conectar.setEnabled(!ocupado);
		conectar.setText(ocupado ? "Conectando…" : TEXTO_CONECTAR);
	}

	void estado(String mensaje) {
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.TINTA);
	}

	void error(String mensaje) {
		ocupado(false);
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.ERROR);
	}

	private void construirContenido(Runnable alVolver) {
		JLabel titulo = Tema.etiqueta(
				"Multijugador en red", Font.BOLD, Tema.TAMANO_TITULO, Tema.TINTA);
		alinear(titulo);
		add(titulo);
		add(Box.createVerticalStrut(Tema.ESPACIO));
		add(seccion("CREAR PARTIDA (HOST)", panelHost()));
		add(seccion("UNIRSE A UNA PARTIDA", panelCliente()));
		estadoMensaje.setName("estado-multijugador");
		alinear(estadoMensaje);
		add(Box.createVerticalGlue());
		add(estadoMensaje);
		add(Box.createVerticalStrut(Tema.ESPACIO));
		add(botonVolver(alVolver));
	}

	private JButton botonVolver(Runnable alVolver) {
		JButton volver = Tema.boton("Volver al menú", false);
		volver.setName("multijugador-volver");
		volver.addActionListener(e -> alVolver.run());
		alinear(volver);
		return volver;
	}

	private JPanel panelHost() {
		JPanel panel = tarjeta();
		jugadores.setName("host-jugadores");
		int alto = jugadores.getPreferredSize().height + ESPACIO_CHICO;
		jugadores.setPreferredSize(new Dimension(ANCHO_SPINNER, alto));
		resumen.setName("host-resumen");
		crear.setName("host-crear");
		crear.addActionListener(e -> pedirCrear());
		agregarFila(panel, 0, "Tu nombre", nombreHost);
		agregarFila(panel, 1, "Jugadores (2 a 4)", jugadores);
		agregarFila(panel, 2, "Partida", resumen);
		JLabel aviso = Tema.etiqueta(AVISO_MENU, Font.PLAIN, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE);
		agregarFila(panel, 3, "", aviso);
		agregarFila(panel, 4, "", crear);
		return panel;
	}

	private JPanel panelCliente() {
		JPanel panel = tarjeta();
		conectar.setName("unirse-conectar");
		conectar.addActionListener(e -> pedirConectar());
		agregarFila(panel, 0, "Tu nombre", nombreCliente);
		agregarFila(panel, 1, "Dirección del host", direccion);
		agregarFila(panel, 2, "", conectar);
		return panel;
	}

	private void pedirCrear() {
		String nombre = nombreValido(nombreHost);
		if (nombre != null) {
			estado(" ");
			alCrear.accept(nombre, (Integer) jugadores.getValue());
		}
	}

	private void pedirConectar() {
		String nombre = nombreValido(nombreCliente);
		if (nombre == null) {
			return;
		}
		if (direccion.getText().isBlank()) {
			error(DIRECCION_VACIA);
			return;
		}
		try {
			alUnirse.accept(nombre, DireccionHost.parsear(direccion.getText()));
		} catch (IllegalArgumentException e) {
			error(e.getMessage());
		}
	}

	private String nombreValido(JTextField campo) {
		String nombre = campo.getText().trim();
		if (nombre.isEmpty() || nombre.length() > MAX_NOMBRE) {
			error(nombre.isEmpty() ? NOMBRE_VACIO : NOMBRE_LARGO);
			campo.requestFocusInWindow();
			return null;
		}
		return nombre;
	}

	private static JTextField campo(String nombre, String texto, int columnas) {
		JTextField campo = new JTextField(texto, columnas);
		campo.setName(nombre);
		campo.setFont(Tema.fuente(Font.PLAIN, Tema.TAMANO_TEXTO));
		campo.setBorder(Tema.marco(Tema.BORDE, 1, ESPACIO_CHICO, ESPACIO_CHICO));
		return campo;
	}

	private static JPanel tarjeta() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(Tema.TARJETA);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setBorder(Tema.marco(Tema.BORDE, 1, Tema.ESPACIO, Tema.MARGEN));
		return panel;
	}

	private static void agregarFila(JPanel panel, int fila, String texto, JComponent control) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = fila;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(ESPACIO_CHICO, 0, ESPACIO_CHICO, Tema.ESPACIO);
		panel.add(etiquetaFila(texto), c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = fila;
		c.weightx = PESO_CAMPO;
		c.anchor = GridBagConstraints.WEST;
		boolean ancho = control instanceof JTextField;
		c.fill = ancho ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		panel.add(control, c);
	}

	private static JLabel etiquetaFila(String texto) {
		JLabel etiqueta = Tema.texto(texto, Font.PLAIN, Tema.TINTA_SUAVE);
		int alto = etiqueta.getPreferredSize().height;
		etiqueta.setPreferredSize(new Dimension(ANCHO_ETIQUETA, alto));
		return etiqueta;
	}

	private static JPanel seccion(String titulo, JPanel contenido) {
		JPanel panel = Tema.panelBorde(new BorderLayout(0, ESPACIO_CHICO), 0, 0);
		JLabel etiqueta = Tema.etiqueta(titulo, Font.BOLD, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE);
		etiqueta.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, 0, 0, 0));
		panel.add(etiqueta, BorderLayout.NORTH);
		panel.add(contenido, BorderLayout.CENTER);
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private static void alinear(JComponent comp) {
		comp.setAlignmentX(LEFT_ALIGNMENT);
	}
}
