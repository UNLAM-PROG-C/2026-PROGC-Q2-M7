package model;

/**
 * Niveles de dificultad. Cada uno fija el tamaño del tablero y la cantidad mínima de números
 * (pistas) que tendrá todo nivel generado: {@link #puntos()}.
 */
public enum Dificultad {
	FACIL(Valores.TAMANO_FACIL, Valores.PUNTOS_FACIL),
	MEDIA(Valores.TAMANO_MEDIO, Valores.PUNTOS_MEDIA),
	DIFICIL(Valores.TAMANO_DIFICIL, Valores.PUNTOS_DIFICIL);

	private static final String TAMANO_INVALIDO = "El tamaño del tablero no es válido.";
	private static final String PUNTOS_INVALIDOS = "Los puntos no son válidos para el tablero.";

	private final int tamano;
	private final int puntos;

	Dificultad(int tamano, int puntos) {
		if (tamano < Nivel.TAMANO_MINIMO) {
			throw new IllegalArgumentException(TAMANO_INVALIDO);
		}
		if (puntos < Nivel.PUNTOS_MINIMOS || puntos > tamano * tamano) {
			throw new IllegalArgumentException(PUNTOS_INVALIDOS);
		}
		this.tamano = tamano;
		this.puntos = puntos;
	}

	public int tamano() {
		return tamano;
	}

	/**
	 * Piso de pistas: todo nivel generado tiene al menos esta cantidad de números. El generador
	 * parte de este piso y agrega las pistas que hagan falta para que la solución sea única.
	 */
	public int puntos() {
		return puntos;
	}

	public int cantidadCeldas() {
		return tamano * tamano;
	}

	private static final class Valores {
		private static final int TAMANO_FACIL = 5;
		private static final int PUNTOS_FACIL = 5;
		private static final int TAMANO_MEDIO = 6;
		private static final int PUNTOS_MEDIA = 6;
		private static final int TAMANO_DIFICIL = 8;
		private static final int PUNTOS_DIFICIL = 8;

		private Valores() {
		}
	}
}
