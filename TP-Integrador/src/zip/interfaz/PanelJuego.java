package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import zip.dominio.Direccion;
import zip.dominio.EstadoJuego;
import zip.dominio.Nivel;

final class PanelJuego {
	private final JPanel panel = new JPanel(new BorderLayout(Estilo.ESPACIO, Estilo.ESPACIO));
	private final PanelTablero tablero = new PanelTablero();
	private final JLabel progreso = Estilo.etiqueta(" ", Estilo.TAMANO_TEXTO);
	private final JLabel siguiente = Estilo.etiqueta(" ", Estilo.TAMANO_TEXTO);
	private final JLabel estadoMensaje = Estilo.etiqueta(" ", Estilo.TAMANO_PEQUENO);
	private final JLabel victoria = Estilo.etiqueta(" ", Estilo.TAMANO_TEXTO);
	private final JLabel etiquetaControles = Estilo.etiqueta(" ", Estilo.TAMANO_PEQUENO);
	private final JButton deshacer = new JButton("Deshacer");
	private final Consumer<Direccion> mover;
	private final Runnable accionDeshacer;
	private boolean jugando;

	PanelJuego(Consumer<Direccion> mover, Runnable accionDeshacer, Runnable reiniciar, Runnable volver) {
		this.mover = mover;
		this.accionDeshacer = accionDeshacer;
		tablero.setFocusable(true);
		Estilo.decorar(panel);
		panel.setName("juego");
		panel.add(encabezado(), BorderLayout.NORTH);
		panel.add(tablero, BorderLayout.CENTER);
		panel.add(pie(reiniciar, volver), BorderLayout.SOUTH);
		nombrarEtiquetas();
	}

	private void nombrarEtiquetas() {
		progreso.setName("progreso");
		siguiente.setName("siguiente");
		estadoMensaje.setName("estadoMensaje");
		victoria.setName("victoria");
	}

	private JPanel encabezado() {
		JPanel encabezado = new JPanel(new GridLayout(0, 1, 0, Estilo.ESPACIO / 2));
		encabezado.setOpaque(false);
		encabezado.add(Estilo.etiqueta("ZIP", Estilo.TAMANO_TITULO));
		encabezado.add(progreso);
		encabezado.add(siguiente);
		encabezado.add(etiquetaControles);
		return encabezado;
	}

	private JPanel pie(Runnable reiniciar, Runnable volver) {
		JPanel pie = new JPanel(new GridLayout(0, 1, 0, Estilo.ESPACIO / 2));
		JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, Estilo.ESPACIO, 0));
		pie.setOpaque(false);
		botones.setOpaque(false);
		deshacer.setName("deshacer");
		deshacer.addActionListener(evento -> accionDeshacer.run());
		botones.add(deshacer);
		botones.add(boton("Reiniciar", "reiniciar", reiniciar));
		botones.add(boton("Volver", "volver", volver));
		pie.add(victoria);
		pie.add(estadoMensaje);
		pie.add(botones);
		return pie;
	}

	private JButton boton(String texto, String nombre, Runnable accion) {
		JButton boton = new JButton(texto);
		boton.setName(nombre);
		boton.addActionListener(evento -> accion.run());
		return boton;
	}

	void controles(Controles controles) {
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		panel.getActionMap().clear();
		for (Direccion direccion : Direccion.values()) {
			vincular("mover-" + direccion, controles.tecla(direccion), () -> {
				if (jugando) {
					mover.accept(direccion);
				}
			});
		}
		vincular("deshacer", KeyEvent.VK_BACK_SPACE, () -> deshacer.doClick());
		etiquetaControles.setText(controles + " para moverte · Backspace para deshacer");
	}

	private void vincular(String nombre, int tecla, Runnable accion) {
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(tecla, 0), nombre);
		panel.getActionMap().put(nombre, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent evento) {
				accion.run();
			}
		});
	}

	void mostrar(EstadoJuego estadoJuego, String mensaje) {
		jugando = !estadoJuego.gano();
		tablero.mostrar(estadoJuego);
		progreso.setText("Celdas recorridas: " + estadoJuego.visitadas() + "/" + Nivel.CANTIDAD_CELDAS);
		siguiente.setText(estadoJuego.gano() ? "Todos los números conectados."
				: "Próximo número: " + estadoJuego.proximoNumero());
		victoria.setText(estadoJuego.gano() ? "¡Nivel completado! Recorriste el 100% del tablero." : " ");
		deshacer.setEnabled(jugando && estadoJuego.visitadas() > 1);
		mensaje(mensaje);
	}

	void mensaje(String mensaje) {
		estadoMensaje.setText(mensaje);
	}

	JPanel componente() {
		return panel;
	}

	void enfocarEntrada() {
		tablero.requestFocusInWindow();
	}
}
