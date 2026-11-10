import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import zip.dominio.GeneradorNivel;
import zip.infraestructura.SesionJuegoLocal;
import zip.interfaz.VentanaJuego;

public final class Principal {
	private Principal() {
	}

	public static void main(String[] argumentos) {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("Zip necesita un escritorio gráfico. Ejecutá bash scripts/run.sh fuera del contenedor.");
			System.exit(1);
		}
		SwingUtilities.invokeLater(Principal::mostrarJuego);
	}

	private static void mostrarJuego() {
		new VentanaJuego(escucha -> new SesionJuegoLocal(new GeneradorNivel()::generar,
				escucha, SwingUtilities::invokeLater)).mostrar();
	}
}
