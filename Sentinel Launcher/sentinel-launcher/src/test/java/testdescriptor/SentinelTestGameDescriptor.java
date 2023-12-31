package testdescriptor;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipOutputStream;

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
	public void downloadClient(String url, String version, File clientOutputDir, ArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, String clientHash) throws IOException {
		clientOutputDir.mkdirs();
		// TODO Auto-generated method stub

	}

	@Override
	public File addClientToArchiveFolder(String version, File archiveClientsDir, File archiveDir,
			ArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef, String clientHash)
			throws IOException {
		throw new IOException("Not implemented");
	}

	@Override
	public void modifyClient(File clientDir, String version, ArchiveInformation archive, JsonObject archiveDef,
			JsonObject descriptorDef) throws IOException {
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
	public void addCleanClientFilesToArchiveFile(ZipOutputStream output, String version, String clientEntryPrefix,
			File archiveFile, ArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientHash, BiConsumer<Integer, Integer> progressCallback) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadAssets(String assetServer, String[] versions, AssetInformation[] assetsNeedingUpdates,
			AssetInformation[] collectedAssets, AssetInformation[] allAssets, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, Map<String, String> assetHashes) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, AssetInformation[] collectedAssets,
			AssetInformation[] allAssets, File assetModifications, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, String clientVersion, File clientDir,
			Runnable successCallback, Consumer<String> errorCallback) {
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
	public void startGameWithStreamingAssets(String assetArchiveURL, AssetInformation[] collectedAssets,
			AssetInformation[] allAssets, File assetModifications, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, String clientVersion, File clientDir,
			Runnable successCallback, Runnable exitCallback, Consumer<String> errorCallback) {
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
