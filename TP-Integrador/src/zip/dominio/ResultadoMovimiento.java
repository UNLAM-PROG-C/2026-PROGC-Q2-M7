package zip.dominio;

public enum ResultadoMovimiento {
	VALIDO("Paso válido."),
	FUERA_DEL_TABLERO("No podés salir del tablero."),
	CELDA_REPETIDA("No podés repetir una celda visitada."),
	FUERA_DE_ORDEN("Debés visitar los números en orden."),
	FINAL_ANTICIPADO("Debés recorrer todas las celdas antes del último número."),
	FINALIZADO("¡Ganaste! Recorriste todo el tablero en orden.");

	private final String mensaje;

	ResultadoMovimiento(String mensaje) {
		this.mensaje = mensaje;
	}

	public String mensaje() {
		return mensaje;
	}
}
