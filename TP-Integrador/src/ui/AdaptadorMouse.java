package ui;

import events.DestinoCelda;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import model.Celda;
import model.EstadoJuego;
import model.Nivel;

/**
 * Traduce el arrastre del mouse sobre el tablero en pedidos de "ir a esta celda".
 *
 * <p>No calcula movimientos: publica cada celda que cruza el cursor y el motor, que tiene el
 * estado real, decide si eso avanza, retrocede o no hace nada. El gesto empieza apretando sobre
 * una celda del camino (la punta para seguir, una anterior para volver hasta ahí).
 */
final class AdaptadorMouse extends MouseAdapter {
	private final PanelTablero tablero;
	private final Consumer<DestinoCelda> irA;
	private Celda ultimaCelda;
	private Nivel nivelDelGesto;
	private boolean arrastrando;

	AdaptadorMouse(PanelTablero tablero, Consumer<DestinoCelda> irA) {
		this.tablero = tablero;
		this.irA = irA;
	}

	void instalar() {
		tablero.addMouseListener(this);
		tablero.addMouseMotionListener(this);
		tablero.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent evento) {
				terminar();
			}
		});
	}

	void reiniciarGesto() {
		terminar();
		ultimaCelda = null;
		nivelDelGesto = null;
	}

	@Override
	public void mousePressed(MouseEvent evento) {
		terminar();
		if (evento.getButton() == MouseEvent.BUTTON1) {
			iniciar(evento.getPoint());
		}
	}

	@Override
	public void mouseDragged(MouseEvent evento) {
		if (!arrastrando) {
			return;
		}
		tablero.celdaEn(evento.getPoint())
				.filter(celda -> !celda.equals(ultimaCelda))
				.ifPresent(this::recorrerHasta);
	}

	@Override
	public void mouseReleased(MouseEvent evento) {
		if (evento.getButton() == MouseEvent.BUTTON1) {
			terminar();
		}
	}

	/** El gesto empieza solo sobre el camino visible; qué pasa después lo decide el motor. */
	private void iniciar(Point punto) {
		Optional<EstadoJuego> estado = tablero.estadoActual();
		Optional<Celda> celda = tablero.celdaEn(punto);
		if (estado.isEmpty() || celda.isEmpty() || !puedeEmpezarEn(estado.get(), celda.get())) {
			return;
		}
		nivelDelGesto = estado.get().nivel();
		ultimaCelda = celda.get();
		arrastrando = true;
		publicar(ultimaCelda);
		tablero.requestFocusInWindow();
	}

	/** Sobre el camino visible, o donde terminó el gesto anterior si el motor aún no lo mostró. */
	private boolean puedeEmpezarEn(EstadoJuego estado, Celda celda) {
		return estado.recorrido().contains(celda)
				|| (estado.nivel() == nivelDelGesto && celda.equals(ultimaCelda));
	}

	/** Publica también las celdas intermedias que un arrastre rápido salteó. */
	private void recorrerHasta(Celda destino) {
		for (Celda celda : celdasEntre(ultimaCelda, destino)) {
			publicar(celda);
		}
		ultimaCelda = destino;
	}

	private void publicar(Celda celda) {
		irA.accept(new DestinoCelda(nivelDelGesto, celda));
	}

	private void terminar() {
		arrastrando = false;
	}

	/** Celdas de un paso cada una que van de desde (excluida) a hasta (incluida). */
	private static List<Celda> celdasEntre(Celda desde, Celda hasta) {
		List<Celda> celdas = new ArrayList<>();
		Celda actual = desde;
		while (!actual.equals(hasta)) {
			actual = pasoHacia(actual, hasta);
			celdas.add(actual);
		}
		return celdas;
	}

	private static Celda pasoHacia(Celda desde, Celda hasta) {
		int filas = hasta.fila() - desde.fila();
		int columnas = hasta.columna() - desde.columna();
		if (Math.abs(columnas) >= Math.abs(filas)) {
			return new Celda(desde.fila(), desde.columna() + Integer.signum(columnas));
		}
		return new Celda(desde.fila() + Integer.signum(filas), desde.columna());
	}
}
