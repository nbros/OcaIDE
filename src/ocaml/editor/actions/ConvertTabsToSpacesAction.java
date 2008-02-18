package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * This action converts tabs to spaces in the current editor.
 */
public class ConvertTabsToSpacesAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof TextEditor) {
					TextEditor editor = (TextEditor) editorPart;

					IEditorInput input = editor.getEditorInput();
					IDocument document = editor.getDocumentProvider().getDocument(input);

					String result = replaceTabsBySpaces(OcamlEditor.getTabSize(), document.get());

					try {
						document.replace(0, document.getLength(), result);
					} catch (BadLocationException e1) {
						OcamlPlugin.logError("bad location while converting tabs to spaces", e1);
						return;
					}

				} else
					MessageDialog.openInformation(window.getShell(), "Ocaml Plugin",
							"Not implemented for this editor.");

			}
		}
	}

	private String replaceTabsBySpaces(int tabSize, String doc) {

		StringBuilder builderTab = new StringBuilder();
		for (int i = 0; i < tabSize; i++)
			builderTab.append(" ");
		final String tab = builderTab.toString();

		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i < doc.length(); i++) {
			char c = doc.charAt(i);

			if (c == '\t')
				stringBuilder.append(tab);
			else
				stringBuilder.append(c);
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
