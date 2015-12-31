package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

public class MoveCursorUpwardByIndent implements IWorkbenchWindowActionDelegate {

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
			int lineNum = doc.getLineOfOffset(cursorOffset);
			int numOfLine = doc.getNumberOfLines(); 
			int docLen = doc.getLength();
			
			if (lineNum <= 0) {
				editor.selectAndReveal(0, 0);
				return;
			}
			
			// ignore empty lines when going back;
			int beginOffsetCurrentLine = 0;
			int endOffsetCurrentLine = 0;
			String currentLine = "";
			while (lineNum > 0) {
				beginOffsetCurrentLine = doc.getLineOffset(lineNum);
				endOffsetCurrentLine = (lineNum < numOfLine-1) ? doc.getLineOffset(lineNum+1) - 1 : docLen - 1;
				currentLine = doc.get(beginOffsetCurrentLine, endOffsetCurrentLine - beginOffsetCurrentLine + 1);
				if (currentLine.trim().isEmpty()) {
					lineNum--;
				}
				else
					break;
			}
			if (lineNum == 0) {
				editor.selectAndReveal(0, 0);
				return;
			}

			
			// if previous line has different indentation and cursor is not
			// in the beginning of current block, then go to the beginning.
			beginOffsetCurrentLine = doc.getLineOffset(lineNum);
			int cursorColumn = cursorOffset - beginOffsetCurrentLine;
			int newOffset = beginOffsetCurrentLine;
			while (newOffset < docLen) {
				Character ch = doc.getChar(newOffset);
				if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n')
					break;
				else
					newOffset++;
			}
			int currentIndent = newOffset - beginOffsetCurrentLine;
			

			int beginOffsetPrevLine = doc.getLineOffset(lineNum-1);
			int endOffsetPrevLine = doc.getLineOffset(lineNum) - 1;
			String prevLine = doc.get(beginOffsetPrevLine, endOffsetPrevLine - beginOffsetPrevLine + 1);
			int prevIndent = computeIndent(prevLine);
			if (prevLine.trim().isEmpty() && cursorColumn > currentIndent) {
				newOffset = beginOffsetCurrentLine + currentIndent;
				editor.selectAndReveal(newOffset, 0);
				return;
			}
			else if ((prevIndent != currentIndent) && (cursorColumn > currentIndent)) {
				newOffset = beginOffsetCurrentLine + currentIndent;
				editor.selectAndReveal(newOffset, 0);
				return;
			}
			
			// ignore empty lines when going back;
			lineNum--;
			endOffsetCurrentLine = 0;
			currentLine = "";
			while (lineNum > 0) {
				beginOffsetCurrentLine = doc.getLineOffset(lineNum);
				endOffsetCurrentLine = (lineNum < numOfLine-1) ? doc.getLineOffset(lineNum+1) - 1 : docLen;
				currentLine = doc.get(beginOffsetCurrentLine, endOffsetCurrentLine - beginOffsetCurrentLine + 1);
				if (currentLine.trim().isEmpty()) {
					lineNum--;
				}
				else
					break;
			}
			if (lineNum == 0) {
				editor.selectAndReveal(0, 0);
				return;
			}

			
			// search back to find the last line has different indentation  
			currentIndent = computeIndent(currentLine);
			while (lineNum > 0) {
				beginOffsetPrevLine = doc.getLineOffset(lineNum-1);
				endOffsetPrevLine = doc.getLineOffset(lineNum) - 1;
				prevLine = doc.get(beginOffsetPrevLine, endOffsetPrevLine - beginOffsetPrevLine + 1);
				prevIndent = computeIndent(prevLine);
				
				if (prevLine.trim().isEmpty()) {
					break;
				}
				else if (prevIndent == currentIndent) {
					lineNum--;
					currentLine = prevLine;
				}
				else
					break;
			}
			newOffset = doc.getLineOffset(lineNum);
			while (newOffset < docLen) {
				Character ch = doc.getChar(newOffset);
				if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n')
					break;
				else
					newOffset++;
			}
			editor.selectAndReveal(newOffset, 0);
		} catch (BadLocationException e) {
			return;
		}
	}
	
	private int computeIndent(String line) {
		if (line.trim().isEmpty())
			return 0;

		if (line.charAt(0) == ' ')
			return 1 + computeIndent(line.substring(1));
		
		if (line.charAt(0) == '\t')
			return OcamlEditor.getTabSize() + computeIndent(line.substring(1));
		
		return 0;
	}
    
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
