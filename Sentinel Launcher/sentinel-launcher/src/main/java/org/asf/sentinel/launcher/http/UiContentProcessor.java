package org.asf.sentinel.launcher.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpRequestProcessor;
import org.asf.sentinel.launcher.LauncherMain;
import org.asf.sentinel.launcher.LauncherUtils;

public class UiContentProcessor extends HttpRequestProcessor {

	@Override
	public HttpRequestProcessor createNewInstance() {
		return new UiContentProcessor();
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public void process(String path, String method, RemoteClient client) throws IOException {
		try {
			// Prepare to retireve resource
			InputStream source = null;
			while (path.startsWith("/"))
				path = path.substring(1);

			// Check scope
			String scope = "launcher";
			if (getRequestQueryParameters().containsKey("scope")) {
				scope = getRequestQueryParameters().get("scope");
			}

			// Find class
			Class<?> cls = LauncherMain.class;
			switch (scope) {

			case "launcher": {
				cls = LauncherMain.class;
				break;
			}

			default: {
				setResponseStatus(404, "Scope not recognized");
				return;
			}

			}

			try {
				// Get source
				URL loc = cls.getProtectionDomain().getCodeSource().getLocation();
				File f = new File(loc.toURI());

				// Check
				if (f.isFile()) {
					// Resource from jar
					source = new URL("jar:" + f.toURI() + "!/" + path).openStream();
				} else {
					// Directory mode
					source = new FileInputStream(new File(f, path));
				}

				// Find type
				String type = MainFileMap.getInstance().getContentType(path);

				// Set output
				setResponseContent(type, source);
			} catch (Exception e) {
				if (source != null)
					source.close();
				setResponseStatus(404, "File not found");
				return;
			}
		} finally {
			LauncherUtils.getInstance()
					.log(getRequest().getRequestMethod() + " /" + path + " : " + getResponse().getResponseCode() + " "
							+ getResponse().getResponseMessage() + " [" + client.getRemoteAddress() + "]");
		}
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
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
