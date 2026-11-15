package network;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Un mensaje del protocolo: tipo más argumentos de texto. Se transmite como una línea.
 *
 * <p>Formato: {@code TIPO arg1 arg2 ...}, separados por un único espacio. Cada argumento se
 * codifica con URL-encoding UTF-8 (espacio, "ñ", tildes y separadores quedan escapados), así el
 * único espacio literal de la línea es el separador. Un mensaje sin argumentos es solo el tipo;
 * un argumento vacío se transmite como token vacío ({@code "TIPO "}).
 */
public record MensajeRed(TipoMensaje tipo, List<String> argumentos) {
	private static final String SEPARADOR = " ";
	private static final int SIN_LIMITE = -1;
	private static final int POSICION_TIPO = 0;

	public MensajeRed {
		Objects.requireNonNull(tipo, "tipo");
		argumentos = List.copyOf(argumentos);
	}

	/** Atajo: cada argumento se convierte con String.valueOf. */
	public static MensajeRed de(TipoMensaje tipo, Object... argumentos) {
		List<String> textos = new ArrayList<>();
		for (Object argumento : argumentos) {
			textos.add(String.valueOf(argumento));
		}
		return new MensajeRed(tipo, textos);
	}

	/** Codifica el mensaje en una línea de texto (sin salto de línea). */
	public String aLinea() {
		StringBuilder linea = new StringBuilder(tipo.name());
		for (String argumento : argumentos) {
			linea.append(SEPARADOR).append(URLEncoder.encode(argumento, StandardCharsets.UTF_8));
		}
		return linea.toString();
	}

	/** Decodifica una línea; vacío si no es un mensaje válido del protocolo. */
	public static Optional<MensajeRed> desdeLinea(String linea) {
		if (linea == null || linea.isEmpty()) {
			return Optional.empty();
		}
		String[] partes = linea.split(SEPARADOR, SIN_LIMITE);
		try {
			return Optional.of(armar(partes));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private static MensajeRed armar(String[] partes) {
		TipoMensaje tipo = TipoMensaje.valueOf(partes[POSICION_TIPO]);
		List<String> argumentos = new ArrayList<>();
		for (int i = POSICION_TIPO + 1; i < partes.length; i++) {
			argumentos.add(decodificar(partes[i]));
		}
		return new MensajeRed(tipo, argumentos);
	}

	/** URL-decoding estricto: solo ASCII y UTF-8 bien formado; si no, IllegalArgumentException. */
	private static String decodificar(String parte) {
		if (!StandardCharsets.US_ASCII.newEncoder().canEncode(parte)) {
			throw new IllegalArgumentException("caracter no ASCII");
		}
		byte[] bytes = URLDecoder.decode(parte, StandardCharsets.ISO_8859_1)
				.getBytes(StandardCharsets.ISO_8859_1);
		try {
			return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
