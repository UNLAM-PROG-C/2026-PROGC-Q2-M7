package events;

import java.util.Objects;
import model.Celda;
import model.Nivel;

/**
 * Payload de {@link TipoEvento#IR_A_CELDA}: la celda a la que se quiere llevar el camino y el
 * nivel sobre el que se pidió. El nivel permite descartar pedidos que quedaron en la cola cuando
 * el tablero ya cambió (por ejemplo, al ganar la ronda en medio de un arrastre).
 */
public record DestinoCelda(Nivel nivel, Celda celda) {
	private static final String NIVEL_NULO = "El nivel no puede ser nulo.";
	private static final String CELDA_NULA = "La celda no puede ser nula.";

	public DestinoCelda {
		Objects.requireNonNull(nivel, NIVEL_NULO);
		Objects.requireNonNull(celda, CELDA_NULA);
	}
}
