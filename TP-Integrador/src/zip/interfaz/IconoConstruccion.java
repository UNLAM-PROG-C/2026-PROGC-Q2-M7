package zip.interfaz;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

final class IconoConstruccion extends JComponent {
	private static final long serialVersionUID = 1L;

	IconoConstruccion() {
		Dimension tamano = new Dimension(Tema.TAMANO_ICONO_CONSTRUCCION,
				Tema.TAMANO_ICONO_CONSTRUCCION);
		setPreferredSize(tamano);
		setMinimumSize(tamano);
		setMaximumSize(tamano);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			IconoLineal.pintar(dibujo, TipoIcono.ENGRANAJE, 0, 0,
					Math.min(getWidth(), getHeight()), Tema.ACENTO);
		} finally {
			dibujo.dispose();
		}
	}
}
