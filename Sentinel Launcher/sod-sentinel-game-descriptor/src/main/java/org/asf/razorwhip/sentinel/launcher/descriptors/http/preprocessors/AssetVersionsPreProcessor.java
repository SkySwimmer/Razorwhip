package org.asf.razorwhip.sentinel.launcher.descriptors.http.preprocessors;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.io.ByteArrayInputStream;
import java.io.File;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.razorwhip.sentinel.launcher.descriptors.http.ContentServerRequestHandler.IPreProcessor;
import org.asf.razorwhip.sentinel.launcher.descriptors.xmls.AssetVersionManifestData;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

public class AssetVersionsPreProcessor implements IPreProcessor {

	private File assetModifications;

	public AssetVersionsPreProcessor(File assetModifications) {
		this.assetModifications = assetModifications;
	}

	@Override
	public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
			HttpResponse response, File sourceDir) {
		return path.toLowerCase().endsWith("/assetversionsdo.xml");
	}

	@Override
	public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
			HttpRequest request, HttpResponse response, InputStream source, File sourceDir) throws IOException {
		// Read manifest
		byte[] manifestB = source.readAllBytes();
		source.close();

		// Decode
		String manifest = new String(manifestB, "UTF-8");

		// Parse
		String[] pathParts = path.split("/");
		if (pathParts.length >= 4) {
			String quality = pathParts[1];
			String plat = pathParts[2];
			String version = pathParts[3];

			// Get data folder path
			String dataRootFolder = path.substring(("/DWADragonsUnity/" + plat + "/" + version + "/").length());
			dataRootFolder = dataRootFolder.substring(0,
					dataRootFolder.toLowerCase().lastIndexOf("/assetversionsdo.xml"));
			dataRootFolder = dataRootFolder.substring(0, dataRootFolder.lastIndexOf("/"));

			// Discover mod assets
			HashMap<String, HashMap<String, File>> assetOverrides = new HashMap<String, HashMap<String, File>>();

			// Discover assets from structured assets
			// Unspecified version, platform, locale and quality
			discoverAssetsIn(new File(assetModifications, "contentoverrides"), assetOverrides, dataRootFolder, "",
					null);

			// Unspecified platform, locale and quality, specified version
			discoverAssetsIn(new File(new File(assetModifications, "contentoverrides"), version), assetOverrides,
					dataRootFolder, "", null);

			// Unspecified locale and quality, specified version and platform
			discoverAssetsIn(new File(new File(assetModifications, "contentoverrides"), plat + "/" + version),
					assetOverrides, dataRootFolder, "", null);

			// Unspecified version, platform and locale, specified quality
			discoverAssetsIn(new File(new File(assetModifications, "contentoverrides"), quality), assetOverrides,
					dataRootFolder, "", null);

			// Unspecified platform and locale, specified version and quality
			discoverAssetsIn(new File(new File(assetModifications, "contentoverrides"), version + "/" + quality),
					assetOverrides, dataRootFolder, "", null);

			// Unspecified locale, specified version, platform and quality
			discoverAssetsIn(
					new File(new File(assetModifications, "contentoverrides"), plat + "/" + version + "/" + quality),
					assetOverrides, dataRootFolder, "", null);

			// Find locales
			for (File dir : new File(assetModifications, "contentoverrides").listFiles(t -> t.isDirectory())) {
				// Unspecified version, platform and quality, specified locale
				discoverAssetsIn(dir, assetOverrides, dataRootFolder, "", dir.getName());

				// Unspecified platform and quality, specified version and locale
				discoverAssetsIn(new File(dir, version), assetOverrides, dataRootFolder, "", dir.getName());

				// Unspecified quality, specified version, locale and platform
				discoverAssetsIn(new File(dir, plat + "/" + version), assetOverrides, dataRootFolder, "",
						dir.getName());

				// Unspecified platform, specified version, locale and quality
				discoverAssetsIn(new File(dir, version + "/" + quality), assetOverrides, dataRootFolder, "",
						dir.getName());

				// Specified version, locale, platform and quality
				discoverAssetsIn(new File(dir, plat + "/" + version + "/" + quality), assetOverrides, dataRootFolder,
						"", dir.getName());
			}

			// Discover assets from server overrides
			discoverServerOverrides(assetOverrides,
					new File(assetModifications,
							"/serveroverrides/DWADragonsUnity/" + plat + "/" + version + "/" + dataRootFolder + "/"),
					dataRootFolder, "", null);

			// Load manifest
			XmlMapper mapper = new XmlMapper();
			mapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
			AssetVersionManifestData assetData = mapper.readValue(manifest, AssetVersionManifestData.class);
			if (assetData.assets != null) {
				// Modern
				ArrayList<AssetVersionManifestData.AssetVersionBlock> assets = new ArrayList<AssetVersionManifestData.AssetVersionBlock>(
						List.of(assetData.assets));

				// Go through assets
				for (AssetVersionManifestData.AssetVersionBlock block : assets) {
					// Check
					if (assetOverrides.containsKey(block.name)) {
						// Get overrides
						HashMap<String, File> overrides = assetOverrides.get(block.name);
						ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock> variants = new ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock>(
								Arrays.asList(block.variants));

						// Go through variants
						for (AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant : variants) {
							// Find locale
							String loc = "en-us";
							if (variant.locale != null)
								loc = variant.locale;

							// Find override
							String locF = loc;
							Optional<String> key = overrides.keySet().stream().filter(t -> t.equalsIgnoreCase(locF))
									.findFirst();
							if (key.isPresent()) {
								File f = overrides.get(key.get());
								int newV = idHash(Files.readAllBytes(f.toPath()));
								if (newV == variant.version)
									newV++;
								variant.version = newV;
								variant.size = f.length();
								overrides.remove(key.get());
							}
						}

						// Add remaining variants
						for (String locale : overrides.keySet()) {
							if (variants.size() > 0 && variants.get(0).locale == null)
								variants.get(0).locale = "en-us";
							File f = overrides.get(locale);
							locale = locale.toLowerCase();

							// Create variant
							AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant = new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock();
							variant.locale = locale;
							variant.size = f.length();
							variant.version = idHash(Files.readAllBytes(f.toPath()));
							variants.add(variant);
						}

						// Remove override
						assetOverrides.remove(block.name);

						// Apply
						block.variants = variants
								.toArray(t -> new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock[t]);
					} else if (assetOverrides.containsKey(block.name.toLowerCase())) {
						// Get overrides
						HashMap<String, File> overrides = assetOverrides.get(block.name.toLowerCase());
						ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock> variants = new ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock>(
								Arrays.asList(block.variants));

						// Go through variants
						for (AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant : variants) {
							// Find locale
							String loc = "en-us";
							if (variant.locale != null)
								loc = variant.locale;

							// Find override
							String locF = loc;
							Optional<String> key = overrides.keySet().stream().filter(t -> t.equalsIgnoreCase(locF))
									.findFirst();
							if (key.isPresent()) {
								File f = overrides.get(key.get());
								int newV = idHash(Files.readAllBytes(f.toPath()));
								if (newV == variant.version)
									newV++;
								variant.version = newV;
								variant.size = f.length();
								overrides.remove(key.get());
							}
						}

						// Add remaining variants
						for (String locale : overrides.keySet()) {
							if (variants.size() > 0 && variants.get(0).locale == null)
								variants.get(0).locale = "en-us";
							File f = overrides.get(locale);
							locale = locale.toLowerCase();

							// Create variant
							AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant = new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock();
							variant.locale = locale;
							variant.size = f.length();
							variant.version = idHash(Files.readAllBytes(f.toPath()));
							variants.add(variant);
						}

						// Remove override
						assetOverrides.remove(block.name.toLowerCase());

						// Apply
						block.variants = variants
								.toArray(t -> new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock[t]);
					}
				}

				// Add remaining
				for (String name : assetOverrides.keySet()) {
					// Get overrides
					HashMap<String, File> overrides = assetOverrides.get(name);

					// Create block
					AssetVersionManifestData.AssetVersionBlock block = new AssetVersionManifestData.AssetVersionBlock();
					ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock> variants = new ArrayList<AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock>();
					block.name = name;

					// Add each locale
					for (String locale : overrides.keySet()) {
						File f = overrides.get(locale);
						locale = locale.toLowerCase();

						// Create variant
						AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock variant = new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock();
						variant.locale = locale.toLowerCase();
						variant.size = f.length();
						variant.version = idHash(Files.readAllBytes(f.toPath()));
						variants.add(variant);
					}
					if (variants.size() == 1 && variants.get(0).locale.equalsIgnoreCase("en-us"))
						variants.get(0).locale = null;

					// Add
					block.variants = variants
							.toArray(t -> new AssetVersionManifestData.AssetVersionBlock.AssetVariantBlock[t]);
					assets.add(block);
				}

				// Apply
				assetData.assets = assets.toArray(t -> new AssetVersionManifestData.AssetVersionBlock[t]);
			} else if (assetData.legacyData != null) {
				// Legacy
				ArrayList<AssetVersionManifestData.AssetBlockLegacy> assets = new ArrayList<AssetVersionManifestData.AssetBlockLegacy>(
						List.of(assetData.legacyData));

				// Go through assets
				for (AssetVersionManifestData.AssetBlockLegacy block : assets) {
					// Check
					if (assetOverrides.containsKey(block.assetName)) {
						// Get overrides
						HashMap<String, File> overrides = assetOverrides.get(block.assetName);

						// Find locale
						Optional<String> key = overrides.keySet().stream().filter(t -> t.equalsIgnoreCase(block.locale))
								.findFirst();
						if (key.isPresent()) {
							File f = overrides.get(key.get());
							int newV = idHash(Files.readAllBytes(f.toPath()));
							if (newV == block.version)
								newV++;
							block.version = newV;
							block.size = f.length();
							overrides.remove(key.get());
						}

						// Remove if empty
						if (overrides.size() == 0)
							assetOverrides.remove(block.assetName);
					}
				}

				// Add remaining assets
				for (String name : assetOverrides.keySet()) {
					// Get overrides
					HashMap<String, File> overrides = assetOverrides.get(name);

					// Add each locale
					for (String locale : overrides.keySet()) {
						File f = overrides.get(locale);
						AssetVersionManifestData.AssetBlockLegacy block = new AssetVersionManifestData.AssetBlockLegacy();
						block.assetName = name;
						block.locale = locale;
						block.size = f.length();
						block.version = idHash(Files.readAllBytes(f.toPath()));
						assets.add(block);
					}
				}

				// Apply
				assetData.legacyData = assets.toArray(t -> new AssetVersionManifestData.AssetBlockLegacy[t]);
			}

			// Write
			manifest = mapper.writer().withFeatures(ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL)
					.withRootName(assetData.assets == null ? "ArrayOfAssetVersion" : "AVL")
					.writeValueAsString(assetData);
		}

		// Return
		return new ByteArrayInputStream(manifest.getBytes("UTF-8"));
	}

	private int idHash(byte[] d) {
		try {
			// IK, MD5, its not for security, just need a way to create 4-byte ids
			// represented as integers
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(d);

			byte[] sub1 = Arrays.copyOfRange(digest, 0, 4);
			byte[] sub2 = Arrays.copyOfRange(digest, 4, 8);
			byte[] sub3 = Arrays.copyOfRange(digest, 8, 12);
			byte[] sub4 = Arrays.copyOfRange(digest, 12, 16);
			int x1 = ByteBuffer.wrap(sub1).getInt();
			int x2 = ByteBuffer.wrap(sub2).getInt();
			int x3 = ByteBuffer.wrap(sub3).getInt();
			int x4 = ByteBuffer.wrap(sub4).getInt();

			return x1 ^ x2 ^ x3 ^ x4;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	private void discoverAssetsIn(File root, HashMap<String, HashMap<String, File>> assetOverrides, String dataDir,
			String prefix, String locale) {
		// Check
		if (root.exists()) {
			// Go through folders
			if (prefix.isEmpty()) {
				// Check locale
				String loc = locale;
				if (loc == null)
					loc = "en-US";

				// Find all
				discoverAssetsIn(new File(root, "RS_CONTENT/"), assetOverrides, dataDir, "RS_CONTENT/", loc);
				discoverAssetsIn(new File(root, "RS_DATA/"), assetOverrides, dataDir, "RS_DATA/", loc);
				discoverAssetsIn(new File(root, "RS_MOVIES/"), assetOverrides, dataDir, "RS_MOVIES/", loc);
				discoverAssetsIn(new File(root, "RS_SHARED/"), assetOverrides, dataDir, "RS_SHARED/", loc);
				discoverAssetsIn(new File(root, "RS_SOUND/"), assetOverrides, dataDir, "RS_SOUND/", loc);
				discoverAssetsIn(new File(root, "RS_SCENE/"), assetOverrides, dataDir, "RS_SCENE/", loc);
			} else {
				// Go through subdirs
				for (File dir : root.listFiles(t -> t.isDirectory()))
					discoverAssetsIn(root, assetOverrides, dataDir, prefix + dir.getName() + "/", locale);

				// Find files
				for (File f : root.listFiles(t -> !t.isDirectory())) {
					// Apply
					HashMap<String, File> lst = assetOverrides.getOrDefault(prefix + f.getName(),
							new HashMap<String, File>());
					lst.put(locale, f);
					assetOverrides.put(prefix + f.getName(), lst);
				}
			}
		}
	}

	private void discoverServerOverrides(HashMap<String, HashMap<String, File>> assetOverrides, File root,
			String dataDir, String prefix, String locale) {
		// Check
		if (root.exists()) {
			// Go through folders
			if (prefix.isEmpty()) {
				// Check locale
				String loc = null;
				if (dataDir.split("/").length >= 2) {
					loc = dataDir.split("/")[1];

					// Go through other roots
					if (loc.equalsIgnoreCase("en-US")) {
						for (File locDir : root.getParentFile().listFiles(t -> t.isDirectory())) {
							if (!locDir.getName().equalsIgnoreCase("en-US")) {
								discoverServerOverrides(assetOverrides, locDir,
										dataDir.split("/")[0] + "/" + locDir.getName()
												+ dataDir.substring((dataDir.split("/")[0] + "/" + loc).length()),
										prefix, locale);
							}
						}
					}
				}

				// Discover
				discoverServerOverrides(assetOverrides, new File(root, "data"), dataDir, "RS_DATA/", loc);
				if (!new File(root, "data").exists())
					discoverServerOverrides(assetOverrides, new File(root, "Data"), dataDir, "RS_DATA/", loc);
				discoverServerOverrides(assetOverrides, new File(root, "contentdata"), dataDir, "RS_CONTENT/", loc);
				if (!new File(root, "contentdata").exists())
					discoverServerOverrides(assetOverrides, new File(root, "ContentData"), dataDir, "RS_CONTENT/", loc);
				discoverServerOverrides(assetOverrides, new File(root, "movies"), dataDir, "RS_MOVIES/", loc);
				if (!new File(root, "movies").exists())
					discoverServerOverrides(assetOverrides, new File(root, "Movies"), dataDir, "RS_MOVIES/", loc);
				discoverServerOverrides(assetOverrides, new File(root, "shareddata"), dataDir, "RS_SHARED/", loc);
				if (!new File(root, "shareddata").exists())
					discoverServerOverrides(assetOverrides, new File(root, "SharedData"), dataDir, "RS_SHARED/", loc);
				discoverServerOverrides(assetOverrides, new File(root, "sound"), dataDir, "RS_SOUND/", loc);
				if (!new File(root, "sound").exists())
					discoverServerOverrides(assetOverrides, new File(root, "Sound"), dataDir, "RS_SOUND/", loc);
				discoverServerOverrides(assetOverrides, new File(root, "scene"), dataDir, "RS_SOUND/", loc);
				if (!new File(root, "scene").exists())
					discoverServerOverrides(assetOverrides, new File(root, "Scenes"), dataDir, "RS_SCENE/", loc);
			} else {
				// Go through subdirs
				for (File dir : root.listFiles(t -> t.isDirectory()))
					discoverServerOverrides(assetOverrides, root, dataDir, prefix + dir.getName() + "/", locale);

				// Find files
				for (File f : root.listFiles(t -> !t.isDirectory())) {
					// Handle locale
					String loc = locale;
					String fn = f.getName();
					if (loc == null) {
						// Check name
						String ext = "";
						if (fn.contains(".")) {
							ext = fn.substring(fn.lastIndexOf("."));
							fn = fn.substring(0, fn.lastIndexOf("."));
						}
						if (fn.contains(".")) {
							loc = fn.substring(fn.lastIndexOf(".") + 1).toLowerCase();
							fn = fn.substring(0, fn.lastIndexOf("."));
						}
						fn = fn + ext;

						// Check
						if (loc == null)
							loc = "en-US";
					}

					// Apply
					HashMap<String, File> lst = assetOverrides.getOrDefault(prefix + f.getName(),
							new HashMap<String, File>());
					lst.put(loc, f);
					assetOverrides.put(prefix + fn, lst);
				}
			}
		}
	}

}
