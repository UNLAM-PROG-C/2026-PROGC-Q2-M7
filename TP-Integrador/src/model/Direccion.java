package model;

import java.util.Optional;

public enum Direccion {
	ARRIBA(-1, 0),
	DERECHA(0, 1),
	ABAJO(1, 0),
	IZQUIERDA(0, -1);

	private final int deltaFila;
	private final int deltaColumna;

	Direccion(int deltaFila, int deltaColumna) {
		this.deltaFila = deltaFila;
		this.deltaColumna = deltaColumna;
	}

	public int deltaFila() {
		return deltaFila;
	}

	public int deltaColumna() {
		return deltaColumna;
	}

	/** Dirección que lleva de una celda a su vecina, o vacío si no son vecinas. */
	public static Optional<Direccion> entre(Celda desde, Celda hasta) {
		for (Direccion direccion : values()) {
			if (desde.mover(direccion).equals(hasta)) {
				return Optional.of(direccion);
			}
		}
		return Optional.empty();
	}
}
