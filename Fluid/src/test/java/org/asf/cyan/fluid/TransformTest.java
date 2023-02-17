package org.asf.cyan.fluid;

import static org.junit.Assert.assertTrue;

import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.junit.Test;

public class TransformTest {

	@Test
	public void testTransformers() throws Throwable {
		// Init
		Fluid.openFluidLoader();
		FluidClassPool pool = FluidClassPool.create();
		pool.importAllSources();
		Fluid.registerAllTransformersFrom(pool);
		Fluid.closeFluidLoader();
		Transformers.initialize();

		// Build test
		byte[] bc = Transformers.applyTransformers("org.asf.cyan.fluid.TestClass",
				pool.getByteCode(pool.getClassNode("org.asf.cyan.fluid.TestClass")), getClass().getClassLoader());
		assertTrue(bc != null);

		// Load and test
		BinaryClassLoader loader = new BinaryClassLoader();
		Class<?> cls = loader.loadClass("org.asf.cyan.fluid.TestClass", bc);
		Object inst = cls.getConstructor().newInstance();
		assertTrue((Boolean) inst.getClass().getMethod("test1").invoke(inst));
		String s = (String) inst.getClass().getMethod("test2").invoke(inst);
		assertTrue(s.equals("headvanillatail"));
		assertTrue(inst.getClass().getMethod("test3").invoke(inst).equals("modded"));
		pool.close();
	}

}
