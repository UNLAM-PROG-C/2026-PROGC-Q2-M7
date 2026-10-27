package zip.dominio;

public enum Direccion {
	ARRIBA(-1, 0),
	DERECHA(0, 1),
	ABAJO(1, 0),
	IZQUIERDA(0, -1);

	private final int deltaFila;
	private final int deltaColumna;

	Direccion(int deltaFila, int deltaColumna) {
		this.deltaFila = deltaFila;
		this.deltaColumna = deltaColumna;
	}

	public int deltaFila() {
		return deltaFila;
	}

	public int deltaColumna() {
		return deltaColumna;
	}
}
