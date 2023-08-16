package testdescriptor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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
			JsonObject descriptorDef) throws IOException {
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

}
