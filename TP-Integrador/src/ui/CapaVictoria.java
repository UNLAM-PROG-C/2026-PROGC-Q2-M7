package ui;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

final class CapaVictoria extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ANCHO_TARJETA_VICTORIA = 420;
	private static final int ALTO_TARJETA_VICTORIA = 370;
	private static final int RELLENO_VICTORIA = 32;
	private static final int TAMANO_DETALLE_VICTORIA = 18;
	private static final int SOMBRA_VICTORIA = 12;
	private static final int ANCHO_BOTON_VICTORIA = 320;
	private static final float TRANSPARENCIA_SUPERPOSICION = 0.35f;
	private static final float TRANSPARENCIA_SOMBRA = 0.16f;

	private final JLabel titulo = crearEtiqueta("¡Completado!", Font.BOLD,
			Tema.TAMANO_TITULO_GRANDE, "victoria");
	private final JLabel detalle = crearEtiqueta("", Font.PLAIN,
			TAMANO_DETALLE_VICTORIA, null);
	private final BotonEstilo jugarNuevamente = BotonEstilo.principal("Jugar de nuevo");
	private final BotonEstilo menu = BotonEstilo.secundario("Volver al menú");

	CapaVictoria(Runnable jugarOtraVez, Runnable volverAlMenu) {
		super(new GridBagLayout());
		setOpaque(false);
		setVisible(false);
		setFocusCycleRoot(true);
		setName("superposicion-victoria");
		instalarBloqueoEntrada();
		configurarBotones(jugarOtraVez, volverAlMenu);
		add(tarjeta());
	}

	private void instalarBloqueoEntrada() {
		MouseAdapter bloqueo = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent evento) {
				if (!menu.hasFocus()) {
					jugarNuevamente.requestFocusInWindow();
				}
			}
		};
		addMouseListener(bloqueo);
		addMouseMotionListener(bloqueo);
	}

	private void configurarBotones(Runnable jugarOtraVez, Runnable volverAlMenu) {
		jugarNuevamente.setName("jugar-de-nuevo");
		menu.setName("menu-victoria");
		jugarNuevamente.addActionListener(evento -> cerrarYEjecutar(jugarOtraVez));
		menu.addActionListener(evento -> cerrarYEjecutar(volverAlMenu));
	}

	void mostrarFinPartida(String tituloTexto, String detalleTexto) {
		titulo.setText(tituloTexto);
		detalle.setText(detalleTexto);
		setVisible(true);
		SwingUtilities.invokeLater(jugarNuevamente::requestFocusInWindow);
	}

	void ocultarSuperposicion() {
		setVisible(false);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			dibujo.setComposite(AlphaComposite.SrcOver.derive(TRANSPARENCIA_SUPERPOSICION));
			dibujo.setColor(Tema.TINTA);
			dibujo.fillRect(0, 0, getWidth(), getHeight());
		} finally {
			dibujo.dispose();
		}
	}

	private JPanel tarjeta() {
		JPanel tarjeta = new TarjetaVictoria();
		tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
		tarjeta.setBorder(BorderFactory.createEmptyBorder(RELLENO_VICTORIA,
				RELLENO_VICTORIA, RELLENO_VICTORIA, RELLENO_VICTORIA));
		tarjeta.setPreferredSize(new Dimension(ANCHO_TARJETA_VICTORIA, ALTO_TARJETA_VICTORIA));
		tarjeta.add(Tema.centrar(titulo));
		tarjeta.add(Box.createVerticalStrut(Tema.SEPARACION_SECCION));
		tarjeta.add(Tema.centrar(detalle));
		tarjeta.add(Box.createVerticalGlue());
		tarjeta.add(Tema.centrar(ajustarBoton(jugarNuevamente)));
		tarjeta.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		tarjeta.add(Tema.centrar(ajustarBoton(menu)));
		return tarjeta;
	}

	private BotonEstilo ajustarBoton(BotonEstilo boton) {
		boton.setPreferredSize(new Dimension(ANCHO_BOTON_VICTORIA,
				boton.getPreferredSize().height));
		boton.setMaximumSize(boton.getPreferredSize());
		return boton;
	}

	private static JLabel crearEtiqueta(String texto, int estilo, int tamano, String nombre) {
		JLabel etiqueta = Tema.etiqueta(texto, estilo, tamano, Tema.TINTA);
		etiqueta.setHorizontalAlignment(SwingConstants.CENTER);
		etiqueta.setName(nombre);
		return etiqueta;
	}

	private void cerrarYEjecutar(Runnable accion) {
		ocultarSuperposicion();
		accion.run();
	}

	private static final class TarjetaVictoria extends JPanel {
		private static final long serialVersionUID = 1L;

		TarjetaVictoria() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics graficos) {
			Graphics2D dibujo = (Graphics2D) graficos.create();
			try {
				Tema.preparar(dibujo);
				pintarTarjeta(dibujo);
			} finally {
				dibujo.dispose();
			}
			super.paintComponent(graficos);
		}

		private void pintarTarjeta(Graphics2D dibujo) {
			int ancho = getWidth() - SOMBRA_VICTORIA;
			int alto = getHeight() - SOMBRA_VICTORIA;
			dibujo.setColor(Tema.conTransparencia(Tema.TINTA, TRANSPARENCIA_SOMBRA));
			dibujo.fill(Tema.redondeado(SOMBRA_VICTORIA / 2.0f, SOMBRA_VICTORIA,
					ancho, alto, Tema.RADIO_TABLERO));
			Tema.pintarCajaRedondeada(dibujo, 0, 0, ancho, alto,
					Tema.RADIO_TABLERO, Tema.TARJETA, null, 0);
		}
	}
}
