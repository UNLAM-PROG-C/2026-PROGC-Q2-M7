package ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import model.Celda;
import model.Direccion;
import model.EstadoJuego;

/** Trazo provisional dibujado con el mouse sobre un estado de origen. */
final class TrazoMouse implements GestoMouse {
	private static final String ORIGEN_NULO = "El origen no puede ser nulo.";
	private static final String DIRECCION_INVALIDA = "Las celdas consecutivas no son adyacentes.";

	private final EstadoJuego origen;
	private final List<Celda> trazo = new ArrayList<>();

	TrazoMouse(EstadoJuego origen) {
		this.origen = Objects.requireNonNull(origen, ORIGEN_NULO);
	}

	@Override
	public EstadoJuego origen() {
		return origen;
	}

	@Override
	public boolean extenderHacia(Celda destino) {
		if (destino.equals(origen.actual())) {
			return vaciarTrazo();
		}
		int indice = trazo.indexOf(destino);
		if (indice >= 0) {
			return truncarEn(indice + 1);
		}
		return avanzarHacia(destino);
	}

	@Override
	public boolean tieneCambios() {
		return !trazo.isEmpty();
	}

	@Override
	public void mostrarEn(PanelTablero tablero) {
		tablero.mostrarTrazo(celdas());
	}

	@Override
	public void confirmar(Consumer<Direccion> mover, Runnable deshacer) {
		direcciones().forEach(mover);
	}

	List<Celda> celdas() {
		return List.copyOf(trazo);
	}

	List<Direccion> direcciones() {
		List<Direccion> direcciones = new ArrayList<>();
		Celda anterior = origen.actual();
		for (Celda celda : trazo) {
			direcciones.add(direccionEntre(anterior, celda));
			anterior = celda;
		}
		return List.copyOf(direcciones);
	}

	private boolean vaciarTrazo() {
		boolean cambio = !trazo.isEmpty();
		trazo.clear();
		return cambio;
	}

	private boolean truncarEn(int cantidad) {
		if (cantidad >= trazo.size()) {
			return false;
		}
		trazo.subList(cantidad, trazo.size()).clear();
		return true;
	}

	private boolean avanzarHacia(Celda destino) {
		if (!mismaLinea(punta(), destino)) {
			return false;
		}
		int cantidadInicial = trazo.size();
		while (!punta().equals(destino)) {
			Celda siguiente = pasoHacia(punta(), destino);
			if (!esPasoValido(siguiente)) {
				break;
			}
			trazo.add(siguiente);
		}
		return trazo.size() > cantidadInicial;
	}

	private Celda punta() {
		return trazo.isEmpty() ? origen.actual() : trazo.get(trazo.size() - 1);
	}

	private static boolean mismaLinea(Celda a, Celda b) {
		return a.fila() == b.fila() || a.columna() == b.columna();
	}

	private static Celda pasoHacia(Celda desde, Celda hasta) {
		return new Celda(
				desde.fila() + Integer.signum(hasta.fila() - desde.fila()),
				desde.columna() + Integer.signum(hasta.columna() - desde.columna()));
	}

	private boolean esPasoValido(Celda siguiente) {
		List<Celda> candidato = new ArrayList<>(origen.recorrido());
		candidato.addAll(trazo);
		candidato.add(siguiente);
		try {
			new EstadoJuego(origen.nivel(), candidato);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static Direccion direccionEntre(Celda desde, Celda hasta) {
		for (Direccion direccion : Direccion.values()) {
			if (desde.mover(direccion).equals(hasta)) {
				return direccion;
			}
		}
		throw new IllegalStateException(DIRECCION_INVALIDA);
	}
}
