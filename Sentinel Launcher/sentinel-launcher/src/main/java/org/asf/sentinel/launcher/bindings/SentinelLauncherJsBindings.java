package org.asf.sentinel.launcher.bindings;

import org.asf.sentinel.launcher.LauncherUtils;
import org.asf.sentinel.launcher.bindings.descriptors.GameDescriptorsInterfaceBindings;
import org.asf.sentinel.launcher.bindings.experiments.ExperimentsInterfaceBindings;
import org.asf.sentinel.launcher.bindings.servers.ServersInterfaceBindings;

import com.sun.javafx.application.PlatformImpl;

import netscape.javascript.JSObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.asf.sentinel.launcher.LauncherMain;

/**
 * 
 * Main bindings
 * 
 * @author Sky Swimmer
 * 
 */
public class SentinelLauncherJsBindings {

	public SentinelLauncherJsBindings(LauncherMain launcher) {
		this.launcher = launcher;
		launcherUtils = launcher.getUtils();
		ipcServerUrl = launcher.getIpcServerBaseUrl();
		launcherVersion = LauncherMain.LAUNCHER_VERSION;

		// Bindings
		servers = new ServersInterfaceBindings();
		experiments = new ExperimentsInterfaceBindings();
	}

	//
	// Main fields
	//
	public LauncherMain launcher;
	public LauncherUtils launcherUtils;
	
	public String launcherVersion;	
	public String ipcServerUrl;

	//
	// Other bindings
	//
	public ServersInterfaceBindings servers;
	public GameDescriptorsInterfaceBindings games;
	public ExperimentsInterfaceBindings experiments;

	//
	// Icons
	//
	public void setIcon(String iconSourceUrl) throws IOException {
		if (iconSourceUrl == null) {
			return;
		}

		// Check
		if (!iconSourceUrl.contains(":"))
			iconSourceUrl = ipcServerUrl + iconSourceUrl;

		// Set
		String iconSourceUrlF = iconSourceUrl;
		runRunnableLaterOnAwt(() -> {
			try {
				// Download and assign
				URL u = new URL(iconSourceUrlF);
				InputStream strm = u.openStream();
				launcherUtils.getLauncherFrame().setIconImage(ImageIO.read(strm));
				strm.close();
			} catch (IOException e) {
			}
		});
	}

	//
	// Logging
	//

	public void log(String message) {
		launcherUtils.log("[UI MESSAGE] " + message);
	}

	public void logDebug(String message) {
		launcherUtils.log("[UI DEBUG] " + message);
	}

	public void logError(String message) {
		launcherUtils.log("[UI ERROR] " + message);
	}

	//
	// File interface
	//

	public File jFileFromStr(String path) {
		return new File(path);
	}

	public File jFileSPSC(String parent, String ch) {
		return new File(parent, ch);
	}

	public File jFileFPSC(File parent, String ch) {
		return new File(parent, ch);
	}

	//
	// Delegate tranlation
	//

	private static class ResCont {
		public Object obj;
	}

	/**
	 * Converts javascript functions to runnables
	 * 
	 * @param func Function to cast
	 * @return Runnable instance
	 */
	public Runnable functionToJRunnable(JSObject func) {
		return () -> {
			ResCont res = new ResCont();
			PlatformImpl.runAndWait(() -> {
				res.obj = func.eval("this()");
			});
		};
	}

	/**
	 * Converts javascript functions to suppliers
	 * 
	 * @param func Function to cast
	 * @return Supplier instance
	 */
	public Supplier<Object> functionToJSupplier(JSObject func) {
		return () -> {
			ResCont res = new ResCont();
			PlatformImpl.runAndWait(() -> {
				res.obj = func.eval("this()");
			});
			return res.obj;
		};
	}

	/**
	 * Runs functions later
	 * 
	 * @param func Javascript function to run later
	 */
	public void runLaterOnAwt(JSObject func) {
		SwingUtilities.invokeLater(functionToJRunnable(func));
	}

	/**
	 * Runs functions later
	 * 
	 * @param run Runnable to run later
	 */
	public void runRunnableLaterOnAwt(Runnable run) {
		SwingUtilities.invokeLater(run);
	}

	/**
	 * Runs functions later
	 * 
	 * @param func Javascript function to run later
	 * @throws InterruptedException      If our thread is interrupted while waiting
	 * @throws InvocationTargetException If an exception is thrown in the runnable
	 */
	public void runOnAwtAndWait(JSObject func) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(functionToJRunnable(func));
	}

	/**
	 * Runs functions later
	 * 
	 * @param run Runnable to run later
	 * @throws InterruptedException      If our thread is interrupted while waiting
	 * @throws InvocationTargetException If an exception is thrown in the runnable
	 */
	public void runRunnableOnAwtAndWait(Runnable run) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(run);
	}

}
