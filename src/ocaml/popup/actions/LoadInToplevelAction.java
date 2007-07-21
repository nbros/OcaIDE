package ocaml.popup.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import ocaml.OcamlPlugin;
import ocaml.views.toplevel.OcamlToplevelView;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on the "Load in Toplevel" menu item in the pop-up for O'Caml
 * modules.
 * 
 * It tries to load the object file with the same base name as the selected ml file, with the "#load" command.
 */
public class LoadInToplevelAction implements IObjectActionDelegate {

	private ArrayList<IFile> files = new ArrayList<IFile>();

	public LoadInToplevelAction() {
	}

	public void run(IAction action) {
		for (IFile file : files) {
			File objFile = null;

			try {
				objFile = file.getLocation().removeFileExtension().addFileExtension("cmo").toFile();

				if (!objFile.exists()) {
					objFile = file.getLocation().removeFileExtension().addFileExtension("cmx").toFile();

					if (objFile.exists()) {
						MessageDialog
								.openInformation(
										null,
										"Cannot load file",
										file.getName()
												+ " is compiled in native mode.\n"
												+ "To be loaded into the toplevel, it needs to be compiled in byte-code mode.");
						continue;
					}

					MessageDialog.openInformation(null, "Cannot load file", file.getName()
							+ " doesn't have a corresponding cmo file.\n" + "Have you compiled it yet?");

					continue;
				}

				if (!objFile.exists()) {
					MessageDialog.openInformation(null, "Cannot load file", file.getName()
							+ " doesn't have a corresponding cmo or cmx file.\n"
							+ "Have you compiled it yet?");
					continue;
				}

				OcamlToplevelView.eval("#cd \"" + objFile.getParent() + "\";;\n" + "#load \""
						+ objFile.getName() + "\";;");

			} catch (Exception e) {
				OcamlPlugin.logError("ocaml plugin error", e);
			}

		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.files.clear();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Iterator<IStructuredSelection> it = sel.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof IFile)
					this.files.add((IFile) o);

			}
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}
}
