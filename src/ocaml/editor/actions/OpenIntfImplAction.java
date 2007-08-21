package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;

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

					IFile file = editor.getFileBeingEdited();

					boolean bInterface = false;

					if (file != null) {
						String filename = file.getFullPath().toOSString();
						if (filename.endsWith(".ml")) {
							filename = filename.substring(0, filename.length() - 3) + ".mli";
							bInterface = true;
						} else if (filename.endsWith(".mli"))
							filename = filename.substring(0, filename.length() - 4) + ".ml";
						else
							return;

						IWorkspace ws = ResourcesPlugin.getWorkspace();
						IResource resource = ws.getRoot().findMember(filename);

						if (resource instanceof IFile) {
							IFile correspondingFile = (IFile) resource;

							try {
								if (bInterface)
									page.openEditor(new FileEditorInput(correspondingFile),
											OcamlEditor.MLI_EDITOR_ID, true);
								else
									page.openEditor(new FileEditorInput(correspondingFile),
											OcamlEditor.ML_EDITOR_ID, true);

							} catch (PartInitException e) {
								OcamlPlugin.logError("ocaml plugin error", e);
							}
						}

					}
				} else
					OcamlPlugin.logError("OpenIntfImplAction: only works on ml and mli files");

			} else
				OcamlPlugin.logError("FormatWithCamlp4Action: editorPart is null");
		} else
			OcamlPlugin.logError("FormatWithCamlp4Action: page is null");

	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
