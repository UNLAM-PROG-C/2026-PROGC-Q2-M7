package zip;

public final class SoportePruebas {
	private SoportePruebas() {
	}

	public static void verificar(boolean condicion, String mensaje) {
		if (!condicion) {
			throw new AssertionError(mensaje);
		}
	}

	public static void esperarExcepcion(Class<? extends Throwable> tipo, Runnable accion) {
		try {
			accion.run();
		} catch (Throwable error) {
			verificar(tipo.isInstance(error), "Se esperaba " + tipo.getSimpleName() + ", se obtuvo " + error);
			return;
		}
		throw new AssertionError("Se esperaba " + tipo.getSimpleName());
	}
}
