package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
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

/** This action increases or decreases the indentation of the selection in the current editor. */
public class ShiftLeftAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {

		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof TextEditor) {
					TextEditor editor = (TextEditor) editorPart;

					TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();

					int selStart = selection.getOffset();
					int selEnd = selStart + selection.getLength();

					// the last selected character can be a newline
					if (selEnd > selStart)
						selEnd--;

					IEditorInput input = editor.getEditorInput();
					IDocument document = editor.getDocumentProvider().getDocument(input);

					int startOffset;
					int endOffset;
					try {
						int startLine = document.getLineOfOffset(selStart);
						int endLine = document.getLineOfOffset(selEnd);

						startOffset = document.getLineOffset(startLine);
						IRegion endLineInfo = document.getLineInformation(endLine);
						endOffset = endLineInfo.getOffset() + endLineInfo.getLength();

						String result = decreaseIndentation(document.get(startOffset, endOffset - startOffset));
						document.replace(startOffset, endOffset - startOffset, result);

						// reselect the initially selected lines
						startOffset = document.getLineOffset(startLine);
						endLineInfo = document.getLineInformation(endLine);
						endOffset = endLineInfo.getOffset() + endLineInfo.getLength();

						TextSelection sel = new TextSelection(startOffset, endOffset - startOffset);
						editor.getSelectionProvider().setSelection(sel);

					} catch (BadLocationException e) {
						OcamlPlugin.logError("Wrong offset", e);
						return;
					}

				} else
					OcamlPlugin.logError("CommentSelectionAction: only works on ml and mli files");

			} else
				OcamlPlugin.logError("CommentSelectionAction: editorPart is null");
		} else
			OcamlPlugin.logError("CommentSelectionAction: page is null");

	}

	private String decreaseIndentation(String input) {
		final int tabSize = OcamlEditor.getTabSize();

		// split the string into lines
		String[] lines = input.split("\\r?\\n");

		if (lines.length > 0) {
			// compute decrease size
			int decrease = tabSize;
			for (String line : lines) {
				if (!line.trim().isEmpty()) {
					int indent = calculateIndent(line, tabSize);
					if (indent < decrease)
						decrease = indent;
				}
			}
			if (decrease == 0)		// nothing left to decrease
				return input;
			
			// subtract 1 indentation from each lines
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (line.length() >= decrease) {
					int d = 0; 
					int u = 0;
					for (u = 0; u < line.length(); u++) {
						if (line.charAt(u) == ' ')
							d++;
						else if (line.charAt(u) == '\t')
							d = d + tabSize;
						if (d >= decrease)
							break;
					}
					String newline = "";
					for (int j = 0; j < d - decrease; j++)
						newline = newline + ' ';
					newline = newline + line.substring(u + 1);
					lines[i] = newline;
				}
			}
			
			// rebuild a string from the lines
			StringBuilder builder = new StringBuilder();
			for (String line : lines)
				builder.append(line + OcamlPlugin.newline);
			// remove the last newline
			builder.setLength(builder.length() - OcamlPlugin.newline.length());
	
			return builder.toString();
		}
		else 
			return input;
	}

	private int calculateIndent(String line, int tabSize) {
		int indent = 0;
		for (int i = 0; i < line.length(); i++)
			if (line.charAt(i) == '\t')
				indent += tabSize;
			else if (line.charAt(i) == ' ')
				indent++;
			else
				break;

		return indent;
	}
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
