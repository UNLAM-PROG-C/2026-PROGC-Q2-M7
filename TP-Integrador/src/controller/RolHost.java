package controller;

import java.util.List;
import network.CanalServidor;
import network.MensajeRecibido;
import network.MensajeRed;
import network.ServidorPartida;
import network.TipoMensaje;
import utils.Constantes;

/**
 * Rol de host: dueño de la sala y autoridad de la partida (decide el ganador). Habla con los
 * clientes por el {@link CanalServidor}; lo recibido llega como eventos al bus del motor.
 */
final class RolHost implements RolPartida {
	static final String MOTIVO_EN_CURSO = "PARTIDA_EN_CURSO";
	private static final int UN_ARGUMENTO = 1;
	private static final int PRIMER_ARGUMENTO = 0;
	private static final String MSJ_SE_FUE = "%s se desconectó.";

	private final MotorJuego motor;
	private final CanalServidor red;

	RolHost(MotorJuego motor, CanalServidor red) {
		this.motor = motor;
		this.red = red;
	}

	@Override
	public void alArrancar() {
		motor.notificarSala();
	}

	@Override
	public void alRecibir(MensajeRecibido recibido) {
		int origen = recibido.origen();
		List<String> args = recibido.mensaje().argumentos();
		if (origen == Constantes.ID_HOST) {
			return;
		}
		switch (recibido.mensaje().tipo()) {
			case HOLA -> recibirHola(origen, args);
			case LISTO -> recibirListo(origen, args);
			case RONDA_COMPLETA -> recibirRonda(origen);
			default -> { }
		}
	}

	private void recibirHola(int origen, List<String> args) {
		SalaPartida sala = motor.sala();
		if (args.size() != UN_ARGUMENTO || sala.contiene(origen)) {
			return;
		}
		if (motor.jugando() || sala.estaCompleta()) {
			rechazar(origen, motor.jugando() ? MOTIVO_EN_CURSO : ServidorPartida.MOTIVO_SALA_LLENA);
			return;
		}
		sala.agregar(origen, args.get(PRIMER_ARGUMENTO));
		red.enviarA(origen, MensajeRed.de(TipoMensaje.BIENVENIDA, origen));
		red.enviarA(origen, ProtocoloSala.mensajeConfig(sala.config()));
		difundirSala();
	}

	private void rechazar(int origen, String motivo) {
		red.enviarA(origen, MensajeRed.de(TipoMensaje.RECHAZADO, motivo));
		red.desconectar(origen);
	}

	private void recibirListo(int origen, List<String> args) {
		if (args.size() == UN_ARGUMENTO) {
			ProtocoloSala.booleano(args.get(PRIMER_ARGUMENTO))
					.ifPresent(listo -> actualizarListo(origen, listo));
		}
	}

	@Override
	public void alMarcarListo(boolean listo) {
		actualizarListo(Constantes.ID_HOST, listo);
	}

	private void actualizarListo(int id, boolean listo) {
		SalaPartida sala = motor.sala();
		if (motor.jugando() || !sala.contiene(id)) {
			return;
		}
		sala.marcarListo(id, listo);
		difundirSala();
		if (sala.todosListos()) {
			long semilla = motor.nuevaSemilla();
			red.enviar(MensajeRed.de(TipoMensaje.EMPEZAR, semilla));
			motor.empezar(semilla);
		}
	}

	private void recibirRonda(int origen) {
		if (!motor.jugando() || !motor.partida().marcador().contiene(origen)) {
			return;
		}
		red.enviarATodosMenos(origen, MensajeRed.de(TipoMensaje.RONDA_COMPLETA, origen));
		motor.rondaDeOtro(origen);
		decidirGanador(origen);
	}

	/** El primero que llega al objetivo, en el orden en que el host procesa, gana. */
	private void decidirGanador(int id) {
		if (motor.partida().alcanzoObjetivo(id)) {
			red.enviar(MensajeRed.de(TipoMensaje.PARTIDA_GANADA, id));
			motor.terminar(id);
		}
	}

	@Override
	public void alCompletarRonda() {
		red.enviar(MensajeRed.de(TipoMensaje.RONDA_COMPLETA, Constantes.ID_HOST));
		decidirGanador(Constantes.ID_HOST);
	}

	@Override
	public void alDesconectarse(int id) {
		SalaPartida sala = motor.sala();
		if (!sala.contiene(id)) {
			return;
		}
		String nombre = sala.nombre(id);
		sala.quitar(id);
		motor.jugadorSeFue(id, String.format(MSJ_SE_FUE, nombre));
		difundirSala();
	}

	@Override
	public void alVolverSala() {
		difundirSala();
	}

	private void difundirSala() {
		red.enviar(ProtocoloSala.mensajeSala(motor.sala().jugadores()));
		motor.notificarSala();
	}
}
