package zip.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class GeneradorNivel {
	private static final int CANTIDAD_PLANTILLAS = 2;
	private static final int CANTIDAD_ROTACIONES = 4;
	private static final String TRANSFORMACION_INVALIDA = "La plantilla o la rotación no es válida.";
	private final Random aleatorio;

	public GeneradorNivel() {
		aleatorio = new Random();
	}

	public GeneradorNivel(long semilla) {
		aleatorio = new Random(semilla);
	}

	public Nivel generar(Dificultad dificultad) {
		Objects.requireNonNull(dificultad, "dificultad");
		int tamano = dificultad.tamano();
		List<Celda> recorrido = recorrido(tamano, aleatorio.nextInt(CANTIDAD_PLANTILLAS),
				aleatorio.nextInt(CANTIDAD_ROTACIONES), aleatorio.nextBoolean(),
				aleatorio.nextBoolean());
		List<Integer> puntosNumerados = posicionesPuntos(dificultad.puntos(), dificultad.cantidadCeldas());
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(dificultad.cantidadCeldas(), 0));
		for (int indice = 0; indice < puntosNumerados.size(); indice++) {
			Celda celda = recorrido.get(puntosNumerados.get(indice));
			numeros.set(celda.fila() * tamano + celda.columna(), indice + 1);
		}
		return new Nivel(tamano, numeros);
	}

	static List<Celda> recorrido(int tamano, int plantilla, int rotaciones,
			boolean reflejado, boolean invertido) {
		validarTransformacion(tamano, plantilla, rotaciones);
		List<Celda> base = plantilla == 0 ? serpiente(tamano) : espiral(tamano);
		List<Celda> recorrido = new ArrayList<>();
		for (Celda celda : base) {
			recorrido.add(transformar(celda, tamano, rotaciones, reflejado));
		}
		if (invertido) {
			Collections.reverse(recorrido);
		}
		return List.copyOf(recorrido);
	}

	private List<Integer> posicionesPuntos(int puntos, int cantidadCeldas) {
		int ultimoIndice = cantidadCeldas - 1;
		List<Integer> interior = new ArrayList<>();
		for (int indice = 1; indice < ultimoIndice; indice++) {
			interior.add(indice);
		}
		Collections.shuffle(interior, aleatorio);
		List<Integer> seleccionada = new ArrayList<>(interior.subList(0, puntos - 2));
		seleccionada.add(0);
		seleccionada.add(ultimoIndice);
		Collections.sort(seleccionada);
		return seleccionada;
	}

	private static List<Celda> serpiente(int tamano) {
		List<Celda> recorrido = new ArrayList<>();
		for (int fila = 0; fila < tamano; fila++) {
			for (int indiceColumna = 0; indiceColumna < tamano; indiceColumna++) {
				int columna = fila % 2 == 0 ? indiceColumna : tamano - 1 - indiceColumna;
				recorrido.add(new Celda(fila, columna));
			}
		}
		return recorrido;
	}

	private static List<Celda> espiral(int tamano) {
		List<Celda> recorrido = new ArrayList<>();
		Celda actual = new Celda(0, 0);
		Direccion direccion = Direccion.DERECHA;
		for (int indice = 0; indice < tamano * tamano; indice++) {
			recorrido.add(actual);
			Celda siguiente = actual.mover(direccion);
			if (!interior(siguiente, tamano) || recorrido.contains(siguiente)) {
				direccion = Direccion.values()[(direccion.ordinal() + 1) % CANTIDAD_ROTACIONES];
			}
			actual = actual.mover(direccion);
		}
		return recorrido;
	}

	private static Celda transformar(Celda celda, int tamano, int rotaciones, boolean reflejado) {
		Celda resultado = reflejado ? new Celda(celda.fila(), tamano - 1 - celda.columna()) : celda;
		for (int rotacion = 0; rotacion < rotaciones; rotacion++) {
			resultado = new Celda(resultado.columna(), tamano - 1 - resultado.fila());
		}
		return resultado;
	}

	private static boolean interior(Celda celda, int tamano) {
		return celda.fila() >= 0 && celda.fila() < tamano
				&& celda.columna() >= 0 && celda.columna() < tamano;
	}

	private static void validarTransformacion(int tamano, int plantilla, int rotaciones) {
		if (tamano < Nivel.TAMANO_MINIMO || plantilla < 0 || plantilla >= CANTIDAD_PLANTILLAS
				|| rotaciones < 0 || rotaciones >= CANTIDAD_ROTACIONES) {
			throw new IllegalArgumentException(TRANSFORMACION_INVALIDA);
		}
	}
}
