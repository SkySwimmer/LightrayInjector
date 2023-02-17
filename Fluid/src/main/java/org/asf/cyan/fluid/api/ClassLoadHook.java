package org.asf.cyan.fluid.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.objectweb.asm.tree.ClassNode;

/**
 * Low-level fluid loading hooks, based on ASM.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
public abstract class ClassLoadHook {
	private String target = "";
	private static Logger log;

	public ClassLoadHook() {
		log = LogManager.getLogger(getClass().getName());
	}

	private HashMap<String, String> mappedProperties = new HashMap<String, String>();
	private HashMap<String[], String> mappedMethods = new HashMap<String[], String>();

	private ArrayList<String> mapProps = new ArrayList<String>();
	private ArrayList<String[]> mapMeths = new ArrayList<String[]>();

	public boolean isSilent() {
		return false;
	}

	/**
	 * Add a property to be mapped, should only be called from build
	 * 
	 * @param name Property name
	 */
	public void addPropertyMapping(String name) {
		mapProps.add(name);
	}

	/**
	 * Add a method to be mapped, should only be called from build
	 * 
	 * @param name      Method name
	 * @param arguments Method argument types
	 */
	public void addMethodMapping(String name, String... arguments) {
		mapMeths.add(arrayFromMap(name, getTarget(), arguments));
	}

	/**
	 * Map a property defined in the class (can only be called after Fluid
	 * initialized this hook)
	 * 
	 * @param name Property name
	 * @return Obfuscated name of the property
	 */
	protected String mapProperty(String name) {
		return mappedProperties.get(name);
	}

	/**
	 * Map a method defined in the class (can only be called after Fluid initialized
	 * this hook)
	 * 
	 * @param name      Method name
	 * @param arguments Method argument types
	 * @return Obfuscated name of the method
	 */
	protected String mapMethod(String name, String... arguments) {
		String[] target = arrayFromMap(name, "", arguments);
		for (String[] method : mappedMethods.keySet()) {
			if (Arrays.equals(method, target))
				return mappedMethods.get(method);
		}
		return null;
	}

	private String[] arrayFromMap(String name, String cls, String[] arguments) {
		ArrayList<String> map = new ArrayList<String>();
		map.add(name);
		map.add(cls);
		map.addAll(Arrays.asList(arguments));
		return map.toArray(t -> new String[t]);
	}

	/**
	 * Set the target of this hook and initializes the mappings for it (called by
	 * the fluid agent, do not call manually, it will throw an exception after it
	 * has been set and gets called again)
	 * 
	 * @param target Target class path (slashed)
	 */
	public void intialize(String target) {
		if (this.target != "")
			throw new IllegalStateException("This hook is already initialized");

		this.target = target;

		log.debug("Initializing class hook " + this.getClass().getSimpleName() + "... target: " + targetPath()
				+ ", mapped target: " + target);
		for (String prop : mapProps) {
			String pOut = Fluid.mapProperty(targetPath(), prop);
			log.debug("Mapped property: " + prop + ", output: " + pOut);
			mappedProperties.put(prop, pOut);
		}

		for (String[] method : mapMeths) {
			String name = method[0];
			String[] arguments = Arrays.copyOfRange(method, 2, method.length);
			String mOut = Fluid.mapMethod(targetPath(), name, arguments);
			log.debug("Mapped method: " + name + ", output: " + mOut);
			mappedMethods.put(method, mOut);
		}
	}

	/**
	 * Get the actual target path of the transformer
	 * 
	 * @return Target path with slashes
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * Get the target class path (such as org.asf.foo.Bar)
	 * 
	 * @return Target class path as string
	 */
	public abstract String targetPath();

	/**
	 * Build the hook (define properties and fields to map)
	 */
	public abstract void build();

	/**
	 * Called when the hook is applied
	 * 
	 * @param loader          The class loader of the loading class
	 * @param cc              The class being loaded
	 * @param cp              The class pool currently in use
	 * @param classfileBuffer Class bytes
	 * @throws ClassNotFoundException If applying fails
	 */
	public abstract void apply(ClassNode cc, FluidClassPool cp, ClassLoader loader, byte[] classfileBuffer)
			throws ClassNotFoundException;
}
