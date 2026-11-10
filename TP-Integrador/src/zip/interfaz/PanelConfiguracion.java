package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.function.BiConsumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import zip.dominio.Nivel;

final class PanelConfiguracion {
	private static final int PUNTOS_PREDETERMINADOS = 6;
	private static final int FILAS_FORMULARIO = 7;
	private final JPanel panel = new JPanel(new BorderLayout(Estilo.ESPACIO, Estilo.ESPACIO));
	private final JComboBox<Controles> controles = new JComboBox<>(Controles.values());
	private final JSpinner puntos = new JSpinner(
			new SpinnerNumberModel(PUNTOS_PREDETERMINADOS, Nivel.PUNTOS_MINIMOS, Nivel.PUNTOS_MAXIMOS, 1));
	private final JButton jugar = new JButton("Jugar");
	private final javax.swing.JLabel estadoMensaje = Estilo.etiqueta(" ", Estilo.TAMANO_PEQUENO);

	PanelConfiguracion(BiConsumer<Controles, Integer> iniciar) {
		Estilo.decorar(panel);
		panel.add(encabezado(), BorderLayout.NORTH);
		panel.add(formulario(), BorderLayout.CENTER);
		panel.add(estadoMensaje, BorderLayout.SOUTH);
		controles.setName("controles");
		puntos.setName("puntos");
		jugar.setName("iniciar");
		estadoMensaje.setName("setup-estadoMensaje");
		jugar.addActionListener(evento -> comenzar(iniciar));
	}

	private JPanel encabezado() {
		JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, Estilo.ESPACIO));
		Estilo.decorar(encabezado);
		encabezado.add(Estilo.etiqueta("ZIP", Estilo.TAMANO_TITULO));
		encabezado.add(Estilo.etiqueta("Conectá los números. Completá el tablero.", Estilo.TAMANO_TEXTO));
		return encabezado;
	}

	private JPanel formulario() {
		JPanel formulario = new JPanel(new GridLayout(FILAS_FORMULARIO, 1, Estilo.ESPACIO, Estilo.ESPACIO));
		Estilo.decorar(formulario);
		formulario.add(Estilo.etiqueta("Controles", Estilo.TAMANO_TEXTO));
		formulario.add(controles);
		formulario.add(Estilo.etiqueta("Puntos numerados (2 a 36)", Estilo.TAMANO_TEXTO));
		formulario.add(puntos);
		formulario.add(Estilo.etiqueta("Grilla 6 × 6 · sin diagonales ni celdas repetidas",
				Estilo.TAMANO_PEQUENO));
		formulario.add(Estilo.etiqueta("Más números no necesariamente significa más dificultad.",
				Estilo.TAMANO_PEQUENO));
		formulario.add(jugar);
		return formulario;
	}

	private void comenzar(BiConsumer<Controles, Integer> iniciar) {
		try {
			puntos.commitEdit();
			iniciar.accept(controles.getItemAt(controles.getSelectedIndex()),
					((Number) puntos.getValue()).intValue());
		} catch (ParseException excepcion) {
			error("Ingresá una cantidad entera entre 2 y 36.");
		}
	}

	void ocupado(boolean ocupado) {
		jugar.setEnabled(!ocupado);
		puntos.setEnabled(!ocupado);
		controles.setEnabled(!ocupado);
		jugar.setText(ocupado ? "Preparando…" : "Jugar");
		estadoMensaje.setForeground(Estilo.TINTA);
		estadoMensaje.setText(ocupado ? "Generando un tablero con solución." : " ");
	}

	void error(String mensaje) {
		ocupado(false);
		estadoMensaje.setForeground(Estilo.ERROR);
		estadoMensaje.setText(mensaje);
	}

	JPanel componente() {
		return panel;
	}
}
