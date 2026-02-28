package org.asf.lightray;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.FlowLayout;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.asf.cyan.fluid.DynamicClassLoader;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformers;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.tree.ClassNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.awt.event.ActionEvent;
import java.awt.Font;
import java.awt.SystemColor;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListDataListener;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainWindow {

	public static String dex2jarInnerFolder = "dex-tools-2.1";
	public static String dex2jarDownload = "https://github.com/pxb1988/dex2jar/releases/download/v2.1/dex2jar-2.1.zip";

	private static MainWindow winInst;
	private JFrame frmLightray;
	private JTextField textField;
	private boolean shiftDown;
	private static DynamicClassLoader dynLoader = new DynamicClassLoader();
	private static HashMap<String, List<ILightrayPatcher>> patchers = new HashMap<String, List<ILightrayPatcher>>();
	private static HashMap<String, PatchEntry> patches = new LinkedHashMap<String, PatchEntry>();
	private static ArrayList<File> libs;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		// Check arguments
		for (String arg : args) {
			if (arg.equalsIgnoreCase("--build-now")) {
				buildFromCli(args);
				return;
			}
		}

		// Show window
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmLightray.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		winInst = this;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		try {
			try {
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
					| UnsupportedLookAndFeelException e1) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e1) {
		}

		frmLightray = new JFrame();
		frmLightray.setTitle("Lightray Injector");
		frmLightray.setBounds(100, 100, 848, 516);
		frmLightray.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmLightray.setLocationRelativeTo(null);
		frmLightray.setResizable(false);
		frmLightray.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(800, 460));
		frmLightray.getContentPane().add(panel);
		panel.setLayout(null);

		textField = new JTextField();
		textField.setBounds(0, 36, 689, 25);
		panel.add(textField);
		textField.setColumns(10);

		JButton btnSelect = new JButton("Browse...");
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser f = new JFileChooser();
				f.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						return arg0.getName().endsWith(".apk") || arg0.isDirectory();
					}

					@Override
					public String getDescription() {
						return "APK files (*.apk)";
					}
				});
				f.setDialogTitle("Select APK file");
				if (f.showDialog(frmLightray, "OK") == JFileChooser.APPROVE_OPTION) {
					try {
						textField.setText(f.getSelectedFile().getCanonicalPath());
					} catch (IOException e) {
						textField.setText(f.getSelectedFile().getAbsolutePath());
					}
				}
			}
		});
		btnSelect.setBounds(695, 36, 105, 25);
		panel.add(btnSelect);

		JLabel lblNewLabel = new JLabel("APK file");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
		lblNewLabel.setBounds(0, 12, 534, 21);
		panel.add(lblNewLabel);

		JButton btnNewButton = new JButton("Build modified app");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// Check input
				if (textField.getText().isBlank() || !new File(textField.getText()).exists()) {
					JOptionPane.showMessageDialog(frmLightray, "Invalid APK file, please select an existing APK file.",
							"Invalid APK", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Check archive
				try {
					ZipFile archive = new ZipFile(textField.getText());
					boolean isAndroid = archive.getEntry("AndroidManifest.xml") != null;
					boolean wasModified = archive.getEntry("assets/lightray-resources.json") != null;
					archive.close();
					if (!isAndroid) {
						JOptionPane.showMessageDialog(frmLightray,
								"Invalid APK file, please select a Android APK file.\n\nFile "
										+ new File(textField.getText()).getName()
										+ " does not contain a android app manifest!",
								"Invalid APK", JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (wasModified) {
						// Check shift
						if (shiftDown) {
							// Warn
							if (JOptionPane.showConfirmDialog(frmLightray,
									"It is highly recommended to NOT use already-patched APK files else things WILL break!\n\n"
											+ "If any issues occur make sure to use a unpatched APK before reporting!\n\nContinue?",
									"Selected a patched APK", JOptionPane.YES_NO_OPTION,
									JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)
								return;
						} else {
							// Error
							JOptionPane.showMessageDialog(frmLightray,
									"It is highly recommended to NOT use already-patched APK files else things WILL break!\n"
											+ "Any old mod resources in the APK will no longer function correctly if you proceed!\n"
											+ "If there were any transformers applied, they will remain in the APK, beware of duplicate transformers!\n\n"
											+ "To continue, please hold SHIFT while pressing the build button. Cancelled modification.",
									"Selected a patched APK", JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(frmLightray,
							"Invalid APK file, please select a Android APK file.\n\nCould not read "
									+ new File(textField.getText()).getName(),
							"Invalid APK", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// Save
				File lastApk = new File("lastapk.json");
				JsonObject lastApkInfo = new JsonObject();
				lastApkInfo.addProperty("path", textField.getText());
				try {
					Files.writeString(lastApk.toPath(), lastApkInfo.toString());
				} catch (IOException e2) {
				}

				frmLightray.dispose();
				ProgressWindow.WindowLogger.showWindow();
				Thread th = new Thread(() -> {
					ProgressWindow.WindowLogger.setLabel("Preparing...");
					ProgressWindow.WindowLogger.log("Processing...");

					try {
						File inputFile = new File(textField.getText());
						File out = new File(inputFile.getParentFile(),
								inputFile.getName().substring(0, inputFile.getName().lastIndexOf("."))
										+ " (patched).apk");
						apply(inputFile, out);
						JOptionPane.showMessageDialog(ProgressWindow.WindowLogger.frame.frm,
								"Success!\nSaved at: '" + out.getAbsolutePath()
										+ "'\n\nThe application has been modified, this program will now close.",
								"Modification successful", JOptionPane.INFORMATION_MESSAGE);
						System.exit(0);
					} catch (Throwable e) {
						ProgressWindow.WindowLogger.setLabel("Fatal error");
						ProgressWindow.WindowLogger.log("Error: " + e.getClass().getTypeName() + ": " + e.getMessage());
						for (StackTraceElement el : e.getStackTrace())
							ProgressWindow.WindowLogger.log("    At: " + el);
						ProgressWindow.WindowLogger.fatalError("Modification failure!\nException: "
								+ e.getClass().getTypeName() + ": " + e.getMessage());
					}
				}, "Modification thread");
				th.setDaemon(true);
				th.start();
			}
		});
		btnNewButton.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnNewButton.setBounds(573, 435, 227, 25);
		panel.add(btnNewButton);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.setFont(new Font("Tahoma", Font.PLAIN, 12));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frmLightray.dispose();
				System.exit(0);
			}
		});
		btnCancel.setBounds(0, 435, 99, 25);
		panel.add(btnCancel);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBounds(0, 93, 800, 330);
		panel.add(scrollPane);

		JList<PatchEntry> list = new JList<PatchEntry>();
		list.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
					e.consume();
					if (list.getSelectedValue() != null) {
						// Update
						PatchEntry entry = list.getSelectedValue();
						setEnabledRecursive(entry, !entry.enabled);

						// Repaint
						list.repaint();

						// Save
						saveModInfo();
					}
				}
			}
		});
		list.addMouseListener(new MouseAdapter() {
			public synchronized void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 1) {
					int index = list.locationToIndex(evt.getPoint());
					if (index >= 0 && index < patches.size()) {
						// Update
						PatchEntry entry = patches.values().toArray(new PatchEntry[0])[index];
						setEnabledRecursive(entry, !entry.enabled);

						// Repaint
						list.repaint();

						// Save
						saveModInfo();
					}
				}
			}
		});
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new ListCellRenderer<PatchEntry>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends PatchEntry> list, PatchEntry entry, int index,
					boolean selected, boolean cellHasFocus) {
				entry.box.setComponentOrientation(list.getComponentOrientation());
				entry.box.setFont(list.getFont());
				entry.box.setBackground(list.getBackground());
				if (cellHasFocus)
					entry.box.setForeground(SystemColor.textHighlight);
				else
					entry.box.setForeground(SystemColor.textText);
				entry.box.setSelected(entry.enabled);
				entry.box.setEnabled(true);
				return entry.box;
			}

		});

		JLabel lblModifications = new JLabel("Modifications");
		lblModifications.setFont(new Font("Dialog", Font.PLAIN, 12));
		lblModifications.setBounds(0, 72, 534, 21);
		panel.add(lblModifications);

		// Load modifications
		File mods = new File("patches");
		mods.mkdirs();
		scan(mods, "", null);
		mods = new File("plainpatches");
		mods.mkdirs();
		scanPlain(mods);

		// Load active modifications
		File lastApk = new File("lastapk.json");
		if (lastApk.exists()) {
			try {
				JsonObject data = JsonParser.parseString(Files.readString(lastApk.toPath())).getAsJsonObject();
				textField.setText(data.get("path").getAsString());
			} catch (JsonSyntaxException | IOException e) {
				JOptionPane.showMessageDialog(frmLightray,
						"An unknown error occured loading previous apk path.\n" + "\n" + "Exception: " + e, "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		File modInfo = new File("activepatches.json");
		if (modInfo.exists()) {
			try {
				JsonArray data = JsonParser.parseString(Files.readString(modInfo.toPath())).getAsJsonArray();
				for (JsonElement ele : data) {
					String entry = ele.getAsString();
					if (patches.containsKey(entry))
						patches.get(entry).enabled = true;
				}
			} catch (JsonSyntaxException | IOException e) {
				JOptionPane.showMessageDialog(frmLightray,
						"An unknown error occured loading active modifications.\n" + "\n" + "Exception: " + e, "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		}

		// Save modification info
		saveModInfo();

		// Load list
		list.setModel(new ListModel<PatchEntry>() {

			@Override
			public PatchEntry getElementAt(int arg0) {
				return patches.values().toArray(new PatchEntry[0])[arg0];
			}

			@Override
			public int getSize() {
				return patches.size();
			}

			@Override
			public void addListDataListener(ListDataListener arg0) {
			}

			@Override
			public void removeListDataListener(ListDataListener arg0) {
			}
		});
		scrollPane.setViewportView(list);
		recurseAddKeyHandler(frmLightray);
	}

	protected static void setEnabledRecursive(PatchEntry entry, boolean enabled) {
		// Set state
		entry.enabled = enabled;
		entry.box.setEnabled(entry.enabled);

		// Update
		for (PatchEntry child : entry.childEntries)
			setEnabledRecursive(child, enabled);
	}

	private static void apply(File inputFile, File outputFile) throws Exception {
		// Parse input
		ZipFile archive = new ZipFile(inputFile);
		Document androidManifestDom = parseAXML(archive.getInputStream(archive.getEntry("AndroidManifest.xml")));
		archive.close();

		// Pull API version
		Element manifestRoot = androidManifestDom.getDocumentElement();
		Element sdkVersion = (Element) manifestRoot.getElementsByTagName("uses-sdk").item(0);
		String minSdk = sdkVersion.getAttribute("android:minSdkVersion");
		String targetSdk = sdkVersion.getAttribute("android:targetSdkVersion");

		// Pull app info
		String pkg = manifestRoot.getAttribute("package");
		String appVerCode = manifestRoot.getAttribute("android:versionCode");
		String appVerName = manifestRoot.getAttribute("android:versionName");
		ProgressWindow.WindowLogger.log("Application: " + pkg + " " + appVerName + " (build " + appVerCode + ")");
		ProgressWindow.WindowLogger.log("Target SDK: " + targetSdk + ", minimal SDK: " + minSdk);
		ProgressWindow.WindowLogger.log("");

		// Prepare directories
		ProgressWindow.WindowLogger.setLabel("Preparing files...");
		ProgressWindow.WindowLogger.log("Creating temporary directories...");
		if (new File("lightray-work").exists())
			deleteDir(new File("lightray-work"));
		new File("lightray-work/mods").mkdirs();
		new File("lightray-work/apks").mkdirs();
		ProgressWindow.WindowLogger.log("Done.");

		// Download dex2jar
		if (!new File("dex2jar/complete").exists()) {
			ProgressWindow.WindowLogger.log("");
			ProgressWindow.WindowLogger.log("Downloading dex2jar... (note this package is owned by pxb1988)");
			ProgressWindow.WindowLogger.setLabel("Downloading dex2jar...");
			downloadFile("lightray-work/dex2jar.zip", dex2jarDownload);
			ProgressWindow.WindowLogger.log("Extracting dex2jar...");
			extractFile("lightray-work/dex2jar.zip", "lightray-work");
			ProgressWindow.WindowLogger.log("Moving dex2jar...");
			new File("lightray-work/" + dex2jarInnerFolder).renameTo(new File("dex2jar"));
			new File("dex2jar/complete").createNewFile();
		}

		// Download build tools
		if (!new File("buildtools/complete").exists()) {
			if (new File("buildtools").exists())
				deleteDir(new File("buildtools"));
			new File("buildtools").mkdirs();
			ProgressWindow.WindowLogger.log("");
			ProgressWindow.WindowLogger
					.log("Downloading Android Build Tools... (note this package is owned by google)");
			ProgressWindow.WindowLogger.setLabel("Downloading Android Build Tools...");
			downloadFile("lightray-work/buildtools/build-tools.zip",
					"https://dl.google.com/android/repository/build-tools_r33-" + platformString() + ".zip");
			ProgressWindow.WindowLogger.log("Extracting buildtools...");
			extractFile("lightray-work/buildtools/build-tools.zip", "lightray-work/buildtools/build-tools-ext");
			ProgressWindow.WindowLogger.log("Moving buildtools...");
			new File("lightray-work/buildtools/build-tools-ext/android-13")
					.renameTo(new File("buildtools/build-tools"));
			deleteDir(new File("lightray-work/buildtools"));
			new File("buildtools/complete").createNewFile();
		}

		// Create keystore
		if (!new File("keystore.jks").exists()) {
			ProgressWindow.WindowLogger.log("");
			ProgressWindow.WindowLogger.log("Generating keystore...");
			ProgressWindow.WindowLogger.setLabel("Generating keystore...");
			String home = System.getProperty("java.home");
			File keytoolFile = new File(home, "bin/keytool");
			ProcessBuilder builder = new ProcessBuilder(keytoolFile.getPath(), "-genkey", "-v", "-keystore",
					"keystore.jks", "-alias", "appmod", "-sigalg", "SHA256withRSA", "-keyalg", "RSA", "-keysize",
					"2048", "-validity", "7300");
			builder.redirectInput(Redirect.PIPE);
			builder.redirectOutput(Redirect.PIPE);
			builder.redirectError(Redirect.PIPE);
			Process proc = builder.start();
			proc.getOutputStream().write("appmod\nappmod\n\n\n\n\n\n\nyes".getBytes());
			proc.getOutputStream().close();
			logAsyncFromStream(proc.getInputStream(), "	  [KEYTOOL] ");
			logAsyncFromStream(proc.getErrorStream(), "	  [KEYTOOL] ");
			proc.waitFor();
			if (proc.exitValue() != 0)
				throw new Exception("Non-zero exit code for KEYTOOL.");
		}

		// Discover mods
		ProgressWindow.WindowLogger.log("");
		ProgressWindow.WindowLogger.log("Discovering modifications...");
		ProgressWindow.WindowLogger.setLabel("Discovering modifications...");

		// Gather active mods
		int max = 0;
		int mPatchers = 0;
		ArrayList<PatchEntry> patches = new ArrayList<PatchEntry>();
		ArrayList<ILightrayPatcher> patchers = new ArrayList<ILightrayPatcher>();
		ArrayList<String> modFiles = new ArrayList<String>();
		for (PatchEntry patch : MainWindow.patches.values()) {
			if (patch.enabled) {
				ProgressWindow.WindowLogger
						.log("Discovered enabled modification: " + patch.name + ", type: " + patch.type);
				if (patch.type == PatchEntryType.COLLECTION) {
					max++;
					patches.add(patch);
				} else if (patch.type == PatchEntryType.PATCHER) {
					mPatchers += MainWindow.patchers.get(patch.name).size();
					patchers.addAll(MainWindow.patchers.get(patch.name));
				} else {
					max++;
					patches.add(patch);
				}
			}
		}

		// Load libraries
		File libsDir = new File("libs");
		libsDir.mkdirs();
		FluidClassPool pool = FluidClassPool.create();
		ProgressWindow.WindowLogger.log("Loading libraries...");
		ProgressWindow.WindowLogger.setLabel("Loading libraries...");
		if (libs == null) {
			libs = new ArrayList<File>();
			loadLibs(libsDir, pool);
		} else {
			// Load libraries from arguments
			for (File m : libs) {
				pool.addSource(m);
				ProgressWindow.WindowLogger.log("  Added: " + m.getName());
			}
		}

		// Load modifications
		ProgressWindow.WindowLogger.setMax(max * 100);
		ProgressWindow.WindowLogger.setValue(0);
		ProgressWindow.WindowLogger.log("Loading modifications...");
		ProgressWindow.WindowLogger.setLabel("Loading modifications...");

		// Manifest override
		boolean changedManifest = false;
		for (PatchEntry entry : patches) {
			// Skip collections
			if (entry.type == PatchEntryType.COLLECTION)
				continue;

			// Check
			InputStream fIn = null;
			ZipFile ar = null;
			if (!entry.preExtracted) {
				// Find entry
				ar = new ZipFile(entry.file);
				ZipEntry ent = ar.getEntry("AndroidManifest.xml");
				if (ent != null) {
					fIn = ar.getInputStream(ent);
				}
				ar.close();
			} else {
				// Find entry
				File ent = new File(entry.file, "AndroidManifest.xml");
				if (ent.exists()) {
					fIn = new FileInputStream(ent);
				}
			}

			// Check manifest
			if (fIn != null) {
				// Dump importing axml
				try {
					// Decode
					Document doc = parseAXML(fIn);

					// Pull API version
					manifestRoot = doc.getDocumentElement();
					sdkVersion = (Element) manifestRoot.getElementsByTagName("uses-sdk")
							.item(0);
					minSdk = sdkVersion.getAttribute("android:minSdkVersion");
					targetSdk = sdkVersion.getAttribute("android:targetSdkVersion");

					// Pull app info
					pkg = manifestRoot.getAttribute("package");
					appVerCode = manifestRoot.getAttribute("android:versionCode");
					appVerName = manifestRoot.getAttribute("android:versionName");
					changedManifest = true;
				} catch (Exception e) {
					// NO
					// Library is unstable lets not crash
				}

				// Close
				fIn.close();
			}
			if (ar != null)
				ar.close();
		}
		if (changedManifest) {
			ProgressWindow.WindowLogger.log("Loaded app manifest patch!");
			ProgressWindow.WindowLogger.log(
					"  Application: " + pkg + " " + appVerName + " (build " + appVerCode
							+ ")");
			ProgressWindow.WindowLogger
					.log("  Target SDK: " + targetSdk + ", minimal SDK: " + minSdk);
		}

		// Extract
		ProgressWindow.WindowLogger.log("Extracting mod resources...");
		ArrayList<String> dexesInjected = new ArrayList<String>();
		HashMap<String, ArrayList<String>> modClassBundles = new HashMap<String, ArrayList<String>>();
		for (PatchEntry entry : patches) {
			// Skip collections
			if (entry.type == PatchEntryType.COLLECTION)
				continue;

			// Extract resources
			ProgressWindow.WindowLogger.log("Applying files from " + entry.name);
			new File("lightray-work/mods").mkdirs();
			if (!entry.preExtracted) {
				// Open archive
				archive = new ZipFile(entry.file);
				int maxC = archive.size();
				float step = 100 / maxC;
				int current = ProgressWindow.WindowLogger.getValue();
				Enumeration<? extends ZipEntry> ents = archive.entries();
				ZipEntry ent = ents.nextElement();
				int i = 0;
				while (ent != null) {
					// Check class compat
					if (ent.getName().endsWith(".class")) {
						String nm = ent.getName().replace("\\", "/");
						while (nm.startsWith("/"))
							nm = nm.substring(1);
						String output = "classes/" + nm;
						if (nm.startsWith("dex/classes")) {
							output = nm.substring(4);
						}

						// Check if META-INF/versions
						String className = output.substring(output.indexOf("/") + 1);
						if (className.startsWith("META-INF/versions/")) {
							// Get version
							String ver = className.substring("META-INF/versions/".length());
							ver = ver.substring(0, ver.indexOf("/"));
							if (ver.matches("^[0-9]+$")) {
								// Java version
								int jvmVersion = Integer.parseInt(ver);

								// Check
								int api = Integer.parseInt(minSdk);
								int maxJvmVer = getMaxJvmVer(api);
								if (jvmVersion > maxJvmVer) {
									// Skip

									// Incompatible
									ent = ents.nextElement();
									continue;
								}
							}
						}
					}

					// Log
					modFiles.add(ent.getName());
					File out = new File("lightray-work/mods", ent.getName());
					if (ent.isDirectory()) {
						out.mkdirs();
					} else {
						ProgressWindow.WindowLogger.log("  Extracting " + ent.getName());
						if (ent.getName().endsWith(".class")) {
							// Check if entry specifies a specific class target
							String nm = ent.getName().replace("\\", "/");
							while (nm.startsWith("/"))
								nm = nm.substring(1);
							String output = "classes/" + nm;
							if (nm.startsWith("dex/classes")) {
								output = nm.substring(4);
							}
							if (!dexesInjected.contains(output.substring(0, output.indexOf("/"))))
								dexesInjected.add(output.substring(0, output.indexOf("/")));

							// Get or create list
							ArrayList<String> fList = modClassBundles.get(entry.name);
							if (fList == null) {
								fList = new ArrayList<String>();
								modClassBundles.put(entry.name, fList);
							}

							// Copy file
							File outp = new File("lightray-work/mod-classes/" + output);
							fList.add(output);
							outp.getParentFile().mkdirs();
							FileOutputStream strm = new FileOutputStream(outp);
							archive.getInputStream(ent).transferTo(strm);
							strm.close();
							modFiles.remove(ent.getName());

							// Check node
							if (outp.exists()
									&& !checkClassVersion(entry.file, output.substring(output.indexOf("/") + 1),
											Integer.parseInt(minSdk))) {
								// Incompatible
								fList.remove(output);
								outp.delete();

								// Warn
								ProgressWindow.WindowLogger.log("    [WARN] Skipping class symbol " + output
										+ ": incompatible with current API version.");
							}

						} else {
							InputStream inp = archive.getInputStream(ent);
							handlePatchResourceFile(out, ent.getName(), inp);
							inp.close();
						}
					}
					if (ents.hasMoreElements())
						ent = ents.nextElement();
					else
						ent = null;
					ProgressWindow.WindowLogger.setValue(current + (int) (step * (float) i++));
				}
				ProgressWindow.WindowLogger.setValue(current + 100);
				archive.close();

				// Determine type
				if (entry.type == PatchEntryType.TRANSFORMER) {
					ProgressWindow.WindowLogger.log("Cleaning transformer files from " + entry.name);
					ZipInputStream strm = new ZipInputStream(new FileInputStream(entry.file));
					pool.addSource(entry.file);
					pool.importArchive(strm);
					strm.close();

					// Check classes
					for (ClassNode node : pool.getLoadedClasses()) {
						if (AnnotationInfo.isAnnotationPresent(FluidTransformer.class, node)) {
							for (File dir : new File("lightray-work/mod-classes").listFiles(t -> t.isDirectory())) {
								if (new File(dir, node.name + ".class").exists()) {
									new File(dir, node.name + ".class").delete();
									ProgressWindow.WindowLogger.log("  Removed " + node.name + ".class");
								}
							}
						}
					}
				}
			} else {
				// Prepare to copy resources
				int maxC = countFiles(entry.file);
				float step = 100 / maxC;
				int current = ProgressWindow.WindowLogger.getValue();

				// Copy
				File out = new File("lightray-work/mods");
				copyPatchResources(entry.file, out, current, step, modFiles, "", 0, dexesInjected, modClassBundles,
						entry, minSdk, pool);
				ProgressWindow.WindowLogger.setValue(current + 100);

				// Determine type
				if (entry.type == PatchEntryType.TRANSFORMER) {
					ProgressWindow.WindowLogger.log("Cleaning transformer files from " + entry.name);
					pool.addSource(entry.file);
					pool.importAllSources();

					// Check classes
					for (ClassNode node : pool.getLoadedClasses()) {
						if (AnnotationInfo.isAnnotationPresent(FluidTransformer.class, node)) {
							for (File dir : new File("lightray-work/mod-classes").listFiles(t -> t.isDirectory())) {
								if (new File(dir, node.name + ".class").exists()) {
									new File(dir, node.name + ".class").delete();
									ProgressWindow.WindowLogger.log("  Removed " + node.name + ".class");
								}
							}
						}
					}
				}
			}
		}
		ProgressWindow.WindowLogger.log("Done.");

		// Apply patchers
		ProgressWindow.WindowLogger.log("Applying patchers...");
		ProgressWindow.WindowLogger.setMax(mPatchers);
		ProgressWindow.WindowLogger.setValue(0);
		for (ILightrayPatcher patcher : patchers) {
			ProgressWindow.WindowLogger.log("  Running patcher: " + patcher.getClass().getTypeName());
			try {
				patcher.apply(inputFile.getPath(), new File("lightray-work"), modFiles, new File("lightray-work/mods"),
						pool);
			} catch (Throwable e) {
				ProgressWindow.WindowLogger.setLabel("Fatal error");
				ProgressWindow.WindowLogger.log("Error: " + e.getClass().getTypeName() + ": " + e.getMessage());
				for (StackTraceElement el : e.getStackTrace())
					ProgressWindow.WindowLogger.log("    At: " + el);
				ProgressWindow.WindowLogger.fatalError(
						"Modification failure!\nFailed to apply patcher: " + patcher.getClass().getTypeName()
								+ "\n\nException: " + e.getClass().getTypeName() + ": " + e.getMessage());
				return;
			}
			ProgressWindow.WindowLogger.increaseProgress();
		}
		ProgressWindow.WindowLogger.log("Generating mod resource manifest..");
		JsonArray arr = new JsonArray();
		for (String resource : modFiles) {
			arr.add(resource);
			ProgressWindow.WindowLogger.log("  Indexed " + resource);
		}
		modFiles.add("assets/lightray-resources.json");
		new File("lightray-work/mods/assets").mkdirs();
		Files.writeString(Path.of("lightray-work/mods/assets/lightray-resources.json"), arr.toString());
		ProgressWindow.WindowLogger.log("Done.");

		// Init FLUID
		ProgressWindow.WindowLogger.log("Initializing FLUID..");
		Fluid.openFluidLoader();
		Fluid.registerAllTransformersFrom(pool);
		Fluid.closeFluidLoader();
		Transformers.initialize();
		for (URL u : pool.getURLSources()) {
			Transformers.addClassSource(u);
		}
		ProgressWindow.WindowLogger.log("Done.");

		// Apply modifications
		ProgressWindow.WindowLogger.log("");
		ProgressWindow.WindowLogger.log("Creating modified APK...");
		ProgressWindow.WindowLogger.setLabel("Creating modified APK...");
		FileOutputStream outp = new FileOutputStream("lightray-work/apks/base.modified.apk");
		ZipOutputStream zipO = new ZipOutputStream(outp);
		archive = new ZipFile(inputFile);

		// Update files
		ProgressWindow.WindowLogger.log("Updating files...");
		int modC = 0;
		for (String file : modFiles) {
			if (archive.getEntry(file) == null)
				modC++;
		}
		ProgressWindow.WindowLogger.setMax(archive.size() + modC);
		ProgressWindow.WindowLogger.setValue(0);
		Enumeration<? extends ZipEntry> ents = archive.entries();
		ZipEntry ent = ents.nextElement();
		ArrayList<String> existingEntries = new ArrayList<String>();
		while (ent != null) {
			existingEntries.add(ent.getName());
			ProgressWindow.WindowLogger.log("  Updating " + ent.getName());

			// Handle
			if (!ent.isDirectory()) {
				InputStream entStrm = archive.getInputStream(ent);

				// Check if its a AXML resource, if so, dump
				if ((ent.getName().endsWith(".xml") || ent.getName().endsWith(".axml"))
						&& !ent.getName().startsWith("assets/") && !ent.getName().startsWith("/assets/")) {
					try {
						// Decode original axml
						new File("lightray-work/axml/" + ent.getName()).getParentFile().mkdirs();
						new File("lightray-work/axml-dump/" + ent.getName()).getParentFile().mkdirs();
						File f = new File("lightray-work/axml/" + ent.getName());
						if (!f.exists()) {
							// Save current
							FileOutputStream strmO = new FileOutputStream(f);
							entStrm.transferTo(strmO);
							strmO.close();
							entStrm = new FileInputStream(f);
						}

						// Decode
						FileInputStream inp = new FileInputStream(f);
						Document doc = parseAXML(inp);
						inp.close();

						// Dump
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						dbf.setNamespaceAware(false);
						DocumentBuilder db = dbf.newDocumentBuilder();
						Document newDoc = db.newDocument();
						NodeList lst = doc.getChildNodes();
						for (int i = 0; i < lst.getLength(); i++) {
							Node node = lst.item(i);
							newDoc.appendChild(newDoc.importNode(node, true));
						}
						FileOutputStream strm = new FileOutputStream(
								new File("lightray-work/axml-dump/" + ent.getName()));
						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						Transformer transformer = transformerFactory.newTransformer();
						transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
						transformer.setOutputProperty(OutputKeys.INDENT, "yes");
						DOMSource source = new DOMSource(newDoc);
						StreamResult result = new StreamResult(strm);
						transformer.transform(source, result);
						strm.close();
					} catch (Exception e) {
						// NO
						// Library is unstable lets not crash
					}
				}

				// Handle mod
				if (modFiles.contains(ent.getName())) {
					ProgressWindow.WindowLogger.log("  Mod install " + ent.getName());

					// Check if its a AXML resource
					if ((ent.getName().endsWith(".xml") || ent.getName().endsWith(".axml"))
							&& !ent.getName().startsWith("assets/") && !ent.getName().startsWith("/assets/")) {
						// Load modded
						File mod = new File("lightray-work/mods/" + ent.getName());
						if (mod.exists()) {
							// Dump importing axml
							try {
								// Decode original axml
								new File("lightray-work/axml-mod-bin/" + ent.getName()).getParentFile().mkdirs();
								new File("lightray-work/axml-mod-dump/" + ent.getName()).getParentFile().mkdirs();
								File f = new File("lightray-work/axml-mod-bin/" + ent.getName());

								// Save current
								FileInputStream fIn = new FileInputStream("lightray-work/mods/" + ent.getName());
								FileOutputStream strmO = new FileOutputStream(f);
								fIn.transferTo(strmO);
								strmO.close();
								fIn.close();
								entStrm = new FileInputStream(f);

								// Decode
								FileInputStream inp = new FileInputStream(f);
								Document doc = parseAXML(inp);
								inp.close();

								// Dump
								DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
								dbf.setNamespaceAware(false);
								DocumentBuilder db = dbf.newDocumentBuilder();
								Document newDoc = db.newDocument();
								NodeList lst = doc.getChildNodes();
								for (int i = 0; i < lst.getLength(); i++) {
									Node node = lst.item(i);
									newDoc.appendChild(newDoc.importNode(node, true));
								}
								FileOutputStream strm = new FileOutputStream(
										new File("lightray-work/axml-mod-dump/" + ent.getName()));
								TransformerFactory transformerFactory = TransformerFactory.newInstance();
								Transformer transformer = transformerFactory.newTransformer();
								transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
								transformer.setOutputProperty(OutputKeys.INDENT, "yes");
								DOMSource source = new DOMSource(newDoc);
								StreamResult result = new StreamResult(strm);
								transformer.transform(source, result);
								strm.close();
							} catch (Exception e) {
								// NO
								// Library is unstable lets not crash
							}
						}
					}

					// Swap streams
					entStrm.close();
					ZipEntry newEnt = new ZipEntry(ent.getName());
					if (ent.getName().equals("resources.arsc") || ent.getName().equals("/resources.arsc")) {
						File file = new File("lightray-work/mods/" + ent.getName());
						newEnt.setMethod(ZipEntry.STORED);
						newEnt.setCrc(computeCrc(file));
						newEnt.setSize(file.length());
						newEnt.setCompressedSize(file.length());
					}
					zipO.putNextEntry(newEnt);
					entStrm = new FileInputStream("lightray-work/mods/" + ent.getName());
				} else if (ent.getName().endsWith(".dex")) {
					// Edit classes
					ProgressWindow.WindowLogger.log("  Processing classes...");
					ProgressWindow.WindowLogger.setLabel("Processing classes...");
					ProgressWindow.WindowLogger.log("    Running dex2jar...");
					FileOutputStream strmO = new FileOutputStream("lightray-work/" + ent.getName());
					String fName = ent.getName().substring(0, ent.getName().lastIndexOf(".dex"));
					entStrm.transferTo(strmO);
					strmO.close();
					entStrm.close();

					// Scan libs
					String jvm = ProcessHandle.current().info().command().get();
					String libs = "";
					for (File lib : new File("dex2jar/lib").listFiles(t -> t.getName().endsWith(".jar"))) {
						if (libs.isEmpty())
							libs = "../dex2jar/lib/" + lib.getName();
						else
							libs += File.pathSeparator + "../dex2jar/lib/" + lib.getName();
					}
					ProcessBuilder builder = new ProcessBuilder(jvm, "-cp", libs,
							"com.googlecode.dex2jar.tools.Dex2jarCmd", ent.getName());
					builder.directory(new File("lightray-work"));
					builder.redirectInput(Redirect.PIPE);
					builder.redirectOutput(Redirect.PIPE);
					builder.redirectError(Redirect.PIPE);
					Process proc = builder.start();
					logAsyncFromStream(proc.getInputStream(), "	  [DEX2JAR] ");
					logAsyncFromStream(proc.getErrorStream(), "	  [DEX2JAR] ");
					proc.waitFor();
					if (proc.exitValue() != 0)
						throw new Exception("Non-zero exit code for dex2jar!\n\n"
								+ "This is most commonly caused by a incompatible java environment.\n\n"
								+ "Try updating your Java installation or try another version of it.\n\n"
								+ "Exit code: " + proc.exitValue());
					ProgressWindow.WindowLogger.log("    Extracting classes...");
					ZipFile archive2 = new ZipFile("lightray-work/" + fName + "-dex2jar.jar");
					new File("lightray-work/" + fName).mkdirs();
					Enumeration<? extends ZipEntry> ents2 = archive2.entries();
					ZipEntry ent2 = ents2.nextElement();
					ArrayList<String> files = new ArrayList<String>();
					while (ent2 != null) {
						File out = new File("lightray-work/" + fName, ent2.getName());
						if (!ent2.isDirectory())
							files.add(ent2.getName());
						if (!out.exists()) {
							// Check if it exists, if it does its a patcher overriding it
							ProgressWindow.WindowLogger.log("      Extracting " + ent2.getName());
							if (ent2.isDirectory()) {
								out.mkdirs();
							} else {
								out.getParentFile().mkdirs();
								FileOutputStream strm = new FileOutputStream(out);
								archive2.getInputStream(ent2).transferTo(strm);
								strm.close();
							}
						}
						if (ents2.hasMoreElements())
							ent2 = ents2.nextElement();
						else
							ent2 = null;
					}
					archive2.close();
					ProgressWindow.WindowLogger.log("    Patching classes...");

					// Install modified classes
					ProgressWindow.WindowLogger.log("    Installing modified classes...");
					for (String file : files) {
						File out = new File("lightray-work/" + fName, file);
						File modded = new File("lightray-work/mod-classes/" + fName + "/" + file);
						File moddedAlt = new File("lightray-work/mod-classes/classes/" + file);
						if (modded.exists()) {
							// Log
							ProgressWindow.WindowLogger.log("        Installing " + file + "...");

							// Delete old
							out.delete();

							// Copy
							Files.copy(modded.toPath(), out.toPath());

							// Delete
							modded.delete();
						} else if (moddedAlt.exists()) {
							// Log
							ProgressWindow.WindowLogger.log("        Installing " + file + "...");

							// Delete old
							out.delete();

							// Copy
							Files.copy(moddedAlt.toPath(), out.toPath());

							// Delete
							moddedAlt.delete();
						}
					}

					// Patch with transformers
					ProgressWindow.WindowLogger.log("    Patching classes with transformers...");

					// Patch classes
					File jar = new File("lightray-work/" + fName + "-dex2jar.jar");
					pool.addSource(jar);
					Transformers.addClassSource(jar);
					boolean hasPatchedClasses = patchClasses(new File("lightray-work/" + fName), fName, "");
					boolean hasNewClasses = dexesInjected.contains(fName);

					// Re-zip
					ProgressWindow.WindowLogger.log("    Zipping classes...");
					FileOutputStream outF = new FileOutputStream("lightray-work/" + fName + ".jar");
					ZipOutputStream clJar = new ZipOutputStream(outF);
					zipAll(new File("lightray-work/" + fName), "", clJar);
					clJar.close();
					outF.close();

					// Check if any user patches were present
					String src = fName + ".jar";
					if (hasNewClasses || hasPatchedClasses) {
						// Run d8
						ProgressWindow.WindowLogger.log("    Running D8...");
						ArrayList<String> cmd = new ArrayList<String>();
						cmd.add(jvm);
						cmd.add("-cp");
						cmd.add(new File("buildtools/build-tools/lib/d8.jar").getCanonicalPath());
						cmd.add("com.android.tools.r8.D8");
						cmd.add("--classfile");
						cmd.add(fName + ".jar");
						cmd.add("--output");
						cmd.add(fName + "-desugared.jar");
						for (File lib : MainWindow.libs) {
							cmd.add("--classpath");
							cmd.add(lib.getCanonicalPath());
						}
						for (PatchEntry entry : MainWindow.patches.values()) {
							if (entry.type == PatchEntryType.COLLECTION)
								continue;
							cmd.add("--classpath");
							cmd.add(entry.file.getCanonicalPath());
						}
						builder = new ProcessBuilder(cmd.toArray(t -> new String[t]));
						builder.directory(new File("lightray-work"));
						builder.redirectInput(Redirect.PIPE);
						builder.redirectOutput(Redirect.PIPE);
						builder.redirectError(Redirect.PIPE);
						proc = builder.start();
						logAsyncFromStream(proc.getInputStream(), "	  [D8] ");
						logAsyncFromStream(proc.getErrorStream(), "	  [D8] ");
						proc.waitFor();
						if (proc.exitValue() != 0)
							ProgressWindow.WindowLogger.log("Warning! Non-zero exit code for d8!\n\n"
									+ "This is most commonly caused by a incompatible java environment.\n\n"
									+ "Try updating your Java installation or try another version of it.\n\n"
									+ "Exit code: " + proc.exitValue()
									+ "\n\nThis might have effect on the usability of the apk!");
						else
							src = fName + "-desugared.jar";
					}

					// Run dx
					ProgressWindow.WindowLogger.log("    Running dx...");
					builder = new ProcessBuilder(jvm, "-cp", libs, "com.android.dx.command.Main", "--dex",
							"--no-strict", "--core-library", "--min-sdk-version", minSdk, "--output",
							fName + "-patched.dex",
							src);
					builder.directory(new File("lightray-work"));
					builder.redirectInput(Redirect.PIPE);
					builder.redirectOutput(Redirect.PIPE);
					builder.redirectError(Redirect.PIPE);
					proc = builder.start();
					logAsyncFromStream(proc.getInputStream(), "	  [DX] ");
					logAsyncFromStream(proc.getErrorStream(), "	  [DX] ");
					proc.waitFor();
					if (proc.exitValue() != 0)
						throw new Exception("Non-zero exit code for dx!\n\n"
								+ "This is most commonly caused by a incompatible java environment.\n\n"
								+ "Try updating your Java installation or try another version of it.\n\n"
								+ "Exit code: " + proc.exitValue());

					// Done
					zipO.putNextEntry(new ZipEntry(ent.getName()));
					entStrm = new FileInputStream("lightray-work/" + fName + "-patched.dex");
					ProgressWindow.WindowLogger.setLabel("Creating modified APK...");
				} else
					zipO.putNextEntry(ent);
				if (entStrm != null) {
					entStrm.transferTo(zipO);
					entStrm.close();
				}
			} else {
				zipO.putNextEntry(new ZipEntry(ent.getName()));
				if (modFiles.contains(ent.getName()))
					modFiles.remove(ent.getName());
			}
			zipO.closeEntry();
			if (ents.hasMoreElements())
				ent = ents.nextElement();
			else
				ent = null;
			ProgressWindow.WindowLogger.increaseProgress();
		}

		// Add remaining classes by patch, each one library
		ProgressWindow.WindowLogger.log("Adding remaining classes...");
		ProgressWindow.WindowLogger.setLabel("Injecting new classes...");
		for (String bundle : modClassBundles.keySet()) {
			ArrayList<String> files = modClassBundles.get(bundle);

			// Check if contents are present
			if (files.stream().anyMatch(t -> new File("lightray-work/mod-classes", t).exists())) {
				// Create dex for this
				ProgressWindow.WindowLogger.log("  Adding classes for " + bundle + "...");

				// Get dex name
				int i = 2;
				File dexOut = new File("lightray-work/classes" + i);
				while (dexOut.exists()) {
					i++;
					dexOut = new File("lightray-work/classes" + i);
				}

				// Create dex folder
				dexOut.mkdirs();
				ProgressWindow.WindowLogger.log("    Finding classes for " + bundle + "...");
				String fName = dexOut.getName();
				for (String file : files) {
					File fSource = new File("lightray-work/mod-classes/" + file);
					File fOut = new File(dexOut, file);
					String name = file.substring(file.indexOf("/") + 1);
					if (fSource.exists()) {
						// Copy file
						ProgressWindow.WindowLogger.log("      Adding " + name + "...");
						fOut.getParentFile().mkdirs();
						if (fOut.exists())
							fOut.delete();
						Files.copy(fSource.toPath(), fOut.toPath());
					}
				}

				// Install dex
				ProgressWindow.WindowLogger.log("  Creating dex for " + bundle + "...");

				// Patch with transformers
				ProgressWindow.WindowLogger.log("    Patching classes with transformers...");

				// Patch classes
				File jar = new File("lightray-work/" + fName + "-dex2jar.jar");
				pool.addSource(jar);
				Transformers.addClassSource(jar);
				patchClasses(new File("lightray-work/" + fName), fName, "");

				// Re-zip
				ProgressWindow.WindowLogger.log("    Zipping classes...");
				FileOutputStream outF = new FileOutputStream("lightray-work/" + fName + ".jar");
				ZipOutputStream clJar = new ZipOutputStream(outF);
				zipAll(new File("lightray-work/" + fName), "", clJar);
				clJar.close();
				outF.close();

				// Scan libs
				String jvm = ProcessHandle.current().info().command().get();
				String libs = "";
				for (File lib : new File("dex2jar/lib").listFiles(t -> t.getName().endsWith(".jar"))) {
					if (libs.isEmpty())
						libs = "../dex2jar/lib/" + lib.getName();
					else
						libs += File.pathSeparator + "../dex2jar/lib/" + lib.getName();
				}

				// Check if any user patches were present
				String src = fName + ".jar";

				// Run d8
				ProgressWindow.WindowLogger.log("    Running D8...");
				ArrayList<String> cmd = new ArrayList<String>();
				cmd.add(jvm);
				cmd.add("-cp");
				cmd.add(new File("buildtools/build-tools/lib/d8.jar").getCanonicalPath());
				cmd.add("com.android.tools.r8.D8");
				cmd.add("--classfile");
				cmd.add(fName + ".jar");
				cmd.add("--output");
				cmd.add(fName + "-desugared.jar");
				for (File lib : MainWindow.libs) {
					cmd.add("--classpath");
					cmd.add(lib.getCanonicalPath());
				}
				for (PatchEntry entry : MainWindow.patches.values()) {
					if (entry.type == PatchEntryType.COLLECTION)
						continue;
					cmd.add("--classpath");
					cmd.add(entry.file.getCanonicalPath());
				}
				ProcessBuilder builder = new ProcessBuilder(cmd.toArray(t -> new String[t]));
				builder.directory(new File("lightray-work"));
				builder.redirectInput(Redirect.PIPE);
				builder.redirectOutput(Redirect.PIPE);
				builder.redirectError(Redirect.PIPE);
				Process proc = builder.start();
				logAsyncFromStream(proc.getInputStream(), "	  [D8] ");
				logAsyncFromStream(proc.getErrorStream(), "	  [D8] ");
				proc.waitFor();
				if (proc.exitValue() != 0)
					ProgressWindow.WindowLogger.log("Warning! Non-zero exit code for d8!\n\n"
							+ "This is most commonly caused by a incompatible java environment.\n\n"
							+ "Try updating your Java installation or try another version of it.\n\n"
							+ "Exit code: " + proc.exitValue()
							+ "\n\nThis might have effect on the usability of the apk!");
				else
					src = fName + "-desugared.jar";

				// Run dx
				ProgressWindow.WindowLogger.log("    Running dx...");
				builder = new ProcessBuilder(jvm, "-cp", libs, "com.android.dx.command.Main", "--dex",
						"--no-strict", "--core-library", "--min-sdk-version", minSdk, "--output",
						fName + "-patched.dex",
						src);
				builder.directory(new File("lightray-work"));
				builder.redirectInput(Redirect.PIPE);
				builder.redirectOutput(Redirect.PIPE);
				builder.redirectError(Redirect.PIPE);
				proc = builder.start();
				logAsyncFromStream(proc.getInputStream(), "	  [DX] ");
				logAsyncFromStream(proc.getErrorStream(), "	  [DX] ");
				proc.waitFor();
				if (proc.exitValue() != 0)
					throw new Exception("Non-zero exit code for dx!\n\n"
							+ "This is most commonly caused by a incompatible java environment.\n\n"
							+ "Try updating your Java installation or try another version of it.\n\n"
							+ "Exit code: " + proc.exitValue());

				// Done
				zipO.putNextEntry(new ZipEntry(fName + ".dex"));
				FileInputStream entStrm = new FileInputStream("lightray-work/" + fName + "-patched.dex");
				entStrm.transferTo(zipO);
				entStrm.close();
				zipO.closeEntry();
			}
		}
		ProgressWindow.WindowLogger.setLabel("Creating modified APK...");

		// Add other files
		ProgressWindow.WindowLogger.log("Adding remaining files...");
		for (String file : new ArrayList<String>(modFiles)) {
			if (existingEntries.contains(file) || file.endsWith("/")) {
				ProgressWindow.WindowLogger.increaseProgress();
				continue; // Skip
			}
			existingEntries.add(file);
			ent = new ZipEntry(file);
			ProgressWindow.WindowLogger.log("  Updating " + ent.getName());
			zipO.putNextEntry(ent);
			if (!file.endsWith("/")) {
				// XML
				boolean compSuccess = false;
				if (!file.startsWith("/assets/") && !file.startsWith("assets/")
						&& (file.endsWith(".axml") || file.endsWith(".xml"))) {
					// Load modded
					File mod = new File("lightray-work/mods/" + ent.getName());
					if (mod.exists()) {
						// Dump importing axml
						try {
							// Decode original axml
							new File("lightray-work/axml-mod-bin/" + ent.getName()).getParentFile().mkdirs();
							new File("lightray-work/axml-mod-dump/" + ent.getName()).getParentFile().mkdirs();
							File f = new File("lightray-work/axml-mod-bin/" + ent.getName());

							// Save current
							FileOutputStream strmO = new FileOutputStream(f);
							InputStream entStrm = new FileInputStream("lightray-work/mods/" + ent.getName());
							entStrm.transferTo(strmO);
							entStrm.close();
							strmO.close();

							// Decode
							FileInputStream inp = new FileInputStream(f);
							Document doc = parseAXML(inp);
							inp.close();

							// Dump
							DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
							dbf.setNamespaceAware(false);
							DocumentBuilder db = dbf.newDocumentBuilder();
							Document newDoc = db.newDocument();
							NodeList lst = doc.getChildNodes();
							for (int i = 0; i < lst.getLength(); i++) {
								Node node = lst.item(i);
								newDoc.appendChild(newDoc.importNode(node, true));
							}
							FileOutputStream strm = new FileOutputStream(
									new File("lightray-work/axml-mod-dump/" + ent.getName()));
							TransformerFactory transformerFactory = TransformerFactory.newInstance();
							Transformer transformer = transformerFactory.newTransformer();
							transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
							transformer.setOutputProperty(OutputKeys.INDENT, "yes");
							DOMSource source = new DOMSource(newDoc);
							StreamResult result = new StreamResult(strm);
							transformer.transform(source, result);
							strm.close();
						} catch (Exception e) {
							// NO
							// Library is unstable lets not crash
						}
					}
				}

				if (!compSuccess) {
					// Write
					InputStream entStrm = new FileInputStream("lightray-work/mods/" + ent.getName());
					modFiles.remove(ent.getName());
					entStrm.transferTo(zipO);
					entStrm.close();
				}
			}
			zipO.closeEntry();
			ProgressWindow.WindowLogger.increaseProgress();
		}

		// Clean
		archive.close();
		zipO.close();
		outp.close();

		// Align apk
		ProgressWindow.WindowLogger.log("");
		ProgressWindow.WindowLogger.log("Creating aligned APK...");
		ProgressWindow.WindowLogger.setLabel("Creating aligned APK...");
		File zipalignFile = new File(
				"buildtools/build-tools/zipalign" + (platformString().equals("windows") ? ".exe" : ""));
		if (!platformString().equals("windows")) {
			try {
				Runtime.getRuntime().exec(new String[] { "chmod", "755", "buildtools/build-tools/zipalign" }).waitFor();
			} catch (Exception e) {
			}
		}
		ProcessBuilder builder = new ProcessBuilder(zipalignFile.getCanonicalPath(), "-fv", "4", "base.modified.apk",
				"base.modified.aligned.apk");
		builder.directory(new File("lightray-work/apks"));
		builder.redirectInput(Redirect.PIPE);
		builder.redirectOutput(Redirect.PIPE);
		builder.redirectError(Redirect.PIPE);
		Process proc = builder.start();
		logAsyncFromStream(proc.getInputStream(), "	  [ZIPALIGN] ");
		logAsyncFromStream(proc.getErrorStream(), "	  [ZIPALIGN] ");
		proc.waitFor();
		if (proc.exitValue() != 0)
			throw new Exception("Non-zero exit code for ZIPALIGN!\n\n"
					+ "This is most commonly caused by a incompatible java environment.\n\n"
					+ "Try updating your Java installation or try another version of it.\n\n" + "Exit code: "
					+ proc.exitValue());

		// Sign apk
		ProgressWindow.WindowLogger.log("");
		ProgressWindow.WindowLogger.log("Signing APK...");
		ProgressWindow.WindowLogger.setLabel("Signing APK...");
		File signApk = new File(
				"buildtools/build-tools/apksigner" + (platformString().equals("windows") ? ".bat" : ""));
		if (!platformString().equals("windows")) {
			try {
				Runtime.getRuntime().exec(new String[] { "chmod", "755", "buildtools/build-tools/apksigner" })
						.waitFor();
			} catch (Exception e) {
			}
		}
		builder = new ProcessBuilder(signApk.getCanonicalPath(), "sign", "--verbose", "--ks", "../../keystore.jks",
				"base.modified.aligned.apk");
		builder.directory(new File("lightray-work/apks"));
		builder.redirectInput(Redirect.PIPE);
		builder.redirectOutput(Redirect.PIPE);
		builder.redirectError(Redirect.PIPE);
		proc = builder.start();
		proc.getOutputStream().write("appmod\n".getBytes());
		proc.getOutputStream().close();
		logAsyncFromStream(proc.getInputStream(), "	  [APKSIGNER] ");
		logAsyncFromStream(proc.getErrorStream(), "	  [APKSIGNER] ");
		proc.waitFor();
		if (proc.exitValue() != 0)
			throw new Exception("Non-zero exit code for APKSIGNER!\n\n"
					+ "This is most commonly caused by a incompatible java environment.\n\n"
					+ "Try updating your Java installation or try another version of it.\n\n" + "Exit code: "
					+ proc.exitValue());
		ProgressWindow.WindowLogger.log("Moving apk...");
		new File("lightray-work/apks/base.modified.aligned.apk")
				.renameTo(new File("lightray-work/apks/base.modified.signed.apk"));

		// Copy final result
		ProgressWindow.WindowLogger.log("Copying final APK...");
		Files.copy(Path.of("lightray-work/apks/base.modified.signed.apk"), outputFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);

		// Complete
		ProgressWindow.WindowLogger.log("");
		ProgressWindow.WindowLogger.log("Completed! Modifications applied successfully!");
		ProgressWindow.WindowLogger.setLabel("Modifications applied successfully!");
	}

	private static int getMaxJvmVer(int api) {
		int maxJvmVer = 8; // Java 8
		if (api >= 34) // API 34+ is java 17
			maxJvmVer = 17;
		else if (api >= 32) // API 32+ is java 11
			maxJvmVer = 11;
		return maxJvmVer;
	}

	private static int getMaxClassVer(int api) {
		int maxClassVer = 52; // 52 = Java 8
		if (api >= 34) // API 34+ is java 17
			maxClassVer = 61; // 61 = java 17
		else if (api >= 32) // API 32+ is java 11
			maxClassVer = 55; // 55 = java 11
		return maxClassVer;
	}

	private static boolean checkClassVersion(File entry, String name, int api) {
		try {
			FluidClassPool pool = FluidClassPool.createEmpty();
			pool.addSource(entry);
			try {
				// Load class
				ClassNode node = pool.getClassNode(name.substring(0, name.lastIndexOf(".class")).replace("/", "."));
				int version = node.version;

				// Check
				int maxClassVer = getMaxClassVer(api);
				if (version > maxClassVer)
					return false;

				// Compatible
				return true;
			} finally {
				pool.close();
			}
		} catch (Exception e) {
			return false;
		}
	}

	private static void logAsyncFromStream(InputStream strm, String prefix) {
		Thread th = new Thread(() -> {
			PrependedBufferStream st = new PrependedBufferStream(strm);
			while (true) {
				try {
					String line = readStreamLine(st);
					if (line == null)
						break;
					ProgressWindow.WindowLogger.log(prefix + line.replace("\r", "").replace("\n", "\n" + prefix));
				} catch (IOException e) {
					break;
				}
			}
		});
		th.setDaemon(true);
		th.setName("Logger");
		th.start();
	}

	private static String readStreamLine(PrependedBufferStream strm) throws IOException {
		// Read a number of bytes
		byte[] content = new byte[20480];
		int read = strm.read(content, 0, content.length);
		if (read <= -1) {
			// Failed
			return null;
		} else {
			// Trim array
			content = Arrays.copyOfRange(content, 0, read);

			// Find newline
			String newData = new String(content, "UTF-8");
			if (newData.contains("\n")) {
				// Found newline
				String line = newData.substring(0, newData.indexOf("\n"));
				int offset = line.length() + 1;
				int returnLength = content.length - offset;
				if (returnLength > 0) {
					// Return
					strm.returnToBuffer(Arrays.copyOfRange(content, offset, content.length));
				}
				return line.replace("\r", "");
			} else {
				// Read more
				while (true) {
					byte[] addition = new byte[20480];
					read = strm.read(addition, 0, addition.length);
					if (read <= -1) {
						// Failed
						strm.returnToBuffer(content);
						return null;
					}

					// Trim
					addition = Arrays.copyOfRange(addition, 0, read);

					// Append
					byte[] newContent = new byte[content.length + addition.length];
					for (int i = 0; i < content.length; i++)
						newContent[i] = content[i];
					for (int i = content.length; i < newContent.length; i++)
						newContent[i] = addition[i - content.length];
					content = newContent;

					// Find newline
					newData = new String(content, "UTF-8");
					if (newData.contains("\n")) {
						// Found newline
						String line = newData.substring(0, newData.indexOf("\n"));
						int offset = line.length() + 1;
						int returnLength = content.length - offset;
						if (returnLength > 0) {
							// Return
							strm.returnToBuffer(Arrays.copyOfRange(content, offset, content.length));
						}
						return line.replace("\r", "");
					}
				}
			}
		}
	}

	private static int copyPatchResources(File source, File outputRoot, int current, float step,
			ArrayList<String> modFiles, String pref, int i, ArrayList<String> dexesInjected,
			HashMap<String, ArrayList<String>> modClassBundles, PatchEntry entry, String minSdk,
			FluidClassPool pool)
			throws IOException, TransformerException, ParserConfigurationException {
		for (File f : source.listFiles()) {
			String name = pref + f.getName() + (f.isDirectory() ? "/" : "");
			modFiles.add(name);
			File out = new File("lightray-work/mods", name);
			if (f.isDirectory()) {
				ProgressWindow.WindowLogger.setValue(current + (int) (step * (float) i++));
				i = copyPatchResources(f, outputRoot, current, step, modFiles, pref + f.getName() + "/", i,
						dexesInjected, modClassBundles, entry, minSdk, pool);
			} else {
				// Check class compat
				if (name.endsWith(".class")) {
					String nm = name;
					while (nm.startsWith("/"))
						nm = nm.substring(1);
					String output = "classes/" + nm;
					if (nm.startsWith("dex/classes")) {
						output = nm.substring(4);
					}

					// Check if META-INF/versions
					String className = output.substring(output.indexOf("/") + 1);
					if (className.startsWith("META-INF/versions/")) {
						// Get version
						String ver = className.substring("META-INF/versions/".length());
						ver = ver.substring(0, ver.indexOf("/"));
						if (ver.matches("^[0-9]+$")) {
							// Java version
							int jvmVersion = Integer.parseInt(ver);

							// Check
							int api = Integer.parseInt(minSdk);
							int maxJvmVer = getMaxJvmVer(api);
							if (jvmVersion > maxJvmVer) {
								// Skip

								// Incompatible
								continue;
							}
						}
					}
				}

				// Handle
				ProgressWindow.WindowLogger.log("  Extracting " + name);
				out.getParentFile().mkdirs();
				if (name.endsWith(".class")) {
					// Check if entry specifies a specific class target
					String nm = name;
					while (nm.startsWith("/"))
						nm = nm.substring(1);
					String output = "classes/" + nm;
					if (nm.startsWith("dex/classes")) {
						output = nm.substring(4);
					}
					if (!dexesInjected.contains(output.substring(0, output.indexOf("/"))))
						dexesInjected.add(output.substring(0, output.indexOf("/")));

					// Get or create list
					ArrayList<String> fList = modClassBundles.get(entry.name);
					if (fList == null) {
						fList = new ArrayList<String>();
						modClassBundles.put(entry.name, fList);
					}

					// Copy file
					File outp = new File("lightray-work/mod-classes/" + output);
					fList.add(output);
					outp.getParentFile().mkdirs();
					FileOutputStream strm = new FileOutputStream(outp);
					InputStream inp = new FileInputStream(f);
					inp.transferTo(strm);
					strm.close();
					inp.close();
					modFiles.remove(name);

					// Check node
					if (outp.exists() && !checkClassVersion(entry.file, output.substring(output.indexOf("/") + 1),
							Integer.parseInt(minSdk))) {
						// Incompatible
						fList.remove(output);
						outp.delete();

						// Warn
						ProgressWindow.WindowLogger.log("    [WARN] Skipping class symbol " + output
								+ ": incompatible with current API version.");
					}
				} else {
					InputStream inp = new FileInputStream(f);
					handlePatchResourceFile(out, name, inp);
					inp.close();
				}
				ProgressWindow.WindowLogger.setValue(current + (int) (step * (float) i++));
			}
		}
		return i;
	}

	private static void handlePatchResourceFile(File out, String pth, InputStream inp)
			throws IOException, TransformerException, ParserConfigurationException {
		out.getParentFile().mkdirs();
		FileOutputStream strm = new FileOutputStream(out);
		inp.transferTo(strm);
		strm.close();
	}

	private static void buildFromCli(String[] args) {
		// Parse arguments
		boolean ignorePatched = false;
		File sourceFile = null;
		File destFile = null;
		ArrayList<File> patches = new ArrayList<File>();
		ArrayList<File> plainPatches = new ArrayList<File>();
		libs = new ArrayList<File>();
		for (int i = 0; i < args.length; i++) {
			// Handle
			String arg = args[i];
			if (arg.equalsIgnoreCase("--apk-source")) {
				if (i + 1 < args.length) {
					// Assign source
					sourceFile = new File(args[i + 1]);

					// Skip next
					i++;
				}
			} else if (arg.equalsIgnoreCase("--apk-dest")) {
				if (i + 1 < args.length) {
					// Assign destination
					destFile = new File(args[i + 1]);

					// Skip next
					i++;
				}
			} else if (arg.equalsIgnoreCase("--add-patch")) {
				if (i + 1 < args.length) {
					// Add patch file
					for (String a : args[i + 1].split(File.pathSeparator)) {
						File patch = new File(a);
						if (patch.exists())
							patches.add(patch);
					}

					// Skip next
					i++;
				}
			} else if (arg.equalsIgnoreCase("--add-folder-patch")) {
				if (i + 1 < args.length) {
					// Add patch file
					for (String a : args[i + 1].split(File.pathSeparator)) {
						File patch = new File(a);
						if (patch.exists() && patch.isDirectory())
							plainPatches.add(patch);
					}

					// Skip next
					i++;
				}
			} else if (arg.equalsIgnoreCase("--add-lib")) {
				if (i + 1 < args.length) {
					// Add lib file
					for (String a : args[i + 1].split(File.pathSeparator)) {
						File lib = new File(a);
						if (lib.exists())
							libs.add(lib);
					}

					// Skip next
					i++;
				}
			} else if (arg.equalsIgnoreCase("--ignore-patched")) {
				ignorePatched = true;
			}
		}

		// Check source
		if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
			if (sourceFile == null)
				System.err.println("Missing '--apk-source' argument");
			else
				System.err.println("Invalid value for '--apk-source', expected a apk file");
			System.exit(1);
		}

		// Check destination
		if (destFile == null)
			destFile = new File(sourceFile.getParentFile(),
					sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf(".")) + " (patched).apk");
		else if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
			System.err.println("Invalid value for '--apk-dest', parent path does not exist");
			System.exit(1);
		}

		// Log
		System.out.println("AerialWorks LightrayInjector loading...");
		System.out.println("Source file: " + sourceFile.getAbsolutePath());
		System.out.println("Destination file: " + destFile.getAbsolutePath());
		System.out.println();

		// Check archive
		try {
			System.out.println("Verifying archive...");
			ZipFile archive = new ZipFile(sourceFile);
			boolean isAndroid = archive.getEntry("AndroidManifest.xml") != null;
			boolean wasModified = archive.getEntry("assets/lightray-resources.json") != null;
			archive.close();
			if (!isAndroid) {
				System.err.println("Invalid APK file, please select a Android APK file.\n\nFile " + sourceFile.getName()
						+ " does not contain a android app manifest!");
				System.exit(1);
				return;
			}
			if (wasModified) {
				if (ignorePatched) {
					// Warn
					System.err.println();
					System.err.println(
							"WARNING!\n\nIt is highly recommended to NOT use already-patched APK files else things WILL break!\n\n"
									+ "If any issues occur make sure to use a unpatched APK before reporting!");
					System.err.println();
				} else {
					// Fail
					System.err.println();
					System.err.println(
							"It is highly recommended to NOT use already-patched APK files else things WILL break!\n"
									+ "Any old mod resources in the APK will no longer function correctly if you proceed!\n"
									+ "If there were any transformers applied, they will remain in the APK, beware of duplicate transformers!\n\n"
									+ "If you wish to proceed, add '--ignore-patched' to the command arguments");
					System.err.println();
					System.exit(1);
				}
			}
		} catch (Exception e) {
			System.err.println(
					"Invalid APK file, please select a Android APK file.\n\nCould not read " + sourceFile.getName());
			System.exit(1);
		}

		// Load modifications
		System.out.println("Loading patches..");
		for (File patch : patches) {
			System.out.println("  Loading patch: " + patch);
			PatchEntry e = loadMod(patch, patch.getParentFile() == null ? "" : patch.getParentFile().getPath() + "/",
					false, null);
			setEnabledRecursive(e, true);
		}
		for (File patch : plainPatches) {
			System.out.println("  Loading plain folder patch: " + patch);
			PatchEntry e = loadExtractedMod(patch, false);
			e.enabled = true;
		}

		// Start patcher
		try {
			System.out.println("Starting LightrayInjector...");
			System.out.println("");
			apply(sourceFile, destFile);
			System.out.println("Saved at: '" + destFile.getAbsolutePath());
		} catch (Throwable e) {
			ProgressWindow.WindowLogger.log("Error: " + e.getClass().getTypeName() + ": " + e.getMessage());
			for (StackTraceElement el : e.getStackTrace())
				ProgressWindow.WindowLogger.log("    At: " + el);
			ProgressWindow.WindowLogger.fatalError(
					"Modification failure!\nException: " + e.getClass().getTypeName() + ": " + e.getMessage());
			System.exit(1);
		}
	}

	private void recurseAddKeyHandler(Component comp) {
		comp.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftDown = true;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SHIFT)
					shiftDown = false;
			}
		});
		if (comp instanceof Container) {
			for (Component c : ((Container) comp).getComponents()) {
				recurseAddKeyHandler(c);
			}
		}
	}

	private static int countFiles(File source) {
		if (!source.isDirectory())
			return 1;
		int c = source.listFiles(t -> !t.isDirectory()).length;
		for (File f : source.listFiles(t -> t.isDirectory())) {
			c += countFiles(f);
			c++;
		}
		return c;
	}

	private static void loadLibs(File libs, FluidClassPool pool) {
		// Scan folder
		for (File m : libs.listFiles()) {
			if (m.isDirectory())
				loadLibs(m, pool);
			else if (m.getName().endsWith(".jar") || m.getName().endsWith(".zip")) {
				pool.addSource(m);
				MainWindow.libs.add(m);
				ProgressWindow.WindowLogger.log("  Added: " + m.getName());
			}
		}
	}

	private static void scan(File mods, String pref, PatchEntry parent) {
		// Scan folder
		for (File m : mods.listFiles()) {
			if (m.isDirectory()) {
				// Load collection
				PatchEntry col = new PatchEntry();
				if (parent != null)
					parent.childEntries.add(col);
				col.file = m;
				col.name = pref + m.getName();
				col.type = PatchEntryType.COLLECTION;
				col.parent = parent;
				col.box = new JCheckBox(
						parentName(parent) + m.getName() + " (" + col.type.toString().toLowerCase() + ")");
				patches.put(col.name, col);
				scan(m, pref + m.getName() + "/", col);
			} else if (m.getName().endsWith(".jar") || m.getName().endsWith(".zip"))
				loadMod(m, pref, true, parent);
		}
	}

	private static String parentName(PatchEntry entry) {
		if (entry != null)
			return parentName(entry.parent) + entry.file.getName() + "/";
		return "";
	}

	private void scanPlain(File mods) {
		// Scan folder
		for (File m : mods.listFiles(t -> t.isDirectory())) {
			loadExtractedMod(m, true);
		}
	}

	private static boolean patchClasses(File dir, String fName, String pref) throws IOException {
		// Scan folder
		boolean res = false;
		for (File m : dir.listFiles()) {
			if (m.isDirectory()) {
				if (patchClasses(m, fName, pref + m.getName() + "/"))
					res = true;
			} else if (m.getName().endsWith(".class")) {
				if (patchClass(m, fName, pref))
					res = true;
			}
		}
		return res;
	}

	private static boolean patchClass(File classFile, String fName, String pref) throws IOException {
		String className = pref + classFile.getName().substring(0, classFile.getName().lastIndexOf(".class"));

		// Check transformer
		if (!Transformers.hasTransformers(className))
			return false;

		// Apply patch
		ProgressWindow.WindowLogger.log("      Patching: " + className);
		byte[] bytecode = Files.readAllBytes(classFile.toPath());
		byte[] modified = Transformers.applyTransformers(className, bytecode, dynLoader);
		if (modified != null) {
			// Write
			Files.write(classFile.toPath(), modified);
		}
		return true;
	}

	private static void zipAll(File dir, String pref, ZipOutputStream zOut) throws IOException {
		// Zip folder
		for (File m : dir.listFiles()) {
			if (m.isDirectory()) {
				zOut.putNextEntry(new ZipEntry(pref + m.getName() + "/"));
				zOut.closeEntry();
				zipAll(m, pref + m.getName() + "/", zOut);
				ProgressWindow.WindowLogger.log("      Zipping: " + pref + m.getName() + "/");
			}
		}
		for (File m : dir.listFiles()) {
			if (!m.isDirectory()) {
				zOut.putNextEntry(new ZipEntry(pref + m.getName()));
				FileInputStream strm = new FileInputStream(m);
				strm.transferTo(zOut);
				strm.close();
				zOut.closeEntry();
				ProgressWindow.WindowLogger.log("      Zipping: " + pref + m.getName());
			}
		}
	}

	private static PatchEntry loadMod(File mod, String pref, boolean windowed, PatchEntry parent) {
		// Check type
		if (mod.isDirectory()) {
			// Load collection
			PatchEntry col = new PatchEntry();
			if (parent != null)
				parent.childEntries.add(col);
			col.file = mod;
			col.name = pref + mod.getName();
			col.type = PatchEntryType.COLLECTION;
			col.parent = parent;
			col.box = new JCheckBox(
					parentName(parent) + mod.getName() + " (" + col.type.toString().toLowerCase() + ")");
			patches.put(col.name, col);
			scan(mod, pref + mod.getName() + "/", col);
			return col;
		}

		// Load mod
		PatchEntry patch = new PatchEntry();
		patch.file = mod;
		patch.name = pref + mod.getName();
		patch.type = PatchEntryType.RESOURCE;
		patch.parent = parent;

		// Check parent
		if (parent != null)
			parent.childEntries.add(patch);

		// Load into memory
		try {
			// Determine type
			if (mod.getName().endsWith(".jar")) {
				FluidClassPool pool = FluidClassPool.createEmpty();
				try {
					ZipInputStream strm = new ZipInputStream(new FileInputStream(mod));
					pool.importArchive(strm);
					strm.close();

					// Check classes
					for (ClassNode node : pool.getLoadedClasses()) {
						if (AnnotationInfo.isAnnotationPresent(FluidTransformer.class, node)) {
							patch.type = PatchEntryType.TRANSFORMER;
						} else if (AnnotationInfo.isAnnotationPresent(LightrayPatcher.class, node)) {
							patch.type = PatchEntryType.PATCHER;
							break;
						}
					}

					// Load patcher if needed
					if (patch.type == PatchEntryType.PATCHER) {
						// Load patcher classes
						dynLoader.addUrl(mod.toURI().toURL());
						patchers.put(patch.name, new ArrayList<ILightrayPatcher>());
						for (ClassNode node : pool.getLoadedClasses()) {
							if (AnnotationInfo.isAnnotationPresent(LightrayPatcher.class, node)) {
								// Load patcher
								Class<?> cls = dynLoader.loadClass(node.name.replace("/", "."));
								if (!ILightrayPatcher.class.isAssignableFrom(cls)) {
									// Error, invalid type
									if (windowed) {
										JOptionPane.showMessageDialog(winInst.frmLightray,
												"An error occured loading modification: " + patch.name + "\n" + "\n"
														+ "Error: the patcher '" + cls.getTypeName()
														+ "' does not inherit the ILightrayPatcher interface.",
												"Error", JOptionPane.ERROR_MESSAGE);
									} else {
										System.err.println("An error occured loading modification: " + patch.name + "\n"
												+ "\n" + "Error: the patcher '" + cls.getTypeName()
												+ "' does not inherit the ILightrayPatcher interface.");
										System.exit(1);
									}
									return null;
								}

								// Create instance
								patchers.get(patch.name).add((ILightrayPatcher) cls.getConstructor().newInstance());
							}
						}
					}
				} finally {
					// Close pool
					pool.close();
				}
			}
		} catch (Exception e) {
			if (windowed) {
				JOptionPane.showMessageDialog(winInst.frmLightray,
						"An error occured loading modification: " + patch.name + "\n" + "\n" + "Exception: " + e,
						"Error", JOptionPane.ERROR_MESSAGE);
			} else {
				System.err.println(
						"An error occured loading modification: " + patch.name + "\n" + "\n" + "Exception: " + e);
				System.exit(1);
			}
			return null;
		}
		patch.box = new JCheckBox(
				parentName(parent) + mod.getName() + " (" + patch.type.toString().toLowerCase() + ")");

		// Add
		patches.put(patch.name, patch);
		return patch;
	}

	private static PatchEntry loadExtractedMod(File mod, boolean windowed) {
		// Load mod
		PatchEntry patch = new PatchEntry();
		patch.file = mod;
		patch.name = mod.getName();
		patch.type = PatchEntryType.RESOURCE;
		patch.preExtracted = true;

		// Load into memory
		try {
			// Determine type
			FluidClassPool pool = FluidClassPool.createEmpty();
			try {
				// Import
				pool.addSource(mod);
				pool.importAllSources();

				// Check classes
				for (ClassNode node : pool.getLoadedClasses()) {
					if (AnnotationInfo.isAnnotationPresent(FluidTransformer.class, node)) {
						patch.type = PatchEntryType.TRANSFORMER;
					} else if (AnnotationInfo.isAnnotationPresent(LightrayPatcher.class, node)) {
						patch.type = PatchEntryType.PATCHER;
						break;
					}
				}

				// Load patcher if needed
				if (patch.type == PatchEntryType.PATCHER) {
					// Load patcher classes
					dynLoader.addUrl(mod.toURI().toURL());
					patchers.put(patch.name, new ArrayList<ILightrayPatcher>());
					for (ClassNode node : pool.getLoadedClasses()) {
						if (AnnotationInfo.isAnnotationPresent(LightrayPatcher.class, node)) {
							// Load patcher
							Class<?> cls = dynLoader.loadClass(node.name.replace("/", "."));
							if (!ILightrayPatcher.class.isAssignableFrom(cls)) {
								// Error, invalid type
								if (windowed) {
									JOptionPane.showMessageDialog(winInst.frmLightray,
											"An error occured loading modification: " + patch.name + "\n" + "\n"
													+ "Error: the patcher '" + cls.getTypeName()
													+ "' does not inherit the ILightrayPatcher interface.",
											"Error", JOptionPane.ERROR_MESSAGE);
								} else {
									System.err.println("An error occured loading modification: " + patch.name + "\n"
											+ "\n" + "Error: the patcher '" + cls.getTypeName()
											+ "' does not inherit the ILightrayPatcher interface.");
									System.exit(1);
								}
								return null;
							}

							// Create instance
							patchers.get(patch.name).add((ILightrayPatcher) cls.getConstructor().newInstance());
						}
					}
				}
			} finally {
				// Close pool
				pool.close();
			}
		} catch (Exception e) {
			if (windowed) {
				JOptionPane.showMessageDialog(winInst.frmLightray,
						"An error occured loading modification: " + patch.name + "\n" + "\n" + "Exception: " + e,
						"Error", JOptionPane.ERROR_MESSAGE);
			} else {
				System.err.println(
						"An error occured loading modification: " + patch.name + "\n" + "\n" + "Exception: " + e);
				System.exit(1);
			}
			return null;
		}
		patch.box = new JCheckBox(mod.getName() + " (" + patch.type.toString().toLowerCase() + ")");

		// Add
		patches.put(patch.name, patch);
		return patch;
	}

	private void saveModInfo() {
		// Save active mod info json
		JsonArray arr = new JsonArray();
		for (PatchEntry patch : patches.values()) {
			if (patch.enabled)
				arr.add(patch.name);
		}
		try {
			Files.writeString(Path.of("activepatches.json"), arr.toString());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(frmLightray,
					"An unknown error occured saving active modifications.\n" + "\n" + "Exception: " + e, "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private static String platformString() {
		String info = System.getProperty("os.name").toLowerCase();
		if (info.startsWith("windows"))
			return "windows";
		else if (info.startsWith("mac") || info.startsWith("osx") // Just in case
				|| info.startsWith("darwin"))
			return "macosx";
		else if (info.startsWith("linux"))
			return "linux";
		else
			return null;
	}

	private static void deleteDir(File dir) {
		for (File d : dir.listFiles(t -> t.isDirectory()))
			deleteDir(d);
		for (File d : dir.listFiles(t -> !t.isDirectory()))
			d.delete();
		dir.delete();
	}

	private static void extractFile(String source, String output) throws IOException {
		new File(output).mkdirs();
		ZipFile archive = new ZipFile(source);
		ProgressWindow.WindowLogger.setMax(archive.size());
		Enumeration<? extends ZipEntry> ents = archive.entries();
		ZipEntry ent = ents.nextElement();
		while (ent != null) {
			ProgressWindow.WindowLogger.log("  Extracting " + ent.getName());
			File out = new File(output, ent.getName());
			if (ent.isDirectory()) {
				out.mkdirs();
			} else {
				out.getParentFile().mkdirs();
				FileOutputStream strm = new FileOutputStream(out);
				archive.getInputStream(ent).transferTo(strm);
				strm.close();
			}
			if (ents.hasMoreElements())
				ent = ents.nextElement();
			else
				ent = null;
			ProgressWindow.WindowLogger.increaseProgress();
		}
		archive.close();
		ProgressWindow.WindowLogger.setMax(100);
		ProgressWindow.WindowLogger.setValue(100);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		ProgressWindow.WindowLogger.setMax(100);
		ProgressWindow.WindowLogger.setValue(0);
	}

	private static void downloadFile(String output, String url) throws IOException {
		new File(output).getParentFile().mkdirs();
		ProgressWindow.WindowLogger.log("Downloading " + url + " -> " + output + "...");
		URLConnection strm = new URL(url).openConnection();
		FileOutputStream outp = new FileOutputStream(output);
		download(strm, outp);
		outp.close();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
		ProgressWindow.WindowLogger.setMax(100);
		ProgressWindow.WindowLogger.setValue(0);
	}

	private static void download(URLConnection urlConnection, OutputStream output) throws IOException {
		// Prepare
		InputStream data = urlConnection.getInputStream();
		ProgressWindow.WindowLogger.setMax(urlConnection.getContentLength() / 1000);
		ProgressWindow.WindowLogger.setValue(0);

		// Download
		int i = 1;
		while (true) {
			byte[] b = data.readNBytes(1000);
			if (b.length == 0)
				break;
			else {
				output.write(b);
				ProgressWindow.WindowLogger.setValue(i++);
			}
		}
		ProgressWindow.WindowLogger.setMax(100);
		ProgressWindow.WindowLogger.setValue(100);
		data.close();
	}

	private static Document parseAXML(InputStream inp) throws IOException, ParserConfigurationException {
		AxmlReader reader = new AxmlReader(inp.readAllBytes());
		return parseAxml(reader);
	}

	private static Document parseAxml(AxmlReader reader) throws IOException, ParserConfigurationException {
		// Create new document container
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document newDoc = db.newDocument();

		// Start reading
		HashMap<String, String> attrs = new HashMap<String, String>();
		HashMap<String, String> namespaces = new HashMap<String, String>();
		reader.accept(new AxmlVisitor() {

			@Override
			public void attr(String ns, String name, int resourceId, int type, Object obj) {
				super.attr(ns, name, resourceId, type, obj);
				attrs.put((ns == null ? "" : ns + ":") + name, name);
			}

			@Override
			public NodeVisitor child(String ns, String name) {
				return new ChildNodeVisitor(namespaces, ns, name, newDoc, newDoc);
			}

			@Override
			public void text(int lineNumber, String value) {
				super.text(lineNumber, value);
			}

			@Override
			public void ns(String prefix, String uri, int ln) {
				super.ns(prefix, uri, ln);
				attrs.put("xmlns:" + prefix, uri);
				namespaces.put(uri, prefix);
			}

		});
		if (newDoc.getDocumentElement() != null) {
			// Assign attributes
			attrs.forEach((key, val) -> newDoc.getDocumentElement().setAttribute(key, val));
		}
		return newDoc;
	}

	private static long computeCrc(File file) throws IOException {
		CRC32 crc = new CRC32();
		FileInputStream in = new FileInputStream(file);
		while (true) {
			byte[] buf = new byte[2048];
			int read = in.read(buf);
			if (read <= 0)
				break;
			crc.update(buf, 0, read);
		}
		in.close();
		return crc.getValue();
	}
}
