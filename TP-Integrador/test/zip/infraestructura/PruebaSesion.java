package zip.infraestructura;

import static zip.SoportePruebas.verificar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.swing.SwingUtilities;
import zip.aplicacion.ActualizacionSesion;
import zip.dominio.Direccion;
import zip.dominio.EstadoJuego;
import zip.dominio.Nivel;
import zip.dominio.GeneradorNivel;

public final class PruebaSesion {
	private static final int SEGUNDOS_ESPERA = 3;
	private static final int TRES_CELDAS = 3;
	private static final long SEMILLA = 42;
	private static final int CAPACIDAD_PRUEBA = 8;

	private PruebaSesion() {
	}

	public static void main(String[] argumentos) throws Exception {
		accionesOrdenadas();
		actualizacionesInmutables();
		saturacionRechazaExplicitamente();
		cierreSuprimeEntrega();
		falloYRecuperacion();
		trabajoLentoMantieneEdtDisponible();
		cierreInterrumpeTrabajoActivo();
		verificar(sinTrabajadores(), "Todos los trabajadores de sesión deben finalizar");
		System.out.println("PruebaSesion: 7 escenarios correctos");
	}

	private static Nivel nivel(int puntos) {
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(Nivel.CANTIDAD_CELDAS, 0));
		numeros.set(0, 1);
		numeros.set(Nivel.CANTIDAD_CELDAS - Nivel.TAMANO, 2);
		return new Nivel(numeros);
	}

	private static void accionesOrdenadas() throws Exception {
		try (Escenario escenario = escenario(PruebaSesion::nivel)) {
			verificar(escenario.sesion().iniciar(2), "Inicio aceptado");
			escenario.tomar();
			verificar(escenario.sesion().mover(Direccion.DERECHA), "Primer movimiento aceptado");
			verificar(escenario.sesion().mover(Direccion.ABAJO), "Segundo movimiento aceptado");
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 2, "Primer movimiento");
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == TRES_CELDAS, "FIFO");
			verificar(escenario.sesion().deshacer(), "Deshacer aceptado");
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 2, "Deshacer aplicado");
			verificar(escenario.sesion().reiniciar(), "Reinicio aceptado");
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 1, "Reinicio aplicado");
		}
	}

	private static void actualizacionesInmutables() throws Exception {
		try (Escenario escenario = escenario(PruebaSesion::nivel)) {
			escenario.sesion().iniciar(2);
			EstadoJuego anterior = escenario.tomar().estadoJuego().orElseThrow();
			escenario.sesion().mover(Direccion.DERECHA);
			escenario.tomar();
			verificar(anterior.visitadas() == 1, "El estado anterior no puede cambiar");
			escenario.sesion().mover(Direccion.IZQUIERDA);
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 2,
					"Un movimiento inválido no cambia el estado");
		}
	}

	private static void saturacionRechazaExplicitamente() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1), liberacion = new CountDownLatch(1);
		try (Escenario escenario = escenarioBloqueado(iniciado, liberacion)) {
			escenario.sesion().iniciar(2);
			esperar(iniciado);
			verificar(escenario.sesion().mover(Direccion.DERECHA), "Se acepta un movimiento encolado");
			verificar(!escenario.sesion().mover(Direccion.ABAJO), "La cola llena rechaza sin descartar");
			liberacion.countDown();
			escenario.tomar();
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 2,
					"El movimiento aceptado se conserva");
		} finally {
			liberacion.countDown();
		}
	}

	private static Escenario escenarioBloqueado(CountDownLatch iniciado, CountDownLatch liberacion) {
		return escenario(puntos -> {
			iniciado.countDown();
			esperarTrabajador(liberacion);
			return nivel(puntos);
		}, 1);
	}

	private static void cierreSuprimeEntrega() throws Exception {
		Queue<Runnable> entregas = new ConcurrentLinkedQueue<>();
		CountDownLatch programado = new CountDownLatch(1);
		Escenario escenario = escenarioDiferido(entregas, programado);
		try {
			escenario.sesion().iniciar(2);
			esperar(programado);
			escenario.close();
			entregas.remove().run();
			verificar(escenario.actualizaciones().isEmpty(), "Una sesión cerrada no actualiza otra pantalla");
			verificar(!escenario.sesion().mover(Direccion.DERECHA), "Una sesión cerrada rechaza entradas");
		} finally {
			escenario.close();
		}
	}

	private static Escenario escenarioDiferido(Queue<Runnable> entregas, CountDownLatch programado) {
		BlockingQueue<ActualizacionSesion> actualizaciones = new LinkedBlockingQueue<>();
		Consumer<Runnable> despachar = respuesta -> {
			entregas.add(respuesta);
			programado.countDown();
		};
		return new Escenario(
				new SesionJuegoLocal(PruebaSesion::nivel, actualizaciones::add, despachar, 1), actualizaciones);
	}

	private static void falloYRecuperacion() throws Exception {
		try (Escenario escenario = escenario(new GeneradorNivel(SEMILLA)::generar)) {
			escenario.sesion().iniciar(1);
			ActualizacionSesion error = escenario.tomar();
			verificar(error.fallo() && error.estadoJuego().isEmpty(), "El fallo de generación es explícito");
			verificar(!error.mensaje().isBlank(), "El fallo tiene un mensaje visible");
			escenario.sesion().iniciar(2);
			verificar(!escenario.tomar().fallo(), "El trabajador sobrevive a una acción fallida");
		}
	}

	private static void trabajoLentoMantieneEdtDisponible() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1);
		CountDownLatch liberacion = new CountDownLatch(1);
		try (Escenario escenario = escenarioBloqueado(iniciado, liberacion)) {
			SwingUtilities.invokeAndWait(() -> verificar(escenario.sesion().iniciar(2), "El EDT envía la acción"));
			esperar(iniciado);
			CountDownLatch pulso = new CountDownLatch(1);
			SwingUtilities.invokeLater(pulso::countDown);
			verificar(pulso.await(1, TimeUnit.SECONDS), "El EDT debe responder mientras el trabajador está bloqueado");
			liberacion.countDown();
			verificar(!escenario.tomar().fallo(), "El trabajo demorado finaliza");
		} finally {
			liberacion.countDown();
		}
	}

	private static Escenario escenario(IntFunction<Nivel> niveles) {
		return escenario(niveles, CAPACIDAD_PRUEBA);
	}

	private static void cierreInterrumpeTrabajoActivo() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1);
		AtomicBoolean interrumpido = new AtomicBoolean();
		try (Escenario escenario = escenario(puntos -> nivelInterrumpido(iniciado, interrumpido))) {
			escenario.sesion().iniciar(2);
			esperar(iniciado);
			escenario.sesion().mover(Direccion.DERECHA);
			escenario.sesion().close();
			verificar(escenario.sesion().esperarFinalizacion(SEGUNDOS_ESPERA, TimeUnit.SECONDS),
					"El cierre tiene un tiempo máximo");
			verificar(interrumpido.get(), "El trabajo activo recibe la interrupción");
			verificar(escenario.actualizaciones().isEmpty(),
					"No quedan resultados activos ni encolados tras el cierre");
		}
	}

	private static Nivel nivelInterrumpido(CountDownLatch iniciado, AtomicBoolean interrumpido) {
		iniciado.countDown();
		try {
			new CountDownLatch(1).await(2 * SEGUNDOS_ESPERA, TimeUnit.SECONDS);
			throw new AssertionError("Solo una interrupción puede liberar este trabajador");
		} catch (InterruptedException excepcion) {
			interrumpido.set(true);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupción inyectada durante el cierre", excepcion);
		}
	}

	private static Escenario escenario(IntFunction<Nivel> niveles, int capacidad) {
		BlockingQueue<ActualizacionSesion> actualizaciones = new LinkedBlockingQueue<>();
		return new Escenario(
				new SesionJuegoLocal(niveles, actualizaciones::add, Runnable::run, capacidad), actualizaciones);
	}

	private static void esperar(CountDownLatch senal) throws InterruptedException {
		verificar(senal.await(SEGUNDOS_ESPERA, TimeUnit.SECONDS), "Se agotó la espera de la señal de prueba");
	}

	private static void esperarTrabajador(CountDownLatch senal) {
		try {
			esperar(senal);
		} catch (InterruptedException excepcion) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Trabajador de prueba interrumpido", excepcion);
		}
	}

	private static boolean sinTrabajadores() {
		return Thread.getAllStackTraces().keySet().stream()
				.noneMatch(hilo -> hilo.isAlive() && hilo.getName().equals("zip-sesion"));
	}

	private record Escenario(SesionJuegoLocal sesion, BlockingQueue<ActualizacionSesion> actualizaciones)
			implements AutoCloseable {
		ActualizacionSesion tomar() throws InterruptedException {
			ActualizacionSesion actualizacion = actualizaciones.poll(SEGUNDOS_ESPERA, TimeUnit.SECONDS);
			verificar(actualizacion != null, "Se agotó la espera del resultado de sesión");
			return actualizacion;
		}

		@Override
		public void close() {
			sesion.close();
			try {
				verificar(sesion.esperarFinalizacion(SEGUNDOS_ESPERA, TimeUnit.SECONDS),
						"El trabajador debe finalizar");
			} catch (InterruptedException excepcion) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupción durante la limpieza de la prueba", excepcion);
			}
		}
	}
}
