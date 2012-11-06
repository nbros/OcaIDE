package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * This action converts spaces to tabs in the current editor.
 */
public class ConvertSpacesToTabsAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					try {
						TextEditor editor = (TextEditor) editorPart;
						IEditorInput input = editor.getEditorInput();
						IDocument doc = editor.getDocumentProvider().getDocument(input);
						
						// collect text in the selection and convert
						ISelectionProvider selectionProvider = editor.getSelectionProvider();
						TextSelection textSelection = (TextSelection) selectionProvider.getSelection();
						String selectedText = textSelection.getText();
						if (selectedText.equals("")) {
							MessageDialog.openInformation(window.getShell(), "Ocaml Plugin", "Please make a selection first!");
							return;
						}
						String newSelectedText = replaceSpacesByTabs(OcamlEditor.getTabSize(), selectedText); 
						int selectedOffset = textSelection.getOffset();
						
						// collect text before the selection 
						int beforeLength = selectedOffset;
						String beforeSelectedText = doc.get(0, beforeLength); 

						// collect the text after the selection
						int afterOffset = selectedOffset + textSelection.getLength();
						String afterSelectedText = doc.get(afterOffset, doc.getLength() - afterOffset); 

						// replace data of document
						String newText = beforeSelectedText + newSelectedText + afterSelectedText;
						doc.replace(0, doc.getLength(), newText);
						
						// set the converted text to be selected
						TextSelection newTextSelection = new TextSelection(selectedOffset, newSelectedText.length());
						selectionProvider.setSelection(newTextSelection);
					} catch (BadLocationException e1) {
						OcamlPlugin.logError("bad location while converting spaces to tabs", e1);
						return;
					}

				} else
					MessageDialog.openInformation(window.getShell(), "Ocaml Plugin",
							"Not implemented for this editor.");

			}
		}
	}

	private String replaceSpacesByTabs(int tabSize, String doc) {
		StringBuilder stringBuilder = new StringBuilder();

		int nSpaces = 0;
		boolean lineStart = true;
		for (int i = 0; i < doc.length(); i++) {
			char c = doc.charAt(i);

			if (c == ' ' && lineStart)
				nSpaces++;
			else if (c == '\t' && lineStart)
				nSpaces += tabSize;
			else if (lineStart) {
				lineStart = false;
				int nTabs = nSpaces / tabSize;
				for (int j = 0; j < nTabs; j++)
					stringBuilder.append('\t');
				nSpaces = 0;
				stringBuilder.append(c);
			} else
				stringBuilder.append(c);

			if (c == '\n')
				lineStart = true;
		}

		return stringBuilder.toString();
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}
}
