package org.asf.cyan.fluid;

import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.junit.Test;

public class TransformTest {

	@Test
	public void testTransformers() throws ClassNotFoundException {
		// Init
		Transformers.initialize();

		// Build test
		FluidClassPool pool = FluidClassPool.create();
		byte[] bc = Transformers.applyTransformers("org.asf.cyan.fluid.TestClass",
				pool.getByteCode(pool.getClassNode("org.asf.cyan.fluid.TestClass")), getClass().getClassLoader());
		bc = bc;
	}

}
