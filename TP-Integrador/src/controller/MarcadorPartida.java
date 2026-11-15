package controller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Rondas ganadas por cada jugador de la partida en curso. Confinado al hilo del motor. */
final class MarcadorPartida {
	private static final int SIN_RONDAS = 0;
	private static final String NOMBRE_DESCONOCIDO = "?";

	private final Map<Integer, PuntajeJugador> puntajes = new LinkedHashMap<>();

	MarcadorPartida(List<JugadorSala> jugadores, int idLocal) {
		for (JugadorSala j : jugadores) {
			puntajes.put(j.id(),
					new PuntajeJugador(j.id(), j.nombre(), SIN_RONDAS, j.id() == idLocal, true));
		}
	}

	boolean contiene(int id) {
		return puntajes.containsKey(id);
	}

	int rondas(int id) {
		PuntajeJugador p = puntajes.get(id);
		return p == null ? SIN_RONDAS : p.rondas();
	}

	String nombre(int id) {
		PuntajeJugador p = puntajes.get(id);
		return p == null ? NOMBRE_DESCONOCIDO : p.nombre();
	}

	/** Suma una ronda al jugador y devuelve su total. */
	int sumar(int id) {
		cambiar(id, p -> new PuntajeJugador(p.id(), p.nombre(), p.rondas() + 1,
				p.esLocal(), p.conectado()));
		return rondas(id);
	}

	void desconectar(int id) {
		cambiar(id, p -> new PuntajeJugador(p.id(), p.nombre(), p.rondas(),
				p.esLocal(), false));
	}

	/** Desconecta a quienes no figuran entre los presentes; true si cambió algo. */
	boolean sincronizarConectados(Collection<Integer> presentes) {
		boolean huboCambios = false;
		for (PuntajeJugador p : puntajes()) {
			if (p.conectado() && !presentes.contains(p.id())) {
				desconectar(p.id());
				huboCambios = true;
			}
		}
		return huboCambios;
	}

	List<PuntajeJugador> puntajes() {
		return List.copyOf(puntajes.values());
	}

	private void cambiar(int id, UnaryOperator<PuntajeJugador> cambio) {
		puntajes.computeIfPresent(id, (clave, p) -> cambio.apply(p));
	}
}
