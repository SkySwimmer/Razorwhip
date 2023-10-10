package org.asf.razorwhip.sentinel.launcher.experiments.api;

import com.google.gson.JsonObject;

/**
 * 
 * Experiment interface
 * 
 * @author Sky Swimmer
 * 
 */
public interface IExperiment {

	/**
	 * Called when the experiment is loaded
	 */
	public void onLoad();

	/**
	 * Called when the experiment is just enabled (called only once on enabled)
	 */
	public void onEnable();

	/**
	 * Called when the experiment is just disabled (called only once on disable)
	 */
	public void onDisable();

	/**
	 * Called when the experiment config is loaded
	 * 
	 * @param config Experiment configuration object
	 */
	public default void onLoadConfig(JsonObject config) {
	}

	/**
	 * Called when the experiment config is saved
	 * 
	 * @param config Experiment configuration object
	 */
	public default void onSaveConfig(JsonObject config) {
	}

}
