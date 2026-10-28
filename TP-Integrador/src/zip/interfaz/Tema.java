package zip.interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
	static final Color FOCO = ACENTO;
	static final Color ERROR = new Color(0xA43535);
	static final Color SUPERPOSICION = TINTA;
	static final Color SOMBRA_TARJETA = TINTA;
	static final Color HALO = ACENTO;

	static final int RADIO_GRANDE = 16;
	static final int RADIO_MEDIO = 12;
	static final int GROSOR_FOCO = 3;
	static final int DESPLAZAMIENTO_FOCO = 2;
	static final int GROSOR_BORDE = 2;
	static final int ALTO_SOMBRA = 4;
	static final int DESPLAZAMIENTO_PRESIONADO = 2;
	static final int HALO_TARJETA = 3;
	static final int MARGEN_CONTROL = GROSOR_FOCO + DESPLAZAMIENTO_FOCO;

	static final int ANCHO_VENTANA = 720;
	static final int ALTO_VENTANA = 860;
	static final int ANCHO_MINIMO_VENTANA = 720;
	static final int ALTO_MINIMO_VENTANA = 860;
	static final int RELLENO_VERTICAL_MENU = 48;
	static final int RELLENO_HORIZONTAL_MENU = 56;
	static final int TAMANO_TITULO_MENU = 56;
	static final int TAMANO_SUBTITULO_MENU = 18;
	static final int TAMANO_TITULO_SECCION = 13;
	static final int SEPARACION_TITULO_SECCION = 12;
	static final int SEPARACION_SECCION = 28;
	static final int SEPARACION_ENCABEZADO = 8;
	static final int SEPARACION_TARJETAS = 16;
	static final int ALTO_TARJETA_MODO = 84;
	static final int ALTO_TARJETA_CONTROLES = 160;
	static final int ALTO_TARJETA_DIFICULTAD = 136;
	static final int RELLENO_TARJETA = 18;
	static final int TAMANO_TITULO_TARJETA = 17;
	static final int TAMANO_TEXTO_TARJETA = 14;
	static final int TAMANO_VALOR_TARJETA = 34;
	static final int TAMANO_RADIO = 20;
	static final int PUNTO_RADIO = 8;
	static final int ESPACIO_RADIO = 34;
	static final int TAMANO_TECLA_MENU = 44;
	static final int CANTIDAD_TECLAS_CONTROL = 4;
	static final int RELLENO_INFERIOR_TECLAS = 18;
	static final int TAMANO_TECLA_JUEGO = 28;
	static final int SEPARACION_TECLAS = 6;
	static final int RADIO_TECLA = 10;
	static final int TAMANO_TEXTO_TECLA = 16;
	static final int TAMANO_RESUMEN = 14;
	static final int ALTO_LINEA_RESUMEN = 20;
	static final int SEPARACION_RESUMEN = 12;
	static final int TAMANO_ESTADO = 13;

	static final int ALTO_VISUAL_PRINCIPAL = 60;
	static final int ALTO_PRINCIPAL = ALTO_VISUAL_PRINCIPAL + MARGEN_CONTROL;
	static final int TAMANO_TEXTO_PRINCIPAL = 20;
	static final int ALTO_VISUAL_SECUNDARIO = 48;
	static final int ALTO_SECUNDARIO = ALTO_VISUAL_SECUNDARIO + MARGEN_CONTROL;
	static final int TAMANO_TEXTO_SECUNDARIO = 16;
	static final int ALTO_FANTASMA = 40;
	static final int ANCHO_VOLVER_FANTASMA = 112;
	static final int TAMANO_ICONO_TARJETA = 28;
	static final int TAMANO_ICONO_BOTON = 18;
	static final float GROSOR_ICONO = 2.0f;

	static final int RELLENO_VERTICAL_JUEGO = 32;
	static final int RELLENO_HORIZONTAL_JUEGO = 40;
	static final int SEPARACION_JUEGO = 16;
	static final int TAMANO_TITULO_JUEGO = 28;
	static final int ESPACIADO_TITULO_JUEGO_PIXELES = 4;
	static final int SEPARACION_INDICADORES = 12;
	static final int ALTO_INDICADOR = 58;
	static final int RELLENO_HORIZONTAL_INDICADOR = 16;
	static final int RELLENO_VERTICAL_INDICADOR = 10;
	static final int TAMANO_ETIQUETA_INDICADOR = 13;
	static final int TAMANO_VALOR_INDICADOR = 20;
	static final int ALTO_PROGRESO = 8;
	static final int RADIO_PROGRESO = 4;
	static final int RELLENO_TABLERO = 12;
	static final int TAMANO_PREFERIDO_TABLERO = 440;
	static final int RADIO_TABLERO = 20;
	static final int SEPARACION_CELDAS = 8;
	static final int RADIO_CELDA = 12;
	static final int DIAMETRO_NUMERO = 40;
	static final int BORDE_NUMERO = 3;
	static final int TAMANO_NUMERO = 16;
	static final int PUNTO_ACTUAL = 14;
	static final float GROSOR_RECORRIDO = 5.0f;
	static final int TAMANO_TEXTO_AYUDA = 14;
	static final int SEPARACION_AYUDA = 8;
	static final int SEPARACION_ACCIONES = 12;
	static final int ANCHO_BOTON_SECUNDARIO = 170;
	static final int ANCHO_TECLA_BACKSPACE = 88;

	static final int ANCHO_CONTENIDO_PROXIMAMENTE = 460;
	static final int ANCHO_BOTON_PROXIMAMENTE = 360;
	static final int TAMANO_TITULO_PROXIMAMENTE = 32;
	static final int TAMANO_TEXTO_PROXIMAMENTE = 18;
	static final int RELLENO_VERTICAL_INSIGNIA = 8;
	static final int RELLENO_HORIZONTAL_INSIGNIA = 14;
	static final int RADIO_INSIGNIA = 18;
	static final int TAMANO_ICONO_CONSTRUCCION = 92;
	static final int SEPARACION_ICONO_PROXIMAMENTE = 22;
	static final int SEPARACION_INSIGNIA_PROXIMAMENTE = 18;
	static final int SEPARACION_TITULO_PROXIMAMENTE = 14;
	static final int SEPARACION_DESCRIPCION_PROXIMAMENTE = 28;
	static final int SEPARACION_BOTON_PROXIMAMENTE = 10;
	static final int ALTO_DESCRIPCION_PROXIMAMENTE = 76;

	static final int ANCHO_TARJETA_VICTORIA = 420;
	static final int ALTO_TARJETA_VICTORIA = 370;
	static final int RELLENO_VICTORIA = 32;
	static final int TAMANO_TITULO_VICTORIA = 32;
	static final int TAMANO_DETALLE_VICTORIA = 18;
	static final int SOMBRA_VICTORIA = 12;
	static final int ANCHO_BOTON_VICTORIA = 320;

	static final float ESPACIADO_TITULO = 0.11f;
	static final float ESPACIADO_SECCION = 0.12f;
	static final float ESPACIADO_TITULO_JUEGO = 0.14f;
	static final float TRANSPARENCIA_SUPERPOSICION = 0.35f;
	static final float TRANSPARENCIA_DESHABILITADO = 0.45f;
	static final float TRANSPARENCIA_SOMBRA = 0.16f;
	static final float TRANSPARENCIA_HALO = 0.18f;
	static final float PROPORCION_ICONO_TECLA = 0.52f;
	static final int MAXIMO_CANAL_COLOR = 255;

	private static final String[] FUENTES_CANDIDATAS = {
		"SF Pro Text", "Segoe UI", "Helvetica Neue"
	};
	private static final String FAMILIA_FUENTE = detectarFamiliaFuente();

	private Tema() {
	}

	static Font fuente(float tamano, boolean negrita) {
		int estilo = negrita ? Font.BOLD : Font.PLAIN;
		return new Font(FAMILIA_FUENTE, estilo, Math.round(tamano)).deriveFont(tamano);
	}

	static Font fuenteConEspaciado(float tamano, boolean negrita, float espaciado) {
		Map<TextAttribute, Object> atributos = new HashMap<>(fuente(tamano, negrita).getAttributes());
		atributos.put(TextAttribute.TRACKING, espaciado);
		return fuente(tamano, negrita).deriveFont(atributos);
	}

	static void preparar(Graphics2D dibujo) {
		dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
	}

	static void rellenarBorde(Graphics2D dibujo, float x, float y, float ancho,
			float alto, float radio, Color borde, Color relleno) {
		dibujo.setColor(borde);
		dibujo.fill(redondeado(x, y, ancho, alto, radio));
		dibujo.setColor(relleno);
		dibujo.fill(redondeadoInterior(x, y, ancho, alto, radio));
	}

	static void pintarFoco(Graphics2D dibujo, float x, float y, float ancho,
			float alto, float radio) {
		dibujo.setColor(FOCO);
		dibujo.setStroke(new BasicStroke(GROSOR_FOCO, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		dibujo.draw(redondeado(x, y, ancho, alto, radio));
	}

	static Shape redondeado(float x, float y, float ancho, float alto, float radio) {
		return new RoundRectangle2D.Float(x, y, ancho, alto, radio, radio);
	}

	static Color conTransparencia(Color color, float transparencia) {
		int valor = Math.round(transparencia * MAXIMO_CANAL_COLOR);
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), valor);
	}

	private static Shape redondeadoInterior(float x, float y, float ancho,
			float alto, float radio) {
		float borde = GROSOR_BORDE;
		float radioInterior = Math.max(0, radio - borde);
		return redondeado(x + borde, y + borde, ancho - 2 * borde,
				alto - 2 * borde, radioInterior);
	}

	private static String detectarFamiliaFuente() {
		String[] disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
		return Arrays.stream(FUENTES_CANDIDATAS)
				.filter(candidata -> Arrays.asList(disponibles).contains(candidata))
				.findFirst()
				.orElse(Font.SANS_SERIF);
	}
}
