package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editor.formatting.OcamlFormatter;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This action calls the formatter on the current selection in the OCaml editor
 * (or all the code in the editor if nothing is selected). The formatter
 * indents, formats comments, adjusts spaces between words...
 */
public class FormatterAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					OcamlFormatter.formatInterface = editor.getFileBeingEdited()
							.getName().endsWith(".mli");

					ITextOperationTarget textOperationTarget = (ITextOperationTarget) editor
							.getAdapter(org.eclipse.jface.text.ITextOperationTarget.class);

					if (textOperationTarget.canDoOperation(SourceViewer.FORMAT))
						textOperationTarget.doOperation(SourceViewer.FORMAT);
					else
						OcamlPlugin
								.logError("OcamlPlugin: operation \"format\" not supported by this control");

					// editor.rebuildOutline(100);
				} else
					MessageDialog.openInformation(window.getShell(),
							"Ocaml Plugin",
							"Formatting is only implemented for .ml files.");

			}
		}
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}
}
