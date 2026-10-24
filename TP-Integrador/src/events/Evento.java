package events;

public record Evento(TipoEvento tipo, Object payload) {
	public Evento(TipoEvento tipo) {
		this(tipo, null);
	}
}
