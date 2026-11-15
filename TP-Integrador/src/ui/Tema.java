package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;

final class Tema {
	static final Color FONDO = new Color(0xEEF1FB);
	static final Color TINTA = new Color(0x1B1F3B);
	static final Color TINTA_SUAVE = new Color(0x4A5073);
	static final Color ACENTO = new Color(0x4F46E5);
	static final Color TARJETA = Color.WHITE;
	static final Color BORDE = new Color(0xC9CEE6);
	static final Color FONDO_TABLERO = new Color(0xDDE1F5);
	static final Color VISITADA = new Color(0xDAD7FB);
	static final Color ERROR = new Color(0xA43535);

	static final int ANCHO_VENTANA = 720;
	static final int ALTO_VENTANA = 860;
	static final int MARGEN = 24;
	static final int ESPACIO = 12;
	static final int TAMANO_TITULO = 28;
	static final int TAMANO_SUBTITULO = 16;
	static final int TAMANO_TEXTO = 14;
	static final int TAMANO_CHICO = 12;

	private static final int RELLENO_BOTON_V = 10;
	private static final int RELLENO_BOTON_H = 18;

	private Tema() {
	}

	static void aplicarDefaultsSwing() {
		Font texto = fuente(Font.PLAIN, TAMANO_TEXTO);
		Font negrita = fuente(Font.BOLD, TAMANO_TEXTO);
		UIManager.put("Panel.background", FONDO);
		UIManager.put("Label.font", texto);
		UIManager.put("Label.foreground", TINTA);
		UIManager.put("Button.font", negrita);
		UIManager.put("Button.background", TARJETA);
		UIManager.put("Button.foreground", TINTA);
		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
		aplicarDefaultsControles(texto);
		aplicarDefaultsFeedback(texto, negrita);
	}

	private static void aplicarDefaultsControles(Font texto) {
		UIManager.put("ToggleButton.font", texto);
		UIManager.put("ToggleButton.background", TARJETA);
		UIManager.put("ToggleButton.foreground", TINTA);
		UIManager.put("ToggleButton.select", VISITADA);
		UIManager.put("Slider.background", FONDO);
		UIManager.put("Slider.foreground", TINTA);
		UIManager.put("Slider.font", fuente(Font.BOLD, TAMANO_CHICO));
	}

	private static void aplicarDefaultsFeedback(Font texto, Font negrita) {
		UIManager.put("OptionPane.background", TARJETA);
		UIManager.put("OptionPane.messageForeground", TINTA);
		UIManager.put("OptionPane.messageFont", texto);
		UIManager.put("OptionPane.buttonFont", negrita);
		UIManager.put("ProgressBar.background", FONDO_TABLERO);
		UIManager.put("ProgressBar.foreground", ACENTO);
		UIManager.put("ProgressBar.selectionBackground", TINTA);
		UIManager.put("ProgressBar.selectionForeground", TARJETA);
		UIManager.put("ProgressBar.font", fuente(Font.BOLD, TAMANO_CHICO));
	}

	static Font fuente(int estilo, int tamano) {
		return new Font(Font.SANS_SERIF, estilo, tamano);
	}

	static JLabel etiqueta(String texto, int estilo, int tamano, Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(fuente(estilo, tamano));
		etiqueta.setForeground(color);
		return etiqueta;
	}

	static JLabel texto(String texto, int estilo, Color color) {
		return etiqueta(texto, estilo, TAMANO_TEXTO, color);
	}

	static JButton boton(String texto, boolean principal) {
		JButton boton = new JButton(texto);
		boton.setFont(fuente(principal ? Font.BOLD : Font.PLAIN, TAMANO_TEXTO));
		boton.setBackground(principal ? ACENTO : TARJETA);
		boton.setForeground(principal ? Color.WHITE : TINTA);
		boton.setOpaque(true);
		boton.setFocusPainted(false);
		boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		boton.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(principal ? ACENTO : BORDE),
				BorderFactory.createEmptyBorder(RELLENO_BOTON_V, RELLENO_BOTON_H,
						RELLENO_BOTON_V, RELLENO_BOTON_H)));
		return boton;
	}

	static Border relleno(int vertical, int horizontal) {
		return BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal);
	}

	static Border marco(Color color, int grosor, int vertical, int horizontal) {
		return BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(color, grosor), relleno(vertical, horizontal));
	}

	static JPanel panelBorde(LayoutManager layout, int vertical, int horizontal) {
		JPanel panel = new JPanel(layout);
		panel.setOpaque(false);
		panel.setBorder(relleno(vertical, horizontal));
		return panel;
	}
}
