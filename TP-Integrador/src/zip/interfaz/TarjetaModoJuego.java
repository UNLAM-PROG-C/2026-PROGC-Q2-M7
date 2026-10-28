package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import zip.aplicacion.ModoJuego;

final class TarjetaModoJuego extends TarjetaSeleccionable {
	private static final long serialVersionUID = 1L;
	private final ModoJuego modo;
	private final IconoLineal icono;

	TarjetaModoJuego(ModoJuego modo, String titulo, String descripcion) {
		this(modo, titulo, descripcion, crearContenido(modo, titulo, descripcion));
	}

	private TarjetaModoJuego(ModoJuego modo, String titulo, String descripcion,
			Contenido contenido) {
		super(contenido.panel(), titulo, descripcion);
		this.modo = modo;
		this.icono = contenido.icono();
		setPreferredSize(new Dimension(0, Tema.ALTO_TARJETA_MODO));
		setMinimumSize(getPreferredSize());
	}

	ModoJuego modo() {
		return modo;
	}

	@Override
	protected void cambioSeleccion(boolean seleccionada) {
		icono.establecerColor(seleccionada ? Tema.ACENTO : Tema.TINTA);
	}

	private static Contenido crearContenido(ModoJuego modo, String titulo, String descripcion) {
		IconoLineal icono = new IconoLineal(modo == ModoJuego.UN_JUGADOR
				? TipoIcono.PERSONA : TipoIcono.PERSONAS, Tema.TAMANO_ICONO_TARJETA, Tema.TINTA);
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_TARJETA,
				Tema.RELLENO_TARJETA, Tema.RELLENO_TARJETA,
				Tema.RELLENO_TARJETA + Tema.ESPACIO_RADIO));
		panel.add(icono, BorderLayout.WEST);
		panel.add(copia(titulo, descripcion), BorderLayout.CENTER);
		return new Contenido(panel, icono);
	}

	private static JPanel copia(String titulo, String descripcion) {
		JPanel copia = new JPanel();
		configurarCopia(copia);
		JLabel nombre = etiqueta(titulo, Tema.TAMANO_TITULO_TARJETA, true, Tema.TINTA);
		JLabel apoyo = etiqueta(descripcion, Tema.TAMANO_TEXTO_TARJETA, false, Tema.TINTA_SUAVE);
		nombre.setAlignmentX(LEFT_ALIGNMENT);
		apoyo.setAlignmentX(LEFT_ALIGNMENT);
		copia.add(Box.createVerticalGlue());
		copia.add(nombre);
		copia.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		copia.add(apoyo);
		copia.add(Box.createVerticalGlue());
		return copia;
	}

	private static void configurarCopia(JPanel copia) {
		copia.setOpaque(false);
		copia.setLayout(new BoxLayout(copia, BoxLayout.Y_AXIS));
		copia.setBorder(BorderFactory.createEmptyBorder(0, Tema.SEPARACION_TARJETAS, 0, 0));
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita,
			java.awt.Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Tema.fuente(tamano, negrita));
		etiqueta.setForeground(color);
		return etiqueta;
	}

	private record Contenido(JPanel panel, IconoLineal icono) {
	}
}
