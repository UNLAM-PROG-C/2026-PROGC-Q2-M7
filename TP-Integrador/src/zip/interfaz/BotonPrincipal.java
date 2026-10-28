package zip.interfaz;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

final class BotonPrincipal extends ControlAccion {
	private static final long serialVersionUID = 1L;
	private String texto;

	BotonPrincipal(String texto) {
		super(texto);
		this.texto = texto;
		Dimension tamano = new Dimension(0, Tema.ALTO_PRINCIPAL);
		setPreferredSize(tamano);
		setMinimumSize(tamano);
	}

	void cambiarTexto(String texto) {
		this.texto = texto;
		getAccessibleContext().setAccessibleName(texto);
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
		float superior = Tema.DESPLAZAMIENTO_FOCO;
		float presion = estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : 0;
		float sombra = estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : Tema.ALTO_SOMBRA;
		float ancho = getWidth() - 2.0f * Tema.MARGEN_CONTROL;
		float altoFrente = Tema.ALTO_VISUAL_PRINCIPAL - sombra;
		dibujo.setColor(Tema.SOMBRA_ACENTO);
		dibujo.fill(Tema.redondeado(x, superior + sombra, ancho, altoFrente, Tema.RADIO_GRANDE));
		dibujo.setColor(Tema.ACENTO);
		dibujo.fill(Tema.redondeado(x, superior + presion, ancho, altoFrente, Tema.RADIO_GRANDE));
	}

	private void pintarContenido(Graphics2D dibujo) {
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TEXTO_PRINCIPAL, true));
		dibujo.setColor(Tema.TARJETA);
		FontMetrics metricas = dibujo.getFontMetrics();
		float anchoContenido = metricas.stringWidth(texto) + Tema.TAMANO_ICONO_BOTON
				+ Tema.SEPARACION_TARJETAS;
		float izquierda = (getWidth() - anchoContenido) / 2;
		float centroY = Tema.DESPLAZAMIENTO_FOCO + Tema.ALTO_VISUAL_PRINCIPAL / 2.0f;
		float lineaBase = centroY + (metricas.getAscent() - metricas.getDescent()) / 2.0f;
		dibujo.drawString(texto, izquierda, lineaBase);
		IconoLineal.pintar(dibujo, TipoIcono.FLECHA_DERECHA,
				izquierda + metricas.stringWidth(texto) + Tema.SEPARACION_TARJETAS,
				centroY - Tema.TAMANO_ICONO_BOTON / 2.0f,
				Tema.TAMANO_ICONO_BOTON, Tema.TARJETA);
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (hasFocus()) {
			Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
					getWidth() - 2.0f * Tema.DESPLAZAMIENTO_FOCO,
					Tema.ALTO_VISUAL_PRINCIPAL, Tema.RADIO_GRANDE);
		}
	}
}
