package org.asf.razorwhip.sentinel.launcher.assets;

import java.io.File;

/**
 * 
 * Asset Information Container
 * 
 * @author Sky Swimmer
 * 
 */
public class AssetInformation {

	/**
	 * Asset path
	 */
	public String assetPath;

	/**
	 * Asset hash
	 */
	public String assetHash;

	/**
	 * Asset client versions (often empty unless this object is returned from
	 * collected assets)
	 */
	public String[] clientVersions = new String[0];

	/**
	 * Local asset file
	 */
	public File localAssetFile;

	/**
	 * Checks if the asset is up to date
	 * 
	 * @return True if up-to-date, false otherwise
	 */
	public boolean isUpToDate() {
		return localAssetFile.exists();
	}

	@Override
	public String toString() {
		return assetPath;
	}

}
