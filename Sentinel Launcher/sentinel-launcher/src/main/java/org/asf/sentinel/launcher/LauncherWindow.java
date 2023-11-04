package org.asf.sentinel.launcher;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

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

/**
 * 
 * Sentinel Launcher Window
 * 
 * @author Sky Swimmer
 * 
 */
public class LauncherWindow {

	private JFrame frmSentinelLauncher;

	private WebView browser;
	private WebEngine engine;
	private Scene scene;

	private ArrayList<Runnable> postInitListeners = new ArrayList<Runnable>();
	private ArrayList<Consumer<String>> listeners = new ArrayList<Consumer<String>>();
	private ArrayList<Consumer<Document>> loadListeners = new ArrayList<Consumer<Document>>();

	public LauncherWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		LauncherUtils.getInstance().log("Initializing UI...");

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
					browser.setPrefSize(panel.getWidth(), panel.getHeight());
			}

		});

		// Create scene and init web browser
		SwingUtilities.invokeLater(() -> {
			PlatformImpl.startup(() -> {
				LauncherUtils.getInstance().log("Starting JavaFX webview...");

				// Create stage
				Stage stage = new Stage();
				stage.setResizable(true);

				// Create root group
				Group root = new Group();
				scene = new Scene(root, panel.getWidth(), panel.getHeight());
				stage.setScene(scene);

				// Create browser
				browser = new WebView();
				browser.setContextMenuEnabled(false);

				// Create engine
				engine = browser.getEngine();
				engine.setJavaScriptEnabled(true);

				// Bind events
				listeners.forEach(listener -> {
					engine.locationProperty().addListener((url, o, v) -> {
						LauncherUtils.getInstance().log("Navigating to " + url);

						@SuppressWarnings("unchecked")
						ReadOnlyProperty<String> prop = (ReadOnlyProperty<String>) url;
						listener.accept(prop.getValue());
					});
				});
				listeners.clear();
				loadListeners.forEach(listener -> {
					engine.documentProperty().addListener((t, o, v) -> {
						if (v != null)
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
				LauncherUtils.getInstance().log("Post-initializing...");
				for (Runnable run : postInitListeners)
					run.run();
				postInitListeners.clear();
			});
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
		engine.locationProperty().addListener((url, o, v) -> {
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
		engine.documentProperty().addListener((t, o, v) -> {
			if (v != null)
				listener.accept(v);
		});
	}

	/**
	 * Adds post-init listeners
	 * 
	 * @param listener Listener to add
	 */
	public void addPostInitListener(Runnable listener) {
		if (engine != null) {
			PlatformImpl.startup(() -> {
				listener.run();
			});
			return;
		}
		postInitListeners.add(listener);
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
	 * Retrieves the launcher WebView instance
	 * 
	 * @return WebView instance
	 */
	public WebView getWebView() {
		return browser;
	}

	/**
	 * Retrieves the launcher frame
	 * 
	 * @return JFrame instance
	 */
	public JFrame getFrame() {
		return frmSentinelLauncher;
	}

}
