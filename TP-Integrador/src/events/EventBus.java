package events;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventBus {
	private final BlockingQueue<Evento> colaEventos = new LinkedBlockingQueue<>();

	public void publicar(Evento e) {
		if (e != null){
			colaEventos.offer(e);
		}
	}

	public Evento tomar() throws InterruptedException {
		return colaEventos.take();
	}
}
