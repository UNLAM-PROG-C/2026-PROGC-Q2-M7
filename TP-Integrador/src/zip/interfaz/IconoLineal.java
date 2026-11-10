package zip.interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

final class IconoLineal extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final double CIRCULO_COMPLETO = 360;
	private static final float INICIO_REINICIO = 40;
	private static final float EXTENSION_REINICIO = 285;
	private static final int DIENTES_ENGRANAJE = 8;
	private static final float CENTRO = 0.5f;
	private static final float CABEZA_PERSONA = 0.28f;
	private static final float SUPERIOR_CABEZA_PERSONA = 0.12f;
	private static final float IZQUIERDA_PERSONA = 0.22f;
	private static final float DERECHA_PERSONA = 0.78f;
	private static final float SUPERIOR_CUERPO_PERSONA = 0.55f;
	private static final float INFERIOR_PERSONA = 0.88f;
	private static final float ESCALA_PERSONAS = 0.78f;
	private static final float DESPLAZAMIENTO_Y_PERSONAS = 0.18f;
	private static final float SUPERIOR_FLECHA = 0.18f;
	private static final float INFERIOR_FLECHA = 0.82f;
	private static final float FLECHA_IZQUIERDA = 0.25f;
	private static final float FLECHA_DERECHA = 0.75f;
	private static final float HOMBRO_FLECHA = 0.43f;
	private static final float DESHACER_X = 0.2f;
	private static final float DESHACER_Y = 0.25f;
	private static final float ANCHO_DESHACER = 0.65f;
	private static final float ALTO_DESHACER = 0.55f;
	private static final float CABEZA_DESHACER_X = 0.18f;
	private static final float CABEZA_DESHACER_Y = 0.48f;
	private static final float PUNTA_DESHACER_X = 0.45f;
	private static final float PUNTA_DESHACER_Y = 0.3f;
	private static final float INICIO_DESHACER = 330;
	private static final float EXTENSION_DESHACER = 235;
	private static final float MARGEN_REINICIO = 0.14f;
	private static final float TAMANO_REINICIO = 0.72f;
	private static final float CABEZA_REINICIO_X = 0.79f;
	private static final float CABEZA_REINICIO_Y = 0.12f;
	private static final float PUNTO_REINICIO_X = 0.88f;
	private static final float PUNTO_REINICIO_Y = 0.39f;
	private static final float PUNTA_REINICIO_X = 0.61f;
	private static final float PUNTA_REINICIO_Y = 0.31f;
	private static final float EXTERIOR_ENGRANAJE = 0.28f;
	private static final float INTERIOR_ENGRANAJE = 0.1f;
	private static final float DIENTE_ENGRANAJE = 0.4f;
	private final TipoIcono tipo;
	private Color color;

	IconoLineal(TipoIcono tipo, int tamano, Color color) {
		this.tipo = tipo;
		this.color = color;
		Dimension dimension = new Dimension(tamano, tamano);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);
		setOpaque(false);
	}

	void establecerColor(Color color) {
		this.color = color;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			pintar(dibujo, tipo, 0, 0, Math.min(getWidth(), getHeight()), color);
		} finally {
			dibujo.dispose();
		}
	}

	static void pintar(Graphics2D dibujo, TipoIcono tipo, float x, float y,
			float tamano, Color color) {
		dibujo.setColor(color);
		dibujo.setStroke(new BasicStroke(Tema.GROSOR_ICONO,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		switch (tipo) {
			case PERSONA -> pintarPersona(dibujo, x, y, tamano);
			case PERSONAS -> pintarPersonas(dibujo, x, y, tamano);
			case FLECHA_ARRIBA, FLECHA_IZQUIERDA, FLECHA_ABAJO, FLECHA_DERECHA ->
				pintarFlecha(dibujo, tipo, x, y, tamano);
			case DESHACER -> pintarDeshacer(dibujo, x, y, tamano);
			case REINICIAR -> pintarReiniciar(dibujo, x, y, tamano);
			case ENGRANAJE -> pintarEngranaje(dibujo, x, y, tamano);
		}
	}

	private static void pintarPersona(Graphics2D dibujo, float x, float y, float tamano) {
		float cabeza = tamano * CABEZA_PERSONA;
		float centro = x + tamano / 2;
		dibujo.draw(new Ellipse2D.Float(centro - cabeza / 2,
				y + tamano * SUPERIOR_CABEZA_PERSONA, cabeza, cabeza));
		Path2D cuerpo = new Path2D.Float();
		cuerpo.moveTo(x + tamano * IZQUIERDA_PERSONA, y + tamano * INFERIOR_PERSONA);
		cuerpo.curveTo(x + tamano * IZQUIERDA_PERSONA, y + tamano * SUPERIOR_CUERPO_PERSONA,
				x + tamano * DERECHA_PERSONA, y + tamano * SUPERIOR_CUERPO_PERSONA,
				x + tamano * DERECHA_PERSONA, y + tamano * INFERIOR_PERSONA);
		dibujo.draw(cuerpo);
	}

	private static void pintarPersonas(Graphics2D dibujo, float x, float y, float tamano) {
		float persona = tamano * ESCALA_PERSONAS;
		Graphics2D copia = (Graphics2D) dibujo.create();
		copia.translate(tamano * IZQUIERDA_PERSONA, 0);
		pintarPersona(copia, x, y, persona);
		copia.dispose();
		pintarPersona(dibujo, x, y + tamano * DESPLAZAMIENTO_Y_PERSONAS, persona);
	}

	private static void pintarFlecha(Graphics2D dibujo, TipoIcono tipo,
			float x, float y, float tamano) {
		Graphics2D rotado = (Graphics2D) dibujo.create();
		rotado.rotate(rotacion(tipo), x + tamano / 2, y + tamano / 2);
		Path2D flecha = new Path2D.Float();
		flecha.moveTo(x + tamano * CENTRO, y + tamano * SUPERIOR_FLECHA);
		flecha.lineTo(x + tamano * CENTRO, y + tamano * INFERIOR_FLECHA);
		flecha.moveTo(x + tamano * FLECHA_IZQUIERDA, y + tamano * HOMBRO_FLECHA);
		flecha.lineTo(x + tamano * CENTRO, y + tamano * SUPERIOR_FLECHA);
		flecha.lineTo(x + tamano * FLECHA_DERECHA, y + tamano * HOMBRO_FLECHA);
		rotado.draw(flecha);
		rotado.dispose();
	}

	private static double rotacion(TipoIcono tipo) {
		return switch (tipo) {
			case FLECHA_DERECHA -> Math.PI / 2;
			case FLECHA_ABAJO -> Math.PI;
			case FLECHA_IZQUIERDA -> -Math.PI / 2;
			default -> 0;
		};
	}

	private static void pintarDeshacer(Graphics2D dibujo, float x, float y, float tamano) {
		dibujo.draw(new Arc2D.Float(x + tamano * DESHACER_X, y + tamano * DESHACER_Y,
				tamano * ANCHO_DESHACER, tamano * ALTO_DESHACER,
				INICIO_DESHACER, EXTENSION_DESHACER, Arc2D.OPEN));
		Path2D cabeza = new Path2D.Float();
		cabeza.moveTo(x + tamano * CABEZA_DESHACER_X, y + tamano * CABEZA_DESHACER_Y);
		cabeza.lineTo(x + tamano * DESHACER_X, y + tamano * DESHACER_X);
		cabeza.lineTo(x + tamano * PUNTA_DESHACER_X, y + tamano * PUNTA_DESHACER_Y);
		dibujo.draw(cabeza);
	}

	private static void pintarReiniciar(Graphics2D dibujo, float x, float y, float tamano) {
		dibujo.draw(new Arc2D.Float(x + tamano * MARGEN_REINICIO, y + tamano * MARGEN_REINICIO,
				tamano * TAMANO_REINICIO, tamano * TAMANO_REINICIO,
				INICIO_REINICIO, EXTENSION_REINICIO, Arc2D.OPEN));
		Path2D cabeza = new Path2D.Float();
		cabeza.moveTo(x + tamano * CABEZA_REINICIO_X, y + tamano * CABEZA_REINICIO_Y);
		cabeza.lineTo(x + tamano * PUNTO_REINICIO_X, y + tamano * PUNTO_REINICIO_Y);
		cabeza.lineTo(x + tamano * PUNTA_REINICIO_X, y + tamano * PUNTA_REINICIO_Y);
		dibujo.draw(cabeza);
	}

	private static void pintarEngranaje(Graphics2D dibujo, float x, float y, float tamano) {
		float centroX = x + tamano / 2;
		float centroY = y + tamano / 2;
		float exterior = tamano * EXTERIOR_ENGRANAJE;
		float interior = tamano * INTERIOR_ENGRANAJE;
		dibujo.draw(new Ellipse2D.Float(centroX - exterior, centroY - exterior,
				2 * exterior, 2 * exterior));
		dibujo.draw(new Ellipse2D.Float(centroX - interior, centroY - interior,
				2 * interior, 2 * interior));
		pintarDientesEngranaje(dibujo, centroX, centroY, exterior, tamano);
	}

	private static void pintarDientesEngranaje(Graphics2D dibujo, float centroX,
			float centroY, float exterior, float tamano) {
		for (int indice = 0; indice < DIENTES_ENGRANAJE; indice++) {
			double angulo = CIRCULO_COMPLETO / DIENTES_ENGRANAJE * indice;
			double radianes = Math.toRadians(angulo);
			float inicioX = centroX + (float) Math.cos(radianes) * exterior;
			float inicioY = centroY + (float) Math.sin(radianes) * exterior;
			float finX = centroX + (float) Math.cos(radianes) * tamano * DIENTE_ENGRANAJE;
			float finY = centroY + (float) Math.sin(radianes) * tamano * DIENTE_ENGRANAJE;
			dibujo.drawLine(Math.round(inicioX), Math.round(inicioY),
					Math.round(finX), Math.round(finY));
		}
	}
}
