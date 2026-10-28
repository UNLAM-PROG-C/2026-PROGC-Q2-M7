package zip.interfaz;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

final class BotonSecundario extends ControlAccion {
	private static final long serialVersionUID = 1L;
	private String texto;
	private final TipoIcono icono;

	BotonSecundario(String texto, TipoIcono icono) {
		super(texto);
		this.texto = texto;
		this.icono = icono;
		Dimension tamano = new Dimension(0, Tema.ALTO_SECUNDARIO);
		setPreferredSize(tamano);
		setMinimumSize(tamano);
	}

	void cambiarTexto(String texto) {
		this.texto = texto;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			aplicarTransparenciaDeshabilitado(dibujo);
			pintarBoton(dibujo);
			pintarContenido(dibujo);
			pintarFoco(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void aplicarTransparenciaDeshabilitado(Graphics2D dibujo) {
		if (!isEnabled()) {
			dibujo.setComposite(AlphaComposite.SrcOver.derive(Tema.TRANSPARENCIA_DESHABILITADO));
		}
	}

	private void pintarBoton(Graphics2D dibujo) {
		float x = Tema.MARGEN_CONTROL;
		float y = Tema.DESPLAZAMIENTO_FOCO + (estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : 0);
		float ancho = getWidth() - 2.0f * Tema.MARGEN_CONTROL;
		float alto = Tema.ALTO_VISUAL_SECUNDARIO - Tema.ALTO_SOMBRA;
		dibujo.setColor(estaEncima() ? Tema.ACENTO : Tema.BORDE);
		dibujo.fill(Tema.redondeado(x, y + Tema.ALTO_SOMBRA, ancho, alto,
				Tema.RADIO_MEDIO));
		Tema.rellenarBorde(dibujo, x, y, ancho, alto, Tema.RADIO_MEDIO,
				estaEncima() ? Tema.ACENTO : Tema.BORDE, Tema.TARJETA);
	}

	private void pintarContenido(Graphics2D dibujo) {
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TEXTO_SECUNDARIO, true));
		dibujo.setColor(Tema.TINTA);
		FontMetrics metricas = dibujo.getFontMetrics();
		int espacioIcono = icono == null ? 0 : Tema.TAMANO_ICONO_BOTON + Tema.SEPARACION_TECLAS;
		float izquierda = izquierdaContenido(metricas, espacioIcono);
		float centroY = Tema.DESPLAZAMIENTO_FOCO + Tema.ALTO_VISUAL_SECUNDARIO / 2.0f;
		if (icono != null) {
			IconoLineal.pintar(dibujo, icono, izquierda,
					centroY - Tema.TAMANO_ICONO_BOTON / 2.0f,
					Tema.TAMANO_ICONO_BOTON, Tema.TINTA);
		}
		float lineaBase = centroY + (metricas.getAscent() - metricas.getDescent()) / 2.0f;
		dibujo.drawString(texto, izquierda + espacioIcono, lineaBase);
	}

	private float izquierdaContenido(FontMetrics metricas, int espacioIcono) {
		float anchoContenido = metricas.stringWidth(texto) + espacioIcono;
		return (getWidth() - anchoContenido) / 2;
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (hasFocus()) {
			Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
					getWidth() - 2.0f * Tema.DESPLAZAMIENTO_FOCO,
					Tema.ALTO_VISUAL_SECUNDARIO, Tema.RADIO_MEDIO);
		}
	}
}
