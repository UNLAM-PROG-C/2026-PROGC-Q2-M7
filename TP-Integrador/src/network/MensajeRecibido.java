package network;

/**
 * Payload de {@code TipoEvento.MENSAJE_RED}.
 *
 * @param origen id del jugador que lo mandó (en el cliente siempre es el host,
 *     {@code Constantes.ID_HOST})
 */
public record MensajeRecibido(int origen, MensajeRed mensaje) {
}
