import java.io.IOException;
import java.util.Scanner;

import controller.MotorJuego;
import events.EventBus;
import events.Evento;
import events.TipoEvento;
import utils.Constantes;

public final class Main {

	private Main() {}

	public static void main(String[] args) throws IOException {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Elegí rol (1: Host, 2: Cliente): ");
		int rol = Integer.parseInt(scanner.nextLine().trim());

		EventBus bus = new EventBus();
		MotorJuego motor = new MotorJuego(rol, bus);
		motor.iniciar();

		ejecutarBucleConsola(bus, scanner);
		motor.finalizar();
	}

	private static void ejecutarBucleConsola(EventBus bus, Scanner scanner) {
		System.out.println("\n[LISTO] Presioná ENTER para enviar tomate:");
		while (true) {
			String entrada = scanner.nextLine();
			if (Constantes.COMANDO_SALIR.equalsIgnoreCase(entrada.trim())) {
				break;
			}
			bus.publicar(new Evento(TipoEvento.GRILLA_COMPLETA));
		}
	}
}