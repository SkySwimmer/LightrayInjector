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

import org.asf.cyan.fluid.DynamicClassLoader;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.Transformers;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.tree.ClassNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
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

	private JFrame frmLightray;
	private JTextField textField;
	private boolean shiftDown;
	private DynamicClassLoader dynLoader = new DynamicClassLoader();
	private HashMap<String, List<ILightrayPatcher>> patchers = new HashMap<String, List<ILightrayPatcher>>();
	private HashMap<String, PatchEntry> patches = new HashMap<String, PatchEntry>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
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

				frmLightray.dispose();
				Thread th = new Thread(() -> {
					try {
						EventQueue.invokeAndWait(() -> {
							ProgressWindow.WindowLogger.showWindow();
						});
					} catch (InvocationTargetException | InterruptedException e1) {
					}
					ProgressWindow.WindowLogger.setLabel("Preparing...");
					ProgressWindow.WindowLogger.log("Processing...");

					try {
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
							ProgressWindow.WindowLogger
									.log("Downloading dex2jar... (note this package is owned by pxb1988)");
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
									"https://dl.google.com/android/repository/build-tools_r33-" + platformString()
											+ ".zip");
							ProgressWindow.WindowLogger.log("Extracting buildtools...");
							extractFile("lightray-work/buildtools/build-tools.zip",
									"lightray-work/buildtools/build-tools-ext");
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
							ProcessBuilder builder = new ProcessBuilder(keytoolFile.getPath(), "-genkey", "-v",
									"-keystore", "keystore.jks", "-alias", "appmod", "-sigalg", "SHA256withRSA",
									"-keyalg", "RSA", "-keysize", "2048", "-validity", "7300");
							builder.redirectInput(Redirect.PIPE);
							builder.redirectOutput(Redirect.PIPE);
							builder.redirectError(Redirect.PIPE);
							Process proc = builder.start();
							proc.getOutputStream().write("appmod\nappmod\n\n\n\n\n\n\nyes".getBytes());
							proc.getOutputStream().close();
							ProgressWindow.WindowLogger.log(new String(proc.getInputStream().readAllBytes(), "UTF-8")
									.trim().replace("\r", "").replace("\n", "\n    [KEYTOOL] "));
							ProgressWindow.WindowLogger.log(new String(proc.getErrorStream().readAllBytes(), "UTF-8")
									.trim().replace("\r", "").replace("\n", "\n    [KEYTOOL] "));
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
						for (PatchEntry patch : MainWindow.this.patches.values()) {
							if (patch.enabled) {
								ProgressWindow.WindowLogger.log(
										"Discovered enabled modification: " + patch.name + ", type: " + patch.type);
								if (patch.type == PatchEntryType.PATCHER) {
									mPatchers += MainWindow.this.patchers.get(patch.name).size();
									patchers.addAll(MainWindow.this.patchers.get(patch.name));
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
						loadLibs(libsDir, pool);

						// Load modifications
						ProgressWindow.WindowLogger.setMax(max);
						ProgressWindow.WindowLogger.setValue(0);
						ProgressWindow.WindowLogger.log("Loading modifications...");
						ProgressWindow.WindowLogger.setLabel("Loading modifications...");
						ProgressWindow.WindowLogger.log("Extracting mod resources...");
						for (PatchEntry entry : patches) {
							// Extract resources
							ProgressWindow.WindowLogger.log("Applying files from " + entry.name);
							new File("lightray-work/mods").mkdirs();
							File mod = new File("patches/" + entry.name);
							ZipFile archive = new ZipFile(mod);
							ProgressWindow.WindowLogger.setMax(archive.size());
							Enumeration<? extends ZipEntry> ents = archive.entries();
							ZipEntry ent = ents.nextElement();
							while (ent != null) {
								ProgressWindow.WindowLogger.log("  Extracting " + ent.getName());
								modFiles.add(ent.getName());
								File out = new File("lightray-work/mods", ent.getName());
								if (ent.isDirectory()) {
									out.mkdirs();
								} else {
									if (ent.getName().endsWith(".class")) {
										// Copy file
										File outp = new File("lightray-work/classes/" + ent.getName());
										outp.getParentFile().mkdirs();
										FileOutputStream strm = new FileOutputStream(outp);
										archive.getInputStream(ent).transferTo(strm);
										strm.close();
										modFiles.remove(ent.getName());
									} else {
										out.getParentFile().mkdirs();
										FileOutputStream strm = new FileOutputStream(out);
										archive.getInputStream(ent).transferTo(strm);
										strm.close();
									}
								}
								if (ents.hasMoreElements())
									ent = ents.nextElement();
								else
									ent = null;
							}
							archive.close();

							// Determine type
							if (entry.type == PatchEntryType.TRANSFORMER) {
								ProgressWindow.WindowLogger.log("Cleaning transformer files from " + entry.name);
								ZipInputStream strm = new ZipInputStream(new FileInputStream(mod));
								pool.addSource(mod);
								pool.importArchive(strm);
								strm.close();

								// Check classes
								for (ClassNode node : pool.getLoadedClasses()) {
									if (AnnotationInfo.isAnnotationPresent(FluidTransformer.class, node)) {
										new File("lightray-work/classes/" + node.name + ".class").delete();
										ProgressWindow.WindowLogger.log("  Removed " + node.name + ".class");
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
								patcher.apply(textField.getText(), new File("lightray-work"), modFiles,
										new File("lightray-work/mods"), pool);
							} catch (Throwable e) {
								ProgressWindow.WindowLogger.setLabel("Fatal error");
								ProgressWindow.WindowLogger
										.log("Error: " + e.getClass().getTypeName() + ": " + e.getMessage());
								for (StackTraceElement el : e.getStackTrace())
									ProgressWindow.WindowLogger.log("    At: " + el);
								ProgressWindow.WindowLogger
										.fatalError("Modification failure!\nFailed to apply patcher: "
												+ patcher.getClass().getTypeName() + "\n\nException: "
												+ e.getClass().getTypeName() + ": " + e.getMessage());
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
						ZipFile archive = new ZipFile(textField.getText());

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
							zipO.putNextEntry(ent);
							InputStream entStrm = archive.getInputStream(ent);
							if (!ent.isDirectory()) {
								if (modFiles.contains(ent.getName())) {
									ProgressWindow.WindowLogger.log("  Mod install " + ent.getName());

									// Swap streams
									entStrm.close();
									entStrm = new FileInputStream("lightray-work/mods/" + ent.getName());
									modFiles.remove(ent.getName());
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
									for (File lib : new File("dex2jar/lib")
											.listFiles(t -> t.getName().endsWith(".jar"))) {
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
									ProgressWindow.WindowLogger.log("      [DEX2JAR] "
											+ new String(proc.getInputStream().readAllBytes(), "UTF-8").trim()
													.replace("\r", "").replace("\n", "\n      [DEX2JAR] "));
									ProgressWindow.WindowLogger.log("      [DEX2JAR] "
											+ new String(proc.getErrorStream().readAllBytes(), "UTF-8").trim()
													.replace("\r", "").replace("\n", "\n      [DEX2JAR] "));
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
									while (ent2 != null) {
										File out = new File("lightray-work/" + fName, ent2.getName());
										if (!out.exists()) { // Check if it exists, if it does its a patcher overriding
																// it
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
									ProgressWindow.WindowLogger.setLabel("    Patching classes...");

									// Patch classes
									File jar = new File("lightray-work/" + fName + "-dex2jar.jar");
									pool.addSource(jar);
									Transformers.addClassSource(jar);
									patchClasses(new File("lightray-work/" + fName), "");

									// Re-zip
									ProgressWindow.WindowLogger.log("    Zipping classes...");
									FileOutputStream outF = new FileOutputStream("lightray-work/" + fName + ".jar");
									ZipOutputStream clJar = new ZipOutputStream(outF);
									zipAll(new File("lightray-work/" + fName), "", clJar);
									clJar.close();
									outF.close();

									// Run jar2dex
									ProgressWindow.WindowLogger.log("    Running jar2dex...");
									builder = new ProcessBuilder(jvm, "-cp", libs,
											"com.googlecode.dex2jar.tools.Jar2Dex", fName + ".jar");
									builder.directory(new File("lightray-work"));
									builder.redirectInput(Redirect.PIPE);
									builder.redirectOutput(Redirect.PIPE);
									builder.redirectError(Redirect.PIPE);
									proc = builder.start();
									ProgressWindow.WindowLogger.log("      [JAR2DEX] "
											+ new String(proc.getInputStream().readAllBytes(), "UTF-8").trim()
													.replace("\r", "").replace("\n", "\n      [JAR2DEX] "));
									ProgressWindow.WindowLogger.log("      [JAR2DEX] "
											+ new String(proc.getErrorStream().readAllBytes(), "UTF-8").trim()
													.replace("\r", "").replace("\n", "\n      [JAR2DEX] "));
									proc.waitFor();
									if (proc.exitValue() != 0)
										throw new Exception("Non-zero exit code for jar2dex!\n\n"
												+ "This is most commonly caused by a incompatible java environment.\n\n"
												+ "Try updating your Java installation or try another version of it.\n\n"
												+ "Exit code: " + proc.exitValue());

									// Done
									entStrm = new FileInputStream("lightray-work/" + fName + "-jar2dex.dex");
									ProgressWindow.WindowLogger.setLabel("Creating modified APK...");
								}
								entStrm.transferTo(zipO);
								entStrm.close();
							} else if (modFiles.contains(ent.getName()))
								modFiles.remove(ent.getName());
							zipO.closeEntry();
							if (ents.hasMoreElements())
								ent = ents.nextElement();
							else
								ent = null;
							ProgressWindow.WindowLogger.increaseProgress();
						}

						// Add other files
						ProgressWindow.WindowLogger.log("Adding remaining files...");
						for (String file : new ArrayList<String>(modFiles)) {
							if (existingEntries.contains(file) || file.endsWith("/")) {
								ProgressWindow.WindowLogger.increaseProgress();
								continue;// Skip
							}
							existingEntries.add(file);
							ent = new ZipEntry(file);
							ProgressWindow.WindowLogger.log("  Updating " + ent.getName());
							zipO.putNextEntry(ent);
							if (!file.endsWith("/")) {
								// Swap streams
								InputStream entStrm = new FileInputStream("lightray-work/mods/" + ent.getName());
								modFiles.remove(ent.getName());
								entStrm.transferTo(zipO);
								entStrm.close();
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
								Runtime.getRuntime()
										.exec(new String[] { "chmod", "755", "buildtools/build-tools/zipalign" })
										.waitFor();
							} catch (Exception e) {
							}
						}
						ProcessBuilder builder = new ProcessBuilder(zipalignFile.getCanonicalPath(), "-fv", "4",
								"base.modified.apk", "base.modified.aligned.apk");
						builder.directory(new File("lightray-work/apks"));
						builder.redirectInput(Redirect.PIPE);
						builder.redirectOutput(Redirect.PIPE);
						builder.redirectError(Redirect.PIPE);
						Process proc = builder.start();
						ProgressWindow.WindowLogger.log(new String(proc.getInputStream().readAllBytes(), "UTF-8").trim()
								.replace("\r", "").replace("\n", "\n    [ZIPALIGN] "));
						ProgressWindow.WindowLogger.log(new String(proc.getErrorStream().readAllBytes(), "UTF-8").trim()
								.replace("\r", "").replace("\n", "\n    [ZIPALIGN] "));
						proc.waitFor();
						if (proc.exitValue() != 0)
							throw new Exception("Non-zero exit code for ZIPALIGN!\n\n"
									+ "This is most commonly caused by a incompatible java environment.\n\n"
									+ "Try updating your Java installation or try another version of it.\n\n"
									+ "Exit code: " + proc.exitValue());

						// Sign apk
						ProgressWindow.WindowLogger.log("");
						ProgressWindow.WindowLogger.log("Signing APK...");
						ProgressWindow.WindowLogger.setLabel("Signing APK...");
						File signApk = new File("buildtools/build-tools/apksigner"
								+ (platformString().equals("windows") ? ".bat" : ""));
						if (!platformString().equals("windows")) {
							try {
								Runtime.getRuntime()
										.exec(new String[] { "chmod", "755", "buildtools/build-tools/apksigner" })
										.waitFor();
							} catch (Exception e) {
							}
						}
						builder = new ProcessBuilder(signApk.getCanonicalPath(), "sign", "--verbose", "--ks",
								"../../keystore.jks", "base.modified.aligned.apk");
						builder.directory(new File("lightray-work/apks"));
						builder.redirectInput(Redirect.PIPE);
						builder.redirectOutput(Redirect.PIPE);
						builder.redirectError(Redirect.PIPE);
						proc = builder.start();
						proc.getOutputStream().write("appmod\n".getBytes());
						proc.getOutputStream().close();
						ProgressWindow.WindowLogger.log(new String(proc.getInputStream().readAllBytes(), "UTF-8").trim()
								.replace("\r", "").replace("\n", "\n    [APKSIGNER] "));
						ProgressWindow.WindowLogger.log(new String(proc.getErrorStream().readAllBytes(), "UTF-8").trim()
								.replace("\r", "").replace("\n", "\n    [APKSIGNER] "));
						proc.waitFor();
						if (proc.exitValue() != 0)
							throw new Exception("Non-zero exit code for APKSIGNER!\n\n"
									+ "This is most commonly caused by a incompatible java environment.\n\n"
									+ "Try updating your Java installation or try another version of it.\n\n"
									+ "Exit code: " + proc.exitValue());
						ProgressWindow.WindowLogger.log("Moving apk...");
						new File("lightray-work/apks/base.modified.aligned.apk")
								.renameTo(new File("lightray-work/apks/base.modified.signed.apk"));

						// Copy final result
						ProgressWindow.WindowLogger.log("Copying final APK...");
						File baseAPK = new File(textField.getText());
						File out = new File(baseAPK.getParentFile(),
								baseAPK.getName().substring(0, baseAPK.getName().lastIndexOf(".")) + " (patched).apk");
						Files.copy(Path.of("lightray-work/apks/base.modified.signed.apk"), out.toPath(),
								StandardCopyOption.REPLACE_EXISTING);

						// Complete
						ProgressWindow.WindowLogger.log("");
						ProgressWindow.WindowLogger.log("Completed! Modifications applied successfully!");
						ProgressWindow.WindowLogger.setLabel("Modifications applied successfully!");
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
						PatchEntry entry = list.getSelectedValue();
						entry.enabled = !entry.enabled;
						entry.box.setEnabled(entry.enabled);
						list.repaint();
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
						PatchEntry entry = patches.values().toArray(new PatchEntry[0])[index];
						entry.enabled = !entry.enabled;
						entry.box.setEnabled(entry.enabled);
						list.repaint();
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
					entry.box.setForeground(SystemColor.activeCaption);
				else
					entry.box.setForeground(list.getForeground());
				entry.box.setSelected(entry.enabled);
				entry.box.setEnabled(cellHasFocus);
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
		scan(mods, "");

		// Load active modifications
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

	private void loadLibs(File libs, FluidClassPool pool) {
		// Scan folder
		for (File m : libs.listFiles()) {
			if (m.isDirectory())
				loadLibs(m, pool);
			else if (m.getName().endsWith(".jar") || m.getName().endsWith(".zip")) {
				pool.addSource(m);
				ProgressWindow.WindowLogger.log("  Added: " + m.getName());
			}
		}
	}

	private void scan(File mods, String pref) {
		// Scan folder
		for (File m : mods.listFiles()) {
			if (m.isDirectory())
				scan(m, pref + m.getName() + "/");
			else if (m.getName().endsWith(".jar") || m.getName().endsWith(".zip"))
				loadMod(m, pref);
		}
	}

	private void patchClasses(File dir, String pref) throws IOException {
		// Scan folder
		for (File m : dir.listFiles()) {
			if (m.isDirectory())
				patchClasses(m, pref + m.getName() + "/");
			else if (m.getName().endsWith(".class"))
				patchClass(m, pref);
		}
	}

	private void patchClass(File classFile, String pref) throws IOException {
		String className = pref + classFile.getName().substring(0, classFile.getName().lastIndexOf(".class"));

		// Apply patch
		ProgressWindow.WindowLogger.log("      Patching: " + className);
		byte[] bytecode = Files.readAllBytes(classFile.toPath());
		byte[] modified = Transformers.applyTransformers(className, bytecode, dynLoader);
		if (modified != null) {
			// Write
			Files.write(classFile.toPath(), modified);
		}
	}

	private void zipAll(File dir, String pref, ZipOutputStream zOut) throws IOException {
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

	private void loadMod(File mod, String pref) {
		// Load mod
		PatchEntry patch = new PatchEntry();
		patch.name = pref + mod.getName();
		patch.type = PatchEntryType.RESOURCE;

		// Load into memory
		FluidClassPool pool = FluidClassPool.createEmpty();
		try {
			ZipInputStream strm = new ZipInputStream(new FileInputStream(mod));
			pool.importArchive(strm);
			strm.close();

			// Determine type
			if (mod.getName().endsWith(".jar")) {
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
								JOptionPane.showMessageDialog(frmLightray,
										"An error occured loading modification: " + patch.name + "\n" + "\n"
												+ "Error: the patcher '" + cls.getTypeName()
												+ "' does not inherit the ILightrayPatcher interface.",
										"Error", JOptionPane.ERROR_MESSAGE);
								return;
							}

							// Create instance
							patchers.get(patch.name).add((ILightrayPatcher) cls.getConstructor().newInstance());
						}
					}
				}
			}

			// Close pool
			pool.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frmLightray,
					"An error occured loading modification: " + patch.name + "\n" + "\n" + "Exception: " + e, "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		patch.box = new JCheckBox(mod.getName() + " (" + patch.type.toString().toLowerCase() + ")");

		// Add
		patches.put(patch.name, patch);
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

	private String platformString() {
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

	private void deleteDir(File dir) {
		for (File d : dir.listFiles(t -> t.isDirectory()))
			deleteDir(d);
		for (File d : dir.listFiles(t -> !t.isDirectory()))
			d.delete();
		dir.delete();
	}

	private void extractFile(String source, String output) throws IOException {
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

	private void downloadFile(String output, String url) throws IOException {
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

	private void download(URLConnection urlConnection, OutputStream output) throws IOException {
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
}
