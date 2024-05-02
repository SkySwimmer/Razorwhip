package org.asf.razorwhip.sentinel.launcher.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PackageSignatureUtils {

	/**
	 * Checks if a package is signed
	 * 
	 * @param packageFile Package file
	 * @return True if signed, false otherwise
	 */
	public static boolean isPackageSigned(File packageFile) {
		// Load zip
		try {
			ZipFile zip = new ZipFile(packageFile);
			try {
				boolean signed = zip.getEntry("SENTINEL.PACKAGESIGNATURE.SIG") != null;
				zip.close();
				return signed;
			} finally {
				zip.close();
			}
		} catch (Exception e) {
			return false;
		}
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
	 */
	public static boolean verifyPackageSignature(File packageFile, File publicKeyFile) {
		try {
			// Load zip
			ZipFile zip = new ZipFile(packageFile);
			try {
				ZipEntry sigEntry = zip.getEntry("SENTINEL.PACKAGESIGNATURE.SIG");
				boolean hasSignature = sigEntry != null;
				if (!hasSignature) {
					// Signature missing, public key exists
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
				File zipTemp = File.createTempFile("sentinelsigverif-", "-" + packageFile.getPath() + ".vtp");
				zipTemp.deleteOnExit();
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
					} catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
							| SignatureException e) {
						throw new IOException("Signature verification error", e);
					}
				} finally {
					zipTemp.delete();
				}
			} finally {
				zip.close();
			}
		} catch (Exception e) {
			return false;
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

}
