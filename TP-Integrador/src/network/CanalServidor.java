package network;

/** Canal del host: además de difundir, puede hablarle a un jugador puntual o echarlo. */
public interface CanalServidor extends CanalRed {
	void enviarA(int idJugador, MensajeRed mensaje);

	void enviarATodosMenos(int idExcluido, MensajeRed mensaje);

	void desconectar(int idJugador);

	int puerto();
}
