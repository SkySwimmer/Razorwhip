package org.asf.razorwhip.sentinel.launcher.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;

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
	 * Called to verify assets of a client
	 * 
	 * @param assetServer   Asset server URL
	 * @param assetDir      Asset root directory
	 * @param version       Client version
	 * @param archiveDef    Archive definition object
	 * @param descriptorDef Descriptor definition object
	 * @param assetHashes   Asset hash list
	 * @return True if verified, false if assets need updating or redownloading
	 * @throws IOException If verifying fails
	 */
	public boolean verifyLocalAssets(String assetServer, File assetDir, String version, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException;

	/**
	 * Called to retrieve the download size of the asset update
	 * 
	 * @param assetServer    Asset server URL
	 * @param assetDir       Asset root directory
	 * @param versions       Client version list
	 * @param archiveDef     Archive definition object
	 * @param descriptorDef  Descriptor definition object
	 * @param assetHashes    Asset hash list
	 * @param assetFileSizes Asset file size list
	 * @return Download size in bytes
	 * @throws IOException If indexing fails
	 */
	public long getAssetDownloadSize(String assetServer, File assetDir, String[] versions, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes, HashMap<String, Long> assetFileSizes)
			throws IOException;

	/**
	 * Called to download client assets
	 * 
	 * @param assetServer   Asset server URL
	 * @param assetDir      Asset root directory
	 * @param versions      Client version array
	 * @param archiveDef    Archive definition object
	 * @param descriptorDef Descriptor definition object
	 * @param assetHashes   Asset hash list
	 * @throws IOException If downloading fails
	 */
	public void downloadAssets(String assetServer, File assetDir, String[] versions, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException;

	/**
	 * Called to prepare to start the game
	 * 
	 * @param assetArchiveURL    URL to the asset archive
	 * @param assetModifications Local asset modifications version
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (must be called for the launch
	 *                           process to continue)
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback);

	/**
	 * Called to prepare to start the game
	 * 
	 * @param assetArchive       Local asset archive folder
	 * @param assetModifications Local asset modifications version
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (must be called for the launch
	 *                           process to continue)
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void prepareLaunchWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback);

	/**
	 * Called to start the game
	 * 
	 * @param assetArchiveURL    URL to the asset archive
	 * @param assetModifications Local asset modifications version
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (closes the launcher window)
	 * @param exitCallback       Callback for game exit
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void startGameWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback);

	/**
	 * Called to start the game
	 * 
	 * @param assetArchive       Local asset archive folder
	 * @param assetModifications Local asset modifications version
	 * @param archiveDef         Archive definition object
	 * @param descriptorDef      Descriptor definition object
	 * @param clientVersion      Client version
	 * @param clientDir          Client folder
	 * @param successCallback    Callback for success (closes the launcher window)
	 * @param exitCallback       Callback for game exit
	 * @param errorCallback      Callback for errors (call this should an error
	 *                           occur)
	 */
	public void startGameWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback);

}
