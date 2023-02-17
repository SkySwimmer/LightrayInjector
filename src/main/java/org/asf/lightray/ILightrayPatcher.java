package org.asf.lightray;

import java.io.File;
import java.util.ArrayList;

import org.asf.cyan.fluid.bytecode.FluidClassPool;

/**
 * 
 * Lightray Patcher Interface (requires the {@link LightrayPatcher} annotation
 * to be present)
 * 
 * @author Sky Swimmer
 *
 */
public interface ILightrayPatcher {

	/**
	 * Applies the patcher
	 * 
	 * @param apk           Input APK
	 * @param workDir       Working directory
	 * @param resourceFiles List of resource files that will be added to the APK
	 * @param resourceDir   Resource directory
	 * @param pool          Transformer class pool
	 */
	public void apply(String apk, File workDir, ArrayList<String> resourceFiles, File resourceDir, FluidClassPool pool);

}
