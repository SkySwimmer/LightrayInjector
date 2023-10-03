package org.asf.lightray;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultCaret;

import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingConstants;

import java.awt.Dimension;

public class ProgressWindow {

	public JFrame frm;

	public static class WindowLogger {
		public static ProgressWindow frame;

		public static void showWindow() {
			frame = new ProgressWindow();
			frame.frm.setVisible(true);
		}

		public static void closeWindow() {
			if (frame != null) {
				frame.frm.dispose();
				frame = null;
			}
		}

		public static void addMax(int max) {
			if (frame == null)
				return;
			frame.progressBar.setMaximum(frame.progressBar.getMaximum() + max);
		}

		public static void setMax(int max) {
			if (frame == null)
				return;
			frame.progressBar.setMaximum(max);
		}

		public static void setValue(int val) {
			if (frame == null)
				return;
			frame.progressBar.setValue(val);
		}

		public static int getMax() {
			if (frame == null)
				return 0;
			return frame.progressBar.getMaximum();
		}

		public static int getValue() {
			if (frame == null)
				return 0;
			return frame.progressBar.getValue();
		}

		public static void increaseProgress() {
			if (frame == null)
				return;
			if (frame.progressBar.getValue() + 1 > frame.progressBar.getMaximum())
				return;
			frame.progressBar.setValue(frame.progressBar.getValue() + 1);
		}

		public static void fatalError() {
			if (frame == null)
				return;
			JOptionPane.showMessageDialog(frame.frm, "A fatal error occured:\n" + frame.lastMessageText, "Fatal Error",
					JOptionPane.ERROR_MESSAGE);
		}

		public static void fatalError(String msg) {
			if (frame == null) {
				System.err.println(msg);
				return;
			}
			JOptionPane.showMessageDialog(frame.frm, "A fatal error occured:\n" + msg, "Fatal Error",
					JOptionPane.ERROR_MESSAGE);
		}

		public static void log(String message) {
			System.out.println(message);

			if (frame == null)
				return;
			if (message.endsWith("\n"))
				message = message.substring(0, message.length() - 1);

			if (message.contains("\n"))
				for (String ln : message.split("\n"))
					frame.log(ln);
			else
				frame.log(message);
		}

		public static void setLabel(String message) {
			if (frame == null)
				return;
			frame.setLabel(message);
		}
	}

	private JTextArea textArea = new JTextArea();
	private JLabel lastMessage = new JLabel();
	private String lastMessageText;

	public void setLabel(String message) {
		if (!message.isEmpty())
			lastMessage.setText(message);
	}

	public void log(String message) {
		lastMessageText = message;
		String log = textArea.getText() + message;
		textArea.setText(log + "\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
	}

	private JPanel contentPane;
	private final JProgressBar progressBar = new JProgressBar();
	private final JPanel panel_1 = new JPanel();
	private final JPanel panel_2 = new JPanel();
	private final JPanel panel_2_1 = new JPanel();
	private final JPanel panel_2_1_1 = new JPanel();

	/**
	 * Create the frame.
	 */
	public ProgressWindow() {
		frm = new JFrame();
		frm.setTitle("Modification Progress...");
		frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frm.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent arg0) {
				System.exit(122);
			}

		});
		frm.setBounds(100, 100, 672, 245);
		frm.setResizable(false);
		frm.setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));

		frm.setContentPane(contentPane);
		textArea.setFont(new Font("Liberation Mono", Font.PLAIN, 12));

		textArea.setEditable(false);
		JScrollPane pane = new JScrollPane(textArea);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		pane.setBorder(new LineBorder(new Color(0, 0, 0)));

		DefaultCaret caret = (DefaultCaret) textArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		contentPane.add(pane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		contentPane.add(panel, BorderLayout.NORTH);
		panel.setLayout(new BorderLayout(0, 0));
		lastMessage.setHorizontalAlignment(SwingConstants.CENTER);
		lastMessage.setFont(new Font("Liberation Mono", Font.PLAIN, 14));

		panel.add(lastMessage);
		panel_2_1_1.setPreferredSize(new Dimension(10, 5));

		panel.add(panel_2_1_1, BorderLayout.NORTH);
		panel.add(panel_2_1, BorderLayout.SOUTH);
		panel_2_1.setPreferredSize(new Dimension(10, 5));
		contentPane.add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new BorderLayout(0, 0));
		panel_1.add(progressBar, BorderLayout.SOUTH);
		progressBar.setMaximum(0);
		progressBar.setValue(0);
		panel_2.setPreferredSize(new Dimension(10, 5));

		panel_1.add(panel_2, BorderLayout.CENTER);
	}

}
