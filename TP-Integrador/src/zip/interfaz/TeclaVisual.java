package zip.interfaz;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

final class TeclaVisual extends JComponent {
	private static final long serialVersionUID = 1L;
	private final String texto;
	private final TipoIcono icono;

	TeclaVisual(String texto, int ancho, int alto) {
		this(texto, null, ancho, alto);
	}

	TeclaVisual(TipoIcono icono, int tamano) {
		this(null, icono, tamano, tamano);
	}

	private TeclaVisual(String texto, TipoIcono icono, int ancho, int alto) {
		this.texto = texto;
		this.icono = icono;
		Dimension dimension = new Dimension(ancho, alto + Tema.ALTO_SOMBRA);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			pintarTecla(dibujo);
			pintarEtiqueta(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void pintarTecla(Graphics2D dibujo) {
		float alto = getHeight() - Tema.ALTO_SOMBRA;
		dibujo.setColor(Tema.TINTA);
		dibujo.fill(Tema.redondeado(0, Tema.ALTO_SOMBRA, getWidth(), alto,
				Tema.RADIO_TECLA));
		Tema.rellenarBorde(dibujo, 0, 0, getWidth(), alto, Tema.RADIO_TECLA,
				Tema.TINTA, Tema.TARJETA);
	}

	private void pintarEtiqueta(Graphics2D dibujo) {
		float altoVisual = getHeight() - Tema.ALTO_SOMBRA;
		if (icono != null) {
			float tamanoIcono = Math.min(getWidth(), altoVisual) * Tema.PROPORCION_ICONO_TECLA;
			IconoLineal.pintar(dibujo, icono, (getWidth() - tamanoIcono) / 2,
					(altoVisual - tamanoIcono) / 2, tamanoIcono, Tema.TINTA);
			return;
		}
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TEXTO_TECLA, true));
		dibujo.setColor(Tema.TINTA);
		FontMetrics metricas = dibujo.getFontMetrics();
		float x = (getWidth() - metricas.stringWidth(texto)) / 2.0f;
		float y = (altoVisual + metricas.getAscent() - metricas.getDescent()) / 2.0f;
		dibujo.drawString(texto, x, y);
	}
}
