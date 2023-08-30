package org.asf.razorwhip.sentinel.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.lang.reflect.InvocationTargetException;

import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;
import org.asf.razorwhip.sentinel.launcher.api.ObjectTag;
import org.asf.razorwhip.sentinel.launcher.windows.VersionManagerWindow;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LauncherUtils {

	static String[] args;

	static BackgroundPanel panel;
	static JPanel progressPanel;
	static JProgressBar progressBar;
	static JLabel statusLabel;

	static DynamicClassLoader loader = new DynamicClassLoader();

	static String gameID;
	static String softwareID;
	static String softwareVersion;
	static String softwareName;

	static LauncherMain launcherWindow;

	static IGameDescriptor gameDescriptor;
	static IEmulationSoftwareProvider emulationSoftware;

	public static JsonObject sacConfig;
	public static String urlBaseSoftwareFile;
	public static String urlBaseDescriptorFile;
	public static String assetSourceURL;
	static boolean assetManagementAvailable = false;

	private static HashMap<String, ObjectTag> tags = new HashMap<String, ObjectTag>();

	static void addUrlToComponentClassLoader(URL url) {
		loader.addUrl(url);
	}

	/**
	 * Checks if asset management is available
	 * 
	 * @return True if available, false otherwise
	 */
	public static boolean isAssetManagementAvailable() {
		return assetManagementAvailable;
	}

	/**
	 * Adds object tags
	 * 
	 * @param name Tag name
	 * @return ObjectTag instance
	 */
	public static ObjectTag addTag(String name) {
		ObjectTag tag = getTag(name);
		if (tag != null)
			return tag;
		tag = new ObjectTag();
		tags.put(name.toLowerCase(), tag);
		return tag;
	}

	/**
	 * Retrieves object tags
	 * 
	 * @param name Tag name
	 * @return ObjectTag instance or null
	 */
	public static ObjectTag getTag(String name) {
		return tags.get(name.toLowerCase());
	}

	/**
	 * Removes object tags
	 * 
	 * @param name Tag name
	 * @return ObjectTag instance or null
	 */
	public static ObjectTag removeTag(String name) {
		return tags.remove(name.toLowerCase());
	}

	/**
	 * Checks if a tag is present
	 * 
	 * @param name Tag name
	 * @return True if present, false otherwise
	 */
	public static boolean hasTag(String name) {
		return tags.containsKey(name.toLowerCase());
	}

	/**
	 * Retrieves the program arguments
	 * 
	 * @return Program argument strings
	 */
	public static String[] getProgramArguments() {
		return args;
	}

	/**
	 * Retrieves the active game descriptor
	 * 
	 * @return IGameDescriptor instance
	 */
	public static IGameDescriptor getGameDescriptor() {
		return gameDescriptor;
	}

	/**
	 * Retrieves the game ID
	 * 
	 * @return Game ID string
	 */
	public static String getGameID() {
		return gameID;
	}

	/**
	 * Retrieves the emulation software ID
	 * 
	 * @return Emulation software ID string
	 */
	public static String getSoftwareID() {
		return softwareID;
	}

	/**
	 * Retrieves the emulation software version
	 * 
	 * @return Emulation software version string
	 */
	public static String getSoftwareVersion() {
		return softwareVersion;
	}

	/**
	 * Retrieves the emulation software name
	 * 
	 * @return Emulation software name string
	 */
	public static String getSoftwareName() {
		return softwareName;
	}

	/**
	 * Retrieves the emulation software provider
	 * 
	 * @return IEmulationSoftwareProvider instance
	 */
	public static IEmulationSoftwareProvider getEmulationSoftware() {
		return emulationSoftware;
	}

	/**
	 * Shows the progress panel
	 */
	public static void showProgressPanel() {
		if (LauncherUtils.progressPanel == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressPanel.setVisible(true);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Hides the progress panel
	 */
	public static void hideProgressPanel() {
		if (LauncherUtils.progressPanel == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressPanel.setVisible(false);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Resets the progress bar
	 */
	public static void resetProgressBar() {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(100);
				progressBar.setValue(0);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Assigns the progress bar values
	 * 
	 * @param value Progress value
	 * @param max   Progress max
	 */
	public static void setProgress(int value, int max) {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(max);
				progressBar.setValue(value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Assigns the progress bar value
	 * 
	 * @param value Progress value
	 */
	public static void setProgress(int value) {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setValue(value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Retrieves the progress bar value
	 * 
	 * @return Progress bar value
	 */
	public static int getProgress() {
		return progressBar.getValue();
	}

	/**
	 * Retrieves the max progress bar value
	 * 
	 * @return Progress bar max value
	 */
	public static int getProgressMax() {
		return progressBar.getMaximum();
	}

	/**
	 * Increases the progress bar value
	 * 
	 * @param value Progress value to increase with
	 */
	public static void increaseProgress(int value) {
		if (LauncherUtils.progressBar == null)
			return;
		SwingUtilities.invokeLater(() -> {
			if (progressBar.getValue() + value > progressBar.getMaximum())
				progressBar.setValue(progressBar.getMaximum());
			else
				progressBar.setValue(progressBar.getValue() + value);
			panel.repaint();
		});
	}

	/**
	 * Increases the progress bar value
	 */
	public static void increaseProgress() {
		if (LauncherUtils.progressBar == null)
			return;
		SwingUtilities.invokeLater(() -> {
			if (progressBar.getValue() + 1 > progressBar.getMaximum())
				progressBar.setValue(progressBar.getMaximum());
			else
				progressBar.setValue(progressBar.getValue() + 1);
			panel.repaint();
		});
	}

	/**
	 * Assigns the progress bar max value
	 * 
	 * @param max Progress max value
	 */
	public static void setProgressMax(int max) {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				progressBar.setMaximum(max);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Prints a log message
	 * 
	 * @param message      Message to log
	 * @param statusUpdate True to update the label, false otherwise
	 */
	public static void log(String message, boolean statusUpdate) {
		try {
			if (LauncherUtils.statusLabel != null) {
				SwingUtilities.invokeAndWait(() -> {
					if (statusUpdate) {
						statusLabel.setText(" " + message);
						panel.repaint();
					}
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
		System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] " + message);
	}

	/**
	 * Prints a log message
	 * 
	 * @param message Message to log
	 */
	public static void log(String message) {
		System.out.println("[LAUNCHER] [SENTINEL LAUNCHER] " + message);
	}

	/**
	 * Assigns the status message
	 * 
	 * @param message Message to use
	 */
	public static void setStatus(String message) {
		try {
			if (LauncherUtils.statusLabel != null) {
				SwingUtilities.invokeAndWait(() -> {
					statusLabel.setText(" " + message);
					panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Parses Sentinel property sets
	 * 
	 * @param props Property set to parse
	 * @return Properties map
	 */
	public static Map<String, String> parseProperties(String props) {
		HashMap<String, String> properties = new HashMap<String, String>();
		for (String line : props.replace("\r", "").split("\n")) {
			if (line.isEmpty() || line.startsWith("#") || !line.contains(": "))
				continue;
			String key = line;
			String value = "";
			if (key.contains(": ")) {
				value = key.substring(key.indexOf(": ") + 2);
				key = key.substring(0, key.indexOf(": "));
			}
			properties.put(key, value);
		}
		return properties;
	}

	/**
	 * Downloads a string
	 * 
	 * @param url URL to download from
	 * @return Downloaded string
	 * @throws IOException If downloading fails
	 */
	public static String downloadString(String url) throws IOException {
		URLConnection conn = new URL(url).openConnection();
		InputStream strm = conn.getInputStream();
		String data = new String(strm.readAllBytes(), "UTF-8");
		strm.close();
		return data;
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

	/**
	 * Deletes a directory
	 * 
	 * @param dir Directory to download
	 */
	public static void deleteDir(File dir) {
		if (!dir.exists())
			return;
		for (File subDir : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(subDir);
		}
		for (File file : dir.listFiles(t -> !t.isDirectory())) {
			file.delete();
		}
		dir.delete();
	}

	/**
	 * Copies folders (shows progress)
	 * 
	 * @param source      Source folder
	 * @param destination Destination folder
	 * @throws IOException If copying fails
	 */
	public static void copyDirWithProgress(File source, File destination) throws IOException {
		if (!source.exists())
			return;
		LauncherUtils.setProgress(0, indexDir(source));
		LauncherUtils.showProgressPanel();
		copyDirWithProgressI(source, destination);
	}

	private static void copyDirWithProgressI(File source, File destination) throws IOException {
		if (!source.exists())
			return;
		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithProgressI(subDir, new File(destination, subDir.getName()));
			LauncherUtils.increaseProgress();
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			LauncherUtils.increaseProgress();
		}
	}

	/**
	 * Copies folders
	 * 
	 * @param source      Source folder
	 * @param destination Destination folder
	 * @throws IOException If copying fails
	 */
	public static void copyDirWithoutProgress(File source, File destination) throws IOException {
		if (!source.exists())
			return;
		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithoutProgress(subDir, new File(destination, subDir.getName()));
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Downloads a file (shows progress)
	 * 
	 * @param url  URL to the file to download
	 * @param outp Output file
	 * @throws IOException If downloading fails
	 */
	public static void downloadFile(String url, File outp) throws IOException {
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();
		URLConnection urlConnection = new URL(url).openConnection();
		try {
			int l = urlConnection.getContentLength();
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					LauncherUtils.progressBar.setMaximum(l < 1000 ? 1 : l / 1000);
					LauncherUtils.progressBar.setValue(0);
					LauncherUtils.panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
		InputStream data = urlConnection.getInputStream();
		FileOutputStream out = new FileOutputStream(outp);
		while (true) {
			byte[] b = data.readNBytes(1000);
			if (b.length == 0)
				break;
			else {
				out.write(b);
				if (progressBar != null) {
					SwingUtilities.invokeLater(() -> {
						LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getValue() + 1);
						LauncherUtils.panel.repaint();
					});
				}
			}
		}
		out.close();
		data.close();
		try {
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getMaximum());
					LauncherUtils.panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Unzips a zip file (shows progress)
	 * 
	 * @param input  File to unzip
	 * @param output Folder to unzip into
	 * @throws IOException If unzipping fails
	 */
	public static void unZip(File input, File output) throws IOException {
		LauncherUtils.resetProgressBar();
		LauncherUtils.showProgressPanel();
		output.mkdirs();

		// count entries
		ZipFile archive = new ZipFile(input);
		int count = 0;
		Enumeration<? extends ZipEntry> en = archive.entries();
		while (en.hasMoreElements()) {
			en.nextElement();
			count++;
		}
		archive.close();

		// prepare and log
		archive = new ZipFile(input);
		en = archive.entries();
		try {
			int fcount = count;
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(fcount);
					progressBar.setValue(0);
					panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (en.hasMoreElements()) {
			ZipEntry ent = en.nextElement();
			if (ent == null)
				break;

			if (ent.isDirectory()) {
				new File(output, ent.getName()).mkdirs();
			} else {
				File out = new File(output, ent.getName());
				if (out.getParentFile() != null && !out.getParentFile().exists())
					out.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(out);
				InputStream is = archive.getInputStream(ent);
				is.transferTo(os);
				is.close();
				os.close();
			}

			if (progressBar != null) {
				SwingUtilities.invokeLater(() -> {
					progressBar.setValue(progressBar.getValue() + 1);
					panel.repaint();
				});
			}
		}

		// finish progress
		try {
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getMaximum());
					LauncherUtils.panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
		archive.close();
	}

	/**
	 * Extracts tar.gz files
	 * 
	 * @param input  File to extract
	 * @param output Folder to extract into
	 * @throws IOException If extracting fails
	 */
	public static void unTarGz(File input, File output) throws IOException {
		output.mkdirs();

		// count entries
		InputStream file = new FileInputStream(input);
		GZIPInputStream gzip = new GZIPInputStream(file);
		TarArchiveInputStream tar = new TarArchiveInputStream(gzip);
		int count = 0;
		while (tar.getNextEntry() != null) {
			count++;
		}
		tar.close();
		gzip.close();
		file.close();

		// prepare and log
		file = new FileInputStream(input);
		gzip = new GZIPInputStream(file);
		tar = new TarArchiveInputStream(gzip);
		try {
			int fcount = count;
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					progressBar.setMaximum(fcount);
					progressBar.setValue(0);
					panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}

		// extract
		while (true) {
			ArchiveEntry ent = tar.getNextEntry();
			if (ent == null)
				break;

			if (ent.isDirectory()) {
				new File(output, ent.getName()).mkdirs();
			} else {
				File out = new File(output, ent.getName());
				if (out.getParentFile() != null && !out.getParentFile().exists())
					out.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(out);
				InputStream is = tar;
				is.transferTo(os);
				os.close();
			}

			if (progressBar != null) {
				SwingUtilities.invokeLater(() -> {
					progressBar.setValue(progressBar.getValue() + 1);
					panel.repaint();
				});
			}
		}

		// finish progress
		try {
			if (progressBar != null) {
				SwingUtilities.invokeAndWait(() -> {
					LauncherUtils.progressBar.setValue(LauncherUtils.progressBar.getMaximum());
					LauncherUtils.panel.repaint();
				});
			}
		} catch (InvocationTargetException | InterruptedException e) {
		}
		tar.close();
		gzip.close();
		file.close();
	}

	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

	/**
	 * Converts a byte array to a HEX string
	 * 
	 * @param bytes Byte array
	 * @return Hex string
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_ARRAY[v >>> 4];
			hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Creates a sha-256 hash
	 * 
	 * @param data Data to hash
	 * @return Hash string
	 */
	public static String sha256Hash(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(data);
			return bytesToHex(hash).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a sha-512 hash
	 * 
	 * @param data Data to hash
	 * @return Hash string
	 */
	public static String sha512Hash(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			byte[] hash = digest.digest(data);
			return bytesToHex(hash).toLowerCase();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Shows the client version manager
	 * 
	 * @param firstTime True if this is the first time the window is displayed,
	 *                  false otherwise, should be false unless called internally by
	 *                  the launcher itself
	 * @return True if saved, false if cancelled
	 * @throws IOException If an error occurs loading the window
	 */
	public static boolean showVersionManager(boolean firstTime) throws IOException {
		VersionManagerWindow window = new VersionManagerWindow(launcherWindow.frmSentinelLauncher, firstTime);
		boolean saved = window.showDialog();

		// Check changes
		if (saved) {
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
						deleteDir(new File("clients/client-" + version));
					} else
						newHashList.add(version, localHashList.get(version));
				}
				Files.writeString(localVersions.toPath(), newHashList.toString());
			}

			// Delete last selected version
			new File("lastclient.json").delete();
		}

		return saved;
	}

	/**
	 * Checks if a package is signed
	 * 
	 * @param packageFile Package file
	 * @return True if signed, false otherwise
	 * @throws IOException If verifying if the package is signed fails
	 */
	public static boolean isPackageSigned(File packageFile) throws IOException {
		// Load zip
		ZipFile zip = new ZipFile(packageFile);
		boolean signed = zip.getEntry("SENTINEL.PACKAGESIGNATURE.SIG") != null;
		zip.close();
		return signed;
	}

	/**
	 * Extracts the public key of a package file
	 * 
	 * @param packageFile         Package file
	 * @param publicKeyOutputFile Package verification public key target file
	 * @throws IOException If extracting fails
	 */
	public static void extractPackagePublicKey(File packageFile, File publicKeyOutputFile) throws IOException {
		// Load zip
		ZipFile zip = new ZipFile(packageFile);
		ZipEntry keyEntry = zip.getEntry("SENTINEL.PACKAGEKEY.PEM");
		if (keyEntry == null) {
			// Key missing
			zip.close();
			throw new IOException("Package does not have a key");
		}

		// Transfer
		InputStream strm = zip.getInputStream(keyEntry);
		if (publicKeyOutputFile.getParentFile() != null)
			publicKeyOutputFile.getParentFile().mkdirs();
		FileOutputStream fOut = new FileOutputStream(publicKeyOutputFile);
		strm.transferTo(fOut);
		fOut.close();
		strm.close();

		// Close
		zip.close();
	}

	/**
	 * Verifies the signature of a package
	 * 
	 * @param packageFile   Package file
	 * @param publicKeyFile Package verification public key
	 * @return True if valid, false if invalid
	 * @throws IOException If verification errors
	 */
	public static boolean verifyPackageSignature(File packageFile, File publicKeyFile) throws IOException {
		// Load zip
		ZipFile zip = new ZipFile(packageFile);
		ZipEntry sigEntry = zip.getEntry("SENTINEL.PACKAGESIGNATURE.SIG");
		boolean hasSignature = sigEntry != null;
		if (!hasSignature) {
			// Signature missing, public key exists
			zip.close();
			return !publicKeyFile.exists();
		}

		// Read signature
		InputStream strm = zip.getInputStream(sigEntry);
		byte[] sig = strm.readAllBytes();
		strm.close();

		// Write key if it doesnt exist
		if (!publicKeyFile.exists()) {
			ZipEntry keyEntry = zip.getEntry("SENTINEL.PACKAGEKEY.PEM");
			if (keyEntry == null) {
				// Key missing
				zip.close();
				return false;
			}

			// Transfer
			strm = zip.getInputStream(keyEntry);
			if (publicKeyFile.getParentFile() != null)
				publicKeyFile.getParentFile().mkdirs();
			FileOutputStream fOut = new FileOutputStream(publicKeyFile);
			strm.transferTo(fOut);
			fOut.close();
			strm.close();
		}

		// Read key
		byte[] key = pemDecode(Files.readString(publicKeyFile.toPath()));

		// Copy zip
		File zipTemp = new File(packageFile.getPath() + ".vtp");
		try {
			// Transfer package without signature
			FileOutputStream fO = new FileOutputStream(zipTemp);
			ZipOutputStream zO = new ZipOutputStream(fO);
			try {
				// Load old
				FileInputStream fIn = new FileInputStream(packageFile);
				ZipInputStream zIn = new ZipInputStream(fIn);
				try {
					// Transfer
					ZipEntry entry = zIn.getNextEntry();
					while (entry != null) {
						// Check name
						if (entry.getName().equals("SENTINEL.PACKAGESIGNATURE.SIG")) {
							// Get next
							entry = zIn.getNextEntry();
							continue;
						}

						// Put entry
						zO.putNextEntry(entry);
						if (!entry.getName().replace("\\", "/").endsWith("/")) {
							// Transfer data
							zIn.transferTo(zO);
						}
						zO.closeEntry();

						// Get next
						entry = zIn.getNextEntry();
					}

					// Close zip
					zip.close();
				} finally {
					zIn.close();
					fIn.close();
				}
			} finally {
				zO.close();
				fO.close();
			}

			// Verfiy
			try {
				// Load key
				KeyFactory fac = KeyFactory.getInstance("RSA");
				PublicKey publicKey = fac.generatePublic(new X509EncodedKeySpec(key));

				// Init verification
				Signature s = Signature.getInstance("Sha512WithRSA");
				s.initVerify(publicKey);

				// Update
				FileInputStream fIn = new FileInputStream(zipTemp);
				while (true) {
					byte[] data = new byte[20480];
					int i = fIn.read(data);
					if (i <= 0)
						break;
					s.update(data, 0, i);
				}
				fIn.close();

				// Verify
				return s.verify(sig);
			} catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | SignatureException e) {
				throw new IOException("Signature verification error", e);
			}

		} finally {
			zipTemp.delete();
		}
	}

	/**
	 * Verifies version requirements
	 * 
	 * @param version      Version string
	 * @param versionCheck Version check string
	 * @return True if valid, false otherwise
	 */
	public static boolean verifyVersionRequirement(String version, String versionCheck) {
		for (String filter : versionCheck.split("\\|\\|")) {
			filter = filter.trim();
			if (verifyVersionRequirementPart(version, filter))
				return true;
		}
		return false;
	}

	private static boolean verifyVersionRequirementPart(String version, String versionCheck) {
		// Handle versions
		for (String filter : versionCheck.split("&")) {
			filter = filter.trim();

			// Verify filter string
			if (filter.startsWith("!=")) {
				// Not equal
				if (version.equals(filter.substring(2)))
					return false;
			} else if (filter.startsWith("==")) {
				// Equal to
				if (!version.equals(filter.substring(2)))
					return false;
			} else if (filter.startsWith(">=")) {
				int[] valuesVersionCurrent = parseVersionValues(version);
				int[] valuesVersionCheck = parseVersionValues(filter.substring(2));

				// Handle each
				for (int i = 0; i < valuesVersionCheck.length; i++) {
					int val = valuesVersionCheck[i];

					// Verify lengths
					if (i > valuesVersionCurrent.length)
						break;

					// Verify value
					int i2 = 0;
					if (i < valuesVersionCurrent.length)
						i2 = valuesVersionCurrent[i];
					if (i2 < val)
						return false;
				}
			} else if (filter.startsWith("<=")) {
				int[] valuesVersionCurrent = parseVersionValues(version);
				int[] valuesVersionCheck = parseVersionValues(filter.substring(2));

				// Handle each
				for (int i = 0; i < valuesVersionCheck.length; i++) {
					int val = valuesVersionCheck[i];

					// Verify lengths
					if (i > valuesVersionCurrent.length)
						break;

					// Verify value
					int i2 = 0;
					if (i < valuesVersionCurrent.length)
						i2 = valuesVersionCurrent[i];
					if (i2 > val)
						return false;
				}
			} else if (filter.startsWith(">")) {
				int[] valuesVersionCurrent = parseVersionValues(version);
				int[] valuesVersionCheck = parseVersionValues(filter.substring(1));

				// Handle each
				for (int i = 0; i < valuesVersionCheck.length; i++) {
					int val = valuesVersionCheck[i];

					// Verify lengths
					if (i > valuesVersionCurrent.length)
						break;

					// Verify value
					int i2 = 0;
					if (i < valuesVersionCurrent.length)
						i2 = valuesVersionCurrent[i];
					if (i2 <= val)
						return false;
				}
			} else if (filter.startsWith("<")) {
				int[] valuesVersionCurrent = parseVersionValues(version);
				int[] valuesVersionCheck = parseVersionValues(filter.substring(1));

				// Handle each
				for (int i = 0; i < valuesVersionCheck.length; i++) {
					int val = valuesVersionCheck[i];

					// Verify lengths
					if (i > valuesVersionCurrent.length)
						break;

					// Verify value
					int i2 = 0;
					if (i < valuesVersionCurrent.length)
						i2 = valuesVersionCurrent[i];
					if (i2 >= val)
						return false;
				}
			} else {
				// Equal to
				if (!version.equals(filter))
					return false;
			}
		}

		// Valid
		return true;
	}

	private static int[] parseVersionValues(String version) {
		ArrayList<Integer> values = new ArrayList<Integer>();

		// Parse version string
		String buffer = "";
		for (char ch : version.toCharArray()) {
			if (ch == '-' || ch == '.') {
				// Handle segment
				if (!buffer.isEmpty()) {
					// Check if its a number
					if (buffer.matches("^[0-9]+$")) {
						// Add value
						try {
							values.add(Integer.parseInt(buffer));
						} catch (Exception e) {
							// ... okay... add first char value instead
							values.add((int) buffer.charAt(0));
						}
					} else {
						// Check if its a full word and doesnt contain numbers
						if (buffer.matches("^[^0-9]+$")) {
							// It is, add first char value
							values.add((int) buffer.charAt(0));
						} else {
							// Add each value
							for (char ch2 : buffer.toCharArray())
								values.add((int) ch2);
						}
					}
				}
				buffer = "";
			} else {
				// Add to segment buffer
				buffer += ch;
			}
		}
		if (!buffer.isEmpty()) {
			// Check if its a number
			if (buffer.matches("^[0-9]+$")) {
				// Add value
				try {
					values.add(Integer.parseInt(buffer));
				} catch (Exception e) {
					// ... okay... add first char value instead
					values.add((int) buffer.charAt(0));
				}
			} else {
				// Check if its a full word and doesnt contain numbers
				if (buffer.matches("^[^0-9]+$")) {
					// It is, add first char value
					values.add((int) buffer.charAt(0));
				} else {
					// Add each value
					for (char ch : buffer.toCharArray())
						values.add((int) ch);
				}
			}
		}

		int[] arr = new int[values.size()];
		for (int i = 0; i < arr.length; i++)
			arr[i] = values.get(i);
		return arr;
	}

	// PEM parser
	private static byte[] pemDecode(String pem) {
		String base64 = pem.replace("\r", "");

		// Strip header
		while (base64.startsWith("-"))
			base64 = base64.substring(1);
		while (!base64.startsWith("-"))
			base64 = base64.substring(1);
		while (base64.startsWith("-"))
			base64 = base64.substring(1);

		// Clean data
		base64 = base64.replace("\n", "");

		// Strip footer
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (!base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);
		while (base64.endsWith("-"))
			base64 = base64.substring(0, base64.length() - 1);

		// Decode and return
		return Base64.getDecoder().decode(base64);
	}

	static void extractEmulationSoftware(File sourceFile, String version) throws IOException {
		// Extract
		LauncherUtils.log("Extracting software package...");
		LauncherUtils.deleteDir(new File("cache/emulationsoftwaretmp"));
		LauncherUtils.unZip(sourceFile, new File("cache/emulationsoftwaretmp"));

		// Update data
		if (new File("cache/emulationsoftwaretmp", "baseserver").exists()) {
			LauncherUtils.log("Updating server software to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/emulationsoftwaretmp", "baseserver"), new File("server"));
		}
		if (new File("cache/emulationsoftwaretmp", "expandedserver").exists()) {
			LauncherUtils.log("Updating extra server files to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/emulationsoftwaretmp", "expandedserver"),
					new File("server"));
		}
		if (new File("cache/emulationsoftwaretmp", "rootdata").exists()) {
			LauncherUtils.log("Updating data to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/emulationsoftwaretmp", "rootdata"), new File("."));
		}
		if (new File("emulationsoftwaretmp", "assetmodifications").exists()) {
			LauncherUtils.log("Updating asset modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/emulationsoftwaretmp", "assetmodifications"),
					new File("assetmodifications"));
		}
		for (File clientDir : new File("clients")
				.listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
			String clientVersion = clientDir.getName().substring("client-".length());
			LauncherUtils.log("Updating " + clientVersion + " client modifications to " + version + "...", true);

			// Check modifications
			File modsSpecific = new File("cache/emulationsoftwaretmp", "clientmodifications-" + clientVersion);
			File modsGeneral = new File("cache/emulationsoftwaretmp", "clientmodifications");
			if (modsSpecific.exists() || modsGeneral.exists()) {
				// Close clients
				LauncherMain.closeClientsIfNeeded();

				// Update
				LauncherUtils.copyDirWithProgress(modsGeneral, clientDir);
				LauncherUtils.copyDirWithProgress(modsSpecific, clientDir);
			}
		}
		if (new File("cache/emulationsoftwaretmp", "defaultpayloads").exists()) {
			LauncherUtils.log("Updating default payloads to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/emulationsoftwaretmp", "defaultpayloads"),
					new File("payloads"));
		}

		// Delete
		LauncherUtils.deleteDir(new File("cache/emulationsoftwaretmp"));
	}

	static void extractGameDescriptor(File sourceFile, String version) throws IOException {
		// Extract
		LauncherUtils.log("Extracting game descriptor package...");
		LauncherUtils.deleteDir(new File("cache/gamedescriptortmp"));
		LauncherUtils.unZip(sourceFile, new File("cache/gamedescriptortmp"));

		// Update data
		if (new File("cache/gamedescriptortmp", "defaultpayloads").exists()) {
			LauncherUtils.log("Updating default payloads to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/gamedescriptortmp", "defaultpayloads"),
					new File("payloads"));
		}
		if (new File("cache/gamedescriptortmp", "rootdata").exists()) {
			LauncherUtils.log("Updating data to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/gamedescriptortmp", "rootdata"), new File("."));
		}
		if (new File("cache/gamedescriptortmp", "assetmodifications").exists()) {
			LauncherUtils.log("Updating asset modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("cache/gamedescriptortmp", "assetmodifications"),
					new File("assetmodifications"));
		}
		for (File clientDir : new File("clients")
				.listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
			String clientVersion = clientDir.getName().substring("client-".length());
			LauncherUtils.log("Updating " + clientVersion + " client modifications to " + version + "...", true);

			// Check modifications
			File modsSpecific = new File("cache/gamedescriptortmp", "clientmodifications-" + clientVersion);
			File modsGeneral = new File("cache/gamedescriptortmp", "clientmodifications");
			if (modsSpecific.exists() || modsGeneral.exists()) {
				// Close clients
				LauncherMain.closeClientsIfNeeded();

				// Update
				LauncherUtils.copyDirWithProgress(modsGeneral, clientDir);
				LauncherUtils.copyDirWithProgress(modsSpecific, clientDir);
			}
		}

		// Delete
		LauncherUtils.deleteDir(new File("cache/gamedescriptortmp"));
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
		JsonObject archiveLst = JsonParser.parseString(Files.readString(new File("assets/assetarchives.json").toPath()))
				.getAsJsonObject();
		JsonObject archiveDef = archiveLst.get(id).getAsJsonObject();
		JsonArray clientsArr = new JsonArray();
		File clientListFile = new File("assets/clients.json");
		if (clientListFile.exists()) {
			clientsArr = JsonParser.parseString(Files.readString(Path.of("assets/clients.json"))).getAsJsonArray();
		}

		// Create list
		ArrayList<String> clients = new ArrayList<String>();
		for (String clientVersion : archiveDef.get("clients").getAsJsonObject().keySet()) {
			// Check if present
			boolean found = false;
			for (JsonElement ele : clientsArr) {
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
}
