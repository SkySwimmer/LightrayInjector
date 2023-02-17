package org.asf.cyan.fluid.reports;

import java.util.function.Supplier;

public class CallableReportEntry extends ReportEntry<Supplier<?>> {

	public CallableReportEntry(Supplier<?> value) {
		super(value);
	}

	public CallableReportEntry(String key, Supplier<?> value) {
		super(key, value);
	}

}
