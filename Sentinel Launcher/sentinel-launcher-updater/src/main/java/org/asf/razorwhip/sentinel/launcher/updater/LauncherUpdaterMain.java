package org.asf.razorwhip.sentinel.launcher.updater;

import java.awt.EventQueue;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JProgressBar;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileSystemView;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.SwingConstants;

public class LauncherUpdaterMain {

	private static boolean installerMode;

	private static JFrame frmLauncher;
	private static JLabel lblNewLabel;

	/**
	 * Launch the application.
	 * 
	 * @throws IOException
	 * @throws JsonSyntaxException
	 */
	public static void main(String[] args) throws JsonSyntaxException, IOException {
		if (new File("installerdata").exists()) {
			installerMode = true;

			// Check OS
			if (System.getProperty("os.name").toLowerCase().contains("darwin")
					|| System.getProperty("os.name").toLowerCase().contains("mac")) {
				// Check package
				if (!new File("installerdata", "Contents/MacOS").exists()) {
					// Error
					JOptionPane.showMessageDialog(null,
							"This installer is not compatible with MacOS, please download the macos-specific installer.",
							"Installer Error", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
					return;
				}
			}

			// Handle arguments
			boolean launch = false;
			int operation = -1;
			String installPath = null;
			for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("--")) {
					String opt = args[i].substring(2);
					String val = null;
					if (opt.contains("=")) {
						val = opt.substring(opt.indexOf("=") + 1);
						opt = opt.substring(0, opt.indexOf("="));
					}

					// Handle argument
					switch (opt) {

					case "help": {
						System.out.println("Arguments:");
						System.out.println(" --install                     -  selects the install operation");
						System.out.println(" --uninstall                   -  selects the uninstall operation");
						System.out.println(
								" --launch-on-complete          -  enables launching of the installed launcher when installation completes");
						System.out.println(" --installation-path \"<path>\"  -  defines the installation path");
						System.exit(0);
					}

					case "install":
						operation = 0;
						break;
					case "uninstall":
						operation = 1;
						break;
					case "launch-on-complete":
						launch = true;
						break;

					case "installation-path": {
						// Retrieve argument if needed
						if (val == null) {
							if (i + 1 < args.length)
								val = args[i + 1];
							else
								break;
							i++;
						}

						// Set path
						installPath = val;
						if (!new File(installPath).exists()) {
							System.err.println("Error: installation folder does not exist");
							System.exit(1);
						}
						break;
					}

					}
				}
			}

			// Perform installer operation if needed
			if (operation != -1) {
				// Load info
				String launcherURL = null;
				String launcherVersion = null;
				String launcherDir;
				String projName;
				File instDir;

				// Read launcher info
				String dirName;
				String url;
				try {
					JsonObject conf = JsonParser.parseString(Files.readString(Path.of("launcher.json")))
							.getAsJsonObject();
					projName = conf.get("projectName").getAsString();
					dirName = conf.get("launcherDirName").getAsString();
					url = conf.get("launcherUpdateListUrl").getAsString();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null,
							"Invalid " + (installerMode ? "installer" : "launcher") + " configuration.",
							(installerMode ? "Installer" : "Launcher") + " Error", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
					return;
				}

				// Download data
				try {
					InputStream strm = new URL(url).openStream();
					String data = new String(strm.readAllBytes(), "UTF-8");
					strm.close();
					JsonObject info = JsonParser.parseString(data).getAsJsonObject();
					launcherVersion = info.get("latest").getAsString();
					launcherURL = info.get("versions").getAsJsonObject().get(launcherVersion).getAsJsonObject()
							.get("url").getAsString();
				} catch (IOException e) {
					// Offline
				}
				launcherDir = dirName;

				// Build folder path
				boolean inHome = false;
				if (System.getenv("LOCALAPPDATA") == null) {
					instDir = new File(System.getProperty("user.home") + "/.local/share");
					if (!instDir.exists()) {
						inHome = true;
						instDir = new File(System.getProperty("user.home"));
					}
				} else {
					instDir = new File(System.getenv("LOCALAPPDATA"));
				}
				if (new File("installation.json").exists())
					instDir = new File(JsonParser.parseString(Files.readString(Path.of("installation.json")))
							.getAsJsonObject().get("installationDirectory").getAsString());
				else {
					// Check appdata
					if (System.getenv("LOCALAPPDATA") == null) {
						// Check OSX
						if (System.getProperty("os.name").toLowerCase().contains("darwin")
								|| System.getProperty("os.name").toLowerCase().contains("mac") || !inHome) {
							instDir = new File(instDir, launcherDir);
						} else {
							instDir = new File(instDir, "." + launcherDir);
						}
					} else
						instDir = new File(instDir, launcherDir);
				}
				if (!instDir.exists())
					instDir.mkdirs();

				// Check redirect
				File installDirFile = new File(instDir, "installation.json");
				if (new File(instDir, "installation.json").exists())
					instDir = new File(
							JsonParser.parseString(Files.readString(new File(instDir, "installation.json").toPath()))
									.getAsJsonObject().get("installationDirectory").getAsString());

				// Perform operation
				if (operation == 0) {
					// Install
					if (installPath != null)
						instDir = new File(installPath, launcherDir);
					performInstallLauncher(instDir, null, launcherVersion, projName, launcherURL, launch,
							installDirFile);
				} else if (operation == 1) {
					// Uninstall
					uninstallLauncher(instDir, null, launcherVersion, projName, launcherURL, launcherDir);
				}

				// Exit
				System.exit(0);
			}
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new LauncherUpdaterMain();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LauncherUpdaterMain() {
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

		frmLauncher = new JFrame();
		frmLauncher.setResizable(false);
		frmLauncher.setBounds(100, 100, 419, 82);
		frmLauncher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmLauncher.setLocationRelativeTo(null);
		try {
			InputStream strmi = getClass().getClassLoader().getResourceAsStream("icon.png");
			frmLauncher.setIconImage(ImageIO.read(strmi));
			strmi.close();
		} catch (Exception e1) {
		}
		try {
			frmLauncher.setIconImage(ImageIO.read(new File("icon.png")));
		} catch (Exception e1) {
		}

		JPanel panel = new JPanel();
		frmLauncher.getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(146, 15));
		panel.add(progressBar, BorderLayout.SOUTH);

		lblNewLabel = new JLabel("New label");
		frmLauncher.getContentPane().add(lblNewLabel, BorderLayout.CENTER);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setPreferredSize(new Dimension(46, 20));

		// Load info
		String launcherURL = null;
		String launcherVersion = null;
		String launcherDir;
		String projName;
		File instDir;

		// Read launcher info
		String dirName;
		String url;
		try {
			JsonObject conf = JsonParser.parseString(Files.readString(Path.of("launcher.json"))).getAsJsonObject();
			projName = conf.get("projectName").getAsString();
			dirName = conf.get("launcherDirName").getAsString();
			url = conf.get("launcherUpdateListUrl").getAsString();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null,
					"Invalid " + (installerMode ? "installer" : "launcher") + " configuration.",
					(installerMode ? "Installer" : "Launcher") + " Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		// Download data
		try {
			InputStream strm = new URL(url).openStream();
			String data = new String(strm.readAllBytes(), "UTF-8");
			strm.close();
			JsonObject info = JsonParser.parseString(data).getAsJsonObject();
			launcherVersion = info.get("latest").getAsString();
			launcherURL = info.get("versions").getAsJsonObject().get(launcherVersion).getAsJsonObject().get("url")
					.getAsString();
		} catch (IOException e) {
			// Offline
		}
		launcherDir = dirName;

		// Set title
		if (!installerMode)
			frmLauncher.setTitle(projName + " Launcher");
		else
			frmLauncher.setTitle(projName + " Installer");

		try {
			// Build folder path
			boolean inHome = false;
			if (System.getenv("LOCALAPPDATA") == null) {
				instDir = new File(System.getProperty("user.home") + "/.local/share");
				if (!instDir.exists()) {
					inHome = true;
					instDir = new File(System.getProperty("user.home"));
				}
			} else {
				instDir = new File(System.getenv("LOCALAPPDATA"));
			}
			if (new File("installation.json").exists())
				instDir = new File(JsonParser.parseString(Files.readString(Path.of("installation.json")))
						.getAsJsonObject().get("installationDirectory").getAsString());
			else {
				// Check appdata
				if (System.getenv("LOCALAPPDATA") == null) {
					// Check OSX
					if (System.getProperty("os.name").toLowerCase().contains("darwin")
							|| System.getProperty("os.name").toLowerCase().contains("mac") || !inHome) {
						instDir = new File(instDir, launcherDir);
					} else {
						instDir = new File(instDir, "." + launcherDir);
					}
				} else
					instDir = new File(instDir, launcherDir);
			}
			if (!instDir.exists())
				instDir.mkdirs();

			// Check redirect
			if (new File(instDir, "installation.json").exists())
				instDir = new File(
						JsonParser.parseString(Files.readString(new File(instDir, "installation.json").toPath()))
								.getAsJsonObject().get("installationDirectory").getAsString());

			// Start launcher
			if (!installerMode)
				startLauncher(instDir, progressBar, launcherVersion, projName, launcherURL);
			else {
				File dir = instDir;

				// Installer mode, ask which operation to perform
				String launcherVersionF = launcherVersion;
				String launcherURLF = launcherURL;
				Thread th = new Thread(() -> {
					log("Waiting for user to select installer operation...");
					frmLauncher.setVisible(true);
					SwingUtilities.invokeLater(() -> {
						progressBar.setMaximum(100);
						progressBar.setValue(0);

						// Show message
						int selected;
						File verFile = new File(dir, "currentversion.info");
						if (verFile.exists()) {
							selected = JOptionPane.showOptionDialog(null,
									"Welcome to the " + projName
											+ " installer!\n\nPlease select installer operation...\n ",
									projName + " Installer", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
									null, new Object[] { "Install or update", "Uninstall", "Cancel" }, "Cancel");

							// Quit if cancelled
							if (selected == 2 || selected == -1)
								System.exit(0);
						} else {
							selected = JOptionPane.showOptionDialog(null,
									"Welcome to the " + projName
											+ " installer!\n\nPlease select installer operation...\n ",
									projName + " Installer", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
									null, new Object[] { "Install the launcher", "Cancel" }, "Cancel");

							// Quit if cancelled
							if (selected == 1 || selected == -1)
								System.exit(0);
						}

						// Run installer
						Thread th2 = new Thread(() -> {
							try {
								// Log
								log("Processing selected operation...");
								SwingUtilities.invokeAndWait(() -> {
									progressBar.setMaximum(100);
									progressBar.setValue(0);
								});

								// Run operation
								if (selected == 0)
									installLauncher(dir, progressBar, launcherVersionF, projName, launcherURLF,
											launcherDir);
								else {
									SwingUtilities.invokeAndWait(() -> {
										if (JOptionPane.showConfirmDialog(frmLauncher,
												"Are you sure you wish to uninstall the " + projName
														+ " launcher and client?\n\nWARNING: This will delete all player data for local servers!",
												"Uninstall Launcher", JOptionPane.YES_NO_OPTION,
												JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
											System.exit(1);
										}
										if (JOptionPane.showConfirmDialog(frmLauncher,
												"Launcher will be uninstalled and all " + projName
														+ " data will be deleted from your disk.",
												"Uninstall Launcher", JOptionPane.OK_CANCEL_OPTION,
												JOptionPane.INFORMATION_MESSAGE) != JOptionPane.OK_OPTION) {
											System.exit(1);
										}
									});
									uninstallLauncher(dir, progressBar, launcherVersionF, projName, launcherURLF,
											launcherDir);
								}
							} catch (Exception e) {
								try {
									SwingUtilities.invokeAndWait(() -> {
										String stackTrace = "";
										for (StackTraceElement ele : e.getStackTrace())
											stackTrace += "\n     At: " + ele;
										JOptionPane.showMessageDialog(frmLauncher,
												"An error occured while running the installer.\nUnable to continue, the installer will now close.\n\nError details: "
														+ e + stackTrace
														+ "\nPlease report this error to the server operators.",
												"Installer Error", JOptionPane.ERROR_MESSAGE);
										System.exit(1);
									});
								} catch (InvocationTargetException | InterruptedException e1) {
								}
							}
						}, "Installer Thread");
						th2.setDaemon(true);
						th2.start();
					});
				}, "Installer Thread");
				th.setDaemon(true);
				th.start();
			}
		} catch (Exception e) {
			String stackTrace = "";
			for (StackTraceElement ele : e.getStackTrace())
				stackTrace += "\n     At: " + ele;
			JOptionPane.showMessageDialog(frmLauncher,
					"An error occured while running the " + (installerMode ? "installer" : "launcher")
							+ ".\nUnable to continue, the " + (installerMode ? "installer" : "launcher")
							+ " will now close.\n\nError details: " + e + stackTrace
							+ "\nPlease report this error to the server operators.",
					(installerMode ? "Installer" : "Launcher") + " Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	private static void installLauncher(File instDir, JProgressBar progressBar, String launcherVersion, String projName,
			String launcherURL, String launcherDir) throws IOException {
		// Build folder path
		boolean inHome = false;
		File instDir2;
		if (System.getenv("LOCALAPPDATA") == null) {
			instDir2 = new File(System.getProperty("user.home") + "/.local/share");
			if (!instDir2.exists()) {
				inHome = true;
				instDir2 = new File(System.getProperty("user.home"));
			}
		} else {
			instDir2 = new File(System.getenv("LOCALAPPDATA"));
		}
		if (new File("installation.json").exists())
			try {
				instDir2 = new File(JsonParser.parseString(Files.readString(Path.of("installation.json")))
						.getAsJsonObject().get("installationDirectory").getAsString());
			} catch (JsonSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		else {
			// Check appdata
			if (System.getenv("LOCALAPPDATA") == null) {
				// Check OSX
				if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac") || !inHome) {
					instDir2 = new File(instDir2, launcherDir);
				} else {
					instDir2 = new File(instDir2, "." + launcherDir);
				}
			} else
				instDir2 = new File(instDir2, launcherDir);
		}
		File installDirFile = new File(instDir2, "installation.json");
		if (!installDirFile.exists() && !System.getProperty("os.name").toLowerCase().contains("darwin")
				&& !System.getProperty("os.name").toLowerCase().contains("mac")) {
			// Select installation path
			int res = JOptionPane.showConfirmDialog(frmLauncher,
					"Do you wish to use the default installation directory?",
					"Installling the " + projName + " launcher", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (res == JOptionPane.CANCEL_OPTION)
				System.exit(1);

			// Prompt path select
			if (res == JOptionPane.NO_OPTION) {
				while (true) {
					// Prompt selection
					JFileChooser chooser = new JFileChooser();
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					chooser.setDialogTitle("Select installation directory...");
					if (chooser.showOpenDialog(frmLauncher) != JFileChooser.APPROVE_OPTION)
						System.exit(1);

					// Check file
					if (!chooser.getSelectedFile().exists() || chooser.getSelectedFile().isFile()) {
						JOptionPane.showMessageDialog(frmLauncher,
								"The folder you selected is not valid or does not exist.", "Invalid folder",
								JOptionPane.ERROR_MESSAGE);
						continue;
					}

					// Set install dir
					instDir = new File(chooser.getSelectedFile(), launcherDir);
					break;
				}
			}
		}

		// Install
		performInstallLauncher(instDir, progressBar, launcherVersion, projName, launcherURL, true, installDirFile);

		// Exit
		System.exit(0);
	}

	private static void performInstallLauncher(File instDir, JProgressBar progressBar, String launcherVersion,
			String projName, String launcherURL, boolean launchOnComplete, File installDirFile) throws IOException {
		log("Preparing to install...");
		frmLauncher.setVisible(true);
		File instSource = new File("installerdata");

		// Detect OS
		int os;
		if (System.getProperty("os.name").toLowerCase().contains("darwin")
				|| System.getProperty("os.name").toLowerCase().contains("mac")) {
			os = 0; // MacOS
		} else if (System.getProperty("os.name").toLowerCase().contains("win")) {
			os = 1; // Windows
		} else {
			os = 2; // Linux
		}

		// Create output
		log("Creating installation directories...");
		File launcherOut;
		if (os != 0)
			// Windows and Linux
			launcherOut = instDir;
		else
			// MacOS
			launcherOut = new File("/Applications/" + projName + ".app");
		launcherOut.mkdirs();

		// Install launcher
		log("Copying base launcher...");
		if (progressBar != null) {
			try {
				int c = countDir(instSource);
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(c);
					progressBar.setValue(0);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}
		copyDir(instSource, instDir, progressBar);
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(100);
				progressBar.setValue(100);
				progressBar.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// MacOS
		if (os == 0) {
			// Install launcher
			log("Installing launcher application...");
			if (progressBar != null) {
				try {
					int c = countDir(instSource);
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setMaximum(c);
						progressBar.setValue(0);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
			copyDir(instSource, launcherOut, progressBar);
			try {
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(100);
					progressBar.setValue(100);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}

		// Copy launcher info
		log("Copying launcher information...");
		File sOut = new File(launcherOut, "launcher.json");
		if (sOut.exists())
			sOut.delete();
		Files.copy(new File("launcher.json").toPath(), sOut.toPath());

		// Copy runtime
		log("Copying java runtime...");
		if (progressBar != null) {
			try {
				int c = countDir(new File(os == 1 ? "win" : (os == 0 ? "osx" : "linux")));
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(c);
					progressBar.setValue(0);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}
		copyDir(new File(os == 1 ? "win" : (os == 0 ? "osx" : "linux")),
				new File(launcherOut, os == 1 ? "win" : (os == 0 ? "osx" : "linux")), progressBar);
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(100);
				progressBar.setValue(100);
				progressBar.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// Set perms
		if (os == 2) {
			// Linux-only
			log("Setting permissions...");
			ProcessBuilder proc = new ProcessBuilder("chmod", "+x",
					new File(launcherOut, "launcher.sh").getCanonicalPath());
			try {
				proc.start().waitFor();
			} catch (InterruptedException e1) {
			}
		}

		// Write initial version
		log("Saving initial version...");
		Files.writeString(new File(instDir, "currentversion.info").toPath(), "none");

		// Write installation directory
		JsonObject infoJson = new JsonObject();
		infoJson.addProperty("installationDirectory", instDir.getAbsolutePath());
		log("Creating installation target specifier file...");
		installDirFile.getParentFile().mkdirs();
		Files.writeString(installDirFile.toPath(), infoJson.toString());

		// Windows and linux only, macos already has the app installed now
		if (os != 0) {
			log("Creating shortcuts...");
			if (os == 1) {
				// Windows

				// Build classpath
				String classPath = "";
				for (File f : new File(launcherOut, "libs").listFiles(t -> t.isFile() && t.getName().endsWith(".jar")))
					if (classPath.isEmpty())
						classPath = "libs/" + f.getName();
					else
						classPath += ";libs/" + f.getName();
				for (File f : launcherOut.listFiles(t -> t.isFile() && t.getName().endsWith(".jar")))
					if (classPath.isEmpty())
						classPath = f.getName();
					else
						classPath += ";" + f.getName();

				// Find desktop
				// Ugly but uh it works?
				File winDesktop = FileSystemView.getFileSystemView().getHomeDirectory();

				// Create shortcuts
				File lnk = new File(winDesktop, projName + ".lnk");
				createWindowsShortcut(lnk, new File(launcherOut, "win/java-17/bin/javaw.exe"),
						"-cp '" + classPath + "' org.asf.razorwhip.sentinel.launcher.updater.LauncherUpdaterMain",
						launcherOut, new File(launcherOut, "icon.ico"));
				lnk = new File(new File(System.getenv("APPDATA"), "Microsoft/Windows/Start Menu/Programs"),
						projName + ".lnk");
				createWindowsShortcut(lnk, new File(launcherOut, "win/java-17/bin/javaw.exe"),
						"-cp '" + classPath + "' org.asf.razorwhip.sentinel.launcher.updater.LauncherUpdaterMain",
						launcherOut, new File(launcherOut, "icon.ico"));
			} else {
				// Linux
				File applicationFile = new File(
						System.getProperty("user.home") + "/.local/share/applications/" + projName + ".desktop");

				// Generate desktop entry
				FileOutputStream sO = new FileOutputStream(applicationFile);
				sO.write("[Desktop Entry]\n".getBytes("UTF-8"));
				sO.write(("Name=" + projName + "\n").getBytes("UTF-8"));
				sO.write(("Categories=Game;\n").getBytes("UTF-8"));
				sO.write(("Comment=Launcher for " + projName + "\n").getBytes("UTF-8"));
				sO.write(("Exec=" + new File(launcherOut, "launcher.sh").getAbsolutePath() + "\n").getBytes("UTF-8"));
				sO.write(("Icon=" + new File(launcherOut, "icon.png").getAbsolutePath() + "\n").getBytes("UTF-8"));
				sO.write(("Path=" + launcherOut.getAbsolutePath() + "\n").getBytes("UTF-8"));
				sO.write(("StartupNotify=true\n").getBytes("UTF-8"));
				sO.write(("Terminal=false\n").getBytes("UTF-8"));
				sO.write(("Type=Application\n").getBytes("UTF-8"));
				sO.write(("X-KDE-RunOnDiscreteGpu=true\n").getBytes("UTF-8"));
				sO.close();
			}
		}

		// Done
		log("Installation completed!");
		if (frmLauncher != null && !launchOnComplete) {
			JOptionPane.showMessageDialog(frmLauncher, "Successfully installed the launcher!", "Installed the launcher",
					JOptionPane.INFORMATION_MESSAGE);
		}

		// Launch if needed
		if (launchOnComplete) {
			if (os != 0) {
				// Start process
				ProcessBuilder builder = new ProcessBuilder(
						(os == 1 ? new File(launcherOut, "launcher.bat").getAbsolutePath()
								: new File(launcherOut, "launcher.sh").getAbsolutePath()));
				builder.directory(launcherOut.getAbsoluteFile());
				builder.inheritIO();
				Process proc = builder.start();
				try {
					SwingUtilities.invokeAndWait(() -> {
						frmLauncher.dispose();
					});
					proc.waitFor();
				} catch (InterruptedException | InvocationTargetException e) {
				}
				System.exit(proc.exitValue());
			} else {
				// Start OSX launcher
				ProcessBuilder builder = new ProcessBuilder("open", "-n", launcherOut.getAbsolutePath());
				builder.inheritIO();
				Process proc = builder.start();
				try {
					SwingUtilities.invokeAndWait(() -> {
						frmLauncher.dispose();
					});
					proc.waitFor();
				} catch (InterruptedException | InvocationTargetException e) {
				}
				System.exit(proc.exitValue());
			}
		}
	}

	private static void createWindowsShortcut(File lnk, File executable, String args, File cwd, File icon)
			throws IOException {
		// Create temp file
		File vbs = File.createTempFile("desktopcreate-", ".vbs");
		vbs.deleteOnExit();

		// Write script
		FileOutputStream sO = new FileOutputStream(vbs);
		sO.write("Set sh = CreateObject(\"WScript.Shell\")\r\n".getBytes("UTF-8"));
		sO.write("Set sc = sh.CreateShortCut(WScript.Arguments(0))\r\n".getBytes("UTF-8"));
		sO.write("sc.IconLocation = WScript.Arguments(1)\r\n".getBytes("UTF-8"));
		sO.write("sc.TargetPath = WScript.Arguments(2)\r\n".getBytes("UTF-8"));
		sO.write("sc.Arguments = Replace(WScript.Arguments(3), \"'\", chr(34))\r\n".getBytes("UTF-8"));
		sO.write("sc.WorkingDirectory = WScript.Arguments(4)\r\n".getBytes("UTF-8"));
		sO.write("sc.Save\r\n".getBytes("UTF-8"));
		sO.close();

		// Start script
		ProcessBuilder proc = new ProcessBuilder("wscript", vbs.getCanonicalPath(), lnk.getAbsolutePath(),
				icon.getAbsolutePath(), executable.getAbsolutePath(), args, cwd.getAbsolutePath());
		try {
			proc.start().waitFor();
		} catch (InterruptedException e1) {
		}
		vbs.delete();
	}

	private static void uninstallLauncher(File instDir, JProgressBar progressBar, String launcherVersion,
			String projName, String launcherURL, String launcherDir) {
		log("Preparing to uninstall...");
		frmLauncher.setVisible(true);

		// Check for installation data
		log("Finding launcher files...");
		File verFile = new File(instDir, "currentversion.info");
		if (!verFile.exists()) {
			// Error
			if (frmLauncher != null) {
				JOptionPane.showMessageDialog(frmLauncher,
						"Unable to uninstall the launcher as it has not been installed yet.", "Installer Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			} else {
				System.err.println("Error: unable to uninstall the launcher as it has not been installed yet.");
				System.exit(1);
			}
		}

		// Uninstall
		log("Uninstalling launcher...");
		if (progressBar != null) {
			try {
				int c = countDir(instDir);
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(c);
					progressBar.setValue(0);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}

		// Delete directory
		deleteDir(instDir, progressBar);
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(100);
				progressBar.setValue(100);
				progressBar.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// Remove shortcuts
		log("Removing application shortcut files...");
		if (System.getProperty("os.name").toLowerCase().contains("darwin")
				|| System.getProperty("os.name").toLowerCase().contains("mac")) {
			// Mac
			log("Removing launcher from applications...");
			File launcherFile = new File("/Applications/" + projName + ".app");
			if (launcherFile.exists()) {
				// Delete the launcher app
				log("Deleting launcher application...");
				if (progressBar != null) {
					try {
						int c = countDir(launcherFile);
						SwingUtilities.invokeAndWait(() -> {
							progressBar.setMaximum(c);
							progressBar.setValue(0);
							progressBar.repaint();
						});
					} catch (InvocationTargetException | InterruptedException e) {
					}
				}

				// Delete directory
				deleteDir(launcherFile, progressBar);
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setMaximum(100);
						progressBar.setValue(100);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		} else if (System.getProperty("os.name").toLowerCase().contains("win")) {
			// Windows
			File winDesktop = FileSystemView.getFileSystemView().getHomeDirectory();
			File lnk = new File(winDesktop, projName + ".lnk");
			if (lnk.exists())
				lnk.delete();
			lnk = new File(new File(System.getenv("APPDATA"), "Microsoft/Windows/Start Menu/Programs"),
					projName + ".lnk");
			if (lnk.exists())
				lnk.delete();
		} else {
			// Linux
			File applicationFile = new File(
					System.getProperty("user.home") + "/.local/share/applications/" + projName + ".desktop");
			if (applicationFile.exists())
				applicationFile.delete();
		}

		// Remove path file
		log("Removing installation target specifier file...");

		// Build folder path
		boolean inHome = false;
		if (System.getenv("LOCALAPPDATA") == null) {
			instDir = new File(System.getProperty("user.home") + "/.local/share");
			if (!instDir.exists()) {
				inHome = true;
				instDir = new File(System.getProperty("user.home"));
			}
		} else {
			instDir = new File(System.getenv("LOCALAPPDATA"));
		}
		if (new File("installation.json").exists())
			try {
				instDir = new File(JsonParser.parseString(Files.readString(Path.of("installation.json")))
						.getAsJsonObject().get("installationDirectory").getAsString());
			} catch (JsonSyntaxException | IOException e) {
				throw new RuntimeException(e);
			}
		else {
			// Check appdata
			if (System.getenv("LOCALAPPDATA") == null) {
				// Check OSX
				if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac") || !inHome) {
					instDir = new File(instDir, launcherDir);
				} else {
					instDir = new File(instDir, "." + launcherDir);
				}
			} else
				instDir = new File(instDir, launcherDir);
		}
		File installDirFile = new File(instDir, "installation.json");
		if (installDirFile.exists())
			installDirFile.delete();

		// Done
		log("Uninstallation completed!");
		if (frmLauncher != null) {
			JOptionPane.showMessageDialog(frmLauncher, "Successfully uninstalled the launcher!",
					"Uninstalled the launcher", JOptionPane.INFORMATION_MESSAGE);
		}
		System.exit(0);
	}

	private static void copyDir(File dir, File output, JProgressBar progressBar) throws IOException {
		if (!dir.exists())
			throw new FileNotFoundException(dir.getPath());

		output.mkdirs();
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			copyDir(subDir, new File(output, subDir.getName()), progressBar);
			if (progressBar != null) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setValue(progressBar.getValue() + 1);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			if (new File(output, file.getName()).exists())
				new File(output, file.getName()).delete();
			Files.copy(file.toPath(), new File(output, file.getName()).toPath());
			if (progressBar != null) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setValue(progressBar.getValue() + 1);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		}
		if (progressBar != null) {
			try {
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setValue(progressBar.getValue() + 1);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}
	}

	private static void deleteDir(File dir, JProgressBar progressBar) {
		if (Files.isSymbolicLink(dir.toPath())) {
			// Skip symlink
			dir.delete();
			return;
		}
		if (!dir.exists() || dir.listFiles() == null) {
			if (progressBar != null) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setValue(progressBar.getValue() + 1);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
			dir.delete();
			return;
		}
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir, progressBar);
			if (progressBar != null) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setValue(progressBar.getValue() + 1);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
			if (progressBar != null) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setValue(progressBar.getValue() + 1);
						progressBar.repaint();
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}
			}
		}
		dir.delete();
		if (progressBar != null) {
			try {
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setValue(progressBar.getValue() + 1);
					progressBar.repaint();
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}
	}

	private static int countDir(File dir) {
		if (!dir.exists() || dir.listFiles() == null) {
			return 1;
		}

		int i = 0;
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			i += countDir(subDir);
		}
		var listFiles = dir.listFiles(t -> !t.isDirectory());
		for (int j = 0; j < listFiles.length; j++) {
			i++;
		}
		i++;
		return i;
	}

	private void startLauncher(File instDir, JProgressBar progressBar, String launcherVersion, String projName,
			String launcherURL) {
		File dir = instDir;
		Thread th = new Thread(() -> {
			// Set progress bar status
			try {
				log("Checking launcher files...");
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(100);
					progressBar.setValue(0);
				});

				// Check version file
				File verFile = new File(dir, "currentversion.info");
				String currentVersion = "";
				boolean isNew = !verFile.exists();
				if (!isNew)
					currentVersion = Files.readString(verFile.toPath());

				// Check updates
				log("Checking for updates...");
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(100);
					progressBar.setValue(0);
				});
				if (!new File(new File(dir, "launcher"), "startup.json").exists()) {
					currentVersion = "none";
					if (launcherVersion == null) {
						JOptionPane.showMessageDialog(null,
								"There is a update required, please connect to the internet.", "Launcher Error",
								JOptionPane.ERROR_MESSAGE);
						System.exit(1);
						return;
					}
				}
				if (launcherVersion != null && !currentVersion.equals(launcherVersion)) {
					if (isNew) {
						// Prompt
						SwingUtilities.invokeAndWait(() -> {
							if (JOptionPane.showConfirmDialog(frmLauncher,
									"Do you wish to install the " + projName + " launcher and client?",
									"Install Launcher", JOptionPane.YES_NO_OPTION,
									JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION) {
								System.exit(1);
							}
						});
					}

					// Update label
					log("Updating launcher...");
					frmLauncher.setVisible(true);

					// Download zip
					File tmpOut = new File(dir, "launcher.zip");
					downloadFile(launcherURL, tmpOut, progressBar);

					// Extract zip
					try {
						log("Extracting launcher update...");
						SwingUtilities.invokeAndWait(() -> {
							progressBar.setMaximum(100);
							progressBar.setValue(0);
						});
					} catch (InvocationTargetException | InterruptedException e) {
					}
					unZip(tmpOut, new File(dir, "launcher"), progressBar);
				}

				// Prepare to start launcher
				try {
					log("Starting...");
					SwingUtilities.invokeAndWait(() -> {
						progressBar.setMaximum(100);
						progressBar.setValue(0);
					});
				} catch (InvocationTargetException | InterruptedException e) {
				}

				// Start launcher, compute arguments
				JsonObject startupInfo = JsonParser
						.parseString(Files.readString(new File(new File(dir, "launcher"), "startup.json").toPath()))
						.getAsJsonObject();
				ArrayList<String> cmd = new ArrayList<String>();
				cmd.add(startupInfo.get("executable").getAsString()
						.replace("$<dir>", new File(dir, "launcher").getAbsolutePath())
						.replace("$<jvm>", new File(ProcessHandle.current().info().command().get()).getAbsolutePath()));
				for (JsonElement ele : startupInfo.get("arguments").getAsJsonArray())
					cmd.add(ele.getAsString().replace("$<dir>", new File(dir, "launcher").getAbsolutePath())
							.replace("$<jvm>", ProcessHandle.current().info().command().get())
							.replace("$<pathsep>", File.pathSeparator).replace("$<project>", projName));

				// Detect OS
				int os;
				if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac"))
					os = 0; // MacOS
				else if (System.getProperty("os.name").toLowerCase().contains("win"))
					os = 1; // Windows
				else
					os = 2; // Linux

				// Start process
				ProcessBuilder builder = new ProcessBuilder(cmd.toArray(t -> new String[t]));
				builder.directory(new File(dir, "launcher"));
				builder.environment().put("SENTINEL_LAUNCHER_PATH",
						(os == 0 || os == 2 ? new File("launcher.sh").getAbsolutePath()
								: new File("launcher.bat").getAbsolutePath()));
				builder.inheritIO();
				Process proc = builder.start();

				// Mark done
				if (launcherVersion != null && !currentVersion.equals(launcherVersion))
					Files.writeString(verFile.toPath(), launcherVersion);
				SwingUtilities.invokeAndWait(() -> {
					frmLauncher.setVisible(false);
				});
				int exitCode = proc.waitFor();
				if (exitCode == 237) {
					// Load info
					String projName2 = projName;

					// Read launcher info
					String url;
					try {
						JsonObject conf = JsonParser.parseString(Files.readString(Path.of("launcher.json")))
								.getAsJsonObject();
						projName2 = conf.get("projectName").getAsString();
						url = conf.get("launcherUpdateListUrl").getAsString();
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null,
								"Invalid " + (installerMode ? "installer" : "launcher") + " configuration.",
								(installerMode ? "Installer" : "Launcher") + " Error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
						return;
					}

					// Download data
					String launcherVersion2 = launcherVersion;
					String launcherURL2 = launcherURL;
					try {
						InputStream strm = new URL(url).openStream();
						String data = new String(strm.readAllBytes(), "UTF-8");
						strm.close();
						JsonObject info = JsonParser.parseString(data).getAsJsonObject();
						launcherVersion2 = info.get("latest").getAsString();
						launcherURL2 = info.get("versions").getAsJsonObject().get(launcherVersion).getAsJsonObject()
								.get("url").getAsString();
					} catch (IOException e) {
						// Offline
					}
					startLauncher(instDir, progressBar, launcherVersion2, projName2, launcherURL2);
					return;
				}
				System.exit(exitCode);
			} catch (Exception e) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						String stackTrace = "";
						for (StackTraceElement ele : e.getStackTrace())
							stackTrace += "\n     At: " + ele;
						JOptionPane.showMessageDialog(frmLauncher,
								"An error occured while running the launcher.\nUnable to continue, the launcher will now close.\n\nError details: "
										+ e + stackTrace + "\nPlease report this error to the server operators.",
								"Launcher Error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					});
				} catch (InvocationTargetException | InterruptedException e1) {
				}
			}
		}, "Launcher Thread");
		th.start();
	}

	private void downloadFile(String url, File outp, JProgressBar progressBar)
			throws MalformedURLException, IOException {
		URLConnection urlConnection = new URL(url).openConnection();
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(urlConnection.getContentLength() / 1000);
				progressBar.setValue(0);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
		InputStream data = urlConnection.getInputStream();
		FileOutputStream out = new FileOutputStream(outp);
		while (true) {
			byte[] b = data.readNBytes(1000);
			if (b.length == 0)
				break;
			else {
				out.write(b);
				SwingUtilities.invokeLater(() -> {
					progressBar.setValue(progressBar.getValue() + 1);
				});
			}
		}
		out.close();
		data.close();
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(progressBar.getMaximum());
		});
	}

	private static void log(String message) {
		if (lblNewLabel != null) {
			try {
				SwingUtilities.invokeAndWait(() -> {
					lblNewLabel.setText(" " + message);
				});
			} catch (InvocationTargetException | InterruptedException e) {
			}
		}
		System.out.println("[LAUNCHER] [UPDATER] " + message);
	}

	private void unZip(File input, File output, JProgressBar bar) throws IOException {
		output.mkdirs();

		// count entries
		ZipFile archive = new ZipFile(input);
		int count = 0;
		Enumeration<? extends ZipEntry> en = archive.entries();
		while (en.hasMoreElements()) {
			en.nextElement();
			count++;
		}
		archive.close();

		// prepare and log
		archive = new ZipFile(input);
		en = archive.entries();
		try {
			int fcount = count;
			SwingUtilities.invokeAndWait(() -> {
				bar.setMaximum(fcount);
				bar.setValue(0);
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (en.hasMoreElements()) {
			ZipEntry ent = en.nextElement();
			if (ent == null)
				break;

			if (ent.isDirectory()) {
				new File(output, ent.getName()).mkdirs();
			} else {
				File out = new File(output, ent.getName());
				if (out.getParentFile() != null && !out.getParentFile().exists())
					out.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(out);
				InputStream is = archive.getInputStream(ent);
				is.transferTo(os);
				is.close();
				os.close();
			}

			SwingUtilities.invokeLater(() -> {
				bar.setValue(bar.getValue() + 1);
			});
		}

		// finish progress
		SwingUtilities.invokeLater(() -> {
			bar.setValue(bar.getValue() + 1);
		});
		archive.close();
	}
}
