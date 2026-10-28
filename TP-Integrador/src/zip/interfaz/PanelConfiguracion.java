package zip.interfaz;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import zip.aplicacion.ModoJuego;
import zip.dominio.Dificultad;

final class PanelConfiguracion {
	private static final String RESUMEN = "resumen";
	private static final String ESTADO_MENSAJE = "estadoMensaje";
	private final JPanel panel = panelVertical();
	private final Map<ModoJuego, TarjetaModoJuego> modos = new EnumMap<>(ModoJuego.class);
	private final Map<Controles, TarjetaControles> controles = new EnumMap<>(Controles.class);
	private final Map<Dificultad, TarjetaDificultad> dificultades = new EnumMap<>(Dificultad.class);
	private final JLabel resumen = etiqueta("", Tema.TAMANO_RESUMEN, false, Tema.TINTA_SUAVE);
	private final JLabel estadoMensaje = etiqueta(" ", Tema.TAMANO_ESTADO, false, Tema.TINTA);
	private final CardLayout tarjetasMensaje = new CardLayout();
	private final JPanel mensaje = new JPanel(tarjetasMensaje);
	private final BotonPrincipal jugar = new BotonPrincipal("Jugar");
	private final IniciadorJuego iniciar;
	private final Consumer<ModoJuego> modoSeleccionado;
	private Controles controlesSeleccionados = Controles.FLECHAS;
	private Dificultad dificultadSeleccionada = Dificultad.MEDIA;
	private ModoJuego modoSeleccionadoActual = ModoJuego.UN_JUGADOR;

	PanelConfiguracion(IniciadorJuego iniciar, Consumer<ModoJuego> modoSeleccionado) {
		this.iniciar = iniciar;
		this.modoSeleccionado = modoSeleccionado;
		configurarPanel();
		crearOpciones();
		construirDisposicion();
		actualizarSeleccion();
	}

	private void configurarPanel() {
		panel.setName("configuracion");
		panel.setBackground(Tema.FONDO);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU, Tema.RELLENO_VERTICAL_MENU,
				Tema.RELLENO_HORIZONTAL_MENU));
		jugar.setName("iniciar");
		estadoMensaje.setName("estado-configuracion");
		resumen.setName("resumen-seleccion");
		jugar.agregarEscucha(evento -> iniciar.iniciar(controlesSeleccionados, dificultadSeleccionada));
	}

	private void crearOpciones() {
		GrupoSeleccion grupoModo = new GrupoSeleccion();
		agregarModo(grupoModo, new TarjetaModoJuego(ModoJuego.UN_JUGADOR, "Un jugador", "A tu ritmo."));
		agregarModo(grupoModo, new TarjetaModoJuego(ModoJuego.MULTIJUGADOR,
				"Multijugador", "Jugá con otras personas."));
		GrupoSeleccion grupoControles = new GrupoSeleccion();
		for (Controles valor : Controles.values()) {
			agregarControl(grupoControles, new TarjetaControles(valor));
		}
		GrupoSeleccion grupoDificultad = new GrupoSeleccion();
		for (Dificultad valor : Dificultad.values()) {
			agregarDificultad(grupoDificultad, new TarjetaDificultad(valor));
		}
	}

	private void construirDisposicion() {
		panel.add(encabezado());
		panel.add(seccion("MODO DE JUEGO", grilla(modos.values().toArray(new JComponent[0]))));
		panel.add(Box.createVerticalStrut(Tema.SEPARACION_SECCION));
		panel.add(seccion("CONTROLES", grilla(controles.values().toArray(new JComponent[0]))));
		panel.add(Box.createVerticalStrut(Tema.SEPARACION_SECCION));
		panel.add(seccion("DIFICULTAD", grilla(dificultades.values().toArray(new JComponent[0]))));
		panel.add(Box.createVerticalGlue());
		panel.add(pie());
	}

	private void agregarModo(GrupoSeleccion grupo, TarjetaModoJuego tarjeta) {
		tarjeta.setName(tarjeta.modo() == ModoJuego.UN_JUGADOR ? "modo-un-jugador" : "modo-multijugador");
		tarjeta.agregarEscucha(evento -> elegirModo(tarjeta.modo()));
		grupo.agregar(tarjeta);
		modos.put(tarjeta.modo(), tarjeta);
	}

	private void agregarControl(GrupoSeleccion grupo, TarjetaControles tarjeta) {
		tarjeta.setName(tarjeta.controles() == Controles.FLECHAS ? "controles-flechas" : "controles-wasd");
		tarjeta.agregarEscucha(evento -> elegirControles(tarjeta.controles()));
		grupo.agregar(tarjeta);
		controles.put(tarjeta.controles(), tarjeta);
	}

	private void agregarDificultad(GrupoSeleccion grupo, TarjetaDificultad tarjeta) {
		tarjeta.setName("dificultad-" + tarjeta.dificultad().name().toLowerCase(java.util.Locale.ROOT));
		tarjeta.agregarEscucha(evento -> elegirDificultad(tarjeta.dificultad()));
		grupo.agregar(tarjeta);
		dificultades.put(tarjeta.dificultad(), tarjeta);
	}

	private JPanel encabezado() {
		JPanel encabezado = panelVertical();
		JLabel titulo = etiqueta("ZIP", Tema.TAMANO_TITULO_MENU, true, Tema.TINTA);
		titulo.setFont(Tema.fuenteConEspaciado(Tema.TAMANO_TITULO_MENU, true, Tema.ESPACIADO_TITULO));
		JLabel subtitulo = etiqueta("Conectá los números. Completá el tablero.",
				Tema.TAMANO_SUBTITULO_MENU, false, Tema.TINTA_SUAVE);
		encabezado.add(titulo);
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		encabezado.add(subtitulo);
		encabezado.add(Box.createVerticalGlue());
		return ajustarTamano(encabezado, encabezado.getPreferredSize().height);
	}

	private JPanel seccion(String titulo, JPanel tarjetas) {
		JPanel seccion = panelVertical();
		JLabel etiqueta = etiqueta(titulo, Tema.TAMANO_TITULO_SECCION, true, Tema.TINTA_SUAVE);
		etiqueta.setFont(Tema.fuenteConEspaciado(Tema.TAMANO_TITULO_SECCION, true, Tema.ESPACIADO_SECCION));
		seccion.add(etiqueta);
		seccion.add(Box.createVerticalStrut(Tema.SEPARACION_TITULO_SECCION));
		seccion.add(tarjetas);
		return ajustarTamano(seccion, seccion.getPreferredSize().height);
	}

	private JPanel grilla(JComponent[] tarjetas) {
		JPanel grilla = new JPanel(new java.awt.GridLayout(1, tarjetas.length, Tema.SEPARACION_TARJETAS, 0));
		grilla.setOpaque(false);
		grilla.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (JComponent tarjeta : tarjetas) {
			grilla.add(tarjeta);
		}
		return grilla;
	}

	private JPanel pie() {
		JPanel pie = panelVertical();
		configurarMensaje();
		jugar.setAlignmentX(Component.LEFT_ALIGNMENT);
		jugar.setMaximumSize(new Dimension(Integer.MAX_VALUE, jugar.getPreferredSize().height));
		pie.add(mensaje);
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_RESUMEN));
		pie.add(jugar);
		return ajustarTamano(pie, pie.getPreferredSize().height);
	}

	private void configurarMensaje() {
		mensaje.setOpaque(false);
		mensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
		resumen.setHorizontalAlignment(SwingConstants.CENTER);
		estadoMensaje.setHorizontalAlignment(SwingConstants.CENTER);
		mensaje.add(resumen, RESUMEN);
		mensaje.add(estadoMensaje, ESTADO_MENSAJE);
		mensaje.setPreferredSize(new Dimension(0, Tema.ALTO_LINEA_RESUMEN));
		mensaje.setMinimumSize(mensaje.getPreferredSize());
		mensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, Tema.ALTO_LINEA_RESUMEN));
	}

	private void elegirModo(ModoJuego modo) {
		modoSeleccionadoActual = modo;
		actualizarSeleccion();
		modoSeleccionado.accept(modo);
	}

	private void elegirControles(Controles valor) {
		controlesSeleccionados = valor;
		actualizarSeleccion();
	}

	private void elegirDificultad(Dificultad valor) {
		dificultadSeleccionada = valor;
		actualizarSeleccion();
	}

	private void actualizarSeleccion() {
		modos.get(modoSeleccionadoActual).seleccionar(true);
		controles.get(controlesSeleccionados).seleccionar(true);
		dificultades.get(dificultadSeleccionada).seleccionar(true);
		resumen.setText(controlesSeleccionados + " · " + TarjetaDificultad.titulo(dificultadSeleccionada)
				+ " " + dificultadSeleccionada.tamano() + "×" + dificultadSeleccionada.tamano()
				+ " · sin diagonales ni celdas repetidas");
		tarjetasMensaje.show(mensaje, RESUMEN);
	}

	void seleccionarModo(ModoJuego modo) {
		modoSeleccionadoActual = modo;
		actualizarSeleccion();
	}

	void ocupado(boolean ocupado) {
		jugar.setEnabled(!ocupado);
		modos.values().forEach(tarjeta -> tarjeta.setEnabled(!ocupado));
		controles.values().forEach(tarjeta -> tarjeta.setEnabled(!ocupado));
		dificultades.values().forEach(tarjeta -> tarjeta.setEnabled(!ocupado));
		jugar.cambiarTexto(ocupado ? "Preparando…" : "Jugar");
		if (ocupado) {
			mostrarEstado("Generando un tablero con solución.", Tema.TINTA);
		} else {
			tarjetasMensaje.show(mensaje, RESUMEN);
		}
	}

	void error(String texto) {
		ocupado(false);
		mostrarEstado(texto, Tema.ERROR);
	}

	JPanel componente() {
		return panel;
	}

	private void mostrarEstado(String texto, java.awt.Color color) {
		estadoMensaje.setText(texto);
		estadoMensaje.setForeground(color);
		tarjetasMensaje.show(mensaje, ESTADO_MENSAJE);
	}

	private static JPanel panelVertical() {
		JPanel resultado = new JPanel();
		resultado.setLayout(new BoxLayout(resultado, BoxLayout.Y_AXIS));
		resultado.setOpaque(false);
		resultado.setAlignmentX(Component.LEFT_ALIGNMENT);
		return resultado;
	}

	private static JPanel ajustarTamano(JPanel panel, int alto) {
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
		return panel;
	}

	private static JLabel etiqueta(String texto, int tamano, boolean negrita,
			java.awt.Color color) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Tema.fuente(tamano, negrita));
		etiqueta.setForeground(color);
		etiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);
		return etiqueta;
	}
}
