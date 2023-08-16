package org.asf.razorwhip.sentinel.launcher.api;

/**
 * 
 * Emulation Software Provider Interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IEmulationSoftwareProvider {

	/**
	 * Called to initialize the emulation software provider
	 */
	public void init();

	/**
	 * Called after the emulation software provider has been updated
	 */
	public default void postUpdate() {
	}

	/**
	 * Called to show the launcher option window
	 */
	public void showOptionWindow();
}
