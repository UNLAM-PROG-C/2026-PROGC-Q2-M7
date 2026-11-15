package controller;

import events.Evento;
import events.EventBus;
import events.TipoEvento;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import utils.Constantes;

/**
 * Temporizador del tomatazo: cada {@link #aplicar} cancela la limpieza anterior y programa una
 * nueva, así el tomate dura la duración completa desde el último. El hilo del timer solo publica
 * {@code LIMPIAR_TOMATE} en el bus; el estado lo cambia el motor. Cada limpieza lleva un número
 * de generación para descartar las que quedaron viejas en la cola.
 */
final class TomateTemporizado implements AutoCloseable {
	private static final long MILIS_POR_SEGUNDO = 1000L;
	private static final String NOMBRE_HILO = "Timer-Tomate";

	static final long DURACION_MILIS = Constantes.SEGUNDOS_TOMATE * MILIS_POR_SEGUNDO;

	private final EventBus bus;
	private final long duracionMilis;
	private final ScheduledExecutorService timer =
			Executors.newSingleThreadScheduledExecutor(TomateTemporizado::crearHilo);
	private ScheduledFuture<?> pendiente;
	private long generacion;
	private boolean activo;

	TomateTemporizado(EventBus bus, long duracionMilis) {
		this.bus = bus;
		this.duracionMilis = duracionMilis;
	}

	private static Thread crearHilo(Runnable tarea) {
		Thread hilo = new Thread(tarea, NOMBRE_HILO);
		hilo.setDaemon(true);
		return hilo;
	}

	/** Empieza (o reinicia) la cuenta regresiva. */
	void aplicar() {
		cancelar();
		long mia = generacion;
		activo = true;
		pendiente = timer.schedule(() -> bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE, mia)),
				duracionMilis, TimeUnit.MILLISECONDS);
	}

	/** True (una sola vez) si el aviso corresponde a la cuenta vigente. */
	boolean consumir(Object payload) {
		boolean vigente = activo && payload instanceof Long g && g == generacion;
		if (vigente) {
			activo = false;
		}
		return vigente;
	}

	void cancelar() {
		if (pendiente != null) {
			pendiente.cancel(false);
			pendiente = null;
		}
		generacion++;
		activo = false;
	}

	@Override
	public void close() {
		timer.shutdownNow();
	}
}
