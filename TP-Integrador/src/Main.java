import javax.swing.SwingUtilities;

import ui.VentanaJuego;

/** Punto de entrada: abre la ventana del juego en el hilo de eventos de Swing. */
public final class Main {

	private Main() {}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new VentanaJuego().mostrar());
	}
}
