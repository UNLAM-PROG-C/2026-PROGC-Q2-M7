package controller;

import java.util.List;
import java.util.Objects;
import model.EstadoJuego;

/**
 * Foto inmutable de la partida que el motor le manda a la vista.
 *
 * @param estadoJuego tablero actual; null indica un error sin tablero (ver {@link #error})
 * @param marcador rondas ganadas por cada jugador (uno solo en partida local)
 * @param nombreGanador nombre de quien ganó la partida, o null si no terminó
 */
public record ActualizacionVista(
		EstadoJuego estadoJuego,
		String mensaje,
		boolean manchadoPorTomate,
		List<PuntajeJugador> marcador,
		int rondasObjetivo,
		boolean partidaFinalizada,
		boolean ganoLocal,
		String nombreGanador) {

	public ActualizacionVista {
		Objects.requireNonNull(mensaje, "mensaje");
		marcador = List.copyOf(marcador);
	}

	/** Actualización sin tablero: la vista la toma como error y vuelve al menú o a la sala. */
	public static ActualizacionVista error(String mensaje) {
		return new ActualizacionVista(null, mensaje, false, List.of(), 1, false, false, null);
	}

	public boolean multijugador() {
		return marcador.size() > 1;
	}
}
