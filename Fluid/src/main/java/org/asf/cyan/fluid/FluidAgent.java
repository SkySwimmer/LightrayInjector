package org.asf.cyan.fluid;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import org.asf.cyan.fluid.DynamicClassLoader.LoadedClassProvider;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;

/**
 * Fluid Agent Class, without this, Fluid won't work
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidAgent {

	/**
	 * Main premain startup method
	 * 
	 * @param args Arguments
	 * @param inst Java instrumentation
	 */
	public static void premain(final String args, final Instrumentation inst) {
		agentmain(args, inst);
	}

	public static String getMarker() {
		return "Agent";
	}

	private static boolean initialized = false;
	private static boolean loaded = false;
	private static ArrayList<ClassLoader> knownLoaders = new ArrayList<ClassLoader>();

	public static void initialize() {
		if (initialized)
			throw new IllegalStateException("Cannot re-initialize FLUID!");
		if (!Transformers.isInitialized())
			Transformers.initialize();
	}

	static boolean ranHooks = false;
	private static boolean loadedAgents = false;
	private static Instrumentation inst = null;

	/**
	 * Adds the given file to the system class path
	 * 
	 * @param f File to add
	 * @throws IOException If adding the jar fails
	 */
	public static void addToClassPath(File f) throws IOException {
		inst.appendToSystemClassLoaderSearch(new JarFile(f));
	}

	/**
	 * Main agent startup method
	 * 
	 * @param args Arguments
	 * @param inst Java instrumentation
	 */
	public static void agentmain(final String args, final Instrumentation inst) {
		if (FluidAgent.inst == null)
			FluidAgent.inst = inst;
		if (loaded)
			return;

		loaded = true;
		inst.addTransformer(new ClassFileTransformer() {
			@Override
			public synchronized byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) {
				if (!initialized)
					return null;

				if (!loadedAgents) {
					loadedAgents = true;

					if (!DynamicClassLoader.knowsLoadedClassProvider("fluidagent")) {
						DynamicClassLoader.registerLoadedClassProvider(new LoadedClassProvider() {

							@Override
							public String name() {
								return "fluidagent";
							}

							@Override
							public Class<?> provide(String name) {
								return getLoadedClass(name);
							}
						});
					}

					Fluid.getAgents().forEach((cls, meth) -> {
						try {
							Class<?> agent = FluidAgent.class.getClassLoader().loadClass(cls);
							if (meth != null) {
								Method mth = agent.getMethod(meth, String.class, Instrumentation.class);
								mth.invoke(null, args, inst);
							}
						} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
								| InvocationTargetException | NoSuchMethodException | SecurityException e) {
						}
					});
				}

				if (loader != null && !knownLoaders.contains(loader)) {
					knownLoaders.add(loader);
					Transformers.addClassSource(new LoaderClassSourceProvider(loader));
				}

				if (!Transformers.hasTransformers(className))
					return classfileBuffer;
				return Transformers.applyTransformers(className, classfileBuffer, loader);
			}
		});
	}

	public static void forAllClasses(Consumer<Class<?>> function) {
		Class<?>[] classes = inst.getAllLoadedClasses();
		for (Class<?> cls : classes)
			function.accept(cls);
	}

	public static Class<?> getLoadedClass(String name) {
		Class<?>[] classes = inst.getAllLoadedClasses();
		for (Class<?> cls : classes)
			if (cls.getTypeName().equals(name))
				return cls;
		return null;
	}
}
