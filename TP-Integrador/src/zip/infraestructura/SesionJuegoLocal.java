package zip.infraestructura;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Logger;
import zip.aplicacion.SesionJuego;
import zip.aplicacion.ActualizacionSesion;
import zip.dominio.Direccion;
import zip.dominio.MotorJuego;
import zip.dominio.Nivel;

public final class SesionJuegoLocal implements SesionJuego {
	private static final Logger REGISTRO = Logger.getLogger(SesionJuegoLocal.class.getName());
	private static final int CAPACIDAD_COLA = 64;
	private final ThreadPoolExecutor trabajador;
	private final AtomicBoolean cerrada = new AtomicBoolean();
	private final IntFunction<Nivel> niveles;
	private final Consumer<ActualizacionSesion> escucha;
	private final Consumer<Runnable> despachador;
	private Optional<MotorJuego> motor = Optional.empty();

	public SesionJuegoLocal(IntFunction<Nivel> niveles, Consumer<ActualizacionSesion> escucha,
			Consumer<Runnable> despachador) {
		this(niveles, escucha, despachador, CAPACIDAD_COLA);
	}

	SesionJuegoLocal(IntFunction<Nivel> niveles, Consumer<ActualizacionSesion> escucha,
			Consumer<Runnable> despachador, int capacidad) {
		this.niveles = Objects.requireNonNull(niveles, "niveles");
		this.escucha = Objects.requireNonNull(escucha, "escucha");
		this.despachador = Objects.requireNonNull(despachador, "despachador");
		trabajador = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<>(capacidad), tarea -> new Thread(tarea, "zip-sesion"),
				new ThreadPoolExecutor.AbortPolicy());
	}

	@Override
	public boolean iniciar(int puntos) {
		return enviar(() -> {
			motor = Optional.of(new MotorJuego(niveles.apply(puntos)));
			return "Conectá los números en orden y recorré todas las celdas.";
		});
	}

	@Override
	public boolean mover(Direccion direccion) {
		Objects.requireNonNull(direccion, "direccion");
		return enviar(() -> juego().mover(direccion).mensaje());
	}

	@Override
	public boolean deshacer() {
		return enviar(() -> juego().deshacer() ? "Último paso deshecho."
				: "No se puede deshacer: estás en el inicio o la partida terminó.");
	}

	@Override
	public boolean reiniciar() {
		return enviar(() -> {
			juego().reiniciar();
			return "Mismo tablero, nuevo intento.";
		});
	}

	private MotorJuego juego() {
		return motor.orElseThrow(() -> new IllegalStateException("Primero hay que iniciar una partida."));
	}

	private boolean enviar(Supplier<String> accion) {
		if (cerrada.get()) {
			return rechazar("La sesión está cerrada.");
		}
		try {
			trabajador.execute(() -> ejecutarAccion(accion));
			return true;
		} catch (RejectedExecutionException excepcion) {
			return rechazar("No se aceptó la acción: cola completa o sesión cerrada.");
		}
	}

	private boolean rechazar(String mensaje) {
		REGISTRO.warning(mensaje);
		return false;
	}

	private void ejecutarAccion(Supplier<String> accion) {
		if (!cerrada.get()) {
			ActualizacionSesion actualizacion = procesar(accion);
			despachador.accept(() -> {
				if (!cerrada.get()) {
					escucha.accept(actualizacion);
				}
			});
		}
	}

	private ActualizacionSesion procesar(Supplier<String> accion) {
		try {
			return actualizacion(accion.get(), false);
		} catch (RuntimeException excepcion) {
			REGISTRO.log(java.util.logging.Level.SEVERE, "Falló una acción de la sesión.", excepcion);
			return actualizacion("No se pudo completar la acción. Volvé a intentar o reiniciá.", true);
		}
	}

	private ActualizacionSesion actualizacion(String mensaje, boolean fallo) {
		return new ActualizacionSesion(motor.map(MotorJuego::estadoJuego), mensaje, fallo);
	}

	@Override
	public void close() {
		if (cerrada.compareAndSet(false, true)) {
			trabajador.shutdownNow();
		}
	}

	public boolean esperarFinalizacion(long tiempoEspera, TimeUnit unidad) throws InterruptedException {
		return trabajador.awaitTermination(tiempoEspera, unidad);
	}
}
