package controller;

/** Rondas ganadas por un jugador en la partida en curso. */
public record PuntajeJugador(int id, String nombre, int rondas, boolean esLocal,
		boolean conectado) {
}
