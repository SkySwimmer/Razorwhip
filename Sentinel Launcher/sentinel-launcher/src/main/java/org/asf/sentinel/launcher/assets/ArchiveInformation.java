package org.asf.sentinel.launcher.assets;

import com.google.gson.JsonObject;

/**
 * 
 * Asset Archive Information Container
 * 
 * @author Sky Swimmer
 * 
 */
public class ArchiveInformation {

	public String archiveID;
	public String archiveName;
	public String descriptorType;

	public ArchiveMode mode;
	public String source;

	public JsonObject archiveDef;
	public JsonObject archiveClientLst;

	public boolean supportsDownloads;
	public boolean supportsStreaming;

	public boolean isDeprecated;
	public String deprecationNotice;

	public boolean connectionAvailable;
	public boolean isUserArchive;

	@Override
	public String toString() {
		String str = "";
		if (isDeprecated)
			str += "[Deprecated] ";
		str += archiveName;
		return str;
	}
}
