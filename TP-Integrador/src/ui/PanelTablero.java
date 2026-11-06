package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JPanel;
import model.Celda;
import model.EstadoJuego;

final class PanelTablero extends JPanel {
	private static final long serialVersionUID = 1L;
	private transient Optional<EstadoJuego> estado = Optional.empty();
	private boolean manchadoPorTomate;

	PanelTablero() {
		setName("tablero");
		setBackground(Tema.FONDO);
		setPreferredSize(new Dimension(Tema.TAMANO_PREFERIDO_TABLERO,
				Tema.TAMANO_PREFERIDO_TABLERO));
	}

	void mostrar(EstadoJuego estado, boolean manchadoPorTomate) {
		this.estado = Optional.of(estado);
		this.manchadoPorTomate = manchadoPorTomate;
		repaint();
	}

	void mostrar(EstadoJuego estado) {
		mostrar(estado, false);
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		super.paintComponent(graficos);
		if (estado.isEmpty()) {
			return;
		}
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			Geometria grilla = geometria();
			dibujarTablero(dibujo, grilla);
			if (manchadoPorTomate) {
				dibujarTomate(dibujo, grilla);
			}
		} finally {
			dibujo.dispose();
		}
	}

	private EstadoJuego estadoJuego() {
		return estado.orElseThrow();
	}

	private Geometria geometria() {
		int ladoTablero = Math.min(getWidth(), getHeight());
		int izquierdaTablero = (getWidth() - ladoTablero) / 2;
		int superiorTablero = (getHeight() - ladoTablero) / 2;
		int tamanoNivel = estadoJuego().nivel().tamano();
		int espacioGrilla = ladoTablero - 2 * Tema.RELLENO_TABLERO;
		int celda = (espacioGrilla - (tamanoNivel - 1) * Tema.SEPARACION_CELDAS) / tamanoNivel;
		int ladoGrilla = celda * tamanoNivel + (tamanoNivel - 1) * Tema.SEPARACION_CELDAS;
		int izquierdaGrilla = izquierdaTablero + (ladoTablero - ladoGrilla) / 2;
		int superiorGrilla = superiorTablero + (ladoTablero - ladoGrilla) / 2;
		return new Geometria(izquierdaTablero, superiorTablero, ladoTablero,
				izquierdaGrilla, superiorGrilla, celda, tamanoNivel);
	}

	private void dibujarTablero(Graphics2D dibujo, Geometria grilla) {
		dibujo.setColor(Tema.FONDO_TABLERO);
		dibujo.fill(Tema.redondeado(grilla.izquierdaTablero(), grilla.superiorTablero(),
				grilla.ladoTablero(), grilla.ladoTablero(), Tema.RADIO_TABLERO));
		Set<Celda> visitadas = new HashSet<>(estadoJuego().recorrido());
		dibujarCeldas(dibujo, grilla, visitadas);
		dibujarRecorrido(dibujo, grilla);
		dibujarActual(dibujo, grilla);
		dibujarNumeros(dibujo, grilla);
	}

	private void dibujarCeldas(Graphics2D dibujo, Geometria grilla, Set<Celda> visitadas) {
		for (int fila = 0; fila < grilla.tamanoNivel(); fila++) {
			for (int columna = 0; columna < grilla.tamanoNivel(); columna++) {
				Celda celda = new Celda(fila, columna);
				dibujarCelda(dibujo, grilla, celda, visitadas.contains(celda));
			}
		}
	}

	private void dibujarCelda(Graphics2D dibujo, Geometria grilla, Celda celda, boolean visitadas) {
		Color color = visitadas ? Tema.VISITADA : Tema.TARJETA;
		if (celda.equals(estadoJuego().actual())) {
			color = Tema.ACENTO;
		}
		dibujo.setColor(color);
		dibujo.fill(Tema.redondeado(grilla.x(celda), grilla.y(celda), grilla.celda(),
				grilla.celda(), Tema.RADIO_CELDA));
	}

	private void dibujarRecorrido(Graphics2D dibujo, Geometria grilla) {
		List<Celda> recorrido = estadoJuego().recorrido();
		dibujo.setColor(Tema.ACENTO);
		dibujo.setStroke(new BasicStroke(Tema.GROSOR_RECORRIDO,
				BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int indice = 1; indice < recorrido.size(); indice++) {
			dibujo.drawLine(grilla.centroX(recorrido.get(indice - 1)),
					grilla.centroY(recorrido.get(indice - 1)),
					grilla.centroX(recorrido.get(indice)), grilla.centroY(recorrido.get(indice)));
		}
	}

	private void dibujarActual(Graphics2D dibujo, Geometria grilla) {
		Celda actual = estadoJuego().actual();
		int x = grilla.centroX(actual) - Tema.PUNTO_ACTUAL / 2;
		int y = grilla.centroY(actual) - Tema.PUNTO_ACTUAL / 2;
		dibujo.setColor(Tema.TARJETA);
		dibujo.fillOval(x, y, Tema.PUNTO_ACTUAL, Tema.PUNTO_ACTUAL);
	}

	private void dibujarNumeros(Graphics2D dibujo, Geometria grilla) {
		for (int fila = 0; fila < grilla.tamanoNivel(); fila++) {
			for (int columna = 0; columna < grilla.tamanoNivel(); columna++) {
				Celda celda = new Celda(fila, columna);
				int numero = estadoJuego().nivel().numeroEn(celda);
				if (numero > 0) {
					dibujarNumero(dibujo, grilla, celda, numero);
				}
			}
		}
	}

	private void dibujarNumero(Graphics2D dibujo, Geometria grilla, Celda celda, int numero) {
		int diametro = Math.min(Tema.DIAMETRO_NUMERO,
				grilla.celda() - 2 * Tema.GROSOR_BORDE);
		int x = grilla.centroX(celda) - diametro / 2;
		int y = grilla.centroY(celda) - diametro / 2;
		if (numero < estadoJuego().proximoNumero()) {
			rellenarNumero(dibujo, x, y, diametro, Tema.ACENTO, Tema.TARJETA);
		} else if (numero == estadoJuego().proximoNumero()) {
			delinearNumero(dibujo, x, y, diametro);
		} else {
			rellenarNumero(dibujo, x, y, diametro, Tema.TINTA, Tema.TARJETA);
		}
		dibujarTextoNumero(dibujo, grilla, celda, numero);
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
		int margen = Tema.BORDE_NUMERO;
		dibujo.setColor(Tema.TARJETA);
		dibujo.fill(new Ellipse2D.Float(x + margen, y + margen,
				diametro - 2 * margen, diametro - 2 * margen));
		dibujo.setColor(Tema.TINTA);
	}

	private void dibujarTextoNumero(Graphics2D dibujo, Geometria grilla, Celda celda,
			int numero) {
		dibujo.setFont(Tema.fuente(Tema.TAMANO_NUMERO, true));
		String texto = Integer.toString(numero);
		FontMetrics metricas = dibujo.getFontMetrics();
		int x = grilla.centroX(celda) - metricas.stringWidth(texto) / 2;
		int y = grilla.centroY(celda) + (metricas.getAscent() - metricas.getDescent()) / 2;
		dibujo.drawString(texto, x, y);
	}

	private void dibujarTomate(Graphics2D dibujo, Geometria grilla) {
		int centroX = grilla.izquierdaTablero() + grilla.ladoTablero() / 2;
		int centroY = grilla.superiorTablero() + grilla.ladoTablero() / 2;
		int radio = (int) (grilla.ladoTablero() * 0.40);

		// Splat blobs overlay
		dibujo.setColor(new Color(220, 38, 38, 220));
		dibujo.fillOval(centroX - radio, centroY - radio, radio * 2, radio * 2);

		// Splatter droplets
		int rBump = (int) (radio * 0.45);
		dibujo.fillOval(centroX - radio - rBump / 3, centroY - rBump, rBump * 2, rBump * 2);
		dibujo.fillOval(centroX + radio - rBump, centroY - rBump / 2, rBump * 2, rBump * 2);
		dibujo.fillOval(centroX - rBump, centroY - radio - rBump / 3, rBump * 2, rBump * 2);
		dibujo.fillOval(centroX - rBump / 2, centroY + radio - rBump, rBump * 2, rBump * 2);

		int rGota = (int) (radio * 0.22);
		dibujo.fillOval(centroX - radio - rGota * 2, centroY + radio / 2, rGota * 2, rGota * 2);
		dibujo.fillOval(centroX + radio + rGota, centroY - radio / 2, rGota * 2, rGota * 2);

		// Tomato stem (green leaves)
		dibujo.setColor(new Color(34, 197, 94));
		int anchoTallo = (int) (radio * 0.35);
		int altoTallo = (int) (radio * 0.20);
		dibujo.fillOval(centroX - anchoTallo / 2, centroY - radio - altoTallo / 2, anchoTallo, altoTallo);
		int[] talloX = {centroX - anchoTallo / 3, centroX, centroX + anchoTallo / 3};
		int[] talloY = {centroY - radio, centroY - radio - altoTallo, centroY - radio};
		dibujo.fillPolygon(talloX, talloY, 3);

		// "¡TOMATAZO!" banner
		dibujo.setFont(Tema.fuente(Tema.TAMANO_TITULO_PROXIMAMENTE, true));
		FontMetrics fm = dibujo.getFontMetrics();
		String texto = "¡TOMATAZO!";
		int textX = centroX - fm.stringWidth(texto) / 2;
		int textY = centroY + (fm.getAscent() - fm.getDescent()) / 2;

		// Text shadow
		dibujo.setColor(new Color(0, 0, 0, 160));
		dibujo.drawString(texto, textX + 2, textY + 2);
		// Text
		dibujo.setColor(Color.WHITE);
		dibujo.drawString(texto, textX, textY);
	}

	private record Geometria(int izquierdaTablero, int superiorTablero, int ladoTablero,
			int izquierdaGrilla, int superiorGrilla, int celda, int tamanoNivel) {
		int x(Celda posicion) {
			return izquierdaGrilla + posicion.columna() * (celda + Tema.SEPARACION_CELDAS);
		}

		int y(Celda posicion) {
			return superiorGrilla + posicion.fila() * (celda + Tema.SEPARACION_CELDAS);
		}

		int centroX(Celda posicion) {
			return x(posicion) + celda / 2;
		}

		int centroY(Celda posicion) {
			return y(posicion) + celda / 2;
		}
	}
}
