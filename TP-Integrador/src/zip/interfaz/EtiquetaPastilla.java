package zip.interfaz;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JLabel;

final class EtiquetaPastilla extends JLabel {
	private static final long serialVersionUID = 1L;
	EtiquetaPastilla(String texto) {
		super(texto);
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			dibujo.setColor(Tema.ACENTO_SUAVE);
			dibujo.fill(Tema.redondeado(0, 0, getWidth(), getHeight(), Tema.RADIO_INSIGNIA));
		} finally {
			dibujo.dispose();
		}
		super.paintComponent(graficos);
	}

	@Override
	public Dimension getMaximumSize() {
		return getPreferredSize();
	}
}
