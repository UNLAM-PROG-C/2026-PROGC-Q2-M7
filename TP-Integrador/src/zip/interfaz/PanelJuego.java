package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import zip.dominio.Direccion;
import zip.dominio.EstadoJuego;

final class PanelJuego {
	private final JPanel panel = new JPanel(new BorderLayout(0, Tema.SEPARACION_JUEGO));
	private final PanelTablero tablero = new PanelTablero();
	private final IndicadorInformacion celdas = new IndicadorInformacion("Celdas", "progreso");
	private final IndicadorInformacion siguiente = new IndicadorInformacion("Próximo número", "siguiente");
	private final BarraProgreso progreso = new BarraProgreso();
	private final JLabel estadoMensaje = etiqueta(" ", Tema.TAMANO_ESTADO, Tema.TINTA_SUAVE);
	private final JPanel teclasAyuda = horizontal();
	private final BotonSecundario deshacer = new BotonSecundario("Deshacer", TipoIcono.DESHACER);
	private final BotonSecundario reiniciar = new BotonSecundario("Reiniciar", TipoIcono.REINICIAR);
	private final Consumer<Direccion> mover;
	private final Runnable accionDeshacer;
	private boolean jugando;

	PanelJuego(Consumer<Direccion> mover, Runnable accionDeshacer,
			Runnable accionReiniciar, Runnable volver) {
		this.mover = mover;
		this.accionDeshacer = accionDeshacer;
		configurarPanel();
		panel.add(encabezado(volver), BorderLayout.NORTH);
		panel.add(tablero, BorderLayout.CENTER);
		panel.add(pie(accionReiniciar), BorderLayout.SOUTH);
	}

	private void configurarPanel() {
		tablero.setFocusable(true);
		panel.setName("juego");
		panel.setBackground(Tema.FONDO);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_JUEGO,
				Tema.RELLENO_HORIZONTAL_JUEGO, Tema.RELLENO_VERTICAL_JUEGO,
				Tema.RELLENO_HORIZONTAL_JUEGO));
		estadoMensaje.setName("estado-mensaje");
		deshacer.setName("deshacer");
		reiniciar.setName("reiniciar");
		deshacer.agregarEscucha(evento -> accionDeshacer.run());
	}

	private JPanel encabezado(Runnable accionVolver) {
		JPanel encabezado = vertical();
		encabezado.add(barraSuperior(accionVolver));
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		encabezado.add(chips());
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		encabezado.add(progreso);
		return encabezado;
	}

	private JPanel barraSuperior(Runnable accionVolver) {
		JPanel barra = new JPanel(new BorderLayout());
		barra.setOpaque(false);
		barra.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel titulo = etiqueta("ZIP", Tema.TAMANO_TITULO_JUEGO, Tema.TINTA);
		titulo.setFont(Tema.fuenteConEspaciado(Tema.TAMANO_TITULO_JUEGO, true,
				Tema.ESPACIADO_TITULO_JUEGO));
		BotonFantasma volver = new BotonFantasma("Volver", true);
		volver.setName("volver");
		volver.setPreferredSize(new Dimension(Tema.ANCHO_VOLVER_FANTASMA, Tema.ALTO_FANTASMA));
		volver.agregarEscucha(evento -> accionVolver.run());
		barra.add(titulo, BorderLayout.WEST);
		barra.add(volver, BorderLayout.EAST);
		return barra;
	}

	private JPanel chips() {
		JPanel chips = new JPanel(new GridLayout(1, 2, Tema.SEPARACION_INDICADORES, 0));
		chips.setOpaque(false);
		chips.setAlignmentX(Component.LEFT_ALIGNMENT);
		chips.add(celdas);
		chips.add(siguiente);
		chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_INDICADOR));
		return chips;
	}

	private JPanel pie(Runnable accionReiniciar) {
		JPanel pie = vertical();
		reiniciar.agregarEscucha(evento -> accionReiniciar.run());
		pie.add(centrado(teclasAyuda));
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_AYUDA));
		pie.add(centrado(etiqueta("Conectá los números en orden y recorré todas las celdas.",
				Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE)));
		pie.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		pie.add(centrado(estadoMensaje));
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		pie.add(centrado(acciones()));
		return pie;
	}

	private JPanel acciones() {
		JPanel acciones = horizontal();
		Dimension boton = new Dimension(Tema.ANCHO_BOTON_SECUNDARIO, Tema.ALTO_SECUNDARIO);
		deshacer.setPreferredSize(boton);
		reiniciar.setPreferredSize(boton);
		acciones.add(deshacer);
		acciones.add(Box.createHorizontalStrut(Tema.SEPARACION_ACCIONES));
		acciones.add(reiniciar);
		return acciones;
	}

	void controles(Controles controles) {
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		panel.getActionMap().clear();
		for (Direccion direccion : Direccion.values()) {
			vincular("mover-" + direccion, controles.tecla(direccion), () -> mover(direccion));
		}
		vincular("deshacer", KeyEvent.VK_BACK_SPACE, deshacer::hacerClic);
		reconstruirAyuda(controles);
	}

	private void reconstruirAyuda(Controles controles) {
		teclasAyuda.removeAll();
		teclasAyuda.add(teclasDireccion(controles));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TARJETAS));
		teclasAyuda.add(new TeclaVisual("Backspace",
				Tema.ANCHO_TECLA_BACKSPACE, Tema.TAMANO_TECLA_JUEGO));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
		teclasAyuda.add(etiqueta("= deshacer", Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE));
		teclasAyuda.revalidate();
		teclasAyuda.repaint();
	}

	private JPanel teclasDireccion(Controles controles) {
		JPanel teclas = horizontal();
		JComponent[] valores = teclasJuego(controles);
		for (int indice = 0; indice < valores.length; indice++) {
			if (indice > 0) {
				teclas.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
			}
			teclas.add(valores[indice]);
		}
		return teclas;
	}

	private JComponent[] teclasJuego(Controles controles) {
		if (controles == Controles.FLECHAS) {
			return new JComponent[] {teclaJuego(TipoIcono.FLECHA_ARRIBA), teclaJuego(TipoIcono.FLECHA_IZQUIERDA),
				teclaJuego(TipoIcono.FLECHA_ABAJO), teclaJuego(TipoIcono.FLECHA_DERECHA)};
		}
		return new JComponent[] {teclaJuego("W"), teclaJuego("A"), teclaJuego("S"), teclaJuego("D")};
	}

	private JComponent teclaJuego(TipoIcono icono) {
		return new TeclaVisual(icono, Tema.TAMANO_TECLA_JUEGO);
	}

	private JComponent teclaJuego(String texto) {
		return new TeclaVisual(texto, Tema.TAMANO_TECLA_JUEGO, Tema.TAMANO_TECLA_JUEGO);
	}

	private void mover(Direccion direccion) {
		if (jugando) {
			mover.accept(direccion);
		}
	}

	private void vincular(String nombre, int tecla, Runnable accion) {
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(tecla, 0), nombre);
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
		celdas.establecerValor(estadoJuego.visitadas() + "/" + estadoJuego.nivel().cantidadCeldas());
		siguiente.establecerValor(estadoJuego.gano()
				? "Completado" : Integer.toString(estadoJuego.proximoNumero()));
		progreso.establecerValor((double) estadoJuego.visitadas()
				/ estadoJuego.nivel().cantidadCeldas());
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

	private static JPanel vertical() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private static JPanel horizontal() {
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		return panel;
	}

	private static JPanel centrado(Component componente) {
		JPanel envoltorio = horizontal();
		envoltorio.add(Box.createHorizontalGlue());
		envoltorio.add(componente);
		envoltorio.add(Box.createHorizontalGlue());
		return envoltorio;
	}

	private static JLabel etiqueta(String texto, int tamano, java.awt.Color color) {
		JLabel etiqueta = new JLabel(texto, SwingConstants.LEFT);
		etiqueta.setFont(Tema.fuente(tamano, false));
		etiqueta.setForeground(color);
		return etiqueta;
	}
}
