package controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import network.CanalRed;
import network.MensajeRecibido;
import network.MensajeRed;
import network.ServidorPartida;
import network.TipoMensaje;
import utils.Constantes;

/**
 * Rol de cliente: obedece al host. Solo acepta los mensajes que un host puede mandar y,
 * al completar todas sus rondas, espera la confirmación del host en lugar de declararse ganador.
 */
final class RolCliente implements RolPartida {
	private static final int UN_ARGUMENTO = 1;
	private static final int PRIMER_ARGUMENTO = 0;
	private static final String MSJ_RECHAZO = "El host rechazó la conexión: ";
	private static final String MSJ_CONEXION_PERDIDA = "Se perdió la conexión con el host.";

	private static final Map<String, String> MOTIVOS = Map.of(
			ServidorPartida.MOTIVO_SALA_LLENA, "La sala está llena.",
			RolHost.MOTIVO_EN_CURSO, "La partida ya está en curso.");

	private final MotorJuego motor;
	private final CanalRed red;
	private final String nombre;

	RolCliente(MotorJuego motor, CanalRed red, String nombre) {
		this.motor = motor;
		this.red = red;
		this.nombre = nombre;
	}

	@Override
	public void alArrancar() {
		red.enviar(MensajeRed.de(TipoMensaje.HOLA, nombre));
	}

	@Override
	public void alRecibir(MensajeRecibido recibido) {
		List<String> args = recibido.mensaje().argumentos();
		if (recibido.origen() != Constantes.ID_HOST) {
			return;
		}
		switch (recibido.mensaje().tipo()) {
			case BIENVENIDA -> recibirBienvenida(args);
			case CONFIG -> recibirConfig(args);
			case SALA -> recibirSala(args);
			case EMPEZAR -> recibirEmpezar(args);
			case RONDA_COMPLETA -> recibirRonda(args);
			case PARTIDA_GANADA -> recibirGanador(args);
			case RECHAZADO -> recibirRechazo(args);
			default -> { }
		}
	}

	private Optional<Integer> idUnico(List<String> args) {
		return args.size() == UN_ARGUMENTO
				? ProtocoloSala.entero(args.get(PRIMER_ARGUMENTO)) : Optional.empty();
	}

	private void recibirBienvenida(List<String> args) {
		Optional<Integer> id = idUnico(args);
		boolean sinId = motor.sala().idLocal() == SalaPartida.SIN_ID;
		if (sinId && !motor.jugando() && id.isPresent() && id.get() != Constantes.ID_HOST) {
			motor.sala().idLocal(id.get());
		}
	}

	private void recibirConfig(List<String> args) {
		if (!motor.jugando()) {
			ProtocoloSala.leerConfig(args).ifPresent(motor.sala()::config);
		}
	}

	private void recibirSala(List<String> args) {
		Optional<List<JugadorSala>> jugadores = ProtocoloSala.leerSala(args);
		if (jugadores.isEmpty()) {
			return;
		}
		motor.sala().reemplazar(jugadores.get());
		if (motor.jugando()) {
			motor.sincronizarConectados(jugadores.get());
		} else {
			motor.notificarSala();
		}
	}

	private void recibirEmpezar(List<String> args) {
		SalaPartida sala = motor.sala();
		boolean listoParaEmpezar = !motor.jugando() && sala.config() != null
				&& sala.contiene(sala.idLocal());
		if (listoParaEmpezar && args.size() == UN_ARGUMENTO) {
			ProtocoloSala.largo(args.get(PRIMER_ARGUMENTO)).ifPresent(motor::empezar);
		}
	}

	private void recibirRonda(List<String> args) {
		Optional<Integer> id = idUnico(args);
		if (motor.jugando() && id.isPresent() && id.get() != motor.sala().idLocal()
				&& motor.partida().marcador().contiene(id.get())) {
			motor.rondaDeOtro(id.get());
		}
	}

	private void recibirGanador(List<String> args) {
		Optional<Integer> id = idUnico(args);
		if (motor.jugando() && id.isPresent() && motor.partida().marcador().contiene(id.get())) {
			motor.terminar(id.get());
		}
	}

	private void recibirRechazo(List<String> args) {
		if (!motor.jugando() && args.size() == UN_ARGUMENTO) {
			String motivo = args.get(PRIMER_ARGUMENTO);
			motor.abortar(MSJ_RECHAZO + MOTIVOS.getOrDefault(motivo, motivo));
		}
	}

	@Override
	public void alMarcarListo(boolean listo) {
		if (!motor.jugando() && motor.sala().idLocal() != SalaPartida.SIN_ID) {
			red.enviar(MensajeRed.de(TipoMensaje.LISTO, listo));
		}
	}

	@Override
	public void alCompletarRonda() {
		red.enviar(MensajeRed.de(TipoMensaje.RONDA_COMPLETA));
	}

	@Override
	public void alPerderConexion() {
		motor.abortar(MSJ_CONEXION_PERDIDA);
	}
}
