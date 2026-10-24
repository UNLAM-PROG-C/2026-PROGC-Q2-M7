package zip.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class GeneradorNivel {
	private static final int CANTIDAD_PLANTILLAS = 2;
	private static final int CANTIDAD_ROTACIONES = 4;
	private static final int ULTIMO_INDICE = Nivel.CANTIDAD_CELDAS - 1;
	private static final String TRANSFORMACION_INVALIDA = "La plantilla o la rotación no es válida.";
	private final Random aleatorio;

	public GeneradorNivel() {
		aleatorio = new Random();
	}

	public GeneradorNivel(long semilla) {
		aleatorio = new Random(semilla);
	}

	public Nivel generar(int puntos) {
		Nivel.validarPuntos(puntos);
		List<Celda> recorrido = recorrido(aleatorio.nextInt(CANTIDAD_PLANTILLAS),
				aleatorio.nextInt(CANTIDAD_ROTACIONES), aleatorio.nextBoolean(), aleatorio.nextBoolean());
		List<Integer> puntosNumerados = posicionesPuntos(puntos);
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(Nivel.CANTIDAD_CELDAS, 0));
		for (int indice = 0; indice < puntosNumerados.size(); indice++) {
			Celda celda = recorrido.get(puntosNumerados.get(indice));
			numeros.set(celda.fila() * Nivel.TAMANO + celda.columna(), indice + 1);
		}
		return new Nivel(numeros);
	}

	static List<Celda> recorrido(int plantilla, int rotaciones, boolean reflejado, boolean invertido) {
		validarTransformacion(plantilla, rotaciones);
		List<Celda> base = plantilla == 0 ? serpiente() : espiral();
		List<Celda> recorrido = new ArrayList<>();
		for (Celda celda : base) {
			recorrido.add(transformar(celda, rotaciones, reflejado));
		}
		if (invertido) {
			Collections.reverse(recorrido);
		}
		return List.copyOf(recorrido);
	}

	private List<Integer> posicionesPuntos(int puntos) {
		List<Integer> interiores = new ArrayList<>();
		for (int indice = 1; indice < ULTIMO_INDICE; indice++) {
			interiores.add(indice);
		}
		Collections.shuffle(interiores, aleatorio);
		List<Integer> seleccionados = new ArrayList<>(interiores.subList(0, puntos - 2));
		seleccionados.add(0);
		seleccionados.add(ULTIMO_INDICE);
		Collections.sort(seleccionados);
		return seleccionados;
	}

	private static List<Celda> serpiente() {
		List<Celda> recorrido = new ArrayList<>();
		for (int fila = 0; fila < Nivel.TAMANO; fila++) {
			for (int indiceColumna = 0; indiceColumna < Nivel.TAMANO; indiceColumna++) {
				int columna = fila % 2 == 0 ? indiceColumna : Nivel.TAMANO - 1 - indiceColumna;
				recorrido.add(new Celda(fila, columna));
			}
		}
		return recorrido;
	}

	private static List<Celda> espiral() {
		List<Celda> recorrido = new ArrayList<>();
		Celda actual = new Celda(0, 0);
		Direccion direccion = Direccion.DERECHA;
		for (int indice = 0; indice < Nivel.CANTIDAD_CELDAS; indice++) {
			recorrido.add(actual);
			Celda siguiente = actual.mover(direccion);
			if (!interior(siguiente) || recorrido.contains(siguiente)) {
				direccion = Direccion.values()[(direccion.ordinal() + 1) % CANTIDAD_ROTACIONES];
			}
			actual = actual.mover(direccion);
		}
		return recorrido;
	}

	private static Celda transformar(Celda celda, int rotaciones, boolean reflejado) {
		Celda resultado = reflejado ? new Celda(celda.fila(), Nivel.TAMANO - 1 - celda.columna()) : celda;
		for (int rotacion = 0; rotacion < rotaciones; rotacion++) {
			resultado = new Celda(resultado.columna(), Nivel.TAMANO - 1 - resultado.fila());
		}
		return resultado;
	}

	private static boolean interior(Celda celda) {
		return celda.fila() >= 0 && celda.fila() < Nivel.TAMANO
				&& celda.columna() >= 0 && celda.columna() < Nivel.TAMANO;
	}

	private static void validarTransformacion(int plantilla, int rotaciones) {
		if (plantilla < 0 || plantilla >= CANTIDAD_PLANTILLAS
				|| rotaciones < 0 || rotaciones >= CANTIDAD_ROTACIONES) {
			throw new IllegalArgumentException(TRANSFORMACION_INVALIDA);
		}
	}
}
