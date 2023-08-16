package org.asf.razorwhip.sentinel.launcher.api;

import java.util.HashMap;

/**
 * 
 * Object Tag - named object memory
 * 
 * @author Sky Swimmer
 *
 */
public class ObjectTag {

	private HashMap<String, Object> memory = new HashMap<String, Object>();

	/**
	 * Retrieves objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 * @return Object instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> type) {
		return (T) memory.get(type.getTypeName());
	}

	/**
	 * Stores objects
	 * 
	 * @param <T>    Object type
	 * @param type   Object class
	 * @param object Object instance
	 */
	public <T> void setValue(Class<T> type, T object) {
		memory.put(type.getTypeName(), object);
	}

	/**
	 * Removes objects
	 * 
	 * @param <T>  Object type
	 * @param type Object class
	 */
	public <T> void removeValue(Class<T> type) {
		memory.remove(type.getTypeName());
	}

}
