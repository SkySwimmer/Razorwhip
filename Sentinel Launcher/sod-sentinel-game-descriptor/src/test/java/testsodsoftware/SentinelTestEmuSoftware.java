package testsodsoftware;

import java.io.IOException;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;

public class SentinelTestEmuSoftware implements IEmulationSoftwareProvider {

	@Override
	public void init() {
		getClass();
	}

	@Override
	public void showOptionWindow() {
		try {
			LauncherUtils.showVersionManager(false);
		} catch (IOException e) {
		}
	}

}
