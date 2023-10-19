package org.asf.razorwhip.sentinel.launcher.descriptors.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.asf.connective.RemoteClient;
import org.asf.connective.objects.HttpRequest;
import org.asf.connective.objects.HttpResponse;
import org.asf.connective.processors.HttpPushProcessor;
import org.asf.razorwhip.sentinel.launcher.AssetManager;
import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.assets.AssetInformation;
import org.asf.razorwhip.sentinel.launcher.descriptors.SodGameDescriptor;
import org.asf.razorwhip.sentinel.launcher.descriptors.util.TripleDesUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class ContentServerRequestHandler extends HttpPushProcessor {

	private boolean encryptedInput;

	private String path;
	private IPreProcessor[] preProcessors;
	private HashMap<String, String> encryptedDocs;
	private HashMap<String, File> overriddenAssetFiles;

	private boolean hashed;

	private String fallbackAssetServerEndpoint;
	private Map<String, AssetInformation> assets;
	private File overrideDir;

	private String sanitizePath(String path) {
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");
		if (!path.startsWith("/"))
			path = "/" + path;
		return path;
	}

	private File loadAsset(String path) {
		// Check
		if (assets == null)
			return null;

		// Sanitize
		if (path.contains("\\"))
			path = path.replace("\\", "/");
		while (path.startsWith("/"))
			path = path.substring(1);
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);
		while (path.contains("//"))
			path = path.replace("//", "/");

		// Return
		AssetInformation asset = assets.get(path.toLowerCase());
		if (asset == null || !asset.localAssetFile.exists())
			return null;
		return asset.localAssetFile;
	}

	public interface IPreProcessor {
		public boolean match(String path, String method, RemoteClient client, String contentType, HttpRequest request,
				HttpResponse response, File sourceFile);

		public InputStream preProcess(String path, String method, RemoteClient client, String contentType,
				HttpRequest request, HttpResponse response, InputStream source, File sourceFile) throws IOException;
	}

	public ContentServerRequestHandler(JsonObject archiveDef, JsonObject descriptorDef,
			Map<String, AssetInformation> assets, String path, IPreProcessor[] preProcessors,
			String fallbackAssetServerEndpoint, File overrideDir) throws IOException {
		this.assets = assets;
		this.path = sanitizePath(path);
		this.preProcessors = preProcessors;
		this.overrideDir = overrideDir;
		this.fallbackAssetServerEndpoint = fallbackAssetServerEndpoint;
		this.hashed = archiveDef.has("useHashedArchive") && archiveDef.get("useHashedArchive").getAsBoolean();

		// Populate encrypted documents
		encryptedDocs = new HashMap<String, String>();
		boolean edgeFormatted = descriptorDef.has("edgeformattedManifests")
				&& descriptorDef.get("edgeformattedManifests").getAsBoolean();
		encryptedInput = !descriptorDef.has("encryptedManifests")
				|| descriptorDef.get("encryptedManifests").getAsBoolean();
		if (edgeFormatted) {
			// Find files
			HashMap<String, String> assetHashes = new HashMap<String, String>();
			indexAssetHashes(assetHashes, new File("assets/descriptor/hashes.shl"));

			// Find main files
			for (String key : assetHashes.keySet()) {
				if (key.startsWith("DWADragonsUnity/") && key.endsWith("/DWADragonsMain.xml")
						&& key.split("/").length == 4) {
					// Check encryption
					boolean encrypted = !assetHashes.containsKey(key + ".edgeunencrypted");
					String pth = "/" + key;

					// Load secret
					if (encrypted) {
						String secret = "C92EC1AA-54CD-4D0C-A8D5-403FCCF1C0BD";
						String assetPath = "DWADragonsUnity/" + key.split("/")[1] + "/" + key.split("/")[2]
								+ "/versionxmlsecret.conf";
						if (assets != null) {
							File verSpecificSecret = loadAsset(assetPath);
							if (verSpecificSecret != null) {
								// Read
								for (String line : Files.readAllLines(verSpecificSecret.toPath())) {
									if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
										String k = line.substring(0, line.indexOf("="));
										String v = line.substring(line.indexOf("=") + 1);
										if (k.equals("xmlsecret"))
											secret = v;
									}
								}
							}
						} else {
							// Download
							String url = fallbackAssetServerEndpoint;
							if (!url.endsWith("/"))
								url += "/";
							if (hashed) {
								AssetInformation asset = AssetManager.getActiveArchive().getAsset(assetPath);
								if (asset == null)
									url += assetPath;
								else
									url += asset.assetHash + ".sa";
							} else
								url += assetPath;
							for (String line : LauncherUtils.downloadString(url).split("\n")) {
								if (!line.isBlank() && !line.startsWith("#") && line.contains("=")) {
									String k = line.substring(0, line.indexOf("="));
									String v = line.substring(line.indexOf("=") + 1);
									if (k.equals("xmlsecret"))
										secret = v;
								}
							}
						}
						encryptedDocs.put(pth.toLowerCase(), secret);
					}
				}
			}
		} else {
			// Read list
			JsonObject secrets = descriptorDef.get("xmlSecrets").getAsJsonObject();
			for (String key : secrets.keySet()) {
				if (key.contains("-")) {
					String plat = key.substring(0, key.indexOf("-"));
					String ver = key.substring(key.indexOf("-") + 1);

					// Add
					encryptedDocs.put(("/DWADragonsUnity/" + plat + "/" + ver + "/DWADragonsMain.xml").toLowerCase(),
							secrets.get(key).getAsString());
				}
			}
		}
		new File(overrideDir, "serveroverrides").mkdirs();
		new File(overrideDir, "contentoverrides").mkdirs();
		overriddenAssetFiles = new HashMap<String, File>();
	}

	private void indexAssetHashes(HashMap<String, String> assetHashes, File hashFile)
			throws JsonSyntaxException, IOException {
		// Load hashes
		String[] lines = Files.readString(hashFile.toPath()).split("\n");
		for (String line : lines) {
			if (line.isEmpty())
				continue;
			// Parse
			String name = line.substring(0, line.indexOf(": ")).replace(";sp;", " ").replace(";cl;", ":")
					.replace(";sl;", ";");
			String hash = line.substring(line.indexOf(": ") + 2);
			assetHashes.put(name, hash);
		}
	}

	public ContentServerRequestHandler(boolean hashed, Map<String, AssetInformation> assets, String path,
			IPreProcessor[] preProcessors, String fallbackAssetServerEndpoint, File overrideDir,
			HashMap<String, String> encryptedDocs, boolean encryptedInput, HashMap<String, File> overriddenAssetFiles) {
		this.hashed = hashed;
		this.assets = assets;
		this.path = sanitizePath(path);
		this.preProcessors = preProcessors;
		this.overrideDir = overrideDir;
		this.fallbackAssetServerEndpoint = fallbackAssetServerEndpoint;
		this.encryptedDocs = encryptedDocs;
		this.encryptedInput = encryptedInput;
		this.overriddenAssetFiles = overriddenAssetFiles;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ContentServerRequestHandler(hashed, assets, path, preProcessors, fallbackAssetServerEndpoint,
				overrideDir, encryptedDocs, encryptedInput, overriddenAssetFiles);
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			// Compute subpath
			path = sanitizePath(path.substring(this.path.length()));
			if (path.equals("/sentineltest/sod/testrunning")) {
				setResponseContent("text/plain", "assetserver-sentinel-sod-" + SodGameDescriptor.ASSET_SERVER_VERSION);
				return;
			}

			// Make sure its not attempting to access a resource outside of the scope
			if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
				setResponseStatus(403, "Forbidden");
				return;
			}

			// Parse
			if (path.toLowerCase().startsWith("/mobileproxy.com/") || path.toLowerCase().equals("/mobileproxy.com")) {
				// Get subpath
				path = sanitizePath(path.substring("/mobileproxy.com".length()));

				// Make sure its not attempting to access a resource outside of the scope
				if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
					setResponseStatus(403, "Forbidden");
					return;
				}

				// Check
				if (path.substring(1).contains("/"))
					path = "/DWADragonsUnity" + path;
			} else if (path.toLowerCase().startsWith("/sentinel/") || path.toLowerCase().equals("/sentinel")) {
				// Get subpath
				path = sanitizePath(path.substring("/sentinel".length()));

				// Make sure its not attempting to access a resource outside of the scope
				if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
					setResponseStatus(403, "Forbidden");
					return;
				}

				// Check
				if (path.substring(1).contains("/"))
					path = "/DWADragonsUnity" + path;
			} else if (path.toLowerCase().startsWith("/sentinelproxy.com/")
					|| path.toLowerCase().equals("/sentinelproxy.com")) {
				// Get subpath
				path = sanitizePath(path.substring("/sentinelproxy.com".length()));

				// Make sure its not attempting to access a resource outside of the scope
				if (path.startsWith("..") || path.endsWith("..") || path.contains("/..") || path.contains("../")) {
					setResponseStatus(403, "Forbidden");
					return;
				}
			}

			// Map
			if (path.toLowerCase().startsWith("/dwadragonsunity/")) {
				String[] pathParts = path.split("/");
				if (pathParts.length >= 4) {
					// 1: [0] = <empty>
					// 2: [1] = dwadragonsunity
					// 3: [2] = <platform>
					// 4: [3] = <version>
					// 5: [4+] = asset
					String plat = pathParts[2];
					String version = pathParts[3];

					// Check if there is a asset
					if (pathParts.length >= 5) {
						String requestedAsset = path
								.substring(("/dwadragonsunity/" + plat + "/" + version + "/").length());
						String[] assetParts = requestedAsset.split("/");

						// Check asset
						// 1: [0] = Quality
						// 2: [1+] = Asset

						// Check if there is an asset
						String quality = assetParts[0];
						if (assetParts.length >= 2) {
							requestedAsset = path.substring(
									("/dwadragonsunity/" + plat + "/" + version + "/" + quality + "/").length());
							assetParts = requestedAsset.split("/");

							// Check if this is a version with a legacy structure
							if (assetParts.length >= 2) {
								// Check path
								if (assetParts[1].equalsIgnoreCase("ContentData")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "contentdata" + file;
									assetParts = requestedAsset.split("/");
								} else if (assetParts[1].equalsIgnoreCase("Data")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "data" + file;
									assetParts = requestedAsset.split("/");
								} else if (assetParts[1].equalsIgnoreCase("Movies")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "movies" + file;
									assetParts = requestedAsset.split("/");
								} else if (assetParts[1].equalsIgnoreCase("SharedData")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "shareddata" + file;
									assetParts = requestedAsset.split("/");
								} else if (assetParts[1].equalsIgnoreCase("Sound")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "sound" + file;
									assetParts = requestedAsset.split("/");
								} else if (assetParts[1].equalsIgnoreCase("Scenes")) {
									// Map
									String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
											+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
									requestedAsset = "scene" + file;
									assetParts = requestedAsset.split("/");
								}
							}

							// Check
							if (requestedAsset.toLowerCase().startsWith("data/content/")
									|| requestedAsset.toLowerCase().equals("data/content")) {
								path = requestedAsset.substring("data".length());
							}
						}
					}
				}
			}

			// Prepare
			long length = -1;
			InputStream fileData = null;
			File requestedFile = loadAsset(path);

			// Check result
			if (requestedFile == null && assets != null) {
				// Find another quality level if possible
				if (path.toLowerCase().startsWith("/dwadragonsunity/")) {
					String[] pathParts = path.split("/");
					if (pathParts.length >= 4) {
						// 1: [0] = <empty>
						// 2: [1] = dwadragonsunity
						// 3: [2] = <platform>
						// 4: [3] = <version>
						// 5: [4+] = asset
						String plat = pathParts[2];
						String version = pathParts[3];
						if (path.toLowerCase().startsWith(
								"/dwadragonsunity/" + plat.toLowerCase() + "/" + version.toLowerCase() + "/")) {
							// Check quality
							if (pathParts.length >= 5) {
								String requestedAsset = path.substring(
										("/dwadragonsunity/" + plat.toLowerCase() + "/" + version.toLowerCase() + "/")
												.length());
								String[] assetParts = requestedAsset.split("/");

								// Check asset
								// 1: [0] = Quality
								// 2: [1+] = Asset

								// Parse URL further
								String quality = assetParts[0];
								if (assetParts.length >= 2) {
									requestedAsset = path.substring(
											("/dwadragonsunity/" + plat + "/" + version + "/" + quality + "/")
													.length());

									// Try medium quality first
									if (requestedFile == null && !quality.equalsIgnoreCase("mid")) {
										String pth = "/dwadragonsunity/" + plat + "/" + version + "/Mid/"
												+ requestedAsset;
										requestedFile = loadAsset(pth);
									}

									// Try low quality next
									if (requestedFile == null && !quality.equalsIgnoreCase("low")) {
										String pth = "/dwadragonsunity/" + plat + "/" + version + "/Low/"
												+ requestedAsset;
										requestedFile = loadAsset(pth);
									}

									// Finally try high quality
									if (requestedFile == null && !quality.equalsIgnoreCase("high")) {
										String pth = "/dwadragonsunity/" + plat + "/" + version + "/High/"
												+ requestedAsset;
										requestedFile = loadAsset(pth);
									}
								}
							}
						}
					}
				}
			}

			// Check modifications
			File modFile = new File(new File(overrideDir, "serveroverrides"), path);
			if (modFile.exists())
				requestedFile = modFile;
			else {
				// Check data
				if (path.toLowerCase().startsWith("/dwadragonsunity/")) {
					String[] pathParts = path.split("/");
					if (pathParts.length >= 4) {
						// 1: [0] = <empty>
						// 2: [1] = dwadragonsunity
						// 3: [2] = <platform>
						// 4: [3] = <version>
						// 5: [4+] = asset
						String plat = pathParts[2];
						String version = pathParts[3];

						// Check if there is a asset
						if (pathParts.length >= 5) {
							String requestedAsset = path
									.substring(("/dwadragonsunity/" + plat + "/" + version + "/").length());
							String[] assetParts = requestedAsset.split("/");

							// Check asset
							// 1: [0] = Quality
							// 2: [1+] = Asset

							// Check if there is an asset
							String quality = assetParts[0];
							String locale = null;
							if (assetParts.length >= 2) {
								requestedAsset = path.substring(
										("/dwadragonsunity/" + plat + "/" + version + "/" + quality + "/").length());
								assetParts = requestedAsset.split("/");

								// Check if this is a version with a legacy structure
								if (assetParts.length >= 2) {
									if (assetParts[1].equalsIgnoreCase("ContentData")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "contentdata" + file;
										assetParts = requestedAsset.split("/");
									} else if (assetParts[1].equalsIgnoreCase("Data")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "data" + file;
										assetParts = requestedAsset.split("/");
									} else if (assetParts[1].equalsIgnoreCase("Movies")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "movies" + file;
										assetParts = requestedAsset.split("/");
									} else if (assetParts[1].equalsIgnoreCase("SharedData")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "shareddata" + file;
										assetParts = requestedAsset.split("/");
									} else if (assetParts[1].equalsIgnoreCase("Sound")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "sound" + file;
										assetParts = requestedAsset.split("/");
									} else if (assetParts[1].equalsIgnoreCase("Scenes")) {
										// Map
										locale = assetParts[0];
										String file = path.substring(("/dwadragonsunity/" + plat + "/" + version + "/"
												+ quality + "/" + assetParts[0] + "/" + assetParts[1]).length());
										requestedAsset = "scene" + file;
										assetParts = requestedAsset.split("/");
									}
								}

								// Parse locale
								if (locale == null) {
									locale = "en-us";

									// Check name
									String fn = new File(requestedAsset).getName();
									if (fn.contains(".")) {
										fn = fn.substring(0, fn.lastIndexOf("."));
									}
									if (fn.contains(".")) {
										locale = fn.substring(fn.lastIndexOf(".") + 1).toLowerCase();
										fn = fn.substring(0, fn.lastIndexOf("."));
									}
								}

								// Map
								if (requestedAsset.toLowerCase().startsWith("data/"))
									requestedAsset = "RS_DATA/" + requestedAsset.substring("data/".length());
								else if (requestedAsset.toLowerCase().startsWith("contentdata/"))
									requestedAsset = "RS_CONTENT/" + requestedAsset.substring("contentdata/".length());
								else if (requestedAsset.toLowerCase().startsWith("movies/"))
									requestedAsset = "RS_MOVIES/" + requestedAsset.substring("movies/".length());
								else if (requestedAsset.toLowerCase().startsWith("shareddata/"))
									requestedAsset = "RS_SHARED/" + requestedAsset.substring("shareddata/".length());
								else if (requestedAsset.toLowerCase().startsWith("sound/"))
									requestedAsset = "RS_SOUND/" + requestedAsset.substring("sound/".length());
								else if (requestedAsset.toLowerCase().startsWith("scene/"))
									requestedAsset = "RS_SCENE/" + requestedAsset.substring("scene/".length());

								// Check file
								if (overriddenAssetFiles.containsKey(requestedAsset)
										&& overriddenAssetFiles.get(requestedAsset).exists()) {
									modFile = overriddenAssetFiles.get(requestedAsset);
									requestedFile = modFile;
								} else {
									//
									// Contentoverrides structures:
									//
									// Without quality, without locale:
									// <RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <version>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <platform>/<version>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									//
									// With quality, without locale:
									// <quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <version>/<quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <platform>/<version>/<quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									//
									// Without quality, with locale:
									// <locale>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <locale>/<version>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <locale>/<platform>/<version>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									//
									// With quality, with locale:
									// <locale>/<quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <locale>/<version>/<quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									// <locale>/<platform>/<version>/<quality>/<RS_DATA/RS_CONTENT/RS_MOVIES/RS_SHARED/RS_SOUND/RS_SCENE>/<asset>
									//

									// Try 4 things:
									// - Without lowercase
									// - With lowercase
									// - With .unity3d in file and without lowercase
									// - With .unity3d in file and with lowercase
									for (int i = 0; i < 4; i++) {
										String assetFile = requestedAsset;

										// Check if we need to search for the unity3d file
										if (i >= 2) {
											// Add .unity3d if needed
											String fileName = requestedAsset;
											if (fileName.contains("/"))
												fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
											if (!fileName.contains("."))
												assetFile = requestedAsset + ".unity3d";
										}

										// Check if we need to try lowercase
										if (i == 1 || i == 3) {
											// Parse
											String fileName = requestedAsset;
											String fileParent = "";
											if (fileName.contains("/")) {
												fileParent = fileName.substring(0, fileName.lastIndexOf("/")) + "/";
												fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
											}

											// Convert to lowercase
											assetFile = fileParent + fileName.toLowerCase();
										}

										// Find
										modFile = new File(new File(overrideDir, "contentoverrides"), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by version
										modFile = new File(new File(overrideDir, "contentoverrides/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by platform and version
										modFile = new File(
												new File(overrideDir, "contentoverrides/" + plat + "/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by quality
										modFile = new File(
												new File(overrideDir, "contentoverrides/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by quality and version
										modFile = new File(
												new File(overrideDir,
														"contentoverrides/" + version + "/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by quality, platform and version
										modFile = new File(new File(overrideDir, "contentoverrides/" + plat + "/"
												+ version + "/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(new File(overrideDir, "contentoverrides/en-us"), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(new File(overrideDir, "contentoverrides/" + locale),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(
												new File(overrideDir, "contentoverrides/" + locale.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(new File(overrideDir, "contentoverrides/en-us/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(
												new File(overrideDir, "contentoverrides/" + locale + "/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(
												new File(overrideDir,
														"contentoverrides/" + locale.toLowerCase() + "/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(
												new File(overrideDir, "contentoverrides/en-us/" + plat + "/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(
												new File(overrideDir,
														"contentoverrides/" + locale + "/" + plat + "/" + version),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale
										modFile = new File(new File(overrideDir, "contentoverrides/"
												+ locale.toLowerCase() + "/" + plat + "/" + version), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir,
												"contentoverrides/en-us/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(
												new File(overrideDir,
														"contentoverrides/" + locale + "/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir, "contentoverrides/"
												+ locale.toLowerCase() + "/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir,
												"contentoverrides/en-us/" + version + "/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir, "contentoverrides/" + locale + "/"
												+ version + "/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir, "contentoverrides/"
												+ locale.toLowerCase() + "/" + version + "/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir, "contentoverrides/en-us/" + plat + "/"
												+ version + "/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(new File(overrideDir, "contentoverrides/" + locale + "/"
												+ plat + "/" + version + "/" + quality.toLowerCase()), assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check by locale and quality
										modFile = new File(
												new File(overrideDir, "contentoverrides/" + locale.toLowerCase() + "/"
														+ plat + "/" + version + "/" + quality.toLowerCase()),
												assetFile);
										if (modFile.exists()) {
											requestedFile = modFile;
											overriddenAssetFiles.put(requestedAsset, modFile);
										}

										// Check result
										if (overriddenAssetFiles.containsKey(requestedAsset))
											break;
									}
								}
							}
						}
					}
				}
			}

			// Find data
			if (requestedFile != null) {
				// From file

				// Open file
				if (!requestedFile.exists()) {
					setResponseStatus(404, "Not found");
					return;
				}
				fileData = new FileInputStream(requestedFile);
				length = requestedFile.length();
			} else if (fallbackAssetServerEndpoint != null) {
				// From server

				// Attempt to contact fallback server
				String url = fallbackAssetServerEndpoint;
				if (!url.endsWith("/"))
					url += "/";
				String pth = path.substring(1);

				// Check
				if (!hashed)
					url += pth;
				else {
					// Find
					AssetInformation asset = AssetManager.getActiveArchive().getAsset(pth);
					if (asset == null)
						url += pth;
					else
						url += asset.assetHash + ".sa";
				}

				// Try to contact server
				try {
					// Pull file
					URL u = new URL(url);
					URLConnection conn = u.openConnection();
					fileData = conn.getInputStream();
					length = conn.getContentLengthLong();
				} catch (Exception e) {
					setResponseStatus(404, "Not found");
					return;
				}
			} else {
				setResponseStatus(404, "Not found");
				return;
			}

			// Find type
			String type = MainFileMap.getInstance().getContentType(path);

			// Check encryption
			boolean encrypted = false;
			if (encryptedDocs.containsKey(path.toLowerCase())) {
				// Decrypt
				if (encryptedInput) {
					// Compute key
					byte[] key;
					try {
						MessageDigest digest = MessageDigest.getInstance("MD5");
						key = digest.digest(encryptedDocs.get(path.toLowerCase()).getBytes("ASCII"));
					} catch (NoSuchAlgorithmException e) {
						throw new RuntimeException(e);
					}

					// Read manifest
					byte[] manifest = fileData.readAllBytes();
					fileData.close();

					// Decrypt with triple DES
					manifest = TripleDesUtil.decrypt(Base64.getDecoder().decode(new String(manifest, "ASCII")), key);

					// Apply
					fileData = new ByteArrayInputStream(manifest);
				}
				encrypted = true;
			}

			// Find preprocessor
			boolean processed = false;
			for (IPreProcessor processor : preProcessors) {
				if (processor.match(path, method, client, contentType, getRequest(), getResponse(), requestedFile)) {
					// Run preprocessor
					fileData = processor.preProcess(path, method, client, contentType, getRequest(), getResponse(),
							fileData, requestedFile);
					processed = true;
				}
			}

			// Re-encrypt
			if (encrypted) {
				processed = true;
				length = -1;

				// Compute key
				byte[] key;
				try {
					MessageDigest digest = MessageDigest.getInstance("MD5");
					key = digest.digest(encryptedDocs.get(path.toLowerCase()).getBytes("ASCII"));
				} catch (NoSuchAlgorithmException e) {
					throw new RuntimeException(e);
				}

				// Read manifest
				byte[] manifest = fileData.readAllBytes();
				fileData.close();

				// Encrypt with triple DES
				manifest = TripleDesUtil.encrypt(manifest, key);

				// Apply
				fileData = new ByteArrayInputStream(Base64.getEncoder().encodeToString(manifest).getBytes("ASCII"));
			}

			// Set output
			if (getResponse().hasHeader("Content-Type"))
				type = getResponse().getHeaderValue("Content-Type");
			if (!processed && length != -1)
				setResponseContent(type, fileData, length);
			else
				setResponseContent(type, fileData);
		} finally {
			LauncherUtils.log(getRequest().getRequestMethod() + " " + path + " : " + getResponse().getResponseCode()
					+ " " + getResponse().getResponseMessage() + " [" + client.getRemoteAddress() + "]");
		}
	}

	private static class MainFileMap extends MimetypesFileTypeMap {
		private static MainFileMap instance;

		private FileTypeMap parent;

		public static MainFileMap getInstance() {
			if (instance == null) {
				instance = new MainFileMap(MimetypesFileTypeMap.getDefaultFileTypeMap());
			}
			return instance;
		}

		public MainFileMap(FileTypeMap parent) {
			this.parent = parent;
			this.addMimeTypes("application/xml	xml");
			this.addMimeTypes("application/json	json");
			this.addMimeTypes("text/ini	ini	ini");
			this.addMimeTypes("text/css	css");
			this.addMimeTypes("text/javascript	js");
			if (new File(".mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of(".mime.types")));
				} catch (IOException e) {
				}
			}
			if (new File("mime.types").exists()) {
				try {
					this.addMimeTypes(Files.readString(Path.of("mime.types")));
				} catch (IOException e) {
				}
			}
		}

		@Override
		public String getContentType(String filename) {
			String type = super.getContentType(filename);
			if (type.equals("application/octet-stream")) {
				type = parent.getContentType(filename);
			}
			return type;
		}
	}

}
