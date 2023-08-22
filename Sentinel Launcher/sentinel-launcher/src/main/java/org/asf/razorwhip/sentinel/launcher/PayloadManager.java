package org.asf.razorwhip.sentinel.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.asf.razorwhip.sentinel.launcher.api.ISentinelPayload;
import org.asf.razorwhip.sentinel.launcher.api.PayloadType;
import org.asf.razorwhip.sentinel.launcher.windows.PayloadManagerWindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Sentinel Payload Manager
 * 
 * @author Sky Swimmer
 *
 */
public class PayloadManager {
	private static boolean discoveredPayloads;
	private static ArrayList<String> payloadLoadOrder = new ArrayList<String>();
	private static LinkedHashMap<String, PayloadEntry> payloads = new LinkedHashMap<String, PayloadEntry>();

	public static class PayloadDependency {
		public String id;
		public String version;
		public String versionString;

		public String url;
	}

	public static class PayloadEntry {
		public File payloadFile;
		public File payloadExtractedDir;

		public String id;
		public String name;
		public String version;

		public PayloadType type;

		public PayloadDependency[] dependencies;
		public PayloadDependency[] optionalDependencies;
		public PayloadDependency[] conflictsWith;
		public String[] loadBefore;

		public Map<String, String> descriptor;
		public ISentinelPayload payloadObject;

		public boolean enabled;
	}

	/**
	 * Retrieves payload instances (returns null for resource payloads)
	 * 
	 * @param id Payload ID
	 * @return ISentinelPayload instance or null
	 */
	public static ISentinelPayload getPayload(String id) {
		if (!isPayloadLoaded(id))
			return null;
		return payloads.get(id).payloadObject;
	}

	/**
	 * Checks if a payload is loaded
	 * 
	 * @param id Payload ID
	 * @return True if loaded, false otherwise
	 */
	public static boolean isPayloadLoaded(String id) {
		return payloads.containsKey(id);
	}

	/**
	 * Retrieves loaded payload IDs
	 * 
	 * @return Array of payload IDs
	 */
	public static String[] getLoadedPayloadIds() {
		return payloadLoadOrder.toArray(t -> new String[t]);
	}

	/**
	 * Deletes payloads marked for deletion
	 * 
	 * @throws IOException If an error occurs while deleting payloads
	 */
	public static void deletePayloadsPendingRemoval() throws IOException {
		// Delete payloads pending removal
		if (new File("payloadstoremove.json").exists()) {
			// Load lists
			JsonArray payloadsToRemove = JsonParser.parseString(Files.readString(Path.of("payloadstoremove.json")))
					.getAsJsonArray();

			// Remove files
			for (JsonElement ele : payloadsToRemove) {
				File payloadF = new File("payloads", ele.getAsString());

				// Delete key
				try {
					Map<String, String> descriptor = LauncherUtils
							.parseProperties(getStringFrom(payloadF, "payloadinfo"));
					String id = payloadF.getName();
					if (descriptor.containsKey("Payload-ID"))
						id = descriptor.get("Payload-ID");
					if (new File("payloadcache/payloadverificationkeys", id + ".pem").exists()) {
						new File("payloadcache/payloadverificationkeys", id + ".pem").delete();
					}
				} catch (Exception e) {
				}
				// Delete
				if (payloadF.exists())
					payloadF.delete();
			}

			// Delete
			new File("payloadstoremove.json").delete();
		}
	}

	/**
	 * Attempts to detect new payloads
	 */
	public static void discoverPayloads() throws IOException {
		// Prepare
		new File("payloads").mkdirs();

		// Load index
		JsonObject index = new JsonObject();
		if (new File("activepayloads.json").exists()) {
			index = JsonParser.parseString(Files.readString(Path.of("activepayloads.json"))).getAsJsonObject();
		}
		for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
			// Check
			if (!index.has(spf.getName())) {
				discoveredPayloads = true;

				// Extract key
				try {
					Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));
					String id = spf.getName();
					if (descriptor.containsKey("Payload-ID"))
						id = descriptor.get("Payload-ID");
					if (LauncherUtils.isPackageSigned(spf) && LauncherUtils.verifyPackageSignature(
							new File("payloadcache/payloadverificationkeys", id + ".pem"), spf)) {
						LauncherUtils.extractPackagePublicKey(
								new File("payloadcache/payloadverificationkeys", id + ".pem"), spf);
					}
				} catch (Exception e) {
				}
			}
		}

		// Index
		indexPayloads();
	}

	/**
	 * Adds all payloads to the index
	 */
	public static JsonObject indexPayloads() throws IOException {
		// Prepare
		new File("payloads").mkdirs();

		// Load index
		JsonObject index = new JsonObject();
		if (new File("activepayloads.json").exists()) {
			index = JsonParser.parseString(Files.readString(Path.of("activepayloads.json"))).getAsJsonObject();
		}

		// Add to index
		JsonObject newIndex = new JsonObject();
		for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
			// Check
			if (!index.has(spf.getName())) {
				newIndex.addProperty(spf.getName(), false);
			} else
				newIndex.addProperty(spf.getName(), index.get(spf.getName()).getAsBoolean());
		}

		// Save index
		Files.writeString(Path.of("activepayloads.json"), newIndex.toString());
		return newIndex;
	}

	/**
	 * Shows the payload management window if needed
	 * 
	 * @throws IOException If loading the payload window fails
	 */
	public static void showPayloadManagementWindowIfNeeded() throws IOException {
		if (discoveredPayloads) {
			LauncherUtils.log("Opening payload manager...", true);
			discoveredPayloads = false;
			JOptionPane.showMessageDialog(null, "Discovered new payloads!", "Payloads detected",
					JOptionPane.INFORMATION_MESSAGE);
			showPayloadManagementWindow();
		}
	}

	/**
	 * Shows the payload management window
	 * 
	 * @throws IOException If loading the payload window fails
	 */
	public static boolean showPayloadManagementWindow() throws IOException {
		JsonObject lastIndex = indexPayloads();
		PayloadManagerWindow window = new PayloadManagerWindow();
		if (window.showDialog()) {
			// Delete removed payloads
			JsonObject index = new JsonObject();
			if (new File("activepayloads.json").exists()) {
				index = JsonParser.parseString(Files.readString(Path.of("activepayloads.json"))).getAsJsonObject();
			}
			JsonArray payloadsToRemove = new JsonArray();
			for (String oldKey : lastIndex.keySet()) {
				if (!index.has(oldKey))
					payloadsToRemove.add(oldKey);
			}
			if (payloadsToRemove.size() != 0) {
				// Save and restart
				Files.writeString(Path.of("payloadstoremove.json"), payloadsToRemove.toString());
				LauncherUtils.log("Requesting restart via exit code 237...");
				System.exit(237);
			}
			return true;
		}
		return false;
	}

	/**
	 * Initializes the payload manager
	 * 
	 * @throws IOException If loading fails
	 */
	public static void initPayloads() throws IOException {
		// Index
		LauncherUtils.log("Preparing to load payloads...");
		JsonObject index = indexPayloads();

		// Prepare
		new File("payloadcache/payloadverificationkeys").mkdirs();

		// Load payload hash list
		JsonObject payloadHashes = new JsonObject();
		if (new File("payloadcache", "payloadhashes.json").exists()) {
			payloadHashes = JsonParser.parseString(Files.readString(Path.of("payloadcache/payloadhashes.json")))
					.getAsJsonObject();
		}

		ArrayList<String> updatedPayloadFiles = new ArrayList<String>();
		ArrayList<String> removedPayloadFiles = new ArrayList<String>();
		while (true) {
			// Find updated and removed payloads
			updatedPayloadFiles.clear();
			removedPayloadFiles.clear();
			payloadLoadOrder.clear();
			payloads.clear();

			for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
				// Check
				if (!payloadHashes.has(spf.getName())) {
					updatedPayloadFiles.add(spf.getName());
					payloadHashes.addProperty(spf.getName(),
							LauncherUtils.sha512Hash(Files.readAllBytes(spf.toPath())));
				} else {
					// Check old hash
					String oHash = payloadHashes.get(spf.getName()).getAsString();
					String cHash = LauncherUtils.sha512Hash(Files.readAllBytes(spf.toPath()));
					if (!oHash.equals(cHash)) {
						// Updated
						updatedPayloadFiles.add(spf.getName());
						payloadHashes.addProperty(spf.getName(), cHash);
					}
				}
			}
			for (String file : payloadHashes.keySet()) {
				if (!new File("payloads", file).exists())
					removedPayloadFiles.add(file);
			}

			// Update list
			for (String file : removedPayloadFiles) {
				payloadHashes.remove(file);
			}

			// Go through updated payloads and verify signatures
			boolean showManager = false;
			for (String file : updatedPayloadFiles.toArray(t -> new String[t])) {
				File spf = new File("payloads", file);

				// Load descriptor for signature verification
				Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));
				String name = spf.getName();
				String id = spf.getName();
				if (descriptor.containsKey("Payload-ID")) {
					name = descriptor.get("Payload-ID");
					id = name;
				}
				if (descriptor.containsKey("Payload-Name"))
					name = descriptor.get("Payload-Name");

				// Check signature
				if (!LauncherUtils.verifyPackageSignature(spf,
						new File("payloadcache/payloadverificationkeys", id + ".pem"))) {
					// Error
					JOptionPane.showMessageDialog(null, "ERROR! Failed to verify package signature!\n" //
							+ "\n" //
							+ "Failed to verify the update of the payload file for '" + name + "'!\n" //
							+ "The file that was added or updated may have been tampered with, proceed with caution!\n" //
							+ "\n" //
							+ "The payload file has been deleted from disk, use the payload manager to re-add it." //
							, "Signature error", JOptionPane.ERROR_MESSAGE);
					spf.delete();
					updatedPayloadFiles.remove(file);
					removedPayloadFiles.add(file);
					payloadHashes.remove(file);
					showManager = true;
				}
			}

			// Delete removed payloads
			LauncherUtils.log("Removing deleted payloads...", true);
			for (String file : removedPayloadFiles) {
				// Delete
				LauncherUtils.deleteDir(new File("payloadcache/payloads/" + file));
			}

			// Extract updated payloads
			LauncherUtils.log("Extracting updated payloads...", true);
			for (String file : updatedPayloadFiles) {
				// Delete
				LauncherUtils.log("Extracting payload " + file + "...");
				LauncherUtils.unZip(new File("payloads", file), new File("payloadcache/payloads/" + file));

				// Check
				File payloadDataFolder = new File("payloadcache/payloads/" + file, "payloaddata");
				if (payloadDataFolder.exists()) {
					// Read descriptor
					Map<String, String> descriptor = LauncherUtils
							.parseProperties(getStringFrom(new File("payloadcache/payloads/" + file), "payloadinfo"));
					if (descriptor.containsKey("Resource-Target") && descriptor.containsKey("Resource-Target-Path")) {
						File target = new File(
								new File(new File("payloadcache/payloads/" + file), descriptor.get("Resource-Target")),
								descriptor.get("Resource-Target-Path"));

						// Create
						target.getParentFile().mkdirs();

						// Move
						payloadDataFolder.renameTo(target);
					}
				}
			}
			LauncherUtils.resetProgressBar();
			LauncherUtils.hideProgressPanel();

			// Save hash list
			if (removedPayloadFiles.size() != 0 || updatedPayloadFiles.size() != 0)
				new File("payloadcache/requireupdate").createNewFile();
			Files.writeString(Path.of("payloadcache/payloadhashes.json"), payloadHashes.toString());

			// Find all payloads
			LauncherUtils.log("Preparing to load payloads...", true);
			LauncherUtils.log("Finding payloads...");
			for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
				boolean enabled = index.has(spf.getName()) && index.get(spf.getName()).getAsBoolean();

				// Load payload
				loadPayloadFile(spf, new File("payloadcache/payloads", spf.getName()), index, enabled);
			}

			// Load debug payloads
			if (System.getProperty("debugPayloadFiles") != null
					|| System.getProperty("debugPayloadHintClasses") != null) {
				LauncherUtils.log("Loading debug payloads...");

				// Load debug files
				if (System.getProperty("debugPayloadFiles") != null) {
					String[] files = System.getProperty("debugPayloadFiles").split(File.pathSeparator);
					for (String file : files) {
						// Load
						File spfD = new File(file);
						loadPayloadFile(spfD, spfD, index, true);
					}
				}

				// Load debug classes
				if (System.getProperty("debugPayloadHintClasses") != null) {
					String[] classes = System.getProperty("debugPayloadHintClasses").split(":");
					for (String clsN : classes) {
						LauncherUtils.log("Loading hint class: " + clsN);
						try {
							Class<?> cls = Class.forName(clsN);
							URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
							File f = new File(loc.toURI());

							// Load payload
							File fD = f;
							if (!fD.isDirectory()) {
								// Extract
								LauncherUtils.log("Extracting payload " + f.getName() + "...");
								LauncherUtils.unZip(f, new File("payloadcache/payloadsdebug/" + clsN));
								fD = new File("payloadcache/payloadsdebug/" + clsN);
							}
							loadPayloadFile(f, fD, index, true);
						} catch (Exception e) {
							throw new IOException("Hint class load failure", e);
						}
					}
				}
			}

			// Compile load order
			LauncherUtils.log("Compiling load order...");
			for (String id : payloads.keySet()) {
				PayloadEntry payload = payloads.get(id);
				if (!payload.enabled || payloadLoadOrder.contains(payload.id))
					continue;
				if (!loadPayload(payload, payloadLoadOrder, payloads))
					showManager = true;
			}

			// Break if possible
			if (!showManager)
				break;
			else if (!showPayloadManagementWindow())
				System.exit(1);
		}
		LauncherUtils.resetProgressBar();
		LauncherUtils.hideProgressPanel();

		// Check index change
		boolean indexChange = false;
		if (!new File("payloadcache/lastactive.json").exists()) {
			indexChange = true;
		} else {
			// Verify
			if (!Files.readString(new File("payloadcache/lastactive.json").toPath()).equals(index.toString()))
				indexChange = true;
		}
		if (!new File("payloadcache/requireupdate").exists()) {
			if (indexChange)
				new File("payloadcache/requireupdate").createNewFile();
		}
		if (indexChange)
			Files.writeString(Path.of("payloadcache/lastactive.json"), index.toString());

		// Re-apply if needed
		if (new File("payloadcache/requireupdate").exists() || System.getProperty("debugPayloadFiles") != null
				|| System.getProperty("debugPayloadHintClasses") != null) {
			// Create directory
			LauncherUtils.deleteDir(new File("payloadcache/payloaddata"));
			new File("payloadcache/payloaddata").mkdirs();

			// Prepare
			LauncherUtils.resetProgressBar();
			LauncherUtils.showProgressPanel();
			LauncherUtils.log("Gathering payload files...", true);

			// Index
			int count = 0;
			for (String id : payloadLoadOrder) {
				count += indexDir(new File(payloads.get(id).payloadExtractedDir, "server"));
				count += indexDir(new File(payloads.get(id).payloadExtractedDir, "rootdata"));
				count += indexDir(new File(payloads.get(id).payloadExtractedDir, "assetmodifications"));
				for (File clientDir : new File(".")
						.listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
					String clientVersion = clientDir.getName().substring("client-".length());
					count += indexDir(new File(payloads.get(id).payloadExtractedDir, "clientmodifications"));
					count += indexDir(
							new File(payloads.get(id).payloadExtractedDir, "clientmodifications-" + clientVersion));
				}
			}
			LauncherUtils.setProgressMax(count);

			// Copy payloads
			for (String id : payloadLoadOrder) {
				// Copy
				LauncherUtils.log("Gathering payload files: " + payloads.get(id).name);
				copyDirWithProgress(new File(payloads.get(id).payloadExtractedDir, "server"),
						new File("payloadcache/payloaddata", "server"), "", null, null);
				copyDirWithProgress(new File(payloads.get(id).payloadExtractedDir, "rootdata"),
						new File("payloadcache/payloaddata", "rootdata"), "", null, null);
				copyDirWithProgress(new File(payloads.get(id).payloadExtractedDir, "assetmodifications"),
						new File("payloadcache/payloaddata", "assetmodifications"), "", null, null);
				for (File clientDir : new File(".")
						.listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
					String clientVersion = clientDir.getName().substring("client-".length());
					copyDirWithProgress(new File(payloads.get(id).payloadExtractedDir, "clientmodifications"),
							new File("payloadcache/payloaddata", "clientmodifications"), "", null, null);
					copyDirWithProgress(
							new File(payloads.get(id).payloadExtractedDir, "clientmodifications-" + clientVersion),
							new File("payloadcache/payloaddata", "clientmodifications-" + clientVersion), "", null,
							null);
				}
			}

			// Read old index
			LauncherUtils.log("Loading previous payload file list...", true);
			ArrayList<String> lastFiles = new ArrayList<String>();
			ArrayList<String> newIndex = new ArrayList<String>();
			File payloadIndexFile = new File("payloadcache/index.sfl");
			if (payloadIndexFile.exists()) {
				for (String line : Files.readAllLines(payloadIndexFile.toPath())) {
					lastFiles.add(line);
				}
			}

			// Apply
			LauncherUtils.log("Applying payloads...", true);
			count = 0;
			count += indexDir(new File("payloadcache/payloaddata", "server"));
			count += indexDir(new File("payloadcache/payloaddata", "rootdata"));
			count += indexDir(new File("payloadcache/payloaddata", "assetmodifications"));
			for (File clientDir : new File(".").listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
				String clientVersion = clientDir.getName().substring("client-".length());
				count += indexDir(new File("payloadcache/payloaddata", "clientmodifications"));
				count += indexDir(new File("payloadcache/payloaddata", "clientmodifications-" + clientVersion));
			}
			LauncherUtils.resetProgressBar();
			LauncherUtils.setProgressMax(count);
			copyDirWithProgress(new File("payloadcache/payloaddata", "server"), new File("server"), "server/",
					lastFiles, newIndex);
			copyDirWithProgress(new File("payloadcache/payloaddata", "rootdata"), new File("."), "", lastFiles,
					newIndex);
			copyDirWithProgress(new File("payloadcache/payloaddata", "assetmodifications"),
					new File("assetmodifications"), "assetmodifications/", lastFiles, newIndex);
			for (File clientDir : new File(".").listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
				String clientVersion = clientDir.getName().substring("client-".length());
				copyDirWithProgress(new File("payloadcache/payloaddata", "clientmodifications"), clientDir,
						clientDir.getName(), lastFiles, newIndex);
				copyDirWithProgress(new File("payloadcache/payloaddata", "clientmodifications-" + clientVersion),
						clientDir, "", lastFiles, newIndex);
			}

			// Apply list
			String indexStr = "";
			for (String file : newIndex) {
				if (indexStr.isEmpty())
					indexStr = file;
				else
					indexStr += "\n" + file;
			}
			Files.writeString(payloadIndexFile.toPath(), indexStr);

			// Delete unused files
			for (String file : lastFiles) {
				if (new File(file).exists())
					new File(file).delete();
			}

			// Delete marker
			if (new File("payloadcache/requireupdate").exists())
				new File("payloadcache/requireupdate").delete();
			LauncherUtils.resetProgressBar();
			LauncherUtils.hideProgressPanel();
		}
		if (System.getProperty("debugPayloadFiles") != null || System.getProperty("debugPayloadHintClasses") != null)
			new File("payloadcache/requireupdate").createNewFile();

		// Load payload classes
		LauncherUtils.log("Loading payload classes...");
		LauncherUtils.setStatus("Loading payloads..");
		for (String id : payloadLoadOrder) {
			PayloadEntry payload = payloads.get(id);

			// Check type
			if (payload.type == PayloadType.PAYLOAD) {
				LauncherUtils.log("Loading payload: " + id);
				if (!payload.descriptor.containsKey("Payload-Class"))
					throw new IOException("Payload " + id + " does not have a payload class field in its descriptor!");

				// Add source
				LauncherUtils.addUrlToComponentClassLoader(payload.payloadFile.toURI().toURL());

				try {
					// Load class
					Class<?> payloadCls = LauncherUtils.loader.loadClass(payload.descriptor.get("Payload-Class"));
					if (!ISentinelPayload.class.isAssignableFrom(payloadCls))
						throw new IllegalArgumentException(payload.descriptor.get("Payload-Class")
								+ " does not implement " + ISentinelPayload.class.getTypeName());
					Constructor<?> pCt = payloadCls.getConstructor();
					payload.payloadObject = (ISentinelPayload) pCt.newInstance();

					// Init
					LauncherUtils.log("Pre-initializing: " + payload.id);
				} catch (Exception e) {
					if (e instanceof IOException)
						throw (IOException) e;
					else
						throw new IOException("Payload loading error occurred", e);
				}
				if (payload.payloadObject != null) {
					LauncherUtils.log("Pre-initializing: " + payload.id);
					payload.payloadObject.preInit();
				}
			}
		}

		// Initialize payloads
		LauncherUtils.log("Initializing payloads...");
		for (String id : payloadLoadOrder) {
			PayloadEntry payload = payloads.get(id);
			if (payload.payloadObject != null) {
				LauncherUtils.log("Initializing: " + payload.id);
				payload.payloadObject.init();
			}
		}
	}

	static void postInitPayloads() {
		LauncherUtils.log("Post-initializing payloads...");
		for (String id : payloadLoadOrder) {
			PayloadEntry payload = payloads.get(id);
			if (payload.payloadObject != null) {
				LauncherUtils.log("Post-initializing: " + payload.id);
				payload.payloadObject.postInit();
			}
		}
	}

	private static void loadPayloadFile(File spf, File extractedDir, JsonObject index, boolean enabled)
			throws IOException {
		LauncherUtils.log("Loading payload file: " + spf.getPath());

		// Load SPF file descriptor
		Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));

		// Create payload object
		PayloadEntry payload = new PayloadEntry();
		payload.id = spf.getName();
		payload.name = spf.getName();
		payload.version = "default";
		payload.descriptor = descriptor;
		payload.payloadFile = spf;
		payload.payloadExtractedDir = extractedDir;
		payload.enabled = enabled;

		// Check type
		String type = "Full";
		if (descriptor.containsKey("Type"))
			type = descriptor.get("Type");
		type = type.toLowerCase();

		// Check
		if (!type.equals("resource") && !type.equals("full")) {
			// Incompatible
			index.remove(spf.getName());
			return;
		}

		// Set type
		payload.type = type.equals("resource") ? PayloadType.RESOURCE : PayloadType.PAYLOAD;

		// Load settings
		if (descriptor.containsKey("Payload-ID")) {
			payload.id = descriptor.get("Payload-ID");
			payload.name = payload.id;
		}
		if (descriptor.containsKey("Payload-Name"))
			payload.name = descriptor.get("Payload-Name");
		if (descriptor.containsKey("Payload-Version"))
			payload.version = descriptor.get("Payload-Version");

		// Check compatibility
		if (descriptor.containsKey("Game-ID")) {
			// Check
			if (!descriptor.get("Game-ID").equalsIgnoreCase(LauncherUtils.getGameID())) {
				// Incompatible
				index.remove(spf.getName());
				return;
			}
		}
		if (descriptor.containsKey("Software-ID")) {
			// Check
			if (!descriptor.get("Software-ID").equalsIgnoreCase(LauncherUtils.getSoftwareID())) {
				// Incompatible
				index.remove(spf.getName());
				return;
			}
		}

		// Load dependencies
		payload.dependencies = new PayloadDependency[0];
		payload.optionalDependencies = new PayloadDependency[0];
		payload.loadBefore = new String[0];
		payload.conflictsWith = new PayloadDependency[0];
		try {
			JsonObject depsConfig = JsonParser.parseString(getStringFrom(spf, "dependencies.json")).getAsJsonObject();

			// Prepare lists
			ArrayList<PayloadDependency> deps = new ArrayList<PayloadDependency>();
			ArrayList<PayloadDependency> optDeps = new ArrayList<PayloadDependency>();
			ArrayList<String> loadBefore = new ArrayList<String>();
			ArrayList<PayloadDependency> conflicts = new ArrayList<PayloadDependency>();

			// Read from config
			if (depsConfig.has("dependencies")) {
				for (JsonElement ele : depsConfig.get("dependencies").getAsJsonArray()) {
					JsonObject dep = ele.getAsJsonObject();

					// Add
					PayloadDependency dependency = new PayloadDependency();
					dependency.id = dep.get("id").getAsString();
					if (dep.has("version")) {
						dependency.version = dep.get("version").getAsString();
						if (dep.has("versionString"))
							dependency.versionString = dep.get("versionString").getAsString();
						else
							dependency.versionString = dependency.version;
					}
					if (dep.has("url"))
						dependency.url = dep.get("url").getAsString();
					deps.add(dependency);
				}
			}
			if (depsConfig.has("optionalDependencies")) {
				for (JsonElement ele : depsConfig.get("optionalDependencies").getAsJsonArray()) {
					JsonObject dep = ele.getAsJsonObject();

					// Add
					PayloadDependency dependency = new PayloadDependency();
					dependency.id = dep.get("id").getAsString();
					if (dep.has("version")) {
						dependency.version = dep.get("version").getAsString();
						if (dep.has("versionString"))
							dependency.versionString = dep.get("versionString").getAsString();
						else
							dependency.versionString = dependency.version;
					}
					optDeps.add(dependency);
				}
			}
			if (depsConfig.has("conflicts")) {
				for (JsonElement ele : depsConfig.get("conflicts").getAsJsonArray()) {
					JsonObject dep = ele.getAsJsonObject();

					// Add
					PayloadDependency conflict = new PayloadDependency();
					conflict.id = dep.get("id").getAsString();
					if (dep.has("version"))
						conflict.version = dep.get("version").getAsString();
					conflicts.add(conflict);
				}
			}
			if (depsConfig.has("loadBefore")) {
				for (JsonElement ele : depsConfig.get("loadBefore").getAsJsonArray()) {
					loadBefore.add(ele.getAsString());
				}
			}

			// Apply
			payload.dependencies = deps.toArray(t -> new PayloadDependency[t]);
			payload.loadBefore = loadBefore.toArray(t -> new String[t]);
			payload.conflictsWith = conflicts.toArray(t -> new PayloadDependency[t]);
		} catch (IOException e) {
		}

		// Add
		if (payloads.containsKey(payload.id)) {
			// Warn
			JOptionPane.showMessageDialog(null,
					"WARNING! Payload conflict detected!\n\nPayload ID: " + payload.id
							+ "\nTwo or more files provide this payload ID.\n\nFile 1: "
							+ payloads.get(payload.id).payloadFile.getName() + "\nFile 2: "
							+ payload.payloadFile.getName() + "\n\nThe first file will be used.",
					"Warning", JOptionPane.WARNING_MESSAGE);
			index.remove(spf.getName());
			return;
		}
		payloads.put(payload.id, payload);
	}

	private static boolean loadPayload(PayloadEntry payload, ArrayList<String> payloadLoadOrder,
			LinkedHashMap<String, PayloadEntry> payloads) {
		// Check dependencies
		for (PayloadDependency dep : payload.dependencies) {
			// Check dependency status
			if (!payloads.containsKey(dep.id)) {
				JOptionPane.showMessageDialog(null, "There were dependency errors, the payload manager will be opened.",
						"Dependency error", JOptionPane.ERROR_MESSAGE);
				return false;
			} else {
				// Check version if needed
				PayloadEntry dependency = payloads.get(dep.id);
				if (dep.version != null) {
					// Check version
					if (!LauncherUtils.verifyVersionRequirement(dependency.version, dep.version)) {
						JOptionPane.showMessageDialog(null,
								"There were dependency errors, the payload manager will be opened.", "Dependency error",
								JOptionPane.ERROR_MESSAGE);
						return false;
					}
				}

				// Success
				if (!loadPayload(payloads.get(dep.id), payloadLoadOrder, payloads))
					return false; // Error while loading dependency
			}
		}

		// Load optional dependencies
		for (PayloadDependency dep : payload.optionalDependencies) {
			// Check dependency status
			if (payloads.containsKey(dep.id)) {
				// Check version if needed
				PayloadEntry dependency = payloads.get(dep.id);
				if (dep.version != null) {
					// Check version
					if (LauncherUtils.verifyVersionRequirement(dependency.version, dep.version)) {
						// Success
						if (!loadPayload(payloads.get(dep.id), payloadLoadOrder, payloads))
							return false; // Error while loading dependency
					}
				} else {
					// Success
					if (!loadPayload(payloads.get(dep.id), payloadLoadOrder, payloads))
						return false; // Error while loading dependency
				}
			}
		}

		// Check conflicts
		for (PayloadDependency conflict : payload.conflictsWith) {
			if (payloads.containsKey(conflict.id)) {
				// Check
				if (conflict.version == null) {
					// Error
					JOptionPane.showMessageDialog(null,
							"There were conflict errors, the payload manager will be opened.", "Dependency error",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			} else {
				// Check version
				if (!LauncherUtils.verifyVersionRequirement(payloads.get(conflict.id).version, conflict.version)) {
					// Error
					JOptionPane.showMessageDialog(null,
							"There were conflict errors, the payload manager will be opened.", "Dependency error",
							JOptionPane.ERROR_MESSAGE);
					return false;
				}
			}
		}

		// Handle load-before
		for (String id : payloads.keySet()) {
			PayloadEntry p = payloads.get(id);
			if (p.enabled && Stream.of(p.loadBefore).anyMatch(t -> t.equals(payload.id))) {
				// Add
				if (!payloadLoadOrder.contains(p.id))
					payloadLoadOrder.add(p.id);
			}
		}

		// Add
		if (!payloadLoadOrder.contains(payload.id))
			payloadLoadOrder.add(payload.id);

		// Return
		return true;
	}

	/**
	 * Attempts to update payload files
	 * 
	 * @throws IOException If an error occurs while updating
	 */
	public static void checkForUpdates() throws IOException {
		// Log
		LauncherUtils.log("Checking for payload updates...", true);

		// Go through .spf files
		new File("payloads").mkdirs();
		for (File spf : new File("payloads").listFiles(t -> t.isFile() && t.getName().endsWith(".spf"))) {
			// Load descriptor
			Map<String, String> descriptor = LauncherUtils.parseProperties(getStringFrom(spf, "payloadinfo"));
			if (descriptor.containsKey("Update-List-URL") && descriptor.containsKey("Payload-Version")) {
				// Prepare
				String name = spf.getName();
				String id = spf.getName();
				if (descriptor.containsKey("Payload-ID")) {
					name = descriptor.get("Payload-ID");
					id = name;
				}
				if (descriptor.containsKey("Payload-Name"))
					name = descriptor.get("Payload-Name");
				LauncherUtils.log("Checking for updates for " + name + "...", true);
				boolean updatedFile = false;
				LauncherUtils.resetProgressBar();

				// Check updates
				try {
					String lst = null;
					try {
						lst = LauncherUtils.downloadString(descriptor.get("Update-List-URL"));
					} catch (IOException e) {
						LauncherUtils.log("Could not download update list!");
					}
					if (lst != null) {
						JsonObject list = JsonParser.parseString(lst).getAsJsonObject();
						String latest = list.get("latest").getAsString();
						String current = descriptor.get("Payload-Version");

						// Check
						if (!latest.equals(current)) {
							// Update
							LauncherUtils.log("Updating " + name + " to " + latest + "...", true);
							JsonObject versionData = list.get("versions").getAsJsonObject().get(latest)
									.getAsJsonObject();
							String url = versionData.get("url").getAsString();
							LauncherUtils.resetProgressBar();
							LauncherUtils.downloadFile(url, new File("payloads", spf.getName() + ".tmp"));

							// Load hashes
							String remoteHash = versionData.get("hash").getAsString();
							String localHash = LauncherUtils.sha256Hash(
									Files.readAllBytes(new File("payloads", spf.getName() + ".tmp").toPath()));

							// Verify hashes
							boolean hashSuccess = true;
							if (!localHash.equals(remoteHash)) {
								// Redownload
								LauncherUtils.resetProgressBar();
								LauncherUtils.downloadFile(url, new File("payloads", spf.getName() + ".tmp"));

								// Recheck
								localHash = LauncherUtils.sha256Hash(
										Files.readAllBytes(new File("payloads", spf.getName() + ".tmp").toPath()));
								if (!localHash.equals(remoteHash)) {
									// Integrity check failure
									new File("payloads", spf.getName() + ".tmp").delete();
									if (JOptionPane.showConfirmDialog(null,
											"Failed to verify the integrity of the downloaded payload file for '" + name
													+ "', the update will not be applied!\n"
													+ "\nThe launcher will continue with version " + current
													+ ", if you cancel, the launcher will be closed.",
											"Integrity check failure", JOptionPane.OK_CANCEL_OPTION,
											JOptionPane.ERROR_MESSAGE) == JOptionPane.CANCEL_OPTION) {
										System.exit(1);
									}
									hashSuccess = false;
								}
							}

							// Verify signature
							if (hashSuccess) {
								LauncherUtils.log("Verifying signature...", true);
								LauncherUtils.resetProgressBar();
								LauncherUtils.hideProgressPanel();
								if (!LauncherUtils.verifyPackageSignature(new File("payloads", spf.getName() + ".tmp"),
										new File("payloadcache/payloadverificationkeys", id + ".pem"))) {
									// Warn
									while (true) {
										if (JOptionPane.showConfirmDialog(null,
												"WARNING! Failed to verify package signature!\n" //
														+ "\n" //
														+ "Failed to verify the update of the payload file for '" + name
														+ "'!\n" //
														+ "The file that was downloaded may have been tampered with, proceed with caution!\n" //
														+ "\n" //
														+ "It is recommended to contact the developers if possible and ask them if the keys have changed between versions "
														+ current + " and " + latest + ".\n" //
														+ "\n" //
														+ "Do you wish to continue with the update? Selecting no will cancel the update." //
												, "Warning", JOptionPane.YES_NO_OPTION,
												JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
											if (JOptionPane.showConfirmDialog(null,
													"Are you sure you want to ignore the file signature?", "Warning",
													JOptionPane.YES_NO_OPTION,
													JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
												break;
											} else
												continue;
										}
										hashSuccess = false;
										break;
									}
								}
								LauncherUtils.resetProgressBar();
								LauncherUtils.showProgressPanel();
							}

							// Check success
							if (hashSuccess) {
								// Rename old file
								new File("payloads", spf.getName())
										.renameTo(new File("payloads", spf.getName() + ".old"));
								new File("payloads", spf.getName() + ".tmp")
										.renameTo(new File("payloads", spf.getName()));
								updatedFile = true;
								LauncherUtils.log("Updated '" + name + "' to " + latest + "!");
							}
						}
					}
				} catch (Exception e) {
					try {
						if (updatedFile) {
							// Restore
							updatedFile = false;
							new File("payloads", spf.getName()).delete();
							new File("payloads", spf.getName() + ".old").renameTo(new File("payloads", spf.getName()));
						}
						String nameF = name;
						SwingUtilities.invokeAndWait(() -> {
							String stackTrace = "";
							for (StackTraceElement ele : e.getStackTrace())
								stackTrace += "\n     At: " + ele;
							System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] Error occurred: " + e + stackTrace);
							JOptionPane.showMessageDialog(null,
									"An error occured while updating payload '" + nameF + "'.\n\nError details: " + e
											+ stackTrace + "\n\nThe update has been cancelled.",
									"Update Error", JOptionPane.ERROR_MESSAGE);
						});
					} catch (InvocationTargetException | InterruptedException e1) {
					}
				}

				// Check success
				if (updatedFile)
					new File("payloads", spf.getName() + ".old").delete();
			}
		}

		// Hide bars
		LauncherUtils.hideProgressPanel();
		LauncherUtils.resetProgressBar();
	}

	private static String getStringFrom(File file, String entry) throws IOException {
		if (file.isDirectory()) {
			FileInputStream strm = new FileInputStream(new File(file, entry));

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		}

		// Get zip
		ZipFile f = new ZipFile(file);
		try {
			ZipEntry ent = f.getEntry(entry);
			if (ent == null) {
				throw new FileNotFoundException("Entry " + entry + " not found in " + file);
			}

			// Get stream
			InputStream strm = f.getInputStream(ent);

			// Read
			String res = new String(strm.readAllBytes(), "UTF-8");
			strm.close();

			// Return
			return res;
		} finally {
			f.close();
		}
	}

	private static int indexDir(File dir) {
		if (!dir.exists())
			return 0;
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

	private static void copyDirWithProgress(File source, File destination, String prefix, ArrayList<String> lastFiles,
			ArrayList<String> index) throws IOException {
		if (!source.exists())
			return;
		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithProgress(subDir, new File(destination, subDir.getName()), prefix + subDir.getName() + "/",
					lastFiles, index);
			LauncherUtils.increaseProgress();
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			if (lastFiles != null && lastFiles.contains(prefix + file.getName())) {
				lastFiles.remove(prefix + file.getName());
				if (index != null && !index.contains(prefix + file.getName()))
					index.add(prefix + file.getName());
			} else if (index != null && !index.contains(prefix + file.getName())
					&& !new File(destination, file.getName()).exists())
				index.add(prefix + file.getName());
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			LauncherUtils.increaseProgress();
		}
	}

}
