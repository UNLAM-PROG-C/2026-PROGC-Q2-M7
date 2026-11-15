package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import controller.EstadoSala;
import controller.JugadorSala;
import model.ConfigPartida;
import utils.Constantes;

/**
 * Sala de espera: configuración, dirección para compartir, jugadores y su estado.
 * Es una vista pasiva (patrón MVC): se redibuja entera con cada {@link EstadoSala}.
 */
final class PanelSala extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int MS_AVISO_COPIADO = 1500;
	private static final String TEXTO_COPIAR = "Copiar";
	private static final String TEXTO_COPIADO = "¡Copiado!";
	private static final String LISTO = "✓ Listo";
	private static final String EN_ESPERA = "En espera";
	private static final String MARCA_LOCAL = " (vos)";
	private static final String MARCA_HOST = " (host)";
	private static final String SIN_CONFIG = "Esperando la configuración del host…";

	private final JLabel configuracion = Tema.etiqueta(
			" ", Font.BOLD, Tema.TAMANO_SUBTITULO, Tema.TINTA);
	private final JLabel direccion = Tema.etiqueta(
			" ", Font.BOLD, Tema.TAMANO_SUBTITULO, Tema.ACENTO);
	private final JButton copiar = Tema.boton(TEXTO_COPIAR, false);
	private final JPanel cajaDireccion = cajaDireccion();
	private final JPanel lista = new JPanel();
	private final JLabel faltan = Tema.texto(" ", Font.BOLD, Tema.TINTA_SUAVE);
	private final JLabel mensaje = Tema.texto(" ", Font.PLAIN, Tema.TINTA_SUAVE);
	private final JButton listo = Tema.boton("Estoy listo", true);
	private final transient Consumer<Boolean> alMarcarListo;
	private boolean listoLocal;

	PanelSala(Consumer<Boolean> alMarcarListo, Runnable alSalir) {
		super();
		this.alMarcarListo = alMarcarListo;
		setName("sala");
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(
				Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		construir(alSalir);
	}

	void mostrar(EstadoSala estado) {
		ConfigPartida config = estado.config();
		configuracion.setText(config == null ? SIN_CONFIG : Resumen.configuracion(
				config.dificultad(), config.rondas()) + " · " + config.jugadores() + " jugadores");
		boolean hayDireccion = !estado.direccionCompartir().isBlank();
		direccion.setText(estado.direccionCompartir());
		cajaDireccion.setVisible(hayDireccion);
		actualizarLista(estado);
		actualizarFaltantes(estado);
		mensaje.setText(estado.mensaje().isBlank() ? " " : estado.mensaje());
		actualizarBoton(estado);
	}

	private void construir(Runnable alSalir) {
		listo.setName("sala-listo");
		listo.addActionListener(e -> alMarcarListo.accept(!listoLocal));
		prepararLista();
		agregar(Tema.etiqueta("Sala de espera", Font.BOLD, Tema.TAMANO_TITULO, Tema.TINTA));
		agregar(configuracion);
		agregar(cajaDireccion);
		agregar(titulo("JUGADORES"));
		agregar(lista);
		agregar(faltan);
		agregar(mensaje);
		add(Box.createVerticalGlue());
		agregar(ancho(listo));
		agregar(ancho(botonSalir(alSalir)));
	}

	private static JButton botonSalir(Runnable alSalir) {
		JButton salir = Tema.boton("Salir", false);
		salir.setName("sala-salir");
		salir.addActionListener(e -> alSalir.run());
		return salir;
	}

	private void prepararLista() {
		lista.setName("sala-jugadores");
		lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
		lista.setOpaque(false);
	}

	private JPanel cajaDireccion() {
		JPanel caja = new JPanel(new BorderLayout(Tema.ESPACIO, 0));
		caja.setBackground(Tema.TARJETA);
		caja.setBorder(Tema.marco(Tema.ACENTO, 2, Tema.ESPACIO, Tema.MARGEN));
		direccion.setName("sala-direccion");
		copiar.setName("sala-copiar");
		copiar.addActionListener(e -> copiarDireccion());
		JLabel aviso = Tema.etiqueta("Compartí esta dirección:", Font.PLAIN, Tema.TAMANO_TEXTO,
				Tema.TINTA_SUAVE);
		caja.add(aviso, BorderLayout.NORTH);
		caja.add(direccion, BorderLayout.CENTER);
		JPanel lado = Tema.panelBorde(new BorderLayout(), 0, 0);
		lado.add(copiar, BorderLayout.SOUTH);
		caja.add(lado, BorderLayout.EAST);
		caja.setMaximumSize(new Dimension(Integer.MAX_VALUE, caja.getPreferredSize().height));
		return caja;
	}

	private void copiarDireccion() {
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
					.setContents(new StringSelection(direccion.getText()), null);
			copiar.setText(TEXTO_COPIADO);
		} catch (IllegalStateException e) {
			copiar.setText("No se pudo copiar");
		}
		Timer reinicio = new Timer(MS_AVISO_COPIADO, e -> copiar.setText(TEXTO_COPIAR));
		reinicio.setRepeats(false);
		reinicio.start();
	}

	private void actualizarLista(EstadoSala estado) {
		lista.removeAll();
		for (JugadorSala jugador : estado.jugadores()) {
			lista.add(fila(jugador, estado));
			lista.add(Box.createVerticalStrut(Tema.ESPACIO / 2));
		}
		lista.revalidate();
		lista.repaint();
	}

	private static JPanel fila(JugadorSala jugador, EstadoSala estado) {
		boolean local = jugador.id() == estado.idLocal();
		String nombre = jugador.nombre() + (local ? MARCA_LOCAL : "")
				+ (jugador.id() == Constantes.ID_HOST ? MARCA_HOST : "");
		JPanel fila = new JPanel(new BorderLayout());
		fila.setName("sala-jugador-" + jugador.id());
		fila.setBackground(local ? Tema.VISITADA : Tema.TARJETA);
		fila.setBorder(Tema.marco(local ? Tema.ACENTO : Tema.BORDE, 1, Tema.ESPACIO, Tema.ESPACIO));
		fila.add(Tema.texto(nombre, local ? Font.BOLD : Font.PLAIN, Tema.TINTA), BorderLayout.WEST);
		fila.add(estadoJugador(jugador), BorderLayout.EAST);
		fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height));
		return fila;
	}

	private static JLabel estadoJugador(JugadorSala jugador) {
		String texto = jugador.listo() ? LISTO : EN_ESPERA;
		return Tema.etiqueta(texto, Font.BOLD, Tema.TAMANO_TEXTO,
				jugador.listo() ? Tema.ACENTO : Tema.TINTA_SUAVE);
	}

	private void actualizarFaltantes(EstadoSala estado) {
		int esperados = estado.config() == null ? 0 : estado.config().jugadores();
		int faltantes = esperados - estado.jugadores().size();
		faltan.setText(faltantes <= 0 ? " " : faltantes == 1 ? "Falta 1 jugador"
				: "Faltan " + faltantes + " jugadores");
	}

	private void actualizarBoton(EstadoSala estado) {
		listoLocal = estado.jugadores().stream()
				.anyMatch(j -> j.id() == estado.idLocal() && j.listo());
		listo.setText(listoLocal ? "Cancelar listo" : "Estoy listo");
		listo.setEnabled(estado.idLocal() >= 0);
	}

	private static JButton ancho(JButton boton) {
		boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, boton.getPreferredSize().height));
		return boton;
	}

	private void agregar(JComponent comp) {
		comp.setAlignmentX(LEFT_ALIGNMENT);
		add(comp);
		add(Box.createVerticalStrut(Tema.ESPACIO));
	}

	private static JLabel titulo(String texto) {
		return Tema.etiqueta(texto, Font.BOLD, Tema.TAMANO_CHICO, Tema.TINTA_SUAVE);
	}
}
