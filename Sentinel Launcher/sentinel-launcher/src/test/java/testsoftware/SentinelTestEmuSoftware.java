package testsoftware;

import java.io.File;
import java.io.IOException;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;

public class SentinelTestEmuSoftware implements IEmulationSoftwareProvider {

	@Override
	public void init() {
		LauncherUtils.showProgressPanel();
		try {
			LauncherUtils.downloadFile(
					"https://projectedge.net:5319/Modified%20Builds/android%20client%20modification.zip",
					new File("test.zip"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
