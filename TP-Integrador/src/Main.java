import javax.swing.SwingUtilities;

import ui.VentanaJuego;

public final class Main {

	private Main() {}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new VentanaJuego().mostrar());
	}
}
