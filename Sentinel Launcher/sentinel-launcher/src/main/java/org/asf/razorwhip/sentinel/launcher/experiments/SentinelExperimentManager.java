package org.asf.razorwhip.sentinel.launcher.experiments;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asf.razorwhip.sentinel.launcher.LauncherUtils;
import org.asf.razorwhip.sentinel.launcher.windows.ExperimentManagerWindow;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Sentinel experiment manager
 * 
 * @author Sky Swimmer
 * 
 */
public class SentinelExperimentManager extends ExperimentManager {

	public static void bindManager() {
		implementation = new SentinelExperimentManager();
	}

	/**
	 * Shows the experiment manager window
	 */
	public static void showExperimentManagerWindow() {
		ExperimentManagerWindow window = new ExperimentManagerWindow();
		if (window.showDialog()) {
			// Restart sentinel
			LauncherUtils.log("Requesting restart via exit code 237...");
			System.exit(237);
		}
	}

	@Override
	protected void init() {
		SentinelDefaultExperiments.registerExperiments(implementation);
	}

	@Override
	protected void registerExperimentInterfaces(String key) {
		// Sentinel does not have this feature support due to lack of Fluid libraries,
		// we dont really need it either, we can do it manually
	}

	@Override
	protected void saveExperimentConfig(JsonObject config) throws IOException {
		Files.writeString(Path.of("experiments.json"), new GsonBuilder().setPrettyPrinting().create().toJson(config));
	}

	@Override
	protected JsonObject loadExperimentConfig() throws IOException {
		if (!new File("experiments.json").exists())
			return new JsonObject();
		return JsonParser.parseString(Files.readString(Path.of("experiments.json"))).getAsJsonObject();
	}

	@Override
	protected void saveExperimentCache(JsonObject config) throws IOException {
		new File("cache").mkdirs();
		Files.writeString(Path.of("cache/experiments.json"),
				new GsonBuilder().setPrettyPrinting().create().toJson(config));
	}

	@Override
	protected JsonObject loadExperimentCache() throws IOException {
		if (!new File("cache/experiments.json").exists())
			return new JsonObject();
		return JsonParser.parseString(Files.readString(Path.of("cache/experiments.json"))).getAsJsonObject();
	}

}
