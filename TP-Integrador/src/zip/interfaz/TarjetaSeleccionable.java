package zip.interfaz;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;

class TarjetaSeleccionable extends ControlAccion {
	private static final long serialVersionUID = 1L;
	private transient GrupoSeleccion grupo;
	private boolean seleccionada;

	TarjetaSeleccionable(JComponent contenido, String nombreAccesible, String descripcionAccesible) {
		super(nombreAccesible);
		getAccessibleContext().setAccessibleDescription(descripcionAccesible);
		setLayout(new java.awt.BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		setOpaque(false);
		add(contenido);
	}

	final void seleccionar(boolean seleccionada) {
		if (seleccionada && grupo != null) {
			grupo.seleccionar(this);
			return;
		}
		aplicarSeleccion(seleccionada);
	}

	final boolean estaSeleccionada() {
		return seleccionada;
	}

	final void unir(GrupoSeleccion grupo) {
		this.grupo = grupo;
	}

	final void aplicarSeleccion(boolean seleccionada) {
		this.seleccionada = seleccionada;
		cambioSeleccion(seleccionada);
		repaint();
	}

	@Override
	protected final void activar() {
		seleccionar(true);
		notificarAccion();
	}

	@Override
	protected final void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			aplicarTransparenciaDeshabilitado(dibujo);
			pintarTarjeta(dibujo);
			pintarRadio(dibujo);
			pintarFoco(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	protected void cambioSeleccion(boolean seleccionada) {
	}

	private void pintarTarjeta(Graphics2D dibujo) {
		if (seleccionada) {
			dibujo.setColor(Tema.conTransparencia(Tema.ACENTO, Tema.TRANSPARENCIA_HALO));
			dibujo.fill(Tema.redondeado(Tema.MARGEN_CONTROL, Tema.MARGEN_CONTROL,
					anchoTarjeta(Tema.MARGEN_CONTROL), altoTarjeta(Tema.MARGEN_CONTROL),
					Tema.RADIO_GRANDE));
		}
		float margenCuerpo = Tema.MARGEN_CONTROL + Tema.HALO_TARJETA;
		Color borde = seleccionada || estaEncima() ? Tema.ACENTO : Tema.BORDE;
		Color relleno = seleccionada ? Tema.TARJETA : Tema.TARJETA_INACTIVA;
		Tema.rellenarBorde(dibujo, margenCuerpo, margenCuerpo, anchoTarjeta(margenCuerpo),
				altoTarjeta(margenCuerpo), Tema.RADIO_GRANDE, borde, relleno);
	}

	private void pintarRadio(Graphics2D dibujo) {
		float margenCuerpo = Tema.MARGEN_CONTROL + Tema.HALO_TARJETA;
		float x = getWidth() - margenCuerpo - Tema.RELLENO_TARJETA - Tema.TAMANO_RADIO;
		float y = margenCuerpo + Tema.RELLENO_TARJETA;
		Color color = seleccionada ? Tema.ACENTO : Tema.BORDE;
		pintarAnilloRadio(dibujo, x, y, color);
		pintarPuntoRadio(dibujo, x, y);
	}

	private void pintarAnilloRadio(Graphics2D dibujo, float x, float y, Color color) {
		dibujo.setColor(color);
		dibujo.fillOval(Math.round(x), Math.round(y), Tema.TAMANO_RADIO, Tema.TAMANO_RADIO);
		dibujo.setColor(seleccionada ? Tema.TARJETA : Tema.TARJETA_INACTIVA);
		int interior = Tema.TAMANO_RADIO - 2 * Tema.GROSOR_BORDE;
		dibujo.fillOval(Math.round(x) + Tema.GROSOR_BORDE,
				Math.round(y) + Tema.GROSOR_BORDE, interior, interior);
	}

	private void pintarPuntoRadio(Graphics2D dibujo, float x, float y) {
		if (seleccionada) {
			int desplazamiento = (Tema.TAMANO_RADIO - Tema.PUNTO_RADIO) / 2;
			dibujo.setColor(Tema.ACENTO);
			dibujo.fillOval(Math.round(x) + desplazamiento, Math.round(y) + desplazamiento,
					Tema.PUNTO_RADIO, Tema.PUNTO_RADIO);
		}
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (hasFocus()) {
			Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
					anchoTarjeta(Tema.DESPLAZAMIENTO_FOCO), altoTarjeta(Tema.DESPLAZAMIENTO_FOCO),
					Tema.RADIO_GRANDE);
		}
	}

	private void aplicarTransparenciaDeshabilitado(Graphics2D dibujo) {
		if (!isEnabled()) {
			dibujo.setComposite(AlphaComposite.SrcOver.derive(Tema.TRANSPARENCIA_DESHABILITADO));
		}
	}

	private float anchoTarjeta(float margen) {
		return getWidth() - 2 * margen;
	}

	private float altoTarjeta(float margen) {
		return getHeight() - 2 * margen;
	}

}
