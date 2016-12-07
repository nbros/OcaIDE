package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

public class SelectDownwardByIndent implements IWorkbenchWindowActionDelegate {

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
		
		Point selection = styledText.getSelection();
		int startOffset = (selection.x < cursorOffset) ? selection.x : selection.y;

		try {
			int lineNum= doc.getLineOfOffset(cursorOffset);
			int numOfLine = doc.getNumberOfLines();
			int docLen = doc.getLength();
			
			if (lineNum == (numOfLine - 2)) {
				int lastLineOffset = doc.getLineOffset(lineNum+1);
				styledText.setSelection(startOffset, lastLineOffset);
				return;
			}
			
			if (lineNum >= (numOfLine - 1)) {
				styledText.setSelection(startOffset, doc.getLength());
				return;
			}
			
			// ignore empty lines while going down, go to the first non empty line
			int beginOffset = doc.getLineOffset(lineNum);
			int endOffset = doc.getLineOffset(lineNum+1) - 1;
			String currentLine = doc.get(beginOffset, endOffset - beginOffset + 1);
			
			if (currentLine.trim().isEmpty()) {
				lineNum++;
				while (lineNum < numOfLine) {
					beginOffset = doc.getLineOffset(lineNum);
					endOffset = (lineNum < numOfLine - 1) ? doc.getLineOffset(lineNum+1) - 1 : docLen - 1;
					currentLine = doc.get(beginOffset, endOffset - beginOffset + 1);
					if (currentLine.trim().isEmpty())
						lineNum++;
					else 
						break;
				}
				if (lineNum >= numOfLine)
					lineNum = numOfLine - 1;
				int newOffset = doc.getLineOffset(lineNum);
				int k = newOffset;
				while (k < docLen) {
					Character ch = doc.getChar(k);
					if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r')
						break;
					else {
						if (ch == '\n' || ch == '\r')
							newOffset = k + 1;
						k++;
					}
				}
				
				styledText.setSelection(startOffset, newOffset);
				return;
			}

			// find next line which has different identation
			int currentIndent = computeIndent(currentLine);
			lineNum++;
			while (lineNum < numOfLine - 1) {
				beginOffset = doc.getLineOffset(lineNum);
				endOffset = (lineNum < numOfLine - 1) ? doc.getLineOffset(lineNum+1) - 1 : docLen - 1;
				String nextLine = doc.get(beginOffset, endOffset - beginOffset + 1);
				int nextIndent = computeIndent(nextLine);
				if (currentIndent != nextIndent) {
					break;
				}
				else if (nextLine.trim().isEmpty()) {
					currentIndent = -1;
					lineNum++;
				}
				else {
					lineNum++;
					currentLine = nextLine;
					currentIndent = nextIndent;
				}
			}
			
			// find location to jump to
			int newOffset = doc.getLineOffset(lineNum);
			int k = newOffset;
			while (k < docLen) {
				Character ch = doc.getChar(k);
				if (ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r')
					break;
				else {
					if (ch == '\n' || ch == '\r')
						newOffset = k + 1;
					k++;
				}
			}
			
			styledText.setSelection(startOffset, newOffset);
		} catch (BadLocationException e) {
			e.printStackTrace();
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
