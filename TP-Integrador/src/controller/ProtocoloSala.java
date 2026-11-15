package controller;

import static network.TipoMensaje.CONFIG;
import static network.TipoMensaje.SALA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.ConfigPartida;
import model.Dificultad;
import network.MensajeRed;

/**
 * Codifica y decodifica los argumentos de los mensajes de sala. Todo parseo es defensivo: ante
 * cualquier argumento mal formado devuelve vacío en lugar de lanzar.
 */
final class ProtocoloSala {
	private static final int ARGUMENTOS_CONFIG = 3;
	private static final int ARGUMENTOS_POR_JUGADOR = 3;
	private static final int POS_DIFICULTAD = 0;
	private static final int POS_RONDAS = 1;
	private static final int POS_JUGADORES = 2;
	private static final int POS_ID = 0;
	private static final int POS_NOMBRE = 1;
	private static final int POS_LISTO = 2;
	private static final String VERDADERO = "true";
	private static final String FALSO = "false";

	private ProtocoloSala() {
	}

	static MensajeRed mensajeConfig(ConfigPartida c) {
		return MensajeRed.de(CONFIG, c.dificultad().name(), c.rondas(), c.jugadores());
	}

	static MensajeRed mensajeSala(List<JugadorSala> jugadores) {
		List<Object> argumentos = new ArrayList<>();
		for (JugadorSala j : jugadores) {
			argumentos.add(j.id());
			argumentos.add(j.nombre());
			argumentos.add(j.listo());
		}
		return MensajeRed.de(SALA, argumentos.toArray());
	}

	static Optional<ConfigPartida> leerConfig(List<String> a) {
		if (a.size() != ARGUMENTOS_CONFIG) {
			return Optional.empty();
		}
		try {
			ConfigPartida c = new ConfigPartida(Dificultad.valueOf(a.get(POS_DIFICULTAD)),
					Integer.parseInt(a.get(POS_RONDAS)), Integer.parseInt(a.get(POS_JUGADORES)));
			return c.esMultijugador() ? Optional.of(c) : Optional.empty();
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	static Optional<List<JugadorSala>> leerSala(List<String> a) {
		if (a.size() % ARGUMENTOS_POR_JUGADOR != 0
				|| a.size() / ARGUMENTOS_POR_JUGADOR > ConfigPartida.MAX_JUGADORES) {
			return Optional.empty();
		}
		Map<Integer, JugadorSala> jugadores = new LinkedHashMap<>();
		for (int i = 0; i < a.size(); i += ARGUMENTOS_POR_JUGADOR) {
			Optional<JugadorSala> j = leerJugador(a.subList(i, i + ARGUMENTOS_POR_JUGADOR));
			if (j.isEmpty() || jugadores.put(j.get().id(), j.get()) != null) {
				return Optional.empty();
			}
		}
		return Optional.of(List.copyOf(jugadores.values()));
	}

	private static Optional<JugadorSala> leerJugador(List<String> t) {
		Optional<Integer> id = entero(t.get(POS_ID));
		Optional<Boolean> listo = booleano(t.get(POS_LISTO));
		if (id.isEmpty() || id.get() < 0 || listo.isEmpty()) {
			return Optional.empty();
		}
		String nombre = SalaPartida.nombreSeguro(t.get(POS_NOMBRE), id.get());
		return Optional.of(new JugadorSala(id.get(), nombre, listo.get(), true));
	}

	static Optional<Integer> entero(String texto) {
		try {
			return Optional.of(Integer.parseInt(texto));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	static Optional<Long> largo(String texto) {
		try {
			return Optional.of(Long.parseLong(texto));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	static Optional<Boolean> booleano(String texto) {
		if (VERDADERO.equals(texto)) {
			return Optional.of(true);
		}
		return FALSO.equals(texto) ? Optional.of(false) : Optional.empty();
	}
}
