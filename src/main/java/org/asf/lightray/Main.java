package org.asf.lightray;

import java.io.IOException;

import javax.swing.JOptionPane;
import javax.tools.ToolProvider;

public class Main {

	public static void main(String[] args) throws IOException {
		String jv = System.getProperty("java.version");
		if (jv.contains("."))
			jv = jv.substring(0, jv.indexOf("."));

		// Check version
		if (Integer.parseInt(jv) < 15) {
			JOptionPane.showMessageDialog(null, "Lightray requires JAVA 15+\nPlease use a newer JVM.",
					"Unable to launch", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}

		// Check JDK
		if (ToolProvider.getSystemJavaCompiler() == null) {
			JOptionPane.showMessageDialog(null,
					"Lightray requires to be running on a JDK.\nPresently you are using a JRE environment, please installa a Java JDK instead.",
					"Unable to launch", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		}

		// Start
		run(args);
	}

	public static void run(String[] args) throws IOException {
		MainWindow.main(args);
	}

}
