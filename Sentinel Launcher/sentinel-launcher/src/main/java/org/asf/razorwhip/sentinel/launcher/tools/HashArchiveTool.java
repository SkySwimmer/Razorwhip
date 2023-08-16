package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;

public class HashArchiveTool {

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 2 || !new File(args[0]).exists() || !new File(args[0]).isDirectory()) {
			System.err.println("Usage: sentinel-hash-archive \"<archive-path>\" \"<output-path>\"");
			System.err.println(
					"Example: sentinel-hash-archive \"/examplearchive/assets\" \"/examplearchive/descriptorroot/hashes.shl\"");
			System.exit(1);
			return;
		}
		new File(args[1]).mkdirs();

		// Index
		FileOutputStream fO = new FileOutputStream(new File(args[1]));
		hashFolder(new File(args[0]), fO, "");
		fO.close();
		System.out.println("Done.");
	}

	private static void hashFolder(File source, FileOutputStream outputFile, String prefix) throws IOException {
		System.out.println("Hashing /" + prefix);
		for (File dir : source.listFiles(t -> t.isDirectory())) {
			hashFolder(dir, outputFile, prefix + dir.getName() + "/");
		}
		for (File f : source.listFiles(t -> !t.isDirectory())) {
			System.out.println("Hashing /" + prefix + f.getName());
			String hash = LauncherUtils.sha512Hash(Files.readAllBytes(f.toPath()));
			String name = prefix + f.getName();
			name = name.replace(";", ";sl;").replace(":", ";cl;").replace(" ", ";sp;");
			outputFile.write((name + ": " + hash + "\n").getBytes("UTF-8"));
		}
	}
}
