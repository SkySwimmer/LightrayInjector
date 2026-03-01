package org.asf.cyan.fluid.bytecode.sources;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
	public void importAllRead(FluidClassPool pool) {
		if (isZipLike()) {
			// Load zip
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
					pool.importArchiveClasses(zip, true);
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
		} else {
			// Try loading from files
			if (url.getProtocol().equals("file")) {
				// Get URI
				File path = null;
				try {
					URI u = url.toURI();
					if (u.getAuthority() == null || u.getAuthority().equals("")) {
						// Try loading
						path = new File(u);
					}
				} catch (Exception e) {
				}
				if (path != null) {
					// Load// Scan folder
					importFromFolder(path, pool, "", true);
				}
			}
		}
	}

	@Override
	public void importAllFind(FluidClassPool pool) {
		if (isZipLike()) {
			// Load zip
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
					pool.importArchiveClasses(zip, false);
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
		} else {
			// Try loading from files
			if (url.getProtocol().equals("file")) {
				// Get URI
				File path = null;
				try {
					URI u = url.toURI();
					if (u.getAuthority() == null || u.getAuthority().equals("")) {
						// Try loading
						path = new File(u);
					}
				} catch (Exception e) {
				}
				if (path != null) {
					// Load// Scan folder
					importFromFolder(path, pool, "", false);
				}
			}
		}
	}

	private void importFromFolder(File dir, FluidClassPool pool, String pref, boolean read) {
		if (!dir.exists())
			return;
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				// Import subdirs
				importFromFolder(f, pool, pref + f.getName() + ".", read);
			} else if (f.getName().endsWith(".class")) {
				// Try import
				if (read) {
					// Read
					try {
						InputStream strm = new FileInputStream(f);
						pool.readClass(pref + f.getName().substring(0, f.getName().lastIndexOf(".class")), strm);
						strm.close();
					} catch (IOException e) {
					}
				} else {
					// Add
					pool.addKnownClass(pref + f.getName().substring(0, f.getName().lastIndexOf(".class")));
				}
			}
		}
	}

}
