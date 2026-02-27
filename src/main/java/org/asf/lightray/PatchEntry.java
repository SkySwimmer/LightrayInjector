package org.asf.lightray;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JCheckBox;

public class PatchEntry {

	public File file;
	public String name;
	public boolean enabled;
	public JCheckBox box;
	public PatchEntryType type;
	public boolean preExtracted;
	public PatchEntry parent;

	public ArrayList<PatchEntry> childEntries = new ArrayList<PatchEntry>();

}
