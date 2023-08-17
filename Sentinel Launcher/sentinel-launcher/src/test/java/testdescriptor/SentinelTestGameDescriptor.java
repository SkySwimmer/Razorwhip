package testdescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

import com.google.gson.JsonObject;

public class SentinelTestGameDescriptor implements IGameDescriptor {

	@Override
	public void init() {
		getClass();
	}

	@Override
	public boolean verifyAssetConnection(String assetURL) {
		return true;
	}

	@Override
	public void downloadClient(String url, String version, File clientOutputDir, JsonObject archiveDef,
			JsonObject descriptorDef, String clientHash) throws IOException {
		clientOutputDir.mkdirs();
		// TODO Auto-generated method stub

	}

	@Override
	public void modifyClient(File clientDir, String version, JsonObject archiveDef, JsonObject descriptorDef)
			throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean verifyLocalAssets(String assetServer, File assetDir, String version, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException {
		return true;
	}

	@Override
	public void downloadAssets(String assetServer, File assetDir, String[] versions, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void prepareLaunchWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void startGameWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void startGameWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

}
