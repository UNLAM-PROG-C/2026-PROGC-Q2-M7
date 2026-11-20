package ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

final class CapaVictoria extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ANCHO_TARJETA = 420;
	private static final int ALTO_TARJETA = 300;
	private static final float TRANSPARENCIA = 0.35f;
	private static final String TEXTO_REVANCHA = "Jugar de nuevo";
	private static final String TEXTO_ESPERANDO = "Esperando rival...";
	private static final String TEXTO_GENERANDO = "Generando...";

	private final JLabel titulo = etiqueta("¡Completado!", Font.BOLD, Tema.TAMANO_TITULO, "victoria");
	private final JLabel detalle = etiqueta("", Font.PLAIN, Tema.TAMANO_SUBTITULO, "victoria-detalle");
	private final JButton jugarNuevamente = Tema.boton(TEXTO_REVANCHA, true);
	private final JButton menu = Tema.boton("Volver al menú", false);
	private boolean multijugador;

	CapaVictoria(Runnable jugarOtraVez, Runnable volverAlMenu) {
		super(new GridBagLayout());
		setOpaque(false);
		setVisible(false);
		setFocusCycleRoot(true);
		setName("superposicion-victoria");
		MouseAdapter bloqueo = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				if (!menu.hasFocus()) { jugarNuevamente.requestFocusInWindow(); }
			}
		};
		addMouseListener(bloqueo);
		addMouseMotionListener(bloqueo);
		add(tarjeta(jugarOtraVez, volverAlMenu));
	}

	void mostrarFinPartida(String tituloTexto, String detalleHtml, boolean multijugador) {
		boolean recienAbierta = !isVisible();
		this.multijugador = multijugador;
		if (recienAbierta) {
			reiniciarBotonRevancha();
		}
		titulo.setText(tituloTexto);
		detalle.setText("<html><div style='text-align:center;width:340px;'>" + detalleHtml + "</div></html>");
		setVisible(true);
		if (recienAbierta) {
			SwingUtilities.invokeLater(jugarNuevamente::requestFocusInWindow);
		}
	}

	void ocultarSuperposicion() {
		reiniciarBotonRevancha();
		setVisible(false);
	}

	private void reiniciarBotonRevancha() {
		jugarNuevamente.setEnabled(true);
		jugarNuevamente.setText(TEXTO_REVANCHA);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			dibujo.setComposite(AlphaComposite.SrcOver.derive(TRANSPARENCIA));
			dibujo.setColor(Tema.TINTA);
			dibujo.fillRect(0, 0, getWidth(), getHeight());
		} finally {
			dibujo.dispose();
		}
	}

	private JPanel tarjeta(Runnable jugarOtraVez, Runnable volverAlMenu) {
		jugarNuevamente.setName("jugar-de-nuevo");
		menu.setName("menu-victoria");
		jugarNuevamente.addActionListener(e -> {
			jugarNuevamente.setEnabled(false);
			jugarNuevamente.setText(multijugador ? TEXTO_ESPERANDO : TEXTO_GENERANDO);
			jugarOtraVez.run();
		});
		menu.addActionListener(e -> {
			ocultarSuperposicion();
			volverAlMenu.run();
		});
		return armarContenedorTarjeta();
	}

	private JPanel armarContenedorTarjeta() {
		JPanel tarjeta = new JPanel(new BorderLayout(0, Tema.MARGEN));
		tarjeta.setBackground(Tema.TARJETA);
		tarjeta.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(Tema.BORDE),
				BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN)));
		tarjeta.setPreferredSize(new Dimension(ANCHO_TARJETA, ALTO_TARJETA));
		tarjeta.add(bloque(titulo, detalle), BorderLayout.NORTH);
		tarjeta.add(bloque(jugarNuevamente, menu), BorderLayout.SOUTH);
		return tarjeta;
	}

	private static JPanel bloque(JComponent primero, JComponent segundo) {
		JPanel panel = new JPanel(new GridLayout(0, 1, 0, Tema.ESPACIO));
		panel.setOpaque(false);
		panel.add(primero);
		panel.add(segundo);
		return panel;
	}

	private static JLabel etiqueta(String texto, int estilo, int tamano, String nombre) {
		JLabel etiqueta = Tema.etiqueta(texto, estilo, tamano, Tema.TINTA);
		etiqueta.setAlignmentX(CENTER_ALIGNMENT);
		etiqueta.setHorizontalAlignment(JLabel.CENTER);
		etiqueta.setName(nombre);
		return etiqueta;
	}
}
