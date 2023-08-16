package org.asf.razorwhip.sentinel.launcher.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

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

}
