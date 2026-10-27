package zip.aplicacion;

import java.util.Objects;
import java.util.Optional;
import zip.dominio.EstadoJuego;

public record ActualizacionSesion(Optional<EstadoJuego> estadoJuego, String mensaje, boolean fallo) {
	public ActualizacionSesion {
		Objects.requireNonNull(estadoJuego, "estadoJuego");
		Objects.requireNonNull(mensaje, "mensaje");
	}
}
