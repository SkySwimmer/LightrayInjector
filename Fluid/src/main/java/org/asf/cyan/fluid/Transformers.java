package org.asf.cyan.fluid;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asf.cyan.fluid.Transformer.AnnotationInfo;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.objectweb.asm.tree.ClassNode;

/**
 * 
 * Transformer Utility Class
 * 
 * @author Sky Swimmer
 *
 */
public class Transformers {

	private static boolean inited;
	private static FluidClassPool pool;
	private static Logger log = LogManager.getLogger("Fluid");
	private static ArrayList<ClassLoadHook> hooks = new ArrayList<ClassLoadHook>();
	private static HashMap<String, String> transformerOwners = new HashMap<String, String>();
	private static HashMap<String, ArrayList<ClassNode>> transformers = new HashMap<String, ArrayList<ClassNode>>();

	/**
	 * Initializes the transformer utility
	 */
	public static void initialize() {
		if (inited)
			throw new IllegalStateException("Cannot re-initialize FLUID!");

		for (ClassLoadHook hook : Fluid.getHooks()) {
			String target = Fluid.mapClass(hook.targetPath());
			hook.build();
			hook.intialize(target.replaceAll("\\.", "/"));
			hooks.add(hook);
		}

		int index = 0;
		for (String transformer : Fluid.getTransformers()) {
			ClassNode transformerNode;
			try {
				transformerNode = Fluid.getTransformerPool().getClassNode(transformer);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}

			String target = null;
			for (AnnotationInfo anno : AnnotationInfo.create(transformerNode)) {
				if (anno.is(TargetClass.class)) {
					target = anno.get("target");
				}
			}
			if (target != null) {
				target = Fluid.mapClass(target);
				ArrayList<ClassNode> trs = transformers.getOrDefault(target.replaceAll("\\.", "/"),
						new ArrayList<ClassNode>());
				trs.add(transformerNode);
				transformers.put(target.replaceAll("\\.", "/"), trs);
				transformerOwners.put(transformerNode.name, Fluid.getTransformerOwners()[index]);
			}
			index++;
		}

		pool = FluidClassPool.create();

		for (URL u : Fluid.getTransformerPool().getURLSources()) {
			if (!Stream.of(pool.getURLSources()).anyMatch(t -> t.toString().equals(u.toString()))) {
				pool.addSource(u);
			}
		}

		inited = true;

		for (Runnable hook : Fluid.getPostInitHooks()) {
			hook.run();
		}
	}

	/**
	 * Add source URLs, can be a jar or class folder
	 * 
	 * @param source Source URL
	 */
	public static void addClassSource(URL source) {
		pool.addSource(source);
	}

	/**
	 * Add source providers
	 * 
	 * @param provider Source provider
	 */
	public static void addClassSource(IClassSourceProvider<?> provider) {
		pool.addSource(provider);
	}

	/**
	 * Checks if the transformer utility is initialized
	 * 
	 * @return True if initialized, false otherwise
	 */
	public static boolean isInitialized() {
		return inited;
	}

	/**
	 * Applies transformers
	 * 
	 * @param className       Class name
	 * @param classfileBuffer Class bytecode
	 * @param loader          Class loader
	 * @return Modified bytecode
	 */
	public static byte[] applyTransformers(String className, byte[] classfileBuffer, ClassLoader loader) {
		if (!inited)
			throw new IllegalStateException("Transformer utility is not initialized");
		return applyTransformers(className, classfileBuffer, loader, pool, hooks, transformers, transformerOwners);
	}

	/**
	 * Applies transformers
	 * 
	 * @param className         Class name
	 * @param classfileBuffer   Class bytecode
	 * @param loader            Class loader
	 * @param pool              Class pool
	 * @param hooks             List of class hooks
	 * @param transformers      Map of transformers
	 * @param transformerOwners Map of transformer owners
	 * @return Modified bytecode
	 */
	public static byte[] applyTransformers(String className, byte[] classfileBuffer, ClassLoader loader,
			FluidClassPool pool, List<ClassLoadHook> hooks, Map<String, ArrayList<ClassNode>> transformers,
			Map<String, String> transformerOwners) {
		boolean match = false;
		boolean transformerMatch = false;
		if (hooks.stream().anyMatch(t -> {
			String target = t.getTarget();
			if (target.equals("@ANY"))
				return true;

			return target.equals(className);
		})) {
			match = true;
		}
		if (transformers.keySet().stream().anyMatch(t -> t.equals(className))) {
			transformerMatch = true;
		}

		if (!match && !transformerMatch) {
			try {
				pool.rewriteClass(className, classfileBuffer);
			} catch (ClassNotFoundException e) {
				pool.readClass(className, classfileBuffer);
			}
			return null;
		}

		byte[] bytecode = null;
		if (match) {
			ClassNode cls;
			try {
				cls = pool.rewriteClass(className, classfileBuffer);
			} catch (ClassNotFoundException e) {
				cls = pool.readClass(className, classfileBuffer);
			}
			ClassNode cc = cls;

			hooks.stream().filter(t -> {
				String target = t.getTarget();
				if (target.equals("@ANY"))
					return true;
				return target.equals(className);
			}).forEach(hook -> {
				try {
					if (!hook.isSilent())
						log.debug("Applying hook " + hook.getClass().getTypeName() + " to class " + className);

					hook.apply(cc, pool, loader, classfileBuffer);
				} catch (ClassNotFoundException e) {
					log.error("FLUID hook apply failed, hook type: " + hook.getClass().getTypeName(), e);
				}
			});

			bytecode = pool.getByteCode(cc.name);
		}
		if (transformerMatch) {
			String clName = className.replaceAll("/", ".");
			for (Mapping<?> map : Fluid.getMappings()) {
				boolean found = false;
				for (Mapping<?> mp : map.mappings) {
					if (mp.obfuscated.equals(clName)) {
						clName = mp.name;
						found = true;
						break;
					}
				}
				if (found)
					break;
			}

			ClassNode cls = null;
			if (bytecode != null) {
				try {
					cls = pool.rewriteClass(className, bytecode);
				} catch (ClassNotFoundException e) {
					cls = pool.readClass(className, bytecode);
				}
			} else {
				try {
					cls = pool.rewriteClass(className, classfileBuffer);
				} catch (ClassNotFoundException e) {
					cls = pool.readClass(className, classfileBuffer);
				}
			}

			Transformer.transform(cls, transformerOwners, transformers, clName, className, pool,
					Fluid.getTransformerPool(), loader);
			bytecode = pool.getByteCode(cls.name);
		} else {
			if (bytecode != null) {
				try {
					pool.rewriteClass(className, bytecode);
				} catch (ClassNotFoundException e) {
					pool.readClass(className, bytecode);
				}
			} else {
				try {
					pool.rewriteClass(className, classfileBuffer);
				} catch (ClassNotFoundException e) {
					pool.readClass(className, classfileBuffer);
				}
			}
		}

		return bytecode;
	}

}
