package org.asf.cyan.fluid.bytecode.sources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
public class FileClassSourceProvider implements IClassSourceProvider<File> {

	private File file;

	public FileClassSourceProvider(File file) {
		this.file = file;
	}

	public boolean isZipLike() {
		return file.getName().endsWith(".jar") || file.getName().endsWith(".zip");
	}

	@Override
	public ComparisonMethod getComparisonMethod() {
		return ComparisonMethod.OBJECT_EQUALS;
	}

	@Override
	public File providerObject() {
		return file;
	}

	@Override
	public InputStream getStream(String classType) {
		URL url;
		try {
			url = file.toURI().toURL();
		} catch (MalformedURLException e1) {
			return null;
		}
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

		InputStream strm;
		try {
			strm = url.openStream();
		} catch (IOException e) {
			return null;
		}
		return strm;
	}

	@Override
	public void importAllRead(FluidClassPool pool) {
		if (isZipLike()) {
			InputStream strm;
			try {
				strm = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
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
		} else if (file.isDirectory()) {
			// Scan folder
			importFromFolder(file, pool, "", true);
		}
	}

	@Override
	public void importAllFind(FluidClassPool pool) {
		if (isZipLike()) {
			InputStream strm;
			try {
				strm = new FileInputStream(file);
			} catch (FileNotFoundException e1) {
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
		} else if (file.isDirectory()) {
			// Scan folder
			importFromFolder(file, pool, "", false);
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
