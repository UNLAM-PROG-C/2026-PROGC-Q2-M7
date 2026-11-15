package controller;

import java.util.List;
import java.util.Objects;
import model.ConfigPartida;

/**
 * Foto inmutable de la sala de espera que el motor le manda a la vista.
 *
 * @param config configuración elegida por el host (null en el cliente hasta recibirla)
 * @param jugadores jugadores en la sala, en orden de llegada (el host primero)
 * @param idLocal id del jugador de esta ventana (-1 si el cliente todavía no fue aceptado)
 * @param esHost true si esta ventana es el host
 * @param direccionCompartir "ip:puerto" del host para compartir con los demás
 * @param mensaje texto de estado para mostrar ("Esperando jugadores…", etc.)
 */
public record EstadoSala(ConfigPartida config, List<JugadorSala> jugadores, int idLocal,
		boolean esHost, String direccionCompartir, String mensaje) {

	public EstadoSala {
		jugadores = List.copyOf(jugadores);
		Objects.requireNonNull(mensaje, "mensaje");
	}

	/** True si la sala está completa y todos marcaron listo. */
	public boolean todosListos() {
		return config != null && jugadores.size() == config.jugadores()
				&& jugadores.stream().allMatch(JugadorSala::listo);
	}
}
