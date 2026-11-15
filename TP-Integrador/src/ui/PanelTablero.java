package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JPanel;
import model.Celda;
import model.EstadoJuego;

final class PanelTablero extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int RELLENO_TABLERO = 12;
	private static final int TAMANO_PREFERIDO = 440;
	private static final int SEPARACION_CELDAS = 8;
	private static final int RADIO_TABLERO = 20;
	private static final int RADIO_CELDA = 12;
	private static final int DIAMETRO_NUMERO = 40;
	private static final int BORDE_NUMERO = 3;
	private static final int MARGEN_NUMERO = 4;
	private static final int TAMANO_NUMERO = 16;
	private static final int TAMANO_TOMATE = 32;
	private static final int PUNTO_ACTUAL = 14;
	private static final float GROSOR_RECORRIDO = 5.0f;
	private static final float ESCALA_RADIO_TOMATE = 0.44f;
	private static final float ESCALA_BULTOS_TOMATE = 0.48f;
	private static final float ESCALA_GOTA_TOMATE = 0.25f;
	private static final float ESCALA_ANCHO_TALLO = 0.35f;
	private static final float ESCALA_ALTO_TALLO = 0.20f;
	private static final float ESCALA_BRILLO_TOMATE = 0.28f;
	private static final int DIVISOR_TERCIO = 3;
	private static final int VERTICES_TALLO = 3;
	private static final String TEXTO_TOMATE = "¡TOMATAZO!";
	private static final Color TOMATE_CUERPO = new Color(220, 25, 25, 252);
	private static final Color TOMATE_SOMBRA = new Color(175, 15, 15, 255);
	private static final Color TOMATE_BRILLO = new Color(248, 113, 113, 210);
	private static final Color TOMATE_HOJA = new Color(34, 197, 94);
	private static final Color TOMATE_TEXTO = Color.WHITE;
	private static final Color TOMATE_SOMBRA_TEXTO = new Color(0, 0, 0, 220);
	private static final BasicStroke TRAZO_RECORRIDO = trazo(GROSOR_RECORRIDO);
	private static final Font FUENTE_NUMERO = Tema.fuente(Font.BOLD, TAMANO_NUMERO);
	private static final Font FUENTE_TOMATE = Tema.fuente(Font.BOLD, TAMANO_TOMATE);

	private transient EstadoJuego estado;
	private boolean[][] visitadas = new boolean[0][0];
	private boolean manchadoPorTomate;

	PanelTablero() {
		setName("tablero");
		setBackground(Tema.FONDO);
		setPreferredSize(new Dimension(TAMANO_PREFERIDO, TAMANO_PREFERIDO));
	}

	void mostrar(EstadoJuego nuevoEstado, boolean manchado) {
		this.estado = nuevoEstado;
		this.manchadoPorTomate = manchado;
		actualizarVisitadas(nuevoEstado);
		repaint();
	}

	Optional<EstadoJuego> estadoActual() {
		return Optional.ofNullable(estado);
	}

	/** Celda bajo el punto, o vacío si cae fuera de la grilla o entre celdas. */
	Optional<Celda> celdaEn(Point punto) {
		if (estado == null) {
			return Optional.empty();
		}
		return geometria().celdaEn(punto);
	}

	private void actualizarVisitadas(EstadoJuego nuevoEstado) {
		int tamano = nuevoEstado.nivel().tamano();
		if (visitadas.length != tamano) {
			visitadas = new boolean[tamano][tamano];
		} else {
			for (boolean[] fila : visitadas) {
				Arrays.fill(fila, false);
			}
		}
		for (Celda celda : nuevoEstado.recorrido()) {
			visitadas[celda.fila()][celda.columna()] = true;
		}
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		super.paintComponent(graficos);
		if (estado == null) { return; }
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			preparar(dibujo);
			dibujarTablero(dibujo, geometria());
		} finally {
			dibujo.dispose();
		}
	}

	private Geometria geometria() {
		int ladoTablero = Math.max(1, Math.min(getWidth(), getHeight()));
		int izqTablero = (getWidth() - ladoTablero) / 2;
		int supTablero = (getHeight() - ladoTablero) / 2;
		int tamanoNivel = estado.nivel().tamano();
		int espacioGrilla = ladoTablero - 2 * RELLENO_TABLERO;
		int separacion = (tamanoNivel - 1) * SEPARACION_CELDAS;
		int celda = Math.max(1, (espacioGrilla - separacion) / tamanoNivel);
		int ladoGrilla = celda * tamanoNivel + separacion;
		return new Geometria(izqTablero, supTablero, ladoTablero,
				izqTablero + (ladoTablero - ladoGrilla) / 2,
				supTablero + (ladoTablero - ladoGrilla) / 2, celda, tamanoNivel);
	}

	private void dibujarTablero(Graphics2D dibujo, Geometria grilla) {
		dibujo.setColor(Tema.FONDO_TABLERO);
		dibujo.fill(redondeado(grilla.izqTablero(), grilla.supTablero(),
				grilla.ladoTablero(), grilla.ladoTablero(), RADIO_TABLERO));
		dibujarCeldas(dibujo, grilla);
		dibujarRecorrido(dibujo, grilla);
		Celda actual = estado.actual();
		dibujo.setColor(Tema.TARJETA);
		dibujo.fillOval(grilla.centroX(actual.columna()) - PUNTO_ACTUAL / 2,
				grilla.centroY(actual.fila()) - PUNTO_ACTUAL / 2, PUNTO_ACTUAL, PUNTO_ACTUAL);
		dibujarNumeros(dibujo, grilla);
		if (manchadoPorTomate) { dibujarTomate(dibujo, grilla); }
	}

	private void dibujarCeldas(Graphics2D dibujo, Geometria grilla) {
		Celda actual = estado.actual();
		for (int fila = 0; fila < grilla.tamanoNivel(); fila++) {
			for (int col = 0; col < grilla.tamanoNivel(); col++) {
				boolean esActual = fila == actual.fila() && col == actual.columna();
				dibujo.setColor(esActual ? Tema.ACENTO : (visitadas[fila][col] ? Tema.VISITADA : Tema.TARJETA));
				dibujo.fill(redondeado(grilla.x(col), grilla.y(fila), grilla.celda(), grilla.celda(), RADIO_CELDA));
			}
		}
	}

	private void dibujarRecorrido(Graphics2D dibujo, Geometria grilla) {
		List<Celda> recorrido = estado.recorrido();
		dibujo.setColor(Tema.ACENTO);
		dibujo.setStroke(TRAZO_RECORRIDO);
		for (int indice = 1; indice < recorrido.size(); indice++) {
			Celda anterior = recorrido.get(indice - 1);
			Celda siguiente = recorrido.get(indice);
			dibujo.drawLine(grilla.centroX(anterior.columna()), grilla.centroY(anterior.fila()),
					grilla.centroX(siguiente.columna()), grilla.centroY(siguiente.fila()));
		}
	}

	private void dibujarNumeros(Graphics2D dibujo, Geometria grilla) {
		List<Integer> numeros = estado.nivel().numeros();
		int tamano = grilla.tamanoNivel();
		for (int fila = 0; fila < tamano; fila++) {
			for (int col = 0; col < tamano; col++) {
				int numero = numeros.get(fila * tamano + col);
				if (numero > 0) { dibujarNumero(dibujo, grilla, fila, col, numero); }
			}
		}
	}

	private void dibujarNumero(Graphics2D dibujo, Geometria grilla, int fila, int col, int numero) {
		int diametro = Math.max(1, Math.min(DIAMETRO_NUMERO, grilla.celda() - MARGEN_NUMERO));
		int x = grilla.centroX(col) - diametro / 2;
		int y = grilla.centroY(fila) - diametro / 2;
		int proximo = estado.proximoNumero();
		dibujo.setColor(numero <= proximo ? Tema.ACENTO : Tema.TINTA);
		dibujo.fillOval(x, y, diametro, diametro);
		int interior = diametro - 2 * BORDE_NUMERO;
		if (numero == proximo && interior > 0) {
			dibujo.setColor(Tema.TARJETA);
			dibujo.fillOval(x + BORDE_NUMERO, y + BORDE_NUMERO, interior, interior);
		}
		dibujo.setColor(numero == proximo ? Tema.TINTA : Tema.TARJETA);
		texto(dibujo, FUENTE_NUMERO, Integer.toString(numero), grilla.centroX(col), grilla.centroY(fila), false);
	}

	private void dibujarTomate(Graphics2D dibujo, Geometria grilla) {
		int cx = grilla.izqTablero() + grilla.ladoTablero() / 2;
		int cy = grilla.supTablero() + grilla.ladoTablero() / 2;
		int radio = Math.max(1, (int) (grilla.ladoTablero() * ESCALA_RADIO_TOMATE));
		dibujarSalpicaduraTomate(dibujo, cx, cy, radio);
		dibujarGotasTomate(dibujo, cx, cy, radio);
		dibujarTalloTomate(dibujo, cx, cy, radio);
		texto(dibujo, FUENTE_TOMATE, TEXTO_TOMATE, cx, cy, true);
	}

	private void dibujarSalpicaduraTomate(Graphics2D dibujo, int cx, int cy, int radio) {
		dibujo.setColor(TOMATE_CUERPO);
		dibujo.fillOval(cx - radio, cy - radio, radio * 2, radio * 2);
		int bulto = (int) (radio * ESCALA_BULTOS_TOMATE);
		dibujo.fillOval(cx - radio - bulto / DIVISOR_TERCIO, cy - bulto, bulto * 2, bulto * 2);
		dibujo.fillOval(cx + radio - bulto, cy - bulto / 2, bulto * 2, bulto * 2);
		dibujo.fillOval(cx - bulto, cy - radio - bulto / DIVISOR_TERCIO, bulto * 2, bulto * 2);
		dibujo.fillOval(cx - bulto / 2, cy + radio - bulto, bulto * 2, bulto * 2);
		dibujo.setColor(TOMATE_SOMBRA);
		dibujo.fillOval(cx - radio / 2, cy - radio / 2, radio, radio);
	}

	private void dibujarGotasTomate(Graphics2D dibujo, int cx, int cy, int radio) {
		int gota = (int) (radio * ESCALA_GOTA_TOMATE);
		int brillo = (int) (radio * ESCALA_BRILLO_TOMATE);
		dibujo.setColor(TOMATE_CUERPO);
		dibujo.fillOval(cx - radio - gota * 2, cy + radio / 2, gota * 2, gota * 2);
		dibujo.fillOval(cx + radio + gota, cy - radio / 2, gota * 2, gota * 2);
		dibujo.setColor(TOMATE_BRILLO);
		dibujo.fillOval(cx - radio / 2 - brillo / 2, cy - radio / 2 - brillo / 2, brillo, brillo);
	}

	private void dibujarTalloTomate(Graphics2D dibujo, int cx, int cy, int radio) {
		dibujo.setColor(TOMATE_HOJA);
		int ancho = (int) (radio * ESCALA_ANCHO_TALLO);
		int alto = (int) (radio * ESCALA_ALTO_TALLO);
		dibujo.fillOval(cx - ancho / 2, cy - radio - alto / 2, ancho, alto);
		int[] talloX = {cx - ancho / DIVISOR_TERCIO, cx, cx + ancho / DIVISOR_TERCIO};
		int[] talloY = {cy - radio, cy - radio - alto, cy - radio};
		dibujo.fillPolygon(talloX, talloY, VERTICES_TALLO);
	}

	private void texto(Graphics2D dibujo, Font fuente, String texto, int cx, int cy, boolean sombraTomate) {
		dibujo.setFont(fuente);
		FontMetrics metricas = dibujo.getFontMetrics();
		int x = cx - metricas.stringWidth(texto) / 2;
		int y = cy + (metricas.getAscent() - metricas.getDescent()) / 2;
		if (sombraTomate) {
			dibujo.setColor(TOMATE_SOMBRA_TEXTO);
			dibujo.drawString(texto, x + 2, y + 2);
			dibujo.setColor(TOMATE_TEXTO);
		}
		dibujo.drawString(texto, x, y);
	}

	private static void preparar(Graphics2D dibujo) {
		dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		dibujo.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}

	private static BasicStroke trazo(float grosor) {
		return new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	private static Shape redondeado(float x, float y, float ancho, float alto, float radio) {
		return new RoundRectangle2D.Float(x, y, ancho, alto, radio, radio);
	}

	private record Geometria(int izqTablero, int supTablero, int ladoTablero,
			int izqGrilla, int supGrilla, int celda, int tamanoNivel) {
		int x(int col) { return izqGrilla + col * (celda + SEPARACION_CELDAS); }
		int y(int fila) { return supGrilla + fila * (celda + SEPARACION_CELDAS); }
		int centroX(int col) { return x(col) + celda / 2; }
		int centroY(int fila) { return y(fila) + celda / 2; }

		Optional<Celda> celdaEn(Point punto) {
			int columna = indiceEn(punto.x - izqGrilla);
			int fila = indiceEn(punto.y - supGrilla);
			if (columna < 0 || fila < 0) {
				return Optional.empty();
			}
			return Optional.of(new Celda(fila, columna));
		}

		private int indiceEn(int desplazamiento) {
			int paso = celda + SEPARACION_CELDAS;
			if (desplazamiento < 0 || desplazamiento % paso >= celda) {
				return -1;
			}
			int indice = desplazamiento / paso;
			return indice < tamanoNivel ? indice : -1;
		}
	}
}
