package org.asf.razorwhip.sentinel.launcher.descriptors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class SodGameDescriptor implements IGameDescriptor {

	@Override
	public void init() {
	}

	@Override
	public boolean verifyAssetConnection(String assetURL) {
		// Check connection
		try {
			if (!assetURL.endsWith("/"))
				assetURL += "/";

			// Test
			new URL(assetURL + "ServerDown.xml").openStream().close();
			return true;
		} catch (IOException e) {
			// Not connected
			return false;
		}
	}

	@Override
	public void downloadClient(String url, String version, File clientOutputDir, JsonObject archiveDef,
			JsonObject descriptorDef, String clientHash) throws IOException {
		// Download zip
		new File("clientzips").mkdirs();
		LauncherUtils.log("Downloading " + version + " client...", true);
		LauncherUtils.downloadFile(url, new File("clientzips/" + version + ".zip"));

		// Verify hash
		LauncherUtils.log("Verifying integrity...", true);
		String cHash = LauncherUtils
				.sha512Hash(Files.readAllBytes(new File("clientzips/" + version + ".zip").toPath()));
		if (!cHash.equals(clientHash)) {
			// Retry
			LauncherUtils.log("Downloading " + version + " client...", true);
			LauncherUtils.downloadFile(url, new File("clientzips/" + version + ".zip"));
			LauncherUtils.log("Verifying integrity...", true);
			cHash = LauncherUtils.sha512Hash(Files.readAllBytes(new File("clientzips/" + version + ".zip").toPath()));
			if (!cHash.equals(clientHash)) {
				throw new IOException("Integrity check failed!");
			}
		}

		// Extract
		LauncherUtils.log("Extracting " + version + " client...", true);
		LauncherUtils.unZip(new File("clientzips", version + ".zip"), clientOutputDir);
	}

	@Override
	public void modifyClient(File clientDir, String version, JsonObject archiveDef, JsonObject descriptorDef)
			throws IOException {
		// Modify the client

		// Modify resources.assets
		byte[] resourcesData = Files.readAllBytes(new File(clientDir, "DOMain_Data/resources.assets").toPath());
		String endpoint = descriptorDef.get("clientEndpoints").getAsJsonObject().get(version).getAsString();
		if (!endpoint.endsWith("/"))
			endpoint += "/";
		endpoint += "DWADragonsUnity/";
		replaceData(resourcesData, endpoint, "localhost:5317/DWADragonsUnity/");
		Files.write(new File(clientDir, "DOMain_Data/resources.assets").toPath(), resourcesData);
	}

	private byte[] reverse(byte[] data) {
		int ind = 0;
		byte[] iRev = new byte[data.length];
		for (int i = data.length - 1; i >= 0; i--) {
			iRev[ind++] = data[i];
		}
		return iRev;
	}

	private void replaceData(byte[] assetsData, String source, String target) throws UnsupportedEncodingException {
		// Locate byte offset
		while (true) {
			int offset = findBytes(assetsData, source.getBytes("UTF-8"));
			if (offset == -1)
				break;

			// Overwrite the data
			int length = ByteBuffer.wrap(reverse(Arrays.copyOfRange(assetsData, offset - 4, offset))).getInt();
			byte[] addr = target.getBytes(StandardCharsets.UTF_8);
			for (int i = offset; i < offset + length; i++)
				if (i - offset >= addr.length)
					assetsData[i] = 0;
				else
					assetsData[i] = addr[i - offset];
		}
	}

	private int findBytes(byte[] source, byte[] match) {
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		for (int i = 0; i < source.length; i++) {
			int pos = buffer.size();
			byte b = source[i];
			if (pos < match.length && b == match[pos])
				buffer.add(b);
			else if (pos == match.length)
				return i - buffer.size();
			else if (pos != 0)
				buffer.clear();
		}
		return -1;
	}

	@Override
	public boolean verifyLocalAssets(String assetServer, File assetDir, String version, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException {
		// Check ServerDown
		if (!new File(assetDir, "ServerDown.xml").exists())
			return false;

		// Load local hashes
		HashMap<String, String> assetHashesLocal = new HashMap<String, String>();
		indexAssetHashes(assetHashesLocal, new File("assets/localhashes.shl"));

		// Check root files
		for (String name : assetHashes.keySet()) {
			if (!name.contains("/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					return false;
			}
		}

		// Check other files
		for (String name : assetHashes.keySet()) {
			if (!name.toLowerCase().startsWith("content/") && !name.toLowerCase().startsWith("dwadragonsunity/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					return false;
			}
		}

		// Check content files
		for (String name : assetHashes.keySet()) {
			if (name.toLowerCase().startsWith("content/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					return false;
			}
		}

		// Check asset files
		for (String name : assetHashes.keySet()) {
			if (name.toLowerCase().startsWith("dwadragonsunity/win/" + version + "/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					return false;
			}
		}

		// Success
		return true;
	}

	private void indexAssetHashes(HashMap<String, String> assetHashes, File hashFile)
			throws JsonSyntaxException, IOException {
		// Check file
		if (!hashFile.exists())
			return;

		// Load hashes
		String[] lines = Files.readString(hashFile.toPath()).split("\n");
		for (String line : lines) {
			if (line.isEmpty())
				continue;
			// Parse
			String name = line.substring(0, line.indexOf(": ")).replace(";sp;", " ").replace(";cl;", ":")
					.replace(";sl;", ";");
			String hash = line.substring(line.indexOf(": ") + 2);
			assetHashes.put(name, hash);
		}
	}

	private boolean checkAsset(String name, HashMap<String, String> assetHashes,
			HashMap<String, String> assetHashesLocal, File assetDir) throws IOException {
		// Check
		LauncherUtils.log("Verifying asset: " + name);
		File asset = new File(assetDir, name);
		if (!asset.exists())
			return false;

		// Check hash
		String hashC = assetHashesLocal.get(name);
		if (hashC == null)
			return false;
		String hashR = assetHashes.get(name);
		return hashC.equals(hashR);
	}

	@Override
	public void downloadAssets(String assetServer, File assetDir, String[] versions, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes) throws IOException {
		// Load local hashes
		LauncherUtils.log("Collecting assets...", true);
		HashMap<String, String> assetHashesLocal = new HashMap<String, String>();
		indexAssetHashes(assetHashesLocal, new File("assets/localhashes.shl"));

		// Collect assets to download
		ArrayList<String> assetsToDownload = new ArrayList<String>();

		// Check root files
		for (String name : assetHashes.keySet()) {
			if (!name.contains("/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					assetsToDownload.add(name);
			}
		}

		// Check other files
		for (String name : assetHashes.keySet()) {
			if (!name.toLowerCase().startsWith("content/") && !name.toLowerCase().startsWith("dwadragonsunity/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					assetsToDownload.add(name);
			}
		}

		// Check content files
		for (String name : assetHashes.keySet()) {
			if (name.toLowerCase().startsWith("content/")) {
				// Check
				if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
					assetsToDownload.add(name);
			}
		}

		// Check asset files
		for (String version : versions) {
			for (String name : assetHashes.keySet()) {
				if (name.toLowerCase().startsWith("dwadragonsunity/win/" + version + "/")) {
					// Check
					if (!checkAsset(name, assetHashes, assetHashesLocal, assetDir))
						assetsToDownload.add(name);
				}
			}
		}

		// Prepare to download
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();
		LauncherUtils.setProgress(0, assetsToDownload.size());

		// Download
		int i = 0;
		int i2 = 0;
		for (String asset : assetsToDownload) {
			// Log
			LauncherUtils.log("Downloading asset: " + asset);
			LauncherUtils.setStatus("Downloading " + (i + 1) + "/" + assetsToDownload.size() + " assets...");

			// Prepare download
			String url = assetServer;
			if (!url.endsWith("/"))
				url += "/";
			url += asset;
			URLConnection urlConnection = new URL(url).openConnection();
			File assetF = new File(assetDir, asset);
			assetF.getParentFile().mkdirs();

			// Download
			InputStream data = urlConnection.getInputStream();
			FileOutputStream out = new FileOutputStream(assetF);
			data.transferTo(out);
			data.close();
			out.close();

			// Verify hash
			String rHash = assetHashes.get(asset);
			String cHash = LauncherUtils.sha512Hash(Files.readAllBytes(assetF.toPath()));
			if (!rHash.equals(cHash)) {
				// Failed
				assetF.delete();
				throw new IOException("Integrity check failure");
			}
			assetHashesLocal.put(asset, rHash);

			// Check if the list should be saved
			i2++;
			if (i2 >= 75) {
				i2 = 0;

				// Save hash list
				FileOutputStream fO = new FileOutputStream(new File("assets/localhashes.shl"));
				for (String name : assetHashesLocal.keySet()) {
					String hash = assetHashesLocal.get(name);
					name = name.replace(";", ";sl;").replace(":", ";cl;").replace(" ", ";sp;");
					fO.write((name + ": " + hash + "\n").getBytes("UTF-8"));
				}
				fO.close();
			}

			// Increase
			LauncherUtils.increaseProgress();
			i++;
		}

		// Done
		LauncherUtils.setProgress(LauncherUtils.getProgressMax());

		// Save hash list
		FileOutputStream fO = new FileOutputStream(new File("assets/localhashes.shl"));
		for (String name : assetHashesLocal.keySet()) {
			String hash = assetHashesLocal.get(name);
			name = name.replace(";", ";sl;").replace(":", ";cl;").replace(" ", ";sp;");
			fO.write((name + ": " + hash + "\n").getBytes("UTF-8"));
		}
		fO.close();
	}
}
