package model;

import java.util.Arrays;
import java.util.Optional;

public final class SolucionadorZip {
	public static final int ABORTADO = -1;

	private static final int SIN_CELDA = -1;
	private static final int PRIMER_NUMERO = 1;
	private static final int SEGUNDO_NUMERO = 2;
	private static final int SIN_SALIDA = 0;
	private static final int UNICA_SALIDA = 1;

	private final int total;
	private final long topeNodos;
	private final int[][] vecinos;
	private final boolean[] visitada;
	private final int[] camino;
	private final int[] pendientes;
	private final int[] marca;
	private int sello;
	private int[] pistas;
	private int[] referencia;
	private int[] alternativa;
	private int celdaFinal;
	private int limite;
	private int encontradas;
	private long nodos;
	private boolean abortado;

	public SolucionadorZip(int tamano, long topeNodos) {
		this.total = tamano * tamano;
		this.topeNodos = topeNodos;
		this.vecinos = vecinosDe(tamano);
		this.visitada = new boolean[total];
		this.camino = new int[total];
		this.pendientes = new int[total];
		this.marca = new int[total];
	}

	static int[][] vecinosDe(int tamano) {
		int[][] vecinos = new int[tamano * tamano][];
		for (int celda = 0; celda < vecinos.length; celda++) {
			vecinos[celda] = vecinosDeCelda(tamano, celda);
		}
		return vecinos;
	}

	public int contar(int[] pistas, int limite, int[] referencia) {
		reiniciar(pistas, limite, referencia);
		int inicio = celdaDe(PRIMER_NUMERO);
		visitada[inicio] = true;
		camino[0] = inicio;
		buscar(inicio, 1, SEGUNDO_NUMERO);
		visitada[inicio] = false;
		return abortado ? ABORTADO : encontradas;
	}

	public Optional<int[]> alternativa() {
		return Optional.ofNullable(alternativa).map(int[]::clone);
	}

	private static int[] vecinosDeCelda(int tamano, int celda) {
		int[] lista = new int[Direccion.values().length];
		int cantidad = 0;
		for (Direccion direccion : Direccion.values()) {
			int fila = celda / tamano + direccion.deltaFila();
			int columna = celda % tamano + direccion.deltaColumna();
			if (fila >= 0 && fila < tamano && columna >= 0 && columna < tamano) {
				lista[cantidad++] = fila * tamano + columna;
			}
		}
		return Arrays.copyOf(lista, cantidad);
	}

	private void reiniciar(int[] pistas, int limite, int[] referencia) {
		this.pistas = pistas;
		this.limite = limite;
		this.referencia = referencia;
		this.alternativa = null;
		this.encontradas = 0;
		this.nodos = 0;
		this.abortado = false;
		Arrays.fill(visitada, false);
		this.celdaFinal = celdaDe(Arrays.stream(pistas).max().orElse(PRIMER_NUMERO));
	}

	private int celdaDe(int numero) {
		for (int celda = 0; celda < total; celda++) {
			if (pistas[celda] == numero) {
				return celda;
			}
		}
		return SIN_CELDA;
	}

	private void buscar(int actual, int largo, int siguiente) {
		if (encontradas >= limite || abortado) {
			return;
		}
		if (++nodos > topeNodos) {
			abortado = true;
		} else if (largo == total) {
			registrar();
		} else if (viable(actual, largo)) {
			for (int vecino : vecinos[actual]) {
				explorar(vecino, largo, siguiente);
			}
		}
	}

	private void explorar(int vecino, int largo, int siguiente) {
		if (visitada[vecino] || !admite(vecino, largo, siguiente)) {
			return;
		}
		visitada[vecino] = true;
		camino[largo] = vecino;
		int numero = pistas[vecino];
		buscar(vecino, largo + 1, numero > 0 ? siguiente + 1 : siguiente);
		visitada[vecino] = false;
	}

	private boolean admite(int vecino, int largo, int siguiente) {
		int numero = pistas[vecino];
		boolean numeroEnOrden = numero == 0 || numero == siguiente;
		return numeroEnOrden && (vecino != celdaFinal || largo + 1 == total);
	}

	private void registrar() {
		encontradas++;
		if (alternativa == null && referencia != null && !Arrays.equals(camino, referencia)) {
			alternativa = camino.clone();
		}
	}

	private boolean viable(int actual, int largo) {
		int raiz = SIN_CELDA;
		for (int celda = 0; celda < total; celda++) {
			if (visitada[celda]) {
				continue;
			}
			if (sinSalida(celda, actual)) {
				return false;
			}
			raiz = raiz == SIN_CELDA ? celda : raiz;
		}
		return libresConexas(raiz, total - largo);
	}

	private boolean sinSalida(int celda, int actual) {
		int salidas = 0;
		for (int vecino : vecinos[celda]) {
			if (!visitada[vecino] || vecino == actual) {
				salidas++;
			}
		}
		return salidas == SIN_SALIDA || (salidas == UNICA_SALIDA && celda != celdaFinal);
	}

	private boolean libresConexas(int raiz, int libres) {
		sello++;
		int inicio = 0;
		int fin = 0;
		pendientes[fin++] = raiz;
		marca[raiz] = sello;
		while (inicio < fin) {
			fin = encolarLibres(pendientes[inicio++], fin);
		}
		return fin == libres;
	}

	private int encolarLibres(int celda, int fin) {
		for (int vecino : vecinos[celda]) {
			if (!visitada[vecino] && marca[vecino] != sello) {
				marca[vecino] = sello;
				pendientes[fin++] = vecino;
			}
		}
		return fin;
	}
}
