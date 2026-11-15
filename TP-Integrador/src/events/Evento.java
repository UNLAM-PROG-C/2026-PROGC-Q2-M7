package events;

public record Evento(TipoEvento tipo, Object payload) {
	private static final char SEPARADOR = ':';

	public Evento(TipoEvento tipo) {
		this(tipo, null);
	}

	public String aLineaRed() {
		return payload == null ? tipo.name() : tipo.name() + SEPARADOR + payload;
	}

	public static Evento desdeLineaRed(String linea) {
		if (linea == null || linea.isBlank()) {
			return null;
		}
		int sep = linea.indexOf(SEPARADOR);
		String nombre = sep < 0 ? linea : linea.substring(0, sep);
		String datos = sep < 0 ? null : linea.substring(sep + 1);
		TipoEvento tipo = TipoEvento.desdeTexto(nombre);
		return tipo == null ? null : new Evento(tipo, datos);
	}
}
