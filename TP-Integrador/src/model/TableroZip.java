package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TableroZip {
	private static final String NIVEL_NULO = "El nivel no puede ser nulo.";
	private static final String DIRECCION_NULA = "La dirección no puede ser nula.";
	private static final String CELDA_NULA = "La celda no puede ser nula.";

	private final Nivel nivel;
	private final List<Celda> recorrido = new ArrayList<>();
	private int proximoNumero;

	public TableroZip(Nivel nivel) {
		this.nivel = Objects.requireNonNull(nivel, NIVEL_NULO);
		reiniciar();
	}

	public Nivel nivel() {
		return nivel;
	}

	public EstadoJuego estadoJuego() {
		return EstadoJuego.desdeTablero(nivel, recorrido, proximoNumero);
	}

	public ResultadoMovimiento mover(Direccion direccion) {
		Objects.requireNonNull(direccion, DIRECCION_NULA);
		if (recorrido.size() == nivel.cantidadCeldas()) {
			return ResultadoMovimiento.FINALIZADO;
		}
		Celda siguiente = recorrido.get(recorrido.size() - 1).mover(direccion);
		ResultadoMovimiento resultado = ReglasJuego.evaluar(nivel, recorrido, siguiente);
		if (resultado == ResultadoMovimiento.VALIDO || resultado == ResultadoMovimiento.FINALIZADO) {
			recorrido.add(siguiente);
			if (nivel.numeroEn(siguiente) > 0) {
				proximoNumero++;
			}
		}
		return resultado;
	}

	/**
	 * Lleva la punta del camino a una celda: si ya es parte del camino retrocede hasta ella, si es
	 * vecina de la punta avanza (con las reglas de {@link #mover}), y si no, no hace nada.
	 */
	public ResultadoMovimiento irA(Celda destino) {
		Objects.requireNonNull(destino, CELDA_NULA);
		if (recorrido.contains(destino)) {
			return retrocederHasta(destino);
		}
		return Direccion.entre(actual(), destino)
				.map(this::mover)
				.orElse(ResultadoMovimiento.SIN_EFECTO);
	}

	private ResultadoMovimiento retrocederHasta(Celda destino) {
		boolean retrocedio = false;
		while (!actual().equals(destino) && deshacer()) {
			retrocedio = true;
		}
		return retrocedio ? ResultadoMovimiento.RETROCESO : ResultadoMovimiento.SIN_EFECTO;
	}

	private Celda actual() {
		return recorrido.get(recorrido.size() - 1);
	}

	public boolean deshacer() {
		if (recorrido.size() == 1 || recorrido.size() == nivel.cantidadCeldas()) {
			return false;
		}
		Celda removida = recorrido.remove(recorrido.size() - 1);
		if (nivel.numeroEn(removida) > 0) {
			proximoNumero--;
		}
		return true;
	}

	public void reiniciar() {
		recorrido.clear();
		recorrido.add(nivel.inicio());
		proximoNumero = 2;
	}
}
