package ui;

import model.Dificultad;

/** Textos compartidos para describir la configuración de una partida. */
final class Resumen {
	private static final String UNA_RONDA = " ronda";
	private static final String VARIAS_RONDAS = " rondas";
	private static final String SEPARADOR = " · ";
	private static final String MULTIPLICADOR = "×";

	private Resumen() {
	}

	static String dificultad(Dificultad dificultad) {
		return switch (dificultad) {
			case FACIL -> "Fácil";
			case MEDIA -> "Media";
			case DIFICIL -> "Difícil";
		};
	}

	static String rondas(int rondas) {
		return rondas + (rondas == 1 ? UNA_RONDA : VARIAS_RONDAS);
	}

	static String configuracion(Dificultad dificultad, int rondas) {
		return dificultad(dificultad) + " " + dificultad.tamano() + MULTIPLICADOR
				+ dificultad.tamano() + SEPARADOR + "A " + rondas(rondas);
	}
}
