package org.asf.sentinel.launcher.ui;

import org.asf.sentinel.launcher.LauncherUtils;
import org.asf.sentinel.launcher.LauncherMain;

public class SentinelLauncherJsBindings {

	private LauncherMain launcher;

	public SentinelLauncherJsBindings(LauncherMain launcher) {
		this.launcher = launcher;
		launcherUtils = launcher.getUtils();
	}

	public String getLauncherServerUrl() {
		return launcher.getIpcServerBaseUrl();
	}

	public LauncherUtils launcherUtils;

	public void log(String message) {
		launcherUtils.log("[UI MESSAGE] " + message);
	}

	public void logDebug(String message) {
		launcherUtils.log("[UI DEBUG] " + message);
	}

	public void logError(String message) {
		launcherUtils.log("[UI ERROR] " + message);
	}

}
