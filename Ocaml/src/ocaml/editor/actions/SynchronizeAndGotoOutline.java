package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlHyperlinkDetector;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;
import ocaml.views.outline.OcamlOutlineControl;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

public class SynchronizeAndGotoOutline implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	public void run(IAction action) {

		IWorkbenchPage page = window.getActivePage();
		
		if (page == null) {
			OcamlPlugin.logWarning(SynchronizeAndGotoOutline.class.getSimpleName() 
					+ ": page is null");
			return;
		}

		IEditorPart editorPart = page.getActiveEditor();
		if (editorPart == null) {
			OcamlPlugin.logError(SynchronizeAndGotoOutline.class.getSimpleName() 
					+ ": editorPart is null");
			return;
		}
		
		if (!(editorPart instanceof TextEditor)) {
			OcamlPlugin.logError(SynchronizeAndGotoOutline.class.getSimpleName() 
					+ ": only works on ml and mli files");
			return;
		}
		
		if (editorPart instanceof OcamlEditor) {
			OcamlEditor editor = (OcamlEditor) editorPart;
			editor.synchronizeOutline();
			// show compiler output
			editor.getOutline().setFocus();
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
