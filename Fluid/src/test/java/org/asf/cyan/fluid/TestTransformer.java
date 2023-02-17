package org.asf.cyan.fluid;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetName;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "org.asf.cyan.fluid.TestClass")
public class TestTransformer {

	@InjectAt(location = InjectLocation.TAIL)
	public void hello() {
		System.out.println("Hello from transformer!");
	}

	@Erase
	public boolean test1() {
		return true;
	}

	@TargetType(target = "java.lang.String")
	@InjectAt(location = InjectLocation.HEAD, offset = 1)
	public void test2(@LocalVariable String str) {
		str = "head" + str;
	}

	@TargetName(target = "test2")
	@TargetType(target = "java.lang.String")
	@InjectAt(location = InjectLocation.TAIL)
	public void test2_i2(@LocalVariable String str) {
		str += "tail";
	}

	@Erase
	public String test3() {
		return "modded";
	}

}
