package ui;

import java.awt.event.KeyEvent;
import java.util.Map;
import model.Direccion;

public enum Controles {
	FLECHAS("Flechas", "↑ ← ↓ →", Map.of(
			Direccion.ARRIBA, KeyEvent.VK_UP,
			Direccion.DERECHA, KeyEvent.VK_RIGHT,
			Direccion.ABAJO, KeyEvent.VK_DOWN,
			Direccion.IZQUIERDA, KeyEvent.VK_LEFT)),
	WASD("W A S D", "W A S D", Map.of(
			Direccion.ARRIBA, KeyEvent.VK_W,
			Direccion.DERECHA, KeyEvent.VK_D,
			Direccion.ABAJO, KeyEvent.VK_S,
			Direccion.IZQUIERDA, KeyEvent.VK_A)),
	MOUSE("Mouse", "Arrastrar", Map.of());

	private static final String SIN_TECLAS = "Los controles de Mouse no usan teclas de dirección.";

	private final String etiqueta;
	private final String leyenda;
	private final Map<Direccion, Integer> teclas;

	Controles(String etiqueta, String leyenda, Map<Direccion, Integer> teclas) {
		this.etiqueta = etiqueta;
		this.leyenda = leyenda;
		this.teclas = teclas;
	}

	public boolean usaMouse() {
		return this == MOUSE;
	}

	public int tecla(Direccion direccion) {
		if (usaMouse()) {
			throw new IllegalStateException(SIN_TECLAS);
		}
		return teclas.get(direccion);
	}

	public String leyendaTeclas() {
		return leyenda;
	}

	@Override
	public String toString() {
		return etiqueta;
	}
}
