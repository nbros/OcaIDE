package ocaml;

import java.io.File;
import java.util.ArrayList;

import ocaml.natures.OcamlNatureMakefile;
import ocaml.natures.OcamlbuildNature;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

/**
 * This resource change listener monitors additions and removals of folders in ocaml projects to automatically
 * modify the project paths accordingly.
 */
public class FolderChangeListener implements IResourceChangeListener {

	public void resourceChanged(IResourceChangeEvent event) {
		try {
			event.getDelta().accept(new IResourceDeltaVisitor() {

				public boolean visit(IResourceDelta delta) throws CoreException {
					IResource res = delta.getResource();
					if (res.getType() == IResource.FOLDER) {

						IProject project = res.getProject();

						// if this project is an ocaml project
						if (project.getNature(OcamlbuildNature.ID) != null
								|| project.getNature(OcamlNatureMakefile.ID) != null) {

							switch (delta.getKind()) {
							case IResourceDelta.ADDED: {

								// ignore special directories
								if (res.getName().startsWith(".") || res.getName().equals("_build"))
									break;

								// add this path to the project paths
								OcamlPaths paths = new OcamlPaths(project);
								String[] strPaths = paths.getPaths();
								String[] newPaths = new String[strPaths.length + 1];
								System.arraycopy(strPaths, 0, newPaths, 0, strPaths.length);
								newPaths[strPaths.length] = res.getProjectRelativePath().toPortableString();
								paths.setPaths(newPaths);

								break;
							}
							case IResourceDelta.REMOVED: {
								// remove this path from the project paths
								File removedFolder = new File(res.getLocation().toOSString());

								ArrayList<String> paths = new ArrayList<String>();

								OcamlPaths opaths = new OcamlPaths(project);
								for (String p : opaths.getPaths()) {
									File file = new File(p);
									if (!file.isAbsolute())
										file = new File(project.getLocation().toOSString(), p);

									if (!file.equals(removedFolder))
										paths.add(p);
								}

								opaths.setPaths(paths.toArray(new String[paths.size()]));

								break;
							}
							}
						}
					}
					return true; // visit the children
				}

			});

		} catch (CoreException e) {
			OcamlPlugin.logError("Error in folder change listener", e);
		}

	}

}
