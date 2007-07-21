package ocaml.build;

import java.util.List;

import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/** A visitor to find what files to handle in a project: ml, mli, mly and mll files. This visitor also handles added directories, 
 * by adding them to the project's paths
 * 
 * @see Misc#isOCamlSourceFile(IFile)
 * 
 */
public class SuitableFilesFinder implements IResourceVisitor {

	/** files found while visiting */
	private List<IFile> files;
	
	/** a list of paths to add to the project */
	private List<IPath> paths;

	/** 
	 * @param files
	 *            where to store the results
	 * @param paths
	 *            where to store the paths found
	 */
	public SuitableFilesFinder(final List<IFile> files, final List<IPath> paths) {
		this.files = files;
		this.paths = paths;
	}

	/** Look at the type and extension of each resource, and only keep what's suitable. */
	public boolean visit(IResource resource) throws CoreException {

		if (!resource.exists()
				|| Misc.getResourceProperty(resource,
						Misc.EXTERNAL_SOURCES_FOLDER).equals("true")) {
			return false;
		}

		// On ne retient que les fichiers de source non générés automatiquement
		if (resource.getType() == IResource.FILE) {
			if (Misc.isOCamlSourceFile((IFile) resource)
					&& !Misc.isGeneratedFile((IFile) resource)) {
				files.add((IFile) resource);
			}
		} else if (resource.getType() == IResource.FOLDER
				|| resource.getType() == IResource.PROJECT) {
			// Ajouter le chemin (On vérifiera lors de OcamlPath.addToPaths
			// qu'il ne correspond pas à un repertoire lié ou de sources
			// externes etc...)
			paths.add(resource.getProjectRelativePath());
		}
		return true;
	}

}
