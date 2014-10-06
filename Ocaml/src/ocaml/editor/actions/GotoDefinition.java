package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlHyperlinkDetector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;

/** This action marks occurrences of the text currently selected in the editor. */
public class GotoDefinition implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	private static final String OCAML_GOTO_DEFINITION_ID = "ocaml.marker.gotodefinition";
	
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
		
		if (!(editorPart instanceof TextEditor)) {
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": only works on ml and mli files");
			return;
		}
		
		if (editorPart instanceof OcamlEditor) {
			OcamlEditor editor = (OcamlEditor) editorPart;
			TextSelection selection = 
					(TextSelection) editor.getSelectionProvider().getSelection();
			int offset = selection.getOffset();
			OcamlHyperlinkDetector hyperlinkdetector = new OcamlHyperlinkDetector(editor);
			ITextViewer textViewer = editor.getTextViewer();
			IHyperlink hyperlink =  hyperlinkdetector.makeHyperlink(textViewer, offset);
			if (hyperlink != null)
				hyperlink.open();
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
