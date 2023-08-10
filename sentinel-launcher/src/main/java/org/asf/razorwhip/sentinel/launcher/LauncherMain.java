package org.asf.razorwhip.sentinel.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.FlowLayout;

public class LauncherMain {

	public static final String LAUNCHER_VERSION = "1.0.0.A1";

	private JFrame frmSentinelLauncher;
	private JLabel lblStatusLabel;
	private static String[] args;
	private boolean shiftDown;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		LauncherMain.args = args;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LauncherMain window = new LauncherMain();
					window.frmSentinelLauncher.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LauncherMain() {
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

		frmSentinelLauncher = new JFrame();
		frmSentinelLauncher.addKeyListener(new KeyAdapter() {
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
		frmSentinelLauncher.setResizable(false);
		frmSentinelLauncher.setBounds(100, 100, 678, 262);
		frmSentinelLauncher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSentinelLauncher.setLocationRelativeTo(null);

		BackgroundPanel panel_1 = new BackgroundPanel();
		panel_1.setForeground(Color.WHITE);
		frmSentinelLauncher.getContentPane().add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		LauncherUtils.panel = panel_1;

		JPanel panel = new JPanel();
		panel.setBackground(new Color(255, 255, 255, 0));
		panel_1.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		JPanel panel_4 = new JPanel();
		panel_4.setPreferredSize(new Dimension(10, 30));
		panel.add(panel_4, BorderLayout.SOUTH);
		panel_4.setBackground(new Color(10, 10, 10, 100));
		panel_4.setLayout(new BorderLayout(0, 0));

		lblStatusLabel = new JLabel("New label");
		panel_4.add(lblStatusLabel, BorderLayout.CENTER);
		lblStatusLabel.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblStatusLabel.setForeground(Color.WHITE);
		lblStatusLabel.setPreferredSize(new Dimension(46, 20));
		LauncherUtils.statusLabel = lblStatusLabel;

		JPanel panel_5 = new JPanel();
		panel_5.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_5.setPreferredSize(new Dimension(200, 10));
		panel_5.setBackground(new Color(255, 255, 255, 0));
		panel_4.add(panel_5, BorderLayout.EAST);
		panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.X_AXIS));
		LauncherUtils.progressPanel = panel_5;

		JProgressBar progressBar = new JProgressBar();
		panel_5.add(progressBar);
		progressBar.setPreferredSize(new Dimension(500, 14));
		progressBar.setBackground(new Color(240, 240, 240, 100));
		LauncherUtils.progressBar = progressBar;

		JPanel panelLabels = new JPanel();
		panelLabels.setBackground(Color.LIGHT_GRAY);
		panel.add(panelLabels, BorderLayout.CENTER);
		panelLabels.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel_2 = new JPanel();
		panel_2.setBackground(Color.LIGHT_GRAY);
		panel_2.setPreferredSize(new Dimension(600, 180));
		panelLabels.add(panel_2);
		panel_2.setLayout(null);

		JLabel lblNewLabel = new JLabel("<Project Name>");
		lblNewLabel.setBounds(12, 85, 576, 33);
		panel_2.add(lblNewLabel);
		lblNewLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
		panel_5.setVisible(false);

		// Contact server
		String softwareSourceClass;
		String descriptorSourceClass;
		File gameDescriptorFile = new File("gamedescriptor.sgd");
		File emulationSoftwareFile = new File("emulationsoftware.svp");
		boolean dirModeDescriptorFile = false;
		boolean dirModeSoftwareFile = false;
		String urlBaseDescriptorFile;
		String urlBaseSoftwareFile;
		Map<String, String> gameDescriptor;
		Map<String, String> softwareDescriptor;
		try {
			// Check debug
			LauncherUtils.log("Loading debug settings...");
			if (System.getProperty("debugGameDescriptorHintClass") != null) {
				// Find class
				LauncherUtils.log("Loading debug descriptor...");
				Class<?> cls = Class.forName(System.getProperty("debugGameDescriptorHintClass"));
				URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
				File f = new File(loc.toURI());

				// Check file
				if (f.isDirectory()) {
					dirModeDescriptorFile = true;
				}
				gameDescriptorFile = f;
				LauncherUtils.log("Using descriptor: " + f);
			}
			if (System.getProperty("debugEmulationSoftwareHintClass") != null) {
				// Find class
				LauncherUtils.log("Loading debug emulation software...");
				Class<?> cls = Class.forName(System.getProperty("debugEmulationSoftwareHintClass"));
				URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
				File f = new File(loc.toURI());

				// Check file
				if (f.isDirectory()) {
					dirModeSoftwareFile = true;
				}
				emulationSoftwareFile = f;
				LauncherUtils.log("Using emulation software: " + f);
			}

			// Check files
			LauncherUtils.log("Checking if the setup needs to be launched...");
			if (!gameDescriptorFile.exists() || !emulationSoftwareFile.exists()
					|| (System.getProperty("debugShowSetup") != null
							&& !System.getProperty("debugShowSetup").equals("false"))) {
				// Open setup wizard
				LauncherUtils.log("Opening setup...");
				// TODO
				gameDescriptorFile = gameDescriptorFile;
				throw new Exception("Setup is not implemented");
			}

			// Prepare
			LauncherUtils.log("Preparing data source URLs...");
			if (!dirModeDescriptorFile)
				urlBaseDescriptorFile = "jar:" + gameDescriptorFile.toURI() + "!";
			else
				urlBaseDescriptorFile = gameDescriptorFile.toURI().toString();
			if (!urlBaseDescriptorFile.endsWith("/"))
				urlBaseDescriptorFile += "/";
			if (!dirModeSoftwareFile)
				urlBaseSoftwareFile = "jar:" + emulationSoftwareFile.toURI() + "!";
			else
				urlBaseSoftwareFile = emulationSoftwareFile.toURI().toString();
			if (!urlBaseSoftwareFile.endsWith("/"))
				urlBaseSoftwareFile += "/";
			LauncherUtils.addUrlToComponentClassLoader(new URL(urlBaseDescriptorFile));
			LauncherUtils.addUrlToComponentClassLoader(new URL(urlBaseSoftwareFile));

			// Read descriptor info
			LauncherUtils.log("Loading game descriptor information...");
			gameDescriptor = LauncherUtils.parseProperties(downloadString(urlBaseSoftwareFile + "descriptorinfo"));
			descriptorSourceClass = gameDescriptor.get("Game-Descriptor-Class");
			LauncherUtils.gameID = gameDescriptor.get("Game-ID");
			if (descriptorSourceClass == null)
				throw new IOException("No descriptor class defined in game descriptor");
			if (LauncherUtils.gameID == null)
				throw new IOException("No game ID defined in game descriptor");

			// Read software info
			LauncherUtils.log("Loading emulation software information...");
			softwareDescriptor = LauncherUtils.parseProperties(downloadString(urlBaseSoftwareFile + "softwareinfo"));
			if (!softwareDescriptor.containsKey("Game-ID"))
				throw new IOException("No game ID defined in emulation software descriptor");
			softwareSourceClass = softwareDescriptor.get("Software-Class");
			LauncherUtils.softwareName = softwareDescriptor.get("Project-Name");
			LauncherUtils.softwareVersion = softwareDescriptor.get("Software-Version");
			LauncherUtils.softwareID = softwareDescriptor.get("Software-ID");
			if (softwareSourceClass == null)
				throw new IOException("No software class defined in emulation software descriptor");
			if (LauncherUtils.softwareID == null)
				throw new IOException("No software ID defined in emulation software descriptor");
			if (LauncherUtils.softwareVersion == null)
				throw new IOException("No software version defined in emulation software descriptor");
			if (LauncherUtils.softwareName == null)
				throw new IOException("No software name defined in emulation software descriptor");
			if (!softwareDescriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID()))
				throw new IllegalArgumentException("Emulation software '" + LauncherUtils.softwareName
						+ "' is incompatible with the current game.");

			// Set title
			LauncherUtils.log(LauncherUtils.softwareName + " Launcher v" + LAUNCHER_VERSION);
			LauncherUtils.log("Game ID: " + LauncherUtils.gameID);
			LauncherUtils.log("Emulation software ID: " + LauncherUtils.softwareID);
			LauncherUtils.log("Emulation software version: " + LauncherUtils.softwareVersion);
			frmSentinelLauncher.setTitle(LauncherUtils.softwareName + " Launcher");
			lblNewLabel.setText(LauncherUtils.softwareName);

			// Download banner and set image
			String banner = urlBaseSoftwareFile + "banner.png";
			panelLabels.setVisible(false);
			try {
				BufferedImage img = ImageIO.read(new URL(banner));
				panel_1.setImage(img);
			} catch (IOException e) {
				banner = urlBaseSoftwareFile + "banner.jpg";
				try {
					BufferedImage img = ImageIO.read(new URL(banner));
					panel_1.setImage(img);
				} catch (IOException e2) {
					banner = urlBaseDescriptorFile + "banner.png";
					try {
						BufferedImage img = ImageIO.read(new URL(banner));
						panel_1.setImage(img);
					} catch (IOException e3) {
						banner = urlBaseDescriptorFile + "banner.jpg";
						try {
							BufferedImage img = ImageIO.read(new URL(banner));
							panel_1.setImage(img);
						} catch (IOException e4) {
							panelLabels.setVisible(true);
						}
					}
				}
			}
		} catch (Exception e) {
			String stackTrace = "";
			for (StackTraceElement ele : e.getStackTrace())
				stackTrace += "\n     At: " + ele;
			log("Error occurred: " + e + stackTrace);
			JOptionPane.showMessageDialog(frmSentinelLauncher,
					"An error occured while running the launcher.\nUnable to continue, the launcher will now close.\n\nError details: "
							+ e + stackTrace + "\nPlease report this error to the server operators.",
					"Launcher Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
			return;
		}

		String urlBaseSoftwareFileF = urlBaseSoftwareFile;
		String urlBaseDescriptorFileF = urlBaseDescriptorFile;
		boolean dirModeDescriptorFileF = dirModeDescriptorFile;
		boolean dirModeSoftwareFileF = dirModeSoftwareFile;
		Thread th = new Thread(() -> {
			// Set progress bar status
			try {
				// Set label
				LauncherUtils.log("Preparing launcher...", true);
				LauncherUtils.resetProgressBar();

				boolean updatedSoftware = false;
				boolean updatedDescriptor = false;
				String descriptorClsName = descriptorSourceClass;
				String softwareClsName = softwareSourceClass;

				// Check for game descriptor updates
				if (!dirModeDescriptorFileF) {
					if (gameDescriptor.containsKey("Update-List-URL")
							&& gameDescriptor.containsKey("Game-Descriptor-Version")) {
						LauncherUtils.log("Checking for game descriptor updates...");

						// Download list
						String lst = null;
						try {
							lst = downloadString(gameDescriptor.get("Update-List-URL"));
						} catch (IOException e) {
							LauncherUtils.log("Could not download update list!");
						}
						if (lst != null) {
							JsonObject list = JsonParser.parseString(lst).getAsJsonObject();
							String latest = list.get("latest").getAsString();
							String current = gameDescriptor.get("Game-Descriptor-Version");

							// Check
							if (!latest.equals(current)) {
								// Update
								LauncherUtils.log("Updating game descriptor...", true);
								LauncherUtils.log("Updating to " + latest + "...");
								JsonObject versionData = list.get("versions").getAsJsonObject().get(latest)
										.getAsJsonObject();
								String url = versionData.get("url").getAsString();
								LauncherUtils.resetProgressBar();
								LauncherUtils.downloadFile(url, new File("gamedescriptor.sgd.tmp"));
								new File("gamedescriptor.sgd").renameTo(new File("gamedescriptor.sgd.old"));
								new File("gamedescriptor.sgd.tmp").renameTo(new File("gamedescriptor.sgd"));

								// Update
								LauncherUtils.extractGameDescriptor(new File("emulationsoftware.svp.tmp"), latest);

								// Reload
								updatedDescriptor = true;
								LauncherUtils.log("Reloading launcher...", true);
								LauncherUtils.hideProgressPanel();
								LauncherUtils.resetProgressBar();

								// Read descriptor info
								LauncherUtils.log("Loading game descriptor information...");
								gameDescriptor.clear();
								gameDescriptor.putAll(LauncherUtils
										.parseProperties(downloadString(urlBaseSoftwareFileF + "descriptorinfo")));
								descriptorClsName = gameDescriptor.get("Game-Descriptor-Class");
								LauncherUtils.gameID = gameDescriptor.get("Game-ID");
								if (descriptorClsName == null)
									throw new IOException("No descriptor class defined in game descriptor");
								if (LauncherUtils.gameID == null)
									throw new IOException("No game ID defined in game descriptor");
								LauncherUtils.log("Updated game descriptor to " + latest + "!");
							}
							LauncherUtils.setStatus("Preparing launcher...");
						}
					}
				}

				// Check for software updates
				if (!dirModeSoftwareFileF) {
					if (softwareDescriptor.containsKey("Update-List-URL")) {
						LauncherUtils.log("Checking for software updates...");

						// Download list
						String lst = null;
						try {
							lst = downloadString(softwareDescriptor.get("Update-List-URL"));
						} catch (IOException e) {
							LauncherUtils.log("Could not download update list!");
						}
						if (lst != null) {
							JsonObject list = JsonParser.parseString(lst).getAsJsonObject();
							String latest = list.get("latest").getAsString();

							// Check
							if (!latest.equals(LauncherUtils.softwareVersion)) {
								// Update
								LauncherUtils.log("Updating emulation software...", true);
								LauncherUtils.log("Updating to " + latest + "...");
								JsonObject versionData = list.get("versions").getAsJsonObject().get(latest)
										.getAsJsonObject();
								String url = versionData.get("url").getAsString();
								LauncherUtils.resetProgressBar();
								LauncherUtils.downloadFile(url, new File("emulationsoftware.svp.tmp"));
								new File("emulationsoftware.svp").renameTo(new File("emulationsoftware.svp.old"));
								new File("emulationsoftware.svp.tmp").renameTo(new File("emulationsoftware.svp"));

								// Update
								LauncherUtils.extractEmulationSoftware(new File("emulationsoftware.svp.tmp"), latest);

								// Reload
								updatedSoftware = true;
								LauncherUtils.log("Reloading launcher...", true);
								LauncherUtils.hideProgressPanel();
								LauncherUtils.resetProgressBar();
								softwareDescriptor.clear();
								softwareDescriptor.putAll(LauncherUtils
										.parseProperties(downloadString(urlBaseSoftwareFileF + "softwareinfo")));
								if (!softwareDescriptor.containsKey("Game-ID"))
									throw new IOException("No game ID defined in emulation software descriptor");
								LauncherUtils.softwareName = softwareDescriptor.get("Project-Name");
								softwareClsName = softwareDescriptor.get("Software-Class");
								LauncherUtils.softwareVersion = softwareDescriptor.get("Software-Version");
								LauncherUtils.softwareID = softwareDescriptor.get("Software-ID");
								if (softwareClsName == null)
									throw new IOException("No software class defined in emulation software descriptor");
								if (LauncherUtils.softwareID == null)
									throw new IOException("No software ID defined in emulation software descriptor");
								if (LauncherUtils.softwareVersion == null)
									throw new IOException(
											"No software version defined in emulation software descriptor");
								if (LauncherUtils.softwareName == null)
									throw new IOException("No software name defined in emulation software descriptor");
								if (!softwareDescriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID()))
									throw new IllegalArgumentException("Emulation software '"
											+ LauncherUtils.softwareName + "' is incompatible with the current game.");
								LauncherUtils.log(LauncherUtils.softwareName + " Launcher v" + LAUNCHER_VERSION);
								LauncherUtils.log("Game ID: " + LauncherUtils.gameID);
								LauncherUtils.log("Emulation software ID: " + LauncherUtils.softwareID);
								LauncherUtils.log("Emulation software version: " + LauncherUtils.softwareVersion);
								SwingUtilities.invokeAndWait(() -> {
									frmSentinelLauncher.setTitle(LauncherUtils.softwareName + " Launcher");
									lblNewLabel.setText(LauncherUtils.softwareName);

									// Download banner and set image
									String banner = urlBaseSoftwareFileF + "banner.png";
									panelLabels.setVisible(false);
									try {
										BufferedImage img = ImageIO.read(new URL(banner));
										panel_1.setImage(img);
									} catch (IOException e) {
										banner = urlBaseSoftwareFileF + "banner.jpg";
										try {
											BufferedImage img = ImageIO.read(new URL(banner));
											panel_1.setImage(img);
										} catch (IOException e2) {
											banner = urlBaseDescriptorFileF + "banner.png";
											try {
												BufferedImage img = ImageIO.read(new URL(banner));
												panel_1.setImage(img);
											} catch (IOException e3) {
												banner = urlBaseDescriptorFileF + "banner.jpg";
												try {
													BufferedImage img = ImageIO.read(new URL(banner));
													panel_1.setImage(img);
												} catch (IOException e4) {
													panelLabels.setVisible(true);
												}
											}
										}
									}
								});
								LauncherUtils.log("Updated emulation software to " + latest + "!");
							}
							LauncherUtils.setStatus("Preparing launcher...");
						}
					}
				}

				try {
					// Load object
					LauncherUtils.log("Loading game descriptor class...");
					Class<?> gameDescr = LauncherUtils.loader.loadClass(descriptorClsName);
					if (!IGameDescriptor.class.isAssignableFrom(gameDescr))
						throw new IllegalArgumentException(
								descriptorClsName + " does not implement " + IGameDescriptor.class.getTypeName());
					Constructor<?> gdCt = gameDescr.getConstructor();
					LauncherUtils.gameDescriptor = (IGameDescriptor) gdCt.newInstance();
					LauncherUtils.gameDescriptor.init();

					// Apply update
					if (updatedDescriptor) {
						new File("gamedescriptor.sgd.old").delete();
						LauncherUtils.gameDescriptor.postUpdate();
					}
				} catch (Exception e) {
					if (updatedDescriptor) {
						// Restore
						new File("gamedescriptor.sgd").delete();
						new File("gamedescriptor.sgd.old").renameTo(new File("gamedescriptor.sgd"));
					}
					if (updatedSoftware) {
						// Restore
						new File("emulationsoftware.svp").delete();
						new File("emulationsoftware.svp.old").renameTo(new File("emulationsoftware.svp"));
					}
					throw e;
				}

				try {
					// Load object
					LauncherUtils.log("Loading emulation software provider class...");
					Class<?> softProv = LauncherUtils.loader.loadClass(softwareClsName);
					if (!IEmulationSoftwareProvider.class.isAssignableFrom(softProv))
						throw new IllegalArgumentException(softwareClsName + " does not implement "
								+ IEmulationSoftwareProvider.class.getTypeName());
					Constructor<?> sfCt = softProv.getConstructor();
					LauncherUtils.emulationSoftware = (IEmulationSoftwareProvider) sfCt.newInstance();
					LauncherUtils.emulationSoftware.init();

					// Apply update
					if (updatedSoftware) {
						new File("emulationsoftware.svp.old").delete();
						LauncherUtils.emulationSoftware.postUpdate();
					}
				} catch (Exception e) {
					if (updatedSoftware) {
						// Restore
						new File("emulationsoftware.svp").delete();
						new File("emulationsoftware.svp.old").renameTo(new File("emulationsoftware.svp"));
					}
					throw e;
				}

				// TODO: launcher logic
			} catch (Exception e) {
				try {
					SwingUtilities.invokeAndWait(() -> {
						String stackTrace = "";
						for (StackTraceElement ele : e.getStackTrace())
							stackTrace += "\n     At: " + ele;
						JOptionPane.showMessageDialog(frmSentinelLauncher,
								"An error occured while running the launcher.\nUnable to continue, the launcher will now close.\n\nError details: "
										+ e + stackTrace + "\nPlease report this error to the server operators.",
								"Launcher Error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					});
				} catch (InvocationTargetException | InterruptedException e1) {
				}
			}
		}, "Launcher Thread");
		th.setDaemon(true);
		th.start();

	}

	private String downloadString(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		String data = new String(strm.readAllBytes(), "UTF-8");
		return data;
	}

	private void log(String message) {
		lblStatusLabel.setText(" " + message);
		System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] " + message);
	}

}
