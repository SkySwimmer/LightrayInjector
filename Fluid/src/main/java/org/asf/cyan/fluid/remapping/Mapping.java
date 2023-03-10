package org.asf.cyan.fluid.remapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.asf.cyan.fluid.Fluid;

public class Mapping<T extends Mapping<T>> {

	protected Mapping() {
		super();
	}

	/**
	 * If this returns true, createTargetMap will allow supertypes to override
	 * methods in classes.
	 */
	public boolean allowSupertypeFinalOverride() {
		return false;
	}

	public MAPTYPE mappingType = MAPTYPE.TOPLEVEL;

	public String type = null;
	public String[] argumentTypes = new String[0];
	public String name = null;
	public String mappingsVersion = null;
	public String obfuscated = null;
	public Mapping<?>[] mappings = new Mapping<?>[0];

	public MAPTYPE getMappingType() {
		return mappingType;
	}

	/**
	 * Parse (Mojang) ProGuard obfuscation mappings into this configuration
	 * 
	 * @param mappings Jar mappings input text
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public <M extends Mapping<M>> T parseProGuardMappings(String mappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replace("\r", "");
		mappings = mappings.replace("\t", "    ");
		ArrayList<Mapping<M>> mappingsLst = new ArrayList<Mapping<M>>();
		ArrayList<Mapping<M>> mappingsLstFmp = new ArrayList<Mapping<M>>();
		Mapping<M> mp = null;
		String[] lines = mappings.split("\n");
		for (String line : lines) {
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("    ")) {
				mappingsLstFmp.add(new Mapping<T>().parseProGuardEntry(line.substring(4)));
			} else {
				if (mp != null) {
					mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
					mappingsLst.add(mp);
					mappingsLstFmp.clear();
					mp = null;
				}
				mp = new Mapping<M>().parseProGuardEntry(line);
			}
		}
		if (mp != null) {
			mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
			mappingsLst.add(mp);
			mappingsLstFmp.clear();
			mp = null;
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	private <M extends Mapping<M>> M parseProGuardEntry(String entry) {
		mappingType = MAPTYPE.PROPERTY;
		if (entry.endsWith(":")) {
			entry = entry.substring(0, entry.length() - 1);
			mappingType = MAPTYPE.CLASS;
		}
		String output = entry.substring(entry.indexOf(" -> ") + 4);
		String input = entry.substring(0, entry.indexOf(" -> "));
		if (input.matches("^([0-9]+:[0-9]+:)?[A-Za-z0-9.$_\\[\\]]+ .*\\(.*\\).*$")) {
			mappingType = MAPTYPE.METHOD;
			Matcher m = Pattern.compile("^([0-9]+:[0-9]+:)?([A-Za-z0-9.$_\\[\\]]+) (.*)").matcher(input);
			m.matches();
			if (m.group(1) != null)
				input = input.substring(m.group(1).length());
			String arguments = input.substring(input.indexOf("(") + 1);
			arguments = arguments.substring(0, arguments.lastIndexOf(")"));

			if (!arguments.isEmpty()) {
				arguments = arguments.replace(", ", ",");
				argumentTypes = arguments.split(",");
			}

			input = input.substring(0, input.indexOf("("));
		}

		switch (mappingType) {
		case CLASS:
			name = input;
			obfuscated = output;
			break;
		default:
			name = input.substring(input.indexOf(" ") + 1);
			type = input.substring(0, input.indexOf(" "));
			obfuscated = output;
		}

		return (M) this;
	}

	/**
	 * Parse TSRG mappings into this configuration, WARNING: NO TYPE SUPPORT FOR
	 * PROPERTIES
	 * 
	 * @param mappings Jar mappings input text
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public <M extends Mapping<M>> T parseTSRGMappings(String mappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replace("\r", "");
		mappings = mappings.replace("\t", "    ");
		ArrayList<Mapping<M>> mappingsLst = new ArrayList<Mapping<M>>();
		ArrayList<Mapping<M>> mappingsLstFmp = new ArrayList<Mapping<M>>();
		Mapping<M> mp = null;
		String[] lines = mappings.split("\n");
		int version = 1;
		if (lines.length > 0) {
			if (lines[0].split(" ").length > 2) {
				String TSRGversion = lines[0].split(" ")[0];
				if (TSRGversion.equals("tsrg2")) {
					version = 2;
				} else {
					throw new RuntimeException("Unsupported TSRG version: " + TSRGversion);
				}
			}
		}

		boolean firstLine = true;
		for (String line : lines) {
			if (firstLine && version > 1) {
				firstLine = false;
				continue;
			}
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("    ")) {
				if (version == 2) {
					if (line.startsWith("        "))
						continue;
				}
				mappingsLstFmp.add(new Mapping<T>().parseTSRGEntry(line.substring(4), false, version));
			} else {
				if (mp != null) {
					mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
					mappingsLst.add(mp);
					mappingsLstFmp.clear();
					mp = null;
				}
				mp = new Mapping<M>().parseTSRGEntry(line, true, version);
			}
		}
		if (mp != null) {
			mp.mappings = mappingsLstFmp.toArray(t -> new Mapping[t]);
			mappingsLst.add(mp);
			mappingsLstFmp.clear();
			mp = null;
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		for (Mapping<?> m : mappingsLst) {
			for (Mapping<?> t : m.mappings) {
				if (t.mappingType.equals(MAPTYPE.METHOD)) {
					String[] types = t.argumentTypes;
					if (types.length != 0) {
						int ind = 0;
						for (String type : types) {
							String tSuffix = "";
							if (type.contains("[]")) {
								tSuffix = type.substring(type.indexOf("["));
								type = type.substring(0, type.indexOf("["));
							}
							Mapping<?> map = mapClassToMapping(type, t2 -> true, true);
							if (map != null)
								types[ind++] = map.name + tSuffix;
							else
								ind++;
						}
						t.argumentTypes = types;
					}
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				}
			}
		}

		this.mappings = mappingsLst.toArray(t -> new Mapping[t]);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	private <M extends Mapping<M>> M parseTSRGEntry(String entry, boolean isClass, int tsrgVersion) {
		mappingType = (isClass ? MAPTYPE.CLASS : MAPTYPE.PROPERTY);
		String output = entry.substring(entry.indexOf(" ") + 1);
		String input = entry.substring(0, entry.indexOf(" "));
		if (output.matches(TsrgFormats.forVersion(tsrgVersion))) {
			mappingType = MAPTYPE.METHOD;
			Matcher m = Pattern.compile(TsrgFormats.forVersionMatcher(tsrgVersion)).matcher(output);
			m.matches();
			String arguments = m.group(1);

			String[] argumentTypes = Fluid.parseMultipleDescriptors(arguments);
			String returnT = Fluid.parseDescriptor(m.group(2));

			type = returnT;
			output = m.group(3);
			this.argumentTypes = argumentTypes;
		} else {
			if (tsrgVersion == 2) {
				output = output.substring(0, output.lastIndexOf(" "));
			}
		}

		name = output;
		obfuscated = input;

		if (mappingType.equals(MAPTYPE.CLASS) && name.contains("/"))
			name = name.replace("/", ".");
		if (mappingType.equals(MAPTYPE.CLASS) && obfuscated.contains("/"))
			obfuscated = obfuscated.replace("/", ".");

		return (M) this;
	}

	private static class TsrgFormats {
		public static String tsrg1 = "^\\(.*\\).* [A-Za-z0-9.$_\\[\\]]+$";
		public static String trsg1_mtc = "^\\((.*)\\)(.*) ([A-Za-z0-9.$_\\[\\]]+)$";

		public static String tsrg2 = "^\\(.*\\).* [A-Za-z0-9.$_\\[\\]<>]+ [0-9]+$";
		public static String trsg2_mtc = "^\\((.*)\\)(.*) ([A-Za-z0-9.$_\\[\\]<>]+) [0-9]+$";

		public static String forVersion(int version) {
			switch (version) {
			case 1:
				return tsrg1;
			case 2:
				return tsrg2;
			default:
				return null;
			}
		}

		public static String forVersionMatcher(int version) {
			switch (version) {
			case 1:
				return trsg1_mtc;
			case 2:
				return trsg2_mtc;
			default:
				return null;
			}
		}
	}

	private class Target {
		public String in = "";
		public String out = "";
		public String type = "";
		public String[] types = new String[0];
		public ArrayList<Target> fields = new ArrayList<Target>();
		public ArrayList<Target> methods = new ArrayList<Target>();
	}

	private class TargetMap extends HashMap<String, Target> {
		private static final long serialVersionUID = 7415810166801904059L;
	}

	/**
	 * Parse Tiny V1 mappings into this configuration.
	 * 
	 * @param mappings         Jar mappings input text
	 * @param inputClassifier  Input classifier to use (obfuscated)
	 * @param outputClassifier Output classifier to use (deobfuscated)
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public T parseTinyV1Mappings(String mappings, String inputClassifier, String outputClassifier) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replace("\r", "");
		String[] lines = mappings.split("\n");

		TargetMap mp = new TargetMap();

		int id1 = 0;
		int id2 = 0;

		int index = 0;
		String[] tagsHeader = lines[0].split("\t");
		for (String str : tagsHeader) {
			if (str.equals(inputClassifier)) {
				id1 = index;
			} else if (str.equals(outputClassifier)) {
				id2 = index;
			}
			index++;
		}

		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			String tags[] = line.split("\t");

			switch (tags[0]) {
			case "CLASS":
				Target t = new Target();
				t.in = tags[id1];
				t.out = tags[id2];
				mp.put(tags[id1], t);
				break;
			case "FIELD":
				String clsF = tags[1];
				String returnTypeF = tags[2];
				Target field = new Target();
				field.in = tags[2 + id1];
				field.out = tags[2 + id2];
				field.type = Fluid.parseDescriptor(returnTypeF);

				Target cF = mp.get(clsF);
				cF.fields.add(field);
				mp.put(cF.in, cF);
				break;
			case "METHOD":
				String clsM = tags[1];
				String descriptor = tags[2];
				String returnTypeM = Fluid.parseDescriptor(descriptor.substring(descriptor.lastIndexOf(")") + 1));
				String types = descriptor.substring(0, descriptor.lastIndexOf(")"));
				types = types.substring(1);

				String[] argTypes = Fluid.parseMultipleDescriptors(types);

				Target method = new Target();
				method.in = tags[2 + id1];
				method.out = tags[2 + id2];
				method.types = argTypes;
				method.type = returnTypeM;

				Target cM = mp.get(clsM);
				cM.methods.add(method);
				mp.put(cM.in, cM);
				break;
			default:
				break;
			}
		}

		this.mappings = mp.values().stream().map(t -> {
			@SuppressWarnings("rawtypes")
			Mapping<?> map = new Mapping();

			map.type = t.out.replace("/", ".");
			map.mappingType = MAPTYPE.CLASS;
			map.obfuscated = t.in.replace("/", ".");
			map.name = t.out.replace("/", ".");

			ArrayList<Mapping<?>> localmappings = new ArrayList<Mapping<?>>();
			for (Target meth : t.methods) {
				@SuppressWarnings("rawtypes")
				Mapping<?> methmap = new Mapping();

				methmap.argumentTypes = meth.types;
				methmap.name = meth.out;
				methmap.obfuscated = meth.in;
				methmap.type = meth.type.replace("/", ".");
				methmap.mappingType = MAPTYPE.METHOD;

				localmappings.add(methmap);
			}
			for (Target field : t.fields) {
				@SuppressWarnings("rawtypes")
				Mapping<?> fieldmap = new Mapping();

				fieldmap.name = field.out;
				fieldmap.obfuscated = field.in;
				fieldmap.type = field.type.replace("/", ".");
				fieldmap.mappingType = MAPTYPE.PROPERTY;

				localmappings.add(fieldmap);
			}

			map.mappings = localmappings.toArray(t2 -> new Mapping<?>[t2]);
			return map;
		}).toArray(t -> new Mapping<?>[t]);

		for (Mapping<?> m : this.mappings) {
			for (Mapping<?> t : m.mappings) {
				if (t.mappingType.equals(MAPTYPE.METHOD)) {
					String[] types = t.argumentTypes;
					if (types.length != 0) {
						int ind = 0;
						for (String type : types) {
							String tSuffix = "";
							if (type.contains("[]")) {
								tSuffix = type.substring(type.indexOf("["));
								type = type.substring(0, type.indexOf("["));
							}
							Mapping<?> map = mapClassToMapping(type, t2 -> true, true);
							if (map != null)
								types[ind++] = map.name + tSuffix;
							else
								ind++;
						}
						t.argumentTypes = types;
					}
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				} else if (t.mappingType.equals(MAPTYPE.PROPERTY)) {
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				}
			}
		}

		return (T) this;
	}

	/**
	 * Parse Tiny V2 mappings into this configuration.
	 * 
	 * @param mappings         Tiny mappings input text
	 * @param inputClassifier  Input classifier to use (obfuscated)
	 * @param outputClassifier Output classifier to use (deobfuscated)
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public T parseTinyV2Mappings(String mappings, String inputClassifier, String outputClassifier) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replace("\r", "");
		String[] lines = mappings.split("\n");

		TargetMap mp = new TargetMap();

		int id1 = 0;
		int id2 = 0;

		int index = 0;

		int count = lines.length;
		for (String line : lines) {
			if (line.startsWith("#") || line.isEmpty()) {
				count--;
			}
		}
		String[] newlines = new String[count];
		count = 0;
		for (String line : lines)
			if (!line.startsWith("#") && !line.isEmpty()) {
				newlines[count++] = line;
			}
		lines = newlines;

		String[] tagsHeader = lines[0].split("\t");
		if (tagsHeader[0].equals("v1")) {
			LogManager.getLogger().warn(
					"WARNING: Called the Tiny 2.0 parser for Tiny v1 mappings! The request is delegated to the legacy parser, this behaviour will be removed in Cyan 2.0!"); // TODO
			return parseTinyV1Mappings(mappings, inputClassifier, outputClassifier);
		}
		for (String str : tagsHeader) {
			if (str.equals(inputClassifier)) {
				id1 = index;
			} else if (str.equals(outputClassifier)) {
				id2 = index;
			}
			index++;
		}

		Target last = null;
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			String tags[] = line.split("\t");
			if (tags[0].isEmpty()) {
				if (tags[1].equals("c"))
					continue;
				tags = Arrays.copyOfRange(tags, 1, tags.length);
			}

			switch (tags[0]) {
			case "c":
				Target t = new Target();
				t.in = tags[id1 - 2];
				t.out = tags[id2 - 2];
				last = t;

				mp.put(t.in, t);
				break;
			case "f":
				String returnTypeF = tags[1];
				Target field = new Target();
				field.in = tags[id1 - 1];
				field.out = tags[id2 - 1];
				field.type = Fluid.parseDescriptor(returnTypeF);

				last.fields.add(field);
				break;
			case "m":
				String descriptor = tags[1];
				String returnType = Fluid.parseDescriptor(descriptor.substring(descriptor.lastIndexOf(")") + 1));
				String types = descriptor.substring(0, descriptor.lastIndexOf(")"));
				types = types.substring(1);

				String[] argTypes = Fluid.parseMultipleDescriptors(types);

				Target method = new Target();
				method.in = tags[id1 - 1];
				method.out = tags[id2 - 1];
				method.types = argTypes;
				method.type = returnType;

				last.methods.add(method);
				break;
			default:
				break;
			}
		}

		this.mappings = mp.values().stream().map(t -> {
			@SuppressWarnings("rawtypes")
			Mapping<?> map = new Mapping();

			map.mappingType = MAPTYPE.CLASS;
			map.obfuscated = t.in.replace("/", ".");
			map.name = t.out.replace("/", ".");

			ArrayList<Mapping<?>> localmappings = new ArrayList<Mapping<?>>();
			for (Target meth : t.methods) {
				@SuppressWarnings("rawtypes")
				Mapping<?> methmap = new Mapping();

				methmap.argumentTypes = meth.types;
				methmap.name = meth.out;
				methmap.obfuscated = meth.in;
				methmap.type = meth.type;
				methmap.mappingType = MAPTYPE.METHOD;

				localmappings.add(methmap);
			}
			for (Target field : t.fields) {
				@SuppressWarnings("rawtypes")
				Mapping<?> fieldmap = new Mapping();

				fieldmap.name = field.out;
				fieldmap.obfuscated = field.in;
				fieldmap.type = field.type;
				fieldmap.mappingType = MAPTYPE.PROPERTY;

				localmappings.add(fieldmap);
			}

			map.mappings = localmappings.toArray(t2 -> new Mapping<?>[t2]);
			return map;
		}).toArray(t -> new Mapping<?>[t]);

		for (Mapping<?> m : this.mappings) {
			for (Mapping<?> t : m.mappings) {
				if (t.mappingType.equals(MAPTYPE.METHOD)) {
					String[] types = t.argumentTypes;
					if (types.length != 0) {
						int ind = 0;
						for (String type : types) {
							String tSuffix = "";
							if (type.contains("[]")) {
								tSuffix = type.substring(type.indexOf("["));
								type = type.substring(0, type.indexOf("["));
							}
							Mapping<?> map = mapClassToMapping(type, t2 -> true, true);
							if (map != null)
								types[ind++] = map.name + tSuffix;
							else
								ind++;
						}
						t.argumentTypes = types;
					}
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				} else if (t.mappingType.equals(MAPTYPE.PROPERTY)) {
					Mapping<?> map2 = mapClassToMapping(t.type, t2 -> true, true);
					if (map2 != null)
						t.type = map2.name;
				}
			}
		}

		return (T) this;
	}

	/**
	 * Converts CSRG mappings to CCFG
	 * 
	 * @param mappings CSRG content to parse
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public T parseCSRGMappings(String mappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		mappings = mappings.replace("\r", "");
		String[] mappingsLines = mappings.split("\n");

		HashMap<String, Mapping<?>> mappingsCache = new HashMap<String, Mapping<?>>();
		for (String line : mappingsLines) {
			if (line.startsWith("#"))
				continue;

			String[] entries = line.split(" ");

			String obfus;
			String deobf;
			String owner;
			switch (entries.length) {
			case 2:
				// Class

				obfus = entries[0].replace(".", "").replace("/", ".");
				deobf = entries[1].replace(".", "").replace("/", ".");

				Mapping<?> clsMapping = new SimpleMappings();
				if (mappingsCache.containsKey(deobf))
					clsMapping = mappingsCache.get(deobf);
				clsMapping.mappingType = MAPTYPE.CLASS;
				clsMapping.obfuscated = obfus;
				clsMapping.name = deobf;
				mappingsCache.put(deobf, clsMapping);

				break;
			case 3:
				// Field

				owner = entries[0].replace(".", "").replace("/", ".");
				obfus = entries[1].replace(".", "").replace("/", ".");
				deobf = entries[2].replace(".", "").replace("/", ".");

				Mapping<?> fOwnerMap = mappingsCache.get(owner);
				if (fOwnerMap == null) {
					fOwnerMap = new SimpleMappings();
					fOwnerMap.mappingType = MAPTYPE.CLASS;
					fOwnerMap.obfuscated = owner;
					fOwnerMap.name = owner;
					mappingsCache.put(owner, fOwnerMap);
				}

				Mapping<?> fieldMap = new SimpleMappings();
				fieldMap.mappingType = MAPTYPE.PROPERTY;
				fieldMap.obfuscated = obfus;
				fieldMap.name = deobf;
				fOwnerMap.mappings = appendMapping(fieldMap, fOwnerMap.mappings);

				break;
			case 4:
				// Method

				owner = entries[0].replace(".", "").replace("/", ".");
				obfus = entries[1].replace(".", "").replace("/", ".");
				String desc = entries[2];
				deobf = entries[3].replace(".", "").replace("/", ".");

				Mapping<?> mOwnerMap = mappingsCache.get(owner);
				if (mOwnerMap == null) {
					mOwnerMap = new SimpleMappings();
					mOwnerMap.mappingType = MAPTYPE.CLASS;
					mOwnerMap.obfuscated = owner;
					mOwnerMap.name = owner;
					mappingsCache.put(owner, mOwnerMap);
				}

				Mapping<?> methMap = new SimpleMappings();
				methMap.mappingType = MAPTYPE.METHOD;
				methMap.obfuscated = obfus;
				methMap.name = deobf;

				methMap.argumentTypes = Fluid.parseMultipleDescriptors(desc.substring(1, desc.lastIndexOf(")")));
				methMap.type = Fluid.parseDescriptor(desc.substring(desc.lastIndexOf(")") + 1));

				mOwnerMap.mappings = appendMapping(methMap, mOwnerMap.mappings);

				break;
			default:
				break;
			}
		}
		this.mappings = mappingsCache.values().toArray(t -> new Mapping[t]);

		return (T) this;
	}

	private Mapping<?>[] appendMapping(Mapping<?> append, Mapping<?>[] old) {
		Mapping<?>[] newMaps = new Mapping[old.length + 1];
		for (int i = 0; i < old.length; i++)
			newMaps[i] = old[i];
		newMaps[newMaps.length - 1] = append;
		return newMaps;
	}

	/**
	 * Parse (Spigot) hybrid SRG/CSRG mappings into this configuration.
	 * 
	 * @param fallback             The mappings to use for additional information
	 * @param classMappingsCsrg    Input class mappings (CSRG)
	 * @param memberMappingsCsrg   Input member mappings (CSRG)
	 * @param packageMappingsSrg   Input package mappings (SRG)
	 * @param finalPackageMappings Final package mappings
	 * @return Self
	 */
	@SuppressWarnings("unchecked")
	public T parseMultiMappings(Mapping<?> fallback, String classMappingsCsrg, String memberMappingsCsrg,
			String packageMappingsSrg, Map<String, String> finalPackageMappings) {
		mappingType = MAPTYPE.TOPLEVEL;
		name = null;
		type = null;
		obfuscated = null;

		classMappingsCsrg = classMappingsCsrg.replace("\r", "");
		memberMappingsCsrg = memberMappingsCsrg.replace("\r", "");
		packageMappingsSrg = packageMappingsSrg.replace("\r", "");

		String[] linesClasses = classMappingsCsrg.split("\n");
		String[] linesMembers = memberMappingsCsrg.split("\n");
		String[] linesPackages = packageMappingsSrg.split("\n");

		HashMap<String, String> packages = new HashMap<String, String>();
		for (String line : linesPackages) {
			if (line.startsWith("#") || line.isEmpty())
				continue;

			String[] entries = line.split(" ");
			String obfus = entries[0];
			String deobf = entries[1];

			if (obfus.endsWith("/"))
				obfus = obfus.substring(0, obfus.length() - 1);
			if (deobf.endsWith("/"))
				deobf = deobf.substring(0, deobf.length() - 1);

			obfus = obfus.replace(".", "");
			deobf = deobf.replace(".", "");
			obfus = obfus.replace("/", ".");
			deobf = deobf.replace("/", ".");

			packages.put(obfus, deobf);
		}

		ArrayList<String> obfusCache = new ArrayList<String>();
		HashMap<String, Mapping<?>> tempMappings = new HashMap<String, Mapping<?>>();
		HashMap<String, Mapping<?>> classes = new HashMap<String, Mapping<?>>();
		for (String line : linesClasses) {
			if (line.startsWith("#"))
				continue;

			String[] entries = line.split(" ");
			String obfus = entries[0];
			String deobf = entries[1];

			obfus = obfus.replace(".", "");
			deobf = deobf.replace(".", "");
			obfus = obfus.replace("/", ".");
			deobf = deobf.replace("/", ".");

			String obfusPackage = "";
			if (obfus.contains(".")) {
				obfusPackage = obfus.substring(0, obfus.lastIndexOf("."));
			}

			String deobfPackage = packages.getOrDefault(obfusPackage, obfusPackage);
			String deobfOld = deobf;

			if (deobf.contains(".")) {
				deobfPackage = deobf.substring(0, deobf.lastIndexOf("."));
				deobf = deobf.substring(deobf.lastIndexOf(".") + 1);
				deobfPackage = packages.getOrDefault(deobfPackage, deobfPackage);
			}

			@SuppressWarnings("rawtypes")
			Mapping<?> map = new Mapping();

			map.mappingType = MAPTYPE.CLASS;
			map.obfuscated = obfus;
			map.name = (deobfPackage.isEmpty() ? "" : deobfPackage + ".") + deobf;

			classes.put(deobfOld, map);
			tempMappings.put(map.obfuscated, map);
			obfusCache.add(obfus);
		}

		HashMap<String, String> fieldHelpers = new HashMap<String, String>();
		for (Mapping<?> clsMapping : fallback.mappings) {
			if (clsMapping.mappingType == MAPTYPE.CLASS) {
				if (!obfusCache.contains(clsMapping.obfuscated)) {
					@SuppressWarnings("rawtypes")
					Mapping<?> map = new Mapping();

					map.mappingType = MAPTYPE.CLASS;
					map.obfuscated = clsMapping.obfuscated;

					map.name = clsMapping.name;
					if (clsMapping.obfuscated.contains("$")) {
						String owner = clsMapping.obfuscated.substring(0, clsMapping.obfuscated.indexOf("$"));
						if (tempMappings.containsKey(owner)) {
							owner = tempMappings.get(owner).name;
							map.name = owner + "$"
									+ clsMapping.obfuscated.substring(clsMapping.obfuscated.indexOf("$") + 1);
						}
					}

					classes.put(map.name, map);
					tempMappings.put(map.obfuscated, map);
					obfusCache.add(clsMapping.name);
				}
				for (Mapping<?> member : clsMapping.mappings) {
					if (member.mappingType == MAPTYPE.PROPERTY) {
						String type = member.type;
						Mapping<?> mp = fallback.mapClassToMapping(type, t -> true, false);
						if (mp != null)
							type = mp.obfuscated;

						fieldHelpers.put(clsMapping.obfuscated + " " + member.obfuscated, type);
					}
				}
			}
		}

		for (String line : linesMembers) {
			if (line.startsWith("#"))
				continue;

			String[] entries = line.split(" ");
			String owner = entries[0];
			owner = owner.replace(".", "");
			owner = owner.replace("/", ".");

			String obfus = entries[1];

			Mapping<?> ownerClass = classes.get(owner);
			String type = fieldHelpers.get(ownerClass.obfuscated + " " + obfus);
			if (type != null) {
				for (Mapping<?> clsMapping : classes.values()) {
					if (clsMapping.obfuscated.equals(type)) {
						type = clsMapping.name;
						break;
					}
				}
			}

			@SuppressWarnings("rawtypes")
			Mapping<?> map = new Mapping();

			String name = "";
			if (entries.length == 3) {
				name = entries[2];
				map.mappingType = MAPTYPE.PROPERTY;
			} else {
				name = entries[3];

				String[] types = Fluid.parseMultipleDescriptors(entries[2].substring(1, entries[2].lastIndexOf(")")));
				type = Fluid.parseDescriptor(entries[2].substring(entries[2].lastIndexOf(")") + 1));

				if (classes.containsKey(type)) {
					String pkg = "";
					if (type.contains(".")) {
						pkg = type.substring(0, type.lastIndexOf("."));
						type = type.substring(type.lastIndexOf(".") + 1);
					}

					pkg = packages.getOrDefault(pkg, pkg);
					type = (pkg.isEmpty() ? "" : pkg + ".") + type;
				}

				map.argumentTypes = types;
				map.mappingType = MAPTYPE.METHOD;
			}

			if (map.argumentTypes != null && map.mappingType == MAPTYPE.METHOD) {
				for (int i = 0; i < map.argumentTypes.length; i++) {
					if (classes.containsKey(map.argumentTypes[i])) {
						String pkg = "";
						if (map.argumentTypes[i].contains(".")) {
							pkg = map.argumentTypes[i].substring(0, map.argumentTypes[i].lastIndexOf("."));
							map.argumentTypes[i] = map.argumentTypes[i]
									.substring(map.argumentTypes[i].lastIndexOf(".") + 1);
						}

						pkg = packages.getOrDefault(pkg, pkg);
						map.argumentTypes[i] = (pkg.isEmpty() ? "" : pkg + ".") + map.argumentTypes[i];
					}

				}
			}

			map.type = type;
			map.name = name;
			map.obfuscated = obfus;

			ArrayList<Mapping<?>> mappings = new ArrayList<Mapping<?>>();
			mappings.addAll(Arrays.asList(ownerClass.mappings));
			mappings.add(map);
			ownerClass.mappings = mappings.toArray(t -> new Mapping<?>[t]);
		}

		for (Mapping<?> cls : classes.values()) {
			cls.name = getNewName(cls.name, finalPackageMappings);

			for (Mapping<?> member : cls.mappings) {
				member.type = getNewName(member.type, finalPackageMappings);
				if (member.argumentTypes != null) {
					for (int i = 0; i < member.argumentTypes.length; i++) {
						member.argumentTypes[i] = getNewName(member.argumentTypes[i], finalPackageMappings);
					}
				}
			}
		}

		this.mappings = classes.values().toArray(t -> new Mapping<?>[t]);
		return (T) this;
	}

	public static String getNewName(String clsIn, Map<String, String> finalPackageMappings) {
		for (String packageIn : finalPackageMappings.keySet()) {
			String packageOut = finalPackageMappings.get(packageIn);
			boolean match = false;

			String pkg = "";
			String clName = clsIn;

			if (clName.contains(".")) {
				pkg = clName.substring(0, clName.lastIndexOf("."));
				clName = clName.substring(clName.lastIndexOf(".") + 1);
			}

			String endPkg = pkg;
			if (endPkg.contains(".")) {
				endPkg = endPkg.substring(endPkg.lastIndexOf(".") + 1);
			}

			if (packageIn.endsWith("**")) {
				String pkgIn = packageIn.substring(0, packageIn.length() - 2);
				if (!pkgIn.endsWith(".")) {
					pkgIn += ".";
				}
				if ((pkg + ".").startsWith(pkgIn)) {
					match = true;
				}
			} else if (packageIn.endsWith("*")) {
				String basePackage = packageIn;
				if (basePackage.endsWith(".*")) {
					basePackage = basePackage.substring(0, basePackage.lastIndexOf("."));
					if ((basePackage + "." + endPkg).equals(pkg) || pkg.equals(basePackage)) {
						match = true;
					}
				} else if (basePackage.equals("*")) {
					if (endPkg.equals(pkg)) {
						match = true;
					}
				}
			} else {
				match = pkg.equals(packageIn);
			}

			if (match) {
				if (packageOut.endsWith("**")) {
					while (packageIn.endsWith("*")) {
						packageIn = packageIn.substring(0, packageIn.lastIndexOf("*"));
					}
					packageOut = packageOut.substring(0, packageOut.lastIndexOf("**"));
					if (packageOut.endsWith(".")) {
						packageOut = packageOut.substring(0, packageOut.lastIndexOf("."));
					}
					if (!packageIn.endsWith(".")) {
						packageIn += ".";
					}

					String base = pkg;
					if (!base.endsWith(".")) {
						base += ".";
					}

					base = base.substring(packageIn.length());

					packageOut = (base.isEmpty() ? packageOut : packageOut + ".") + base;
					if (packageOut.endsWith(".")) {
						packageOut = packageOut.substring(0, packageOut.lastIndexOf("."));
					}
				} else if (packageOut.endsWith("*")) {
					while (packageIn.endsWith("*")) {
						packageIn = packageIn.substring(0, packageIn.lastIndexOf("*"));
					}
					if (!packageIn.endsWith(".")) {
						packageIn += ".";
					}
					if (!pkg.endsWith(".")) {
						pkg += ".";
					}
					packageOut = packageOut.substring(0, packageOut.lastIndexOf("*"));
					String base = pkg.substring(packageIn.length());
					if (base.contains("."))
						base = base.substring(0, base.indexOf("."));
					if (packageOut.contains("."))
						packageOut = packageOut.substring(0, packageOut.lastIndexOf("."));

					packageOut = (packageOut.isEmpty() || base.isEmpty() ? packageOut : packageOut + ".") + base;
				}

				if (packageOut.isEmpty())
					return clName;

				return packageOut + "." + clName;
			}
		}
		return clsIn;
	}

	public Mapping<?> mapClassToMapping(String input, Function<Mapping<?>, Boolean> fn, boolean obfuscated) {
		if (input.contains("[]")) {
			input = input.substring(0, input.indexOf("[]"));
		}
		for (Mapping<?> map : mappings) {
			if ((obfuscated ? map.obfuscated : map.name).equals(input) && map.mappingType == MAPTYPE.CLASS) {
				if (fn.apply(map))
					return map;
			}
		}
		return null;
	}
}
