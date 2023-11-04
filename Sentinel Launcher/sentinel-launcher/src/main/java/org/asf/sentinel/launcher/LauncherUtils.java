package org.asf.sentinel.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.swing.JFrame;

import org.asf.sentinel.launcher.api.ObjectTag;

/**
 * 
 * Launcher Utility Class
 * 
 * @author Sky Swimmer
 * 
 */
public class LauncherUtils {

	static String[] args;
	static DynamicClassLoader loader = new DynamicClassLoader();

	static LauncherMain launcherWindow;

	private static HashMap<String, ObjectTag> tags = new HashMap<String, ObjectTag>();

	static void addUrlToComponentClassLoader(URL url) {
		loader.addUrl(url);
	}

	/**
	 * Retrieves the launcher window
	 * 
	 * @return Launcher JFrame instance
	 */
	public static JFrame getLauncherWindow() {
		return launcherWindow.frmSentinelLauncher;
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
	 * Copies folders
	 * 
	 * @param source      Source folder
	 * @param destination Destination folder
	 * @throws IOException If copying fails
	 */
	public static void copyDir(File source, File destination) throws IOException {
		if (!source.exists())
			return;
		destination.mkdirs();
		for (File subDir : source.listFiles(t -> t.isDirectory())) {
			copyDir(subDir, new File(destination, subDir.getName()));
		}
		for (File file : source.listFiles(t -> !t.isDirectory())) {
			Files.copy(file.toPath(), new File(destination, file.getName()).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}
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
	static byte[] pemDecode(String pem) {
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

}
