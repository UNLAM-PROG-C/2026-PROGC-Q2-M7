package ui;

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
import utils.Constantes;

final class PanelProximamente {
	private final JPanel panel = new JPanel(new BorderLayout());
	private final BotonPrincipal host = new BotonPrincipal("Crear partida (Host)");
	private final BotonPrincipal cliente = new BotonPrincipal("Unirse a partida (Cliente)");
	private final BotonFantasma volver = new BotonFantasma("Volver / Cancelar", false);
	private final JLabel estadoMensaje = etiqueta(" ", Tema.TAMANO_ESTADO, false, Tema.TINTA);

	PanelProximamente(Runnable crearHost, Runnable unirseCliente, Runnable volverAlMenu) {
		panel.setName("proximamente");
		panel.setBackground(Tema.FONDO);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU, Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU));
		host.setName("crear-host");
		cliente.setName("unirse-cliente");
		volver.setName("proximamente-volver");
		estadoMensaje.setName("estado-multijugador");
		estadoMensaje.setHorizontalAlignment(SwingConstants.CENTER);

		host.agregarEscucha(evento -> crearHost.run());
		cliente.agregarEscucha(evento -> unirseCliente.run());
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
		contenido.add(centrado(boton(host)));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		contenido.add(centrado(boton(cliente)));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		contenido.add(centrado(estadoMensaje));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_BOTON_PROXIMAMENTE));
		contenido.add(centrado(volver));
	}

	private JLabel insignia() {
		JLabel insignia = new EtiquetaPastilla("MULTIJUGADOR");
		insignia.setForeground(Tema.SOMBRA_ACENTO);
		insignia.setFont(Tema.fuenteConEspaciado(Tema.TAMANO_TITULO_SECCION, true,
				Tema.ESPACIADO_SECCION));
		insignia.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_INSIGNIA,
				Tema.RELLENO_HORIZONTAL_INSIGNIA, Tema.RELLENO_VERTICAL_INSIGNIA,
				Tema.RELLENO_HORIZONTAL_INSIGNIA));
		return insignia;
	}

	private JLabel titulo() {
		JLabel titulo = etiqueta("Multijugador en Red (TCP)",
				Tema.TAMANO_TITULO_PROXIMAMENTE, true, Tema.TINTA);
		titulo.setHorizontalAlignment(SwingConstants.CENTER);
		return titulo;
	}

	private JTextPane descripcion() {
		JTextPane texto = new JTextPane();
		texto.setText("Elegí si querés alojar la partida esperando en el puerto "
				+ Constantes.PUERTO + " o conectarte como cliente.");
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

	private BotonPrincipal boton(BotonPrincipal b) {
		b.setPreferredSize(new Dimension(Tema.ANCHO_BOTON_PROXIMAMENTE,
				b.getPreferredSize().height));
		b.setMaximumSize(b.getPreferredSize());
		return b;
	}

	void ocupado(boolean ocupado) {
		host.setEnabled(!ocupado);
		cliente.setEnabled(!ocupado);
		volver.setEnabled(true);
		host.cambiarTexto(ocupado ? "Conectando…" : "Crear partida (Host)");
		cliente.cambiarTexto(ocupado ? "Conectando…" : "Unirse a partida (Cliente)");
	}

	void mostrarEstado(String mensaje) {
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.TINTA);
	}

	void mostrarError(String error) {
		ocupado(false);
		estadoMensaje.setText(error);
		estadoMensaje.setForeground(Tema.ERROR);
	}

	void resetear() {
		ocupado(false);
		estadoMensaje.setText(" ");
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
