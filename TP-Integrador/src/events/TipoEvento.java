package events;

public enum TipoEvento {
	MOVIMIENTO,

	CONEXION_ESTABLECIDA,
	CONEXION_PERDIDA,

	GRILLA_COMPLETA,
	RIVAL_COMPLETO_GRILLA,
	LIMPIAR_TOMATE,

	INICIAR,
	DESHACER,
	REINICIAR;

	/**
	 * Convierte un texto en un TipoEvento, devolviendo null si no es válido.
	 * 
	 * Utilizado para traducir mensajes de red a eventos reconocibles.
	 * @param texto
	 * @return
	 */
	public static TipoEvento desdeTexto(String texto) {
		if (texto == null) {
			return null;
		}
		try {
			return valueOf(texto.trim());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
