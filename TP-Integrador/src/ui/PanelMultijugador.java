package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import utils.Constantes;

final class PanelMultijugador extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ANCHO_CONTENIDO = 460;
	private static final int ANCHO_BOTON = 360;
	private static final int TAMANO_TEXTO_DESCRIPCION = 18;
	private static final int TAMANO_ICONO_CONSTRUCCION = 92;
	private static final int SEPARACION_ICONO = 22;
	private static final int SEPARACION_INSIGNIA = 18;
	private static final int SEPARACION_TITULO = 14;
	private static final int SEPARACION_DESCRIPCION = 28;
	private static final int SEPARACION_BOTON_VOLVER = 10;

	private final BotonEstilo host = BotonEstilo.principal("Crear partida (Host)");
	private final BotonEstilo cliente = BotonEstilo.principal("Unirse a partida (Cliente)");
	private final BotonEstilo volver = BotonEstilo.fantasma("Volver / Cancelar");
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN, Tema.TAMANO_ESTADO, Tema.TINTA);

	PanelMultijugador(Runnable alCrearPartida, Runnable alUnirseAPartida, Runnable alVolver) {
		super(new BorderLayout());
		configurarPanel();
		vincularAcciones(alCrearPartida, alUnirseAPartida, alVolver);
		add(construirContenido(), BorderLayout.CENTER);
	}

	void ocupado(boolean ocupado) {
		host.setEnabled(!ocupado);
		cliente.setEnabled(!ocupado);
		volver.setEnabled(true);
		host.setText(ocupado ? "Conectando…" : "Crear partida (Host)");
		cliente.setText(ocupado ? "Conectando…" : "Unirse a partida (Cliente)");
	}

	void estado(String mensaje) {
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.TINTA);
	}

	void error(String mensaje) {
		ocupado(false);
		estadoMensaje.setText(mensaje);
		estadoMensaje.setForeground(Tema.ERROR);
	}

	private void configurarPanel() {
		setName("multijugador");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU, Tema.RELLENO_VERTICAL_MENU, Tema.RELLENO_HORIZONTAL_MENU));
		host.setName("crear-host");
		cliente.setName("unirse-cliente");
		volver.setName("multijugador-volver");
		estadoMensaje.setName("estado-multijugador");
		estadoMensaje.setHorizontalAlignment(SwingConstants.CENTER);
		int altoEstado = estadoMensaje.getPreferredSize().height;
		estadoMensaje.setPreferredSize(new Dimension(0, altoEstado));
		estadoMensaje.setMinimumSize(new Dimension(0, altoEstado));
		estadoMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, altoEstado));
	}

	private void vincularAcciones(Runnable alCrearPartida, Runnable alUnirseAPartida, Runnable alVolver) {
		host.addActionListener(evento -> alCrearPartida.run());
		cliente.addActionListener(evento -> alUnirseAPartida.run());
		volver.addActionListener(evento -> alVolver.run());
	}

	private JPanel construirContenido() {
		JPanel contenido = Tema.panelVertical();
		contenido.add(Box.createVerticalGlue());
		agregarIntroduccion(contenido);
		agregarAcciones(contenido);
		contenido.add(Box.createVerticalGlue());
		return contenido;
	}

	private void agregarIntroduccion(JPanel contenido) {
		JLabel icono = new JLabel(new IconoLineal(TipoIcono.ENGRANAJE, TAMANO_ICONO_CONSTRUCCION, Tema.ACENTO));
		contenido.add(Tema.centrar(icono));
		contenido.add(Box.createVerticalStrut(SEPARACION_ICONO));
		contenido.add(Tema.centrar(Tema.etiquetaPastilla("MULTIJUGADOR")));
		contenido.add(Box.createVerticalStrut(SEPARACION_INSIGNIA));
		contenido.add(Tema.centrar(crearTitulo()));
		contenido.add(Box.createVerticalStrut(SEPARACION_TITULO));
		contenido.add(Tema.centrar(crearDescripcion()));
	}

	private void agregarAcciones(JPanel contenido) {
		contenido.add(Box.createVerticalStrut(SEPARACION_DESCRIPCION));
		contenido.add(Tema.centrar(configurarBoton(host)));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		contenido.add(Tema.centrar(configurarBoton(cliente)));
		contenido.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		contenido.add(estadoMensaje);
		contenido.add(Box.createVerticalStrut(SEPARACION_BOTON_VOLVER));
		contenido.add(Tema.centrar(volver));
	}

	private JLabel crearTitulo() {
		JLabel titulo = Tema.etiqueta("Multijugador en Red (TCP)",
				Font.BOLD, Tema.TAMANO_TITULO_GRANDE, Tema.TINTA);
		titulo.setHorizontalAlignment(SwingConstants.CENTER);
		return titulo;
	}

	private JLabel crearDescripcion() {
		String html = "<html><div style='text-align:center;width:" + ANCHO_CONTENIDO
				+ "px;'>Elegí si querés alojar la partida esperando en el puerto "
				+ Constantes.PUERTO + " o conectarte como cliente.</div></html>";
		JLabel descripcion = Tema.etiqueta(html, Font.PLAIN, TAMANO_TEXTO_DESCRIPCION, Tema.TINTA_SUAVE);
		descripcion.setName("multijugador-descripcion");
		descripcion.setHorizontalAlignment(SwingConstants.CENTER);
		return descripcion;
	}

	private static BotonEstilo configurarBoton(BotonEstilo boton) {
		boton.setPreferredSize(new Dimension(ANCHO_BOTON, boton.getPreferredSize().height));
		boton.setMaximumSize(boton.getPreferredSize());
		return boton;
	}
}
