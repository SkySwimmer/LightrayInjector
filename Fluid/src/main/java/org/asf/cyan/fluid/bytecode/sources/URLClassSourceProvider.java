package org.asf.cyan.fluid.bytecode.sources;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipInputStream;

import org.asf.cyan.fluid.bytecode.FluidClassPool;
import org.asf.cyan.fluid.bytecode.enums.ComparisonMethod;

/**
 * 
 * URL-based class source provider for the FLUID class pool.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class URLClassSourceProvider implements IClassSourceProvider<URL> {

	private URL url;

	public URLClassSourceProvider(URL url) {
		this.url = url;
	}

	public boolean isZipLike() {
		return url.toString().endsWith(".jar") || url.toString().endsWith(".zip");
	}

	@Override
	public ComparisonMethod getComparisonMethod() {
		return ComparisonMethod.OBJECT_EQUALS;
	}

	@Override
	public URL providerObject() {
		return url;
	}

	@Override
	public InputStream getStream(String classType) {
		URL url = this.url;
		if (isZipLike()) {
			try {
				url = new URL("jar:" + url.toString() + "!/" + classType + ".class");
			} catch (MalformedURLException e) {
				return null;
			}
		} else {
			try {
				url = new URL(url + "/" + classType + ".class");
			} catch (MalformedURLException e) {
				return null;
			}
		}

		BufferedInputStream strm;
		try {
			strm = new BufferedInputStream(url.openStream());
		} catch (IOException e) {
			return null;
		}
		return strm;
	}

	@Override
	public void importAll(FluidClassPool pool) {
		if (isZipLike()) {
			URL url = this.url;
			InputStream strm;
			try {
				strm = url.openStream();
			} catch (IOException e1) {
				return;
			}
			try {
				ZipInputStream zip = new ZipInputStream(strm);
				try {
					// Import
					pool.importArchive(zip);
				} finally {
					zip.close();
				}
			} catch (Exception e) {
				// Invalid archive
			} finally {
				// Close stream
				try {
					strm.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
