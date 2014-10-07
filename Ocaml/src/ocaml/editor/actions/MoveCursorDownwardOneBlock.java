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

public class MoveCursorDownwardOneBlock implements IWorkbenchWindowActionDelegate {

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
		Control control = (Control)editor.getAdapter(Control.class);

		if (!(control instanceof StyledText)) 
		{
			OcamlPlugin.logError(GotoDefinition.class.getSimpleName() 
					+ ": Cannot get caret position");
			return;
		}

		final StyledText styledText = (StyledText) control;
		int cursorOffset = styledText.getCaretOffset();

		try {
			int lineNum= doc.getLineOfOffset(cursorOffset);
			int numOfLine = doc.getNumberOfLines();
			
			if (lineNum == (numOfLine - 2)) {
				int lastLineOffset = doc.getLineOffset(lineNum+1);
				editor.selectAndReveal(lastLineOffset, 0);
				return;
			}
			
			if (lineNum >= (numOfLine - 1)) {
				editor.selectAndReveal(doc.getLength(), 0);
				return;
			}
			
			int beginOffset = doc.getLineOffset(lineNum);
			int endOffset = doc.getLineOffset(lineNum+1) - 1;
			String currentLine = doc.get(beginOffset, endOffset - beginOffset + 1);
			boolean isCurrentLineEmpty = currentLine.trim().isEmpty();

			// find next non-empty line which follows an empty line
			lineNum++;
			while (lineNum < numOfLine - 1) {
				beginOffset = doc.getLineOffset(lineNum);
				endOffset = doc.getLineOffset(lineNum+1) - 1;
				String nextLine = doc.get(beginOffset, endOffset - beginOffset + 1);
				if (nextLine.trim().isEmpty()) {
					isCurrentLineEmpty = true;
					lineNum++;
				}
				else if (!isCurrentLineEmpty)
					lineNum++;
				else
					break;
			}
			int newOffset = doc.getLineOffset(lineNum);
			editor.selectAndReveal(newOffset, 0);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
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
