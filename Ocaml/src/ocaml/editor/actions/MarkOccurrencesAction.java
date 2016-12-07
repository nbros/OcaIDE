package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;

/** This action marks occurrences of the text currently selected in the editor. */
public class MarkOccurrencesAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	private static final String OCAML_OCCURRENCES_MARKER_ID = "ocaml.marker.occurrences";
	
	public void run(IAction action) {

		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof TextEditor) {
					TextEditor editor = (TextEditor)editorPart;
					try {
						IEditorInput input = editor.getEditorInput();
						// delete old marker
						IFile ifile = ((FileEditorInput) input).getFile();
						IMarker markes[] = ifile.findMarkers(OCAML_OCCURRENCES_MARKER_ID, false, 0);
						for (int i = 0; i < markes.length; i++)
							markes[i].delete();
						// find all the occurrences of selected text and mark it
						TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
						String text = selection.getText();
						if (text.length() > 0) {
							IDocument document = editor.getDocumentProvider().getDocument(input);
							FindReplaceDocumentAdapter docFind = new FindReplaceDocumentAdapter(document);
							IRegion region = docFind.find(0, text, true, true, false, false);
							while (region != null) {
								// mark the found text
								IResource resource = (IResource) input.getAdapter(IResource.class);
								IMarker marker = resource.createMarker(OCAML_OCCURRENCES_MARKER_ID);
								int startOffset = region.getOffset();
								int endOffset = region.getOffset() + region.getLength();
								marker.setAttribute(IMarker.CHAR_START, startOffset);
								marker.setAttribute(IMarker.CHAR_END, endOffset);
								marker.setAttribute(IMarker.MESSAGE, "");
								region = docFind.find(endOffset + 1, text, true, true, false, false);
							}
						}
					} catch (Exception e) {
						OcamlPlugin.logError(e);
					}
				} else
					OcamlPlugin.logError(MarkOccurrencesAction.class.getSimpleName() + ": only works on ml and mli files");

			} else
				OcamlPlugin.logError(MarkOccurrencesAction.class.getSimpleName() + ": editorPart is null");
		} else
			OcamlPlugin.logError(MarkOccurrencesAction.class.getSimpleName() + ": page is null");
	}
    
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
