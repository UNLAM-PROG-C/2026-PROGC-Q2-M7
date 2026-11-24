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
		boolean ganoLocal,
		boolean multijugador) {

	public ActualizacionVista {
		Objects.requireNonNull(mensaje, "mensaje");
	}
}
