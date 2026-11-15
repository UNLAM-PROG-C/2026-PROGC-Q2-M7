package ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import model.Dificultad;
import model.ModoJuego;
import utils.Constantes;

final class PanelConfiguracion extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String TITULO_HTML = "<html><span style='font-size:28pt'><b>ZIP</b></span><br>"
			+ "<span style='color:#4A5073'>Conectá los números. Completá el tablero.</span></html>";
	private static final String[][] DIFICULTADES = {{"Fácil", "Tablero chico,<br>recorrido directo."},
			{"Media", "Tablero mediano,<br>hay que planificar."},
			{"Difícil", "Tablero grande,<br>cada giro cuenta."}};

	private final transient List<JToggleButton> tarjetas = new ArrayList<>();
	private final JPanel cuerpo = Tema.panelBorde(null, 0, 0);
	private final JSlider sliderRondas = new JSlider(
			Constantes.MIN_RONDAS, Constantes.MAX_RONDAS, Constantes.RONDAS_POR_DEFECTO);
	private final JLabel etiquetaRondas = Tema.etiqueta("", Font.BOLD, Tema.TAMANO_CHICO, Tema.ACENTO);
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN, Tema.TAMANO_TEXTO, Tema.TINTA_SUAVE);
	private final JButton jugar = Tema.boton("Jugar", true);

	private Dificultad dificultadSeleccionada = Dificultad.MEDIA;

	PanelConfiguracion(Consumer<Dificultad> alIniciar, Consumer<ModoJuego> alCambiarModo) {
		super(new BorderLayout(0, Tema.ESPACIO));
		setName("configuracion");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		construirCuerpo(alCambiarModo);
		estadoMensaje.setName("resumen-seleccion");
		estadoMensaje.setHorizontalAlignment(JLabel.CENTER);
		estadoMensaje.setVerticalAlignment(JLabel.BOTTOM);
		jugar.setName("iniciar");
		jugar.addActionListener(e -> alIniciar.accept(dificultadSeleccionada));
		add(estadoMensaje, BorderLayout.CENTER);
		add(jugar, BorderLayout.SOUTH);
		actualizar();
	}

	void seleccionarModo(ModoJuego modo) {
		String nombre = "modo-" + modo.name().toLowerCase(Locale.ROOT).replace('_', '-');
		tarjetas.stream().filter(t -> nombre.equals(t.getName())).findFirst().ifPresent(t -> t.setSelected(true));
		actualizar();
	}

	Dificultad dificultadSeleccionada() {
		return dificultadSeleccionada;
	}

	int cantidadRondas() {
		return sliderRondas.getValue();
	}

	void ocupado(boolean ocupado) {
		jugar.setEnabled(!ocupado);
		sliderRondas.setEnabled(!ocupado);
		tarjetas.forEach(t -> t.setEnabled(!ocupado));
		jugar.setText(ocupado ? "Preparando…" : "Jugar");
		if (ocupado) {
			estadoMensaje.setText("Generando un tablero con solución.");
			estadoMensaje.setForeground(Tema.TINTA);
			return;
		}
		actualizar();
	}

	void error(String mensaje) {
		ocupado(false);
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.ERROR);
	}

	private void construirCuerpo(Consumer<ModoJuego> alCambiarModo) {
		cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
		JLabel titulo = Tema.etiqueta(TITULO_HTML, Font.PLAIN, Tema.TAMANO_SUBTITULO, Tema.TINTA);
		titulo.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		cuerpo.add(titulo);
		grupo("MODO DE JUEGO", ModoJuego.UN_JUGADOR, "modo-", alCambiarModo);
		grupo("DIFICULTAD", dificultadSeleccionada, "dificultad-", d -> dificultadSeleccionada = d);
		seccion("RONDAS PARA GANAR", panelSliderRondas());
		add(cuerpo, BorderLayout.NORTH);
	}

	private <T extends Enum<T>> void grupo(String titulo, T inicial, String prefijo, Consumer<T> alElegir) {
		ButtonGroup grupo = new ButtonGroup();
		JPanel grilla = Tema.panelBorde(new GridLayout(1, 0, Tema.ESPACIO, 0), 0, 0);
		for (T valor : inicial.getDeclaringClass().getEnumConstants()) {
			JToggleButton tarjeta = crearTarjeta(valor, prefijo, valor == inicial, alElegir);
			grupo.add(tarjeta);
			grilla.add(tarjeta);
		}
		seccion(titulo, grilla);
	}

	private <T extends Enum<T>> JToggleButton crearTarjeta(T valor, String pref, boolean sel, Consumer<T> alElegir) {
		JToggleButton tarjeta = new JToggleButton(textoOpcion(valor), sel);
		tarjeta.setName(pref + valor.name().toLowerCase(Locale.ROOT).replace('_', '-'));
		tarjeta.setFocusPainted(false);
		tarjeta.setOpaque(true);
		tarjeta.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tarjeta.addItemListener(e -> actualizarTarjeta(tarjeta));
		tarjeta.addActionListener(e -> { alElegir.accept(valor); actualizar(); });
		actualizarTarjeta(tarjeta);
		tarjetas.add(tarjeta);
		return tarjeta;
	}

	private static void actualizarTarjeta(JToggleButton tarjeta) {
		boolean sel = tarjeta.isSelected();
		tarjeta.setBackground(sel ? Tema.VISITADA : Tema.TARJETA);
		tarjeta.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(sel ? Tema.ACENTO : Tema.BORDE, 2),
				BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.ESPACIO, Tema.MARGEN, Tema.ESPACIO)));
	}

	private void seccion(String titulo, JComponent contenido) {
		JLabel etiqueta = Tema.etiqueta(titulo, Font.BOLD, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE);
		etiqueta.setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, 0, Tema.ESPACIO / 2, 0));
		etiqueta.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		contenido.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		cuerpo.add(etiqueta);
		cuerpo.add(contenido);
	}

	private JPanel panelSliderRondas() {
		sliderRondas.setName("rondas");
		sliderRondas.setOpaque(false);
		sliderRondas.setMajorTickSpacing(1);
		sliderRondas.setPaintTicks(true);
		sliderRondas.setPaintLabels(true);
		sliderRondas.setSnapToTicks(true);
		sliderRondas.addChangeListener(e -> actualizar());
		JPanel panel = Tema.panelBorde(new BorderLayout(Tema.ESPACIO, 0), 0, 0);
		panel.add(sliderRondas, BorderLayout.CENTER);
		panel.add(etiquetaRondas, BorderLayout.EAST);
		return panel;
	}

	private void actualizar() {
		int rondas = cantidadRondas();
		String sufijo = rondas + (rondas == 1 ? " ronda" : " rondas");
		etiquetaRondas.setText(sufijo);
		Dificultad d = dificultadSeleccionada;
		estadoMensaje.setText(DIFICULTADES[d.ordinal()][0]
				+ " " + d.tamano() + "×" + d.tamano() + " · Al mejor de " + sufijo);
		estadoMensaje.setForeground(Tema.TINTA_SUAVE);
	}

	private static String textoOpcion(Enum<?> valor) {
		String cuerpo;
		if (valor instanceof ModoJuego m) {
			cuerpo = m == ModoJuego.UN_JUGADOR ? "<b>Un jugador</b><br><span style='color:#4A5073'>A tu ritmo."
					: "<b>Multijugador</b><br><span style='color:#4A5073'>Jugá con otras personas.";
		} else {
			Dificultad d = (Dificultad) valor;
			cuerpo = "<b>" + DIFICULTADES[d.ordinal()][0] + "<br><big>" + d.tamano() + "×" + d.tamano()
					+ "</big></b><br><span style='color:#4A5073'>" + DIFICULTADES[d.ordinal()][1];
		}
		return "<html><div style='text-align:center'>" + cuerpo + "</span></div></html>";
	}
}
