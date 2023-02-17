package org.asf.cyan.fluid;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * 
 * Binary Class Loader
 * 
 * @author Sky Swimmer
 *
 */
public class BinaryClassLoader extends ClassLoader {

	private HashMap<String, byte[]> classes = new HashMap<String, byte[]>();
	private HashMap<String, Class<?>> loadedClasses = new HashMap<String, Class<?>>();

	/**
	 * Adds classes
	 * 
	 * @param name     Class name
	 * @param bytecode Class bytecode
	 */
	public void addClass(String name, byte[] bytecode) {
		classes.put(name.replace(".", "/"), bytecode);
	}

	/**
	 * Adds and loads classes
	 * 
	 * @param name     Class name
	 * @param bytecode Class bytecode
	 * @return Class instance
	 */
	public Class<?> loadClass(String name, byte[] bytecode) {
		classes.put(name.replace(".", "/"), bytecode);
		loadedClasses.put(name, defineClass(name, ByteBuffer.wrap(bytecode), null));
		return loadedClasses.get(name);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		if (loadedClasses.containsKey(name.replace(".", "/")))
			return loadedClasses.get(name.replace(".", "/"));
		if (!classes.containsKey(name)) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			return Class.forName(name.replace("/", "."), true, l);
		}
		return super.findClass(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		if (loadedClasses.containsKey(name.replace(".", "/")))
			return loadedClasses.get(name.replace(".", "/"));
		return loadClass(name, true);
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (loadedClasses.containsKey(name.replace(".", "/")))
			return loadedClasses.get(name.replace(".", "/"));
		name = name.replace(".", "/");
		if (!classes.containsKey(name)) {
			ClassLoader l = getParent();
			if (l == null)
				l = Thread.currentThread().getContextClassLoader();
			if (l == this)
				l = ClassLoader.getSystemClassLoader();
			return Class.forName(name.replace("/", "."), true, l);
		}
		byte[] data = classes.get(name);
		loadedClasses.put(name, defineClass(name, ByteBuffer.wrap(data), null));
		return loadedClasses.get(name);
	}

}
