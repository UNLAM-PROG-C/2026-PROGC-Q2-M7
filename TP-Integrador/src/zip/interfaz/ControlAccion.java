package zip.interfaz;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.EventListenerList;

abstract class ControlAccion extends JComponent {
	private static final long serialVersionUID = 1L;
	private static final String PRESIONAR = "press";
	private static final String SOLTAR = "liberacion";
	private final EventListenerList escuchas = new EventListenerList();
	private boolean encima;
	private boolean presionado;

	ControlAccion(String nombreAccesible) {
		setFocusable(true);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		getAccessibleContext().setAccessibleName(nombreAccesible);
		instalarAccionesRaton();
		instalarAccionesTeclado();
	}

	@Override
	public AccessibleContext getAccessibleContext() {
		if (accessibleContext == null) {
			accessibleContext = new ControlAccionAccesible();
		}
		return accessibleContext;
	}

	final void agregarEscucha(ActionListener escucha) {
		escuchas.add(ActionListener.class, escucha);
	}

	final void hacerClic() {
		if (isEnabled()) {
			requestFocusInWindow();
			activar();
		}
	}

	final boolean estaEncima() {
		return encima && isEnabled();
	}

	final boolean estaPresionado() {
		return presionado && isEnabled();
	}

	@Override
	public void setEnabled(boolean habilitado) {
		super.setEnabled(habilitado);
		if (!habilitado) {
			encima = false;
			presionado = false;
		}
		repaint();
	}

	protected void activar() {
		notificarAccion();
	}

	protected final void notificarAccion() {
		ActionEvent evento = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getName());
		for (ActionListener escucha : escuchas.getListeners(ActionListener.class)) {
			escucha.actionPerformed(evento);
		}
	}

	private void instalarAccionesRaton() {
		addMouseListener(new AccionesRaton());
	}

	private void instalarAccionesTeclado() {
		vincularTecla(KeyEvent.VK_SPACE);
		vincularTecla(KeyEvent.VK_ENTER);
	}

	private void vincularTecla(int tecla) {
		String presion = PRESIONAR + tecla;
		String liberacion = SOLTAR + tecla;
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tecla, 0, false), presion);
		getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(tecla, 0, true), liberacion);
		getActionMap().put(presion, new AccionPresion(true));
		getActionMap().put(liberacion, new AccionPresion(false));
	}

	private void cambiarEncima(boolean valor) {
		encima = valor;
		repaint();
	}

	private void comenzarPresion(MouseEvent evento) {
		if (isEnabled() && evento.getButton() == MouseEvent.BUTTON1) {
			requestFocusInWindow();
			presionado = true;
			repaint();
		}
	}

	private void terminarPresion(MouseEvent evento) {
		boolean debeActivar = presionado && contains(evento.getPoint());
		presionado = false;
		repaint();
		if (debeActivar) {
			activar();
		}
	}

	private final class AccionPresion extends AbstractAction {
		private static final long serialVersionUID = 1L;
		private final boolean presionando;

		AccionPresion(boolean presionando) {
			this.presionando = presionando;
		}

		@Override
		public void actionPerformed(ActionEvent evento) {
			if (!isEnabled()) {
				return;
			}
			boolean debeActivar = presionado && !presionando;
			presionado = presionando;
			repaint();
			if (debeActivar) {
				activar();
			}
		}
	}

	private final class AccionesRaton extends MouseAdapter {
		@Override
		public void mouseEntered(MouseEvent evento) {
			cambiarEncima(true);
		}

		@Override
		public void mouseExited(MouseEvent evento) {
			cambiarEncima(false);
		}

		@Override
		public void mousePressed(MouseEvent evento) {
			comenzarPresion(evento);
		}

		@Override
		public void mouseReleased(MouseEvent evento) {
			terminarPresion(evento);
		}
	}

	protected class ControlAccionAccesible extends AccessibleJComponent {
		private static final long serialVersionUID = 1L;

		@Override
		public AccessibleRole getAccessibleRole() {
			return AccessibleRole.PUSH_BUTTON;
		}
	}
}
