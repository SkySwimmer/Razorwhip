package org.asf.razorwhip.sentinel.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.asf.razorwhip.sentinel.launcher.assets.ActiveArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveInformation;
import org.asf.razorwhip.sentinel.launcher.assets.ArchiveMode;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;
import org.asf.razorwhip.sentinel.launcher.windows.VersionManagerWindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * Sentinel Asset Management System
 * 
 * @author Sky Swimmer
 * 
 */
public class AssetManager {

	private static LauncherMain launcherWindow;

	private static JsonObject sacConfig;
	private static String assetSourceURL;
	private static boolean assetManagementAvailable = false;

	private static JsonArray localClientsArr;

	private static boolean dirModeDescriptorFileF;
	private static boolean dirModeSoftwareFileF;
	private static File gameDescriptorFileF;
	private static File emulationSoftwareFileF;

	private static ActiveArchiveInformation activeArchive;
	private static HashMap<String, ArchiveInformation> archiveList = new LinkedHashMap<String, ArchiveInformation>();

	private static AssetInformation[] collectedAssets;
	private static String[] collectedClients;

	/**
	 * Retrieves all asset archive information objects
	 * 
	 * @return Array of ArchiveInformation instances
	 */
	public static ArchiveInformation[] getArchives() {
		return archiveList.values().toArray(t -> new ArchiveInformation[t]);
	}

	/**
	 * Retrieves archive information objects by ID
	 * 
	 * @param archiveID Archive ID
	 * @return ArchiveInformation instance or null
	 */
	public static ArchiveInformation getArchive(String archiveID) {
		return archiveList.get(archiveID);
	}

	/**
	 * Retrieves the active asset archive
	 * 
	 * @return ActiveArchiveInformation instance
	 */
	public static ActiveArchiveInformation getActiveArchive() {
		return activeArchive;
	}

	/**
	 * Retrieves the Sentinel Asset Controller (SAC) configuration object
	 * 
	 * @return JsonObject instance
	 */
	public static JsonObject getSentinelAssetControllerConfig() {
		return sacConfig;
	}

	/**
	 * Retrieves the asset information root URL
	 * 
	 * @return Asset information root URL string
	 */
	public static String getAssetInformationRootURL() {
		return assetSourceURL;
	}

	/**
	 * Checks if online asset management is available
	 * 
	 * @return True if available, false otherwise
	 */
	public static boolean isAssetOnlineManagementAvailable() {
		return assetManagementAvailable;
	}

	/**
	 * Shows the client version manager (note this should NOT be called if online
	 * asset management is unavailable)
	 * 
	 * @param firstTime True if this is the first time the window is displayed,
	 *                  false otherwise, should be false unless called internally by
	 *                  the launcher itself
	 * @return True if saved, false if cancelled
	 * @throws IOException If an error occurs loading the window
	 */
	public static boolean showVersionManager(boolean firstTime) throws IOException {
		// Check
		if (archiveList.size() == 0) {
			// Load archives into memory
			JsonObject archiveLst = loadArchiveList();
			loadArchives(archiveLst);
		}

		// Create window
		VersionManagerWindow window = new VersionManagerWindow(launcherWindow.frmSentinelLauncher, firstTime);
		boolean saved = window.showDialog();

		// Check changes
		if (saved) {
			reloadSavedSettings();
		}

		return saved;
	}

	/**
	 * Internal
	 */
	public static void reloadSavedSettings() throws IOException {
		// Delete removed clients
		File localVersions = new File("cache/clienthashes.json");
		if (localVersions.exists()) {
			JsonObject localHashList = JsonParser.parseString(Files.readString(localVersions.toPath()))
					.getAsJsonObject();
			JsonArray clientList = JsonParser.parseString(Files.readString(Path.of("assets/clients.json")))
					.getAsJsonArray();
			JsonObject newHashList = new JsonObject();
			for (String version : localHashList.keySet()) {
				// Check if present
				boolean found = false;
				for (JsonElement ele : clientList) {
					if (ele.getAsString().equals(version)) {
						found = true;
						break;
					}
				}
				if (!found) {
					// Delete client
					LauncherMain.closeClientsIfNeeded();
					LauncherUtils.deleteDir(new File("clients/client-" + version));
				} else
					newHashList.add(version, localHashList.get(version));
			}
			Files.writeString(localVersions.toPath(), newHashList.toString());
		}

		// Delete last selected version
		new File("lastclient.json").delete();
		loadClientList();

		// Reset collected data
		collectedAssets = null;
		collectedClients = null;
	}

	/**
	 * Shows the client version selection window
	 * 
	 * @param closeIfOnlyOneVersion True to automatically select a version if there
	 *                              is only one, false otherwise
	 * @return True if a version was selected, false otherwise
	 * @throws IOException If selecting a client errors
	 */
	public static boolean showClientSelector(boolean closeIfOnlyOneVersion) throws IOException {
		// Load clients list
		JsonObject settings = JsonParser.parseString(Files.readString(new File("assets/localdata.json").toPath()))
				.getAsJsonObject();
		String id = settings.get("id").getAsString();
		JsonObject archiveLst = loadArchiveList();
		JsonObject archiveDef = archiveLst.get(id).getAsJsonObject();

		// Create list
		ArrayList<String> clients = new ArrayList<String>();
		for (String clientVersion : archiveDef.get("clients").getAsJsonObject().keySet()) {
			// Check if present
			boolean found = false;
			for (JsonElement ele : localClientsArr) {
				if (ele.getAsString().equals(clientVersion)) {
					found = true;
					break;
				}
			}
			if (found) {
				clients.add(clientVersion);
			}
		}
		String clientToStart = null;
		if (clients.size() != 1 || !closeIfOnlyOneVersion) {
			// Show popup
			clientToStart = (String) JOptionPane.showInputDialog(null, "Select a client version to launch...",
					"Choose version to start", JOptionPane.QUESTION_MESSAGE, null, clients.toArray(t -> new Object[t]),
					null);
		} else
			clientToStart = clients.get(0);
		if (clientToStart == null)
			return false;

		// Write
		JsonObject lastClient = new JsonObject();
		lastClient.addProperty("version", clientToStart);
		Files.writeString(Path.of("lastclient.json"), lastClient.toString());
		return true;
	}

	/**
	 * Initializes the asset manager
	 * 
	 * @param launcherWindow         Launcher instance
	 * @param softwareDescriptor     Software descriptor data
	 * @param gameDescriptor         Game descriptor data
	 * @param dirModeDescriptorFileF Directory mode (game descriptor)
	 * @param dirModeSoftwareFileF   Directory mode (software descriptor)
	 * @param gameDescriptorFileF    Game descriptor file instance
	 * @param emulationSoftwareFileF Emulation software file instance
	 * @throws Exception if loading fails
	 */
	static void init(LauncherMain launcherWindow, Map<String, String> softwareDescriptor,
			Map<String, String> gameDescriptor, boolean dirModeDescriptorFileF, boolean dirModeSoftwareFileF,
			File gameDescriptorFileF, File emulationSoftwareFileF) throws Exception {
		// Assign fields
		AssetManager.launcherWindow = launcherWindow;
		AssetManager.dirModeDescriptorFileF = dirModeDescriptorFileF;
		AssetManager.dirModeSoftwareFileF = dirModeSoftwareFileF;
		AssetManager.emulationSoftwareFileF = emulationSoftwareFileF;
		AssetManager.gameDescriptorFileF = gameDescriptorFileF;

		// Prepare asset source URL
		String assetSourceURL = "sgd://assetarchiveinfo/";
		if (softwareDescriptor.containsKey("Asset-Information-Root-URL")) {
			assetSourceURL = softwareDescriptor.get("Asset-Information-Root-URL");
		} else if (gameDescriptor.containsKey("Asset-Information-Root-URL")) {
			assetSourceURL = gameDescriptor.get("Asset-Information-Root-URL");
		}
		assetSourceURL = parseURL(assetSourceURL, LauncherUtils.urlBaseDescriptorFile,
				LauncherUtils.urlBaseSoftwareFile, null);
		if (!assetSourceURL.endsWith("/"))
			assetSourceURL += "/";
		AssetManager.assetSourceURL = assetSourceURL;

		// Download SAC configuration into memory
		LauncherUtils.setStatus("Loading archive information...");
		LauncherUtils.log("Downloading asset archive information...");
		new File("assets").mkdirs();
		try {
			byte[] verifConfigPubKeyB = LauncherUtils.pemDecode(downloadString(assetSourceURL + "publickey.pem"));
			byte[] configStringB = downloadBytes(assetSourceURL + "archivesettings.json");
			byte[] configSignature = downloadBytes(assetSourceURL + "archivesettings.json.sig");

			// Load key
			KeyFactory fac = KeyFactory.getInstance("RSA");
			PublicKey verifConfigPubKey = fac.generatePublic(new X509EncodedKeySpec(verifConfigPubKeyB));

			// Verify
			LauncherUtils.log("Verifying signature of SAC configuration...");
			Signature s = Signature.getInstance("Sha512WithRSA");
			s.initVerify(verifConfigPubKey);
			s.update(configStringB);
			if (!s.verify(configSignature)) {
				LauncherUtils.log("Verification failure!");
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"Failed to verify the signature of the asset archive configuration.\n\nThe launcher cannot download or update assets and clients at this time.",
						"Launcher Error", JOptionPane.ERROR_MESSAGE);
				assetManagementAvailable = false;
			} else
				assetManagementAvailable = true;

			// Save
			LauncherUtils.log("Saving SAC configuration...");
			Files.write(Path.of("assets/sac-config.json"), configStringB);
		} catch (Exception e) {
			SwingUtilities.invokeAndWait(() -> {
				String stackTrace = "";
				Throwable t = e;
				while (t != null) {
					for (StackTraceElement ele : t.getStackTrace())
						stackTrace += "\n     At: " + ele;
					t = t.getCause();
					if (t != null)
						stackTrace += "\nCaused by: " + t;
				}
				System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"An error occured while running the launcher.\n\nError details: " + e + stackTrace,
						"Launcher Error", JOptionPane.ERROR_MESSAGE);
			});
			assetManagementAvailable = false;
		}

		// Check file
		if (new File("assets/sac-config.json").exists()) {
			// Load config
			LauncherUtils.log("Loading SAC configuration...");
			sacConfig = JsonParser.parseString(Files.readString(Path.of("assets/sac-config.json"))).getAsJsonObject();
		}

		// Check
		if (assetManagementAvailable) {
			// Download key
			LauncherUtils.log("Downloading SAC security key...");
			String sacKeyPem;
			try {
				sacKeyPem = downloadString(parseURL(sacConfig.get("archiveDescriptorVerificationkey").getAsString(),
						LauncherUtils.urlBaseDescriptorFile, LauncherUtils.urlBaseSoftwareFile, assetSourceURL));

				// Save key
				LauncherUtils.log("Saving SAC security key...");
				Files.writeString(Path.of("assets/sac-publickey.pem"), sacKeyPem);

				// Check assets
				LauncherUtils.log("Downloading archive list...");
				String archiveList = downloadString(parseURL(sacConfig.get("assetArchiveList").getAsString(),
						LauncherUtils.urlBaseDescriptorFile, LauncherUtils.urlBaseSoftwareFile, assetSourceURL));

				// Save list
				LauncherUtils.log("Saving archive list...");
				Files.writeString(Path.of("assets/assetarchives.json"), archiveList);
			} catch (IOException e) {
				// Check if the game can be played without a internet connection
				assetManagementAvailable = false;
				LauncherUtils.log("Could not download SAC files, verifying state...");
				if (!new File("assets/assetarchives.json").exists() || !new File("assets/sac-publickey.pem").exists()
						|| !new File("assets/localdata.json").exists()) {
					LauncherUtils.log("Missing critical files, unable to start the game.");
					JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
							"Unable to download critical files, please verify your internet connection.",
							"Launcher Error", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
				LauncherUtils.log("Assets are available, game should be playable.");
			}

			// Verify
			if (assetManagementAvailable) {
				LauncherUtils.log("Verifying local asset archives...");
				File localArchiveSettings = new File("assets/localdata.json");
				if (!localArchiveSettings.exists()) {
					// Show selection window
					LauncherUtils.log("Waiting for initial client setup...", true);
					if (!showVersionManager(!localArchiveSettings.exists()))
						System.exit(0);
				}
			}
		} else {
			// Check if the game can be played without a internet connection
			LauncherUtils.log("Could not download SAC configuration, verifying state...");
			if (!new File("assets/assetarchives.json").exists() || !new File("assets/sac-publickey.pem").exists()
					|| !new File("assets/localdata.json").exists()) {
				LauncherUtils.log("Missing critical files, unable to start the game.");
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"Unable to download critical files, please verify your internet connection.", "Launcher Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
			LauncherUtils.log("Assets are available, game should be playable.");
		}
	}

	/**
	 * Initializes archive data
	 */
	static void initArchiveData() throws Exception {
		// Load archives
		LauncherUtils.log("Loading archive settings...");

		// Load client list
		loadClientList();

		// Load settings
		JsonObject settings = JsonParser.parseString(Files.readString(new File("assets/localdata.json").toPath()))
				.getAsJsonObject();

		// Load active archive
		String archiveID = settings.get("id").getAsString();
		JsonObject archiveLst = loadArchiveList();

		// Verify settings
		boolean valid = true;
		LauncherUtils.log("Verifying archive settings...");
		if (archiveLst.has(archiveID)) {
			JsonObject archiveDef = archiveLst.get(archiveID).getAsJsonObject();
			if ((!archiveDef.get("allowFullDownload").getAsBoolean()
					&& !archiveDef.get("allowStreaming").getAsBoolean())
					|| (!archiveDef.get("allowFullDownload").getAsBoolean() && archiveDef.has("deprecated")
							&& archiveDef.has("deprecationNotice") && archiveDef.get("deprecated").getAsBoolean()))
				valid = false;
			else if (!archiveDef.get("type").getAsString().matches("^[A-Za-z0-9\\-.,_ ]+$"))
				valid = false;
		} else
			valid = false;

		// Check clients
		if (valid) {
			// Load lists
			JsonObject archiveDef = archiveLst.get(archiveID).getAsJsonObject();
			JsonObject archiveClientLst = archiveDef.get("clients").getAsJsonObject();

			// Search for element
			boolean found = false;
			for (JsonElement entry : localClientsArr) {
				// Check
				if (archiveClientLst.has(entry.getAsString())) {
					found = true;
					break;
				}
			}

			// Check
			if (!found)
				valid = false;
		}

		// Check
		if (!valid) {
			// Check status
			LauncherUtils.log("Verification failure!");
			if (!assetManagementAvailable) {
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"Unable to download critical files, please verify your internet connection.", "Launcher Error",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}

			// Requires different archive
			LauncherUtils.log("Waiting for client setup...", true);
			if (!showVersionManager(false))
				System.exit(0);

			// Reload
			settings = JsonParser.parseString(Files.readString(new File("assets/localdata.json").toPath()))
					.getAsJsonObject();
			archiveID = settings.get("id").getAsString();
			archiveLst = loadArchiveList();
		}

		// Load archives into memory
		loadArchives(archiveLst);

		// Load active archive
		loadActiveArchive(settings, false);
	}

	/**
	 * Reloads all archives
	 * 
	 * @throws IOException if reloading fails
	 */
	public static void reloadArchives() throws IOException {
		LauncherUtils.log("Reloading asset data...", true);
		LauncherUtils.log("Loading archive settings...");

		// Reload clients
		loadClientList();

		// Load settings
		JsonObject settings = JsonParser.parseString(Files.readString(new File("assets/localdata.json").toPath()))
				.getAsJsonObject();
		JsonObject archiveLst = loadArchiveList();

		// Load archives into memory
		loadArchives(archiveLst);

		// Load active archive
		loadActiveArchive(settings, true);
	}

	/**
	 * Verifies the connection of a streaming asset archive and makes the user
	 * select a different archive if offline
	 * 
	 * @throws IOException If loading fails
	 */
	public static void prepareStreamingArchiveConnection() throws IOException {
		// Check connection if needed
		LauncherUtils.log("Verifying connection...");
		boolean assetConnection = testArchiveConnection(activeArchive);

		// Check mode
		if (activeArchive.streamingModeEnabled) {
			if (!assetConnection) {
				// Error, select different archive
				if (assetManagementAvailable) {
					while (activeArchive.streamingModeEnabled && !assetConnection) {
						// Try to connect
						assetConnection = testArchiveConnection(activeArchive);
						if (JOptionPane.showConfirmDialog(launcherWindow.frmSentinelLauncher,
								"Failed to connect to the asset servers!\n\nThe archive manager will be opened, select cancel to close the launcher.",
								"No connection to server", JOptionPane.OK_CANCEL_OPTION,
								JOptionPane.ERROR_MESSAGE) != JOptionPane.OK_OPTION) {
							System.exit(0);
							return;
						}

						// Ask if the user wants to enter archive configuration
						if (JOptionPane.showConfirmDialog(launcherWindow.frmSentinelLauncher,
								"Do you wish to select a different asset archive?", "Server selection",
								JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
							LauncherUtils.log("Waiting for client setup...", true);
							if (!showVersionManager(false))
								continue;

							// Reload
							reloadArchives();
						}
					}
				} else {
					// No connection
					JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
							"Failed to connect to the asset servers!\n\nPlease verify your internet connection before trying again.",
							"No connection to server", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
			}
		}
	}

	/**
	 * Verifies and updates clients
	 * 
	 * @param throwError True to throw an exception instead of closing the program
	 *                   when the client cannot be downloaded, false otherwise
	 * @throws IOException If verification errors
	 */
	public static void verifyClients(boolean throwError) throws IOException {
		// Determine platform
		String plat = null;
		if (System.getProperty("os.name").toLowerCase().contains("win")
				&& !System.getProperty("os.name").toLowerCase().contains("darwin")) { // Windows
			plat = "windows";
		} else if (System.getProperty("os.name").toLowerCase().contains("darwin")
				|| System.getProperty("os.name").toLowerCase().contains("mac")) { // MacOS
			plat = "macos";
		} else if (System.getProperty("os.name").toLowerCase().contains("linux")) {// Linux
			plat = "linux";
		}

		// Prepare
		LauncherUtils.setStatus("Checking for updates...");
		LauncherUtils.log("Verifying clients...");
		JsonArray clientsArr = new JsonArray();
		File clientListFile = new File("assets/clients.json");
		if (clientListFile.exists()) {
			clientsArr = JsonParser.parseString(Files.readString(Path.of("assets/clients.json"))).getAsJsonArray();
		}
		File localVersions = new File("cache/clienthashes.json");
		if (!localVersions.exists())
			Files.writeString(localVersions.toPath(), "{}");
		JsonObject localHashList = JsonParser.parseString(Files.readString(localVersions.toPath())).getAsJsonObject();
		for (JsonElement clE : clientsArr) {
			String clientVersion = clE.getAsString();
			File clientFolder = new File("clients/client-" + clientVersion);

			// Check if present
			if (activeArchive.archiveClientLst.has(clientVersion)) {
				// Version is present
				LauncherUtils.log("Checking for updates for " + clientVersion + "...", true);

				// Load old hash
				String oHash = "";
				if (localHashList.has(clientVersion))
					oHash = localHashList.get(clientVersion).getAsString();

				// Load new hash
				String cHash = activeArchive.descriptorDef.get("versionHashes").getAsJsonObject().get(plat)
						.getAsJsonObject().get(clientVersion).getAsString();
				if (!oHash.equals(cHash) || !clientFolder.exists()) {
					// Download client
					if (activeArchive.mode == ArchiveMode.REMOTE)
						LauncherUtils.log("Updating client " + clientVersion + "...", true);
					else
						LauncherUtils.log("Extracting client " + clientVersion + "...", true);
					LauncherMain.closeClientsIfNeeded();
					if (activeArchive.mode == ArchiveMode.REMOTE) {
						// Check connection
						if (activeArchive.connectionAvailable) {
							// Download
							LauncherUtils.gameDescriptor.downloadClient(
									activeArchive.archiveClientLst.get(clientVersion).getAsString(), clientVersion,
									clientFolder, activeArchive, activeArchive.archiveDef, activeArchive.descriptorDef,
									cHash);
						} else {
							// Error
							if (throwError)
								throw new IOException("No server connection");
							LauncherUtils.log("Skipped client update as there is no asset server connection.");
							JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
									"Failed to connect to the asset servers!\n\nPlease verify your internet connection before trying again.",
									"No connection to server", JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}
					} else {
						// Check source
						if (!new File(activeArchive.source).exists()) {
							// Error
							if (throwError)
								throw new IOException("Source file does not exist");
							LauncherUtils.log("Skipped client update as the source file is missing.");
							JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
									"Failed to copy client data from asset source file!\n\nThe archive SGA file is not presently on disk!\n"
											+ "File path: " + activeArchive.source,
									"Archive file missing", JOptionPane.ERROR_MESSAGE);
							System.exit(1);
						}

						// Prepare extract
						String clientEntry = activeArchive.archiveClientLst.get(clientVersion).getAsString() + "/";
						LauncherUtils.log("Extracting " + clientVersion + " client...", true);
						clientFolder.mkdirs();

						// Count entries
						ZipFile zip = new ZipFile(activeArchive.source);
						int count = 0;
						Enumeration<? extends ZipEntry> en = zip.entries();
						while (en.hasMoreElements()) {
							ZipEntry ent = en.nextElement();
							if (ent == null)
								break;
							if (!ent.getName().equals(clientEntry) && ent.getName().startsWith(clientEntry))
								count++;
						}
						zip.close();
						LauncherUtils.resetProgressBar();
						LauncherUtils.showProgressPanel();
						LauncherUtils.setProgressMax(count);

						// Prepare and set max
						zip = new ZipFile(activeArchive.source);
						en = zip.entries();
						int max = count;

						// Extract zip
						int i = 0;
						while (en.hasMoreElements()) {
							ZipEntry ent = en.nextElement();
							if (ent == null)
								break;
							if (ent.getName().equals(clientEntry) || !ent.getName().startsWith(clientEntry))
								continue;

							if (ent.isDirectory()) {
								new File(clientFolder, ent.getName().substring(clientEntry.length())).mkdirs();
							} else {
								FileOutputStream output = new FileOutputStream(
										new File(clientFolder, ent.getName().substring(clientEntry.length())));
								InputStream is = zip.getInputStream(ent);
								is.transferTo(output);
								is.close();
								output.close();
							}

							LauncherUtils.setProgress(i++, max);
						}
						LauncherUtils.setProgress(max, max);
						zip.close();
					}

					// Modify client
					LauncherUtils.log("Modifying client " + clientVersion + "...", true);
					LauncherUtils.gameDescriptor.modifyClient(clientFolder, clientVersion, activeArchive,
							activeArchive.archiveDef, activeArchive.descriptorDef);
					LauncherUtils.hideProgressPanel();
					LauncherUtils.resetProgressBar();

					// Re-extract descriptor
					if (!new File("tmp-sgdextract").exists()) {
						LauncherUtils.log("Extracting game descriptor...");
						if (!dirModeDescriptorFileF)
							LauncherUtils.unZip(gameDescriptorFileF, new File("cache/tmp-sgdextract"));
						else
							LauncherUtils.copyDirWithoutProgress(gameDescriptorFileF, new File("cache/tmp-sgdextract"));
					}
					if (new File("cache/tmp-sgdextract", "clientmodifications").exists()) {
						LauncherUtils.log("Copying game descriptor client modifications...");
						LauncherUtils.copyDirWithoutProgress(new File("cache/tmp-sgdextract", "clientmodifications"),
								clientFolder);
					}
					if (new File("cache/tmp-sgdextract", "clientmodifications-" + clientVersion).exists()) {
						LauncherUtils.log("Copying version-specific game descriptor client modifications...");
						LauncherUtils.copyDirWithoutProgress(
								new File("cache/tmp-sgdextract", "clientmodifications-" + clientVersion), clientFolder);
					}

					// Re-extract software
					if (!new File("cache/tmp-svpextract").exists()) {
						LauncherUtils.log("Extracting emulation software...");
						if (!dirModeSoftwareFileF)
							LauncherUtils.unZip(emulationSoftwareFileF, new File("cache/tmp-svpextract"));
						else
							LauncherUtils.copyDirWithoutProgress(emulationSoftwareFileF,
									new File("cache/tmp-svpextract"));
					}
					if (new File("cache/tmp-svpextract", "clientmodifications").exists()) {
						LauncherUtils.log("Copying emulation software client modifications...");
						LauncherUtils.copyDirWithoutProgress(new File("cache/tmp-svpextract", "clientmodifications"),
								clientFolder);
					}
					if (new File("cache/tmp-svpextract", "clientmodifications-" + clientVersion).exists()) {
						LauncherUtils.log("Copying version-specific emulation software client modifications...");
						LauncherUtils.copyDirWithoutProgress(
								new File("cache/tmp-svpextract", "clientmodifications-" + clientVersion), clientFolder);
					}

					// Copy payloads
					LauncherUtils.log("Copying payload client modifications...", true);
					LauncherUtils.copyDirWithoutProgress(
							new File("cache/payloadcache/payloaddata", "clientmodifications"), clientFolder);
					LauncherUtils.copyDirWithoutProgress(
							new File("cache/payloadcache/payloaddata", "clientmodifications"), clientFolder);

					// Save version
					localHashList.addProperty(clientVersion, cHash);
					Files.writeString(localVersions.toPath(), localHashList.toString());
				}
			}
		}

		// Delete
		LauncherUtils.deleteDir(new File("cache/tmp-sgdextract"));
		LauncherUtils.deleteDir(new File("cache/tmp-svpextract"));

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();
		LauncherUtils.setStatus("Checking for updates...");

	}

	/**
	 * Migrates assets if they are using a older format
	 * 
	 * @throws IOException If migration errors
	 */
	public static void migrateAssetsIfNeeded() throws IOException {
		File assetRootData = new File("assets/assetarchive");
		File isSentinelArchive = new File(assetRootData, "sentinelarchive");
		if (assetRootData.exists() && !isSentinelArchive.exists()) {
			// Migrate
			migrateAssets();

			// Hide bars
			LauncherUtils.hideProgressPanel();
			LauncherUtils.resetProgressBar();
		} else if (!isSentinelArchive.exists()) {
			assetRootData.mkdirs();
			isSentinelArchive.createNewFile();
		}
		File assetArchive = new File(assetRootData, "assets");
		assetArchive.mkdirs();
	}

	/**
	 * Verifies and downloads updated assets
	 * 
	 * @throws IOException If verification errors
	 */
	public static void verifyAssets() throws IOException {
		// Check mode
		LauncherUtils.setStatus("Checking for updates...");
		if (!activeArchive.streamingModeEnabled) {
			LauncherUtils.log("Checking for asset archive updates...");

			// Verify assets
			migrateAssetsIfNeeded();
			LauncherUtils.setStatus("Checking for updates...");

			// Verify assets of clients
			AssetInformation[] allAssets = activeArchive.getAllAssets();
			AssetInformation[] collectedAssets = collectAssets();

			// Collect assets needing updates
			LauncherUtils.log("Checking for asset updates...", true);
			Map<String, AssetInformation> assetsNeedingDownloads = new LinkedHashMap<String, AssetInformation>();
			ArrayList<String> clientsNeedingUpdates = new ArrayList<String>();
			for (AssetInformation asset : collectedAssets) {
				if (!asset.isUpToDate()) {
					// Add clients
					for (String version : asset.clientVersions) {
						if (!clientsNeedingUpdates.contains(version))
							clientsNeedingUpdates.add(version);
					}

					// Add asset
					if (!assetsNeedingDownloads.containsKey(asset.assetHash))
						assetsNeedingDownloads.put(asset.assetHash, asset);
				}
			}

			// Update
			if (assetsNeedingDownloads.size() != 0) {
				if (activeArchive.mode == ArchiveMode.REMOTE) {
					// Check connection
					if (!testArchiveConnection(activeArchive) || !assetManagementAvailable) {
						// Error
						LauncherUtils.log("Skipped asset update as there is no asset server connection.");
						JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
								"Failed to connect to the asset servers!\n\nPlease verify your internet connection before trying again.",
								"No connection to server", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}

					// Download
					LauncherUtils.log("Downloading client assets...", true);
					LauncherUtils.gameDescriptor.downloadAssets(activeArchive.source,
							clientsNeedingUpdates.toArray(t -> new String[t]),
							assetsNeedingDownloads.values().toArray(t -> new AssetInformation[t]), collectedAssets,
							allAssets, activeArchive, activeArchive.archiveDef, activeArchive.descriptorDef,
							activeArchive.assetHashes);
				} else {
					// Check source
					if (!new File(activeArchive.source).exists()) {
						// Error
						LauncherUtils.log("Skipped asset update as the source file is missing.");
						JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
								"Failed to copy assets from asset source file!\n\nPlease make sure the following file exists: "
										+ activeArchive.source,
								"Asset source file missing", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}

					// Prepare
					LauncherUtils.log("Extracting client assets...", true);
					ZipFile zip = new ZipFile(activeArchive.source);
					AssetInformation[] assetsNeedingUpdates = assetsNeedingDownloads.values()
							.toArray(t -> new AssetInformation[t]);
					LauncherUtils.setProgress(0, assetsNeedingUpdates.length);
					LauncherUtils.showProgressPanel();

					// Download
					int i = 0;
					for (AssetInformation asset : assetsNeedingUpdates) {
						// Log
						LauncherUtils.log("Extracting asset: " + asset);
						LauncherUtils
								.setStatus("Extracting " + (i + 1) + "/" + assetsNeedingUpdates.length + " assets...");

						// Prepare output
						File assetF = new File(asset.localAssetFile.getPath() + ".tmp");
						assetF.getParentFile().mkdirs();

						// Download
						InputStream data = zip.getInputStream(zip.getEntry("assets/" + asset.assetHash + ".sa"));
						FileOutputStream out = new FileOutputStream(assetF);
						data.transferTo(out);
						data.close();
						out.close();

						// Verify hash
						String rHash = asset.assetHash;
						String cHash = LauncherUtils.sha512Hash(Files.readAllBytes(assetF.toPath()));
						if (!rHash.equals(cHash)) {
							// Failed
							assetF.delete();
							throw new IOException("Integrity check failure");
						}

						// Save
						assetF.renameTo(asset.localAssetFile);

						// Increase
						LauncherUtils.increaseProgress();
						i++;
					}

					// Done
					LauncherUtils.setProgress(LauncherUtils.getProgressMax());
					zip.close();
				}
			}
		}
	}

	/**
	 * Collects all game clients that are currently available
	 * 
	 * @return Array of client version strings
	 */
	public static String[] collectClients() {
		if (collectedClients != null)
			return collectedClients;

		// Collect
		LauncherUtils.log("Collecting game clients...");
		ArrayList<String> gameVersions = new ArrayList<String>();
		for (JsonElement clE : localClientsArr) {
			String clientVersion = clE.getAsString();

			// Check if present
			if (activeArchive.archiveClientLst.has(clientVersion)) {
				// Verify
				gameVersions.add(clientVersion);
			}
		}
		collectedClients = gameVersions.toArray(t -> new String[t]);
		return collectedClients;
	}

	/**
	 * Collects assets for all active game versions
	 * 
	 * @return Array of AssetInformation instances
	 */
	public static AssetInformation[] collectAssets() {
		// Check
		if (collectedAssets != null)
			return collectedAssets;

		// Collect game versions
		String[] gameVersions = collectClients();

		// Load quality levels
		ArrayList<String> qualityLevels = new ArrayList<String>();
		String[] levels = LauncherUtils.gameDescriptor.knownAssetQualityLevels();
		File enabledQualityLevelListFile = new File("assets/qualitylevels.json");
		if (enabledQualityLevelListFile.exists()) {
			// Read quality levels
			try {
				JsonArray qualityLevelsArr = JsonParser
						.parseString(Files.readString(enabledQualityLevelListFile.toPath())).getAsJsonArray();
				for (JsonElement ele : qualityLevelsArr) {
					// Check validity
					String lvl = ele.getAsString();
					if (!qualityLevels.contains(lvl) && Stream.of(levels).anyMatch(t -> t.equalsIgnoreCase(lvl))) {
						// Add
						qualityLevels.add(lvl);
					}
				}
			} catch (IOException e) {
			}
		}

		// Verify
		if (qualityLevels.size() == 0) {
			// Add all
			for (String lvl : levels)
				qualityLevels.add(lvl);
		}

		// Collect asset files
		LauncherUtils.log("Collecting asset files...", true);
		AssetInformation[] allAssets = activeArchive.getAllAssets();
		Map<String, AssetInformation> assets = new LinkedHashMap<String, AssetInformation>();
		for (String clientVersion : gameVersions) {
			LauncherUtils.log("Collecting assets of " + clientVersion + "...", true);
			AssetInformation[] collectedAssets = LauncherUtils.gameDescriptor.collectVersionAssets(allAssets,
					qualityLevels.toArray(t -> new String[t]), clientVersion, activeArchive, activeArchive.archiveDef,
					activeArchive.descriptorDef, activeArchive.assetHashes);
			for (AssetInformation asset : collectedAssets) {
				// Check if present
				if (!assets.containsKey(asset.assetHash.toLowerCase())) {
					AssetInformation as = new AssetInformation();
					as.assetHash = asset.assetHash;
					as.assetPath = asset.assetPath;
					as.localAssetFile = asset.localAssetFile;
					as.clientVersions = new String[] { clientVersion };
					assets.put(asset.assetPath.toLowerCase(), as);
				} else {
					AssetInformation as = assets.get(asset.assetPath.toLowerCase());
					if (!Stream.of(as.clientVersions).anyMatch(t -> t.equalsIgnoreCase(clientVersion)))
						as.clientVersions = appendToStringArray(as.clientVersions, clientVersion);
				}
			}
		}

		// Return
		collectedAssets = assets.values().toArray(t -> new AssetInformation[t]);
		return collectedAssets;
	}

	private static String[] appendToStringArray(String[] source, String ele) {
		String[] res = new String[source.length + 1];
		for (int i = 0; i < source.length; i++)
			res[i] = source[i];
		res[res.length - 1] = ele;
		return res;
	}

	private static void migrateAssets() throws IOException {
		// Prepare
		if (!new File("assets/migrationrejected").exists())
			new File("assets/assetarchive").renameTo(new File("assets/migrationrejected"));
		else if (!new File("assets/assetarchive", "sentinelmigrated").exists())
			LauncherUtils.copyDirWithoutProgress(new File("assets/assetarchive"), new File("assets/migrationrejected"));
		File assetRootData = new File("assets/assetarchive");
		File isSentinelArchive = new File(assetRootData, "sentinelarchive");
		File assetArchive = new File(assetRootData, "assets");
		assetRootData.mkdirs();
		assetArchive.mkdirs();

		// Create migration marker
		new File(assetRootData, "sentinelmigrated").createNewFile();

		// Migrate assets
		File migrationSource = new File("assets/migrationrejected");
		int size = indexDir(migrationSource);
		LauncherUtils.log("Migrating assets to new save structure...");
		LauncherUtils.setStatus("Migrating assets to new save structure... [0/" + size + "]");
		LauncherUtils.setProgress(0, size);
		LauncherUtils.showProgressPanel();

		// Migrate
		ArrayList<String> copiedAssets = new ArrayList<String>();
		migrateAssets(migrationSource, assetArchive, copiedAssets, "");

		// Finish
		isSentinelArchive.createNewFile();
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();
	}

	private static void migrateAssets(File source, File destinationRoot, ArrayList<String> copiedAssets, String prefix)
			throws IOException {
		if (!source.exists())
			return;

		// Go through folders
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			migrateAssets(subDir, destinationRoot, copiedAssets, prefix + subDir.getName() + "/");
			LauncherUtils.increaseProgress();
			LauncherUtils.setStatus("Migrating assets to new save structure... [" + LauncherUtils.getProgress() + "/"
					+ LauncherUtils.getProgressMax() + "]");
		}

		// Go through files
		for (File assetFile : source.listFiles(t -> t.isFile())) {
			// Get path
			String path = prefix + assetFile.getName();

			// Find asset
			AssetInformation asset = activeArchive.getAsset(path);
			if (asset != null) {
				// Found asset
				String hash = asset.assetHash;
				if (!copiedAssets.contains(hash)) {
					// Transfer
					File output = new File(destinationRoot, hash + ".sa");
					if (output.exists())
						output.delete();
					assetFile.renameTo(output);
					copiedAssets.add(hash);
				} else
					assetFile.delete();
			}

			// Increase progress
			LauncherUtils.increaseProgress();
			LauncherUtils.setStatus("Migrating assets to new save structure... [" + LauncherUtils.getProgress() + "/"
					+ LauncherUtils.getProgressMax() + "]");
		}

		// Delete if empty
		if (source.listFiles().length == 0)
			source.delete();
	}

	private static void loadArchives(JsonObject archiveLst) {
		archiveList.clear();

		// Load user-created archives
		JsonObject localArchives = null;
		File localArchivesFile = new File("assets/userarchives.json");
		if (localArchivesFile.exists()) {
			try {
				localArchives = JsonParser.parseString(Files.readString(localArchivesFile.toPath())).getAsJsonObject();
			} catch (IOException e) {
			}
		}

		for (String key : archiveLst.keySet()) {
			ArchiveInformation ac = loadArchive(key, archiveLst, localArchives);
			if (ac.descriptorType.matches("^[A-Za-z0-9\\-.,_ ]+$")) {
				try {
					if (assetManagementAvailable) {
						// Test descriptor
						String dir = parseURL(sacConfig.get("descriptorRoot").getAsString(),
								LauncherUtils.urlBaseDescriptorFile, LauncherUtils.urlBaseSoftwareFile, assetSourceURL);
						if (!dir.endsWith("/"))
							dir += "/";
						new URL(dir + ac.descriptorType + ".hash").openStream().close();
					}

					// Add archive
					archiveList.put(key, ac);
				} catch (IOException e) {
					// Test failed, descriptor invalid
				}
			}
		}
	}

	private static void loadActiveArchive(JsonObject settings, boolean isReload) throws IOException {
		// Load archive settings
		LauncherUtils.setStatus("Loading archive data...");
		LauncherUtils.log("Loading data into memory...");
		String archiveID = settings.get("id").getAsString();
		JsonObject archiveDef = archiveList.get(archiveID).archiveDef;
		boolean streamingAssets = settings.get("stream").getAsBoolean();
		if (!archiveDef.get("allowStreaming").getAsBoolean() || (archiveDef.has("deprecated")
				&& archiveDef.has("deprecationNotice") && archiveDef.get("deprecated").getAsBoolean()))
			streamingAssets = false;
		if (!archiveDef.get("allowFullDownload").getAsBoolean())
			streamingAssets = true;

		// Create object
		ActiveArchiveInformation info = new ActiveArchiveInformation();
		info.archiveID = archiveID;
		info.archiveDef = archiveDef;
		info.archiveName = archiveDef.get("archiveName").getAsString();
		info.descriptorType = archiveDef.get("type").getAsString();

		// Check if its a user-created archive
		File localArchivesFile = new File("assets/userarchives.json");
		if (localArchivesFile.exists()) {
			// Load
			JsonObject localArchives = JsonParser.parseString(Files.readString(localArchivesFile.toPath()))
					.getAsJsonObject();

			// Check
			if (localArchives.has(archiveID))
				info.isUserArchive = true;
		}

		// Load source and mode
		info.mode = ArchiveMode.REMOTE;
		if (archiveDef.has("isSgaFile") && archiveDef.get("isSgaFile").getAsBoolean()) {
			info.mode = ArchiveMode.LOCAL;

			// Load settings
			info.source = archiveDef.get("filePath").getAsString();
			info.supportsDownloads = true;
			info.supportsStreaming = false;

			// Load clients
			info.archiveClientLst = archiveDef.get("clients").getAsJsonObject();
		} else {
			// Load URL
			info.source = archiveDef.get("url").getAsString();

			// Load settings
			info.supportsDownloads = archiveDef.get("allowFullDownload").getAsBoolean();
			info.supportsStreaming = archiveDef.get("allowStreaming").getAsBoolean();
			info.archiveClientLst = archiveDef.get("clients").getAsJsonObject();

			// Load deprecation status
			if (archiveDef.has("deprecated")) {
				info.isDeprecated = archiveDef.get("deprecated").getAsBoolean();
				if (info.isDeprecated && archiveDef.has("deprecationNotice"))
					info.deprecationNotice = archiveDef.get("deprecationNotice").getAsString();
				else
					info.isDeprecated = false;
			}
		}

		// Test connection
		info.connectionAvailable = testArchiveConnection(info);

		// Store current settings
		if (info.supportsStreaming && !info.isDeprecated)
			info.streamingModeEnabled = streamingAssets;
		else if (!info.supportsStreaming)
			info.streamingModeEnabled = false;

		// Prepare to load archive descriptor
		if (info.mode == ArchiveMode.REMOTE) {
			// Remote
			LauncherUtils.log("Checking for archive updates...", true);
			LauncherUtils.log("Verifying connection...");
			boolean assetConnection = info.connectionAvailable;
			updateArchiveDescriptor(assetConnection, info);
		} else {
			// Local
			LauncherUtils.log("Checking for archive updates...", true);
			LauncherUtils.log("Verifying connection...");
			boolean assetConnection = info.connectionAvailable;
			updateLocalArchiveDescriptor(assetConnection, info);
		}

		// Load archive assets
		LauncherUtils.log("Indexing assets... Please wait...", true);
		HashMap<String, String> assetHashes = new LinkedHashMap<String, String>();
		indexAssetHashes(assetHashes, new File("assets/descriptor/hashes.shl"));
		HashMap<String, Long> index = new LinkedHashMap<String, Long>();
		indexAssets(index, new File("assets/descriptor/index.sfl"));
		for (String path : index.keySet()) {
			if (!assetHashes.containsKey(path))
				continue;
			AssetInformation asset = new AssetInformation();
			asset.assetPath = sanitizePath(path);
			asset.assetHash = assetHashes.get(path);
			asset.localAssetFile = new File("assets/assetarchive/assets", asset.assetHash + ".sa");
			info.addAsset(asset);
		}
		info.assetHashes = assetHashes;

		// Load descriptor
		info.descriptorDef = JsonParser.parseString(Files.readString(Path.of("assets/descriptor/descriptor.json")))
				.getAsJsonObject();

		// Unassign collected assets
		collectedAssets = null;
		collectedClients = null;

		// Assign
		activeArchive = info;
	}

	private static ArchiveInformation loadArchive(String archiveID, JsonObject archiveLst, JsonObject userArchives) {
		// Load archive settings
		JsonObject archiveDef = archiveLst.get(archiveID).getAsJsonObject();

		// Create object
		ArchiveInformation info = new ArchiveInformation();
		info.archiveID = archiveID;
		info.archiveDef = archiveDef;
		info.archiveName = archiveDef.get("archiveName").getAsString();
		if (userArchives != null)
			info.isUserArchive = userArchives.has(info.archiveID);
		info.descriptorType = archiveDef.get("type").getAsString();

		// Load source and mode
		info.mode = ArchiveMode.REMOTE;
		if (archiveDef.has("isSgaFile") && archiveDef.get("isSgaFile").getAsBoolean()) {
			info.mode = ArchiveMode.LOCAL;

			// Load settings
			info.source = archiveDef.get("filePath").getAsString();
			info.supportsDownloads = true;
			info.supportsStreaming = false;

			// Load clients
			info.archiveClientLst = archiveDef.get("clients").getAsJsonObject();
		} else {
			// Load URL
			info.source = archiveDef.get("url").getAsString();

			// Load settings
			info.supportsDownloads = archiveDef.get("allowFullDownload").getAsBoolean();
			info.supportsStreaming = archiveDef.get("allowStreaming").getAsBoolean();
			info.archiveClientLst = archiveDef.get("clients").getAsJsonObject();

			// Load deprecation status
			if (archiveDef.has("deprecated")) {
				info.isDeprecated = archiveDef.get("deprecated").getAsBoolean();
				if (info.isDeprecated && archiveDef.has("deprecationNotice"))
					info.deprecationNotice = archiveDef.get("deprecationNotice").getAsString();
				else
					info.isDeprecated = false;
			}
		}

		// Test connection
		info.connectionAvailable = testArchiveConnection(info);

		// Assign
		return info;
	}

	private static void indexAssets(HashMap<String, Long> assets, File sizeFile)
			throws JsonSyntaxException, IOException {
		// Load hashes
		String[] lines = Files.readString(sizeFile.toPath()).split("\n");
		for (String line : lines) {
			if (line.isEmpty())
				continue;
			// Parse
			String name = line.substring(0, line.indexOf(": ")).replace(";sp;", " ").replace(";cl;", ":")
					.replace(";sl;", ";");
			String len = line.substring(line.indexOf(": ") + 2);
			assets.put(name, Long.parseLong(len));
		}
	}

	private static void indexAssetHashes(HashMap<String, String> assetHashes, File hashFile)
			throws JsonSyntaxException, IOException {
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

	private static int indexDir(File dir) {
		int i = 0;
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			i += indexDir(subDir) + 1;
		}
		File[] listFiles = dir.listFiles(t -> !t.isDirectory());
		for (int j = 0; j < listFiles.length; j++) {
			i++;
		}
		return i;
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

	/**
	 * Tests a connection with a asset server of a archive
	 * 
	 * @param info Archive information object
	 * @return True if connected, false otherwise
	 */
	public static boolean testArchiveConnection(ArchiveInformation info) {
		boolean res = true;
		if (info.mode == ArchiveMode.REMOTE)
			res = LauncherUtils.gameDescriptor.verifyAssetConnection(info.source);
		else
			res = new File(info.source).exists();
		info.connectionAvailable = res;
		return res;
	}

	private static void loadClientList() throws JsonSyntaxException, IOException {
		localClientsArr = new JsonArray();
		File clientListFile = new File("assets/clients.json");
		if (clientListFile.exists()) {
			localClientsArr = JsonParser.parseString(Files.readString(Path.of("assets/clients.json"))).getAsJsonArray();
		}
	}

	private static String downloadString(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		return data;
	}

	private static byte[] downloadBytes(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		byte[] data = strm.readAllBytes();
		strm.close();
		return data;
	}

	private static String parseURL(String url, String urlBaseDescriptorFileF, String urlBaseSoftwareFileF,
			String sentinelAssetRoot) {
		if (url.startsWith("sgd:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = urlBaseDescriptorFileF + source;
		} else if (url.startsWith("svp:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = urlBaseSoftwareFileF + source;
		} else if (sentinelAssetRoot != null && url.startsWith("sac:")) {
			String source = url.substring(4);
			while (source.startsWith("/"))
				source = source.substring(1);
			url = sentinelAssetRoot + source;
		}
		return url;
	}

	private static void updateArchiveDescriptor(boolean assetConnection, ArchiveInformation archive)
			throws IOException {
		// Check hash
		String cHashDescriptor = "";
		if (new File("assets/descriptor.hash").exists())
			cHashDescriptor = Files.readString(Path.of("assets/descriptor.hash")).replace("\r", "").replace("\n", "");
		if (assetConnection && assetManagementAvailable) {
			// Log
			LauncherUtils.log("Checking for updates for the archive descriptor...");
			String dir = parseURL(sacConfig.get("descriptorRoot").getAsString(), LauncherUtils.urlBaseDescriptorFile,
					LauncherUtils.urlBaseSoftwareFile, assetSourceURL);
			if (!dir.endsWith("/"))
				dir += "/";
			String rHashDescriptor = downloadString(dir + archive.descriptorType + ".hash").replace("\r", "")
					.replace("\n", "");

			// Check
			if (!rHashDescriptor.equalsIgnoreCase(cHashDescriptor)) {
				// Update
				downloadArchiveDescriptor(archive);
			}
		} else {
			// Skip
			LauncherUtils.log("Skipped archive descriptor update check as there is no asset server connection.");
			if (cHashDescriptor.equals("")) {
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"Failed to connect to the asset servers!\n\nPlease verify your internet connection before trying again.",
						"No connection to server", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();
	}

	private static void updateLocalArchiveDescriptor(boolean assetConnection, ArchiveInformation archive)
			throws IOException {
		// Check hash
		String cHashDescriptor = "";
		if (!new File("assets/descriptor-local.version").exists()) {
			if (assetConnection) {
				// Re-extract descriptor
				LauncherUtils.log("Re-extracting archive descriptor...", true);

				// Count entries
				ZipFile zip = new ZipFile(archive.source);
				int count = 0;
				Enumeration<? extends ZipEntry> en = zip.entries();
				while (en.hasMoreElements()) {
					ZipEntry ent = en.nextElement();
					if (ent == null)
						break;
					if (!ent.getName().equals("descriptor/") && ent.getName().startsWith("descriptor/"))
						count++;
				}
				zip.close();
				LauncherUtils.resetProgressBar();
				LauncherUtils.showProgressPanel();
				LauncherUtils.setProgressMax(count);

				// Extract
				if (new File("assets/descriptor").exists())
					LauncherUtils.deleteDir(new File("assets/descriptor"));
				new File("assets/descriptor").mkdirs();

				// Prepare and set max
				zip = new ZipFile(archive.source);
				en = zip.entries();
				int max = count;

				// Extract zip
				int i = 0;
				while (en.hasMoreElements()) {
					ZipEntry ent = en.nextElement();
					if (ent == null)
						break;
					if (ent.getName().equals("descriptor/") || !ent.getName().startsWith("descriptor/"))
						continue;

					if (ent.isDirectory()) {
						new File("assets/descriptor", ent.getName().substring("descriptor/".length())).mkdirs();
					} else {
						FileOutputStream output = new FileOutputStream(
								new File("assets/descriptor", ent.getName().substring("descriptor/".length())));
						InputStream is = zip.getInputStream(ent);
						is.transferTo(output);
						is.close();
						output.close();
					}

					LauncherUtils.setProgress(i++, max);
				}
				LauncherUtils.setProgress(max, max);
				zip.close();

				// Write fake hash
				Files.writeString(Path.of("assets/descriptor.hash"),
						"local-" + LauncherUtils.sha512Hash((new File(archive.source).lastModified() + "-"
								+ archive.source + "-" + new File(archive.source).length()).getBytes("UTF-8")));
				Files.writeString(Path.of("assets/descriptor-local.version"), "latest");
			} else {
				// Skip
				LauncherUtils.log("Skipped archive descriptor update check as the source file doesnt exist.");
				if (cHashDescriptor.equals("")) {
					JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
							"Could not extract the archive descriptor!\n\nThe archive SGA file is not presently on disk!\n"
									+ "File path: " + archive.source,
							"Archive file missing", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
			}
		}

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();
	}

	private static void downloadArchiveDescriptor(ArchiveInformation archive) throws IOException {
		// Prepare
		LauncherUtils.log("Updating archive information...", true);
		String dir = parseURL(sacConfig.get("descriptorRoot").getAsString(), LauncherUtils.urlBaseDescriptorFile,
				LauncherUtils.urlBaseSoftwareFile, assetSourceURL);
		String rHashDescriptor = downloadString(dir + archive.descriptorType + ".hash").replace("\r", "").replace("\n",
				"");

		// Show bars
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();

		// Download
		LauncherUtils.downloadFile(dir + archive.descriptorType + ".zip", new File("assets/descriptor.zip"));

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();

		// Verify signature
		LauncherUtils.log("Verifying signature... Please wait...", true);
		if (!LauncherUtils.verifyPackageSignature(new File("assets/descriptor.zip"),
				new File("assets/sac-publickey.pem"))) {
			// Check if signed
			if (!LauncherUtils.isPackageSigned(new File("assets/descriptor.zip"))) {
				// Unsigned
				// Check support
				LauncherUtils.log("Package is unsigned.");
				if (!sacConfig.get("allowUnsignedArchiveDescriptors").getAsBoolean()) {
					LauncherUtils.log("Package is unsigned.");
					JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
							"The archive descriptor is unsigned and this game descriptor does not support unsigned archive descriptors.\n\nPlease report this error to the project's archival team.",
							"Update error", JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
			} else {
				LauncherUtils.log("Signature verification failure.");
				JOptionPane.showMessageDialog(launcherWindow.frmSentinelLauncher,
						"Failed to verify integrity of archive descriptor file.\n\nPlease report this error to the project's archival team.",
						"Update error", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		}

		// Extract
		LauncherUtils.log("Extracting archive information...", true);
		if (new File("assets/descriptor").exists())
			LauncherUtils.deleteDir(new File("assets/descriptor"));
		LauncherUtils.unZip(new File("assets/descriptor.zip"), new File("assets/descriptor"));

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();

		// Write hash
		Files.writeString(Path.of("assets/descriptor.hash"), rHashDescriptor);
	}

	private static JsonObject loadArchiveList() throws IOException {
		JsonObject archiveLst = JsonParser.parseString(Files.readString(new File("assets/assetarchives.json").toPath()))
				.getAsJsonObject();

		// Load local archives
		File localArchivesFile = new File("assets/userarchives.json");
		if (localArchivesFile.exists()) {
			// Load
			JsonObject localArchives = JsonParser.parseString(Files.readString(localArchivesFile.toPath()))
					.getAsJsonObject();

			// Add to list
			for (String id : localArchives.keySet()) {
				archiveLst.add(id, localArchives.get(id));
			}
		}

		// Return
		return archiveLst;
	}

	/**
	 * Removes user archives
	 * 
	 * @param archiveID Archive ID
	 * @throws IOException If removing the archive fails
	 */
	public static void removeUserArchive(String archiveID) throws IOException {
		if (!archiveList.containsKey(archiveID) || !archiveList.get(archiveID).isUserArchive)
			throw new IOException("User-added archive '" + archiveID + "' could not be found!");

		// Remove from list
		archiveList.remove(archiveID);

		// Remove from disk
		File localArchivesFile = new File("assets/userarchives.json");
		if (localArchivesFile.exists()) {
			// Load
			JsonObject localArchives = JsonParser.parseString(Files.readString(localArchivesFile.toPath()))
					.getAsJsonObject();

			// Remove archive
			if (localArchives.has(archiveID))
				localArchives.remove(archiveID);

			// Save
			Files.writeString(localArchivesFile.toPath(), localArchives.toString());
		}
	}

	/**
	 * Adds user archives
	 * 
	 * @param archiveDef Archive definition to add
	 * @return ArchiveInformation instance
	 * @throws IOException If adding the archive fails
	 */
	public static ArchiveInformation addUserArchive(JsonObject archiveDef) throws IOException {
		// Generate ID
		String archiveID = "user-" + UUID.randomUUID();
		while (archiveList.containsKey(archiveID)) {
			archiveID = "user-" + UUID.randomUUID();
		}

		// Load list from disk
		JsonObject localArchives = new JsonObject();
		File localArchivesFile = new File("assets/userarchives.json");
		if (localArchivesFile.exists()) {
			// Load
			localArchives = JsonParser.parseString(Files.readString(localArchivesFile.toPath())).getAsJsonObject();
		}

		// Remove archive
		localArchives.add(archiveID, archiveDef);

		// Save
		Files.writeString(localArchivesFile.toPath(), localArchives.toString());

		// Add to list
		JsonObject archiveLst = loadArchiveList();
		ArchiveInformation archive = loadArchive(archiveID, archiveLst, localArchives);
		archiveList.put(archiveID, archive);
		return archive;
	}

}