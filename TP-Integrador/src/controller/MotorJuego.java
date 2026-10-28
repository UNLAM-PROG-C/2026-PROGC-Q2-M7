package controller;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import events.*;
import network.GestorRed;
import utils.Constantes;

public class MotorJuego {
	private final EventBus bus;
	private final GestorRed red;
	private Thread hiloConsumidor;
	private final ScheduledExecutorService timer =
		Executors.newScheduledThreadPool(Constantes.HILOS_TIMER);

	public MotorJuego(int rol, EventBus bus) throws IOException {
		this.bus = bus;
		this.red = new GestorRed(rol, bus);
	}


	public void iniciar() {
		red.iniciarEscucha();
		hiloConsumidor = new Thread(this::cicloConsumidor, "Thread-MotorJuego");
		hiloConsumidor.setDaemon(true);
		hiloConsumidor.start();
	}

	private void cicloConsumidor() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Evento event = bus.tomar();
				despacharEvento(event);

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	private void despacharEvento(Evento event) {
		switch (event.tipo()){
			case MOVIMIENTO -> procesarMovimiento(event.payload());
			case RIVAL_COMPLETO_GRILLA -> procesarRivalCompletoGrilla();
			case LIMPIAR_TOMATE -> procesarLimpiarTomate();
			case CONEXION_PERDIDA -> procesarConexionPerdida();
			case GRILLA_COMPLETA -> procesarGrillaCompleta();
			default -> {
				System.err.println("Evento no conocido: " + event.tipo());
			}
		}
	}


	private void procesarGrillaCompleta() {
		System.out.println("[LOCAL] ¡Completaste la grilla! Enviando tomate al rival...");
    	red.enviarEvento(TipoEvento.RIVAL_COMPLETO_GRILLA);
	}


	private Object procesarConexionPerdida() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'procesarConexionPerdida'");
	}


	private Object procesarMovimiento(Object payload) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'procesarMovimiento'");
	}

	private void procesarLimpiarTomate() {
		// TODO Definir X y la logica de limpieza en la UI.
		System.out.println("[LIMPIO] Pasaron los Xs. Se limpió el tomate.");
	}

	/**
	 * Procesa el evento de rival completando la grilla.
	 * 
	 * TODO: Crear la logica para "manchar" la pantalla del jugador local por X segundos. Definir X segundos.
	 */
	private void procesarRivalCompletoGrilla() {
		System.out.println("[TOMATE] ¡Rival completó grilla! Pantalla manchada por Xs...");
		timer.schedule( () -> 
			bus.publicar(new Evento(TipoEvento.LIMPIAR_TOMATE)),
			4, TimeUnit.SECONDS
		);
	}


	public void finalizar() {
		timer.shutdownNow();
        try {
			red.cerrar();
		} catch (IOException e) {
			System.err.println("Error al cerrar: " + e.getMessage());
		}
	}

}
