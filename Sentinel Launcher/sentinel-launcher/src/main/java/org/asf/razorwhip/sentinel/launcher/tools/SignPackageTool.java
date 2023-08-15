package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;

public class SignPackageTool {

	public static void main(String[] args) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			InvalidKeyException, SignatureException {
		// Check arguments
		if (args.length < 1 || !new File(args[0]).exists()) {
			System.err.println("Usage: sentinel-sign \"<package-file>\"");
			System.exit(1);
			return;
		}

		// Verify zip
		try {
			// Load
			ZipFile zip = new ZipFile(args[0]);
			if (zip.getEntry("SENTINEL.PACKAGEKEY.PEM") != null) {
				System.err.println(
						"Error: the file you supplied already contains a signing key, please make sure the package is unsigned");
				System.exit(1);
				zip.close();
				return;
			}
			if (zip.getEntry("SENTINEL.PACKAGESIGNATURE.SIG") != null) {
				System.err.println(
						"Error: the file you supplied already contains a signature, please make sure the package is unsigned");
				System.exit(1);
				zip.close();
				return;
			}
			zip.close();
		} catch (Exception e) {
			System.err.println("Error: the file you supplied is not a valid package file");
			System.exit(1);
			return;
		}

		// Load or generate keys
		File input = new File(args[0]);
		File publicKey = new File(input.getParentFile() == null ? new File(".") : input.getParentFile(),
				"publickey.pem");
		File privateKey = new File(input.getParentFile() == null ? new File(".") : input.getParentFile(),
				"privatekey.pem");
		if (!publicKey.exists() || !privateKey.exists()) {
			// Generate new keys
			KeyPair pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

			// Save keys
			Files.writeString(publicKey.toPath(), pemEncode(pair.getPublic().getEncoded(), "PUBLIC"));
			Files.writeString(privateKey.toPath(), pemEncode(pair.getPrivate().getEncoded(), "PRIVATE"));
		}

		// Load keys
		KeyFactory fac = KeyFactory.getInstance("RSA");
		PrivateKey privKey = fac
				.generatePrivate(new PKCS8EncodedKeySpec(pemDecode(Files.readString(privateKey.toPath()))));
		PublicKey pubKey = fac.generatePublic(new X509EncodedKeySpec(pemDecode(Files.readString(publicKey.toPath()))));
		String pubKeyPem = pemEncode(pubKey.getEncoded(), "PUBLIC");

		// Create output
		String name = input.getAbsolutePath();
		String extension = "";
		if (name.contains(".")) {
			extension = name.substring(name.lastIndexOf("."));
			name = name.substring(0, name.lastIndexOf("."));
		}
		FileOutputStream fO = new FileOutputStream(new File(name + "-signed" + extension));
		ZipOutputStream zO = new ZipOutputStream(fO);

		// Load old
		FileInputStream fIn = new FileInputStream(input);
		ZipInputStream zIn = new ZipInputStream(fIn);
		try {
			// Transfer
			System.out.println("Copying archive...");
			ZipEntry entry = zIn.getNextEntry();
			while (entry != null) {
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

			// Put key
			ZipEntry keyEntry = new ZipEntry("SENTINEL.PACKAGEKEY.PEM");
			zO.putNextEntry(keyEntry);
			zO.write(pubKeyPem.getBytes("UTF-8"));
			zO.closeEntry();

			// Close
			zO.close();
			fO.close();

			// Init verification
			Signature s = Signature.getInstance("Sha512WithRSA");
			s.initSign(privKey);

			// Prepare signature
			System.out.println("Signing archive...");
			FileInputStream fIn2 = new FileInputStream(new File(name + "-signed" + extension));
			while (true) {
				byte[] data = new byte[20480];
				int i = fIn2.read(data);
				if (i <= 0)
					break;
				s.update(data, 0, i);
			}
			fIn2.close();

			// Sign
			byte[] sig = s.sign();

			// Re-transfer entries
			System.out.println("Modifying archive...");
			System.out.println("Copying archive entries...");
			zIn.close();
			fIn.close();
			fIn = new FileInputStream(input);
			zIn = new ZipInputStream(fIn);
			fO = new FileOutputStream(new File(name + "-signed" + extension));
			zO = new ZipOutputStream(fO);
			entry = zIn.getNextEntry();
			while (entry != null) {
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

			// Put key
			System.out.println("Adding key to archive...");
			zO.putNextEntry(keyEntry);
			zO.write(pubKeyPem.getBytes("UTF-8"));
			zO.closeEntry();

			// Write signature
			System.out.println("Adding signature to archive...");
			ZipEntry sigEntry = new ZipEntry("SENTINEL.PACKAGESIGNATURE.SIG");
			zO.putNextEntry(sigEntry);
			zO.write(sig);
			zO.closeEntry();

			// Close
			zO.close();
			fO.close();
			System.out.println("Signature: " + LauncherUtils.bytesToHex(sig));
		} finally {
			zIn.close();
			fIn.close();
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

	// PEM emitter
	private static String pemEncode(byte[] key, String type) {
		// Generate header
		String PEM = "-----BEGIN " + type + " KEY-----";

		// Generate payload
		String base64 = new String(Base64.getEncoder().encode(key));

		// Generate PEM
		while (true) {
			PEM += "\n";
			boolean done = false;
			for (int i = 0; i < 64; i++) {
				if (base64.isEmpty()) {
					done = true;
					break;
				}
				PEM += base64.substring(0, 1);
				base64 = base64.substring(1);
			}
			if (base64.isEmpty())
				break;
			if (done)
				break;
		}

		// Append footer
		PEM += "\n";
		PEM += "-----END " + type + " KEY-----";

		// Return PEM data
		return PEM;
	}

}
