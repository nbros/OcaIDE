package ocaml.build.makefile;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This action happens when the user clicks on the "clean" item in the "OCaml" menu. It calls
 * IncrementalProjectBuilder.CLEAN_BUILD.
 */
public class CleanAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					try {
						editor.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}

				} else if (editorPart instanceof OcamllexEditor) {
					OcamllexEditor editor = (OcamllexEditor) editorPart;

					try {
						editor.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}

				} else if (editorPart instanceof OcamlyaccEditor) {
					OcamlyaccEditor editor = (OcamlyaccEditor) editorPart;

					try {
						editor.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, null);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}

				}

			}
		}
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}
}
