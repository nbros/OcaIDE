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

/** This action comments or uncomments the selection in the current editor. */
public class CommentSelectionAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private boolean bProtectedComment = false;

	public void run(IAction action) {

		bProtectedComment = "Ocaml_sourceActions_CommentSelectionProtected".equals(action.getId());

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
					if (selEnd > 1)
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

						String result = switchComment(document.get(startOffset, endOffset - startOffset));
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

	private String switchComment(String input) {

		final int tabSize = OcamlEditor.getTabSize();

		// split the string into lines
		String[] lines = input.split("\\r?\\n");

		// uncomment
		if (isCommented(lines)) {
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				lines[i] = unComment(line);
			}
		}
		// comment
		else {
			// find the longest line
			int longest = 0;
			for (String line : lines) {
				int length = calculateLength(line, tabSize);
				if (length > longest)
					longest = length;
			}

			// find the shortest indentation
			int shortest = longest;
			for (String line : lines) {
				if (!line.trim().equals("")) {
					int indent = calculateIndent(line, tabSize);
					if (indent < shortest)
						shortest = indent;
				}
			}

			// comment all the lines
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				lines[i] = comment(line, shortest, longest, tabSize);
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

	private int calculateLength(String line, int tabSize) {
		int length = 0;
		for (int i = 0; i < line.length(); i++)
			if (line.charAt(i) == '\t')
				length += tabSize;
			else
				length++;

		return length;
	}

	// calculate the indentation size of a line
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

	/** Return <code>line</code> commented so that it measures <code>length</code> characters */
	// comment character should be inserted at the shortest indentation position
	// and at the end of longest line.
	private String comment(String line, int indent, int length, int tabSize) {
		
		if (line.trim().equals("")) 
			return line;

		StringBuilder builder = new StringBuilder();

		int position = 0;
		for (int i = 0; i < line.length(); i++) {
			if (position == indent) {
				if (bProtectedComment)
					builder.append("(*| ");
				else
					builder.append("(* ");
			}
			builder.append(line.charAt(i));
			if (line.charAt(i) == '\t')
				position += tabSize;
			else
				position++;
		}

		int trailingSpaces = length - calculateLength(line, tabSize);

		for (int i = 0; i < trailingSpaces; i++)
			builder.append(" ");

		builder.append(" *)");

		return builder.toString();

	}

	/** Uncomment this line if it is commented */
	// uncomments and preserves the alignment of source code
	private String unComment(String line) {
		String lineContent = line.trim();
		boolean is_uncomment = false;
		StringBuilder builder = new StringBuilder();

		if (bProtectedComment) {
			if (isCommentedProtected(lineContent)) {
				lineContent = trimEnd(lineContent.substring(4, lineContent.length() - 3));
				is_uncomment = true;
 			}
		}

		if (isCommented(lineContent)) {
			lineContent = trimEnd(lineContent.substring(3, lineContent.length() - 3));
			is_uncomment = true;
		}

		if (is_uncomment) {
			for (int i = 0; i < line.length(); i++)
				if ((line.charAt(i) == ' ') || (line.charAt(i) == '\t'))
					builder.append(line.charAt(i));
				else
					break;
			builder.append(lineContent);
			line = builder.toString();
		}

		return line;

	}

	/** Remove the terminating blank space from this line and return the result */
	private String trimEnd(String str) {
		char[] chars = str.toCharArray();

		int length = chars.length;

		while (length > 0 && chars[length - 1] <= ' ') {
			length--;
		}

		return str.substring(0, length);
	}

	/** Are all the lines commented? */
	private boolean isCommented(String[] lines) {

		for (String line : lines) {
			String s = line.trim();
			if (!s.equals("") && !isCommented(s))
				return false;
		}

		return true;
	}

	private boolean isCommented(String line) {
		return line.startsWith("(* ") && line.endsWith(" *)");
	}

	private boolean isCommentedProtected(String line) {
		return line.startsWith("(*| ") && line.endsWith(" *)");
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
