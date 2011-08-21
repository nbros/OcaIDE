package ocaml.parsers;

import java.io.File;
import java.io.IOException;

import ocaml.OcamlPlugin;
import ocaml.parser.Def;

/** An entry in the O'Caml interfaces definitions cache. */
class CachedDef {
	private final Def def;
	private final Object path;
	private final long lastModified;
	
	public CachedDef(File file, Def def) {
		this.def = def;
		this.lastModified = file.lastModified();
		
		String path = "";
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
		this.path = path;
	}
	
	/** @return true if the cached version is more recent than that on disk */
	public boolean isMoreRecentThan(File file)
	{
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
	
	/** @return the module definition from the cache */
	public Def getDefinition() {
		return def;
	}
}
