package zip.aplicacion;

import java.util.function.Consumer;

@FunctionalInterface
public interface FabricaSesion {
	SesionJuego crear(Consumer<ActualizacionSesion> escucha);
}
