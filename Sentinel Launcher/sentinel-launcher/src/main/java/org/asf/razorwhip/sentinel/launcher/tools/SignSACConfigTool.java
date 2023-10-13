package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SignSACConfigTool {

	public static void main(String[] args) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
			InvalidKeyException, SignatureException {
		// Check arguments
		if (args.length < 1 || !new File(args[0]).exists()) {
			System.err.println("Usage: sentinel-sign-sacconfig \"<config-file>\"");
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

		// Init verification
		Signature s = Signature.getInstance("Sha512WithRSA");
		s.initSign(privKey);

		// Sign
		String data = Files.readString(input.toPath()).replace("\r", "");
		s.update(data.getBytes("UTF-8"));

		// Sign
		byte[] sig = s.sign();

		// Write
		Files.write(new File(args[0] + ".sig").toPath(), sig);
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
