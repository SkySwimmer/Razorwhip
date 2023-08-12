package org.asf.razorwhip.sentinel.launcher.api;

/**
 * 
 * Game Descriptor Interface
 * 
 * @author Sky Swimmer
 *
 */
public interface IGameDescriptor {

	/**
	 * Called to initialize the descriptor
	 */
	public void init();

	/**
	 * Called after the descriptor data has been updated
	 */
	public default void postUpdate() {
	}

}
