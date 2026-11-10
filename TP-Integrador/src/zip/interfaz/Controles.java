package zip.interfaz;

import java.awt.event.KeyEvent;
import java.util.Map;
import zip.dominio.Direccion;

public enum Controles {
	FLECHAS("Flechas", Map.of(Direccion.ARRIBA, KeyEvent.VK_UP, Direccion.DERECHA, KeyEvent.VK_RIGHT,
			Direccion.ABAJO, KeyEvent.VK_DOWN, Direccion.IZQUIERDA, KeyEvent.VK_LEFT)),
	WASD("WASD", Map.of(Direccion.ARRIBA, KeyEvent.VK_W, Direccion.DERECHA, KeyEvent.VK_D,
			Direccion.ABAJO, KeyEvent.VK_S, Direccion.IZQUIERDA, KeyEvent.VK_A));

	private final String etiqueta;
	private final Map<Direccion, Integer> teclas;

	Controles(String etiqueta, Map<Direccion, Integer> teclas) {
		this.etiqueta = etiqueta;
		this.teclas = teclas;
	}

	public int tecla(Direccion direccion) {
		return teclas.get(direccion);
	}

	@Override
	public String toString() {
		return etiqueta;
	}
}
