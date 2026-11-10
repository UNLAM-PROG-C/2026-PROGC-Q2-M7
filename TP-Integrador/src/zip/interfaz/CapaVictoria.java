package zip.interfaz;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import zip.dominio.EstadoJuego;

final class CapaVictoria extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JLabel detalle = etiqueta("", Tema.TAMANO_DETALLE_VICTORIA, false);
	private final BotonPrincipal jugarNuevamente = new BotonPrincipal("Jugar de nuevo");
	private final BotonSecundario menu = new BotonSecundario("Volver al menú", null);

	CapaVictoria(Runnable jugarOtraVez, Runnable volverAlMenu) {
		super(new GridBagLayout());
		setOpaque(false);
		setVisible(false);
		setName("superposicion-victoria");
		addMouseListener(new MouseAdapter() { });
		jugarNuevamente.setName("jugar-de-nuevo");
		menu.setName("menu-victoria");
		jugarNuevamente.agregarEscucha(evento -> cerrarYEjecutar(jugarOtraVez));
		menu.agregarEscucha(evento -> cerrarYEjecutar(volverAlMenu));
		add(tarjeta());
	}

	void mostrarVictoria(EstadoJuego estadoJuego) {
		detalle.setText(estadoJuego.visitadas() + " de " + estadoJuego.nivel().cantidadCeldas() + " celdas");
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
			dibujo.setComposite(AlphaComposite.SrcOver.derive(Tema.TRANSPARENCIA_SUPERPOSICION));
			dibujo.setColor(Tema.TINTA);
			dibujo.fillRect(0, 0, getWidth(), getHeight());
		} finally {
			dibujo.dispose();
		}
	}

	private JPanel tarjeta() {
		JPanel tarjeta = new TarjetaVictoria();
		tarjeta.setLayout(new BoxLayout(tarjeta, BoxLayout.Y_AXIS));
		tarjeta.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VICTORIA,
				Tema.RELLENO_VICTORIA, Tema.RELLENO_VICTORIA, Tema.RELLENO_VICTORIA));
		tarjeta.setPreferredSize(new Dimension(Tema.ANCHO_TARJETA_VICTORIA, Tema.ALTO_TARJETA_VICTORIA));
		tarjeta.add(centrado(etiqueta("¡Completado!", Tema.TAMANO_TITULO_VICTORIA, true, "victoria")));
		tarjeta.add(Box.createVerticalStrut(Tema.SEPARACION_SECCION));
		tarjeta.add(centrado(detalle));
		tarjeta.add(Box.createVerticalGlue());
		tarjeta.add(centrado(boton(jugarNuevamente)));
		tarjeta.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		tarjeta.add(centrado(boton(menu)));
		return tarjeta;
	}

	private Component boton(Component boton) {
		boton.setPreferredSize(new Dimension(Tema.ANCHO_BOTON_VICTORIA,
				boton.getPreferredSize().height));
		boton.setMaximumSize(boton.getPreferredSize());
		return boton;
	}

	private JPanel centrado(Component componente) {
		JPanel envoltorio = new JPanel();
		envoltorio.setOpaque(false);
		envoltorio.setLayout(new BoxLayout(envoltorio, BoxLayout.X_AXIS));
		envoltorio.add(Box.createHorizontalGlue());
		envoltorio.add(componente);
		envoltorio.add(Box.createHorizontalGlue());
		return envoltorio;
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita) {
		return etiqueta(texto, tamano, negrita, null);
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita, String nombre) {
		JLabel etiqueta = new JLabel(texto, SwingConstants.CENTER);
		etiqueta.setName(nombre);
		etiqueta.setFont(Tema.fuente(tamano, negrita));
		etiqueta.setForeground(Tema.TINTA);
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
			dibujo.setColor(Tema.conTransparencia(Tema.TINTA, Tema.TRANSPARENCIA_SOMBRA));
			dibujo.fill(Tema.redondeado(Tema.SOMBRA_VICTORIA / 2.0f,
					Tema.SOMBRA_VICTORIA, getWidth() - Tema.SOMBRA_VICTORIA,
					getHeight() - Tema.SOMBRA_VICTORIA, Tema.RADIO_TABLERO));
			dibujo.setColor(Tema.TARJETA);
			dibujo.fill(Tema.redondeado(0, 0, getWidth() - Tema.SOMBRA_VICTORIA,
					getHeight() - Tema.SOMBRA_VICTORIA, Tema.RADIO_TABLERO));
		}
	}
}
