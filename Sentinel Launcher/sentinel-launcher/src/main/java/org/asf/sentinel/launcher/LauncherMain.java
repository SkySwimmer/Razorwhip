package org.asf.sentinel.launcher;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.sentinel.launcher.bindings.SentinelLauncherJsBindings;
import org.asf.sentinel.launcher.http.UiContentProcessor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import netscape.javascript.JSObject;

/**
 * 
 * Sentinel Launcher Controller
 * 
 * @author Sky Swimmer
 * 
 */
public class LauncherMain {

	public static String LAUNCHER_VERSION = "1.0.0.A22";

	private static boolean protocolsBound = false;

	private static ArrayList<String> filesToDeleteOnClientUpdate = new ArrayList<String>();
	private static ArrayList<Long> updateActiveClientProcesses = new ArrayList<Long>();

	private String serverUrlBase;
	private ConnectiveHttpServer server;

	private LauncherWindow launcherWindow;
	private SentinelLauncherJsBindings bindings;

	private LauncherUtils utils;

	public LauncherMain() {
		initialize();
	}

	private void initialize() {
		LauncherUtils.launcherInstance = this;
		utils = new LauncherUtils();
		getUtils().log("Preparing...");

		// Setup theme
		getUtils().log("Configuring theme...");
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

		// Bind protocols
		getUtils().log("Binding URL protocols...");
		bindProtocols();

		// Create and start server
		getUtils().log("Starting IPC server...");
		ConnectiveHttpServer server;
		Random rnd = new Random();
		while (true) {
			// Create server
			int port = rnd.nextInt(1024, 65535);
			server = ConnectiveHttpServer.create("HTTP/1.1",
					Map.of("Address", "127.0.0.1", "Port", Integer.toString(port)));

			// Setup
			setupServer(server);

			// Start
			try {
				server.start();
			} catch (IOException e) {
			}

			// Done
			serverUrlBase = "http://127.0.0.1:" + port + "/";
			getUtils().log("Started on port " + port + "!");
			break;
		}
		this.server = server;
		// FIXME: have to shut server down when done

		// Create window
		getUtils().log("Creating window...");
		launcherWindow = new LauncherWindow();
		launcherWindow.addPostInitListener(() -> postLoad());
	}

	private void setupServer(ConnectiveHttpServer server) {
		// Register processors
		server.registerProcessor(new UiContentProcessor());
	}

	private void postLoad() {
		// Bind java-to-js bindings
		bindings = new SentinelLauncherJsBindings(this);
		launcherWindow.getWebEngine().getLoadWorker().stateProperty().addListener((t, o, v) -> {
			JSObject win = (JSObject) launcherWindow.getWebEngine().executeScript("window");
			Object m = win.getMember("sentinel");
			if (m.equals("undefined")) {
				getUtils().log("Binding sentinel functions to JavaScript...");
				win.setMember("sentinel", bindings);
			}
		});
		launcherWindow.addPostLoadListener(t -> {
			JSObject win = (JSObject) launcherWindow.getWebEngine().executeScript("window");
			Object m = win.getMember("sentinel");
			if (m.equals("undefined")) {
				getUtils().log("Binding sentinel functions to JavaScript...");
				win.setMember("sentinel", bindings);
			}
			getUtils().log("Running sentinelInited() in javascript environment...");
			launcherWindow.getWebEngine().executeScript("console.log = (msg) => sentinel.log(msg)");
			launcherWindow.getWebEngine().executeScript("console.error = (msg) => sentinel.logError(msg)");
			launcherWindow.getWebEngine().executeScript("console.debug = (msg) => sentinel.logDebug(msg)");
			launcherWindow.getWebEngine().executeScript("sentinelInited()");
		});

		// Open page
		launcherWindow.getWebEngine().load(serverUrlBase + "index.html");
	}

	private static void bindProtocols() {
		if (protocolsBound)
			return;
		protocolsBound = true;
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

			@Override
			public URLStreamHandler createURLStreamHandler(String protocol) {
				switch (protocol.toLowerCase()) {

				// Sentinel
				case "sentinel": {
					return new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							if (u.getAuthority() == null) {
								throw new IOException("Missing scope in " + u + ", expected sentinel://<scope>/<path>");
							}
							switch (u.getAuthority()) {

							case "launcher": {
								return connectionFromClassAndFile(LauncherMain.class, u.getPath());
							}

							default: {
								throw new IOException("Invalid scope: " + u.getAuthority());
							}

							}
						}
					};
				}

				// Default
				default:
					return null;

				}
			}

			private URLConnection connectionFromClassAndFile(Class<?> cls, String file) throws IOException {
				// Build URL
				try {
					// Check file
					while (file.startsWith("/"))
						file = file.substring(1);

					// Get source
					URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
					File f = new File(loc.toURI());

					// Check
					if (f.isFile()) {
						// File mode
						return new URL("jar:" + f.toURI() + "!/" + file).openConnection();
					} else {
						// Directory mode
						return new File(f, file).toURI().toURL().openConnection();
					}
				} catch (Exception e) {
					throw new IOException("Could not open file " + file, e);
				}
			}

		});
	}

	/**
	 * Retrieves the launcher window
	 * 
	 * @return LauncherWindow instance
	 */
	public LauncherWindow getWindow() {
		return launcherWindow;
	}

	/**
	 * Retrieves the bindings object
	 * 
	 * @return SentinelLauncherJsBindings instance
	 */
	public SentinelLauncherJsBindings getBindings() {
		return bindings;
	}

	/**
	 * Retrieves the internal launcher server
	 * 
	 * @return ConnectiveHttpServer instance
	 */
	public ConnectiveHttpServer getServer() {
		return server;
	}

	/**
	 * Retrieves the URL of the inter-process communication system
	 * 
	 * @return Server base URL
	 */
	public String getIpcServerBaseUrl() {
		return serverUrlBase;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		LauncherUtils.args = args;

		// Check
		if (new File("sentinel.activeprocesses.sjf").exists()) {
			try {
				// Load file
				JsonObject settings = JsonParser.parseString(Files.readString(Path.of("sentinel.activeprocesses.sjf")))
						.getAsJsonObject();
				if (settings.has("processes")) {
					for (JsonElement ele : settings.get("processes").getAsJsonArray())
						updateActiveClientProcesses.add(ele.getAsLong());
				}
				if (settings.has("deleteFilesOnProcessExit")) {
					for (JsonElement ele : settings.get("deleteFilesOnProcessExit").getAsJsonArray())
						filesToDeleteOnClientUpdate.add(ele.getAsString());
				}

				// Delete
				new File("sentinel.activeprocesses.sjf").delete();
			} catch (IOException e) {
			}
		}

		// Run
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LauncherMain main = new LauncherMain();
					main.launcherWindow.getFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	static void closeClientsIfNeeded() {
		if (updateActiveClientProcesses.size() == 0)
			return;
		ArrayList<ProcessHandle> handles = new ArrayList<ProcessHandle>();
		for (long pid : updateActiveClientProcesses) {
			try {
				Optional<ProcessHandle> h = ProcessHandle.of(pid);
				if (h.isPresent() && h.get().isAlive())
					handles.add(h.get());
			} catch (Exception e) {
			}
		}
		if (handles.size() != 0) {
			// Show warning
			JOptionPane.showMessageDialog(LauncherUtils.getLauncherInstance().getUtils().getLauncherFrame(),
					"Warning!\n\nSentinel found some running client processes that need to be closed before the update can be completed.\n\nPlease close the clients before proceeding, press OK to terminate all remaining client processes.",
					"Active client processes detected", JOptionPane.WARNING_MESSAGE);

			// Close processes
			for (ProcessHandle handle : handles)
				if (handle.isAlive())
					handle.destroy();

			// Delete files pending deletion
			for (String f : filesToDeleteOnClientUpdate)
				if (new File(f).exists())
					new File(f).delete();
		}
		updateActiveClientProcesses.clear();
		filesToDeleteOnClientUpdate.clear();
	}

	/**
	 * Retrieves the launcher utilities
	 * 
	 * @return LauncherUtils instance
	 */
	public LauncherUtils getUtils() {
		return utils;
	}

}
