package testdescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;
import org.asf.razorwhip.sentinel.launcher.assets.ActiveArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;

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
	public AssetInformation[] collectVersionAssets(AssetInformation[] assets, String[] qualityLevels, String version,
			ArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			Map<String, String> assetHashes) {
		// TODO Auto-generated method stub
		return new AssetInformation[0];
	}

	@Override
	public void downloadAssets(String assetServer, String[] versions, AssetInformation[] assetsNeedingUpdates,
			AssetInformation[] collectedAssets, AssetInformation[] allAssets, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, Map<String, String> assetHashes) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications,
			ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef, String clientVersion,
			File clientDir, Runnable successCallback, Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void prepareLaunchWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void startGameWithStreamingAssets(String assetArchiveURL, File assetModifications,
			ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef, String clientVersion,
			File clientDir, Runnable successCallback, Runnable exitCallback, Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public void startGameWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Runnable exitCallback,
			Consumer<String> errorCallback) {
		// TODO Auto-generated method stub
		successCallback.run();
	}

	@Override
	public String[] knownAssetQualityLevels() {
		// TODO Auto-generated method stub
		return new String[] { "High", "Medium", "Low" };
	}

}
