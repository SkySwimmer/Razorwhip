package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.IOException;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;

public class VerifySignatureTool {

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 1 || !new File(args[0]).exists()) {
			System.err.println("Usage: sentinel-verify \"<package-file>\"");
			System.exit(1);
			return;
		}
		File input = new File(args[0]);
		File publicKey = new File(input.getParentFile() == null ? new File(".") : input.getParentFile(),
				"publickey.pem");

		// Verify
		System.out.println("Verifying signature...");
		boolean b = LauncherUtils.verifyPackageSignature(input, publicKey);
		if (b) {
			if (!LauncherUtils.isPackageSigned(input))
				System.out.println("Unsigned package.");
			else
				System.out.println("Signature verified.");
		} else {
			System.out.println("Signature invalid.");
			System.exit(1);
		}
	}

}
