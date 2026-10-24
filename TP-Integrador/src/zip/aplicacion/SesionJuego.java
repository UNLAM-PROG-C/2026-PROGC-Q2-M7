package zip.aplicacion;

import zip.dominio.Direccion;

public interface SesionJuego extends AutoCloseable {
	boolean iniciar(int puntos);

	boolean mover(Direccion direccion);

	boolean deshacer();

	boolean reiniciar();

	@Override
	void close();
}
