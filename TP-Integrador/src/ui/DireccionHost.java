package ui;

import utils.Constantes;

/** Dirección "ip:puerto" del host, ya validada. */
record DireccionHost(String host, int puerto) {
	private static final int PUERTO_MAXIMO = 65535;
	private static final String PREFIJO_TCP = "^tcp://";
	private static final char SEPARADOR = ':';
	private static final String PUERTO_INVALIDO = "Puerto inválido: ";

	/** Interpreta "ip", "ip:puerto" o ":puerto"; lanza IllegalArgumentException si es inválida. */
	static DireccionHost parsear(String entrada) {
		String limpio = entrada.trim().replaceFirst(PREFIJO_TCP, "");
		int sep = limpio.indexOf(SEPARADOR);
		String crudo = sep < 0 ? limpio : limpio.substring(0, sep).trim();
		String host = crudo.isEmpty() ? Constantes.IP_LOCAL : crudo;
		int puerto = sep < 0 ? Constantes.PUERTO : leerPuerto(limpio.substring(sep + 1).trim());
		return new DireccionHost(host, puerto);
	}

	private static int leerPuerto(String texto) {
		try {
			int puerto = Integer.parseInt(texto);
			if (puerto >= 1 && puerto <= PUERTO_MAXIMO) {
				return puerto;
			}
		} catch (NumberFormatException ignorada) {
			// se informa abajo con el mismo mensaje
		}
		throw new IllegalArgumentException(PUERTO_INVALIDO + texto);
	}

	@Override
	public String toString() {
		return host + SEPARADOR + puerto;
	}
}
