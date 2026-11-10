package zip.interfaz;

import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class TarjetaControles extends TarjetaSeleccionable {
	private static final long serialVersionUID = 1L;
	private final Controles controles;

	TarjetaControles(Controles controles) {
		super(contenido(controles), controles.toString(), "Elegir controles " + controles);
		this.controles = controles;
		setPreferredSize(new Dimension(0, Tema.ALTO_TARJETA_CONTROLES));
		setMinimumSize(getPreferredSize());
	}

	Controles controles() {
		return controles;
	}

	private static JPanel contenido(Controles controles) {
		JPanel panel = new JPanel(new java.awt.BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.SEPARACION_TITULO_SECCION,
				Tema.RELLENO_TARJETA, Tema.RELLENO_INFERIOR_TECLAS,
				Tema.RELLENO_TARJETA + Tema.ESPACIO_RADIO));
		JLabel titulo = new JLabel(controles.toString());
		titulo.setFont(Tema.fuente(Tema.TAMANO_TITULO_TARJETA, true));
		titulo.setForeground(Tema.TINTA);
		panel.add(titulo, java.awt.BorderLayout.NORTH);
		panel.add(teclado(controles), java.awt.BorderLayout.CENTER);
		return panel;
	}

	private static JPanel teclado(Controles controles) {
		JPanel teclado = new JPanel();
		teclado.setOpaque(false);
		teclado.setLayout(new javax.swing.BoxLayout(teclado, javax.swing.BoxLayout.Y_AXIS));
		teclado.setAlignmentX(CENTER_ALIGNMENT);
		teclado.add(centrado(tecla(controles, true, 0)));
		teclado.add(Box.createVerticalStrut(Tema.SEPARACION_TECLAS));
		teclado.add(teclasInferiores(controles));
		return teclado;
	}

	private static JPanel teclasInferiores(Controles controles) {
		JPanel inferior = horizontal();
		for (int indice = 1; indice < Tema.CANTIDAD_TECLAS_CONTROL; indice++) {
			if (indice > 1) {
				inferior.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
			}
			inferior.add(tecla(controles, false, indice));
		}
		return inferior;
	}

	private static TeclaVisual tecla(Controles controles, boolean superior, int indice) {
		if (controles == Controles.WASD) {
			String[] teclas = {"W", "A", "S", "D"};
			return new TeclaVisual(teclas[indice], Tema.TAMANO_TECLA_MENU, Tema.TAMANO_TECLA_MENU);
		}
		TipoIcono[] arrows = {
			TipoIcono.FLECHA_ARRIBA, TipoIcono.FLECHA_IZQUIERDA, TipoIcono.FLECHA_ABAJO, TipoIcono.FLECHA_DERECHA
		};
		return new TeclaVisual(arrows[indice], Tema.TAMANO_TECLA_MENU);
	}

	private static JPanel centrado(java.awt.Component componente) {
		JPanel fila = horizontal();
		fila.add(Box.createHorizontalGlue());
		fila.add(componente);
		fila.add(Box.createHorizontalGlue());
		return fila;
	}

	private static JPanel horizontal() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS));
		panel.setAlignmentX(CENTER_ALIGNMENT);
		return panel;
	}
}
