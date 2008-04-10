package ocaml.build;

import java.util.regex.Pattern;

import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Visitor to delete automatically generated resources (.cm*, .annot, .mli) 
 */
public class CleanVisitor implements IResourceVisitor {

	/**
	 * Progress monitor for the clean operation.
	 */
	private IProgressMonitor monitor;

	public CleanVisitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	public boolean visit(IResource resource) throws CoreException {
		
		/* Ignore non-existing resources */
		if (!resource.exists()) {
			return false;
		}

		// Don't delete linked resources (linked in Eclipse terminology)
		if (resource.isLinked()) {
			return false;
		}

		// Only for files
		if (resource.getType() != IResource.FILE)
			return true;
		final IFile file = (IFile) resource;

		if (monitor != null)
			monitor.subTask("cleaning " + file.getName());

		final String fileExtension = file.getFileExtension();

		// delete automatically generated files (mli, exe, makeFile)
		if ("true".equals(Misc.getFileProperty(file, OcamlBuilder.IS_GEN))) {
			file.delete(true, monitor);
			if (monitor != null)
				monitor.worked(1);
			return true;
		}

		// If we're here, it means the IS_GEN property wasn't set

		// Delete all files with the extensions: cmo, cmi, cmx, cma, cmxa and annot
		if ((fileExtension != null) && Pattern.matches("cm[oixa]|cmxa|annot", fileExtension)) {
			file.delete(true, monitor);
			if (monitor != null)
				monitor.worked(1);
			return true;
		}

		// If this is a source file, delete error and warning markers
		if ((fileExtension != null) && (Pattern.matches("ml[ily]?", fileExtension))) {
			file.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);

		}

		if (monitor != null)
			monitor.worked(1);
		return true;
	}

}
