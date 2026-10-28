package zip.interfaz;

import zip.dominio.Dificultad;

@FunctionalInterface
interface IniciadorJuego {
	void iniciar(Controles controles, Dificultad dificultad);
}
