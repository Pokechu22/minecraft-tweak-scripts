package pokechu22.repairtool;

import java.io.File;

import javax.swing.DefaultListModel;

public class RepairThread extends Thread {
	// Note: DefaultListModel uses the thread-safe Vector class
	private final DefaultListModel<File> files;
	public RepairThread(DefaultListModel<File> files) {
		this.files = files;
	}

	@Override
	public void run() {
		while (true) {
			if (files.getSize() == 0) {
				Thread.yield();
				continue;
			}
			File file = files.remove(0);
			try {
				Repair.process(file);
			} catch (Exception ex) {
				System.out.println("Failed to process " + file);
				ex.printStackTrace(System.out);
			}
		}
	}
}
