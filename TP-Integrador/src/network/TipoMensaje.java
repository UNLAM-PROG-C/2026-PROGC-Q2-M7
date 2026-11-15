package network;

/** Mensajes del protocolo de la partida en red. Cualquier otra línea recibida se descarta. */
public enum TipoMensaje {
	HOLA,
	BIENVENIDA,
	CONFIG,
	SALA,
	LISTO,
	EMPEZAR,
	RONDA_COMPLETA,
	PARTIDA_GANADA,
	RECHAZADO
}
