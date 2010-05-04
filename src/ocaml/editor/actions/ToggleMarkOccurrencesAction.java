package ocaml.editor.actions;

import ocaml.editor.indexer.MarkOccurrences;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ToggleMarkOccurrencesAction implements IWorkbenchWindowActionDelegate {

	public void dispose() {
		
	}

	public void init(IWorkbenchWindow arg0) {
		
	}

	public void run(IAction arg0) {
		MarkOccurrences.toggleOccurrences = !MarkOccurrences.toggleOccurrences;
	}

	public void selectionChanged(IAction arg0, ISelection arg1) {
		
	}
	

}
