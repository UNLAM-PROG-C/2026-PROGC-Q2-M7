package ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;
import model.Direccion;
import model.EstadoJuego;

/**
 * Traduce el arrastre del mouse sobre el tablero en movimientos y deshaceres inmediatos.
 *
 * <p>Se aprieta sobre cualquier celda del camino (la punta para seguir, una anterior para volver
 * hasta ahí) y se arrastra: hacia adelante avanza y sobre el camino retrocede. Publica los mismos
 * eventos que el teclado; bus, motor y modelo no se enteran del mouse.
 */
final class AdaptadorMouse extends MouseAdapter {
	private final PanelTablero tablero;
	private final Consumer<Direccion> mover;
	private final Runnable deshacer;
	private TrazoMouse camino;
	private boolean arrastrando;
	private boolean habilitado;

	AdaptadorMouse(PanelTablero tablero, Consumer<Direccion> mover, Runnable deshacer) {
		this.tablero = tablero;
		this.mover = mover;
		this.deshacer = deshacer;
	}

	void instalar() {
		tablero.addMouseListener(this);
		tablero.addMouseMotionListener(this);
		tablero.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent evento) {
				arrastrando = false;
			}
		});
	}

	void habilitar(boolean habilitado) {
		this.habilitado = habilitado;
		descartar();
	}

	/** Olvida la copia local: el camino cambió por fuera del mouse (botón deshacer, reiniciar). */
	void descartar() {
		camino = null;
		arrastrando = false;
	}

	@Override
	public void mousePressed(MouseEvent evento) {
		arrastrando = false;
		if (!habilitado || evento.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		sincronizar();
		if (camino != null) {
			tablero.celdaEn(evento.getPoint()).filter(camino::contiene).ifPresent(celda -> {
				camino.llevarHacia(celda);
				arrastrando = true;
				tablero.requestFocusInWindow();
			});
		}
	}

	@Override
	public void mouseDragged(MouseEvent evento) {
		if (!habilitado || !arrastrando) {
			return;
		}
		if (!sigueSincronizado()) {
			descartar();
			return;
		}
		tablero.celdaEn(evento.getPoint()).ifPresent(camino::llevarHacia);
	}

	@Override
	public void mouseReleased(MouseEvent evento) {
		if (evento.getButton() == MouseEvent.BUTTON1) {
			arrastrando = false;
		}
	}

	/** Reusa la copia local mientras el motor solo esté atrasado; si no, la rehace del snapshot. */
	private void sincronizar() {
		Optional<EstadoJuego> estado = tablero.estadoActual();
		if (estado.isEmpty()) {
			camino = null;
		} else if (camino == null || !camino.conoce(estado.get())) {
			camino = new TrazoMouse(estado.get(), mover, deshacer);
		}
	}

	private boolean sigueSincronizado() {
		return tablero.estadoActual().filter(camino::conoce).isPresent();
	}
}
