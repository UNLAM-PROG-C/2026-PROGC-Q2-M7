package zip.dominio;

import java.util.List;
import java.util.Objects;

public final class Nivel {
	public static final int TAMANO = 6;
	public static final int CANTIDAD_CELDAS = 36;
	public static final int PUNTOS_MINIMOS = 2;
	public static final int PUNTOS_MAXIMOS = 36;

	private static final String NUMEROS_NULOS = "Los números no pueden ser nulos.";
	private static final String NUMERO_NULO = "Un número no puede ser nulo.";
	private static final String CELDA_NULA = "La celda no puede ser nula.";
	private static final String CELDA_EXTERIOR = "La celda está fuera del tablero.";
	private static final String PUNTOS_INVALIDOS = "La cantidad de puntos debe estar entre 2 y 36.";
	private static final String TAMANO_INVALIDO = "El tablero debe contener exactamente 36 celdas.";
	private static final String NUMERO_INVALIDO = "Los números deben ser únicos y estar entre 1 y 36, o ser 0.";
	private static final String SECUENCIA_INVALIDA = "Los puntos deben ser consecutivos desde 1.";

	private final List<Integer> numeros;
	private final int puntos;

	public Nivel(List<Integer> numeros) {
		Objects.requireNonNull(numeros, NUMEROS_NULOS);
		numeros.forEach(numero -> Objects.requireNonNull(numero, NUMERO_NULO));
		this.numeros = List.copyOf(numeros);
		this.puntos = validarNumeros(this.numeros);
	}

	public int numeroEn(Celda celda) {
		if (!contiene(celda)) {
			throw new IllegalArgumentException(CELDA_EXTERIOR);
		}
		return numeros.get(celda.fila() * TAMANO + celda.columna());
	}

	public boolean contiene(Celda celda) {
		Objects.requireNonNull(celda, CELDA_NULA);
		return celda.fila() >= 0 && celda.fila() < TAMANO && celda.columna() >= 0 && celda.columna() < TAMANO;
	}

	public Celda inicio() {
		int indice = numeros.indexOf(1);
		return new Celda(indice / TAMANO, indice % TAMANO);
	}

	public int puntos() {
		return puntos;
	}

	public List<Integer> numeros() {
		return numeros;
	}

	static void validarPuntos(int puntos) {
		if (puntos < PUNTOS_MINIMOS || puntos > PUNTOS_MAXIMOS) {
			throw new IllegalArgumentException(PUNTOS_INVALIDOS);
		}
	}

	private static int validarNumeros(List<Integer> numeros) {
		if (numeros.size() != CANTIDAD_CELDAS) {
			throw new IllegalArgumentException(TAMANO_INVALIDO);
		}
		boolean[] vistos = new boolean[CANTIDAD_CELDAS + 1];
		int cantidad = contarNumeros(numeros, vistos);
		validarPuntos(cantidad);
		validarSecuencia(vistos, cantidad);
		return cantidad;
	}

	private static int contarNumeros(List<Integer> numeros, boolean[] vistos) {
		int cantidad = 0;
		for (int numero : numeros) {
			if (numero < 0 || numero > PUNTOS_MAXIMOS || numero > 0 && vistos[numero]) {
				throw new IllegalArgumentException(NUMERO_INVALIDO);
			}
			if (numero > 0) {
				vistos[numero] = true;
				cantidad++;
			}
		}
		return cantidad;
	}

	private static void validarSecuencia(boolean[] vistos, int cantidad) {
		for (int numero = 1; numero <= cantidad; numero++) {
			if (!vistos[numero]) {
				throw new IllegalArgumentException(SECUENCIA_INVALIDA);
			}
		}
	}
}
