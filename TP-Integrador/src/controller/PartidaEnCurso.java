package controller;

import events.DestinoCelda;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import model.ConfigPartida;
import model.Direccion;
import model.ResultadoMovimiento;
import model.TableroZip;

/**
 * Una partida empezada: tablero de la ronda actual del jugador local, marcador y niveles
 * pregenerados (la ronda r del jugador local es {@code niveles.nivel(r)}). Confinada al hilo
 * del motor, salvo {@link #close}, que es idempotente y puede llamarse desde otro hilo.
 */
final class PartidaEnCurso implements AutoCloseable {
	private static final int PRIMERA_RONDA = 0;
	private static final int SIN_GANADOR = -1;

	private final ConfigPartida config;
	private final int idLocal;
	private final PregeneradorNiveles niveles;
	private final MarcadorPartida marcador;
	private final AtomicBoolean cerrada = new AtomicBoolean();
	private TableroZip tablero;
	private boolean finalizada;
	private int idGanador = SIN_GANADOR;

	PartidaEnCurso(ConfigPartida config, long semilla, int idLocal, List<JugadorSala> jugadores) {
		this.config = config;
		this.idLocal = idLocal;
		this.marcador = new MarcadorPartida(jugadores, idLocal);
		this.niveles = new PregeneradorNiveles(config.dificultad(), semilla);
		this.tablero = new TableroZip(niveles.nivel(PRIMERA_RONDA));
	}

	int idLocal() {
		return idLocal;
	}

	boolean finalizada() {
		return finalizada;
	}

	MarcadorPartida marcador() {
		return marcador;
	}

	/** El jugador local ya completó todas sus rondas y espera que se confirme el ganador. */
	boolean esperaConfirmacion() {
		return !finalizada && alcanzoObjetivo(idLocal);
	}

	boolean puedeJugar() {
		return !finalizada && !alcanzoObjetivo(idLocal);
	}

	boolean alcanzoObjetivo(int id) {
		return marcador.rondas(id) >= config.rondas();
	}

	int objetivo() {
		return config.rondas();
	}

	ResultadoMovimiento mover(Direccion direccion) {
		return tablero.mover(direccion);
	}

	/** Igual que mover, pero descarta pedidos hechos sobre un tablero que ya cambió. */
	ResultadoMovimiento irA(DestinoCelda destino) {
		if (destino.nivel() != tablero.nivel()) {
			return ResultadoMovimiento.SIN_EFECTO;
		}
		return tablero.irA(destino.celda());
	}

	boolean deshacer() {
		return tablero.deshacer();
	}

	void reiniciar() {
		tablero.reiniciar();
	}

	boolean rondaCompleta() {
		return tablero.estadoJuego().gano();
	}

	/** Suma la ronda al jugador local y, si le faltan rondas, pasa a su siguiente nivel. */
	int completarRondaLocal() {
		int rondas = marcador.sumar(idLocal);
		if (rondas < config.rondas()) {
			tablero = new TableroZip(niveles.nivel(rondas));
		}
		return rondas;
	}

	int sumarRonda(int id) {
		return marcador.sumar(id);
	}

	void finalizar(int idDelGanador) {
		finalizada = true;
		idGanador = idDelGanador;
		close();
	}

	ActualizacionVista foto(String mensaje, boolean manchadoPorTomate) {
		String ganador = finalizada ? marcador.nombre(idGanador) : null;
		return new ActualizacionVista(tablero.estadoJuego(), mensaje, manchadoPorTomate,
				marcador.puntajes(), config.rondas(), finalizada,
				finalizada && idGanador == idLocal, ganador);
	}

	@Override
	public void close() {
		if (cerrada.compareAndSet(false, true)) {
			niveles.close();
		}
	}
}
