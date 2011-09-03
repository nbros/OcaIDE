package ocaml.popup.actions;

import java.io.ByteArrayInputStream;

import ocaml.OcamlPlugin;
import ocaml.exec.CommandRunner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on the "Generate Interface" menu item in the pop-up for a module
 * in an OCaml managed project.
 */
public class GenMliAction implements IObjectActionDelegate {

	private IFile file = null;

	public GenMliAction() {
	}

	public void run(IAction action) {
		if (file != null) {
			String[] command = new String[4];
			command[0] = OcamlPlugin.getOcamlcFullPath();
			command[1] = "-c";
			command[2] = "-i";
			command[3] = file.getLocation().toOSString();

			CommandRunner cmd = new CommandRunner(command, file.getParent().getLocation().toOSString());
			String result = cmd.getStdout();

			IResource parent = file.getParent();
			IPath mliPath = file.getLocation().removeFileExtension().addFileExtension("mli");
			IFile mliFile = null;
			if (parent instanceof IFolder) {
				IFolder folder = (IFolder) parent;
				mliFile = folder.getFile(mliPath.lastSegment());
			} else if (parent instanceof IProject) {
				IProject project = (IProject) parent;
				mliFile = project.getFile(mliPath.lastSegment());
			}

			if (mliFile != null) {

				try {
					if (mliFile.exists()) {
						if (!MessageDialog.openConfirm(null, "Replace interface file?", mliFile.getName()
								+ " already exists. Are you sure you want to replace it?"))
							return;

						mliFile.delete(false, null);
					}

					ByteArrayInputStream stream = new ByteArrayInputStream(result.getBytes());
					mliFile.create(stream, true, null);
				} catch (CoreException e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}

		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.file = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object o = sel.getFirstElement();
			if (o instanceof IFile)
				this.file = (IFile) o;
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
