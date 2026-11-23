package model;

import java.util.List;
import java.util.Objects;

public record EstadoJuego(Nivel nivel, List<Celda> recorrido, int proximoNumero) {
	public EstadoJuego {
		Objects.requireNonNull(nivel, "El nivel no puede ser nulo.");
		Objects.requireNonNull(recorrido, "El recorrido no puede ser nulo.");
		recorrido = List.copyOf(recorrido);
	}

	public Celda actual() {
		return recorrido.get(recorrido.size() - 1);
	}

	public boolean gano() {
		return visitadas() == nivel.cantidadCeldas() && proximoNumero == nivel.puntos() + 1;
	}

	public int visitadas() {
		return recorrido.size();
	}
}
