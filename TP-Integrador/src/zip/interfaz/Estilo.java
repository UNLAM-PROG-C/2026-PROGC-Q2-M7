package zip.interfaz;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class Estilo {
	static final Color FONDO = new Color(0xF3F6FC);
	static final Color TINTA = new Color(0x19233B);
	static final Color ACENTO = new Color(0x365CE5);
	static final Color VISITADA = new Color(0xE4EAFF);
	static final Color INICIO = new Color(0xCDEDDD);
	static final Color FIN = new Color(0xE6DCFF);
	static final Color ERROR = new Color(0xA43535);
	static final int ESPACIO = 16;
	static final int TAMANO_TITULO = 36;
	static final int TAMANO_TEXTO = 16;
	static final int TAMANO_PEQUENO = 13;

	private Estilo() {
	}

	static JLabel etiqueta(String texto, int tamano) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, tamano));
		etiqueta.setForeground(TINTA);
		return etiqueta;
	}

	static void decorar(JPanel panel) {
		panel.setBackground(FONDO);
		panel.setBorder(BorderFactory.createEmptyBorder(ESPACIO, ESPACIO, ESPACIO, ESPACIO));
	}
}
