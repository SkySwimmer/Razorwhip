package testsoftware;

import java.io.File;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;

import com.google.gson.JsonObject;

public class SentinelTestEmuSoftware implements IEmulationSoftwareProvider {

	@Override
	public void init() {
		getClass();
	}

	@Override
	public void showOptionWindow() {
		JOptionPane.showMessageDialog(null, "Test");
	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		successCallback.run();
	}

	@Override
	public void prepareLaunchWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		successCallback.run();
	}

}
