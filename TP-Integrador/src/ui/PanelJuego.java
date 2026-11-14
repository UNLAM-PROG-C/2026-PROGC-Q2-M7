package ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import controller.ActualizacionVista;
import model.Direccion;
import model.EstadoJuego;

final class PanelJuego extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int RELLENO_HORIZONTAL_JUEGO = 40;
	private static final int TAMANO_TITULO_JUEGO = 28;
	private static final float ESPACIADO_TITULO_JUEGO = 0.14f;
	private static final int ANCHO_VOLVER_FANTASMA = 172;
	private static final int ALTO_INDICADOR = 58;
	private static final int RELLENO_HORIZONTAL_INDICADOR = 16;
	private static final int RELLENO_VERTICAL_INDICADOR = 10;
	private static final int TAMANO_ETIQUETA_INDICADOR = 13;
	private static final int TAMANO_VALOR_INDICADOR = 20;
	private static final int ALTO_PROGRESO = 8;
	private static final int RADIO_PROGRESO = 4;
	private static final int ANCHO_BOTON_SECUNDARIO = 170;
	private static final int ANCHO_TECLA_BACKSPACE = 104;
	private static final int PROGRESO_MAXIMO = 100;

	private final PanelTablero tablero = new PanelTablero();
	private final ChipInformacion celdas = new ChipInformacion("Celdas", "progreso");
	private final ChipInformacion siguiente = new ChipInformacion("Próximo número", "siguiente");
	private final BarraProgresoJuego progreso = new BarraProgresoJuego();
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN,
			Tema.TAMANO_ESTADO, Tema.TINTA_SUAVE);
	private final JPanel teclasAyuda = Tema.panelHorizontal();
	private final BotonEstilo deshacer = BotonEstilo.secundario("Deshacer", TipoIcono.DESHACER);
	private final BotonEstilo reiniciar = BotonEstilo.secundario("Reiniciar", TipoIcono.REINICIAR);
	private final transient Consumer<Direccion> mover;
	private Controles controlesActuales;
	private boolean jugando;

	PanelJuego(Consumer<Direccion> mover, Runnable accionDeshacer,
			Runnable accionReiniciar, Runnable volver) {
		super(new BorderLayout(0, Tema.SEPARACION_GRANDE));
		this.mover = mover;
		configurarPanel(accionDeshacer, accionReiniciar);
		add(encabezado(volver), BorderLayout.NORTH);
		add(tablero, BorderLayout.CENTER);
		add(pie(), BorderLayout.SOUTH);
		controles(Controles.FLECHAS);
	}

	private void configurarPanel(Runnable accionDeshacer, Runnable accionReiniciar) {
		tablero.setFocusable(true);
		setName("juego");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.MARGEN_PANEL_JUEGO,
				RELLENO_HORIZONTAL_JUEGO, Tema.MARGEN_PANEL_JUEGO, RELLENO_HORIZONTAL_JUEGO));
		estadoMensaje.setName("estado-mensaje");
		configurarBoton(deshacer, "deshacer", accionEnJuego(accionDeshacer));
		configurarBoton(reiniciar, "reiniciar", accionEnJuego(accionReiniciar));
	}

	private Runnable accionEnJuego(Runnable accion) {
		return () -> {
			accion.run();
			enfocarEntrada();
		};
	}

	private void configurarBoton(BotonEstilo boton, String nombre, Runnable accion) {
		boton.setName(nombre);
		boton.setFocusable(true);
		boton.addActionListener(evento -> accion.run());
	}

	private JPanel encabezado(Runnable accionVolver) {
		JPanel encabezado = Tema.panelVertical();
		encabezado.add(barraSuperior(accionVolver));
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		encabezado.add(chips());
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		encabezado.add(progreso);
		return encabezado;
	}

	private JPanel barraSuperior(Runnable accionVolver) {
		JPanel barra = new JPanel(new BorderLayout());
		barra.setOpaque(false);
		barra.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel titulo = Tema.etiqueta("ZIP", Font.BOLD, TAMANO_TITULO_JUEGO, Tema.TINTA);
		titulo.setFont(Tema.fuenteConEspaciado(Font.BOLD, TAMANO_TITULO_JUEGO,
				ESPACIADO_TITULO_JUEGO));
		barra.add(titulo, BorderLayout.WEST);
		barra.add(crearBotonVolver(accionVolver), BorderLayout.EAST);
		return barra;
	}

	private BotonEstilo crearBotonVolver(Runnable accionVolver) {
		BotonEstilo volver = BotonEstilo.fantasma("Desconectar / Menú",
				TipoIcono.FLECHA_IZQUIERDA);
		configurarBoton(volver, "volver", accionVolver);
		int ancho = Math.max(ANCHO_VOLVER_FANTASMA, volver.getPreferredSize().width);
		volver.setPreferredSize(new Dimension(ancho, Tema.ALTO_FANTASMA));
		return volver;
	}

	private JPanel chips() {
		JPanel chips = new JPanel(new GridLayout(1, 2, Tema.SEPARACION_MEDIA, 0));
		chips.setOpaque(false);
		chips.setAlignmentX(Component.LEFT_ALIGNMENT);
		chips.add(celdas);
		chips.add(siguiente);
		chips.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_INDICADOR));
		return chips;
	}

	private JPanel pie() {
		JPanel pie = Tema.panelVertical();
		pie.add(Tema.centrar(teclasAyuda));
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		pie.add(Tema.centrar(Tema.etiqueta(
				"Conectá los números en orden y recorré todas las celdas.",
				Font.PLAIN, Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE)));
		pie.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		pie.add(Tema.centrar(estadoMensaje));
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		pie.add(Tema.centrar(acciones()));
		return pie;
	}

	private JPanel acciones() {
		JPanel acciones = Tema.panelHorizontal();
		Dimension boton = new Dimension(ANCHO_BOTON_SECUNDARIO, Tema.ALTO_SECUNDARIO);
		deshacer.setPreferredSize(boton);
		reiniciar.setPreferredSize(boton);
		acciones.add(deshacer);
		acciones.add(Box.createHorizontalStrut(Tema.SEPARACION_MEDIA));
		acciones.add(reiniciar);
		return acciones;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			revalidate();
		}
	}

	void controles(Controles controles) {
		if (controles == controlesActuales) {
			return;
		}
		this.controlesActuales = controles;
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).clear();
		getActionMap().clear();
		for (Direccion direccion : Direccion.values()) {
			vincular("mover-" + direccion, controles.tecla(direccion),
					() -> mover(direccion));
		}
		vincular("deshacer", KeyEvent.VK_BACK_SPACE, deshacer::doClick);
		reconstruirAyuda(controles);
	}

	private void reconstruirAyuda(Controles controles) {
		teclasAyuda.removeAll();
		teclasAyuda.add(teclasDireccion(controles));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TARJETAS));
		teclasAyuda.add(new TeclaVisual("Backspace", ANCHO_TECLA_BACKSPACE,
				Tema.TAMANO_TECLA_JUEGO));
		teclasAyuda.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
		teclasAyuda.add(Tema.etiqueta("= deshacer", Font.PLAIN,
				Tema.TAMANO_TEXTO_AYUDA, Tema.TINTA_SUAVE));
		teclasAyuda.revalidate();
		teclasAyuda.repaint();
	}

	private JPanel teclasDireccion(Controles controles) {
		JPanel teclas = Tema.panelHorizontal();
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
			return new JComponent[] {
				new TeclaVisual(TipoIcono.FLECHA_ARRIBA, Tema.TAMANO_TECLA_JUEGO),
				new TeclaVisual(TipoIcono.FLECHA_IZQUIERDA, Tema.TAMANO_TECLA_JUEGO),
				new TeclaVisual(TipoIcono.FLECHA_ABAJO, Tema.TAMANO_TECLA_JUEGO),
				new TeclaVisual(TipoIcono.FLECHA_DERECHA, Tema.TAMANO_TECLA_JUEGO)
			};
		}
		return new JComponent[] {teclaLetra("W"), teclaLetra("A"),
			teclaLetra("S"), teclaLetra("D")};
	}

	private JComponent teclaLetra(String texto) {
		return new TeclaVisual(texto, Tema.TAMANO_TECLA_JUEGO, Tema.TAMANO_TECLA_JUEGO);
	}

	private void mover(Direccion direccion) {
		if (jugando) {
			mover.accept(direccion);
		}
	}

	private void vincular(String nombre, int tecla, Runnable accion) {
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(tecla, 0), nombre);
		getActionMap().put(nombre, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent evento) {
				if (entradaHabilitada()) {
					accion.run();
				}
			}
		});
	}

	private boolean entradaHabilitada() {
		JRootPane raiz = SwingUtilities.getRootPane(this);
		return raiz == null || raiz.getGlassPane() == null
				|| !raiz.getGlassPane().isVisible();
	}

	void mostrar(ActualizacionVista act) {
		EstadoJuego estadoJuego = act.estadoJuego();
		jugando = !act.partidaFinalizada() && !estadoJuego.gano();
		tablero.mostrar(estadoJuego, act.manchadoPorTomate());
		celdas.establecerValor(estadoJuego.visitadas() + "/"
				+ estadoJuego.nivel().cantidadCeldas());
		siguiente.establecerValor(textoSiguiente(act, estadoJuego));
		progreso.setValue(calcularPorcentaje(estadoJuego));
		deshacer.setEnabled(jugando && estadoJuego.visitadas() > 1);
		estadoMensaje.setText(act.mensaje());
	}

	private String textoSiguiente(ActualizacionVista act, EstadoJuego estadoJuego) {
		String texto = estadoJuego.gano() ? "Completado"
				: Integer.toString(estadoJuego.proximoNumero());
		if (act.rondasObjetivo() > 1) {
			texto += " · " + act.rondasLocal() + "-" + act.rondasRival()
					+ " (" + act.rondasObjetivo() + ")";
		}
		return texto;
	}

	private int calcularPorcentaje(EstadoJuego estadoJuego) {
		return (int) Math.round(PROGRESO_MAXIMO
				* (double) estadoJuego.visitadas() / estadoJuego.nivel().cantidadCeldas());
	}

	void enfocarEntrada() {
		tablero.requestFocusInWindow();
	}

	private static final class ChipInformacion extends JPanel {
		private static final long serialVersionUID = 1L;
		private final JLabel valor;

		ChipInformacion(String titulo, String nombreValor) {
			super(new BorderLayout(Tema.SEPARACION_CHICA, 0));
			this.valor = crearValor(nombreValor);
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(RELLENO_VERTICAL_INDICADOR,
					RELLENO_HORIZONTAL_INDICADOR, RELLENO_VERTICAL_INDICADOR,
					RELLENO_HORIZONTAL_INDICADOR));
			setPreferredSize(new Dimension(0, ALTO_INDICADOR));
			add(Tema.etiqueta(titulo, Font.PLAIN, TAMANO_ETIQUETA_INDICADOR,
					Tema.TINTA_SUAVE), BorderLayout.WEST);
			add(valor, BorderLayout.EAST);
		}

		private static JLabel crearValor(String nombreValor) {
			JLabel etiqueta = Tema.etiqueta("", Font.BOLD, TAMANO_VALOR_INDICADOR,
					Tema.TINTA);
			etiqueta.setName(nombreValor);
			etiqueta.setHorizontalAlignment(SwingConstants.RIGHT);
			return etiqueta;
		}

		void establecerValor(String texto) {
			valor.setText(texto);
		}

		@Override
		protected void paintComponent(Graphics graficos) {
			Graphics2D dibujo = (Graphics2D) graficos.create();
			try {
				Tema.preparar(dibujo);
				Tema.pintarCajaRedondeada(dibujo, 0, 0, getWidth(), getHeight(),
						Tema.RADIO_MEDIO, Tema.TARJETA, Tema.BORDE, Tema.GROSOR_BORDE);
			} finally {
				dibujo.dispose();
			}
			super.paintComponent(graficos);
		}
	}

	private static final class BarraProgresoJuego extends JProgressBar {
		private static final long serialVersionUID = 1L;

		BarraProgresoJuego() {
			super(0, PROGRESO_MAXIMO);
			setOpaque(false);
			setBorderPainted(false);
			setPreferredSize(new Dimension(0, ALTO_PROGRESO));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_PROGRESO));
			setAlignmentX(Component.LEFT_ALIGNMENT);
		}

		@Override
		protected void paintComponent(Graphics graficos) {
			Graphics2D dibujo = (Graphics2D) graficos.create();
			try {
				pintarBarra(dibujo);
			} finally {
				dibujo.dispose();
			}
		}

		private void pintarBarra(Graphics2D dibujo) {
			Tema.preparar(dibujo);
			int arco = RADIO_PROGRESO * 2;
			Tema.pintarCajaRedondeada(dibujo, 0, 0, getWidth(), getHeight(),
					arco, Tema.BORDE, null, 0);
			int ancho = (int) Math.round(getWidth() * getPercentComplete());
			if (ancho > 0) {
				Tema.pintarCajaRedondeada(dibujo, 0, 0, ancho, getHeight(),
						arco, Tema.ACENTO, null, 0);
			}
		}
	}
}
