package controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.ConfigPartida;

/**
 * Sala de espera de la partida: quién está, quién marcó listo y la configuración. La usa solo
 * el hilo del motor (confinamiento de hilo), por eso no sincroniza.
 */
final class SalaPartida {
	static final int SIN_ID = -1;
	private static final int LARGO_MAXIMO_NOMBRE = 20;
	private static final String PREFIJO_NOMBRE = "Jugador ";
	private static final String CARACTERES_DE_CONTROL = "\\p{Cntrl}";
	private static final String MSJ_FALTAN = "Esperando jugadores (%d/%d)…";
	private static final String MSJ_FALTA_LISTO = "Esperando que todos marquen Listo.";
	private static final String MSJ_TODOS_LISTOS = "¡Todos listos! Empezando…";

	private final boolean esHost;
	private final String direccionCompartir;
	private final Map<Integer, JugadorSala> jugadores = new LinkedHashMap<>();
	private ConfigPartida config;
	private int idLocal;

	SalaPartida(boolean esHost, int idLocal, String direccionCompartir) {
		this.esHost = esHost;
		this.idLocal = idLocal;
		this.direccionCompartir = direccionCompartir;
	}

	/** Deja un nombre imprimible, acotado y no vacío. */
	static String nombreSeguro(String nombre, int id) {
		String limpio = nombre == null ? "" : nombre.replaceAll(CARACTERES_DE_CONTROL, "").trim();
		if (limpio.isEmpty()) {
			return PREFIJO_NOMBRE + (id + 1);
		}
		return limpio.substring(0, Math.min(limpio.length(), LARGO_MAXIMO_NOMBRE));
	}

	ConfigPartida config() {
		return config;
	}

	void config(ConfigPartida nueva) {
		this.config = nueva;
	}

	int idLocal() {
		return idLocal;
	}

	void idLocal(int nuevo) {
		this.idLocal = nuevo;
	}

	boolean contiene(int id) {
		return jugadores.containsKey(id);
	}

	boolean estaCompleta() {
		return config != null && jugadores.size() >= config.jugadores();
	}

	boolean agregar(int id, String nombre) {
		if (contiene(id) || estaCompleta()) {
			return false;
		}
		jugadores.put(id, new JugadorSala(id, nombreSeguro(nombre, id), false, true));
		return true;
	}

	String nombre(int id) {
		JugadorSala j = jugadores.get(id);
		return j == null ? "" : j.nombre();
	}

	void quitar(int id) {
		jugadores.remove(id);
	}

	void marcarListo(int id, boolean listo) {
		jugadores.computeIfPresent(id,
				(clave, j) -> new JugadorSala(j.id(), j.nombre(), listo, j.conectado()));
	}

	void reiniciarListos() {
		for (int id : List.copyOf(jugadores.keySet())) {
			marcarListo(id, false);
		}
	}

	void reemplazar(List<JugadorSala> nuevos) {
		jugadores.clear();
		nuevos.forEach(j -> jugadores.put(j.id(), j));
	}

	List<JugadorSala> jugadores() {
		return List.copyOf(jugadores.values());
	}

	boolean todosListos() {
		return foto("").todosListos();
	}

	EstadoSala fotoEstado() {
		return foto(mensajeEstado());
	}

	private EstadoSala foto(String mensaje) {
		return new EstadoSala(config, jugadores(), idLocal, esHost, direccionCompartir, mensaje);
	}

	private String mensajeEstado() {
		if (config == null || !estaCompleta()) {
			int total = config == null ? 0 : config.jugadores();
			return String.format(MSJ_FALTAN, jugadores.size(), total);
		}
		return todosListos() ? MSJ_TODOS_LISTOS : MSJ_FALTA_LISTO;
	}
}
