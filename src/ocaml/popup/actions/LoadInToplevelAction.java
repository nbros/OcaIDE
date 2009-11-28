package ocaml.popup.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;
import ocaml.views.toplevel.OcamlToplevelView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on the "Load in Toplevel" menu item in the pop-up for
 * O'Caml modules.
 * 
 * It tries to load the object file with the same base name as the selected ml file, with the
 * "#load" command.
 */
public class LoadInToplevelAction implements IObjectActionDelegate {

	private ArrayList<IFile> files = new ArrayList<IFile>();

	public LoadInToplevelAction() {
	}

	public void run(IAction action) {
		for (IFile file : files) {

			try {
				IPath filePath = file.getFullPath();
				File cmoFile = Misc.getOtherFileFor(file.getProject(), filePath, ".cmo");

				if (cmoFile == null || !cmoFile.exists()) {
					File cmxFile = Misc.getOtherFileFor(file.getProject(), filePath, ".cmx");
					
					if (cmxFile != null && cmxFile.exists()) {
						MessageDialog.openInformation(null, "Cannot load file", file.getName()
								+ " is compiled in native mode.\n"
								+ "To be loaded into the toplevel, "
								+ "it needs to be compiled in byte-code mode.");
						continue;
					}

					MessageDialog.openInformation(null, "Cannot load file", file.getName()
							+ " doesn't have a corresponding cmo file.\n"
							+ "Have you compiled it yet?");

					continue;
				}

				// use a double "\" on Windows
				String separator = ((File.separatorChar == '\\') ? "\\\\" : File.separator); 
				
				Path path = new Path(cmoFile.getParent());
				path = (Path)path.makeAbsolute();
				
				String strPath = path.getDevice();
				if(strPath == null)strPath = "";
				strPath = strPath + separator;
				
				for(String segment : path.segments()){
					strPath = strPath + segment + separator;
				}
				
				
					
				OcamlToplevelView.eval("#cd \"" + strPath + "\";;\n" + "#load \""
						+ cmoFile.getName() + "\";;");

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
