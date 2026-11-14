package ui;

import java.util.function.Consumer;
import model.Celda;
import model.Direccion;
import model.EstadoJuego;

/** Gesto provisional de mouse (trazar o recortar) que solo impacta en el juego al confirmarse. */
interface GestoMouse {
	/** Snapshot del juego sobre el que se armó el gesto. */
	EstadoJuego origen();

	/** Lleva el gesto hasta la celda indicada; devuelve true si el gesto cambió. */
	boolean extenderHacia(Celda destino);

	/** Indica si confirmar el gesto modificaría el juego. */
	boolean tieneCambios();

	/** Dibuja la vista previa del gesto en el tablero. */
	void mostrarEn(PanelTablero tablero);

	/** Publica las acciones del gesto: movimientos o pasos a deshacer. */
	void confirmar(Consumer<Direccion> mover, Runnable deshacer);
}
