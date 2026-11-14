package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

final class TarjetaOpcion<T> extends JToggleButton {
	private static final long serialVersionUID = 1L;
	private static final String ACCION_ACTIVAR = "activarTarjeta";
	private final transient T valor;

	TarjetaOpcion(T valor, JComponent contenido, String nombreAccesible,
			String descripcionAccesible) {
		this.valor = valor;
		setContentAreaFilled(false);
		setBorderPainted(false);
		setFocusPainted(false);
		setOpaque(false);
		setRolloverEnabled(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, Tema.ESPACIO_RADIO));
		getAccessibleContext().setAccessibleName(nombreAccesible);
		getAccessibleContext().setAccessibleDescription(descripcionAccesible);
		add(contenido, BorderLayout.CENTER);
		vincularEnter();
	}

	T valor() {
		return valor;
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			Tema.aplicarEstadoHabilitado(dibujo, isEnabled());
			pintarTarjeta(dibujo);
			pintarRadio(dibujo);
			pintarFoco(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void vincularEnter() {
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), ACCION_ACTIVAR);
		getActionMap().put(ACCION_ACTIVAR, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent evento) {
				if (isEnabled()) {
					doClick();
				}
			}
		});
	}

	private void pintarTarjeta(Graphics2D dibujo) {
		if (isSelected()) {
			dibujo.setColor(Tema.conTransparencia(Tema.ACENTO, Tema.TRANSPARENCIA_HALO));
			dibujo.fill(Tema.redondeado(Tema.MARGEN_CONTROL, Tema.MARGEN_CONTROL,
					getWidth() - 2.0f * Tema.MARGEN_CONTROL,
					getHeight() - 2.0f * Tema.MARGEN_CONTROL, Tema.RADIO_GRANDE));
		}
		int margen = Tema.MARGEN_CONTROL + Tema.HALO_TARJETA;
		boolean encima = getModel().isRollover() && isEnabled();
		Color borde = isSelected() || encima ? Tema.ACENTO : Tema.BORDE;
		Color relleno = isSelected() ? Tema.TARJETA : Tema.TARJETA_INACTIVA;
		Tema.pintarCajaRedondeada(dibujo, margen, margen, getWidth() - 2 * margen,
				getHeight() - 2 * margen, Tema.RADIO_GRANDE, relleno, borde, Tema.GROSOR_BORDE);
	}

	private void pintarRadio(Graphics2D dibujo) {
		int margen = Tema.MARGEN_CONTROL + Tema.HALO_TARJETA;
		int x = getWidth() - margen - Tema.RELLENO_TARJETA - Tema.TAMANO_RADIO;
		int y = margen + Tema.RELLENO_TARJETA;
		dibujo.setColor(isSelected() ? Tema.ACENTO : Tema.BORDE);
		dibujo.fillOval(x, y, Tema.TAMANO_RADIO, Tema.TAMANO_RADIO);
		dibujo.setColor(isSelected() ? Tema.TARJETA : Tema.TARJETA_INACTIVA);
		int interior = Tema.TAMANO_RADIO - 2 * Tema.GROSOR_BORDE;
		dibujo.fillOval(x + Tema.GROSOR_BORDE, y + Tema.GROSOR_BORDE, interior, interior);
		pintarPuntoRadio(dibujo, x, y);
	}

	private void pintarPuntoRadio(Graphics2D dibujo, int x, int y) {
		if (isSelected()) {
			int desplazamiento = (Tema.TAMANO_RADIO - Tema.PUNTO_RADIO) / 2;
			dibujo.setColor(Tema.ACENTO);
			dibujo.fillOval(x + desplazamiento, y + desplazamiento,
					Tema.PUNTO_RADIO, Tema.PUNTO_RADIO);
		}
	}

	private void pintarFoco(Graphics2D dibujo) {
		if (hasFocus()) {
			Tema.pintarFoco(dibujo, Tema.DESPLAZAMIENTO_FOCO, Tema.DESPLAZAMIENTO_FOCO,
					getWidth() - 2.0f * Tema.DESPLAZAMIENTO_FOCO,
					getHeight() - 2.0f * Tema.DESPLAZAMIENTO_FOCO, Tema.RADIO_GRANDE);
		}
	}
}
