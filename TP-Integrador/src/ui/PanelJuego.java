package ui;

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
import controller.ActualizacionVista;
import model.Direccion;
import model.EstadoJuego;

final class PanelJuego {
	private static final String AYUDA_MOUSE = "Arrastrá desde el punto y soltá";
	private static final String AYUDA_DESHACER = "= deshacer";
	private static final String AYUDA_DESHACER_MOUSE = "= deshacer · + click en el camino: volver";
	private static final String DESHACER = "deshacer";
	private final JPanel panel = new JPanel(new BorderLayout(0, Tema.SEPARACION_JUEGO));
	private final PanelTablero tablero = new PanelTablero();
	private final IndicadorInformacion celdas = new IndicadorInformacion("Celdas", "progreso");
	private final IndicadorInformacion siguiente = new IndicadorInformacion("Próximo número", "siguiente");
	private final BarraProgreso progreso = new BarraProgreso();
	private final JLabel estadoMensaje = etiqueta(" ", Tema.TAMANO_ESTADO, Tema.TINTA_SUAVE);
	private final JPanel teclasAyuda = horizontal();
	private final BotonSecundario deshacer = new BotonSecundario("Deshacer", TipoIcono.DESHACER);
	private final BotonSecundario reiniciar = new BotonSecundario("Reiniciar", TipoIcono.REINICIAR);
	private final AdaptadorMouse adaptadorMouse = new AdaptadorMouse(tablero, this::mover,
			this::deshacerPaso);
	private final Consumer<Direccion> mover;
	private final Runnable accionDeshacer;
	private boolean jugando;

	PanelJuego(Consumer<Direccion> mover, Runnable accionDeshacer,
			Runnable accionReiniciar, Runnable volver) {
		this.mover = mover;
		this.accionDeshacer = accionDeshacer;
		configurarPanel();
		adaptadorMouse.instalar();
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
		BotonFantasma volver = new BotonFantasma("Desconectar / Menú", true);
		volver.setName("volver");
		volver.setPreferredSize(new Dimension(Tema.ANCHO_VOLVER_FANTASMA + 60, Tema.ALTO_FANTASMA));
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
		if (!controles.usaMouse()) {
			vincularDirecciones(controles);
		}
		if (controles.usaMouse()) {
			vincularBorrarMouse();
		} else {
			vincular(DESHACER, KeyEvent.VK_BACK_SPACE, deshacer::hacerClic);
		}
		adaptadorMouse.habilitar(controles.usaMouse());
		reconstruirAyuda(controles);
	}

	/** En modo mouse Backspace es modificador: con click edita, sola deshace un paso al soltarla. */
	private void vincularBorrarMouse() {
		vincular(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false), DESHACER,
				adaptadorMouse::presionarBorrar);
		vincular(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, true), DESHACER + "-soltar",
				this::soltarBorrar);
	}

	private void soltarBorrar() {
		if (!adaptadorMouse.soltarBorrar()) {
			deshacer.hacerClic();
		}
	}

	private void deshacerPaso() {
		if (jugando) {
			accionDeshacer.run();
		}
	}

	private void vincularDirecciones(Controles controles) {
		for (Direccion direccion : Direccion.values()) {
			vincular("mover-" + direccion, controles.tecla(direccion), () -> mover(direccion));
		}
	}

	private void reconstruirAyuda(Controles controles) {
		teclasAyuda.removeAll();
		teclasAyuda.add(teclasDireccion(controles));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TARJETAS));
		teclasAyuda.add(new TeclaVisual("Backspace",
				Tema.ANCHO_TECLA_BACKSPACE, Tema.TAMANO_TECLA_JUEGO));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
		String deshacerAyuda = controles.usaMouse() ? AYUDA_DESHACER_MOUSE : AYUDA_DESHACER;
		teclasAyuda.add(etiqueta(deshacerAyuda, Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE));
		teclasAyuda.revalidate();
		teclasAyuda.repaint();
	}

	private JPanel teclasDireccion(Controles controles) {
		JPanel teclas = horizontal();
		if (controles.usaMouse()) {
			teclas.add(etiqueta(AYUDA_MOUSE, Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE));
			return teclas;
		}
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
		vincular(KeyStroke.getKeyStroke(tecla, 0), nombre, accion);
	}

	private void vincular(KeyStroke tecla, String nombre, Runnable accion) {
		panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(tecla, nombre);
		panel.getActionMap().put(nombre, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent evento) {
				accion.run();
			}
		});
	}

	void mostrar(ActualizacionVista act) {
		EstadoJuego estadoJuego = act.estadoJuego();
		if (estadoJuego == null) {
			mensaje(act.mensaje());
			return;
		}
		jugando = !act.partidaFinalizada() && !estadoJuego.gano();
		tablero.mostrar(estadoJuego, act.manchadoPorTomate());
		celdas.establecerValor(estadoJuego.visitadas() + "/" + estadoJuego.nivel().cantidadCeldas());
		String textoSiguiente = estadoJuego.gano()
				? "Completado" : Integer.toString(estadoJuego.proximoNumero());
		if (act.rondasObjetivo() > 1) {
			textoSiguiente += " · " + act.rondasLocal() + "-" + act.rondasRival() + " (" + act.rondasObjetivo() + ")";
		}
		siguiente.establecerValor(textoSiguiente);
		progreso.establecerValor((double) estadoJuego.visitadas()
				/ estadoJuego.nivel().cantidadCeldas());
		deshacer.setEnabled(jugando && estadoJuego.visitadas() > 1);
		mensaje(act.mensaje());
	}

	void mostrar(EstadoJuego estadoJuego, String mensaje, boolean manchadoPorTomate) {
		mostrar(new ActualizacionVista(estadoJuego, mensaje, manchadoPorTomate));
	}

	void mostrar(EstadoJuego estadoJuego, String mensaje) {
		mostrar(estadoJuego, mensaje, false);
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
