package ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JButton;

final class BotonEstilo extends JButton {
	private static final long serialVersionUID = 1L;
	private static final int RELLENO_HORIZONTAL = 16;

	enum Estilo {
		PRINCIPAL,
		SECUNDARIO,
		FANTASMA
	}

	private final Estilo estilo;
	private final TipoIcono icono;

	private BotonEstilo(Estilo estilo, String texto, TipoIcono icono) {
		super(texto);
		this.estilo = estilo;
		this.icono = icono;
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setFont(Tema.fuente(Font.BOLD, tamanoTexto()));
	}

	static BotonEstilo principal(String texto) {
		return principal(texto, TipoIcono.FLECHA_DERECHA);
	}

	static BotonEstilo principal(String texto, TipoIcono icono) {
		return new BotonEstilo(Estilo.PRINCIPAL, texto, icono);
	}

	static BotonEstilo secundario(String texto) {
		return secundario(texto, null);
	}

	static BotonEstilo secundario(String texto, TipoIcono icono) {
		return new BotonEstilo(Estilo.SECUNDARIO, texto, icono);
	}

	static BotonEstilo fantasma(String texto) {
		return fantasma(texto, null);
	}

	static BotonEstilo fantasma(String texto, TipoIcono icono) {
		return new BotonEstilo(Estilo.FANTASMA, texto, icono);
	}

	@Override
	public Dimension getPreferredSize() {
		if (isPreferredSizeSet()) {
			return super.getPreferredSize();
		}
		FontMetrics metricas = getFontMetrics(getFont());
		int anchoTexto = metricas.stringWidth(getText());
		int anchoIcono = icono == null ? 0 : Tema.TAMANO_ICONO_BOTON + separacionIcono();
		int ancho = anchoTexto + anchoIcono + 2 * RELLENO_HORIZONTAL + 2 * Tema.MARGEN_CONTROL;
		return new Dimension(ancho, altoComponente());
	}

	@Override
	public Dimension getMinimumSize() {
		if (isMinimumSizeSet()) {
			return super.getMinimumSize();
		}
		return getPreferredSize();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			if (estilo != Estilo.FANTASMA) {
				Tema.aplicarEstadoHabilitado(dibujo, isEnabled());
			}
			pintarFondo(dibujo);
			pintarContenido(dibujo);
			pintarFoco(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void pintarFondo(Graphics2D dibujo) {
		if (estilo == Estilo.FANTASMA) {
			return;
		}
		int x = Tema.MARGEN_CONTROL;
		int ancho = getWidth() - 2 * Tema.MARGEN_CONTROL;
		if (estilo == Estilo.PRINCIPAL) {
			pintarPrincipal(dibujo, x, ancho);
			return;
		}
		pintarSecundario(dibujo, x, ancho);
	}

	private void pintarPrincipal(Graphics2D dibujo, int x, int ancho) {
		int presion = estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : 0;
		int sombra = estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : Tema.ALTO_SOMBRA;
		int altoFrente = Tema.ALTO_VISUAL_PRINCIPAL - sombra;
		Tema.pintarCajaRedondeada(dibujo, x, Tema.DESPLAZAMIENTO_FOCO + sombra, ancho,
				altoFrente, Tema.RADIO_GRANDE, Tema.SOMBRA_ACENTO, null, 0);
		Tema.pintarCajaRedondeada(dibujo, x, Tema.DESPLAZAMIENTO_FOCO + presion, ancho,
				altoFrente, Tema.RADIO_GRANDE, Tema.ACENTO, null, 0);
	}

	private void pintarSecundario(Graphics2D dibujo, int x, int ancho) {
		int presion = estaPresionado() ? Tema.DESPLAZAMIENTO_PRESIONADO : 0;
		int y = Tema.DESPLAZAMIENTO_FOCO + presion;
		int alto = Tema.ALTO_VISUAL_SECUNDARIO - Tema.ALTO_SOMBRA;
		Color borde = estaEncima() ? Tema.ACENTO : Tema.BORDE;
		Tema.pintarCajaRedondeada(dibujo, x, y + Tema.ALTO_SOMBRA, ancho, alto,
				Tema.RADIO_MEDIO, borde, null, 0);
		Tema.pintarCajaRedondeada(dibujo, x, y, ancho, alto,
				Tema.RADIO_MEDIO, Tema.TARJETA, borde, Tema.GROSOR_BORDE);
	}

	private void pintarContenido(Graphics2D dibujo) {
		dibujo.setFont(getFont());
		dibujo.setColor(colorContenido());
		FontMetrics metricas = dibujo.getFontMetrics();
		int anchoTexto = metricas.stringWidth(getText());
		int anchoIcono = icono == null ? 0 : Tema.TAMANO_ICONO_BOTON + separacionIcono();
		float izquierda = (getWidth() - (anchoTexto + anchoIcono)) / 2.0f;
		float centroY = centroVertical();
		float lineaBase = centroY + (metricas.getAscent() - metricas.getDescent()) / 2.0f;
		float textoX = iconoALaDerecha() ? izquierda : izquierda + anchoIcono;
		dibujo.drawString(getText(), textoX, lineaBase);
		pintarIcono(dibujo, izquierda, anchoTexto, centroY);
		pintarSubrayado(dibujo, textoX, textoX + anchoTexto, lineaBase);
	}

	private void pintarIcono(Graphics2D dibujo, float izquierda, int anchoTexto, float centroY) {
		if (icono == null) {
			return;
		}
		float iconoX = iconoALaDerecha() ? izquierda + anchoTexto + separacionIcono() : izquierda;
		IconoLineal.pintar(dibujo, icono, iconoX, centroY - Tema.TAMANO_ICONO_BOTON / 2.0f,
				Tema.TAMANO_ICONO_BOTON, dibujo.getColor());
	}

	private void pintarSubrayado(Graphics2D dibujo, float izquierda, float derecha,
			float lineaBase) {
		if (estilo == Estilo.FANTASMA && estaEncima()) {
			int y = Math.round(lineaBase + Tema.SEPARACION_MINIMA);
			dibujo.drawLine(Math.round(izquierda), y, Math.round(derecha), y);
		}
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (!hasFocus()) {
			return;
		}
		int altoFoco = estilo == Estilo.FANTASMA
				? getHeight() - 2 * Tema.DESPLAZAMIENTO_FOCO : altoVisual();
		int radio = estilo == Estilo.PRINCIPAL ? Tema.RADIO_GRANDE : Tema.RADIO_MEDIO;
		Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
				getWidth() - 2.0f * Tema.DESPLAZAMIENTO_FOCO, altoFoco, radio);
	}

	private boolean estaEncima() {
		return getModel().isRollover() && isEnabled();
	}

	private boolean estaPresionado() {
		return getModel().isPressed() && getModel().isArmed() && isEnabled();
	}

	private boolean iconoALaDerecha() {
		return estilo == Estilo.PRINCIPAL && icono != null;
	}

	private int separacionIcono() {
		return estilo == Estilo.PRINCIPAL ? Tema.SEPARACION_GRANDE : Tema.SEPARACION_CHICA;
	}

	private int tamanoTexto() {
		return estilo == Estilo.PRINCIPAL
				? Tema.TAMANO_TEXTO_PRINCIPAL : Tema.TAMANO_TEXTO_SECUNDARIO;
	}

	private int altoComponente() {
		return switch (estilo) {
			case PRINCIPAL -> Tema.ALTO_PRINCIPAL;
			case SECUNDARIO -> Tema.ALTO_SECUNDARIO;
			case FANTASMA -> Tema.ALTO_FANTASMA;
		};
	}

	private int altoVisual() {
		return estilo == Estilo.PRINCIPAL
				? Tema.ALTO_VISUAL_PRINCIPAL : Tema.ALTO_VISUAL_SECUNDARIO;
	}

	private float centroVertical() {
		if (estilo == Estilo.FANTASMA) {
			return getHeight() / 2.0f;
		}
		return Tema.DESPLAZAMIENTO_FOCO + altoVisual() / 2.0f;
	}

	private Color colorContenido() {
		return switch (estilo) {
			case PRINCIPAL -> Tema.TARJETA;
			case SECUNDARIO -> Tema.TINTA;
			case FANTASMA -> isEnabled() ? Tema.TINTA_SUAVE : Tema.BORDE;
		};
	}
}
