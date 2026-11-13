package ui;

import model.Dificultad;

@FunctionalInterface
interface IniciadorJuego {
	void iniciar(Controles controles, Dificultad dificultad);
}
