package zip.aplicacion;

import zip.dominio.Direccion;
import zip.dominio.Dificultad;

public interface SesionJuego extends AutoCloseable {
	boolean iniciar(Dificultad dificultad);

	boolean mover(Direccion direccion);

	boolean deshacer();

	boolean reiniciar();

	@Override
	void close();
}
