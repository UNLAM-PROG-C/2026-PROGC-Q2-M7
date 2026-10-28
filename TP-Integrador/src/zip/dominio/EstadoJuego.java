package zip.dominio;

import java.util.List;
import java.util.Objects;

public record EstadoJuego(Nivel nivel, List<Celda> recorrido) {
	private static final String NIVEL_NULO = "El nivel no puede ser nulo.";
	private static final String RECORRIDO_NULO = "El recorrido no puede ser nulo.";
	private static final String CELDA_NULA = "Una celda no puede ser nula.";

	public EstadoJuego {
		Objects.requireNonNull(nivel, NIVEL_NULO);
		Objects.requireNonNull(recorrido, RECORRIDO_NULO);
		recorrido.forEach(celda -> Objects.requireNonNull(celda, CELDA_NULA));
		recorrido = List.copyOf(recorrido);
		ReglasJuego.validarRecorrido(nivel, recorrido);
	}

	public Celda actual() {
		return recorrido.get(recorrido.size() - 1);
	}

	public int proximoNumero() {
		return ReglasJuego.proximoNumero(nivel, recorrido);
	}

	public boolean gano() {
		return visitadas() == nivel.cantidadCeldas() && proximoNumero() == nivel.puntos() + 1;
	}

	public int visitadas() {
		return recorrido.size();
	}
}
