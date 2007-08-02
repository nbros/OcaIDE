package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

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
					OcamlEditor editor = (OcamlEditor) editorPart;

					IDocument doc = editor.getTextViewer().getDocument();
					String result = replaceSpacesByTabs(OcamlEditor.getTabSize(), doc.get());

					try {
						doc.replace(0, doc.getLength(), result);
					} catch (BadLocationException e1) {
						OcamlPlugin.logError("bad location while formatting with camlp4", e1);
						return;
					}

				} else
					MessageDialog.openInformation(window.getShell(), "Ocaml Plugin",
							"Formatting is only implemented for .ml files.");

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
