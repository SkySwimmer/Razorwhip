package org.asf.razorwhip.sentinel.launcher.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.razorwhip.sentinel.launcher.assets.ActiveArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;

import com.google.gson.JsonObject;

/**
 * 
 * Game Descriptor Interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IGameDescriptor {

	/**
	 * Called to initialize the descriptor
	 */
	public void init();

	/**
	 * Called after the descriptor data has been updated
	 */
	public default void postUpdate() {
	}

	/**
	 * Checks the connection with the asset serve
	 * 
	 * @param assetURL Asset URL string
	 * @return True if connected, false otherwise
	 */
	public boolean verifyAssetConnection(String assetURL);

	/**
	 * Defines known asset quality levels
	 * 
	 * @return Array of quality level strings
	 */
	public String[] knownAssetQualityLevels();

	/**
	 * Downloads and extracts clients
	 * 
	 * @param url             Client download URL
	 * @param version         Client version
	 * @param clientOutputDir Client output directory
	 * @param archiveDef      Archive definition object
	 * @param descriptorDef   Descriptor definition object
	 * @param clientHash      Expected client hash
	 * @throws IOException If downloading fails
	 */
	public void downloadClient(String url, String version, File clientOutputDir, JsonObject archiveDef,
			JsonObject descriptorDef, String clientHash) throws IOException;

	/**
	 * Called to modify clients
	 * 
	 * @param clientDir     Client folder
	 * @param version       Client version
	 * @param archiveDef    Archive definition object
	 * @param descriptorDef Descriptor definition object
	 * @throws IOException If modifying the client fails
	 */
	public void modifyClient(File clientDir, String version, JsonObject archiveDef, JsonObject descriptorDef)
			throws IOException;

	/**
	 * Collects assets for a specific game version
	 * 
	 * @param assets        Array of all known assets
	 * @param qualityLevels Enabled quality levels
	 * @param version       Client version
	 * @param archive       Archive instance
	 * @param archiveDef    Archive definition object
	 * @param descriptorDef Descriptor definition object
	 * @param assetHashes   Asset hash list
	 * @return Array of AssetInformation instances
	 */
	public AssetInformation[] collectVersionAssets(AssetInformation[] assets, String[] qualityLevels, String version,
			ArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			Map<String, String> assetHashes);

	/**
	 * Called to download client assets
	 * 
	 * @param assetServer          Asset server URL
	 * @param versions             Client version array
	 * @param assetsNeedingUpdates Array of client assets needing updates
	 * @param collectedAssets      Array of all collected client assets that are
	 *                             needed for the game to work
	 * @param allAssets            Array of all assets known in the archive
	 * @param archive              Archive instance
	 * @param archiveDef           Archive definition object
	 * @param descriptorDef        Descriptor definition object
	 * @param assetHashes          Asset hash list
	 * @throws IOException If downloading fails
	 */
	public void downloadAssets(String assetServer, String[] versions, AssetInformation[] assetsNeedingUpdates,
			AssetInformation[] collectedAssets, AssetInformation[] allAssets, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, Map<String, String> assetHashes) throws IOException;

	/**
	 * Called to prepare to start the game
	 * 
	 * @param assetArchiveURL    URL to the asset archive
	 * @param assetModifications Local asset modifications folder
	 * @param archive            Archive instance
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (must be called for the launch
	 *                           process to continue)
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications,
			ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef, String clientVersion,
			File clientDir, Runnable successCallback, Consumer<String> errorCallback);

	/**
	 * Called to prepare to start the game
	 * 
	 * @param collectedAssets    Array of all collected client assets that are
	 *                           needed for the game to work
	 * @param allAssets          Array of all assets known in the archive
	 * @param assetModifications Local asset modifications folder
	 * @param archive            Archive instance
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (must be called for the launch
	 *                           process to continue)
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void prepareLaunchWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Consumer<String> errorCallback);

	/**
	 * Called to start the game
	 * 
	 * @param assetArchiveURL    URL to the asset archive
	 * @param assetModifications Local asset modifications folder
	 * @param archive            Archive instance
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (closes the launcher window)
	 * @param exitCallback       Callback for game exit
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void startGameWithStreamingAssets(String assetArchiveURL, File assetModifications,
			ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef, String clientVersion,
			File clientDir, Runnable successCallback, Runnable exitCallback, Consumer<String> errorCallback);

	/**
	 * Called to start the game
	 * 
	 * @param collectedAssets    Array of all collected client assets that are
	 *                           needed for the game to work
	 * @param allAssets          Array of all assets known in the archive
	 * @param assetModifications Local asset modifications folder
	 * @param archive            Archive instance
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (closes the launcher window)
	 * @param exitCallback       Callback for game exit
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void startGameWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Runnable exitCallback,
			Consumer<String> errorCallback);

}
