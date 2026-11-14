package ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

final class Tema {
	static final Color FONDO = new Color(0xEEF1FB);
	static final Color TINTA = new Color(0x1B1F3B);
	static final Color TINTA_SUAVE = new Color(0x4A5073);
	static final Color ACENTO = new Color(0x4F46E5);
	static final Color SOMBRA_ACENTO = new Color(0x3730A3);
	static final Color TARJETA = Color.WHITE;
	static final Color TARJETA_INACTIVA = new Color(0xF8F9FE);
	static final Color BORDE = new Color(0xC9CEE6);
	static final Color ACENTO_SUAVE = new Color(0xE1E5F8);
	static final Color FONDO_TABLERO = new Color(0xDDE1F5);
	static final Color VISITADA = new Color(0xDAD7FB);
	static final Color ERROR = new Color(0xA43535);
	static final Color TOMATE_CUERPO = new Color(220, 25, 25, 252);
	static final Color TOMATE_SOMBRA = new Color(175, 15, 15, 255);
	static final Color TOMATE_BRILLO = new Color(248, 113, 113, 210);
	static final Color TOMATE_HOJA = new Color(34, 197, 94);
	static final Color TOMATE_TEXTO = Color.WHITE;
	static final Color TOMATE_SOMBRA_TEXTO = new Color(0, 0, 0, 220);

	static final int ANCHO_VENTANA = 720;
	static final int ALTO_VENTANA = 860;

	static final int RADIO_GRANDE = 16;
	static final int RADIO_MEDIO = 12;
	static final int RADIO_TABLERO = 20;
	static final int RADIO_TECLA = 10;

	static final int GROSOR_FOCO = 3;
	static final int DESPLAZAMIENTO_FOCO = 2;
	static final int GROSOR_BORDE = 2;
	static final int ALTO_SOMBRA = 4;
	static final int DESPLAZAMIENTO_PRESIONADO = 2;
	static final int HALO_TARJETA = 3;
	static final int MARGEN_CONTROL = GROSOR_FOCO + DESPLAZAMIENTO_FOCO;

	static final int TAMANO_RADIO = 20;
	static final int PUNTO_RADIO = 8;
	static final int ESPACIO_RADIO = 34;

	static final int RELLENO_VERTICAL_MENU = 48;
	static final int RELLENO_HORIZONTAL_MENU = 56;
	static final int MARGEN_PANEL_JUEGO = 32;
	static final int RELLENO_TARJETA = 18;

	static final int SEPARACION_MINIMA = 2;
	static final int SEPARACION_CHICA = 6;
	static final int SEPARACION_TECLAS = 6;
	static final int SEPARACION_ENCABEZADO = 8;
	static final int SEPARACION_MEDIA = 12;
	static final int SEPARACION_GRANDE = 16;
	static final int SEPARACION_TARJETAS = 16;
	static final int SEPARACION_SECCION = 28;

	static final int ALTO_VISUAL_PRINCIPAL = 60;
	static final int ALTO_PRINCIPAL = ALTO_VISUAL_PRINCIPAL + MARGEN_CONTROL;
	static final int ALTO_VISUAL_SECUNDARIO = 48;
	static final int ALTO_SECUNDARIO = ALTO_VISUAL_SECUNDARIO + MARGEN_CONTROL;
	static final int ALTO_FANTASMA = 40;

	static final int TAMANO_TITULO_GRANDE = 32;
	static final int TAMANO_TEXTO_PRINCIPAL = 20;
	static final int TAMANO_TITULO_TARJETA = 17;
	static final int TAMANO_TEXTO_SECUNDARIO = 16;
	static final int TAMANO_TEXTO_TECLA = 16;
	static final int TAMANO_TEXTO_TARJETA = 14;
	static final int TAMANO_TEXTO_AYUDA = 14;
	static final int TAMANO_TITULO_SECCION = 13;
	static final int TAMANO_ESTADO = 13;

	static final int TAMANO_ICONO_BOTON = 18;
	static final int TAMANO_ICONO_TARJETA = 28;
	static final float GROSOR_ICONO = 2.0f;

	static final int TAMANO_TECLA_MENU = 44;
	static final int TAMANO_TECLA_JUEGO = 28;
	static final float PROPORCION_ICONO_TECLA = 0.52f;

	static final float ESPACIADO_SECCION = 0.12f;
	static final float TRANSPARENCIA_DESHABILITADO = 0.45f;
	static final float TRANSPARENCIA_HALO = 0.18f;

	private static final Color FOCO = ACENTO;
	private static final int RELLENO_VERTICAL_INSIGNIA = 8;
	private static final int RELLENO_HORIZONTAL_INSIGNIA = 14;
	private static final int RADIO_INSIGNIA = 18;
	private static final int MAXIMO_CANAL_COLOR = 255;
	private static final String[] FUENTES_CANDIDATAS = {
		"SF Pro Text", "Segoe UI", "Helvetica Neue"
	};
	private static final String FAMILIA_FUENTE = detectarFamiliaFuente();
	private static final Map<ClaveFuente, Font> CACHE_FUENTES = new ConcurrentHashMap<>();
	private static final Map<Float, BasicStroke> CACHE_TRAZOS = new ConcurrentHashMap<>();

	private Tema() {
	}

	static void aplicarDefaultsSwing() {
		UIManager.put("Panel.background", FONDO);
		UIManager.put("OptionPane.background", TARJETA);
		UIManager.put("OptionPane.messageForeground", TINTA);
		UIManager.put("OptionPane.messageFont", fuente(Font.PLAIN, TAMANO_TEXTO_AYUDA));
		UIManager.put("OptionPane.buttonFont", fuente(Font.BOLD, TAMANO_TEXTO_AYUDA));
		UIManager.put("Slider.background", FONDO);
		UIManager.put("Slider.foreground", TINTA);
		UIManager.put("Slider.font", fuente(Font.BOLD, TAMANO_TEXTO_AYUDA));
		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
	}

	static Font fuente(float tamano, boolean negrita) {
		return fuente(negrita ? Font.BOLD : Font.PLAIN, tamano);
	}

	static Font fuente(int estilo, float tamano) {
		return CACHE_FUENTES.computeIfAbsent(new ClaveFuente(estilo, tamano, 0f), clave ->
				new Font(FAMILIA_FUENTE, clave.estilo(), Math.round(clave.tamano()))
						.deriveFont(clave.tamano()));
	}

	static Font fuenteConEspaciado(float tamano, boolean negrita, float espaciado) {
		int estilo = negrita ? Font.BOLD : Font.PLAIN;
		return CACHE_FUENTES.computeIfAbsent(new ClaveFuente(estilo, tamano, espaciado), clave -> {
			Font base = fuente(clave.estilo(), clave.tamano());
			Map<TextAttribute, Object> atributos = new HashMap<>(base.getAttributes());
			atributos.put(TextAttribute.TRACKING, clave.espaciado());
			return base.deriveFont(atributos);
		});
	}

	static BasicStroke trazo(float grosor) {
		return CACHE_TRAZOS.computeIfAbsent(grosor,
				valor -> new BasicStroke(valor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	}

	static JPanel panelVertical() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	static JPanel panelHorizontal() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		return panel;
	}

	static JPanel centrar(JComponent componente) {
		JPanel envoltorio = new PanelCentrado(componente);
		envoltorio.add(Box.createHorizontalGlue());
		envoltorio.add(componente);
		envoltorio.add(Box.createHorizontalGlue());
		return envoltorio;
	}

	static JLabel etiqueta(String texto, int estilo, int tamano, Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(fuente(estilo, tamano));
		etiqueta.setForeground(color);
		etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
		return etiqueta;
	}

	static JLabel etiquetaPastilla(String texto) {
		JLabel insignia = new EtiquetaPastillaInterna(texto);
		insignia.setFont(fuenteConEspaciado(TAMANO_TITULO_SECCION, true, ESPACIADO_SECCION));
		insignia.setBorder(BorderFactory.createEmptyBorder(RELLENO_VERTICAL_INSIGNIA,
				RELLENO_HORIZONTAL_INSIGNIA, RELLENO_VERTICAL_INSIGNIA,
				RELLENO_HORIZONTAL_INSIGNIA));
		return insignia;
	}

	static void preparar(Graphics2D dibujo) {
		dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
	}

	static void pintarCajaRedondeada(Graphics2D g, int x, int y, int ancho, int alto,
			int radio, Color relleno, Color borde, float grosorBorde) {
		if (borde != null && grosorBorde > 0f) {
			g.setColor(borde);
			g.fill(redondeado(x, y, ancho, alto, radio));
			float radioInterior = Math.max(0f, radio - grosorBorde);
			g.setColor(relleno);
			g.fill(redondeado(x + grosorBorde, y + grosorBorde, ancho - 2f * grosorBorde,
					alto - 2f * grosorBorde, radioInterior));
			return;
		}
		g.setColor(relleno);
		g.fill(redondeado(x, y, ancho, alto, radio));
	}

	static void aplicarEstadoHabilitado(Graphics2D g, boolean habilitado) {
		if (!habilitado) {
			g.setComposite(AlphaComposite.SrcOver.derive(TRANSPARENCIA_DESHABILITADO));
		}
	}

	static void pintarFoco(Graphics2D dibujo, float x, float y, float ancho,
			float alto, float radio) {
		dibujo.setColor(FOCO);
		dibujo.setStroke(trazo(GROSOR_FOCO));
		dibujo.draw(redondeado(x, y, ancho, alto, radio));
	}

	static Shape redondeado(float x, float y, float ancho, float alto, float radio) {
		return new RoundRectangle2D.Float(x, y, ancho, alto, radio, radio);
	}

	static Color conTransparencia(Color color, float transparencia) {
		int valor = Math.round(transparencia * MAXIMO_CANAL_COLOR);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), valor);
	}

	private static String detectarFamiliaFuente() {
		String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
		return Arrays.stream(FUENTES_CANDIDATAS)
				.filter(candidata -> Arrays.asList(disponibles).contains(candidata))
				.findFirst()
				.orElse(Font.SANS_SERIF);
	}

	private record ClaveFuente(int estilo, float tamano, float espaciado) {
	}

	private static final class PanelCentrado extends JPanel {
		private static final long serialVersionUID = 1L;
		private final JComponent contenido;

		PanelCentrado(JComponent contenido) {
			this.contenido = contenido;
			setOpaque(false);
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, contenido.getPreferredSize().height);
		}
	}

	private static final class EtiquetaPastillaInterna extends JLabel {
		private static final long serialVersionUID = 1L;

		EtiquetaPastillaInterna(String texto) {
			super(texto, SwingConstants.CENTER);
			setOpaque(false);
			setForeground(SOMBRA_ACENTO);
		}

		@Override
		protected void paintComponent(Graphics graficos) {
			Graphics2D dibujo = (Graphics2D) graficos.create();
			try {
				preparar(dibujo);
				pintarCajaRedondeada(dibujo, 0, 0, getWidth(), getHeight(),
						RADIO_INSIGNIA, ACENTO_SUAVE, null, 0);
			} finally {
				dibujo.dispose();
			}
			super.paintComponent(graficos);
		}

		@Override
		public Dimension getMaximumSize() {
			return getPreferredSize();
		}
	}
}
