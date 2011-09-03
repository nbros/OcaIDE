package ocaml.editor.actions;

import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.util.OcamlCharacterPairMatcher;
import ocaml.editors.yacc.OcamlyaccEditor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * This action makes the caret in the OCaml editor jump from the delimiter under the caret to the
 * corresponding delimiter (for example, if an opening parenthesis is under the caret, then this action will
 * move the caret to the corresponding closing parenthesis).
 */
public class GotoMatchingDelimiterAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					ISelection sel = editor.getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
						TextSelection selection = (TextSelection) sel;

						IEditorInput input = editor.getEditorInput();
						IDocument document = editor.getDocumentProvider().getDocument(input);
						int offset = selection.getOffset();
						IRegion region = new OcamlCharacterPairMatcher().match(document, offset);

						if (region != null) {
							int pos = region.getOffset() + 1;
							if (pos >= 0 && pos < document.getLength())
								editor.selectAndReveal(pos, 0);
						}
					}

				} else if (editorPart instanceof OcamllexEditor) {
					OcamllexEditor editor = (OcamllexEditor) editorPart;

					ISelection sel = editor.getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
						TextSelection selection = (TextSelection) sel;

						IEditorInput input = editor.getEditorInput();
						IDocument document = editor.getDocumentProvider().getDocument(input);
						int offset = selection.getOffset();
						IRegion region = new OcamlCharacterPairMatcher().match(document, offset);

						if (region != null) {
							int pos = region.getOffset() + 1;
							if (pos >= 0 && pos < document.getLength())
								editor.selectAndReveal(pos, 0);
						}
					}

				} else if (editorPart instanceof OcamlyaccEditor) {
					OcamlyaccEditor editor = (OcamlyaccEditor) editorPart;

					ISelection sel = editor.getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
						TextSelection selection = (TextSelection) sel;

						IEditorInput input = editor.getEditorInput();
						IDocument document = editor.getDocumentProvider().getDocument(input);
						int offset = selection.getOffset();
						IRegion region = new OcamlCharacterPairMatcher().match(document, offset);

						if (region != null) {
							int pos = region.getOffset() + 1;
							if (pos >= 0 && pos < document.getLength())
								editor.selectAndReveal(pos, 0);
						}
					}

				}

			}
		}
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void dispose() {
	}
}
