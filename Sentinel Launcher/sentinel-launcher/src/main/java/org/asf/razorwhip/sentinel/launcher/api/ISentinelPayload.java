package org.asf.razorwhip.sentinel.launcher.api;

import java.io.File;
import java.util.function.Consumer;

import org.asf.razorwhip.sentinel.launcher.assets.ActiveArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;

import com.google.gson.JsonObject;

/**
 * 
 * Sentinel Payload File
 * 
 * @author Sky Swimmer
 *
 */
public interface ISentinelPayload {

	/**
	 * Called when the payload class is first loaded and constructed
	 */
	public default void preInit() {
	}

	/**
	 * Called when all payloads are being initialized
	 */
	public void init();

	/**
	 * Called when loading is done
	 */
	public default void postInit() {
	}

	/**
	 * Called to prepare to start the game
	 * 
	 * @param assetArchiveURL    URL to the asset archive
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
	public default void prepareLaunchWithStreamingAssets(String assetArchiveURL, AssetInformation[] collectedAssets,
			AssetInformation[] allAssets, File assetModifications, ActiveArchiveInformation archive,
			JsonObject archiveDef, JsonObject descriptorDef, String clientVersion, File clientDir,
			Runnable successCallback, Consumer<String> errorCallback) {
		successCallback.run();
	}

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
	public default void prepareLaunchWithLocalAssets(AssetInformation[] collectedAssets, AssetInformation[] allAssets,
			File assetModifications, ActiveArchiveInformation archive, JsonObject archiveDef, JsonObject descriptorDef,
			String clientVersion, File clientDir, Runnable successCallback, Consumer<String> errorCallback) {
		successCallback.run();
	}

	/**
	 * Called when the game exits
	 * 
	 * @param version   Client version
	 * @param clientDir Client folder
	 */
	public default void onGameExit(String version, File clientDir) {
	}

	/**
	 * Called when the game is starting
	 * 
	 * @param version   Client version
	 * @param clientDir Client folder
	 */
	public default void onGameStarting(String version, File clientDir) {
	}

	/**
	 * Called when the game has started
	 * 
	 * @param version   Client version
	 * @param clientDir Client folder
	 */
	public default void onGameLaunchSuccess(String version, File clientDir) {
	}
}
