package zip.dominio;

public enum Dificultad {
	FACIL(Valores.TAMANO_FACIL, Valores.PUNTOS_FACIL),
	MEDIA(Valores.TAMANO_MEDIO, Valores.PUNTOS_MEDIA),
	DIFICIL(Valores.TAMANO_DIFICIL, Valores.PUNTOS_DIFICIL);

	private final int tamano;
	private final int puntos;

	Dificultad(int tamano, int puntos) {
		this.tamano = tamano;
		this.puntos = puntos;
	}

	public int tamano() {
		return tamano;
	}

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
