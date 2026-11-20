package events;

public enum TipoEvento {
	MOVIMIENTO,
	IR_A_CELDA,

	CONEXION_PERDIDA,
	JUGADOR_DESCONECTADO,

	PARTIDA_INICIADA,
	SOLICITAR_REVANCHA,
	GRILLA_COMPLETA,
	RIVAL_COMPLETO_GRILLA,
	LIMPIAR_TOMATE,
	PARTIDA_FINALIZADA,

	INICIAR,
	DESHACER,
	REINICIAR;

	public static TipoEvento desdeTexto(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		String limpio = texto.trim();
		for (TipoEvento tipo : values()) {
			if (tipo.name().equals(limpio)) {
				return tipo;
			}
		}
		return null;
	}
}
