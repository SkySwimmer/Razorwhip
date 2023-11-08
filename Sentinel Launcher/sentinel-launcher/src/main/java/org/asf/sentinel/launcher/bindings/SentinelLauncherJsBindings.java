package org.asf.sentinel.launcher.bindings;

import org.asf.sentinel.launcher.LauncherUtils;
import org.asf.sentinel.launcher.bindings.experiments.ExperimentsInterfaceBindings;
import org.asf.sentinel.launcher.bindings.servers.ServersInterfaceBindings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

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
	public String launcherVersion;
	public LauncherUtils launcherUtils;
	public String ipcServerUrl;
	public boolean bindFailure = false;

	//
	// Other bindings
	//
	public ServersInterfaceBindings servers;
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

		// Download and assign
		URL u = new URL(iconSourceUrl);
		InputStream strm = u.openStream();
		launcherUtils.getLauncherFrame().setIconImage(ImageIO.read(strm));
		strm.close();
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
	// Core binding
	//
	public void sentinelBind() {
		launcherUtils.getLauncherWindow().getWebEngine().executeScript("bind()");
	}

}
