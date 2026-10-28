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
import java.util.function.Function;
import javax.swing.SwingUtilities;
import zip.aplicacion.ActualizacionSesion;
import zip.dominio.Direccion;
import zip.dominio.Dificultad;
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
		verificar(sinTrabajadores(), "Todos los trabajadores de sesión deben finalizar.");
		System.out.println("PruebaSesion: 7 escenarios superados.");
	}

	private static Nivel nivel(Dificultad dificultad) {
		List<Integer> numeros = new ArrayList<>(Collections.nCopies(dificultad.cantidadCeldas(), 0));
		numeros.set(0, 1);
		numeros.set(dificultad.cantidadCeldas() - dificultad.tamano(), 2);
		return new Nivel(dificultad.tamano(), numeros);
	}

	private static void accionesOrdenadas() throws Exception {
		try (Escenario escenario = escenario(PruebaSesion::nivel)) {
			verificar(escenario.sesion().iniciar(Dificultad.MEDIA), "El inicio se acepta.");
			escenario.tomar();
			verificar(escenario.sesion().mover(Direccion.DERECHA), "El primer movimiento se acepta.");
			verificar(escenario.sesion().mover(Direccion.ABAJO), "El segundo movimiento se acepta.");
			verificarMovimientosOrdenados(escenario);
			verificar(escenario.sesion().deshacer(), "Deshacer se acepta.");
			verificarVisitadas(escenario, 2, "Deshacer se aplica.");
			verificar(escenario.sesion().reiniciar(), "Reiniciar se acepta.");
			verificarVisitadas(escenario, 1, "Reiniciar se aplica.");
		}
	}

	private static void verificarMovimientosOrdenados(Escenario escenario) throws Exception {
		verificarVisitadas(escenario, 2, "Se aplica el primer movimiento.");
		verificarVisitadas(escenario, TRES_CELDAS, "Las acciones respetan el orden FIFO.");
	}

	private static void verificarVisitadas(Escenario escenario, int esperadas,
			String mensaje) throws Exception {
		verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == esperadas, mensaje);
	}

	private static void actualizacionesInmutables() throws Exception {
		try (Escenario escenario = escenario(PruebaSesion::nivel)) {
			escenario.sesion().iniciar(Dificultad.MEDIA);
			EstadoJuego anterior = escenario.tomar().estadoJuego().orElseThrow();
			escenario.sesion().mover(Direccion.DERECHA);
			escenario.tomar();
			verificar(anterior.visitadas() == 1, "El estado anterior no puede cambiar.");
			escenario.sesion().mover(Direccion.IZQUIERDA);
			verificar(escenario.tomar().estadoJuego().orElseThrow().visitadas() == 2,
					"Un movimiento inválido no cambia el estado.");
		}
	}

	private static void saturacionRechazaExplicitamente() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1);
		CountDownLatch liberacion = new CountDownLatch(1);
		try (Escenario escenario = escenarioBloqueado(iniciado, liberacion)) {
			escenario.sesion().iniciar(Dificultad.MEDIA);
			esperar(iniciado);
			verificar(escenario.sesion().mover(Direccion.DERECHA),
					"Se acepta un movimiento encolado.");
			verificar(!escenario.sesion().mover(Direccion.ABAJO),
					"La cola llena rechaza sin descartar lo aceptado.");
			liberarYVerificarMovimiento(escenario, liberacion);
		} finally {
			liberacion.countDown();
		}
	}

	private static void liberarYVerificarMovimiento(Escenario escenario,
			CountDownLatch liberacion) throws Exception {
		liberacion.countDown();
		escenario.tomar();
		verificarVisitadas(escenario, 2, "El movimiento aceptado se conserva.");
	}

	private static Escenario escenarioBloqueado(CountDownLatch iniciado, CountDownLatch liberacion) {
		return escenario(dificultad -> {
			iniciado.countDown();
			esperarTrabajador(liberacion);
			return nivel(dificultad);
		}, 1);
	}

	private static void cierreSuprimeEntrega() throws Exception {
		Queue<Runnable> entregas = new ConcurrentLinkedQueue<>();
		CountDownLatch programado = new CountDownLatch(1);
		Escenario escenario = escenarioDiferido(entregas, programado);
		try {
			escenario.sesion().iniciar(Dificultad.MEDIA);
			esperar(programado);
			escenario.close();
			entregas.remove().run();
			verificarSesionCerrada(escenario);
		} finally {
			escenario.close();
		}
	}

	private static void verificarSesionCerrada(Escenario escenario) {
		verificar(escenario.actualizaciones().isEmpty(),
				"Una sesión cerrada no puede actualizar otra pantalla.");
		verificar(!escenario.sesion().mover(Direccion.DERECHA),
				"Una sesión cerrada rechaza entradas.");
	}

	private static Escenario escenarioDiferido(Queue<Runnable> entregas, CountDownLatch programado) {
		BlockingQueue<ActualizacionSesion> actualizaciones = new LinkedBlockingQueue<>();
		Consumer<Runnable> despachar = devolucion -> {
			entregas.add(devolucion);
			programado.countDown();
		};
		return new Escenario(new SesionJuegoLocal(
				PruebaSesion::nivel, actualizaciones::add, despachar, 1), actualizaciones);
	}

	private static void falloYRecuperacion() throws Exception {
		GeneradorNivel generador = new GeneradorNivel(SEMILLA);
		try (Escenario escenario = escenario(dificultad -> generarOFallar(generador, dificultad))) {
			escenario.sesion().iniciar(Dificultad.FACIL);
			ActualizacionSesion error = escenario.tomar();
			verificar(error.fallo() && error.estadoJuego().isEmpty(),
					"El fallo de generación es explícito.");
			verificar(!error.mensaje().isBlank(), "El fallo tiene un mensaje visible.");
			escenario.sesion().iniciar(Dificultad.MEDIA);
			verificar(!escenario.tomar().fallo(), "El trabajador sobrevive a una orden fallida.");
		}
	}

	private static Nivel generarOFallar(GeneradorNivel generador, Dificultad dificultad) {
		if (dificultad == Dificultad.FACIL) {
			throw new IllegalArgumentException("Fallo de generación inyectado.");
		}
		return generador.generar(dificultad);
	}

	private static void trabajoLentoMantieneEdtDisponible() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1);
		CountDownLatch liberacion = new CountDownLatch(1);
		try (Escenario escenario = escenarioBloqueado(iniciado, liberacion)) {
			SwingUtilities.invokeAndWait(() ->
				verificar(escenario.sesion().iniciar(Dificultad.MEDIA), "El EDT envía la acción."));
			esperar(iniciado);
			verificarPulsoEdt();
			liberacion.countDown();
			verificar(!escenario.tomar().fallo(), "El trabajo demorado se completa.");
		} finally {
			liberacion.countDown();
		}
	}

	private static void verificarPulsoEdt() throws InterruptedException {
		CountDownLatch pulso = new CountDownLatch(1);
		SwingUtilities.invokeLater(pulso::countDown);
		verificar(pulso.await(1, TimeUnit.SECONDS),
				"El EDT debe responder mientras el trabajador está bloqueado.");
	}

	private static Escenario escenario(Function<Dificultad, Nivel> niveles) {
		return escenario(niveles, CAPACIDAD_PRUEBA);
	}

	private static void cierreInterrumpeTrabajoActivo() throws Exception {
		CountDownLatch iniciado = new CountDownLatch(1);
		AtomicBoolean interrumpido = new AtomicBoolean();
		try (Escenario escenario = escenario(dificultad -> nivelInterrumpido(iniciado, interrumpido))) {
			escenario.sesion().iniciar(Dificultad.MEDIA);
			esperar(iniciado);
			escenario.sesion().mover(Direccion.DERECHA);
			escenario.sesion().close();
			verificar(escenario.sesion().esperarFinalizacion(SEGUNDOS_ESPERA, TimeUnit.SECONDS),
					"El cierre tiene espera acotada.");
			verificar(interrumpido.get(), "El trabajo activo recibe la interrupción.");
			verificar(escenario.actualizaciones().isEmpty(),
					"No se entregan resultados activos ni encolados después del cierre.");
		}
	}

	private static Nivel nivelInterrumpido(CountDownLatch iniciado, AtomicBoolean interrumpido) {
		iniciado.countDown();
		try {
			new CountDownLatch(1).await(2 * SEGUNDOS_ESPERA, TimeUnit.SECONDS);
			throw new AssertionError("Solo una interrupción puede liberar este trabajador.");
		} catch (InterruptedException excepcion) {
			interrumpido.set(true);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupción inyectada durante el cierre.", excepcion);
		}
	}

	private static Escenario escenario(Function<Dificultad, Nivel> niveles, int capacidad) {
		BlockingQueue<ActualizacionSesion> actualizaciones = new LinkedBlockingQueue<>();
		return new Escenario(new SesionJuegoLocal(
				niveles, actualizaciones::add, Runnable::run, capacidad), actualizaciones);
	}

	private static void esperar(CountDownLatch senal) throws InterruptedException {
		verificar(senal.await(SEGUNDOS_ESPERA, TimeUnit.SECONDS),
				"Se agotó el tiempo de espera de la señal de prueba.");
	}

	private static void esperarTrabajador(CountDownLatch senal) {
		try {
			esperar(senal);
		} catch (InterruptedException excepcion) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Trabajador de prueba interrumpido.", excepcion);
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
			verificar(actualizacion != null, "Se agotó la espera del resultado de la sesión.");
			return actualizacion;
		}

		@Override
		public void close() {
			sesion.close();
			try {
				verificar(sesion.esperarFinalizacion(SEGUNDOS_ESPERA, TimeUnit.SECONDS),
						"El trabajador debe finalizar.");
			} catch (InterruptedException excepcion) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupción durante la limpieza de la prueba.", excepcion);
			}
		}
	}
}
