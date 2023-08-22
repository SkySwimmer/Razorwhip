package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class IndexArchiveTool {

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 2 || !new File(args[0]).exists() || !new File(args[0]).isDirectory()) {
			System.err.println("Usage: sentinel-index-archive \"<archive-path>\" \"<output-path>\"");
			System.err.println(
					"Example: sentinel-index-archive \"/examplearchive/assets\" \"/examplearchive/descriptorroot/index.sfl\"");
			System.exit(1);
			return;
		}
		File p = new File(args[1]).getParentFile();
		if (p != null)
			p.mkdirs();

		// Index
		FileOutputStream fO = new FileOutputStream(new File(args[1]));
		indexFolder(new File(args[0]), fO, "");
		fO.close();
		System.out.println("Done.");
	}

	private static void indexFolder(File source, FileOutputStream outputFile, String prefix) throws IOException {
		System.out.println("Indexing /" + prefix);
		for (File dir : source.listFiles(t -> t.isDirectory())) {
			indexFolder(dir, outputFile, prefix + dir.getName() + "/");
		}
		for (File f : source.listFiles(t -> !t.isDirectory())) {
			String name = prefix + f.getName();
			name = name.replace(";", ";sl;").replace(":", ";cl;").replace(" ", ";sp;");
			outputFile.write((name + ": " + f.length() + "\n").getBytes("UTF-8"));
		}
	}
}
