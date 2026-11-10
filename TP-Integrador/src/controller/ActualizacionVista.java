package controller;

import java.util.Objects;
import model.EstadoJuego;

public record ActualizacionVista(
		EstadoJuego estadoJuego,
		String mensaje,
		boolean manchadoPorTomate,
		int rondasLocal,
		int rondasRival,
		int rondasObjetivo,
		boolean partidaFinalizada,
		boolean ganoLocal) {

	public ActualizacionVista {
		Objects.requireNonNull(mensaje, "mensaje");
	}

	public ActualizacionVista(EstadoJuego estadoJuego, String mensaje, boolean manchadoPorTomate) {
		this(estadoJuego, mensaje, manchadoPorTomate, 0, 0, 1, false, false);
	}
}
