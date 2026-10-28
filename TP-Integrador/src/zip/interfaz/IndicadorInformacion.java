package zip.interfaz;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class IndicadorInformacion extends JPanel {
	private static final long serialVersionUID = 1L;
	private final JLabel valor = new JLabel();

	IndicadorInformacion(String etiqueta, String nombreValor) {
		super(new BorderLayout(Tema.SEPARACION_TECLAS, 0));
		setOpaque(false);
		Dimension tamano = new Dimension(0, Tema.ALTO_INDICADOR);
		setPreferredSize(tamano);
		setMinimumSize(tamano);
		setBorder(BorderFactory.createEmptyBorder(Tema.RELLENO_VERTICAL_INDICADOR,
				Tema.RELLENO_HORIZONTAL_INDICADOR, Tema.RELLENO_VERTICAL_INDICADOR,
				Tema.RELLENO_HORIZONTAL_INDICADOR));
		add(etiqueta(etiqueta), BorderLayout.WEST);
		this.valor.setName(nombreValor);
		this.valor.setFont(Tema.fuente(Tema.TAMANO_VALOR_INDICADOR, true));
		this.valor.setForeground(Tema.TINTA);
		add(this.valor, BorderLayout.EAST);
	}

	void establecerValor(String texto) {
		valor.setText(texto);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graficos) {
		Graphics2D dibujo = (Graphics2D) graficos.create();
		try {
			Tema.preparar(dibujo);
			Tema.rellenarBorde(dibujo, 0, 0, getWidth(), getHeight(), Tema.RADIO_MEDIO,
					Tema.BORDE, Tema.TARJETA);
		} finally {
			dibujo.dispose();
		}
		super.paintComponent(graficos);
	}

	private JLabel etiqueta(String texto) {
		JLabel etiqueta = new JLabel(texto);
		etiqueta.setFont(Tema.fuente(Tema.TAMANO_ETIQUETA_INDICADOR, false));
		etiqueta.setForeground(Tema.TINTA_SUAVE);
		return etiqueta;
	}
}
