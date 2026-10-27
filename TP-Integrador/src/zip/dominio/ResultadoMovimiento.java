package zip.dominio;

public enum ResultadoMovimiento {
	VALIDO("Paso válido."),
	FUERA_DEL_TABLERO("No puedes salir del tablero."),
	CELDA_REPETIDA("No puedes repetir una celda visitada."),
	FUERA_DE_ORDEN("Debes visitar los números en orden."),
	FINAL_ANTICIPADO("Debes recorrer las 36 celdas antes del último número."),
	FINALIZADO("¡Ganaste! Recorriste todo el tablero en orden.");

	private final String mensaje;

	ResultadoMovimiento(String mensaje) {
		this.mensaje = mensaje;
	}

	public String mensaje() {
		return mensaje;
	}
}
