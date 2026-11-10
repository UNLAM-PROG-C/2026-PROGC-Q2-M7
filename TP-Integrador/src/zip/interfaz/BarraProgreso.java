package zip.interfaz;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

final class BarraProgreso extends JComponent {
	private static final long serialVersionUID = 1L;
	private double valor;

	BarraProgreso() {
		Dimension dimension = new Dimension(0, Tema.ALTO_PROGRESO);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setOpaque(false);
	}

	void establecerValor(double valor) {
		this.valor = Math.max(0, Math.min(1, valor));
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			dibujo.setColor(Tema.BORDE);
			dibujo.fill(Tema.redondeado(0, 0, getWidth(), getHeight(), Tema.RADIO_PROGRESO));
			dibujo.setColor(Tema.ACENTO);
			dibujo.fill(Tema.redondeado(0, 0, (float) (getWidth() * valor),
					getHeight(), Tema.RADIO_PROGRESO));
		} finally {
			dibujo.dispose();
		}
	}
}
