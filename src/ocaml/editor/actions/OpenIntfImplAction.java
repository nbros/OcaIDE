package ocaml.editor.actions;

import java.io.File;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.ide.IDE;

/** This action opens an editor for the corresponding interface/implementation. */
public class OpenIntfImplAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					IPath file = editor.getPathOfFileBeingEdited();

					if (file != null) {
						String filepath = file.toOSString();
						if (filepath.endsWith(".ml"))
							filepath = filepath.substring(0, filepath.length() - 3) + ".mli";
						else if (filepath.endsWith(".mli"))
							filepath = filepath.substring(0, filepath.length() - 4) + ".ml";
						else
							return;

						try {
							IFileStore fileStore = EFS.getStore(new File(filepath).toURI());
							IDE.openEditorOnFileStore(page, fileStore);

						} catch (Exception e) {
							OcamlPlugin.logError("OpenIntfImplAction.run()", e);
							return;
						}
					}
				} else
					OcamlPlugin.logError("OpenIntfImplAction: only works on ml and mli files");

			} else
				OcamlPlugin.logError("OpenIntfImplAction: editorPart is null");
		} else
			OcamlPlugin.logError("OpenIntfImplAction: page is null");

	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
