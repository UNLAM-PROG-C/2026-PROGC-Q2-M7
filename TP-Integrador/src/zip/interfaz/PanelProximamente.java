package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

final class PanelProximamente {
	private final JPanel panel = new JPanel(new BorderLayout());
	private final BotonPrincipal solo = new BotonPrincipal("Jugar en solitario");
	private final BotonFantasma volver = new BotonFantasma("Volver al menú", false);

	PanelProximamente(Runnable jugarSolo, Runnable volverAlMenu) {
		panel.setName("proximamente");
		panel.setBackground(Tema.FONDO);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU, Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU));
		solo.setName("jugar-solo");
		volver.setName("proximamente-volver");
		solo.agregarEscucha(evento -> jugarSolo.run());
		volver.agregarEscucha(evento -> volverAlMenu.run());
		panel.add(contenido(), BorderLayout.CENTER);
	}

	private JPanel contenido() {
		JPanel contenido = vertical();
		contenido.add(Box.createVerticalGlue());
		agregarIntroduccion(contenido);
		agregarAcciones(contenido);
		contenido.add(Box.createVerticalGlue());
		return contenido;
	}

	private void agregarIntroduccion(JPanel contenido) {
		contenido.add(centrado(new IconoConstruccion()));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_ICONO_PROXIMAMENTE));
		contenido.add(centrado(insignia()));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_INSIGNIA_PROXIMAMENTE));
		contenido.add(centrado(titulo()));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_TITULO_PROXIMAMENTE));
		contenido.add(centrado(descripcion()));
	}

	private void agregarAcciones(JPanel contenido) {
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_DESCRIPCION_PROXIMAMENTE));
		contenido.add(centrado(principal()));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_BOTON_PROXIMAMENTE));
		contenido.add(centrado(volver));
	}

	private JLabel insignia() {
		JLabel insignia = new EtiquetaPastilla("PRÓXIMAMENTE");
		insignia.setForeground(Tema.SOMBRA_ACENTO);
		insignia.setFont(Tema.fuenteConEspaciado(Tema.TAMANO_TITULO_SECCION, true,
				Tema.ESPACIADO_SECCION));
		insignia.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_INSIGNIA,
				Tema.RELLENO_HORIZONTAL_INSIGNIA, Tema.RELLENO_VERTICAL_INSIGNIA,
				Tema.RELLENO_HORIZONTAL_INSIGNIA));
		return insignia;
	}

	private JLabel titulo() {
		JLabel titulo = etiqueta("Multijugador en construcción",
				Tema.TAMANO_TITULO_PROXIMAMENTE, true, Tema.TINTA);
		titulo.setHorizontalAlignment(SwingConstants.CENTER);
		return titulo;
	}

	private JTextPane descripcion() {
		JTextPane texto = new JTextPane();
		texto.setText("Todavía no está disponible. Estamos trabajando para que puedas "
				+ "jugar con otras personas. Mientras tanto, probá el modo un jugador.");
		configurarDescripcion(texto);
		return texto;
	}

	private void configurarDescripcion(JTextPane texto) {
		texto.setName("proximamente-descripcion");
		texto.setEditable(false);
		texto.setFocusable(false);
		texto.setOpaque(false);
		texto.setForeground(Tema.TINTA_SUAVE);
		texto.setFont(Tema.fuente(Tema.TAMANO_TEXTO_PROXIMAMENTE, false));
		texto.setAlignmentX(Component.CENTER_ALIGNMENT);
		texto.setMaximumSize(new Dimension(Tema.ANCHO_CONTENIDO_PROXIMAMENTE, Short.MAX_VALUE));
		texto.setPreferredSize(new Dimension(Tema.ANCHO_CONTENIDO_PROXIMAMENTE,
				Tema.ALTO_DESCRIPCION_PROXIMAMENTE));
		centrarTexto(texto);
	}

	private void centrarTexto(JTextPane texto) {
		SimpleAttributeSet atributos = new SimpleAttributeSet();
		StyleConstants.setAlignment(atributos, StyleConstants.ALIGN_CENTER);
		texto.getStyledDocument().setParagraphAttributes(0,
				texto.getStyledDocument().getLength(), atributos, false);
	}

	private BotonPrincipal principal() {
		solo.setPreferredSize(new Dimension(Tema.ANCHO_BOTON_PROXIMAMENTE,
				solo.getPreferredSize().height));
		solo.setMaximumSize(solo.getPreferredSize());
		return solo;
	}

	private JPanel centrado(Component componente) {
		JPanel envoltorio = horizontal();
		envoltorio.setMaximumSize(new Dimension(Integer.MAX_VALUE,
				componente.getPreferredSize().height));
		envoltorio.add(Box.createHorizontalGlue());
		envoltorio.add(componente);
		envoltorio.add(Box.createHorizontalGlue());
		return envoltorio;
	}

	private static JPanel vertical() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return panel;
	}

	private static JPanel horizontal() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		return panel;
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita,
			java.awt.Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Tema.fuente(tamano, negrita));
		etiqueta.setForeground(color);
		return etiqueta;
	}

	JPanel componente() {
		return panel;
	}
}
