package org.asf.cyan.fluid.bytecode.sources;

import java.io.InputStream;

import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;

/**
 * 
 * Class source provider, used by the class pool
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 */
public interface IClassSourceProvider<T> {

	/**
	 * Gets the comparison method used to compare the provider objects.
	 */
	public ComparisonMethod getComparisonMethod();

	/**
	 * The object used to get the stream, used for comparison with other providers.
	 */
	public T providerObject();

	/**
	 * 
	 * Gets the input stream for the given class.
	 * 
	 * @param classType Class path<br />
	 *                  Example:
	 *                  org/asf/cyan/fluid/bytecode/sources/IClassSourceProvider
	 * 
	 * @return Class stream, null if not found.
	 */
	public InputStream getStream(String classType);

	/**
	 * Used to import all classes (finding and reading classes)
	 * 
	 * @param pool Pool to import the classes in
	 */
	public void importAllRead(FluidClassPool pool);

	/**
	 * Used to import all classes (finding classes only)
	 * 
	 * @param pool Pool to import the classes in
	 */
	public void importAllFind(FluidClassPool pool);
}
