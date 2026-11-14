package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import model.Dificultad;
import model.ModoJuego;
import utils.Constantes;

final class PanelConfiguracion extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int TAMANO_TITULO_MENU = 56;
	private static final int TAMANO_SUBTITULO_MENU = 18;
	private static final float ESPACIADO_TITULO = 0.11f;
	private static final int ALTO_TARJETA_MODO = 80;
	private static final int ALTO_TARJETA_CONTROLES = 152;
	private static final int ALTO_TARJETA_DIFICULTAD = 128;
	private static final int TAMANO_VALOR_TARJETA = 34;
	private static final int CANTIDAD_TECLAS_CONTROL = 4;
	private static final int RELLENO_INFERIOR_TECLAS = 12;
	private static final int TAMANO_RESUMEN = 14;
	private static final int ALTO_LINEA_RESUMEN = 20;

	private final transient Map<ModoJuego, TarjetaOpcion<ModoJuego>> modos = new EnumMap<>(ModoJuego.class);
	private final transient Map<Controles, TarjetaOpcion<Controles>> controles = new EnumMap<>(Controles.class);
	private final transient Map<Dificultad, TarjetaOpcion<Dificultad>> dificultades = new EnumMap<>(Dificultad.class);
	private final transient List<TarjetaOpcion<?>> tarjetas = new ArrayList<>();
	private final JSlider sliderRondas = crearSliderRondas();
	private final JLabel etiquetaRondas = Tema.etiqueta("", Font.BOLD, Tema.TAMANO_TEXTO_AYUDA, Tema.ACENTO);
	private final JLabel estadoMensaje = Tema.etiqueta(" ", Font.PLAIN, TAMANO_RESUMEN, Tema.TINTA_SUAVE);
	private final BotonEstilo jugar = BotonEstilo.principal("Jugar");
	private final transient Consumer<ModoJuego> alCambiarModo;

	private Controles controlesSeleccionados = Controles.FLECHAS;
	private Dificultad dificultadSeleccionada = Dificultad.MEDIA;

	PanelConfiguracion(BiConsumer<Controles, Dificultad> alIniciar, Consumer<ModoJuego> alCambiarModo) {
		this.alCambiarModo = alCambiarModo;
		configurarPanel(alIniciar);
		crearOpciones();
		construirDisposicion();
		aplicarEstadoInicial();
	}

	void seleccionarModo(ModoJuego modo) {
		modos.get(modo).setSelected(true);
		actualizarResumen();
	}

	Controles controlesSeleccionados() {
		return controlesSeleccionados;
	}

	Dificultad dificultadSeleccionada() {
		return dificultadSeleccionada;
	}

	int cantidadRondas() {
		return sliderRondas.getValue();
	}

	void ocupado(boolean ocupado) {
		jugar.setEnabled(!ocupado);
		sliderRondas.setEnabled(!ocupado);
		tarjetas.forEach(tarjeta -> tarjeta.setEnabled(!ocupado));
		jugar.setText(ocupado ? "Preparando…" : "Jugar");
		if (ocupado) {
			mostrarEstado("Generando un tablero con solución.", Tema.TINTA);
			return;
		}
		actualizarResumen();
	}

	void error(String mensaje) {
		ocupado(false);
		mostrarEstado(mensaje, Tema.ERROR);
	}

	private void configurarPanel(BiConsumer<Controles, Dificultad> alIniciar) {
		setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
		setName("configuracion");
		setBackground(Tema.FONDO);
		setBorder(BorderFactory.createEmptyBorder(Tema.SEPARACION_SECCION,
				Tema.RELLENO_HORIZONTAL_MENU, Tema.SEPARACION_SECCION, Tema.RELLENO_HORIZONTAL_MENU));
		jugar.setName("iniciar");
		estadoMensaje.setName("resumen-seleccion");
		jugar.addActionListener(evento -> alIniciar.accept(controlesSeleccionados, dificultadSeleccionada));
	}

	private void crearOpciones() {
		crearOpcionesModo();
		crearOpcionesControles();
		crearOpcionesDificultad();
	}

	private void crearOpcionesModo() {
		ButtonGroup grupo = new ButtonGroup();
		registrarModo(grupo, ModoJuego.UN_JUGADOR, "Un jugador", "A tu ritmo.");
		registrarModo(grupo, ModoJuego.MULTIJUGADOR, "Multijugador", "Jugá con otras personas.");
	}

	private void registrarModo(ButtonGroup grupo, ModoJuego modo, String titulo, String descripcion) {
		TarjetaOpcion<ModoJuego> tarjeta = crearTarjetaModo(modo, titulo, descripcion);
		String sufijo = modo.name().toLowerCase(Locale.ROOT).replace('_', '-');
		registrarOpcion(grupo, modos, tarjeta, "modo-" + sufijo, this::elegirModo);
	}

	private void crearOpcionesControles() {
		ButtonGroup grupo = new ButtonGroup();
		for (Controles valor : Controles.values()) {
			String nombre = "controles-" + valor.name().toLowerCase(Locale.ROOT);
			registrarOpcion(grupo, controles, crearTarjetaControles(valor), nombre, this::elegirControles);
		}
	}

	private void crearOpcionesDificultad() {
		ButtonGroup grupo = new ButtonGroup();
		for (Dificultad valor : Dificultad.values()) {
			String nombre = "dificultad-" + valor.name().toLowerCase(Locale.ROOT);
			registrarOpcion(grupo, dificultades, crearTarjetaDificultad(valor),
					nombre, this::elegirDificultad);
		}
	}

	private <T> void registrarOpcion(ButtonGroup grupo, Map<T, TarjetaOpcion<T>> mapa,
			TarjetaOpcion<T> tarjeta, String nombre, Consumer<T> alElegir) {
		tarjeta.setName(nombre);
		tarjeta.addActionListener(evento -> alElegir.accept(tarjeta.valor()));
		grupo.add(tarjeta);
		mapa.put(tarjeta.valor(), tarjeta);
		tarjetas.add(tarjeta);
	}

	private void construirDisposicion() {
		add(encabezado());
		add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		add(seccion("MODO DE JUEGO", grilla(modos.values())));
		add(Box.createVerticalStrut(Tema.SEPARACION_GRANDE));
		add(seccion("CONTROLES", grilla(controles.values())));
		add(Box.createVerticalStrut(Tema.SEPARACION_GRANDE));
		add(seccion("DIFICULTAD", grilla(dificultades.values())));
		add(Box.createVerticalStrut(Tema.SEPARACION_GRANDE));
		add(seccion("RONDAS PARA GANAR", panelSliderRondas()));
		add(Box.createVerticalGlue());
		add(pie());
	}

	private void aplicarEstadoInicial() {
		modos.get(ModoJuego.UN_JUGADOR).setSelected(true);
		controles.get(controlesSeleccionados).setSelected(true);
		dificultades.get(dificultadSeleccionada).setSelected(true);
		actualizarEtiquetaRondas();
		actualizarResumen();
	}

	private JPanel encabezado() {
		JPanel encabezado = Tema.panelVertical();
		JLabel titulo = Tema.etiqueta("ZIP", Font.BOLD, TAMANO_TITULO_MENU, Tema.TINTA);
		titulo.setFont(Tema.fuenteConEspaciado(Font.BOLD, TAMANO_TITULO_MENU, ESPACIADO_TITULO));
		JLabel subtitulo = Tema.etiqueta("Conectá los números. Completá el tablero.",
				Font.PLAIN, TAMANO_SUBTITULO_MENU, Tema.TINTA_SUAVE);
		encabezado.add(titulo);
		encabezado.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		encabezado.add(subtitulo);
		return fijarAltoMaximo(encabezado);
	}

	private JPanel seccion(String titulo, JPanel contenido) {
		JPanel seccion = Tema.panelVertical();
		JLabel etiqueta = Tema.etiqueta(titulo, Font.BOLD, Tema.TAMANO_TITULO_SECCION, Tema.TINTA_SUAVE);
		etiqueta.setFont(Tema.fuenteConEspaciado(Font.BOLD, Tema.TAMANO_TITULO_SECCION, Tema.ESPACIADO_SECCION));
		seccion.add(etiqueta);
		seccion.add(Box.createVerticalStrut(Tema.SEPARACION_MEDIA));
		seccion.add(contenido);
		return fijarAltoMaximo(seccion);
	}

	private JPanel grilla(Iterable<? extends JComponent> elementos) {
		List<JComponent> lista = new ArrayList<>();
		elementos.forEach(lista::add);
		JPanel grilla = new JPanel(new GridLayout(1, lista.size(), Tema.SEPARACION_TARJETAS, 0));
		grilla.setOpaque(false);
		grilla.setAlignmentX(Component.LEFT_ALIGNMENT);
		lista.forEach(grilla::add);
		return grilla;
	}

	private JSlider crearSliderRondas() {
		JSlider slider = new JSlider(Constantes.MIN_RONDAS, Constantes.MAX_RONDAS, Constantes.RONDAS_POR_DEFECTO);
		slider.setName("rondas");
		slider.setOpaque(false);
		slider.setBackground(Tema.FONDO);
		slider.setForeground(Tema.TINTA);
		slider.setFont(Tema.fuente(Font.BOLD, Tema.TAMANO_TEXTO_AYUDA));
		slider.setMajorTickSpacing(1);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setSnapToTicks(true);
		return slider;
	}

	private JPanel panelSliderRondas() {
		JPanel contenedor = Tema.panelHorizontal();
		contenedor.setAlignmentX(Component.LEFT_ALIGNMENT);
		sliderRondas.addChangeListener(evento -> {
			actualizarEtiquetaRondas();
			actualizarResumen();
		});
		contenedor.add(sliderRondas);
		contenedor.add(Box.createHorizontalStrut(Tema.SEPARACION_MEDIA));
		contenedor.add(etiquetaRondas);
		return contenedor;
	}

	private JPanel pie() {
		JPanel pie = Tema.panelVertical();
		estadoMensaje.setHorizontalAlignment(SwingConstants.CENTER);
		estadoMensaje.setPreferredSize(new Dimension(0, ALTO_LINEA_RESUMEN));
		estadoMensaje.setMinimumSize(estadoMensaje.getPreferredSize());
		estadoMensaje.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_LINEA_RESUMEN));
		jugar.setAlignmentX(Component.LEFT_ALIGNMENT);
		jugar.setMaximumSize(new Dimension(Integer.MAX_VALUE, jugar.getPreferredSize().height));
		pie.add(estadoMensaje);
		pie.add(Box.createVerticalStrut(Tema.SEPARACION_ENCABEZADO));
		pie.add(jugar);
		return fijarAltoMaximo(pie);
	}

	private TarjetaOpcion<ModoJuego> crearTarjetaModo(ModoJuego modo, String titulo, String descripcion) {
		TipoIcono tipo = modo == ModoJuego.UN_JUGADOR ? TipoIcono.PERSONA : TipoIcono.PERSONAS;
		IconoLineal icono = new IconoLineal(tipo, Tema.TAMANO_ICONO_TARJETA, Tema.TINTA);
		JPanel contenido = crearContenidoModo(icono, titulo, descripcion);
		TarjetaOpcion<ModoJuego> tarjeta = new TarjetaOpcion<>(modo, contenido, titulo, descripcion);
		tarjeta.addItemListener(evento -> icono.establecerColor(tarjeta.isSelected() ? Tema.ACENTO : Tema.TINTA));
		tamanoTarjeta(tarjeta, ALTO_TARJETA_MODO);
		return tarjeta;
	}

	private JPanel crearContenidoModo(IconoLineal icono, String titulo, String descripcion) {
		JPanel panel = new JPanel(new BorderLayout(Tema.SEPARACION_TARJETAS, 0));
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_TARJETA,
				Tema.RELLENO_TARJETA, Tema.RELLENO_TARJETA, Tema.RELLENO_TARJETA));
		panel.add(new JLabel(icono), BorderLayout.WEST);
		panel.add(crearCopiaModo(titulo, descripcion), BorderLayout.CENTER);
		return panel;
	}

	private JPanel crearCopiaModo(String titulo, String descripcion) {
		JPanel copia = Tema.panelVertical();
		copia.add(Box.createVerticalGlue());
		copia.add(Tema.etiqueta(titulo, Font.BOLD, Tema.TAMANO_TITULO_TARJETA, Tema.TINTA));
		copia.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		copia.add(Tema.etiqueta(descripcion, Font.PLAIN, Tema.TAMANO_TEXTO_TARJETA, Tema.TINTA_SUAVE));
		copia.add(Box.createVerticalGlue());
		return copia;
	}

	private TarjetaOpcion<Controles> crearTarjetaControles(Controles valor) {
		JPanel contenido = new JPanel(new BorderLayout());
		contenido.setOpaque(false);
		contenido.setBorder(BorderFactory.createEmptyBorder(Tema.SEPARACION_MEDIA,
				Tema.RELLENO_TARJETA, RELLENO_INFERIOR_TECLAS, Tema.RELLENO_TARJETA));
		contenido.add(Tema.etiqueta(valor.toString(), Font.BOLD,
				Tema.TAMANO_TITULO_TARJETA, Tema.TINTA), BorderLayout.NORTH);
		contenido.add(crearTeclado(valor), BorderLayout.CENTER);
		TarjetaOpcion<Controles> tarjeta = new TarjetaOpcion<>(valor, contenido,
				valor.toString(), "Elegir controles " + valor);
		tamanoTarjeta(tarjeta, ALTO_TARJETA_CONTROLES);
		return tarjeta;
	}

	private JPanel crearTeclado(Controles valor) {
		JPanel teclado = Tema.panelVertical();
		teclado.add(Box.createVerticalGlue());
		teclado.add(Tema.centrar(teclaControl(valor, 0)));
		teclado.add(Box.createVerticalStrut(Tema.SEPARACION_TECLAS));
		teclado.add(Tema.centrar(crearFilaTeclasInferiores(valor)));
		return teclado;
	}

	private JPanel crearFilaTeclasInferiores(Controles valor) {
		JPanel fila = Tema.panelHorizontal();
		for (int indice = 1; indice < CANTIDAD_TECLAS_CONTROL; indice++) {
			if (indice > 1) {
				fila.add(Box.createHorizontalStrut(Tema.SEPARACION_TECLAS));
			}
			fila.add(teclaControl(valor, indice));
		}
		return fila;
	}

	private TeclaVisual teclaControl(Controles valor, int indice) {
		if (valor == Controles.WASD) {
			String[] letras = {"W", "A", "S", "D"};
			return new TeclaVisual(letras[indice], Tema.TAMANO_TECLA_MENU, Tema.TAMANO_TECLA_MENU);
		}
		TipoIcono[] flechas = {
			TipoIcono.FLECHA_ARRIBA, TipoIcono.FLECHA_IZQUIERDA,
			TipoIcono.FLECHA_ABAJO, TipoIcono.FLECHA_DERECHA
		};
		return new TeclaVisual(flechas[indice], Tema.TAMANO_TECLA_MENU);
	}

	private TarjetaOpcion<Dificultad> crearTarjetaDificultad(Dificultad valor) {
		TextoDificultad copia = copiaDificultad(valor);
		JPanel contenido = crearContenidoDificultad(valor, copia);
		TarjetaOpcion<Dificultad> tarjeta = new TarjetaOpcion<>(valor, contenido,
				copia.titulo(), copia.descripcion());
		tamanoTarjeta(tarjeta, ALTO_TARJETA_DIFICULTAD);
		return tarjeta;
	}

	private JPanel crearContenidoDificultad(Dificultad valor, TextoDificultad copia) {
		JPanel contenido = Tema.panelVertical();
		contenido.setBorder(BorderFactory.createEmptyBorder(Tema.SEPARACION_MEDIA,
				Tema.RELLENO_TARJETA, Tema.SEPARACION_MEDIA, Tema.RELLENO_TARJETA));
		contenido.add(Tema.etiqueta(copia.titulo(), Font.BOLD, Tema.TAMANO_TITULO_TARJETA, Tema.TINTA));
		contenido.add(Box.createVerticalStrut(Tema.DESPLAZAMIENTO_FOCO));
		contenido.add(Tema.etiqueta(valor.tamano() + "×" + valor.tamano(),
				Font.BOLD, TAMANO_VALOR_TARJETA, Tema.TINTA));
		contenido.add(Box.createVerticalGlue());
		contenido.add(Tema.etiqueta(copia.primeraLinea(), Font.PLAIN, Tema.TAMANO_TEXTO_TARJETA, Tema.TINTA_SUAVE));
		contenido.add(Tema.etiqueta(copia.segundaLinea(), Font.PLAIN, Tema.TAMANO_TEXTO_TARJETA, Tema.TINTA_SUAVE));
		return contenido;
	}

	private void elegirModo(ModoJuego modo) {
		actualizarResumen();
		alCambiarModo.accept(modo);
	}

	private void elegirControles(Controles valor) {
		controlesSeleccionados = valor;
		actualizarResumen();
	}

	private void elegirDificultad(Dificultad valor) {
		dificultadSeleccionada = valor;
		actualizarResumen();
	}

	private void actualizarEtiquetaRondas() {
		int rondas = cantidadRondas();
		etiquetaRondas.setText(rondas + (rondas == 1 ? " ronda" : " rondas"));
	}

	private void actualizarResumen() {
		int rondas = cantidadRondas();
		String resumen = controlesSeleccionados + " · " + copiaDificultad(dificultadSeleccionada).titulo()
				+ " " + dificultadSeleccionada.tamano() + "×" + dificultadSeleccionada.tamano()
				+ " · Al mejor de " + rondas + (rondas == 1 ? " ronda" : " rondas");
		mostrarEstado(resumen, Tema.TINTA_SUAVE);
	}

	private void mostrarEstado(String texto, Color color) {
		estadoMensaje.setText(texto);
		estadoMensaje.setForeground(color);
	}

	private static void tamanoTarjeta(JComponent tarjeta, int alto) {
		Dimension dimension = new Dimension(0, alto);
		tarjeta.setPreferredSize(dimension);
		tarjeta.setMinimumSize(dimension);
	}

	private static JPanel fijarAltoMaximo(JPanel panel) {
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private static TextoDificultad copiaDificultad(Dificultad dificultad) {
		return switch (dificultad) {
			case FACIL -> new TextoDificultad("Fácil", "Tablero chico, recorrido directo.",
					"Tablero chico,", "recorrido directo.");
			case MEDIA -> new TextoDificultad("Media", "Tablero mediano, hay que planificar.",
					"Tablero mediano,", "hay que planificar.");
			case DIFICIL -> new TextoDificultad("Difícil", "Tablero grande, cada giro cuenta.",
					"Tablero grande,", "cada giro cuenta.");
		};
	}

	private record TextoDificultad(String titulo, String descripcion, String primeraLinea,
			String segundaLinea) {
	}
}
