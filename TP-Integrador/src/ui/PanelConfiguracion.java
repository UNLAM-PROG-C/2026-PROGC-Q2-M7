package ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import model.Dificultad;
import utils.Constantes;

final class PanelConfiguracion extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int RELLENO_CAMPO = 8;
	private static final String[] DIFICULTADES = { "Fácil", "Media", "Difícil"};
	private static final String TEXT_HOST = "Crear partida (Host)";
	private static final String TEXT_CLIENTE = "Unirse (Cliente)";

	private final transient List<JToggleButton> tarjetas = new ArrayList<>();
	private final JPanel cuerpo = Tema.panelBorde(null, 0, 0);
	private final JPanel panelRolRed = panelVertical();
	private final JPanel panelOpcionesPartida = panelVertical();
	private final JPanel panelJugadoresHost = panelVertical();
	private final JPanel panelDatosCliente = panelVertical();
	private final JButton botonModo = Tema.boton("Un jugador", false);
	private final JSlider sliderRondas = new JSlider(
			Constantes.MIN_RONDAS, Constantes.MAX_RONDAS, Constantes.RONDAS_POR_DEFECTO);
	private final JLabel etiquetaRondas = Tema.etiqueta("", Font.BOLD, Tema.TAMANO_CHICO, Tema.ACENTO);
	private final JSlider sliderJugadores = new JSlider(
			Constantes.MIN_JUGADORES_RED, Constantes.MAX_JUGADORES_RED, Constantes.JUGADORES_RED_POR_DEFECTO);
	private final JLabel etiquetaJugadores = Tema.etiqueta("", Font.BOLD, Tema.TAMANO_CHICO, Tema.ACENTO);
	private final JTextField campoDireccion = new JTextField(Constantes.IP_LOCAL + ":" + Constantes.PUERTO);
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN, Tema.TAMANO_TEXTO, Tema.TINTA_SUAVE);
	private final JButton accionPrincipal = Tema.boton("Crear partida (Host)", true);

	private Dificultad dificultadSeleccionada = Dificultad.MEDIA;
	private int rolSeleccionado = Constantes.ROL_HOST;
	private int ultimoRolRed = Constantes.ROL_HOST;
	private boolean enEspera;

	PanelConfiguracion(Runnable alEjecutarAccion, Runnable alCancelar) {
		super(new BorderLayout(0, Tema.ESPACIO));
		setName("configuracion");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		construirCuerpo();
		configurarPie(alEjecutarAccion, alCancelar);
		actualizarVistaModo();
	}

	private void configurarPie(Runnable alEjecutarAccion, Runnable alCancelar) {
		estadoMensaje.setName("resumen-seleccion");
		estadoMensaje.setHorizontalAlignment(JLabel.CENTER);
		estadoMensaje.setVerticalAlignment(JLabel.BOTTOM);
		accionPrincipal.setName("iniciar");
		accionPrincipal.addActionListener(e -> {
			if (enEspera) {
				alCancelar.run();
			} else {
				alEjecutarAccion.run();
			}
		});
		add(estadoMensaje, BorderLayout.CENTER);
		add(accionPrincipal, BorderLayout.SOUTH);
	}

	int rolSeleccionado() { return rolSeleccionado; }

	Dificultad dificultadSeleccionada() { return dificultadSeleccionada; }

	int cantidadRondas() { return sliderRondas.getValue(); }

	int cantidadJugadores() { return sliderJugadores.getValue(); }

	String direccionHost() { return campoDireccion.getText().trim(); }

	void ocupado(boolean ocupado, String mensajeEspera) {
		this.enEspera = ocupado;
		botonModo.setEnabled(!ocupado);
		sliderRondas.setEnabled(!ocupado);
		sliderJugadores.setEnabled(!ocupado);
		campoDireccion.setEnabled(!ocupado);
		tarjetas.forEach(t -> t.setEnabled(!ocupado));
		if (ocupado) {
			accionPrincipal.setText("Cancelar espera");
			estado(mensajeEspera);
		} else {
			actualizarResumenYBoton();
		}
	}

	void estado(String mensaje) {
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.TINTA);
	}

	void error(String mensaje) {
		ocupado(false, "");
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.ERROR);
	}

	private void construirCuerpo() {
		cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
		cuerpo.add(crearBarraSuperior());
		JLabel titulo = Tema.etiqueta("ZIP", Font.BOLD, Tema.TAMANO_TITULO, Tema.TINTA);
		titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, Tema.ESPACIO, 0));
		titulo.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		cuerpo.add(titulo);
		construirSeccionRolRed();
		construirSeccionHost();
		construirSeccionCliente();
		cuerpo.add(panelRolRed);
		cuerpo.add(panelOpcionesPartida);
		cuerpo.add(panelJugadoresHost);
		cuerpo.add(panelDatosCliente);
		add(cuerpo, BorderLayout.NORTH);
	}

	private JPanel crearBarraSuperior() {
		JPanel barra = Tema.panelBorde(new BorderLayout(Tema.ESPACIO, 0), 0, 0);
		barra.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		barra.setBorder(BorderFactory.createEmptyBorder(0, 0, Tema.ESPACIO / 2, 0));
		botonModo.setName("alternar-modo");
		botonModo.addActionListener(e -> alternarModoIndividual());
		barra.add(botonModo, BorderLayout.WEST);
		return barra;
	}

	private void alternarModoIndividual() {
		if (rolSeleccionado == Constantes.ROL_LOCAL) {
			rolSeleccionado = ultimoRolRed;
		} else {
			ultimoRolRed = rolSeleccionado;
			rolSeleccionado = Constantes.ROL_LOCAL;
		}
		actualizarVistaModo();
	}

	private void construirSeccionRolRed() {
		ButtonGroup grupo = new ButtonGroup();
		JPanel grilla = Tema.panelBorde(new GridLayout(1, 0, Tema.ESPACIO, 0), 0, 0);
		JToggleButton host = crearBotonTarjeta("rol-host", TEXT_HOST, true, () -> elegirRolRed(Constantes.ROL_HOST));
		JToggleButton cli = crearBotonTarjeta("rol-cliente", TEXT_CLIENTE, false,
				() -> elegirRolRed(Constantes.ROL_CLIENTE));
		grupo.add(host);
		grupo.add(cli);
		grilla.add(host);
		grilla.add(cli);
		agregarSeccion(panelRolRed, "MODO MULTIJUGADOR", grilla);
	}

	private void elegirRolRed(int rol) {
		this.rolSeleccionado = rol;
		this.ultimoRolRed = rol;
		actualizarVistaModo();
	}

	private void construirSeccionHost() {
		ButtonGroup grupo = new ButtonGroup();
		JPanel grilla = Tema.panelBorde(new GridLayout(1, 0, Tema.ESPACIO, 0), 0, 0);
		for (Dificultad d : Dificultad.values()) {
			JToggleButton t = crearBotonTarjeta("dificultad-" + d.name().toLowerCase(Locale.ROOT),
					textoDificultad(d), d == dificultadSeleccionada, () -> seleccionarDificultad(d));
			grupo.add(t);
			grilla.add(t);
		}
		agregarSeccion(panelOpcionesPartida, "DIFICULTAD", grilla);
		agregarSeccion(panelOpcionesPartida, "RONDAS PARA GANAR",
				crearPanelSlider(sliderRondas, "rondas", etiquetaRondas));
		agregarSeccion(panelJugadoresHost, "JUGADORES EN LA SALA",
				crearPanelSlider(sliderJugadores, "host-jugadores", etiquetaJugadores));
	}

	private void seleccionarDificultad(Dificultad dificultad) {
		this.dificultadSeleccionada = dificultad;
		actualizarResumenYBoton();
	}

	private void construirSeccionCliente() {
		JPanel tarjeta = Tema.panelBorde(new BorderLayout(0, Tema.ESPACIO / 2), Tema.MARGEN, Tema.MARGEN);
		tarjeta.setOpaque(true);
		tarjeta.setBackground(Tema.TARJETA);
		tarjeta.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Tema.BORDE, 1),
				BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN)));
		configurarCampoDireccion();
		tarjeta.add(Tema.etiqueta("Dirección del Host (IP o IP:puerto):", Font.BOLD, Tema.TAMANO_TEXTO, Tema.TINTA),
				BorderLayout.NORTH);
		tarjeta.add(campoDireccion, BorderLayout.CENTER);
		tarjeta.add(Tema.etiqueta("La dificultad y las rondas las define el Host al iniciar.",
				Font.PLAIN, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE), BorderLayout.SOUTH);
		agregarSeccion(panelDatosCliente, "CONEXIÓN AL HOST", tarjeta);
	}

	private void configurarCampoDireccion() {
		campoDireccion.setName("direccion-host");
		campoDireccion.setFont(Tema.fuente(Font.PLAIN, Tema.TAMANO_SUBTITULO));
		campoDireccion.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Tema.BORDE, 1),
				BorderFactory.createEmptyBorder(RELLENO_CAMPO, RELLENO_CAMPO, RELLENO_CAMPO, RELLENO_CAMPO)));
	}

	private JPanel crearPanelSlider(JSlider slider, String nombre, JLabel etiqueta) {
		slider.setName(nombre);
		slider.setOpaque(false);
		slider.setMajorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setSnapToTicks(true);
		slider.addChangeListener(e -> actualizarResumenYBoton());
		JPanel panel = Tema.panelBorde(new BorderLayout(Tema.ESPACIO, 0), 0, 0);
		panel.add(slider, BorderLayout.CENTER);
		panel.add(etiqueta, BorderLayout.EAST);
		return panel;
	}

	private JToggleButton crearBotonTarjeta(String nombre, String html, boolean sel, Runnable alElegir) {
		JToggleButton tarjeta = new JToggleButton(html, sel);
		tarjeta.setName(nombre);
		tarjeta.setFocusPainted(false);
		tarjeta.setOpaque(true);
		tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tarjeta.addItemListener(e -> actualizarTarjeta(tarjeta));
		tarjeta.addActionListener(e -> alElegir.run());
		actualizarTarjeta(tarjeta);
		tarjetas.add(tarjeta);
		return tarjeta;
	}

	private static void actualizarTarjeta(JToggleButton tarjeta) {
		boolean sel = tarjeta.isSelected();
		tarjeta.setBackground(sel ? Tema.VISITADA : Tema.TARJETA);
		tarjeta.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(sel ? Tema.ACENTO : Tema.BORDE, 2),
				BorderFactory.createEmptyBorder(Tema.ESPACIO, Tema.ESPACIO, Tema.ESPACIO, Tema.ESPACIO)));
	}

	private void actualizarVistaModo() {
		boolean local = rolSeleccionado == Constantes.ROL_LOCAL;
		boolean host = rolSeleccionado == Constantes.ROL_HOST;
		botonModo.setText(local ? "← Multijugador" : "Un jugador");
		panelRolRed.setVisible(!local);
		panelOpcionesPartida.setVisible(local || host);
		panelJugadoresHost.setVisible(host);
		panelDatosCliente.setVisible(rolSeleccionado == Constantes.ROL_CLIENTE);
		actualizarResumenYBoton();
		revalidate();
		repaint();
	}

	private void actualizarResumenYBoton() {
		int rondas = cantidadRondas();
		String txtRondas = rondas + (rondas == 1 ? " ronda" : " rondas");
		etiquetaRondas.setText(txtRondas);
		etiquetaJugadores.setText(cantidadJugadores() + " jugadores");
		Dificultad d = dificultadSeleccionada;
		String reglaRondas = rondas == 1 ? "1 ronda" : "Primero a " + txtRondas;
		String base = DIFICULTADES[d.ordinal()] + " " + d.tamano() + "×" + d.tamano()
				+ " · " + reglaRondas;
		aplicarTextoSegunRol(base);
	}

	private void aplicarTextoSegunRol(String resumenBase) {
		if (rolSeleccionado == Constantes.ROL_HOST) {
			accionPrincipal.setText("Crear partida (Host)");
			estadoMensaje.setText(resumenBase + " · " + cantidadJugadores() + " jugadores (puerto "
					+ Constantes.PUERTO + ")");
		} else if (rolSeleccionado == Constantes.ROL_CLIENTE) {
			accionPrincipal.setText("Unirse a partida (Cliente)");
			estadoMensaje.setText("Conectarse como cliente a la dirección ingresada");
		} else {
			accionPrincipal.setText("Jugar (Un jugador)");
			estadoMensaje.setText("Un jugador · " + resumenBase);
		}
		estadoMensaje.setForeground(Tema.TINTA_SUAVE);
	}

	private static void agregarSeccion(JPanel destino, String titulo, JComponent contenido) {
		JLabel etiqueta = Tema.etiqueta(titulo, Font.BOLD, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE);
		etiqueta.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO, 0, Tema.ESPACIO / 2, 0));
		etiqueta.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		contenido.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		destino.add(etiqueta);
		destino.add(contenido);
	}

	private static JPanel panelVertical() {
		JPanel panel = Tema.panelBorde(null, 0, 0);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		return panel;
	}

	private static String textoDificultad(Dificultad d) {
		return "<html><div style='text-align:center'><b>" + DIFICULTADES[d.ordinal()] + "<br><big>"
				+ d.tamano() + "×" + d.tamano() + "</big></b></div></html>";
	}
}
