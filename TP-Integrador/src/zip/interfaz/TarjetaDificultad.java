package zip.interfaz;

import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import zip.dominio.Dificultad;

final class TarjetaDificultad extends TarjetaSeleccionable {
	private static final long serialVersionUID = 1L;
	private final Dificultad dificultad;
	private final transient Texto copia;

	TarjetaDificultad(Dificultad dificultad) {
		this(dificultad, copia(dificultad));
	}

	private TarjetaDificultad(Dificultad dificultad, Texto copia) {
		super(contenido(dificultad, copia), copia.titulo(), copia.descripcion());
		this.dificultad = dificultad;
		this.copia = copia;
		setPreferredSize(new Dimension(0, Tema.ALTO_TARJETA_DIFICULTAD));
		setMinimumSize(getPreferredSize());
	}

	Dificultad dificultad() {
		return dificultad;
	}

	String titulo() {
		return copia.titulo();
	}

	static String titulo(Dificultad dificultad) {
		return copia(dificultad).titulo();
	}

	private static JPanel contenido(Dificultad dificultad, Texto copia) {
		JPanel panel = new JPanel();
		configurarContenido(panel);
		panel.add(etiqueta(copia.titulo(), Tema.TAMANO_TITULO_TARJETA, true, Tema.TINTA));
		panel.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		panel.add(etiqueta(dificultad.tamano() + "×" + dificultad.tamano(),
				Tema.TAMANO_VALOR_TARJETA, true, Tema.TINTA));
		panel.add(Box.createVerticalGlue());
		panel.add(etiqueta(copia.primeraLinea(),
				Tema.TAMANO_TEXTO_TARJETA, false, Tema.TINTA_SUAVE));
		panel.add(etiqueta(copia.segundaLinea(),
				Tema.TAMANO_TEXTO_TARJETA, false, Tema.TINTA_SUAVE));
		return panel;
	}

	private static void configurarContenido(JPanel panel) {
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.SEPARACION_TITULO_SECCION,
				Tema.RELLENO_TARJETA, Tema.SEPARACION_TITULO_SECCION,
				Tema.RELLENO_TARJETA + Tema.ESPACIO_RADIO));
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita,
			java.awt.Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Tema.fuente(tamano, negrita));
		etiqueta.setForeground(color);
		etiqueta.setAlignmentX(LEFT_ALIGNMENT);
		return etiqueta;
	}

	private static Texto copia(Dificultad dificultad) {
		return switch (dificultad) {
			case FACIL -> new Texto("Fácil", "Tablero chico, recorrido directo.",
					"Tablero chico,", "recorrido directo.");
			case MEDIA -> new Texto("Media", "Tablero mediano, hay que planificar.",
					"Tablero mediano,", "hay que planificar.");
			case DIFICIL -> new Texto("Difícil", "Tablero grande, cada giro cuenta.",
					"Tablero grande,", "cada giro cuenta.");
		};
	}

	private record Texto(String titulo, String descripcion, String primeraLinea,
			String segundaLinea) {
	}
}
