package org.asf.cyan.fluid;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "org.asf.cyan.fluid.TestClass")
public class TestTransformer {

	@InjectAt(location = InjectLocation.TAIL)
	public void hello() {
		System.out.println("Hello from transformer!");
	}

}
