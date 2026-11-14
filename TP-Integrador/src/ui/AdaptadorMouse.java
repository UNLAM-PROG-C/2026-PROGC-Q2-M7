package ui;

import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;
import model.Celda;
import model.Direccion;
import model.EstadoJuego;

/**
 * Traduce un arrastre del mouse sobre el tablero en movimientos.
 *
 * <p>El trazo se arma sobre el snapshot visible al apretar y solo se confirma al soltar,
 * publicando una {@link Direccion} por paso, igual que el teclado. Bus, motor y modelo no se
 * enteran del mouse.
 */
final class AdaptadorMouse extends MouseAdapter {
	private final PanelTablero tablero;
	private final Consumer<Direccion> mover;
	private TrazoMouse trazo;
	private EstadoJuego ultimoConfirmado;
	private boolean habilitado;

	AdaptadorMouse(PanelTablero tablero, Consumer<Direccion> mover) {
		this.tablero = tablero;
		this.mover = mover;
	}

	void instalar() {
		tablero.addMouseListener(this);
		tablero.addMouseMotionListener(this);
		tablero.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent evento) {
				cancelar();
			}
		});
	}

	void habilitar(boolean habilitado) {
		this.habilitado = habilitado;
		ultimoConfirmado = null;
		cancelar();
	}

	@Override
	public void mousePressed(MouseEvent evento) {
		cancelar();
		if (habilitado && evento.getButton() == MouseEvent.BUTTON1) {
			iniciarTrazo(evento.getPoint());
		}
	}

	@Override
	public void mouseDragged(MouseEvent evento) {
		if (!habilitado || trazo == null) {
			return;
		}
		if (!estadoSinCambios()) {
			cancelar();
			return;
		}
		tablero.celdaEn(evento.getPoint()).filter(trazo::extenderHacia)
				.ifPresent(celda -> tablero.mostrarTrazo(trazo.celdas()));
	}

	@Override
	public void mouseReleased(MouseEvent evento) {
		if (!habilitado || trazo == null || evento.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		try {
			confirmar(evento.getPoint());
		} finally {
			cancelar();
		}
	}

	private void iniciarTrazo(Point punto) {
		Optional<EstadoJuego> estado = tablero.estadoActual();
		Optional<Celda> celda = tablero.celdaEn(punto);
		if (estado.isEmpty() || celda.isEmpty() || esperandoActualizacion(estado.get())) {
			return;
		}
		if (celda.get().equals(estado.get().actual())) {
			trazo = new TrazoMouse(estado.get());
			tablero.requestFocusInWindow();
		}
	}

	private void confirmar(Point punto) {
		if (tablero.contains(punto) && estadoSinCambios() && !trazo.celdas().isEmpty()) {
			ultimoConfirmado = trazo.origen();
			trazo.direcciones().forEach(mover);
		}
	}

	/** El motor todavía no devolvió el estado posterior al último trazo confirmado. */
	private boolean esperandoActualizacion(EstadoJuego estado) {
		return ultimoConfirmado != null && mismoEstado(estado, ultimoConfirmado);
	}

	private boolean estadoSinCambios() {
		return tablero.estadoActual()
				.filter(actual -> mismoEstado(actual, trazo.origen()))
				.isPresent();
	}

	private static boolean mismoEstado(EstadoJuego uno, EstadoJuego otro) {
		return uno.nivel() == otro.nivel() && uno.recorrido().equals(otro.recorrido());
	}

	private void cancelar() {
		trazo = null;
		tablero.limpiarTrazo();
	}
}
