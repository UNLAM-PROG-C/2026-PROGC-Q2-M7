package controller;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import model.Dificultad;
import model.GeneradorNivel;
import model.Nivel;

/**
 * Genera los niveles de una partida en un hilo propio, una ronda por adelantado.
 * El nivel de cada ronda depende solo de (dificultad, semilla, ronda): todos los jugadores
 * con la misma semilla reciben los mismos tableros en el mismo orden.
 *
 * <p>Patrón productor-consumidor con promesas (Future): el hilo "Thread-Pregenerador" es el
 * productor (un ExecutorService de un solo hilo, daemon); quien llama a {@link #nivel(int)}
 * (el hilo del motor) es el consumidor y recibe un {@link Future} por ronda. Pedir la ronda
 * siguiente apenas se entrega la actual hace que, cuando el jugador la termine, el nivel ya
 * esté calculado y el hilo del motor casi nunca espere.
 */
public final class PregeneradorNiveles implements AutoCloseable {
	private static final String NOMBRE_HILO = "Thread-Pregenerador";
	private static final String RONDA_INVALIDA = "La ronda no puede ser negativa.";
	private static final String CERRADO = "El pregenerador está cerrado.";
	private static final String GENERACION_FALLIDA = "No se pudo generar el nivel.";
	private static final String ESPERA_INTERRUMPIDA = "Espera del nivel interrumpida.";
	private static final long INCREMENTO_RONDA = 0x9E3779B97F4A7C15L;
	private static final long MULTIPLICADOR_A = 0xBF58476D1CE4E5B9L;
	private static final long MULTIPLICADOR_B = 0x94D049BB133111EBL;
	private static final int DESPLAZAMIENTO_A = 30;
	private static final int DESPLAZAMIENTO_B = 27;
	private static final int DESPLAZAMIENTO_C = 31;

	private final Dificultad dificultad;
	private final long semilla;
	private final GeneradorNivel generador = new GeneradorNivel();
	private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(fabricaDeHilo());
	private final Map<Integer, Future<Nivel>> pedidos = new ConcurrentHashMap<>();
	private volatile boolean cerrado;

	public PregeneradorNiveles(Dificultad dificultad, long semilla) {
		this.dificultad = dificultad;
		this.semilla = semilla;
		pedir(0);
	}

	/**
	 * Semilla de una ronda, derivada de forma determinista de (semilla, ronda) con la mezcla
	 * SplitMix64, para que rondas consecutivas no tengan semillas correlacionadas.
	 */
	public static long semillaDeRonda(long semilla, int ronda) {
		long valor = semilla + (ronda + 1L) * INCREMENTO_RONDA;
		valor = (valor ^ (valor >>> DESPLAZAMIENTO_A)) * MULTIPLICADOR_A;
		valor = (valor ^ (valor >>> DESPLAZAMIENTO_B)) * MULTIPLICADOR_B;
		return valor ^ (valor >>> DESPLAZAMIENTO_C);
	}

	/**
	 * Nivel de la ronda (0, 1, 2…). Bloquea solo si todavía no estaba listo; encarga la
	 * siguiente.
	 */
	public Nivel nivel(int ronda) {
		if (ronda < 0) {
			throw new IllegalArgumentException(RONDA_INVALIDA);
		}
		Future<Nivel> actual = pedir(ronda);
		pedir(ronda + 1);
		return esperar(actual);
	}

	@Override
	public void close() {
		cerrado = true;
		ejecutor.shutdownNow();
	}

	private Future<Nivel> pedir(int ronda) {
		if (cerrado) {
			throw new IllegalStateException(CERRADO);
		}
		return pedidos.computeIfAbsent(ronda, this::encargar);
	}

	private Future<Nivel> encargar(int ronda) {
		long semillaRonda = semillaDeRonda(semilla, ronda);
		return ejecutor.submit(() -> generador.generar(dificultad, semillaRonda));
	}

	private Nivel esperar(Future<Nivel> futuro) {
		try {
			return futuro.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ESPERA_INTERRUMPIDA, e);
		} catch (ExecutionException | CancellationException e) {
			throw new IllegalStateException(GENERACION_FALLIDA, e);
		}
	}

	private static ThreadFactory fabricaDeHilo() {
		return tarea -> {
			Thread hilo = new Thread(tarea, NOMBRE_HILO);
			hilo.setDaemon(true);
			return hilo;
		};
	}
}
