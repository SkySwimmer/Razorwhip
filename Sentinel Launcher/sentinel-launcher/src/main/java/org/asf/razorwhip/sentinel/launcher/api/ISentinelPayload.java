package org.asf.razorwhip.sentinel.launcher.api;

/**
 * 
 * Sentinel Payload File
 * 
 * @author Sky Swimmer
 *
 */
public interface ISentinelPayload {

	/**
	 * Called when the payload class is first loaded and constructed
	 */
	public default void preInit() {
	}

	/**
	 * Called when all payloads are being initialized
	 */
	public void init();

	/**
	 * Called when loading is done
	 */
	public default void postInit() {
	}

}
