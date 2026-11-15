package controller;

import network.MensajeRecibido;

/**
 * Estrategia (patrón Strategy) con lo que cambia según el rol de este motor: partida local,
 * host (autoridad) o cliente. El motor hace lo común y delega acá lo específico. Todos los
 * métodos se invocan en el hilo del motor, salvo {@link #alArrancar}, que corre antes de que
 * arranque ese hilo.
 */
interface RolPartida {
	/** Arranque del motor, con la red ya iniciada. */
	default void alArrancar() {
	}

	/** Evento INICIAR de la UI (solo tiene sentido en partida local). */
	default void alIniciarLocal(Object payload) {
	}

	default void alRecibir(MensajeRecibido recibido) {
	}

	default void alDesconectarse(int idJugador) {
	}

	default void alMarcarListo(boolean listo) {
	}

	default void alPerderConexion() {
	}

	/** El jugador local completó una ronda (ya está sumada en el marcador). */
	default void alCompletarRonda() {
	}

	default void alVolverSala() {
	}
}
