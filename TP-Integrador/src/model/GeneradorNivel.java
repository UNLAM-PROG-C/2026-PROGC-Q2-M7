package model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

public final class GeneradorNivel {
	public static final long TOPE_NODOS = 20_000L;

	private static final int MOVIMIENTOS_POR_CELDA = 10;
	private static final int LIMITE_UNICA = 2;
	private static final int UNA_SOLUCION = 1;
	private static final String DIFICULTAD_NULA = "La dificultad no puede ser nula.";

	private final Random aleatorio;

	public GeneradorNivel() {
		this.aleatorio = new Random();
	}

	public GeneradorNivel(long semilla) {
		this.aleatorio = new Random(semilla);
	}

	public Nivel generar(Dificultad dificultad) {
		return generar(dificultad, aleatorio.nextLong());
	}

	public Nivel generar(Dificultad dificultad, long semilla) {
		Objects.requireNonNull(dificultad, DIFICULTAD_NULA);
		Obra obra = new Obra(dificultad.tamano(), new Random(semilla));
		obra.sumarPistasHasta(dificultad.puntos());
		obra.completarHastaUnica();
		obra.minimizar(dificultad.puntos());
		return new Nivel(dificultad.tamano(), Arrays.stream(obra.numeros()).boxed().toList());
	}

	private static int[] serpiente(int tamano) {
		int[] camino = new int[tamano * tamano];
		for (int paso = 0; paso < camino.length; paso++) {
			int fila = paso / tamano;
			int offset = paso % tamano;
			camino[paso] = fila * tamano + (fila % 2 == 0 ? offset : tamano - 1 - offset);
		}
		return camino;
	}

	private static int[] caminoBackbite(int tamano, Random azar) {
		int[][] vecinos = SolucionadorZip.vecinosDe(tamano);
		int[] camino = serpiente(tamano);
		int[] posicion = new int[camino.length];
		for (int paso = 0; paso < camino.length; paso++) {
			posicion[camino[paso]] = paso;
		}
		for (int i = 0; i < MOVIMIENTOS_POR_CELDA * camino.length; i++) {
			moverBackbite(camino, posicion, vecinos, azar);
		}
		return camino;
	}

	private static void moverBackbite(int[] camino, int[] posicion, int[][] vecinos, Random azar) {
		boolean desdeInicio = azar.nextBoolean();
		int extremo = camino[desdeInicio ? 0 : camino.length - 1];
		int[] opciones = vecinos[extremo];
		int vecino = posicion[opciones[azar.nextInt(opciones.length)]];
		if (desdeInicio) {
			invertir(camino, posicion, 0, vecino - 1);
		} else {
			invertir(camino, posicion, vecino + 1, camino.length - 1);
		}
	}

	private static void invertir(int[] camino, int[] posicion, int desde, int hasta) {
		for (int a = desde, b = hasta; a < b; a++, b--) {
			int celda = camino[a];
			camino[a] = camino[b];
			camino[b] = celda;
			posicion[camino[a]] = a;
			posicion[camino[b]] = b;
		}
	}

	private static final class Obra {
		private final int[] camino;
		private final Random azar;
		private final SolucionadorZip solucionador;
		private final TreeSet<Integer> pasos = new TreeSet<>();

		Obra(int tamano, Random azar) {
			this.azar = azar;
			this.camino = caminoBackbite(tamano, azar);
			this.solucionador = new SolucionadorZip(tamano, TOPE_NODOS);
			pasos.add(0);
			pasos.add(camino.length - 1);
		}

		int[] numeros() {
			int[] numeros = new int[camino.length];
			int numero = 1;
			for (int paso : pasos) {
				numeros[camino[paso]] = numero++;
			}
			return numeros;
		}

		void sumarPistasHasta(int cantidad) {
			while (pasos.size() < cantidad) {
				pasos.add(elegir(sinPista(null)));
			}
		}

		void completarHastaUnica() {
			while (!Thread.currentThread().isInterrupted() && !esUnica()) {
				Optional<int[]> alternativa = solucionador.alternativa();
				List<Integer> candidatos = sinPista(alternativa.orElse(null));
				pasos.add(elegir(candidatos.isEmpty() ? sinPista(null) : candidatos));
			}
		}

		void minimizar(int piso) {
			List<Integer> orden = new ArrayList<>(pasos.subSet(1, camino.length - 1));
			Collections.shuffle(orden, azar);
			for (int paso : orden) {
				if (Thread.currentThread().isInterrupted()) {
					return;
				}
				intentarQuitar(paso, piso);
			}
		}

		private void intentarQuitar(int paso, int piso) {
			if (pasos.size() <= piso) {
				return;
			}
			pasos.remove(paso);
			if (!esUnica()) {
				pasos.add(paso);
			}
		}

		private boolean esUnica() {
			return solucionador.contar(numeros(), LIMITE_UNICA, camino) == UNA_SOLUCION;
		}

		private List<Integer> sinPista(int[] alternativa) {
			List<Integer> candidatos = new ArrayList<>();
			for (int paso = 0; paso < camino.length; paso++) {
				boolean diverge = alternativa == null || alternativa[paso] != camino[paso];
				if (diverge && !pasos.contains(paso)) {
					candidatos.add(paso);
				}
			}
			return candidatos;
		}

		private int elegir(List<Integer> candidatos) {
			return candidatos.get(azar.nextInt(candidatos.size()));
		}
	}
}
