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

/** This action increases or decreases the indentation of the selection in the current editor. */
public class MarkOccurrenceAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	
	private final String OcamlOccurenceMarkerID = "ocaml.marker.occurrences";
	
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
						IMarker markes[] = ifile.findMarkers(OcamlOccurenceMarkerID , false, 0);
						for (int i = 0; i < markes.length; i++)
							markes[i].delete();
						// find all the occurrence of selected text and mark it
						TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
						String text = selection.getText();
						if (text.length() > 0) {
							IDocument document = editor.getDocumentProvider().getDocument(input);
							FindReplaceDocumentAdapter docFind = new FindReplaceDocumentAdapter(document);
							IRegion region = docFind.find(0, text, true, true, false, false);
							while (region != null) {
								// mark the found text
								IResource resource = (IResource) input.getAdapter(IResource.class);
								IMarker marker = resource.createMarker(OcamlOccurenceMarkerID);
								int startOffset = region.getOffset();
								int endOffset = region.getOffset() + region.getLength();
								marker.setAttribute(IMarker.CHAR_START, startOffset);
								marker.setAttribute(IMarker.CHAR_END, endOffset);
								region = docFind.find(endOffset + 1, text, true, true, false, false);
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 //IResource.createMarker();
					
				} else
					OcamlPlugin.logError("CommentSelectionAction: only works on ml and mli files");

			} else
				OcamlPlugin.logError("CommentSelectionAction: editorPart is null");
		} else
			OcamlPlugin.logError("CommentSelectionAction: page is null");
	}
    
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
