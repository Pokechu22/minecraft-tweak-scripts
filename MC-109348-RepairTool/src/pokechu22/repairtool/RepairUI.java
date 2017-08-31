package pokechu22.repairtool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class RepairUI extends JFrame {
	private static final long serialVersionUID = -1578388191222209932L;

	@SuppressWarnings("resource") // We're trashing System.out and don't close our delegate, but that doesn't
									// matter
	public static void create() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			RepairUI frame = new RepairUI();
			System.setOut(frame.new DelegatePrintStream());
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private JPanel contentPane;

	// Needed because for some reason directly setting it does not work
	private final Color UNEDITABLE_TEXT_AREA_BACKGROUND = new Color(
			UIManager.getColor("TextArea.disabledBackground").getRGB());
	private DefaultListModel<File> files;
	private JTextArea txtpnLog;

	private StringBuffer logBuilder = new StringBuffer().append("--- Log ---\n");

	private static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException { }
	}

	private class DelegatePrintStream extends PrintStream {
		@SuppressWarnings("resource")
		public DelegatePrintStream() {
			super(new NullOutputStream());
		}

		@Override
		public void print(String x) {
			logBuilder.append(x);
		}

		@Override
		public void println(String x) {
			logBuilder.append(x).append('\n');
			flush();
		}

		@Override
		public void flush() {
			txtpnLog.setText(logBuilder.toString());
		}

		// Handle other writes (shouldn't be called, but for safety)
		// This does not do any encoding. Expect bad results.
		@Override
		public void write(int b) {
			logBuilder.append((char) b);
			if (b == '\n') {
				flush();
			}
		}

		@Override
		public void write(byte[] buf, int off, int len) {
			boolean flush = false;
			for (int i = 0; i < len; i++) {
				logBuilder.append((char) buf[i + off]);
				if (buf[i + off] == '\n') {
					flush = true;
				}
			}
			if (flush) {
				flush();
			}
		}
	}

	/**
	 * Create the frame.
	 */
	public RepairUI() {
		setTitle("MC-109348 repair tool by pokechu22");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JButton btnAddFiles = new JButton("Add files");
		btnAddFiles.addActionListener(this::addFiles);
		contentPane.add(btnAddFiles, BorderLayout.NORTH);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		contentPane.add(splitPane, BorderLayout.CENTER);

		files = new DefaultListModel<>();
		JList<File> fileList = new JList<>();
		fileList.setModel(files);
		splitPane.setLeftComponent(fileList);

		JScrollPane scrollPane = new JScrollPane();
		splitPane.setRightComponent(scrollPane);

		txtpnLog = new JTextArea();
		txtpnLog.setText("--- Log ---");
		txtpnLog.setEditable(false);
		txtpnLog.setBackground(UNEDITABLE_TEXT_AREA_BACKGROUND);
		txtpnLog.setLineWrap(false);
		scrollPane.setViewportView(txtpnLog);

		new RepairThread(files).start();
	}

	private void addFiles(ActionEvent e) {
		JFileChooser chooser = new JFileChooser();
		String appdata = System.getenv("APPDATA");
		if (appdata != null) {
			chooser.setCurrentDirectory(new File(new File(System.getenv("APPDATA"), ".minecraft"), "saves"));
		}
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Anvil region files", "mca");
		chooser.setFileFilter(filter);
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setMultiSelectionEnabled(true);

		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			for (File file : chooser.getSelectedFiles()) {
				files.addElement(file);
			}
		}
	}
}
