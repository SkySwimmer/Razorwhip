package org.asf.razorwhip.sentinel.launcher.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class IndexArchiveTool {

	public static void main(String[] args) throws IOException {
		// Check arguments
		if (args.length < 2 || !new File(args[0]).exists() || !new File(args[0]).isDirectory()) {
			System.err.println("Usage: sentinel-index-archive \"<archive-path>\" \"<output-path>\"");
			System.err.println(
					"Example: sentinel-index-archive \"/examplearchive/assets\" \"/examplearchive/descriptorroot/index\"");
			System.exit(1);
			return;
		}
		new File(args[1]).mkdirs();

		// Index
		indexFolder(new File(args[0]), new File(args[1], "index.json"), "");
		System.out.println("Done.");
	}

	private static void indexFolder(File source, File indexFile, String prefix) throws IOException {
		System.out.println("Indexing /" + prefix);
		JsonObject obj = new JsonObject();
		JsonArray folders = new JsonArray();
		for (File dir : source.listFiles(t -> t.isDirectory())) {
			folders.add(dir.getName());
			File dO = new File(new File(indexFile.getParentFile(), dir.getName()), "index.json");
			dO.getParentFile().mkdirs();
			indexFolder(dir, dO, prefix + dir.getName() + "/");
		}
		obj.add("folders", folders);
		JsonArray files = new JsonArray();
		for (File f : source.listFiles(t -> !t.isDirectory())) {
			files.add(f.getName());
		}
		obj.add("files", files);
		Files.writeString(indexFile.toPath(), new Gson().newBuilder().setPrettyPrinting().create().toJson(obj));
	}
}
