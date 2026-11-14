package ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import model.Celda;
import model.Direccion;
import model.EstadoJuego;
import model.Nivel;

/**
 * Copia local del camino que el mouse va moviendo.
 *
 * <p>Cada paso hacia adelante publica un movimiento y cada paso hacia atrás publica un
 * deshacer, en el momento. Se lleva una copia propia porque el estado que devuelve el motor
 * llega con atraso: el historial permite reconocer esos estados atrasados y distinguirlos de un
 * cambio externo.
 */
final class TrazoMouse {
	private static final String ESTADO_NULO = "El estado no puede ser nulo.";
	private static final String DIRECCION_INVALIDA = "Las celdas consecutivas no son adyacentes.";

	private final Nivel nivel;
	private final List<Celda> recorrido;
	private final Set<List<Celda>> historial = new HashSet<>();
	private final Consumer<Direccion> mover;
	private final Runnable deshacer;

	TrazoMouse(EstadoJuego estado, Consumer<Direccion> mover, Runnable deshacer) {
		Objects.requireNonNull(estado, ESTADO_NULO);
		this.nivel = estado.nivel();
		this.recorrido = new ArrayList<>(estado.recorrido());
		this.mover = mover;
		this.deshacer = deshacer;
		registrar();
	}

	/** True si el estado es este camino o uno por el que ya pasó y el motor todavía no alcanzó. */
	boolean conoce(EstadoJuego estado) {
		return estado.nivel() == nivel && historial.contains(estado.recorrido());
	}

	boolean contiene(Celda celda) {
		return recorrido.contains(celda);
	}

	List<Celda> recorrido() {
		return List.copyOf(recorrido);
	}

	/** Retrocede si el destino ya es parte del camino; si no, avanza en línea recta hacia él. */
	void llevarHacia(Celda destino) {
		int indice = recorrido.indexOf(destino);
		if (indice >= 0) {
			retrocederHasta(indice);
		} else {
			avanzarHacia(destino);
		}
	}

	private void retrocederHasta(int indice) {
		while (recorrido.size() - 1 > indice && puedeDeshacer()) {
			recorrido.remove(recorrido.size() - 1);
			registrar();
			deshacer.run();
		}
	}

	/** Mismas condiciones que el modelo: no se deshace el inicio ni un tablero completo. */
	private boolean puedeDeshacer() {
		return recorrido.size() > 1 && recorrido.size() < nivel.cantidadCeldas();
	}

	private void avanzarHacia(Celda destino) {
		if (!mismaLinea(punta(), destino)) {
			return;
		}
		while (!punta().equals(destino)) {
			Celda siguiente = pasoHacia(punta(), destino);
			if (!esPasoValido(siguiente)) {
				return;
			}
			Direccion direccion = direccionEntre(punta(), siguiente);
			recorrido.add(siguiente);
			registrar();
			mover.accept(direccion);
		}
	}

	private Celda punta() {
		return recorrido.get(recorrido.size() - 1);
	}

	private void registrar() {
		historial.add(List.copyOf(recorrido));
	}

	private boolean esPasoValido(Celda siguiente) {
		List<Celda> candidato = new ArrayList<>(recorrido);
		candidato.add(siguiente);
		try {
			new EstadoJuego(nivel, candidato);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static boolean mismaLinea(Celda una, Celda otra) {
		return una.fila() == otra.fila() || una.columna() == otra.columna();
	}

	private static Celda pasoHacia(Celda desde, Celda hasta) {
		return new Celda(
				desde.fila() + Integer.signum(hasta.fila() - desde.fila()),
				desde.columna() + Integer.signum(hasta.columna() - desde.columna()));
	}

	private static Direccion direccionEntre(Celda desde, Celda hasta) {
		for (Direccion direccion : Direccion.values()) {
			if (desde.mover(direccion).equals(hasta)) {
				return direccion;
			}
		}
		throw new IllegalStateException(DIRECCION_INVALIDA);
	}
}
