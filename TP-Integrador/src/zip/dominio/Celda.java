package zip.dominio;

import java.util.Objects;

public record Celda(int fila, int columna) {
	private static final String DIRECCION_NULA = "La dirección no puede ser nula.";

	public Celda mover(Direccion direccion) {
		Objects.requireNonNull(direccion, DIRECCION_NULA);
		return new Celda(fila + direccion.deltaFila(), columna + direccion.deltaColumna());
	}
}
