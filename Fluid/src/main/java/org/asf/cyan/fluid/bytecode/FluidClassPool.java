package org.asf.cyan.fluid.bytecode;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.URLClassSourceProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 
 * FLUID Class Pool, ASM Support Class, allows for the loading of classes from
 * jars and folders
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidClassPool implements Closeable {
	/**
	 * Main implementation, used to create class pool instances
	 */
	protected static FluidClassPool implementation = new FluidClassPool();

	protected FluidClassPool newInstance() {
		return new FluidClassPool();
	}

	private class ClassEntry {
		public ClassNode node;
		public String firstName;
	}

	protected FluidClassPool() {
	}

	private ArrayList<IClassSourceProvider<?>> sources = new ArrayList<IClassSourceProvider<?>>();
	private ArrayList<ClassEntry> classes = new ArrayList<ClassEntry>();
	private HashMap<String, String> classHashes = new HashMap<String, String>();

	private ArrayList<String> includedclasses = new ArrayList<String>();

	/**
	 * Gets the URL source providers as URLs.
	 */
	public URL[] getURLSources() {
		return sources.stream().filter(t -> t.providerObject() instanceof URL || t.providerObject() instanceof File)
				.map(t -> {
					Object o = t.providerObject();
					if (o instanceof File)
						try {
							return ((File) o).toURI().toURL();
						} catch (MalformedURLException e) {
							return null;
						}
					return (URL) o;
				}).filter(t -> t != null).toArray(t -> new URL[t]);
	}

	/**
	 * Add class names to the included list, if none are added, all will be loaded
	 * on jar import.
	 * 
	 * @param name Class name
	 */
	public void addIncludedClass(String name) {
		includedclasses.add(name);
	}

	public ClassNode[] getLoadedClasses() {
		return classes.stream().map(t -> t.node).toArray(t -> new ClassNode[t]);
	}

	/**
	 * Create a class pool with the default classpath.<br />
	 * <b>NOTE: does not added classes loaded by other class loaders, the bootstrap
	 * class path is not added either.</b>
	 * 
	 * @return New FluidClassPool
	 */
	public static FluidClassPool create() {
		FluidClassPool pool = implementation.newInstance();
		pool.addSource(new LoaderClassSourceProvider(ClassLoader.getSystemClassLoader()));
		pool.addDefaultCp();
		return pool;
	}

	/**
	 * Create an empty class pool
	 * 
	 * @return New FluidClassPool
	 */
	public static FluidClassPool createEmpty() {
		return implementation.newInstance();
	}

	/**
	 * Add source URLs, can be a jar or class folder
	 * 
	 * @param source Source URL
	 */
	public void addSource(URL source) {
		addSource(new URLClassSourceProvider(source));
	}

	/**
	 * Add source files, can be a jar or class folder
	 * 
	 * @param source Source file
	 */
	public void addSource(File source) {
		addSource(new FileClassSourceProvider(source));
	}

	/**
	 * Adds sources based on class instances, finds the folder/jar containing the
	 * class and adds it as a file
	 * 
	 * @param source Source class
	 */
	public void addSource(Class<?> source) {
		URL u = source.getProtectionDomain().getCodeSource().getLocation();
		try {
			File sourceF = new File(u.toURI());
			addSource(sourceF);
		} catch (URISyntaxException e) {
			addSource(u);
		}
	}

	/**
	 * Add source providers
	 * 
	 * @param provider Source provider
	 */
	public void addSource(IClassSourceProvider<?> provider) {
		boolean present = false;
		ArrayList<IClassSourceProvider<?>> backupSources = new ArrayList<IClassSourceProvider<?>>(sources);
		switch (provider.getComparisonMethod()) {
		case OBJECT_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.OBJECT_EQUALS
					&& t.providerObject().equals(provider.providerObject()));
		case CLASS_EQUALS:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_EQUALS && t.providerObject()
							.getClass().getTypeName().equals(provider.providerObject().getClass().getTypeName()));
		case CLASS_ISASSIGNABLE:
			present = backupSources.stream()
					.anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.CLASS_ISASSIGNABLE
							&& t.providerObject().getClass().isAssignableFrom(provider.providerObject().getClass()));
		case LOGICAL_EQUALS:
			present = backupSources.stream().anyMatch(t -> t.getComparisonMethod() == ComparisonMethod.LOGICAL_EQUALS
					&& t.providerObject() == provider.providerObject());
		}

		if (!present) {
			sources.add(provider);
		}
	}

	/**
	 * Loads a class from a byte array
	 * 
	 * @param bytecode Bytecode to convert to a ClassNode
	 * @param name     Class name, renames the class that has been created
	 * @return ClassNode created with the given bytecode, returns existing if one is
	 *         found.
	 */
	public ClassNode readClass(String name, byte[] bytecode) {
		name = name.replaceAll("\\.", "/");

		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				return cls.node;
			}
		}

		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(bytecode);
		reader.accept(node, 0);

		fixLocalVariableNames(node);
		removeNullable(node);
		node.name = name;

		ClassEntry entry = new ClassEntry();
		entry.node = node;
		entry.firstName = name.replace(".", "/");
		classHashes.put(name, getHash(getByteCode(entry.node)));
		classes.add(entry);
		return node;
	}

	/**
	 * Adds a classnode to the class pool
	 * 
	 * @param name Class name
	 * @param cls  Class node
	 */
	public ClassNode addClass(String name, ClassNode cls) {
		name = name.replaceAll("\\.", "/");

		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls2 : clsLstBackup) {
			if (cls2.node.name.equals(name)) {
				cls2.node = cls;
				return cls2.node;
			}
		}

		ClassEntry entry = new ClassEntry();
		entry.node = cls;
		entry.firstName = name.replace(".", "/");
		classes.add(entry);
		return cls;
	}

	/**
	 * Loads a class from a stream
	 * 
	 * @param strm The stream to read into a class
	 * @param name Class name, renames the class that has been created
	 * @return ClassNode created with the given bytecode, returns existing if one is
	 *         found.
	 * @throws IOException if the reading fails.
	 */
	public ClassNode readClass(String name, InputStream strm) throws IOException {
		name = name.replaceAll("\\.", "/");

		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name))
				return cls.node;
		}

		ClassNode node = new ClassNode();
		ClassReader reader = new ClassReader(strm);
		reader.accept(node, 0);

		fixLocalVariableNames(node);
		removeNullable(node);
		node.name = name;

		ClassEntry entry = new ClassEntry();
		entry.node = node;
		entry.firstName = name.replace(".", "/");
		classes.add(entry);
		return node;
	}

	/**
	 * Get a class node by name, loads it if possible, returns a loaded class if
	 * present.
	 * 
	 * @param name Name of the class to get
	 * @return ClassNode
	 * @throws ClassNotFoundException if the class cannot be found.
	 */
	public ClassNode getClassNode(String name) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		final String nameFinal = name;

		Optional<ClassEntry> oldN = new ArrayList<ClassEntry>(classes).stream()
				.filter(t -> t.firstName.equals(nameFinal)).findFirst();
		if (!oldN.isEmpty())
			return oldN.get().node;

		for (ClassEntry cls : new ArrayList<ClassEntry>(classes)) {
			if (cls.node.name.equals(name))
				return cls.node;
		}

		ArrayList<IClassSourceProvider<?>> backupProviders = new ArrayList<IClassSourceProvider<?>>(sources);
		for (IClassSourceProvider<?> provider : backupProviders) {
			try {
				ClassNode node = new ClassNode();
				InputStream strm = provider.getStream(name);
				if (strm == null)
					continue;

				ClassReader reader = new ClassReader(strm);
				reader.accept(node, 0);
				fixLocalVariableNames(node);
				removeNullable(node);
				strm.close();

				ClassEntry entry = new ClassEntry();
				entry.node = node;
				entry.firstName = name.replace(".", "/");
				classes.add(entry);

				return node;
			} catch (IOException ex) {
			}
		}
		throw new ClassNotFoundException("Cannot find class " + name.replaceAll("/", "."));
	}

	/**
	 * Convert a class to bytecode
	 * 
	 * @param name Class name
	 * @return Byte array
	 */
	public byte[] getByteCode(String name) {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name))
				return getByteCode(cls.node);
		}
		return null;
	}

	/**
	 * Convert a class to bytecode
	 * 
	 * @param node Class node
	 * @return Byte array
	 */
	public byte[] getByteCode(ClassNode node) {
		ClassWriter writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Imports all classes from all source providers
	 */
	public void importAllSources() {
		// Load all sources
		for (IClassSourceProvider<?> source : sources) {
			source.importAll(this);
		}
	}

	/**
	 * Imports classes into the pool from an archive, loading all classes of the
	 * archive
	 * 
	 * @param strm Zip input stream
	 * @throws IOException If importing the archive fails
	 */
	public void importArchive(ZipInputStream strm) throws IOException {
		ZipEntry entry = strm.getNextEntry();
		while (entry != null) {
			String path = entry.getName().replace("\\", "/");
			if (path.endsWith(".class")) {
				String name = path;
				name = name.substring(0, name.lastIndexOf(".class"));

				final String nameFinal = name;
				if (!this.classes.stream()
						.anyMatch(t -> t.node.name.equals(nameFinal) || t.firstName.equals(nameFinal))) {
					if (includedclasses.size() == 0 || includedclasses.contains(nameFinal))
						readClass(name, strm);
				}
			}
			entry = strm.getNextEntry();
		}
	}

	/**
	 * Removes a class from the pool
	 * 
	 * @param name Class name
	 * @throws ClassNotFoundException If the class cannot be found.
	 */
	public void detachClass(String name) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				classes.remove(cls);
				return;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}

	private void addDefaultCp() {
		for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
			if (path.equals("."))
				continue;

			File f = new File(path);
			this.addSource(f);
		}
	}

	@Override
	public void close() throws IOException {
		sources.clear();
		classes.clear();
	}

	// Re-generates the variable names if they have unusable names
	private static void fixLocalVariableNames(ClassNode cls) {
		for (MethodNode meth : cls.methods) {
			if (meth.localVariables != null) {
				int varIndex = 0;
				for (LocalVariableNode var : meth.localVariables) {
					if (var.name != null && !var.name.matches("^[A-Za-z0-9_$]+$")) {
						String nm = "var" + var.index;
						while (true) {
							String nameF = nm;
							if (meth.localVariables.stream().anyMatch(t -> t.name.equals(nameF)))
								nm = "var" + var.index + "x" + varIndex++;
							else
								break;
						}
						var.name = nm;
					}
					varIndex++;
				}
			}
		}
	}

	/**
	 * Reads the bytecode into an existing class
	 * 
	 * @param name     Class name
	 * @param bytecode Bytecode to import
	 * @return New class node
	 * @throws ClassNotFoundException
	 */
	public ClassNode rewriteClass(String name, byte[] bytecode) throws ClassNotFoundException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		if (classHashes.containsKey(name) && classHashes.get(name).equals(getHash(bytecode))) {
			for (ClassEntry cls : clsLstBackup) {
				if (cls.node.name.equals(name)) {
					return cls.node;
				}
			}
		}
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				cls.node = new ClassNode();
				ClassReader reader = new ClassReader(bytecode);
				reader.accept(cls.node, 0);
				fixLocalVariableNames(cls.node);
				removeNullable(cls.node);
				classHashes.put(name, getHash(getByteCode(cls.node)));
				return cls.node;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}

	private void removeNullable(ClassNode node) {
		if (node.visibleAnnotations != null) {
			for (AnnotationNode nd : new ArrayList<AnnotationNode>(node.visibleAnnotations)) {
				if (Fluid.parseDescriptor(nd.desc).equals("javax.annotation.Nullable"))
					node.visibleAnnotations.remove(nd);
			}
		}
		if (node.invisibleAnnotations != null) {
			for (AnnotationNode nd : new ArrayList<AnnotationNode>(node.invisibleAnnotations)) {
				if (Fluid.parseDescriptor(nd.desc).equals("javax.annotation.Nullable"))
					node.invisibleAnnotations.remove(nd);
			}
		}
		if (node.visibleTypeAnnotations != null) {
			for (AnnotationNode nd : new ArrayList<AnnotationNode>(node.visibleTypeAnnotations)) {
				if (Fluid.parseDescriptor(nd.desc).equals("javax.annotation.Nullable"))
					node.visibleAnnotations.remove(nd);
			}
		}
		if (node.invisibleTypeAnnotations != null) {
			for (AnnotationNode nd : new ArrayList<AnnotationNode>(node.invisibleTypeAnnotations)) {
				if (Fluid.parseDescriptor(nd.desc).equals("javax.annotation.Nullable"))
					node.invisibleAnnotations.remove(nd);
			}
		}

		for (FieldNode nd : node.fields) {
			if (nd.visibleAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.visibleAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.visibleAnnotations.remove(anno);
				}
			}
			if (nd.invisibleAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.invisibleAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.invisibleAnnotations.remove(anno);
				}
			}
			if (nd.visibleTypeAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.visibleTypeAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.visibleAnnotations.remove(anno);
				}
			}
			if (nd.invisibleTypeAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.invisibleTypeAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.invisibleAnnotations.remove(anno);
				}
			}
		}

		for (MethodNode nd : node.methods) {
			if (nd.visibleParameterAnnotations != null) {
				Stream.of(nd.visibleParameterAnnotations).forEach(t -> {
					if (t != null)
						for (AnnotationNode anno : new ArrayList<AnnotationNode>(t)) {
							if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
								t.remove(anno);
						}
				});
			}
			if (nd.invisibleParameterAnnotations != null) {
				Stream.of(nd.invisibleParameterAnnotations).forEach(t -> {
					if (t != null)
						for (AnnotationNode anno : new ArrayList<AnnotationNode>(t)) {
							if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
								t.remove(anno);
						}
				});
			}
			if (nd.visibleLocalVariableAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.visibleLocalVariableAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.visibleLocalVariableAnnotations.remove(anno);
				}
			}
			if (nd.invisibleLocalVariableAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.invisibleLocalVariableAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.invisibleLocalVariableAnnotations.remove(anno);
				}
			}
			if (nd.visibleAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.visibleAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.visibleAnnotations.remove(anno);
				}
			}
			if (nd.invisibleAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.invisibleAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.invisibleAnnotations.remove(anno);
				}
			}
			if (nd.visibleTypeAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.visibleTypeAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.visibleAnnotations.remove(anno);
				}
			}
			if (nd.invisibleTypeAnnotations != null) {
				for (AnnotationNode anno : new ArrayList<AnnotationNode>(nd.invisibleTypeAnnotations)) {
					if (Fluid.parseDescriptor(anno.desc).equals("javax.annotation.Nullable"))
						nd.invisibleAnnotations.remove(anno);
				}
			}
		}
	}

	/**
	 * Reads the bytecode into an existing class
	 * 
	 * @param name  Class name
	 * @param input Bytecode stream to import
	 * @return New class node
	 * @throws ClassNotFoundException If the class cannot be found
	 * @throws IOException            If reading fails
	 */
	public ClassNode rewriteClass(String name, InputStream input) throws ClassNotFoundException, IOException {
		name = name.replaceAll("\\.", "/");
		ArrayList<ClassEntry> clsLstBackup = new ArrayList<ClassEntry>(classes);
		for (ClassEntry cls : clsLstBackup) {
			if (cls.node.name.equals(name)) {
				cls.node = new ClassNode();
				ClassReader reader = new ClassReader(input);
				reader.accept(cls.node, 0);
				fixLocalVariableNames(cls.node);
				removeNullable(cls.node);
				classHashes.put(name, getHash(getByteCode(cls.node)));
				return cls.node;
			}
		}
		throw new ClassNotFoundException("Could not find class " + name.replaceAll("/", "."));
	}

	private String getHash(byte[] data) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] sha = digest.digest(data);
			StringBuilder result = new StringBuilder();
			for (byte aByte : sha) {
				result.append(String.format("%02x", aByte));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException e) {
		}
		return null;
	}

}
