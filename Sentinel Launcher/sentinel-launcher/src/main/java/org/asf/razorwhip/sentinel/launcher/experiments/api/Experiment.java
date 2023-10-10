package org.asf.razorwhip.sentinel.launcher.experiments.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface Experiment {

	/**
	 * Defines the experiment key this class is attached to
	 * 
	 * @return Experiment key
	 */
	public String value();

}
