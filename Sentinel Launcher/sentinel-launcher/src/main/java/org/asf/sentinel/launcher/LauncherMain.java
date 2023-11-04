package org.asf.sentinel.launcher;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.asf.sentinel.launcher.ui.SentinelLauncherJsBindings;
import org.w3c.dom.Document;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.javafx.application.PlatformImpl;

import javafx.beans.property.ReadOnlyProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class LauncherMain {

	public static String LAUNCHER_VERSION = "1.0.0.A23";

	JFrame frmSentinelLauncher;

	private static ArrayList<String> filesToDeleteOnClientUpdate = new ArrayList<String>();
	private static ArrayList<Long> updateActiveClientProcesses = new ArrayList<Long>();

	private WebView browser;
	private WebEngine engine;
	private Scene scene;

	private SentinelLauncherJsBindings bindings;

	private ArrayList<Consumer<String>> listeners = new ArrayList<Consumer<String>>();
	private ArrayList<Consumer<Document>> loadListeners = new ArrayList<Consumer<Document>>();

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
					LauncherMain window = new LauncherMain();
					LauncherUtils.launcherWindow = window;
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
			JOptionPane.showMessageDialog(LauncherUtils.launcherWindow.frmSentinelLauncher,
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

		// Bind protocols
		bindProtocols();

		// Create window
		frmSentinelLauncher = new JFrame("Sentinel Launcher");
		frmSentinelLauncher.setResizable(true);
		frmSentinelLauncher.setMinimumSize(new Dimension(800, 600));
		frmSentinelLauncher.setBounds(100, 100, 800, 600);
		frmSentinelLauncher.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSentinelLauncher.setLocationRelativeTo(null);

		// Add JFX panel
		JFXPanel panel = new JFXPanel();
		frmSentinelLauncher.setContentPane(panel);

		// Bind resize
		panel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				if (browser != null)
					browser.setPrefSize(frmSentinelLauncher.getWidth(), frmSentinelLauncher.getHeight());
			}

		});

		// Create scene and init web browser
		PlatformImpl.startup(() -> {
			// Create stage
			Stage stage = new Stage();
			stage.setResizable(true);

			// Create root group
			Group root = new Group();
			scene = new Scene(root, frmSentinelLauncher.getWidth(), frmSentinelLauncher.getHeight());
			stage.setScene(scene);

			// Create browser
			browser = new WebView();

			// Create engine
			engine = browser.getEngine();
			engine.setJavaScriptEnabled(true);

			// Bind events
			listeners.forEach(listener -> {
				engine.locationProperty().addListener(url -> {
					@SuppressWarnings("unchecked")
					ReadOnlyProperty<String> prop = (ReadOnlyProperty<String>) url;
					listener.accept(prop.getValue());
				});
			});
			listeners.clear();
			loadListeners.forEach(listener -> {
				engine.getLoadWorker().stateProperty().addListener(t -> {
					if (engine.getDocument() != null)
						listener.accept(engine.getDocument());
				});
			});
			loadListeners.clear();

			// Add browser
			ObservableList<Node> children = root.getChildren();
			children.add(browser);

			// Add to panel
			panel.setScene(scene);

			// Done loading
			postLoad();
		});
	}

	private void postLoad() {
		// Bind java-to-js bindings
		bindings = new SentinelLauncherJsBindings(this);
		engine.getLoadWorker().stateProperty().addListener(t -> {
			JSObject win = (JSObject) engine.executeScript("window");
			win.setMember("Sentinel", bindings);
		});

		// Open page
		navigate("sentinel://index.html");
	}

	private void bindProtocols() {
		URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {

			@Override
			public URLStreamHandler createURLStreamHandler(String protocol) {
				switch (protocol.toLowerCase()) {

				// Sentinel
				case "sentinel": {
					return new URLStreamHandler() {

						@Override
						protected URLConnection openConnection(URL u) throws IOException {
							return connectionFromClassAndFile(LauncherMain.class,
									u.toString().substring("sentinel:".length()));
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
	 * Navigates to a URL
	 * 
	 * @param url URL to navigate to
	 */
	public void navigate(String url) {
		PlatformImpl.startup(() -> {
			engine.load(url);
		});
	}

	/**
	 * Adds URL listeners
	 * 
	 * @param listener Consumer thats called when URLs are being loaded
	 */
	@SuppressWarnings("unchecked")
	public void addUrlListener(Consumer<String> listener) {
		if (engine == null) {
			listeners.add(listener);
			return;
		}
		engine.locationProperty().addListener(url -> {
			ReadOnlyProperty<String> prop = (ReadOnlyProperty<String>) url;
			listener.accept(prop.getValue());
		});
	}

	/**
	 * Adds document post-load listeners
	 * 
	 * @param listener Consumer thats called when a document is loaded
	 */
	public void addPostLoadListener(Consumer<Document> listener) {
		if (engine == null) {
			loadListeners.add(listener);
			return;
		}
		engine.getLoadWorker().stateProperty().addListener(t -> {
			if (engine.getDocument() != null)
				listener.accept(engine.getDocument());
		});
	}

	/**
	 * Retrieves the web engine
	 * 
	 * @return WebEngine instance
	 */
	public WebEngine getWebEngine() {
		return engine;
	}

	/**
	 * Retrieves the bindings object
	 * 
	 * @return SentinelLauncherJsBindings instance
	 */
	public SentinelLauncherJsBindings getBindings() {
		return bindings;
	}

}
