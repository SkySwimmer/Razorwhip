package org.asf.razorwhip.sentinel.launcher.experiments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.asf.razorwhip.sentinel.launcher.experiments.api.IExperiment;

import com.google.gson.JsonObject;

/**
 * 
 * Experiment Manager System - Controls experimental features
 * 
 * @author Sky Swimmer
 * 
 */
public abstract class ExperimentManager {

	protected static ExperimentManager implementation;
	private boolean inited = false;

	private boolean enabled;
	private JsonObject managerConfig;
	private JsonObject managerCache;
	private HashMap<String, ArrayList<IExperiment>> experimentInterfaces = new HashMap<String, ArrayList<IExperiment>>();
	private HashMap<String, String> experimentPrettyNames = new HashMap<String, String>();
	private LinkedHashMap<String, Boolean> experiments = new LinkedHashMap<String, Boolean>();
	private ArrayList<String> experimentsNeedingReinit = new ArrayList<String>();

	private void initManager() {
		if (inited)
			return;
		inited = true;

		// Load config
		try {
			managerConfig = loadExperimentConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			managerCache = loadExperimentCache();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (managerConfig.has("enabled"))
			enabled = managerConfig.get("enabled").getAsBoolean();

		// Load experiments
		init();

		// Save config
		try {
			saveConfig();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Retrieves the active experiment manager
	 * 
	 * @return ExperimentManager instance
	 */
	public static ExperimentManager getInstance() {
		return implementation;
	}

	/**
	 * Called to initialize the experiment manager
	 */
	protected abstract void init();

	/**
	 * Called to save the experiment configuration
	 * 
	 * @param config Experiment configuration to save
	 * @throws IOException If saving fails
	 */
	protected abstract void saveExperimentConfig(JsonObject config) throws IOException;

	/**
	 * Called to load the experiment config
	 * 
	 * @return Experiment configuration json
	 * @throws IOException If loading fails
	 */
	protected abstract JsonObject loadExperimentConfig() throws IOException;

	/**
	 * Called to save the experiment cache configuration
	 * 
	 * @param config Experiment configuration to save
	 * @throws IOException If saving fails
	 */
	protected abstract void saveExperimentCache(JsonObject config) throws IOException;

	/**
	 * Called to load the experiment cache config
	 * 
	 * @return Experiment configuration json
	 * @throws IOException If loading fails
	 */
	protected abstract JsonObject loadExperimentCache() throws IOException;

	/**
	 * Retrieves all experiment keys
	 * 
	 * @return Array of experiment keys
	 */
	public String[] getExperiments() {
		return experiments.keySet().toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the pretty name of experiment keys
	 * 
	 * @param key Experiment key
	 * @return Experiment pretty name or its key if no name is set
	 */
	public String getExperimentName(String key) {
		// Init
		initManager();

		// Return
		return experimentPrettyNames.getOrDefault(key, key);
	}

	/**
	 * Assigns the pretty name of experiment keys
	 * 
	 * @param key  Experiment key
	 * @param name New experiment name
	 * @throws IllegalArgumentException If the experiment does not exist
	 */
	public void setExperimentName(String key, String name) throws IllegalArgumentException {
		// Init
		initManager();

		// Check existence
		if (!experiments.containsKey(key))
			throw new IllegalArgumentException("Experiment '" + key + "' does not exist");

		// Set
		experimentPrettyNames.put(key, name);
	}

	/**
	 * Registers experiments
	 * 
	 * @param key Experiment key to register
	 */
	public void registerExperiment(String key) {
		// Init
		initManager();

		// Add
		if (!experiments.containsKey(key)) {
			// Load settings
			JsonObject enabledExperiments = new JsonObject();
			if (managerConfig.has("experiments"))
				enabledExperiments = managerConfig.get("experiments").getAsJsonObject();

			// Register and set state
			experiments.put(key, enabledExperiments.has(key) ? enabledExperiments.get(key).getAsBoolean() : false);
		}
	}

	/**
	 * Registers experiment interfaces
	 * 
	 * @param key   Experiment key
	 * @param inter Interface to register
	 * @throws IllegalArgumentException If the experiment does not exist
	 */
	public void registerExperimentInterface(String key, IExperiment inter) throws IllegalArgumentException {
		// Init
		initManager();

		// Check existence
		if (!experiments.containsKey(key))
			throw new IllegalArgumentException("Experiment '" + key + "' does not exist");

		// Register
		if (!experimentInterfaces.containsKey(key))
			experimentInterfaces.put(key, new ArrayList<IExperiment>());
		if (experimentInterfaces.get(key).contains(inter))
			return;
		experimentInterfaces.get(key).add(inter);

		// Load config into interface
		JsonObject experimentConfig = new JsonObject();
		JsonObject experimentSettings = new JsonObject();
		if (managerConfig.has("configurations"))
			experimentSettings = managerConfig.get("configurations").getAsJsonObject();
		if (experimentSettings.has(key))
			experimentConfig = experimentSettings.get(key).getAsJsonObject();
		inter.onLoadConfig(experimentConfig);

		// Check cache
		boolean oldState = false;
		if (managerCache.has(key)) {
			// Load old state
			oldState = managerCache.get(key).getAsBoolean();
		}

		// Check state
		if (oldState != isExperimentEnabled(key)) {
			// Apply state
			if (isExperimentEnabled(key))
				inter.onEnable();
			else
				inter.onDisable();

			// Apply to cache
			managerCache.addProperty(key, isExperimentEnabled(key));
			experimentsNeedingReinit.add(key);

			// Save cache
			try {
				saveExperimentCache(managerCache);
			} catch (IOException e) {
			}
		} else if (experimentsNeedingReinit.contains(key)) {
			// Apply state
			if (isExperimentEnabled(key))
				inter.onEnable();
			else
				inter.onDisable();
		}

		// Call load
		if (isExperimentEnabled(key))
			inter.onLoad();
	}

	/**
	 * Enables the experiment manager
	 * 
	 * @throws IOException If saving errors
	 */
	public void enable() throws IOException {
		// Init
		initManager();

		// Check
		if (enabled)
			return;

		// Enable
		enabled = true;

		// Save
		saveConfig();
	}

	/**
	 * Checks if the experiment manager is enabled
	 * 
	 * @return True if enabled, false otherwise
	 */
	public boolean isEnabled() {
		// Init
		initManager();

		// Check
		if (System.getProperty("enableAllExperiments") != null)
			return true;

		// Return
		return enabled;
	}

	/**
	 * Checks if experiments are enabled
	 * 
	 * @param key Experiment key
	 * @return True if enabled, false otherwise
	 */
	public boolean isExperimentEnabled(String key) {
		// Init
		initManager();

		// Check existence
		if (!experiments.containsKey(key))
			return false;

		// Check override
		if (System.getProperty("enableAllExperiments") != null)
			return true;

		// Return
		return experiments.get(key);
	}

	/**
	 * Changes experiment settings
	 * 
	 * @param key   Experiment key
	 * @param state Experiment state
	 * @throws IOException              If saving fails
	 * @throws IllegalArgumentException If the experiment does not exist
	 */
	public void setExperimentEnabled(String key, boolean state) throws IOException, IllegalArgumentException {
		// Init
		initManager();

		// Check existence
		if (!experiments.containsKey(key))
			throw new IllegalArgumentException("Experiment '" + key + "' does not exist");

		// Check current state
		if (experiments.get(key) == state)
			return;

		// Assign
		experiments.put(key, state);
		experimentsNeedingReinit.remove(key);

		// Call interfaces if needed
		if (isExperimentEnabled(key) != state) {
			if (experimentInterfaces.containsKey(key)) {
				for (IExperiment inter : experimentInterfaces.get(key)) {
					if (state) {
						inter.onEnable();
						inter.onLoad();
					} else if (System.getProperty("enableAllExperiments") != null)
						inter.onDisable();
				}
			}
		}

		// Save
		saveConfig();
	}

	/**
	 * Saves the experiment configuration
	 * 
	 * @throws IOException If saving errors
	 */
	public void saveConfig() throws IOException {
		// Init
		initManager();

		// Add settings
		managerConfig.addProperty("enabled", enabled);
		JsonObject enabledExperiments = new JsonObject();
		JsonObject cacheData = new JsonObject();
		if (managerConfig.has("experiments"))
			enabledExperiments = managerConfig.get("experiments").getAsJsonObject();
		managerConfig.add("experiments", enabledExperiments);
		JsonObject experimentSettings = new JsonObject();
		if (managerConfig.has("configurations"))
			experimentSettings = managerConfig.get("configurations").getAsJsonObject();
		managerConfig.add("configurations", experimentSettings);

		// Assign values
		for (String experiment : experiments.keySet()) {
			// Save state
			enabledExperiments.addProperty(experiment, experiments.get(experiment));
			cacheData.addProperty(experiment, isExperimentEnabled(experiment));

			// Save configs
			JsonObject experimentConfig = new JsonObject();
			if (experimentSettings.has(experiment))
				experimentConfig = experimentSettings.get(experiment).getAsJsonObject();
			experimentSettings.add(experiment, experimentConfig);

			// Find classes and run save
			if (experimentInterfaces.containsKey(experiment)) {
				for (IExperiment inter : experimentInterfaces.get(experiment))
					inter.onSaveConfig(experimentConfig);
			}
		}

		// Save to disk
		saveExperimentConfig(managerConfig);

		// Save cache
		saveExperimentCache(cacheData);
		managerCache = cacheData;
	}

}
