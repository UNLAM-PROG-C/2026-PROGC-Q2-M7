package controller;

/** Un jugador en la sala de espera. El host tiene id {@code Constantes.ID_HOST}. */
public record JugadorSala(int id, String nombre, boolean listo, boolean conectado) {
}
