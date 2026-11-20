package model;

import java.util.List;
import java.util.Objects;

public final class EstadoJuego {
	private static final String NIVEL_NULO = "El nivel no puede ser nulo.";
	private static final String RECORRIDO_NULO = "El recorrido no puede ser nulo.";
	private static final String CELDA_NULA = "Una celda no puede ser nula.";

	private final Nivel nivel;
	private final List<Celda> recorrido;
	private final int proximoNumero;

	public EstadoJuego(Nivel nivel, List<Celda> recorrido) {
		Objects.requireNonNull(nivel, NIVEL_NULO);
		Objects.requireNonNull(recorrido, RECORRIDO_NULO);
		recorrido.forEach(celda -> Objects.requireNonNull(celda, CELDA_NULA));
		List<Celda> copia = List.copyOf(recorrido);
		ReglasJuego.validarRecorrido(nivel, copia);
		this.nivel = nivel;
		this.recorrido = copia;
		this.proximoNumero = ReglasJuego.proximoNumero(nivel, copia);
	}

	private EstadoJuego(Nivel nivel, List<Celda> recorrido, int proximoNumero) {
		this.nivel = nivel;
		this.recorrido = recorrido;
		this.proximoNumero = proximoNumero;
	}

	static EstadoJuego desdeTablero(Nivel nivel, List<Celda> recorrido, int proximoNumero) {
		Objects.requireNonNull(nivel, NIVEL_NULO);
		Objects.requireNonNull(recorrido, RECORRIDO_NULO);
		return new EstadoJuego(nivel, List.copyOf(recorrido), proximoNumero);
	}

	public Nivel nivel() {
		return nivel;
	}

	public List<Celda> recorrido() {
		return recorrido;
	}

	public Celda actual() {
		return recorrido.get(recorrido.size() - 1);
	}

	public int proximoNumero() {
		return proximoNumero;
	}

	public boolean gano() {
		return visitadas() == nivel.cantidadCeldas() && proximoNumero == nivel.puntos() + 1;
	}

	public int visitadas() {
		return recorrido.size();
	}

	@Override
	public boolean equals(Object otro) {
		if (this == otro) {
			return true;
		}
		if (!(otro instanceof EstadoJuego estado)) {
			return false;
		}
		return Objects.equals(nivel, estado.nivel) && Objects.equals(recorrido, estado.recorrido);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nivel, recorrido);
	}

	@Override
	public String toString() {
		return "EstadoJuego[nivel=" + nivel + ", recorrido=" + recorrido + "]";
	}
}
