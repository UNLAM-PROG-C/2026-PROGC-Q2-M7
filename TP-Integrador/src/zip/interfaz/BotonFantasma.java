package zip.interfaz;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

final class BotonFantasma extends ControlAccion {
	private static final long serialVersionUID = 1L;
	private final String texto;
	private final boolean flecha;

	BotonFantasma(String texto, boolean flecha) {
		super(texto);
		this.texto = texto;
		this.flecha = flecha;
		Dimension tamano = new Dimension(0, Tema.ALTO_FANTASMA);
		setPreferredSize(tamano);
		setMinimumSize(tamano);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			pintarContenido(dibujo);
			pintarFoco(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void pintarContenido(Graphics2D dibujo) {
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TEXTO_SECUNDARIO, true));
		dibujo.setColor(isEnabled() ? Tema.TINTA_SUAVE : Tema.BORDE);
		FontMetrics metricas = dibujo.getFontMetrics();
		int espacioIcono = flecha ? Tema.TAMANO_ICONO_BOTON + Tema.SEPARACION_TECLAS : 0;
		float ancho = metricas.stringWidth(texto) + espacioIcono;
		float izquierda = (getWidth() - ancho) / 2;
		float lineaBase = (getHeight() + metricas.getAscent() - metricas.getDescent()) / 2.0f;
		pintarFlecha(dibujo, izquierda);
		dibujo.drawString(texto, izquierda + espacioIcono, lineaBase);
		pintarSubrayado(dibujo, izquierda + espacioIcono, izquierda + ancho, lineaBase);
	}

	private void pintarFlecha(Graphics2D dibujo, float izquierda) {
		if (flecha) {
			IconoLineal.pintar(dibujo, TipoIcono.FLECHA_IZQUIERDA, izquierda,
					(getHeight() - Tema.TAMANO_ICONO_BOTON) / 2.0f,
					Tema.TAMANO_ICONO_BOTON, dibujo.getColor());
		}
	}

	private void pintarSubrayado(Graphics2D dibujo, float izquierda, float derecha,
			float lineaBase) {
		if (estaEncima()) {
			dibujo.drawLine(Math.round(izquierda), Math.round(lineaBase + Tema.DESPLAZAMIENTO_FOCO),
					Math.round(derecha), Math.round(lineaBase + Tema.DESPLAZAMIENTO_FOCO));
		}
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (hasFocus()) {
			Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
					getWidth() - 2.0f * Tema.DESPLAZAMIENTO_FOCO,
					getHeight() - 2.0f * Tema.DESPLAZAMIENTO_FOCO, Tema.RADIO_MEDIO);
		}
	}
}
