package zip.dominio;

import java.util.List;
import java.util.Objects;

public final class Nivel {
	public static final int PUNTOS_MINIMOS = 2;
	public static final int TAMANO_MINIMO = 2;

	private static final String NUMEROS_NULOS = "Los números no pueden ser nulos.";
	private static final String NUMERO_NULO = "Un número no puede ser nulo.";
	private static final String CELDA_NULA = "La celda no puede ser nula.";
	private static final String CELDA_EXTERIOR = "La celda está fuera del tablero.";
	private static final String PUNTOS_INVALIDOS = "La cantidad de puntos no es válida para el tablero.";
	private static final String TAMANO_INVALIDO = "El tamaño del tablero no es válido.";
	private static final String NUMERO_INVALIDO = "Los números deben ser únicos, consecutivos o cero.";
	private static final String SECUENCIA_INVALIDA = "Los puntos deben ser consecutivos desde 1.";

	private final int tamano;
	private final int cantidadCeldas;
	private final List<Integer> numeros;
	private final int puntos;

	public Nivel(int tamano, List<Integer> numeros) {
		Objects.requireNonNull(numeros, NUMEROS_NULOS);
		numeros.forEach(numero -> Objects.requireNonNull(numero, NUMERO_NULO));
		this.tamano = validarTamano(tamano);
		this.cantidadCeldas = tamano * tamano;
		this.numeros = List.copyOf(numeros);
		this.puntos = validarNumeros(this.numeros, cantidadCeldas);
	}

	public int numeroEn(Celda celda) {
		if (!contiene(celda)) {
			throw new IllegalArgumentException(CELDA_EXTERIOR);
		}
		return numeros.get(celda.fila() * tamano + celda.columna());
	}

	public boolean contiene(Celda celda) {
		Objects.requireNonNull(celda, CELDA_NULA);
		return celda.fila() >= 0 && celda.fila() < tamano
				&& celda.columna() >= 0 && celda.columna() < tamano;
	}

	public Celda inicio() {
		int indice = numeros.indexOf(1);
		return new Celda(indice / tamano, indice % tamano);
	}

	public int tamano() {
		return tamano;
	}

	public int cantidadCeldas() {
		return cantidadCeldas;
	}

	public int puntos() {
		return puntos;
	}

	public List<Integer> numeros() {
		return numeros;
	}

	private static int validarTamano(int tamano) {
		if (tamano < TAMANO_MINIMO || tamano > Math.sqrt(Integer.MAX_VALUE)) {
			throw new IllegalArgumentException(TAMANO_INVALIDO);
		}
		return tamano;
	}

	private static void validarPuntos(int puntos, int cantidadCeldas) {
		if (puntos < PUNTOS_MINIMOS || puntos > cantidadCeldas) {
			throw new IllegalArgumentException(PUNTOS_INVALIDOS);
		}
	}

	private static int validarNumeros(List<Integer> numeros, int cantidadCeldas) {
		if (numeros.size() != cantidadCeldas) {
			throw new IllegalArgumentException(TAMANO_INVALIDO);
		}
		boolean[] vistos = new boolean[cantidadCeldas + 1];
		int cantidad = contarNumeros(numeros, vistos);
		validarPuntos(cantidad, cantidadCeldas);
		validarSecuencia(vistos, cantidad);
		return cantidad;
	}

	private static int contarNumeros(List<Integer> numeros, boolean[] vistos) {
		int cantidad = 0;
		for (int numero : numeros) {
			if (numero < 0 || numero >= vistos.length || numero > 0 && vistos[numero]) {
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
