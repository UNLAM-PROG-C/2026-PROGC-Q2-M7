package ui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import model.Celda;
import model.Direccion;
import model.EstadoJuego;

/** Recorte del camino confirmado: conserva el recorrido hasta una celda y deshace el resto. */
final class RecorteMouse implements GestoMouse {
	private static final String ORIGEN_NULO = "El origen no puede ser nulo.";

	private final EstadoJuego origen;
	private int conservadas;

	private RecorteMouse(EstadoJuego origen, int conservadas) {
		this.origen = Objects.requireNonNull(origen, ORIGEN_NULO);
		this.conservadas = conservadas;
	}

	/** Crea un recorte si la celda pertenece al camino confirmado. */
	static Optional<RecorteMouse> desde(EstadoJuego origen, Celda celda) {
		int indice = origen.recorrido().indexOf(celda);
		return indice < 0 ? Optional.empty() : Optional.of(new RecorteMouse(origen, indice + 1));
	}

	@Override
	public EstadoJuego origen() {
		return origen;
	}

	@Override
	public boolean extenderHacia(Celda destino) {
		int indice = origen.recorrido().indexOf(destino);
		if (indice < 0 || indice + 1 == conservadas) {
			return false;
		}
		conservadas = indice + 1;
		return true;
	}

	@Override
	public boolean tieneCambios() {
		return pasos() > 0;
	}

	@Override
	public void mostrarEn(PanelTablero tablero) {
		tablero.mostrarRecorte(vistaPrevia(), recortadas());
	}

	@Override
	public void confirmar(Consumer<Direccion> mover, Runnable deshacer) {
		for (int paso = 0; paso < pasos(); paso++) {
			deshacer.run();
		}
	}

	int pasos() {
		return origen.recorrido().size() - conservadas;
	}

	EstadoJuego vistaPrevia() {
		return new EstadoJuego(origen.nivel(), origen.recorrido().subList(0, conservadas));
	}

	List<Celda> recortadas() {
		List<Celda> recorrido = origen.recorrido();
		return List.copyOf(recorrido.subList(conservadas, recorrido.size()));
	}
}
