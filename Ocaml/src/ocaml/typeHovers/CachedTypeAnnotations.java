package ocaml.typeHovers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import ocaml.OcamlPlugin;


/** An entry in the type annotations cache */
public class CachedTypeAnnotations {

	private ArrayList<TypeAnnotation> annotations;
	private final Object path;
	private final long lastModified;

	public CachedTypeAnnotations(File file) {
		this.lastModified = file.lastModified();

		String path = "";
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
		this.path = path;
		this.annotations = new ArrayList<TypeAnnotation>();
	}

	/** @return true if the cached version is more recent than that on disk */
	public boolean isMoreRecentThan(File file) {
		return file.lastModified() <= this.lastModified;
	}

	/** @return true if <code>file</code> is the same file as the cached one */
	public boolean sameAs(File file) {
		try {
			return file.getCanonicalPath().equals(this.path);
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return false;
		}
	}

	/** Add a type annotation to the cache */
	public void addAnnotation(TypeAnnotation annotation) {
		if (annotation != null)
			annotations.add(annotation);
	}

	/** @return all the type annotations in cache */
	public TypeAnnotation[] getAnnotations() {
		return annotations.toArray(new TypeAnnotation[0]);
	}
}
