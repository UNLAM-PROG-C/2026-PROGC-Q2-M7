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
	private static final int ULTIMO_INDICE = Nivel.CANTIDAD_CELDAS - 1;
	private static final int ULTIMA_FILA = Nivel.TAMANO - 1;
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
		pruebasNivel();
		pruebasGenerador();
		pruebasEstado();
		pruebasMovimiento();
		pruebasHistorial();
		pruebasVictoria();
		System.out.println("Dominio Zip: " + verificaciones + " verificaciones correctas.");
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
		comprobar(nivel.inicio().equals(new Celda(0, 0)), "Inicio en el número 1.");
		comprobar(nivel.numeroEn(new Celda(ULTIMA_FILA, 0)) == 2, "Números en orden por filas.");
		comprobar(nivel.numeroEn(new Celda(0, 1)) == 0, "Las celdas libres son cero.");
		comprobar(nivel.contiene(new Celda(ULTIMA_FILA, ULTIMA_FILA)), "La esquina inferior pertenece.");
		limitesNivel(nivel);
		nivelesInvalidos();
		numerosInvalidos();
		inmutabilidadNivel(nivel);
	}

	private void limitesNivel(Nivel nivel) {
		for (Celda celda : List.of(new Celda(-1, 0), new Celda(0, -1),
			new Celda(Nivel.TAMANO, 0), new Celda(0, Nivel.TAMANO))) {
			comprobar(!nivel.contiene(celda), "Una celda exterior no pertenece.");
			rechaza(IllegalArgumentException.class, () -> nivel.numeroEn(celda));
		}
		rechaza(NullPointerException.class, () -> nivel.contiene(null));
		rechaza(NullPointerException.class, () -> nivel.numeroEn(null));
	}

	private void nivelesInvalidos() {
		rechaza(NullPointerException.class, () -> new Nivel(null));
		rechaza(IllegalArgumentException.class, () -> new Nivel(List.of()));
		rechaza(IllegalArgumentException.class, () -> new Nivel(Collections.nCopies(ULTIMO_INDICE, 0)));
		rechaza(IllegalArgumentException.class, () -> new Nivel(Collections.nCopies(Nivel.CANTIDAD_CELDAS + 1, 0)));
		rechaza(IllegalArgumentException.class, () -> new Nivel(numerosVacios()));
		List<Integer> unico = numerosVacios();
		unico.set(0, 1);
		rechaza(IllegalArgumentException.class, () -> new Nivel(unico));
		List<Integer> entradaNula = numerosVacios();
		entradaNula.set(0, null);
		rechaza(NullPointerException.class, () -> new Nivel(entradaNula));
	}

	private void numerosInvalidos() {
		List<Integer> fuente = new ArrayList<>(nivel(serpiente(), 0, ULTIMO_INDICE).numeros());
		for (int invalid : new int[] {-1, Nivel.PUNTOS_MAXIMOS + 1, 1, TERCER_NUMERO}) {
			List<Integer> modificados = new ArrayList<>(fuente);
			modificados.set(Nivel.TAMANO * ULTIMA_FILA, invalid);
			rechaza(IllegalArgumentException.class, () -> new Nivel(modificados));
		}
	}

	private void inmutabilidadNivel(Nivel original) {
		List<Integer> fuente = new ArrayList<>(original.numeros());
		Nivel copia = new Nivel(fuente);
		fuente.set(0, 0);
		comprobar(copia.numeroEn(copia.inicio()) == 1, "El nivel copia los números de entrada.");
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().set(0, 0));
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().add(0));
		rechaza(UnsupportedOperationException.class, () -> copia.numeros().clear());
		comprobar(copia.numeros().equals(original.numeros()), "El nivel mantiene su configuración.");
	}

	private void pruebasGenerador() {
		GeneradorNivel primero = new GeneradorNivel(SEMILLA);
		GeneradorNivel segundo = new GeneradorNivel(SEMILLA);
		List<List<Celda>> recorridos = todosLosRecorridos();
		for (int puntos = Nivel.PUNTOS_MINIMOS; puntos <= Nivel.PUNTOS_MAXIMOS; puntos++) {
			Nivel generado = primero.generar(puntos);
			comprobar(generado.puntos() == puntos, "El generador respeta cada configuración.");
			comprobar(generado.numeros().equals(segundo.generar(puntos).numeros()), "Semilla reproducible.");
			resolverGenerado(generado, recorridos);
		}
		limitesGenerador(primero);
		resolverGenerado(new GeneradorNivel().generar(Nivel.PUNTOS_MINIMOS), recorridos);
	}

	private void limitesGenerador(GeneradorNivel generador) {
		rechaza(IllegalArgumentException.class, () -> generador.generar(Nivel.PUNTOS_MINIMOS - 1));
		rechaza(IllegalArgumentException.class, () -> generador.generar(Nivel.PUNTOS_MAXIMOS + 1));
		rechaza(IllegalArgumentException.class, () -> generador.generar(-1));
		rechaza(IllegalArgumentException.class, () -> GeneradorNivel.recorrido(-1, 0, false, false));
		rechaza(IllegalArgumentException.class, () -> GeneradorNivel.recorrido(CANTIDAD_PLANTILLAS, 0, false, false));
		rechaza(IllegalArgumentException.class, () -> GeneradorNivel.recorrido(0, -1, false, false));
		rechaza(IllegalArgumentException.class, () -> GeneradorNivel.recorrido(0, CANTIDAD_ROTACIONES, false, false));
	}

	private List<List<Celda>> todosLosRecorridos() {
		List<List<Celda>> recorridos = new ArrayList<>();
		for (int plantilla = 0; plantilla < CANTIDAD_PLANTILLAS; plantilla++) {
			for (int rotacion = 0; rotacion < CANTIDAD_ROTACIONES; rotacion++) {
				agregarVariantes(recorridos, plantilla, rotacion);
			}
		}
		comprobar(!new HashSet<>(recorridos.get(0)).isEmpty(), "Existen rutas de construcción.");
		comprobar(!recorridos.get(0).equals(recorridos.get(CANTIDAD_ROTACIONES * 2 * 2)),
				"Las dos bases son distintas.");
		return recorridos;
	}

	private void agregarVariantes(List<List<Celda>> recorridos, int plantilla, int rotacion) {
		for (boolean reflejado : new boolean[] {false, true}) {
			for (boolean invertido : new boolean[] {false, true}) {
				List<Celda> recorrido = GeneradorNivel.recorrido(plantilla, rotacion, reflejado, invertido);
				validarRuta(recorrido);
				MotorJuego motor = new MotorJuego(nivel(recorrido, 0, 2, ULTIMO_INDICE));
				completar(motor, recorrido);
				recorridos.add(recorrido);
			}
		}
	}

	private void validarRuta(List<Celda> recorrido) {
		comprobar(recorrido.size() == Nivel.CANTIDAD_CELDAS, "La ruta cubre 36 celdas.");
		comprobar(new HashSet<>(recorrido).size() == Nivel.CANTIDAD_CELDAS, "La ruta no repite celdas.");
		for (Celda celda : recorrido) {
			comprobar(celda.fila() >= 0 && celda.fila() < Nivel.TAMANO, "Fila válida de la ruta.");
			comprobar(celda.columna() >= 0 && celda.columna() < Nivel.TAMANO, "Columna válida de la ruta.");
		}
		for (int indice = 1; indice < recorrido.size(); indice++) {
			direccion(recorrido.get(indice - 1), recorrido.get(indice));
		}
		rechaza(UnsupportedOperationException.class, () -> recorrido.clear());
	}

	private void resolverGenerado(Nivel nivel, List<List<Celda>> recorridos) {
		List<Celda> solucion = recorridos.stream().filter(recorrido -> compatible(nivel, recorrido)).findFirst()
			.orElseThrow(() -> new AssertionError("El nivel generado no conserva una solución."));
		comprobar(nivel.numeroEn(solucion.get(0)) == 1, "El primer punto numerado está en el inicio.");
		comprobar(nivel.numeroEn(solucion.get(ULTIMO_INDICE)) == nivel.puntos(), "El último está en el final.");
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
		return nivel.numeroEn(recorrido.get(0)) == 1 && nivel.numeroEn(recorrido.get(ULTIMO_INDICE)) == nivel.puntos();
	}

	private void pruebasEstado() {
		Nivel nivel = nivel(serpiente(), 0, 2, ULTIMO_INDICE);
		List<Celda> fuente = new ArrayList<>(List.of(nivel.inicio()));
		EstadoJuego estadoJuego = new EstadoJuego(nivel, fuente);
		fuente.add(new Celda(0, 1));
		comprobar(estadoJuego.visitadas() == 1, "El estadoJuego copia el recorrido.");
		comprobar(estadoJuego.nivel() == nivel, "El nivel compartido es inmutable.");
		comprobar(estadoJuego.actual().equals(nivel.inicio()), "Celda inicial del estado de juego.");
		comprobar(estadoJuego.proximoNumero() == 2 && !estadoJuego.gano(), "Estado inicial derivado.");
		rechaza(UnsupportedOperationException.class, () -> estadoJuego.recorrido().add(new Celda(0, 1)));
		rechaza(UnsupportedOperationException.class, () -> estadoJuego.nivel().numeros().set(0, 0));
		estadosInvalidos(nivel);
	}

	private void estadosInvalidos(Nivel nivel) {
		rechaza(NullPointerException.class, () -> new EstadoJuego(null, List.of(nivel.inicio())));
		rechaza(NullPointerException.class, () -> new EstadoJuego(nivel, null));
		rechaza(NullPointerException.class, () -> new EstadoJuego(nivel, Arrays.asList(nivel.inicio(), null)));
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivel, List.of()));
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivel, List.of(new Celda(0, 1))));
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivel, List.of(nivel.inicio(), nivel.inicio())));
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
		MotorJuego ordenado = new MotorJuego(nivel(serpiente(), 0, 2, Nivel.TAMANO * 2 - 1, ULTIMO_INDICE));
		bloqueado(ordenado, Direccion.ABAJO, ResultadoMovimiento.FUERA_DE_ORDEN);
		Nivel nivelFinalAnticipado = nivel(serpiente(), 0, 1);
		MotorJuego anticipado = new MotorJuego(nivelFinalAnticipado);
		bloqueado(anticipado, Direccion.DERECHA, ResultadoMovimiento.FINAL_ANTICIPADO);
		comprobar(anticipado.mover(Direccion.ABAJO) == ResultadoMovimiento.VALIDO,
				"Un rechazo no bloquea otros movimientos.");
		rechaza(IllegalArgumentException.class, () -> new EstadoJuego(nivelFinalAnticipado,
			List.of(nivelFinalAnticipado.inicio(), new Celda(0, 1))));
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
		comprobar(ResultadoMovimiento.FINAL_ANTICIPADO.mensaje().contains("36"),
				"Final prematuro explica las celdas pendientes.");
	}

	private void pruebasHistorial() {
		MotorJuego motor = new MotorJuego(nivel(serpiente(), 0, 2, ULTIMO_INDICE));
		comprobar(!motor.deshacer(), "No se puede deshacer el inicio.");
		motor.mover(Direccion.DERECHA);
		EstadoJuego anterior = motor.estadoJuego();
		motor.mover(Direccion.DERECHA);
		comprobar(motor.estadoJuego().proximoNumero() == TERCER_NUMERO, "Punto numerado visitado.");
		comprobar(motor.deshacer() && motor.estadoJuego().proximoNumero() == 2,
				"Deshacer restaura el punto numerado.");
		comprobar(motor.deshacer() && motor.estadoJuego().visitadas() == 1, "Deshacer vuelve al inicio.");
		comprobar(!motor.deshacer(), "No se elimina el inicio.");
		comprobar(anterior.visitadas() == 2 && anterior.proximoNumero() == 2, "El estadoJuego previo no cambia.");
		historialReinicio(motor, anterior);
	}

	private void historialReinicio(MotorJuego motor, EstadoJuego anterior) {
		motor.mover(Direccion.DERECHA);
		motor.mover(Direccion.DERECHA);
		Nivel nivel = motor.estadoJuego().nivel();
		motor.reiniciar();
		comprobarReinicio(motor, nivel, anterior);
	}

	private void comprobarReinicio(MotorJuego motor, Nivel nivel, EstadoJuego anterior) {
		comprobar(motor.estadoJuego().nivel() == nivel, "Reiniciar conserva el mismo nivel.");
		comprobar(motor.estadoJuego().visitadas() == 1
				&& motor.estadoJuego().proximoNumero() == 2, "Reinicio completo.");
		comprobar(motor.estadoJuego().actual().equals(nivel.inicio()), "Reiniciar vuelve al número 1.");
		comprobar(anterior.visitadas() == 2 && anterior.actual().equals(new Celda(0, 1)),
				"Reiniciar no altera estados anteriores.");
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO,
				"Las celdas quedan libres al reiniciar.");
		comprobar(motor.mover(Direccion.DERECHA) == ResultadoMovimiento.VALIDO,
				"Se puede revisitar el punto numerado tras reiniciar.");
	}

	private void pruebasVictoria() {
		Nivel nivel = nivel(serpiente(), 0, ULTIMO_INDICE);
		MotorJuego motor = new MotorJuego(nivel);
		completar(motor, serpiente());
		EstadoJuego gano = motor.estadoJuego();
		comprobarVictoriaCongelada(motor, gano);
		motor.reiniciar();
		comprobar(!motor.estadoJuego().gano() && motor.estadoJuego().proximoNumero() == 2,
				"Reiniciar descongela.");
		comprobar(gano.gano() && gano.visitadas() == Nivel.CANTIDAD_CELDAS,
				"La victoria anterior sigue inmutable.");
		solucionAlternativa(motor, nivel);
	}

	private void comprobarVictoriaCongelada(MotorJuego motor, EstadoJuego gano) {
		for (Direccion direccion : Direccion.values()) {
			bloqueado(motor, direccion, ResultadoMovimiento.FINALIZADO);
		}
		comprobar(!motor.deshacer() && motor.estadoJuego().equals(gano),
				"La victoria congela también deshacer.");
	}

	private void solucionAlternativa(MotorJuego motor, Nivel nivel) {
		List<Celda> alternativa = Arrays.stream(ALTERNATIVA.split(","))
			.mapToInt(Integer::parseInt).mapToObj(PruebaDominio::celdaEn).toList();
		validarRuta(alternativa);
		comprobar(!alternativa.equals(serpiente()), "La solución alternativa es otra ruta.");
		comprobar(compatible(nivel, alternativa), "La alternativa respeta los mismos puntos numerados.");
		completar(motor, alternativa);
		comprobar(motor.estadoJuego().recorrido().equals(alternativa), "Se acepta una solución no secreta.");
	}

	private void completar(MotorJuego motor, List<Celda> recorrido) {
		for (int indice = 1; indice < recorrido.size(); indice++) {
			comprobar(!motor.estadoJuego().gano(), "No se gana antes de recorrer todo.");
			ResultadoMovimiento esperado = indice == ULTIMO_INDICE
					? ResultadoMovimiento.FINALIZADO : ResultadoMovimiento.VALIDO;
			comprobar(motor.mover(direccion(recorrido.get(indice - 1), recorrido.get(indice))) == esperado,
					"Paso de solución.");
			comprobar(motor.estadoJuego().visitadas() == indice + 1, "Cada paso incorpora exactamente una celda.");
		}
		EstadoJuego estadoJuego = motor.estadoJuego();
		comprobar(estadoJuego.gano(), "Se reconoce la victoria.");
		comprobar(estadoJuego.proximoNumero() == estadoJuego.nivel().puntos() + 1, "Tras ganar, el próximo es N+1.");
		comprobar(estadoJuego.visitadas() == Nivel.CANTIDAD_CELDAS, "La victoria recorre todo el tablero.");
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
		List<Integer> numeros = numerosVacios();
		for (int indice = 0; indice < puntosNumerados.length; indice++) {
			Celda celda = recorrido.get(puntosNumerados[indice]);
			numeros.set(celda.fila() * Nivel.TAMANO + celda.columna(), indice + 1);
		}
		return new Nivel(numeros);
	}

	private static List<Integer> numerosVacios() {
		return new ArrayList<>(Collections.nCopies(Nivel.CANTIDAD_CELDAS, 0));
	}

	private static Celda celdaEn(int indice) {
		return new Celda(indice / Nivel.TAMANO, indice % Nivel.TAMANO);
	}

	private static List<Celda> serpiente() {
		return GeneradorNivel.recorrido(0, 0, false, false);
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
