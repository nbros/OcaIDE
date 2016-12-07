package ocaml.editor.actions;

import ocaml.OcamlPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

public class DeleteTrailingWhitespaces implements IWorkbenchWindowActionDelegate {

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
		
		if (!(editorPart instanceof TextEditor)) {
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": only works on ml and mli files");
			return;
		}
		
		TextEditor editor = (TextEditor) editorPart;
		IEditorInput editorInput = editor.getEditorInput();
		IDocument doc = editor.getDocumentProvider().getDocument(editorInput);

		TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
		
		try {
			int startLine = 0;
			int endLine = 0;
			if (selection.getLength() > 0) {
				startLine = doc.getLineOfOffset(selection.getOffset());
				endLine = doc.getLineOfOffset(selection.getOffset() + selection.getLength());
			}
			else {
				startLine = 0;
				endLine = doc.getNumberOfLines() - 1; 
			}
			String newContent = removeTrailingWhitespaces(doc, startLine, endLine);
			int startOffset = doc.getLineOffset(startLine);
			int length = 0;
			if (endLine < doc.getNumberOfLines() - 1)
				length = doc.getLineOffset(endLine + 1) - doc.getLineDelimiter(endLine).length() - startOffset + 1;
			else
				length = doc.getLength() - startOffset;
			doc.replace(startOffset, length, newContent);
			editor.selectAndReveal(startOffset, newContent.length() - 1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private String removeTrailingWhitespaces(IDocument doc, int startLine, int endLine) throws Exception {
		int line = startLine;
		String newContent = "";
		while (line <= endLine) {

			int offset1 = doc.getLineOffset(line);

			if (line < doc.getNumberOfLines() - 1) {
				String lineDelimiter = doc.getLineDelimiter(line);
				int offset2 = doc.getLineOffset(line+1) - lineDelimiter.length();
				
				String lineContent = doc.get(offset1, offset2 - offset1 + 1);
				lineContent = "L" + lineContent;
				lineContent = lineContent.trim();
				lineContent = lineContent.substring(1);
				newContent = newContent + lineContent + lineDelimiter;
			}
			else {
				String lineContent = doc.get(offset1, doc.getLength() - offset1);
				lineContent = "L" + lineContent;
				lineContent = lineContent.trim();
				lineContent = lineContent.substring(1);
				newContent = newContent + lineContent;
			}
			
			line++;
		}
		
		return newContent;
	}
    
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
