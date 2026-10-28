package zip.dominio;

import static zip.SoportePruebas.verificar;
import static zip.SoportePruebas.esperarExcepcion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public final class PruebaDominio {
	private static final long SEMILLA = 2026L;
	private static final int CANTIDAD_PLANTILLAS = 2;
	private static final int CANTIDAD_ROTACIONES = 4;
	private static final int TERCER_NUMERO = 3;
	private static final int TAMANO_FACIL = 5;
	private static final int TAMANO_MEDIO = 6;
	private static final int TAMANO_DIFICIL = 8;
	private static final int PUNTOS_FACIL = 5;
	private static final int PUNTOS_MEDIA = 6;
	private static final int PUNTOS_DIFICIL = 8;
	private static final int TAMANO = TAMANO_MEDIO;
	private static final int CANTIDAD_CELDAS = TAMANO * TAMANO;
	private static final int ULTIMO_INDICE = CANTIDAD_CELDAS - 1;
	private static final int ULTIMA_FILA = TAMANO - 1;
	private static final String ALTERNATIVA =
		"0,1,2,3,4,5,11,17,23,29,35,34,33,32,31,25,26,27,28,22,21,20,19,13,14,15,16,10,9,8,7,6,12,18,24,30";
	private int verificaciones;

	private PruebaDominio() {
	}

	public static void main(String[] argumentos) {
		new PruebaDominio().ejecutar();
	}

	private void ejecutar() {
		pruebasCelda();
		pruebasDificultad();
		pruebasNivel();
		pruebasGenerador();
		pruebasEstado();
		pruebasMovimiento();
		pruebasHistorial();
		pruebasVictoria();
		System.out.println("Dominio Zip: " + verificaciones + " verificaciones correctas.");
	}

	private void pruebasDificultad() {
		verificarDificultad(Dificultad.FACIL, TAMANO_FACIL, PUNTOS_FACIL);
		verificarDificultad(Dificultad.MEDIA, TAMANO_MEDIO, PUNTOS_MEDIA);
		verificarDificultad(Dificultad.DIFICIL, TAMANO_DIFICIL, PUNTOS_DIFICIL);
	}

	private void verificarDificultad(Dificultad dificultad, int tamano, int puntos) {
		comprobar(dificultad.tamano() == tamano, "Tamaño configurado para " + dificultad);
		comprobar(dificultad.puntos() == puntos, "Puntos configurados para " + dificultad);
		comprobar(dificultad.cantidadCeldas() == tamano * tamano, "Cantidad de celdas derivada.");
	}

	private void pruebasCelda() {
		Celda origen = new Celda(1, 1);
		comprobar(origen.mover(Direccion.ARRIBA).equals(new Celda(0, 1)), "Arriba resta una fila.");
		comprobar(origen.mover(Direccion.DERECHA).equals(new Celda(1, 2)), "Derecha suma una columna.");
		comprobar(origen.mover(Direccion.ABAJO).equals(new Celda(2, 1)), "Abajo suma una fila.");
		comprobar(origen.mover(Direccion.IZQUIERDA).equals(new Celda(1, 0)), "Izquierda resta una columna.");
		comprobar(origen.equals(new Celda(1, 1)), "Celda tiene igualdad por valor.");
		for (Direccion direccion : Direccion.values()) {
			comprobar(Math.abs(direccion.deltaFila()) + Math.abs(direccion.deltaColumna()) == 1, "Paso cardinal.");
		}
		rechaza(NullPointerException.class, () -> origen.mover(null));
	}

	private void pruebasNivel() {
		Nivel nivel = nivel(serpiente(), 0, ULTIMO_INDICE);
		comprobar(nivel.puntos() == Nivel.PUNTOS_MINIMOS, "Cantidad mínima de puntos.");
		comprobar(nivel.tamano() == TAMANO && nivel.cantidadCeldas() == CANTIDAD_CELDAS,
				"Dimensiones del nivel.");
		comprobar(nivel.inicio().equals(new Celda(0, 0)), "Inicio en el número 1.");
		comprobar(nivel.numeroEn(new Celda(ULTIMA_FILA, 0)) == 2, "Números en orden por filas.");
		comprobar(nivel.numeroEn(new Celda(0, 1)) == 0, "Las celdas libres son cero.");
		comprobar(nivel.contiene(new Celda(ULTIMA_FILA, ULTIMA_FILA)),
				"La esquina inferior pertenece.");
		limitesNivel(nivel);
		nivelesInvalidos();
		numerosInvalidos();
		inmutabilidadNivel(nivel);
	}

	private void limitesNivel(Nivel nivel) {
		for (Celda celda : List.of(new Celda(-1, 0), new Celda(0, -1),
			new Celda(nivel.tamano(), 0), new Celda(0, nivel.tamano()))) {
			comprobar(!nivel.contiene(celda), "Una celda exterior no pertenece.");
			rechaza(IllegalArgumentException.class, () -> nivel.numeroEn(celda));
		}
		rechaza(NullPointerException.class, () -> nivel.contiene(null));
		rechaza(NullPointerException.class, () -> nivel.numeroEn(null));
	}

	private void nivelesInvalidos() {
		rechaza(NullPointerException.class, () -> new Nivel(TAMANO, null));
		rechaza(IllegalArgumentException.class, () -> new Nivel(1, List.of()));
		cantidadesInvalidas();
		rechaza(IllegalArgumentException.class, () -> new Nivel(TAMANO, numerosVacios()));
		List<Integer> unico = numerosVacios();
		unico.set(0, 1);
		rechaza(IllegalArgumentException.class, () -> new Nivel(TAMANO, unico));
		List<Integer> entradaNula = numerosVacios();
		entradaNula.set(0, null);
		rechaza(NullPointerException.class, () -> new Nivel(TAMANO, entradaNula));
	}

	private void cantidadesInvalidas() {
		rechaza(IllegalArgumentException.class, () -> new Nivel(TAMANO, List.of()));
		rechaza(IllegalArgumentException.class,
				() -> new Nivel(TAMANO, Collections.nCopies(ULTIMO_INDICE, 0)));
		rechaza(IllegalArgumentException.class,
				() -> new Nivel(TAMANO, Collections.nCopies(CANTIDAD_CELDAS + 1, 0)));
	}

	private void numerosInvalidos() {
		List<Integer> origen = new ArrayList<>(nivel(serpiente(), 0, ULTIMO_INDICE).numeros());
		for (int invalido : new int[] {-1, CANTIDAD_CELDAS + 1, 1, TERCER_NUMERO}) {
			List<Integer> cambiado = new ArrayList<>(origen);
			cambiado.set(TAMANO * ULTIMA_FILA, invalido);
			rechaza(IllegalArgumentException.class, () -> new Nivel(TAMANO, cambiado));
		}
	}

	private void inmutabilidadNivel(Nivel original) {
		List<Integer> origen = new ArrayList<>(original.numeros());
		Nivel copia = new Nivel(original.tamano(), origen);
		origen.set(0, 0);
		comprobar(copia.numeroEn(copia.inicio()) == 1, "El nivel copia los números de entrada.");
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().set(0, 0));
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().add(0));
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().clear());
		comprobar(copia.numeros().equals(original.numeros()), "El nivel mantiene su configuración.");
	}

	private void pruebasGenerador() {
		GeneradorNivel primero = new GeneradorNivel(SEMILLA);
		GeneradorNivel segundo = new GeneradorNivel(SEMILLA);
		for (Dificultad dificultad : Dificultad.values()) {
			verificarDificultadGenerada(primero, segundo, dificultad);
		}
		limitesGenerador(primero);
		resolverGenerado(new GeneradorNivel().generar(Dificultad.MEDIA),
				todosLosRecorridos(TAMANO));
	}

	private void verificarDificultadGenerada(GeneradorNivel primero,
			GeneradorNivel segundo, Dificultad dificultad) {
		List<List<Celda>> recorridos = todosLosRecorridos(dificultad.tamano());
		Nivel generado = primero.generar(dificultad);
		comprobar(generado.tamano() == dificultad.tamano(), "El generador respeta el tamaño.");
		comprobar(generado.puntos() == dificultad.puntos(), "El generador respeta los puntos.");
		comprobar(generado.numeros().equals(segundo.generar(dificultad).numeros()),
				"Semilla reproducible.");
		resolverGenerado(generado, recorridos);
	}

	private void limitesGenerador(GeneradorNivel generador) {
		rechaza(NullPointerException.class, () -> generador.generar(null));
		rechaza(IllegalArgumentException.class,
				() -> GeneradorNivel.recorrido(1, 0, 0, false, false));
		rechaza(IllegalArgumentException.class,
				() -> GeneradorNivel.recorrido(TAMANO, -1, 0, false, false));
		rechaza(IllegalArgumentException.class,
				() -> GeneradorNivel.recorrido(TAMANO, CANTIDAD_PLANTILLAS, 0, false, false));
		rechaza(IllegalArgumentException.class,
				() -> GeneradorNivel.recorrido(TAMANO, 0, -1, false, false));
		rechaza(IllegalArgumentException.class,
				() -> GeneradorNivel.recorrido(TAMANO, 0, CANTIDAD_ROTACIONES, false, false));
	}

	private List<List<Celda>> todosLosRecorridos(int tamano) {
		List<List<Celda>> recorridos = new ArrayList<>();
		for (int plantilla = 0; plantilla < CANTIDAD_PLANTILLAS; plantilla++) {
			for (int rotacion = 0; rotacion < CANTIDAD_ROTACIONES; rotacion++) {
				agregarVariantes(recorridos, tamano, plantilla, rotacion);
			}
		}
		comprobar(!new HashSet<>(recorridos.get(0)).isEmpty(), "Existen rutas de construcción.");
		comprobar(!recorridos.get(0).equals(recorridos.get(CANTIDAD_ROTACIONES * 2 * 2)),
				"Las dos bases son distintas.");
		return recorridos;
	}

	private void agregarVariantes(List<List<Celda>> recorridos, int tamano,
			int plantilla, int rotacion) {
		for (boolean reflejado : new boolean[] {false, true}) {
			for (boolean invertido : new boolean[] {false, true}) {
				List<Celda> recorrido = GeneradorNivel.recorrido(
						tamano, plantilla, rotacion, reflejado, invertido);
				validarRuta(recorrido, tamano);
				MotorJuego motor = new MotorJuego(
						nivel(tamano, recorrido, 0, 2, recorrido.size() - 1));
				completar(motor, recorrido);
				recorridos.add(recorrido);
			}
		}
	}

	private void validarRuta(List<Celda> recorrido, int tamano) {
		comprobar(recorrido.size() == tamano * tamano, "La ruta cubre el tablero.");
		comprobar(new HashSet<>(recorrido).size() == tamano * tamano,
				"La ruta no repite celdas.");
		for (Celda celda : recorrido) {
			comprobar(celda.fila() >= 0 && celda.fila() < tamano, "Fila válida de la ruta.");
			comprobar(celda.columna() >= 0 && celda.columna() < tamano,
					"Columna válida de la ruta.");
		}
		for (int indice = 1; indice < recorrido.size(); indice++) {
			direccion(recorrido.get(indice - 1), recorrido.get(indice));
		}
		rechaza(UnsupportedOperationException.class, () -> recorrido.clear());
	}

	private void resolverGenerado(Nivel nivel, List<List<Celda>> recorridos) {
		List<Celda> solucion = recorridos.stream().filter(recorrido -> compatible(nivel, recorrido)).findFirst()
			.orElseThrow(() -> new AssertionError("El nivel generado no conserva una solución."));
		comprobar(nivel.numeroEn(solucion.get(0)) == 1,
				"El primer punto numerado está en el inicio.");
		comprobar(nivel.numeroEn(solucion.get(nivel.cantidadCeldas() - 1)) == nivel.puntos(),
				"El último está en el final.");
		completar(new MotorJuego(nivel), solucion);
	}

	private static boolean compatible(Nivel nivel, List<Celda> recorrido) {
		int siguiente = 1;
		for (Celda celda : recorrido) {
			int numero = nivel.numeroEn(celda);
			if (numero != 0 && numero != siguiente++) {
				return false;
			}
		}
		return nivel.numeroEn(recorrido.get(0)) == 1
				&& nivel.numeroEn(recorrido.get(nivel.cantidadCeldas() - 1)) == nivel.puntos();
	}

	private void pruebasEstado() {
		Nivel nivel = nivel(serpiente(), 0, 2, ULTIMO_INDICE);
		List<Celda> origen = new ArrayList<>(List.of(nivel.inicio()));
		EstadoJuego estadoJuego = new EstadoJuego(nivel, origen);
		origen.add(new Celda(0, 1));
		comprobar(estadoJuego.visitadas() == 1, "El estadoJuego copia el recorrido.");
		comprobar(estadoJuego.nivel() == nivel, "El nivel compartido es inmutable.");
		comprobar(estadoJuego.actual().equals(nivel.inicio()), "Celda inicial del estadoJuego.");
		comprobar(estadoJuego.proximoNumero() == 2 && !estadoJuego.gano(), "Estado inicial derivado.");
		rechaza(UnsupportedOperationException.class, () -> estadoJuego.recorrido().add(new Celda(0, 1)));
		rechaza(UnsupportedOperationException.class, () -> estadoJuego.nivel().numeros().set(0, 0));
		estadosInvalidos(nivel);
	}

	private void estadosInvalidos(Nivel nivel) {
		rechaza(NullPointerException.class, () -> new EstadoJuego(null, List.of(nivel.inicio())));
		rechaza(NullPointerException.class, () -> new EstadoJuego(nivel, null));
		rechaza(NullPointerException.class,
				() -> new EstadoJuego(nivel, Arrays.asList(nivel.inicio(), null)));
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivel, List.of()));
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivel, List.of(new Celda(0, 1))));
		rechaza(IllegalArgumentException.class,
				() -> new EstadoJuego(nivel, List.of(nivel.inicio(), nivel.inicio())));
		rechaza(IllegalArgumentException.class,
				() -> new EstadoJuego(nivel, List.of(nivel.inicio(), new Celda(1, 1))));
		rechaza(IllegalArgumentException.class,
				() -> new EstadoJuego(nivel, List.of(nivel.inicio(), new Celda(-1, 0))));
	}

	private void pruebasMovimiento() {
		rechaza(NullPointerException.class, () -> new MotorJuego(null));
		MotorJuego motor = new MotorJuego(nivel(serpiente(), 0, 2, ULTIMO_INDICE));
		rechaza(NullPointerException.class, () -> motor.mover(null));
		bloqueado(motor, Direccion.ARRIBA, ResultadoMovimiento.FUERA_DEL_TABLERO);
		bloqueado(motor, Direccion.IZQUIERDA, ResultadoMovimiento.FUERA_DEL_TABLERO);
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO, "Se avanza a una celda libre.");
		bloqueado(motor, Direccion.IZQUIERDA, ResultadoMovimiento.CELDA_REPETIDA);
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO, "Se acepta el siguiente número.");
		comprobar(motor.estadoJuego().proximoNumero() == TERCER_NUMERO,
				"Avanzar un punto numerado actualiza el próximo.");
		movimientoNumerado();
		mensajes();
	}

	private void movimientoNumerado() {
		MotorJuego ordenado = new MotorJuego(
				nivel(serpiente(), 0, 2, TAMANO * 2 - 1, ULTIMO_INDICE));
		bloqueado(ordenado, Direccion.ABAJO, ResultadoMovimiento.FUERA_DE_ORDEN);
		Nivel nivelTemprano = nivel(serpiente(), 0, 1);
		MotorJuego temprano = new MotorJuego(nivelTemprano);
		bloqueado(temprano, Direccion.DERECHA, ResultadoMovimiento.FINAL_ANTICIPADO);
		comprobar(temprano.mover(Direccion.ABAJO) == ResultadoMovimiento.VALIDO,
				"Un rechazo no bloquea otros movimientos.");
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivelTemprano,
			List.of(nivelTemprano.inicio(), new Celda(0, 1))));
	}

	private void mensajes() {
		for (ResultadoMovimiento resultado : ResultadoMovimiento.values()) {
			comprobar(!resultado.mensaje().isBlank(), "Cada resultado explica lo ocurrido.");
		}
		comprobar(ResultadoMovimiento.VALIDO.mensaje().equals("Paso válido."), "Mover no comunica un error.");
		comprobar(ResultadoMovimiento.FUERA_DEL_TABLERO.mensaje().contains("tablero"), "Salir indica el límite.");
		comprobar(ResultadoMovimiento.CELDA_REPETIDA.mensaje().contains("repetir"), "Repetir explica el rechazo.");
		comprobar(ResultadoMovimiento.FUERA_DE_ORDEN.mensaje().contains("orden"),
				"El orden tiene un mensaje específico.");
		comprobar(ResultadoMovimiento.FINAL_ANTICIPADO.mensaje().contains("celdas"),
				"Final prematuro explica las celdas pendientes.");
	}

	private void pruebasHistorial() {
		MotorJuego motor = new MotorJuego(nivel(serpiente(), 0, 2, ULTIMO_INDICE));
		comprobar(!motor.deshacer(), "No se puede deshacer el inicio.");
		motor.mover(Direccion.DERECHA);
		EstadoJuego anterior = motor.estadoJuego();
		motor.mover(Direccion.DERECHA);
		verificarPuntoNumerado(motor);
		comprobar(motor.deshacer() && motor.estadoJuego().visitadas() == 1, "Deshacer vuelve al inicio.");
		comprobar(!motor.deshacer(), "No se elimina el inicio.");
		comprobar(anterior.visitadas() == 2 && anterior.proximoNumero() == 2,
				"El estado de juego previo no cambia.");
		historialReinicio(motor, anterior);
	}

	private void verificarPuntoNumerado(MotorJuego motor) {
		comprobar(motor.estadoJuego().proximoNumero() == TERCER_NUMERO,
				"Punto numerado visitado.");
		comprobar(motor.deshacer() && motor.estadoJuego().proximoNumero() == 2,
				"Deshacer restaura el punto numerado.");
	}

	private void historialReinicio(MotorJuego motor, EstadoJuego anterior) {
		motor.mover(Direccion.DERECHA);
		motor.mover(Direccion.DERECHA);
		Nivel nivel = motor.estadoJuego().nivel();
		motor.reiniciar();
		comprobar(motor.estadoJuego().nivel() == nivel, "Reiniciar conserva el mismo nivel.");
		verificarEstadoReiniciado(motor, anterior, nivel);
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO,
				"Las celdas quedan libres al reiniciar.");
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO,
				"Se puede revisitar el punto numerado tras reiniciar.");
	}

	private void verificarEstadoReiniciado(MotorJuego motor, EstadoJuego anterior, Nivel nivel) {
		comprobar(motor.estadoJuego().visitadas() == 1
				&& motor.estadoJuego().proximoNumero() == 2, "Reinicio completo.");
		comprobar(motor.estadoJuego().actual().equals(nivel.inicio()), "Reiniciar vuelve al número 1.");
		comprobar(anterior.visitadas() == 2 && anterior.actual().equals(new Celda(0, 1)),
				"Reiniciar no altera instantáneas.");
	}

	private void pruebasVictoria() {
		Nivel nivel = nivel(serpiente(), 0, ULTIMO_INDICE);
		MotorJuego motor = new MotorJuego(nivel);
		completar(motor, serpiente());
		EstadoJuego gano = motor.estadoJuego();
		for (Direccion direccion : Direccion.values()) {
			bloqueado(motor, direccion, ResultadoMovimiento.FINALIZADO);
		}
		comprobar(!motor.deshacer() && motor.estadoJuego().equals(gano), "La victoria congela también deshacer.");
		motor.reiniciar();
		comprobar(!motor.estadoJuego().gano() && motor.estadoJuego().proximoNumero() == 2, "Reiniciar descongela.");
		comprobar(gano.gano() && gano.visitadas() == CANTIDAD_CELDAS, "La victoria anterior sigue inmutable.");
		solucionAlternativa(motor, nivel);
	}

	private void solucionAlternativa(MotorJuego motor, Nivel nivel) {
		List<Celda> alternativa = Arrays.stream(ALTERNATIVA.split(","))
			.mapToInt(Integer::parseInt).mapToObj(PruebaDominio::celdaEn).toList();
		validarRuta(alternativa, TAMANO);
		comprobar(!alternativa.equals(serpiente()), "La solución alternativa es otra ruta.");
		comprobar(compatible(nivel, alternativa),
				"La alternativa respeta los mismos puntos numerados.");
		completar(motor, alternativa);
		comprobar(motor.estadoJuego().recorrido().equals(alternativa),
				"Se acepta una solución no secreta.");
	}

	private void completar(MotorJuego motor, List<Celda> recorrido) {
		for (int indice = 1; indice < recorrido.size(); indice++) {
			avanzarSolucion(motor, recorrido, indice);
		}
		EstadoJuego estadoJuego = motor.estadoJuego();
		comprobar(estadoJuego.gano(), "Se reconoce la victoria.");
		comprobar(estadoJuego.proximoNumero() == estadoJuego.nivel().puntos() + 1, "Tras ganar, el próximo es N+1.");
		comprobar(estadoJuego.visitadas() == estadoJuego.nivel().cantidadCeldas(),
				"La victoria recorre todo el tablero.");
	}

	private void avanzarSolucion(MotorJuego motor, List<Celda> recorrido, int indice) {
		comprobar(!motor.estadoJuego().gano(), "No se gana antes de recorrer todo.");
		ResultadoMovimiento esperado = indice == recorrido.size() - 1
				? ResultadoMovimiento.FINALIZADO : ResultadoMovimiento.VALIDO;
		comprobar(motor.mover(direccion(recorrido.get(indice - 1),
				recorrido.get(indice))) == esperado, "Paso de solución.");
		comprobar(motor.estadoJuego().visitadas() == indice + 1,
				"Cada paso incorpora exactamente una celda.");
	}

	private void bloqueado(MotorJuego motor, Direccion direccion, ResultadoMovimiento esperado) {
		EstadoJuego antes = motor.estadoJuego();
		comprobar(motor.mover(direccion) == esperado, "Resultado esperado: " + esperado);
		comprobar(motor.estadoJuego().equals(antes), "Un movimiento rechazado o congelado no cambia el estado.");
	}

	private static Direccion direccion(Celda origen, Celda destino) {
		for (Direccion direccion : Direccion.values()) {
			if (origen.mover(direccion).equals(destino)) {
				return direccion;
			}
		}
		throw new AssertionError("La ruta contiene un paso no cardinal: " + origen + " -> " + destino);
	}

	private static Nivel nivel(List<Celda> recorrido, int... puntosNumerados) {
		return nivel(TAMANO, recorrido, puntosNumerados);
	}

	private static Nivel nivel(int tamano, List<Celda> recorrido, int... puntosNumerados) {
		List<Integer> numeros = numerosVacios(tamano);
		for (int indice = 0; indice < puntosNumerados.length; indice++) {
			Celda celda = recorrido.get(puntosNumerados[indice]);
			numeros.set(celda.fila() * tamano + celda.columna(), indice + 1);
		}
		return new Nivel(tamano, numeros);
	}

	private static List<Integer> numerosVacios() {
		return numerosVacios(TAMANO);
	}

	private static List<Integer> numerosVacios(int tamano) {
		return new ArrayList<>(Collections.nCopies(tamano * tamano, 0));
	}

	private static Celda celdaEn(int indice) {
		return new Celda(indice / TAMANO, indice % TAMANO);
	}

	private static List<Celda> serpiente() {
		return GeneradorNivel.recorrido(TAMANO, 0, 0, false, false);
	}

	private void comprobar(boolean condicion, String mensaje) {
		verificar(condicion, mensaje);
		verificaciones++;
	}

	private void rechaza(Class<? extends Throwable> tipo, Runnable accion) {
		esperarExcepcion(tipo, accion);
		verificaciones++;
	}
}
