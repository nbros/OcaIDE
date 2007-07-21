package ocaml.debugging;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This action is called by the "Step Return" menu item in the "Debug" menu, in the O'Caml Debug
 * perspective.<br>
 * Ask the debugger to go to the position right after the current function call.
 */
public class StepReturnAction implements IWorkbenchWindowActionDelegate {

	public void run(IAction action) {
		OcamlDebugger debugger = OcamlDebugger.getInstance();
		debugger.stepReturn();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}


	public void selectionChanged(IAction action, ISelection selection) {
	}

}
