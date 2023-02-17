package org.asf.cyan.fluid.remapping;

import java.util.ArrayList;
import java.util.Arrays;

public class SimpleMappings extends Mapping<SimpleMappings> {

	private boolean allowSupertypeFinalOverride = false;

	public void setAllowSupertypeFinalOverride(boolean value) {
		allowSupertypeFinalOverride = value;
	}

	@Override
	public boolean allowSupertypeFinalOverride() {
		return allowSupertypeFinalOverride;
	}

	public SimpleMappings() {
	}

	/**
	 * Create a class mapping
	 */
	public SimpleMappings(String name, String obfuscated) {
		this.name = name;
		this.obfuscated = obfuscated;
		this.mappingType = MAPTYPE.CLASS;
	}

	/**
	 * Create a class mapping
	 */
	public SimpleMappings(String name, String obfuscated, Mapping<?>[] mappings) {
		this.name = name;
		this.obfuscated = obfuscated;
		this.mappingType = MAPTYPE.CLASS;
		this.mappings = mappings;
	}

	public SimpleMappings(Mapping<?>[] mappings) {
		this.mappings = mappings;
	}

	public SimpleMappings getClassMapping(String name) {
		for (Mapping<?> mapping : mappings) {
			if (mapping instanceof SimpleMappings && mapping.mappingType == MAPTYPE.CLASS
					&& mapping.name.equals(name)) {
				return (SimpleMappings) mapping;
			}
		}

		return null;
	}

	public String[] getObfuscatedClassNames() {
		ArrayList<String> classes = new ArrayList<String>();

		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS) {
				classes.add(mapping.obfuscated);
			}
		}

		return classes.toArray(t -> new String[t]);
	}

	public String[] getDeobfuscatedClassNames() {
		ArrayList<String> classes = new ArrayList<String>();

		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS) {
				classes.add(mapping.name);
			}
		}

		return classes.toArray(t -> new String[t]);
	}

	public String mapClassToObfuscation(String name) {
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS && mapping.name.equals(name)) {
				return mapping.obfuscated;
			}
		}

		return name;
	}

	public String mapClassToDeobfuscation(String obfus) {
		for (Mapping<?> mapping : mappings) {
			if (mapping.mappingType == MAPTYPE.CLASS && mapping.obfuscated.equals(obfus)) {
				return mapping.name;
			}
		}

		return obfus;
	}

	public SimpleMappings createClassMapping(String name, String obfuscated) {
		return createClassMapping(name, obfuscated, new Mapping<?>[0]);
	}

	public SimpleMappings createClassMapping(String name, String obfuscated, Mapping<?>[] childMappings) {
		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.CLASS;
		ArrayList<Mapping<?>> mappingsL = new ArrayList<Mapping<?>>();
		mappingsL.addAll(Arrays.asList(this.mappings));
		mappingsL.add(mappings);
		this.mappings = mappingsL.toArray(t -> new Mapping<?>[t]);
		return mappings;
	}

	public void add(Mapping<?> map) {
		ArrayList<Mapping<?>> mappingsL = new ArrayList<Mapping<?>>();
		mappingsL.addAll(Arrays.asList(this.mappings));
		mappingsL.add(map);
		this.mappings = mappingsL.toArray(t -> new Mapping<?>[t]);
	}

	/**
	 * Create method mappings, can only be done with class mappings
	 */
	public SimpleMappings createMethod(String name, String obfuscated, String returnType, String... argumentTypes) {
		if (mappingType != MAPTYPE.CLASS) {
			throw new RuntimeException("Cannot add method to non-class mappings");
		}

		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.METHOD;
		mappings.argumentTypes = argumentTypes;
		mappings.type = returnType;
		ArrayList<Mapping<?>> mappingsL = new ArrayList<Mapping<?>>();
		mappingsL.addAll(Arrays.asList(this.mappings));
		mappingsL.add(mappings);
		this.mappings = mappingsL.toArray(t -> new Mapping<?>[t]);
		return mappings;
	}

	/**
	 * Create field mappings, can only be done with class mappings
	 */
	public SimpleMappings createField(String name, String obfuscated, String type) {
		if (mappingType != MAPTYPE.CLASS) {
			throw new RuntimeException("Cannot add method to non-class mappings");
		}

		SimpleMappings mappings = new SimpleMappings();
		mappings.name = name;
		mappings.obfuscated = obfuscated;
		mappings.mappingType = MAPTYPE.PROPERTY;
		mappings.type = type;
		ArrayList<Mapping<?>> mappingsL = new ArrayList<Mapping<?>>();
		mappingsL.addAll(Arrays.asList(this.mappings));
		mappingsL.add(mappings);
		this.mappings = mappingsL.toArray(t -> new Mapping<?>[t]);
		return mappings;
	}
}
