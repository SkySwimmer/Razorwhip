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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import org.asf.connective.ConnectiveHttpServer;
import org.asf.connective.tasks.AsyncTaskManager;
import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;
import org.asf.razorwhip.sentinel.launcher.descriptors.data.LauncherController;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.ContentServerRequestHandler;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.ContentServerRequestHandler.IPreProcessor;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors.ApplicationManifestPreProcessor;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors.AssetVersionsPreProcessor;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors.XmlPreProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class SodGameDescriptor implements IGameDescriptor {

	public static final String ASSET_SERVER_VERSION = "1.0.0.A6";
	public static final String BEPINEX_MINIMAL_GAME_VERSION = "3.12.0";

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

		// Check version
		if (LauncherUtils.verifyVersionRequirement(version, ">=" + BEPINEX_MINIMAL_GAME_VERSION)) {
			// Extract game descriptor file
			LauncherUtils.log("Adding assetfix and BepInEx to client...", true);
			LauncherUtils.copyDirWithProgress(new File("assetfixmod"), clientDir);
			LauncherUtils.hideProgressPanel();
			LauncherUtils.resetProgressBar();
		}
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

	@Override
	public long getAssetDownloadSize(String assetServer, File assetDir, String[] versions, JsonObject archiveDef,
			JsonObject descriptorDef, HashMap<String, String> assetHashes, HashMap<String, Long> assetFileSizes)
			throws IOException {
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

		// Retrieve size
		long size = 0;
		for (String asset : assetsToDownload) {
			size += assetFileSizes.get(asset);
		}
		return size;
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

	public void prepareLaunch(String streamingAssetsURL, File localAssetsDir, File assetModifications,
			JsonObject archiveDef, JsonObject descriptorDef, String clientVersion, File clientDir,
			Runnable successCallback, Consumer<String> errorCallback) {
		// Log
		LauncherUtils.log("Preparing asset server...", true);

		// Create server
		ConnectiveHttpServer server = ConnectiveHttpServer.create("HTTP/1.1",
				Map.of("Address", "0.0.0.0", "Port", "5317"));
		try {
			server.registerProcessor(new ContentServerRequestHandler(archiveDef, descriptorDef, localAssetsDir, "/",
					new IPreProcessor[] {

							new ApplicationManifestPreProcessor(descriptorDef),

							new XmlPreProcessor(descriptorDef),

							new AssetVersionsPreProcessor(assetModifications)

					}, streamingAssetsURL, assetModifications));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// Start
		try {
			server.start();
		} catch (IOException e) {
			// Check if its the right server
			try {
				InputStream strm = new URL("http://localhost:5317/sentineltest/sod/testrunning").openStream();
				byte[] data = strm.readAllBytes();
				strm.close();
				if (!new String(data, "UTF-8").equalsIgnoreCase("assetserver-sentinel-sod-" + ASSET_SERVER_VERSION))
					throw new IOException();
			} catch (Exception e2) {
				errorCallback.accept("Port 5317 is in use and not in use by a compatible Sentinel asset archive!");
				return;
			}
		}

		// Success
		successCallback.run();
	}

	@Override
	public void prepareLaunchWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		prepareLaunch(assetArchiveURL, null, assetModifications, archiveDef, descriptorDef, clientVersion, clientDir,
				successCallback, errorCallback);
	}

	@Override
	public void prepareLaunchWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Consumer<String> errorCallback) {
		prepareLaunch(null, assetArchive, assetModifications, archiveDef, descriptorDef, clientVersion, clientDir,
				successCallback, errorCallback);
	}

	@Override
	public void startGameWithStreamingAssets(String assetArchiveURL, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback) {
		launchGame(clientVersion, clientDir, successCallback, exitCallback, errorCallback);
	}

	@Override
	public void startGameWithLocalAssets(File assetArchive, File assetModifications, JsonObject archiveDef,
			JsonObject descriptorDef, String clientVersion, File clientDir, Runnable successCallback,
			Runnable exitCallback, Consumer<String> errorCallback) {
		launchGame(clientVersion, clientDir, successCallback, exitCallback, errorCallback);
	}

	private void launchGame(String clientVersion, File clientDir, Runnable successCallback, Runnable exitCallback,
			Consumer<String> errorCallback) {
		// Check tags
		if (!LauncherUtils.hasTag("no_launch_client")) {
			try {
				// Launch client
				LauncherUtils.log("Preparing to launch client...", true);

				// Prepare
				ProcessBuilder builder;
				String plat;

				// Determine platform
				if (System.getProperty("os.name").toLowerCase().contains("win")
						&& !System.getProperty("os.name").toLowerCase().contains("darwin")) { // Windows
					plat = "windows";
				} else if (System.getProperty("os.name").toLowerCase().contains("darwin")
						|| System.getProperty("os.name").toLowerCase().contains("mac")) { // MacOS
					plat = "macos";
				} else if (System.getProperty("os.name").toLowerCase().contains("linux")) {// Linux
					plat = "linux";
				} else
					throw new IOException("Unsupported platform");

				// Prepare startup
				File clientFile = new File(clientDir, "DOMain.exe");
				if (plat.equals("windows"))
					builder = new ProcessBuilder(clientFile.getAbsolutePath()); // Windows
				else if (plat.equals("macos") || plat.equals("linux")) {
					builder = new ProcessBuilder("wine", clientFile.getAbsolutePath()); // Linux/macos, need wine
					File prefix = new File("wineprefix");
					if (!new File(prefix, "completed").exists()) {
						prefix.mkdirs();

						// Set overrides
						LauncherUtils.log("Configuring wine...", true);
						LauncherUtils.resetProgressBar();
						try {
							ProcessBuilder proc = new ProcessBuilder("wine", "reg", "add",
									"HKEY_CURRENT_USER\\Software\\Wine\\DllOverrides", "/v", "winhttp", "/d",
									"native,builtin", "/f");
							proc.environment().put("WINEPREFIX", prefix.getCanonicalPath());
							proc.start().waitFor();
							proc = new ProcessBuilder("wine", "reg", "add",
									"HKEY_CURRENT_USER\\Software\\Wine\\DllOverrides", "/v", "d3d11", "/d", "native",
									"/f");
							proc.environment().put("WINEPREFIX", prefix.getCanonicalPath());
							proc.start().waitFor();
							proc = new ProcessBuilder("wine", "reg", "add",
									"HKEY_CURRENT_USER\\Software\\Wine\\DllOverrides", "/v", "d3d10core", "/d",
									"native", "/f");
							proc.environment().put("WINEPREFIX", prefix.getCanonicalPath());
							proc.start().waitFor();
							proc = new ProcessBuilder("wine", "reg", "add",
									"HKEY_CURRENT_USER\\Software\\Wine\\DllOverrides", "/v", "dxgi", "/d", "native",
									"/f");
							proc.environment().put("WINEPREFIX", prefix.getCanonicalPath());
							proc.start().waitFor();
							proc = new ProcessBuilder("wine", "reg", "add",
									"HKEY_CURRENT_USER\\Software\\Wine\\DllOverrides", "/v", "d3d9", "/d", "native",
									"/f");
							proc.environment().put("WINEPREFIX", prefix.getCanonicalPath());
							proc.start().waitFor();
						} catch (Exception e) {
							prefix.delete();
							JOptionPane.showMessageDialog(null,
									"Failed to configure wine, please make sure you have wine installed.",
									"Launcher Error", JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}

						// Download DXVK
						if (plat.equals("linux")) {
							LauncherUtils.log("Installing DXVK...", true);
							String man = LauncherUtils
									.downloadString("https://api.github.com/repos/doitsujin/dxvk/releases/latest");
							JsonArray assets = JsonParser.parseString(man).getAsJsonObject().get("assets")
									.getAsJsonArray();
							String dxvk = null;
							for (JsonElement ele : assets) {
								JsonObject asset = ele.getAsJsonObject();
								if (asset.get("name").getAsString().endsWith(".tar.gz")
										&& !asset.get("name").getAsString().contains("steam")) {
									dxvk = asset.get("browser_download_url").getAsString();
									break;
								}
							}
							if (dxvk == null)
								throw new Exception("Failed to find a DXVK download.");
							LauncherUtils.resetProgressBar();
							LauncherUtils.showProgressPanel();
							LauncherUtils.downloadFile(dxvk, new File("dxvk.tar.gz"));

							// Extract
							LauncherUtils.log("Extracting DXVK...", true);
							LauncherUtils.resetProgressBar();
							LauncherUtils.showProgressPanel();
							LauncherUtils.unTarGz(new File("dxvk.tar.gz"), new File("dxvk"));
							LauncherUtils.hideProgressPanel();
							LauncherUtils.resetProgressBar();

							// Install
							LauncherUtils.log("Installing DXVK...", true);
							File dxvkDir = new File("dxvk").listFiles()[0];
							File wineSys = new File(prefix, "drive_c/windows");
							wineSys.mkdirs();

							// Install for x32
							new File(wineSys, "syswow64").mkdirs();
							for (File f : new File(dxvkDir, "x32").listFiles()) {
								Files.copy(f.toPath(), new File(wineSys, "syswow64/" + f.getName()).toPath(),
										StandardCopyOption.REPLACE_EXISTING);
							}

							// Install for x64
							new File(wineSys, "system32").mkdirs();
							for (File f : new File(dxvkDir, "x64").listFiles()) {
								Files.copy(f.toPath(), new File(wineSys, "system32/" + f.getName()).toPath(),
										StandardCopyOption.REPLACE_EXISTING);
							}
						}

						// Mark done
						new File(prefix, "completed").createNewFile();
					}
					builder.environment().put("WINEPREFIX", prefix.getCanonicalPath());
				} else
					throw new IOException("Unsupported platform");
				builder.inheritIO();
				builder.directory(clientDir);

				// Log
				LauncherUtils.log("Launching client...", true);
				Process proc = builder.start();
				AsyncTaskManager.runAsync(() -> {
					// Wait for exit
					try {
						proc.waitFor();
					} catch (InterruptedException e) {
					}

					// Exited
					exitCallback.run();
				});
			} catch (Exception e) {
				String stackTrace = "";
				Throwable t = e;
				while (t != null) {
					for (StackTraceElement ele : t.getStackTrace())
						stackTrace += "\n     At: " + ele;
					t = t.getCause();
					if (t != null)
						stackTrace += "\nCaused by: " + t;
				}
				errorCallback.accept("An error occurred while starting the client: " + e + stackTrace);
			}
		} else {
			// Pass control to payloads etc
			LauncherController cont = new LauncherController();
			cont.errorCallback = errorCallback;
			cont.exitCallback = exitCallback;
			LauncherUtils.getTag("no_launch_client").setValue(LauncherController.class, cont);
		}

		// Finish
		successCallback.run();
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
			for (int i = offset; i < offset + length && i < assetsData.length; i++)
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

}
