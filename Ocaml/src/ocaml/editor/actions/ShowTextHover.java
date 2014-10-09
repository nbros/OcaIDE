package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlTextHover;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

public class ShowTextHover implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	public void run(IAction action) {

		IWorkbenchPage page = window.getActivePage();
		
		if (page == null) {
			OcamlPlugin.logWarning(GotoDefinition.class.getSimpleName() 
					+ ": page is null");
			return;
		}

		IEditorPart editorPart = page.getActiveEditor();
		if (editorPart == null) {
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": editorPart is null");
			return;
		}
		
		if (!(editorPart instanceof OcamlEditor)) {
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": only works on ml and mli files");
			return;
		}

		final OcamlEditor editor = (OcamlEditor) editorPart;
		ITextViewer viewer = editor.getTextViewer();
		IEditorInput editorInput = editor.getEditorInput();
		IDocument doc = editor.getDocumentProvider().getDocument(editorInput);
		Control control = (Control)editor.getAdapter(Control.class);

		if (!(control instanceof StyledText)) 
		{
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": Cannot get caret position");
			return;
		}
		
		final StyledText styledText = (StyledText) control;
		int offset = styledText.getCaretOffset();
		
		OcamlTextHover hover = new OcamlTextHover(editor);
		IRegion region = hover.getHoverRegion(viewer, offset);
		final String hoverInfo = hover.getHoverInfoOneLine(viewer, region);

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				if (!hoverInfo.equals(""))
					editor.setStatusLineMessage(hoverInfo);
				else
					editor.setStatusLineMessage("");
			}
		});

	}
    
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
