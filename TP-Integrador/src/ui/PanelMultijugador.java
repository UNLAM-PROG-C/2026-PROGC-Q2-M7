package ui;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import utils.Constantes;

final class PanelMultijugador extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int MARGEN_BOTON = 120;
	private static final String TEXTO_HOST = "Crear partida (Host)";
	private static final String TEXTO_CLIENTE = "Unirse a partida (Cliente)";
	private static final String DESCRIPCION = "<html><div style='text-align:center;width:460px;'>"
			+ "Elegí si querés alojar la partida esperando en el puerto " + Constantes.PUERTO
			+ " o conectarte como cliente.</div></html>";

	private final JButton host = Tema.boton(TEXTO_HOST, true);
	private final JButton cliente = Tema.boton(TEXTO_CLIENTE, true);
	private final JButton volver = Tema.boton("Volver / Cancelar", false);
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN, Tema.TAMANO_TEXTO, Tema.TINTA);

	PanelMultijugador(Runnable alCrearPartida, Runnable alUnirseAPartida, Runnable alVolver) {
		super(new GridBagLayout());
		setName("multijugador");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN, Tema.MARGEN, Tema.MARGEN, Tema.MARGEN));
		construirContenido(alCrearPartida, alUnirseAPartida, alVolver);
	}

	void ocupado(boolean ocupado) {
		host.setEnabled(!ocupado);
		cliente.setEnabled(!ocupado);
		host.setText(ocupado ? "Conectando…" : TEXTO_HOST);
		cliente.setText(ocupado ? "Conectando…" : TEXTO_CLIENTE);
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

	private void construirContenido(Runnable alCrear, Runnable alUnirse, Runnable alVolver) {
		agregar(crearInsignia(), Tema.ESPACIO, 0);
		agregar(Tema.etiqueta("Multijugador en Red (TCP)", Font.BOLD, Tema.TAMANO_TITULO, Tema.TINTA), Tema.ESPACIO, 0);
		JLabel desc = Tema.etiqueta(DESCRIPCION, Font.PLAIN, Tema.TAMANO_SUBTITULO, Tema.TINTA_SUAVE);
		desc.setName("multijugador-descripcion");
		agregar(desc, Tema.MARGEN, 0);
		agregar(boton(host, "crear-host", alCrear), Tema.ESPACIO, MARGEN_BOTON);
		agregar(boton(cliente, "unirse-cliente", alUnirse), Tema.ESPACIO, MARGEN_BOTON);
		estadoMensaje.setName("estado-multijugador");
		estadoMensaje.setHorizontalAlignment(JLabel.CENTER);
		agregar(estadoMensaje, Tema.ESPACIO, 0);
		agregar(boton(volver, "multijugador-volver", alVolver), 0, 0);
	}

	private void agregar(JComponent comp, int abajo, int margenH) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weightx = comp == estadoMensaje ? 1 : 0;
		gbc.fill = (comp == estadoMensaje || margenH > 0) ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		gbc.insets = new Insets(0, margenH, abajo, margenH);
		add(comp, gbc);
	}

	private static JButton boton(JButton boton, String nombre, Runnable accion) {
		boton.setName(nombre);
		boton.addActionListener(e -> accion.run());
		return boton;
	}

	private static JLabel crearInsignia() {
		JLabel tag = Tema.etiqueta("MULTIJUGADOR", Font.BOLD, Tema.TAMANO_CHICO, Tema.ACENTO);
		tag.setOpaque(true);
		tag.setBackground(Tema.VISITADA);
		tag.setBorder(BorderFactory.createEmptyBorder(Tema.ESPACIO / 2, Tema.ESPACIO, Tema.ESPACIO / 2, Tema.ESPACIO));
		return tag;
	}
}
