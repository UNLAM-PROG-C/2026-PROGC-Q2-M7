package zip.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MotorJuego {
	private static final String NIVEL_NULO = "El nivel no puede ser nulo.";
	private static final String DIRECCION_NULA = "La dirección no puede ser nula.";

	private final Nivel nivel;
	private final List<Celda> recorrido = new ArrayList<>();

	public MotorJuego(Nivel nivel) {
		this.nivel = Objects.requireNonNull(nivel, NIVEL_NULO);
		reiniciar();
	}

	public EstadoJuego estadoJuego() {
		return new EstadoJuego(nivel, recorrido);
	}

	public ResultadoMovimiento mover(Direccion direccion) {
		Objects.requireNonNull(direccion, DIRECCION_NULA);
		if (recorrido.size() == Nivel.CANTIDAD_CELDAS) {
			return ResultadoMovimiento.FINALIZADO;
		}
		Celda siguiente = recorrido.get(recorrido.size() - 1).mover(direccion);
		ResultadoMovimiento resultado = ReglasJuego.evaluar(nivel, recorrido, siguiente);
		if (resultado == ResultadoMovimiento.VALIDO || resultado == ResultadoMovimiento.FINALIZADO) {
			recorrido.add(siguiente);
		}
		return resultado;
	}

	public boolean deshacer() {
		if (recorrido.size() == 1 || recorrido.size() == Nivel.CANTIDAD_CELDAS) {
			return false;
		}
		recorrido.remove(recorrido.size() - 1);
		return true;
	}

	public void reiniciar() {
		recorrido.clear();
		recorrido.add(nivel.inicio());
	}
}
