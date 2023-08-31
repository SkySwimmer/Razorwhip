package org.asf.razorwhip.sentinel.launcher.assets;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

/**
 * 
 * Active Asset Archive Information Container
 * 
 * @author Sky Swimmer
 * 
 */
public class ActiveArchiveInformation extends ArchiveInformation {

	public JsonObject descriptorDef;

	private Map<String, AssetInformation> assets = new LinkedHashMap<String, AssetInformation>();
	public Map<String, String> assetHashes = new LinkedHashMap<String, String>();

	public boolean streamingModeEnabled;

	/**
	 * Clears all assets
	 */
	public void clearAssets() {
		assets.clear();
	}

	/**
	 * Adds assets to the asset information container
	 * 
	 * @param asset Asset object to add
	 */
	public void addAsset(AssetInformation asset) {
		assets.put(sanitizePath(asset.assetPath.toLowerCase()), asset);
	}

	/**
	 * Retrieves assets by path
	 * 
	 * @param path Asset path
	 * @return AssetInformation instance or null
	 */
	public AssetInformation getAsset(String path) {
		return assets.get(sanitizePath(path.toLowerCase()));
	}

	/**
	 * Retrieves asset file objects
	 * 
	 * @param path Asset path
	 * @return File instance or null
	 */
	public File getAssetFile(String path) {
		AssetInformation asset = getAsset(path);
		if (asset == null)
			return null;
		return asset.localAssetFile;
	}

	/**
	 * Retrieves asset hashes
	 * 
	 * @param path Asset path
	 * @return Asset hash or null
	 */
	public String getAssetHash(String path) {
		AssetInformation asset = getAsset(path);
		if (asset == null)
			return null;
		return asset.assetHash;
	}

	/**
	 * Retrieves all asset information objects
	 * 
	 * @return Array of AssetInformation instances
	 */
	public AssetInformation[] getAllAssets() {
		return assets.values().toArray(t -> new AssetInformation[t]);
	}

	private static String sanitizePath(String path) {
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		return path;
	}

}
