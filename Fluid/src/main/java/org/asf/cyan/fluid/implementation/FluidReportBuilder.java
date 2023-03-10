package org.asf.cyan.fluid.implementation;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.asf.cyan.fluid.reports.CallableReportEntry;
import org.asf.cyan.fluid.reports.ReportBuilder;
import org.asf.cyan.fluid.reports.ReportCategory;
import org.asf.cyan.fluid.reports.ReportEntry;
import org.asf.cyan.fluid.reports.ReportEntryList;
import org.asf.cyan.fluid.reports.ReportNode;

/**
 * 
 * Fluid implementation of the ReportBuilder API.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidReportBuilder extends ReportBuilder {

	@Override
	protected String getImplementationName() {
		return "Fluid";
	}

	@Override
	protected ReportBuilder getNewInstance(String head) {
		ReportBuilder builder = new FluidReportBuilder();
		builder.setHead(head);
		return builder;
	}

	private String head;
	private ArrayList<ReportCategory> categories = new ArrayList<ReportCategory>();

	@Override
	public void append(ReportCategory category) {
		categories.add(category);
	}

	@Override
	public void remove(ReportCategory category) {
		if (categories.contains(category))
			categories.remove(category);
	}

	@Override
	public ReportCategory[] getCategories() {
		return categories.toArray(t -> new ReportCategory[t]);
	}

	@Override
	public String getHead() {
		return head;
	}

	@Override
	public void setHead(String head) {
		this.head = head;
	}

	@Override
	protected void buildHeadString(StringBuilder builder) {
		builder.append(head);
		builder.append("\n");
		int longestHeadLine = 0;
		for (String line : head.split("\n")) {
			if (line.length() > longestHeadLine) {
				longestHeadLine = line.length();
			}
		}

		for (int i = 0; i < longestHeadLine; i++) {
			builder.append("-");
		}
	}

	@Override
	protected void buildCategoryHead(StringBuilder builder, String name, int longestNameLength) {
		builder.append("--- " + name + " ---");
		for (int i = 0; i < longestNameLength - name.length(); i++) {
			builder.append("-");
		}
	}

	@Override
	protected void buildNodeHead(StringBuilder builder, ReportNode node) {
		builder.append(node.name + ":");
	}

	@Override
	protected void buildEntry(StringBuilder builder, ReportEntry<?> node, int longestNameLength) {
		String key = node.key;
		Object val = node.value;
		if (val instanceof Supplier) {
			Supplier<?> callable = (Supplier<?>) val;
			try {
				val = callable.get();
			} catch (Exception e) {
				val = "*** ERROR: " + e.getClass().getTypeName() + ": " + e.getMessage() + " ***";
			}
		}
		builder.append("\t");
		if (key != null) {
			builder.append(key);
			for (int i = key.length(); i < longestNameLength; i++) {
				builder.append(" ");
			}
			builder.append(" : ");
		}
		if (val == null)
			val = "*** null ***";
		builder.append(val.toString().replace("\r", "").replace("\n", "\n\t"));
		builder.append("\n");
	}

	@Override
	public ReportCategory newCategory(String name) {
		ReportCategory cat = new ReportCategory(name);
		append(cat);
		return cat;
	}

	@Override
	public ReportCategory newCategory(String name, ArrayList<ReportNode> nodes) {
		ReportCategory cat = new ReportCategory(name, nodes);
		append(cat);
		return cat;
	}

	@Override
	public ReportNode newNode(ReportCategory category, String name) {
		ReportNode nd = new ReportNode(name);
		category.add(nd);
		return nd;
	}

	@Override
	public ReportNode newNode(ReportCategory category, String name, ReportEntryList entries) {
		ReportNode nd = new ReportNode(name, entries);
		category.add(nd);
		return nd;
	}

	@Override
	public ReportNode appendNode(ReportCategory category, ReportNode node) {
		category.add(node);
		return node;
	}

	@Override
	public ReportEntry<Integer> newEntry(String name, int value) {
		return new ReportEntry<Integer>(value);
	}

	@Override
	public ReportEntry<Boolean> newEntry(String name, boolean value) {
		return new ReportEntry<Boolean>(value);
	}

	@Override
	public ReportEntry<Float> newEntry(String name, float value) {
		return new ReportEntry<Float>(value);
	}

	@Override
	public ReportEntry<Double> newEntry(String name, double value) {
		return new ReportEntry<Double>(value);
	}

	@Override
	public ReportEntry<Byte> newEntry(String name, byte value) {
		return new ReportEntry<Byte>(value);
	}

	@Override
	public ReportEntry<Character> newEntry(String name, char value) {
		return new ReportEntry<Character>(value);
	}

	@Override
	public ReportEntry<String> newEntry(String name, String value) {
		return new ReportEntry<String>(value);
	}

	@Override
	public ReportEntry<Object> newEntry(String name, Object value) {
		return new ReportEntry<Object>(value);
	}

	@Override
	public ReportEntry<?> newEntry(String name, Supplier<?> value) {
		return new CallableReportEntry(value);
	}
}
