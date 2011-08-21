package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;
import ocaml.views.outline.QuickOutline;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/** This action opens the quick outline in the O'Caml editor. */
public class OpenQuickOutlineAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;
					Def definitionsTree = editor.getOutlineDefinitionsTree();
					if(definitionsTree != null) {
						QuickOutline quickOutline = new QuickOutline(window.getShell(), definitionsTree, editor);
						quickOutline.open();
						quickOutline.setFocus();
					}

				}else
					OcamlPlugin.logError("OpenQuickOutlineAction: not an Ocaml editor");

			}else
				OcamlPlugin.logError("OpenQuickOutlineAction: editorPart is null");
		} else
			OcamlPlugin.logError("OpenQuickOutlineAction: page is null");

	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
