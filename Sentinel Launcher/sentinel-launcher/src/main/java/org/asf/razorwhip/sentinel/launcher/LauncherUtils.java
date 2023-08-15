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

import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.asf.razorwhip.sentinel.launcher.api.IEmulationSoftwareProvider;
import org.asf.razorwhip.sentinel.launcher.api.IGameDescriptor;

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

	static IGameDescriptor gameDescriptor;
	static IEmulationSoftwareProvider emulationSoftware;

	static void addUrlToComponentClassLoader(URL url) {
		loader.addUrl(url);
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
	 * Increases the progress bar value
	 * 
	 * @param value Progress value to increase with
	 */
	public static void increaseProgress(int value) {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (progressBar.getValue() + value > progressBar.getMaximum())
					progressBar.setValue(progressBar.getMaximum());
				else
					progressBar.setValue(progressBar.getValue() + value);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
	}

	/**
	 * Increases the progress bar value
	 */
	public static void increaseProgress() {
		if (LauncherUtils.progressBar == null)
			return;
		try {
			SwingUtilities.invokeAndWait(() -> {
				if (progressBar.getValue() + 1 > progressBar.getMaximum())
					progressBar.setValue(progressBar.getMaximum());
				else
					progressBar.setValue(progressBar.getValue() + 1);
				panel.repaint();
			});
		} catch (InvocationTargetException | InterruptedException e) {
		}
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
		LauncherUtils.setProgress(0, indexDir(source));
		LauncherUtils.showProgressPanel();

		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDirWithoutProgress(subDir, new File(destination, subDir.getName()));
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
	 * @param packageFile   Package file
	 * @param publicKeyFile Package verification public key target file
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
		LauncherUtils.deleteDir(new File("emulationsoftwaretmp"));
		LauncherUtils.unZip(sourceFile, new File("emulationsoftwaretmp"));

		// Update data
		if (new File("emulationsoftwaretmp", "baseserver").exists()) {
			LauncherUtils.log("Updating server software to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "baseserver"), new File("server"));
		}
		if (new File("emulationsoftwaretmp", "expandedserver").exists()) {
			LauncherUtils.log("Updating extra server files to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "expandedserver"), new File("server"));
		}
		if (new File("emulationsoftwaretmp", "rootdata").exists()) {
			LauncherUtils.log("Updating data to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "rootdata"), new File("."));
		}
		if (new File("emulationsoftwaretmp", "assetmodifications").exists()) {
			LauncherUtils.log("Updating asset modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "assetmodifications"),
					new File("assetmodifications"));
		}
		for (File clientDir : new File(".").listFiles(t -> t.getName().startsWith("client-") && t.isDirectory())) {
			String clientVersion = clientDir.getName().substring("client-".length());
			LauncherUtils.log("Updating " + clientVersion + " client modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "clientmodifications-" + clientVersion),
					clientDir);
		}
		if (new File("emulationsoftwaretmp", "defaultpayloads").exists()) {
			LauncherUtils.log("Updating default payloads to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "defaultpayloads"),
					new File("payloads"));
		}

		// Delete
		LauncherUtils.deleteDir(new File("emulationsoftwaretmp"));
	}

	static void extractGameDescriptor(File sourceFile, String version) throws IOException {
		// Extract
		LauncherUtils.log("Extracting game descriptor package...");
		LauncherUtils.deleteDir(new File("gamedescriptortmp"));
		LauncherUtils.unZip(sourceFile, new File("gamedescriptortmp"));

		// Update data
		if (new File("emulationsoftwaretmp", "defaultpayloads").exists()) {
			LauncherUtils.log("Updating default payloads to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "defaultpayloads"),
					new File("payloads"));
		}
		if (new File("emulationsoftwaretmp", "rootdata").exists()) {
			LauncherUtils.log("Updating data to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "rootdata"), new File("."));
		}
		if (new File("emulationsoftwaretmp", "assetmodifications").exists()) {
			LauncherUtils.log("Updating asset modifications to " + version + "...", true);
			LauncherUtils.copyDirWithProgress(new File("emulationsoftwaretmp", "assetmodifications"),
					new File("assetmodifications"));
		}

		// Delete
		LauncherUtils.deleteDir(new File("gamedescriptortmp"));
	}
}
