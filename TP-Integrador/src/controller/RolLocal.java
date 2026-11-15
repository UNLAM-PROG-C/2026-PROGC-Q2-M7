package controller;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import model.ConfigPartida;
import utils.Constantes;

/** Rol de partida individual: sin red; el jugador local decide solo cuándo ganó. */
final class RolLocal implements RolPartida {
	private static final String NOMBRE_LOCAL = "Vos";

	private final MotorJuego motor;

	RolLocal(MotorJuego motor) {
		this.motor = motor;
	}

	@Override
	public void alIniciarLocal(Object payload) {
		if (!(payload instanceof ConfigPartida config) || config.esMultijugador()) {
			return;
		}
		SalaPartida sala = motor.sala();
		sala.config(config);
		sala.idLocal(Constantes.ID_HOST);
		sala.reemplazar(List.of(
				new JugadorSala(Constantes.ID_HOST, NOMBRE_LOCAL, true, true)));
		motor.empezar(ThreadLocalRandom.current().nextLong());
	}

	@Override
	public void alCompletarRonda() {
		if (motor.partida().alcanzoObjetivo(Constantes.ID_HOST)) {
			motor.terminar(Constantes.ID_HOST);
		}
	}
}
