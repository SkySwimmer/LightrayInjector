package org.asf.cyan.fluid;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Class tracker for Fluid, allows for finding loaded classes
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidClassTracker {

	static final ArrayList<String> invalidClasses = new ArrayList<String>();
	static final HashMap<String, Class<?>> loadedClasses = new HashMap<String, Class<?>>();

	public static void registerClass(Class<?> cls) {
		synchronized (loadedClasses) {
			if (!isClassLoaded(cls.getTypeName()))
				loadedClasses.put(cls.getTypeName(), cls);
		}
	}

	public static boolean isClassLoaded(String className) {
		synchronized (loadedClasses) {
			return loadedClasses.containsKey(className);
		}
	}
}