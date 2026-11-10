package zip.dominio;

import java.util.ArrayList;
import java.util.List;

final class ReglasJuego {
	private static final String INICIO_INVALIDO = "El recorrido debe comenzar en el número 1.";
	private static final String PASO_INVALIDO = "El recorrido contiene un movimiento inválido.";

	private ReglasJuego() {
	}

	static int proximoNumero(Nivel nivel, List<Celda> recorrido) {
		int siguiente = 1;
		for (Celda celda : recorrido) {
			if (nivel.numeroEn(celda) > 0) {
				siguiente++;
			}
		}
		return siguiente;
	}

	static ResultadoMovimiento evaluar(Nivel nivel, List<Celda> recorrido, Celda destino) {
		if (!nivel.contiene(destino)) {
			return ResultadoMovimiento.FUERA_DEL_TABLERO;
		}
		if (recorrido.contains(destino)) {
			return ResultadoMovimiento.CELDA_REPETIDA;
		}
		return resultadoNumerado(nivel, recorrido, destino);
	}

	static void validarRecorrido(Nivel nivel, List<Celda> recorrido) {
		if (recorrido.isEmpty() || !recorrido.get(0).equals(nivel.inicio())) {
			throw new IllegalArgumentException(INICIO_INVALIDO);
		}
		List<Celda> prefijo = new ArrayList<>();
		prefijo.add(nivel.inicio());
		for (int indice = 1; indice < recorrido.size(); indice++) {
			Celda siguiente = recorrido.get(indice);
			validarPaso(nivel, prefijo, siguiente);
			prefijo.add(siguiente);
		}
	}

	private static ResultadoMovimiento resultadoNumerado(Nivel nivel, List<Celda> recorrido, Celda destino) {
		int numero = nivel.numeroEn(destino);
		if (numero > 0 && numero != proximoNumero(nivel, recorrido)) {
			return ResultadoMovimiento.FUERA_DE_ORDEN;
		}
		if (numero == nivel.puntos()) {
			return recorrido.size() == Nivel.CANTIDAD_CELDAS - 1
					? ResultadoMovimiento.FINALIZADO : ResultadoMovimiento.FINAL_ANTICIPADO;
		}
		return ResultadoMovimiento.VALIDO;
	}

	private static void validarPaso(Nivel nivel, List<Celda> prefijo, Celda siguiente) {
		Celda anterior = prefijo.get(prefijo.size() - 1);
		int distancia = Math.abs(anterior.fila() - siguiente.fila())
				+ Math.abs(anterior.columna() - siguiente.columna());
		ResultadoMovimiento resultado = evaluar(nivel, prefijo, siguiente);
		if (distancia != 1
				|| resultado != ResultadoMovimiento.VALIDO && resultado != ResultadoMovimiento.FINALIZADO) {
			throw new IllegalArgumentException(PASO_INVALIDO);
		}
	}
}
