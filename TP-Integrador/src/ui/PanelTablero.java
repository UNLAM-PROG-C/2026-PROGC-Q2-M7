package ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.swing.JPanel;
import model.Celda;
import model.EstadoJuego;

final class PanelTablero extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int RELLENO_TABLERO = 12;
	private static final int TAMANO_PREFERIDO_TABLERO = 440;
	private static final int SEPARACION_CELDAS = 8;
	private static final int RADIO_CELDA = 12;
	private static final int DIAMETRO_NUMERO = 40;
	private static final int BORDE_NUMERO = 3;
	private static final int TAMANO_NUMERO = 16;
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

	private transient Optional<EstadoJuego> estado = Optional.empty();
	private boolean[][] visitadas = new boolean[0][0];
	private boolean manchadoPorTomate;

	PanelTablero() {
		setName("tablero");
		setBackground(Tema.FONDO);
		setPreferredSize(new Dimension(TAMANO_PREFERIDO_TABLERO, TAMANO_PREFERIDO_TABLERO));
	}

	void mostrar(EstadoJuego nuevoEstado, boolean manchado) {
		this.estado = Optional.of(nuevoEstado);
		this.manchadoPorTomate = manchado;
		actualizarVisitadas(nuevoEstado);
		repaint();
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
		if (estado.isEmpty()) {
			return;
		}
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			dibujarEscena(dibujo);
		} finally {
			dibujo.dispose();
		}
	}

	private void dibujarEscena(Graphics2D dibujo) {
		Tema.preparar(dibujo);
		Geometria grilla = geometria();
		dibujarTablero(dibujo, grilla);
		if (manchadoPorTomate) {
			dibujarTomate(dibujo, grilla);
		}
	}

	private EstadoJuego estadoJuego() {
		return estado.orElseThrow();
	}

	private Geometria geometria() {
		int ladoTablero = Math.max(1, Math.min(getWidth(), getHeight()));
		int izquierdaTablero = (getWidth() - ladoTablero) / 2;
		int superiorTablero = (getHeight() - ladoTablero) / 2;
		int tamanoNivel = estadoJuego().nivel().tamano();
		int espacioGrilla = ladoTablero - 2 * RELLENO_TABLERO;
		int separacion = (tamanoNivel - 1) * SEPARACION_CELDAS;
		int celda = Math.max(1, (espacioGrilla - separacion) / tamanoNivel);
		int ladoGrilla = celda * tamanoNivel + separacion;
		return new Geometria(izquierdaTablero, superiorTablero, ladoTablero,
				izquierdaTablero + (ladoTablero - ladoGrilla) / 2,
				superiorTablero + (ladoTablero - ladoGrilla) / 2, celda, tamanoNivel);
	}

	private void dibujarTablero(Graphics2D dibujo, Geometria grilla) {
		dibujo.setColor(Tema.FONDO_TABLERO);
		dibujo.fill(Tema.redondeado(grilla.izquierdaTablero(), grilla.superiorTablero(),
				grilla.ladoTablero(), grilla.ladoTablero(), Tema.RADIO_TABLERO));
		dibujarCeldas(dibujo, grilla);
		dibujarRecorrido(dibujo, grilla);
		dibujarActual(dibujo, grilla);
		dibujarNumeros(dibujo, grilla);
	}

	private void dibujarCeldas(Graphics2D dibujo, Geometria grilla) {
		Celda actual = estadoJuego().actual();
		for (int fila = 0; fila < grilla.tamanoNivel(); fila++) {
			for (int columna = 0; columna < grilla.tamanoNivel(); columna++) {
				boolean esActual = fila == actual.fila() && columna == actual.columna();
				dibujarCelda(dibujo, grilla, fila, columna, esActual);
			}
		}
	}

	private void dibujarCelda(Graphics2D dibujo, Geometria grilla, int fila, int columna,
			boolean esActual) {
		Color color = esActual ? Tema.ACENTO
				: (visitadas[fila][columna] ? Tema.VISITADA : Tema.TARJETA);
		dibujo.setColor(color);
		dibujo.fill(Tema.redondeado(grilla.x(columna), grilla.y(fila), grilla.celda(),
				grilla.celda(), RADIO_CELDA));
	}

	private void dibujarRecorrido(Graphics2D dibujo, Geometria grilla) {
		List<Celda> recorrido = estadoJuego().recorrido();
		dibujo.setColor(Tema.ACENTO);
		dibujo.setStroke(Tema.trazo(GROSOR_RECORRIDO));
		for (int indice = 1; indice < recorrido.size(); indice++) {
			Celda anterior = recorrido.get(indice - 1);
			Celda siguiente = recorrido.get(indice);
			dibujo.drawLine(grilla.centroX(anterior.columna()), grilla.centroY(anterior.fila()),
					grilla.centroX(siguiente.columna()), grilla.centroY(siguiente.fila()));
		}
	}

	private void dibujarActual(Graphics2D dibujo, Geometria grilla) {
		Celda actual = estadoJuego().actual();
		int x = grilla.centroX(actual.columna()) - PUNTO_ACTUAL / 2;
		int y = grilla.centroY(actual.fila()) - PUNTO_ACTUAL / 2;
		dibujo.setColor(Tema.TARJETA);
		dibujo.fillOval(x, y, PUNTO_ACTUAL, PUNTO_ACTUAL);
	}

	private void dibujarNumeros(Graphics2D dibujo, Geometria grilla) {
		List<Integer> numeros = estadoJuego().nivel().numeros();
		int tamano = grilla.tamanoNivel();
		for (int fila = 0; fila < tamano; fila++) {
			for (int columna = 0; columna < tamano; columna++) {
				int numero = numeros.get(fila * tamano + columna);
				if (numero > 0) {
					dibujarNumero(dibujo, grilla, fila, columna, numero);
				}
			}
		}
	}

	private void dibujarNumero(Graphics2D dibujo, Geometria grilla, int fila, int columna,
			int numero) {
		int diametro = Math.max(1, Math.min(DIAMETRO_NUMERO,
				grilla.celda() - 2 * Tema.GROSOR_BORDE));
		int x = grilla.centroX(columna) - diametro / 2;
		int y = grilla.centroY(fila) - diametro / 2;
		if (numero < estadoJuego().proximoNumero()) {
			rellenarNumero(dibujo, x, y, diametro, Tema.ACENTO, Tema.TARJETA);
		} else if (numero == estadoJuego().proximoNumero()) {
			delinearNumero(dibujo, x, y, diametro);
		} else {
			rellenarNumero(dibujo, x, y, diametro, Tema.TINTA, Tema.TARJETA);
		}
		dibujarTextoNumero(dibujo, grilla, fila, columna, numero);
	}

	private void rellenarNumero(Graphics2D dibujo, int x, int y, int diametro,
			Color relleno, Color texto) {
		dibujo.setColor(relleno);
		dibujo.fill(new Ellipse2D.Float(x, y, diametro, diametro));
		dibujo.setColor(texto);
	}

	private void delinearNumero(Graphics2D dibujo, int x, int y, int diametro) {
		dibujo.setColor(Tema.ACENTO);
		dibujo.fill(new Ellipse2D.Float(x, y, diametro, diametro));
		int interior = diametro - 2 * BORDE_NUMERO;
		if (interior > 0) {
			dibujo.setColor(Tema.TARJETA);
			dibujo.fill(new Ellipse2D.Float(x + BORDE_NUMERO, y + BORDE_NUMERO,
					interior, interior));
		}
		dibujo.setColor(Tema.TINTA);
	}

	private void dibujarTextoNumero(Graphics2D dibujo, Geometria grilla, int fila, int columna,
			int numero) {
		dibujo.setFont(Tema.fuente(TAMANO_NUMERO, true));
		String texto = Integer.toString(numero);
		FontMetrics metricas = dibujo.getFontMetrics();
		int x = grilla.centroX(columna) - metricas.stringWidth(texto) / 2;
		int y = grilla.centroY(fila) + (metricas.getAscent() - metricas.getDescent()) / 2;
		dibujo.drawString(texto, x, y);
	}

	private void dibujarTomate(Graphics2D dibujo, Geometria grilla) {
		int centroX = grilla.izquierdaTablero() + grilla.ladoTablero() / 2;
		int centroY = grilla.superiorTablero() + grilla.ladoTablero() / 2;
		int radio = Math.max(1, (int) (grilla.ladoTablero() * ESCALA_RADIO_TOMATE));
		dibujarSalpicaduraTomate(dibujo, centroX, centroY, radio);
		dibujarGotasTomate(dibujo, centroX, centroY, radio);
		dibujarTalloTomate(dibujo, centroX, centroY, radio);
		dibujarTextoTomate(dibujo, centroX, centroY);
	}

	private void dibujarSalpicaduraTomate(Graphics2D dibujo, int centroX, int centroY, int radio) {
		dibujo.setColor(Tema.TOMATE_CUERPO);
		dibujo.fillOval(centroX - radio, centroY - radio, radio * 2, radio * 2);
		int bulto = (int) (radio * ESCALA_BULTOS_TOMATE);
		dibujo.fillOval(centroX - radio - bulto / DIVISOR_TERCIO, centroY - bulto, bulto * 2, bulto * 2);
		dibujo.fillOval(centroX + radio - bulto, centroY - bulto / 2, bulto * 2, bulto * 2);
		dibujo.fillOval(centroX - bulto, centroY - radio - bulto / DIVISOR_TERCIO, bulto * 2, bulto * 2);
		dibujo.fillOval(centroX - bulto / 2, centroY + radio - bulto, bulto * 2, bulto * 2);
		dibujo.setColor(Tema.TOMATE_SOMBRA);
		dibujo.fillOval(centroX - radio / 2, centroY - radio / 2, radio, radio);
	}

	private void dibujarGotasTomate(Graphics2D dibujo, int centroX, int centroY, int radio) {
		int gota = (int) (radio * ESCALA_GOTA_TOMATE);
		int brillo = (int) (radio * ESCALA_BRILLO_TOMATE);
		dibujo.setColor(Tema.TOMATE_CUERPO);
		dibujo.fillOval(centroX - radio - gota * 2, centroY + radio / 2, gota * 2, gota * 2);
		dibujo.fillOval(centroX + radio + gota, centroY - radio / 2, gota * 2, gota * 2);
		dibujo.setColor(Tema.TOMATE_BRILLO);
		dibujo.fillOval(centroX - radio / 2 - brillo / 2, centroY - radio / 2 - brillo / 2,
				brillo, brillo);
	}

	private void dibujarTalloTomate(Graphics2D dibujo, int centroX, int centroY, int radio) {
		dibujo.setColor(Tema.TOMATE_HOJA);
		int anchoTallo = (int) (radio * ESCALA_ANCHO_TALLO);
		int altoTallo = (int) (radio * ESCALA_ALTO_TALLO);
		dibujo.fillOval(centroX - anchoTallo / 2, centroY - radio - altoTallo / 2,
				anchoTallo, altoTallo);
		int[] talloX = {centroX - anchoTallo / DIVISOR_TERCIO, centroX,
				centroX + anchoTallo / DIVISOR_TERCIO};
		int[] talloY = {centroY - radio, centroY - radio - altoTallo, centroY - radio};
		dibujo.fillPolygon(talloX, talloY, VERTICES_TALLO);
	}

	private void dibujarTextoTomate(Graphics2D dibujo, int centroX, int centroY) {
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TITULO_GRANDE, true));
		FontMetrics metricas = dibujo.getFontMetrics();
		int x = centroX - metricas.stringWidth(TEXTO_TOMATE) / 2;
		int y = centroY + (metricas.getAscent() - metricas.getDescent()) / 2;
		dibujo.setColor(Tema.TOMATE_SOMBRA_TEXTO);
		dibujo.drawString(TEXTO_TOMATE, x + 2, y + 2);
		dibujo.setColor(Tema.TOMATE_TEXTO);
		dibujo.drawString(TEXTO_TOMATE, x, y);
	}

	private record Geometria(int izquierdaTablero, int superiorTablero, int ladoTablero,
			int izquierdaGrilla, int superiorGrilla, int celda, int tamanoNivel) {
		int x(int columna) {
			return izquierdaGrilla + columna * (celda + SEPARACION_CELDAS);
		}

		int y(int fila) {
			return superiorGrilla + fila * (celda + SEPARACION_CELDAS);
		}

		int centroX(int columna) {
			return x(columna) + celda / 2;
		}

		int centroY(int fila) {
			return y(fila) + celda / 2;
		}
	}
}
