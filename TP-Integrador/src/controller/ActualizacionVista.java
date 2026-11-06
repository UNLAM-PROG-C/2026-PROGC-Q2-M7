package controller;

import java.util.Objects;
import model.EstadoJuego;

public record ActualizacionVista(EstadoJuego estadoJuego, String mensaje, boolean manchadoPorTomate) {
	public ActualizacionVista {
		Objects.requireNonNull(mensaje, "mensaje");
	}
}
