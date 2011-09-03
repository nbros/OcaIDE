package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/** This action activates an information popup on the element under the caret in the OCaml editor. */
public class ContextInformationAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					ITextOperationTarget textOperationTarget =
							(ITextOperationTarget) editor
								.getAdapter(org.eclipse.jface.text.ITextOperationTarget.class);

					if (textOperationTarget
						.canDoOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
						textOperationTarget
							.doOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
					else
						OcamlPlugin
							.logError("OcamlPlugin: content assist context information not supported by this control");

				}
				else if (editorPart instanceof OcamllexEditor) {
					OcamllexEditor editor = (OcamllexEditor) editorPart;

					ITextOperationTarget textOperationTarget =
							(ITextOperationTarget) editor
								.getAdapter(org.eclipse.jface.text.ITextOperationTarget.class);

					if (textOperationTarget
						.canDoOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
						textOperationTarget
							.doOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
					else
						OcamlPlugin
							.logError("OcamlPlugin: content assist context information not supported by this control");

				}
				else if (editorPart instanceof OcamlyaccEditor) {
					OcamlyaccEditor editor = (OcamlyaccEditor) editorPart;

					ITextOperationTarget textOperationTarget =
							(ITextOperationTarget) editor
								.getAdapter(org.eclipse.jface.text.ITextOperationTarget.class);

					if (textOperationTarget
						.canDoOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION))
						textOperationTarget
							.doOperation(SourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
					else
						OcamlPlugin
							.logError("OcamlPlugin: content assist context information not supported by this control");

				}

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
