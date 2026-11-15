package model;

import java.util.Objects;

/** Configuración de una partida: la elige el host y los demás jugadores la reciben. */
public record ConfigPartida(Dificultad dificultad, int rondas, int jugadores) {
	public static final int MIN_RONDAS = 1;
	public static final int MAX_RONDAS = 5;
	public static final int MIN_JUGADORES = 1;
	public static final int MIN_JUGADORES_RED = 2;
	public static final int MAX_JUGADORES = 4;
	private static final String DIFICULTAD_NULA = "La dificultad no puede ser nula.";
	private static final String RONDAS_INVALIDAS = "Rondas fuera de rango: ";
	private static final String JUGADORES_INVALIDOS = "Jugadores fuera de rango: ";

	public ConfigPartida {
		Objects.requireNonNull(dificultad, DIFICULTAD_NULA);
		if (rondas < MIN_RONDAS || rondas > MAX_RONDAS) {
			throw new IllegalArgumentException(RONDAS_INVALIDAS + rondas);
		}
		if (jugadores < MIN_JUGADORES || jugadores > MAX_JUGADORES) {
			throw new IllegalArgumentException(JUGADORES_INVALIDOS + jugadores);
		}
	}

	public boolean esMultijugador() {
		return jugadores > MIN_JUGADORES;
	}
}
