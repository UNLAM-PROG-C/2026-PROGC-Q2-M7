package network;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Dirección IP de esta máquina para que el host la comparta con los demás jugadores. */
public final class DireccionLocal {
	private static final String LOOPBACK = "127.0.0.1";
	private static final String PREFIJO_CASA = "192.168.";
	private static final String PREFIJO_10 = "10.";
	private static final int PRIORIDAD_CASA = 0;
	private static final int PRIORIDAD_10 = 1;
	private static final int PRIORIDAD_OTRA = 2;

	private DireccionLocal() {
	}

	/** IPv4 de la red local (no loopback); si no hay, 127.0.0.1. */
	public static String ipParaCompartir() {
		try {
			return candidatas().stream()
					.min(Comparator.comparingInt(DireccionLocal::prioridad))
					.orElse(LOOPBACK);
		} catch (SocketException | RuntimeException e) {
			return LOOPBACK;
		}
	}

	private static List<String> candidatas() throws SocketException {
		List<String> ips = new ArrayList<>();
		var interfaces = NetworkInterface.getNetworkInterfaces();
		if (interfaces == null) {
			return ips;
		}
		for (NetworkInterface interfaz : Collections.list(interfaces)) {
			if (interfaz.isUp() && !interfaz.isLoopback()) {
				agregarIpv4Privadas(interfaz, ips);
			}
		}
		return ips;
	}

	private static void agregarIpv4Privadas(NetworkInterface interfaz, List<String> ips) {
		for (InetAddress direccion : Collections.list(interfaz.getInetAddresses())) {
			if (direccion instanceof Inet4Address && direccion.isSiteLocalAddress()
					&& !direccion.isLoopbackAddress()) {
				ips.add(direccion.getHostAddress());
			}
		}
	}

	private static int prioridad(String ip) {
		if (ip.startsWith(PREFIJO_CASA)) {
			return PRIORIDAD_CASA;
		}
		return ip.startsWith(PREFIJO_10) ? PRIORIDAD_10 : PRIORIDAD_OTRA;
	}
}
