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
 * Traduce gestos del mouse sobre el tablero en acciones del juego.
 *
 * <p>Sin Backspace, un arrastre desde la celda actual traza camino nuevo ({@link TrazoMouse}).
 * Con Backspace apretado, un arrastre sobre el camino confirmado lo recorta ({@link RecorteMouse}).
 * El gesto se arma sobre el snapshot visible al apretar y solo se confirma al soltar, publicando
 * los mismos eventos que el teclado: bus, motor y modelo no se enteran del mouse.
 */
final class AdaptadorMouse extends MouseAdapter {
	private final PanelTablero tablero;
	private final Consumer<Direccion> mover;
	private final Runnable deshacer;
	private GestoMouse gesto;
	private EstadoJuego ultimoConfirmado;
	private boolean habilitado;
	private boolean edicion;
	private boolean edicionUsada;

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
				cancelar();
			}
		});
	}

	void habilitar(boolean habilitado) {
		this.habilitado = habilitado;
		ultimoConfirmado = null;
		edicion = false;
		cancelar();
	}

	/** Backspace apretado: el próximo click sobre el camino entra en edición. */
	void presionarBorrar() {
		if (!edicion) {
			edicion = true;
			edicionUsada = false;
		}
	}

	/** Backspace soltado: devuelve true si durante la pulsación se editó con el mouse. */
	boolean soltarBorrar() {
		edicion = false;
		return edicionUsada;
	}

	@Override
	public void mousePressed(MouseEvent evento) {
		cancelar();
		if (habilitado && evento.getButton() == MouseEvent.BUTTON1) {
			iniciarGesto(evento.getPoint());
		}
	}

	@Override
	public void mouseDragged(MouseEvent evento) {
		if (!habilitado || gesto == null) {
			return;
		}
		if (!estadoSinCambios()) {
			cancelar();
			return;
		}
		tablero.celdaEn(evento.getPoint()).filter(gesto::extenderHacia)
				.ifPresent(celda -> gesto.mostrarEn(tablero));
	}

	@Override
	public void mouseReleased(MouseEvent evento) {
		if (!habilitado || gesto == null || evento.getButton() != MouseEvent.BUTTON1) {
			return;
		}
		try {
			confirmar(evento.getPoint());
		} finally {
			cancelar();
		}
	}

	private void iniciarGesto(Point punto) {
		Optional<EstadoJuego> estado = tablero.estadoActual();
		Optional<Celda> celda = tablero.celdaEn(punto);
		if (estado.isEmpty() || celda.isEmpty() || esperandoActualizacion(estado.get())) {
			return;
		}
		gesto = crearGesto(estado.get(), celda.get()).orElse(null);
		if (gesto != null) {
			edicionUsada |= edicion;
			gesto.mostrarEn(tablero);
			tablero.requestFocusInWindow();
		}
	}

	private Optional<GestoMouse> crearGesto(EstadoJuego estado, Celda celda) {
		if (edicion) {
			return RecorteMouse.desde(estado, celda).map(GestoMouse.class::cast);
		}
		if (celda.equals(estado.actual())) {
			return Optional.of(new TrazoMouse(estado));
		}
		return Optional.empty();
	}

	private void confirmar(Point punto) {
		if (tablero.contains(punto) && estadoSinCambios() && gesto.tieneCambios()) {
			ultimoConfirmado = gesto.origen();
			gesto.confirmar(mover, deshacer);
		}
	}

	/** El motor todavía no devolvió el estado posterior al último gesto confirmado. */
	private boolean esperandoActualizacion(EstadoJuego estado) {
		return ultimoConfirmado != null && mismoEstado(estado, ultimoConfirmado);
	}

	private boolean estadoSinCambios() {
		return tablero.estadoActual()
				.filter(actual -> mismoEstado(actual, gesto.origen()))
				.isPresent();
	}

	private static boolean mismoEstado(EstadoJuego uno, EstadoJuego otro) {
		return uno.nivel() == otro.nivel() && uno.recorrido().equals(otro.recorrido());
	}

	private void cancelar() {
		gesto = null;
		tablero.limpiarTrazo();
	}
}
