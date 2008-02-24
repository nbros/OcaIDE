package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.util.Misc;
import ocaml.views.toplevel.OcamlCustomToplevelView;
import ocaml.views.toplevel.OcamlToplevelView;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Action from the O'Caml editor to evaluate a piece of code in the last focused toplevel.
 */
public class ToplevelQuickEvalAction implements IWorkbenchWindowActionDelegate {
	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;

					// Misc.showView(OcamlToplevelView.ID);
					/*
					 * Show the last focused toplevel view (in which the user input will be evaluated), so
					 * that the user can see the result of the evaluation
					 */
					OcamlToplevelView lastFocusedToplevelInstance = OcamlPlugin
							.getLastFocusedToplevelInstance();
					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					try {
						String id = null;
						if (lastFocusedToplevelInstance instanceof OcamlCustomToplevelView) {
							id = OcamlCustomToplevelView.ID;
						} else if (lastFocusedToplevelInstance instanceof OcamlToplevelView) {
							id = OcamlToplevelView.ID;
						}

						activePage.showView(id, lastFocusedToplevelInstance.getSecondaryId(),
								IWorkbenchPage.VIEW_VISIBLE);
					} catch (PartInitException e) {
						OcamlPlugin.logError("Error showing toplevel view", e);
					}

					ISelection sel = editor.getSelectionProvider().getSelection();
					if (sel instanceof TextSelection) {
						TextSelection selection = (TextSelection) sel;

						IEditorInput input = editor.getEditorInput();
						IDocument document = editor.getDocumentProvider().getDocument(input);
						String strSelected = null;
						try {
							strSelected = document.get(selection.getOffset(), selection.getLength());
						} catch (BadLocationException e1) {
							OcamlPlugin.logError("ocaml plugin error", e1);
							return;
						}

						strSelected = strSelected.trim();

						// If nothing is selected, then evaluate the current line
						if (strSelected.equals("")) {
							// Only spaces are selected
							if (selection.getLength() != 0)
								return;

							try {
								IRegion line = document.getLineInformationOfOffset(selection.getOffset());
								strSelected = document.get(line.getOffset(), line.getLength());
								strSelected = strSelected.trim();

								if (strSelected.equals(""))
									return;
							} catch (BadLocationException e1) {
								OcamlPlugin.logError("ocaml plugin error", e1);
								return;
							}

						}

						/*
						 * Add semicolons at the end if they are missing, so that the toplevel will evaluate
						 * the expression
						 */
						if (!strSelected.endsWith(";;"))
							strSelected = strSelected + ";;";

						// TODO: find last focused custom toplevel too

						// ask the last focused toplevel view to evaluate the selection
						OcamlToplevelView.eval(strSelected);

						editor.setFocus();
					}

				} else
					MessageDialog.openInformation(window.getShell(), "Ocaml Plugin",
							"this operation is only supported on .ml files");

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
