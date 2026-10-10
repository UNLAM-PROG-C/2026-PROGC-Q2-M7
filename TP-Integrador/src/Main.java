import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import utils.TaskUtils;

public final class Main {
	private Main() {
	}

	public static void main(String[] args) {
		if (GraphicsEnvironment.isHeadless()) {
			System.err.println("> Swing necesita un entorno gráfico, tendras que ejecutar la aplicación en tu escritorio.");
			System.exit(1);
		}

		SwingUtilities.invokeLater(Main::showWindow);
	}

	private static void showWindow() {
		JFrame window = new JFrame("Trabajo práctico integrador");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		DefaultListModel<String> tasks = new DefaultListModel<>();
		JList<String> taskList = new JList<>(tasks);
		taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JTextField input = new JTextField(24);
		JButton add = new JButton("Agregar");
		JButton remove = new JButton("Eliminar seleccionada");
		remove.setEnabled(false);
		JLabel status = new JLabel(TaskUtils.formatCount(tasks.size()));

		Runnable addTask = () -> {
			String task = input.getText().trim();
			if (task.isEmpty()) {
				JOptionPane.showMessageDialog(window, "Escribí una tarea antes de agregarla.",
					"Tarea vacía", JOptionPane.WARNING_MESSAGE);
				input.requestFocusInWindow();
				return;
			}
			tasks.addElement(task);
			input.setText("");
			status.setText(TaskUtils.formatCount(tasks.size()));
			input.requestFocusInWindow();
		};

		add.addActionListener(event -> addTask.run());
		input.addActionListener(event -> addTask.run());
		taskList.addListSelectionListener(event -> remove.setEnabled(!taskList.isSelectionEmpty()));
		remove.addActionListener(event -> {
			int selected = taskList.getSelectedIndex();
			if (selected >= 0) {
				tasks.remove(selected);
				status.setText(TaskUtils.formatCount(tasks.size()));
			}
		});

		JPanel entry = new JPanel(new BorderLayout(8, 8));
		JLabel label = new JLabel("Nueva tarea:");
		label.setLabelFor(input);
		entry.add(label, BorderLayout.NORTH);
		entry.add(input, BorderLayout.CENTER);
		entry.add(add, BorderLayout.EAST);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		actions.add(status);
		actions.add(remove);

		JPanel content = new JPanel(new BorderLayout(12, 12));
		content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		content.add(entry, BorderLayout.NORTH);
		content.add(new JScrollPane(taskList), BorderLayout.CENTER);
		content.add(actions, BorderLayout.SOUTH);
		content.setPreferredSize(new Dimension(520, 340));
		window.setContentPane(content);

		window.getRootPane().setDefaultButton(add);
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);
		input.requestFocusInWindow();
	}
}
