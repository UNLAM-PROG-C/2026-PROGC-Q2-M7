package events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventBus {
	public static final int CAPACIDAD_MAXIMA = 256;

	private final BlockingQueue<Evento> colaEventos = new LinkedBlockingQueue<>(CAPACIDAD_MAXIMA);

	public boolean publicar(Evento e) {
		if (e == null || e.tipo() == null) {
			return false;
		}
		return colaEventos.offer(e);
	}

	public Evento tomar() throws InterruptedException {
		return colaEventos.take();
	}
}
