package zip.interfaz;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JPanel;
import zip.dominio.Celda;
import zip.dominio.EstadoJuego;
import zip.dominio.Nivel;

final class PanelTablero extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAMANO_TABLERO = 480;
	private static final int RELLENO = 12;
	private static final int SEPARACION = 5;
	private static final int RADIO_ESQUINA = 14;
	private static final int DIVISOR_NODO = 3;
	private static final int DIVISOR_FUENTE = 4;
	private static final float PROPORCION_RECORRIDO = 0.13f;
	private static final float GROSOR_ANILLO = 3.0f;
	private transient Optional<EstadoJuego> estado = Optional.empty();

	PanelTablero() {
		setName("tablero");
		setBackground(Estilo.FONDO);
		setPreferredSize(new Dimension(TAMANO_TABLERO, TAMANO_TABLERO));
	}

	void mostrar(EstadoJuego estado) {
		this.estado = Optional.of(estado);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		super.paintComponent(graficos);
		if (estado.isPresent()) {
			Graphics2D dibujo = (Graphics2D) graficos.create();
			try {
				dibujo.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				dibujarTablero(dibujo, geometry());
			} finally {
				dibujo.dispose();
			}
		}
	}

	private EstadoJuego estadoJuego() {
		return estado.orElseThrow();
	}

	private Geometria geometry() {
		int tamano = Math.max(1, (Math.min(getWidth(), getHeight()) - 2 * RELLENO) / Nivel.TAMANO);
		return new Geometria(tamano, (getWidth() - tamano * Nivel.TAMANO) / 2,
				(getHeight() - tamano * Nivel.TAMANO) / 2);
	}

	private void dibujarTablero(Graphics2D dibujo, Geometria grilla) {
		Set<Celda> visitadas = new HashSet<>(estadoJuego().recorrido());
		for (int fila = 0; fila < Nivel.TAMANO; fila++) {
			for (int columna = 0; columna < Nivel.TAMANO; columna++) {
				dibujarCelda(dibujo, grilla, new Celda(fila, columna), visitadas);
			}
		}
		dibujarRecorrido(dibujo, grilla);
		dibujarNumeros(dibujo, grilla);
		dibujarActual(dibujo, grilla);
	}

	private void dibujarCelda(Graphics2D dibujo, Geometria grilla, Celda celda, Set<Celda> visitadas) {
		dibujo.setColor(colorCelda(celda, visitadas.contains(celda)));
		dibujo.fillRoundRect(grilla.x(celda) - grilla.tamano() / 2 + SEPARACION,
				grilla.y(celda) - grilla.tamano() / 2 + SEPARACION, grilla.tamano() - 2 * SEPARACION,
				grilla.tamano() - 2 * SEPARACION, RADIO_ESQUINA, RADIO_ESQUINA);
	}

	private Color colorCelda(Celda celda, boolean visitadas) {
		int numero = estadoJuego().nivel().numeroEn(celda);
		if (numero == 1) {
			return Estilo.INICIO;
		}
		if (numero == estadoJuego().nivel().puntos()) {
			return Estilo.FIN;
		}
		return visitadas ? Estilo.VISITADA : Color.WHITE;
	}

	private void dibujarRecorrido(Graphics2D dibujo, Geometria grilla) {
		dibujo.setColor(Estilo.ACENTO);
		dibujo.setStroke(new BasicStroke(grilla.tamano() * PROPORCION_RECORRIDO, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		List<Celda> recorrido = estadoJuego().recorrido();
		for (int indice = 1; indice < recorrido.size(); indice++) {
			Celda anterior = recorrido.get(indice - 1);
			Celda actual = recorrido.get(indice);
			dibujo.drawLine(grilla.x(anterior), grilla.y(anterior), grilla.x(actual), grilla.y(actual));
		}
	}

	private void dibujarNumeros(Graphics2D dibujo, Geometria grilla) {
		dibujo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, grilla.tamano() / DIVISOR_FUENTE));
		for (int fila = 0; fila < Nivel.TAMANO; fila++) {
			for (int columna = 0; columna < Nivel.TAMANO; columna++) {
				Celda celda = new Celda(fila, columna);
				if (estadoJuego().nivel().numeroEn(celda) > 0) {
					dibujarNumero(dibujo, grilla, celda);
				}
			}
		}
	}

	private void dibujarNumero(Graphics2D dibujo, Geometria grilla, Celda celda) {
		int diametro = grilla.tamano() / DIVISOR_NODO;
		dibujo.setColor(Color.WHITE);
		dibujo.fillOval(grilla.x(celda) - diametro / 2, grilla.y(celda) - diametro / 2, diametro, diametro);
		dibujo.setColor(Estilo.TINTA);
		String texto = Integer.toString(estadoJuego().nivel().numeroEn(celda));
		FontMetrics metricas = dibujo.getFontMetrics();
		dibujo.drawString(texto, grilla.x(celda) - metricas.stringWidth(texto) / 2,
				grilla.y(celda) + (metricas.getAscent() - metricas.getDescent()) / 2);
	}

	private void dibujarActual(Graphics2D dibujo, Geometria grilla) {
		int radio = grilla.tamano() / DIVISOR_FUENTE;
		Celda actual = estadoJuego().actual();
		dibujo.setColor(Estilo.ACENTO);
		dibujo.setStroke(new BasicStroke(GROSOR_ANILLO));
		dibujo.drawOval(grilla.x(actual) - radio, grilla.y(actual) - radio, 2 * radio, 2 * radio);
	}

	private record Geometria(int tamano, int izquierda, int superior) {
		int x(Celda celda) {
			return izquierda + celda.columna() * tamano + tamano / 2;
		}

		int y(Celda celda) {
			return superior + celda.fila() * tamano + tamano / 2;
		}
	}
}
