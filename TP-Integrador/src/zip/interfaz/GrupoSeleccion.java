package zip.interfaz;

import java.util.ArrayList;
import java.util.List;

final class GrupoSeleccion {
	private final List<TarjetaSeleccionable> tarjetas = new ArrayList<>();

	void agregar(TarjetaSeleccionable tarjeta) {
		tarjetas.add(tarjeta);
		tarjeta.unir(this);
	}

	void seleccionar(TarjetaSeleccionable seleccionada) {
		tarjetas.forEach(tarjeta -> tarjeta.aplicarSeleccion(tarjeta == seleccionada));
	}
}
