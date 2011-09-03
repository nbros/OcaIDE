package ocaml.debugging.actions;

import ocaml.debugging.OcamlDebugger;
import ocaml.editors.OcamlEditor;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This action is called by the "Put Breakpoint" menu item in the "Debug" menu, in the OCaml Debug
 * perspective.<br>
 * Ask the debugger to put a breakpoint at the position of the caret in the editor.
 */
public class PutBreakpointAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window = null;

	public void run(IAction action) {

		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;
					int offset = editor.getCaretOffset();
					
					IPath file = editor.getPathOfFileBeingEdited();
					if(file == null)
						return;
					
					String filename = file.lastSegment();

					if (filename.endsWith(".ml")) {
						String moduleName = Character.toUpperCase(filename.charAt(0))
								+ filename.substring(1, filename.length() - 3);

						OcamlDebugger debugger = OcamlDebugger.getInstance();
						debugger.putBreakpointAt(moduleName, offset);
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
