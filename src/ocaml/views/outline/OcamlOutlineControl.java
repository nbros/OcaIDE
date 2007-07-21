package ocaml.views.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/** Implements the outline view for the O'Caml editor */
public final class OcamlOutlineControl extends ContentOutlinePage {

	protected Object input;

	protected OcamlEditor editor;

	/**
	 * Creates a content outline page using the given provider and the given editor.
	 */
	public OcamlOutlineControl(OcamlEditor editor) {
		super();
		this.editor = editor;
	}

	/** Install the content provider and label provider */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = this.getTreeViewer();
		viewer.setContentProvider(new OcamlOutlineContentProvider());
		viewer.setLabelProvider(new OcamlOutlineDecoratingLabelProvider(new OcamlOutlineLabelProvider(), null));
		viewer.addSelectionChangedListener(this);
		if (this.input != null)
			viewer.setInput(this.input);
	}

	/** Selection in the outline changed: we reselect the corresponding line in the O'Caml editor */
	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		ISelection selection = event.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sel = ((IStructuredSelection) selection);
			Object element = sel.getFirstElement();
			if (element instanceof Def) {
				Def def = (Def) element;

				OcamlEditor editor = ((OcamlEditor) this.editor);
				IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

				IRegion region = null;
				try {
					int start = document.getLineOffset(Def.getLine(def.posStart)) + Def.getColumn(def.posStart);
					int length = Def.getColumn(def.posEnd) - Def.getColumn(def.posStart) + 1;
					region = new Region(start, length);
				} catch (BadLocationException e) {
					OcamlPlugin.logError("ocaml plugin error (bad location)", e);
				}

				if (region != null) {
					ISelection editorSel = editor.getSelectionProvider().getSelection();
					if (editorSel instanceof TextSelection) {
						TextSelection editorSelection = (TextSelection) editorSel;

						int offset = editorSelection.getOffset();
						
						/*
						 * If the editor is already at the right offset, we do nothing. Otherwise, we would
						 * enter an infinite loop, because the editor changes selection in the outline every
						 * time its position changes, and the outline changes the selection in the editor every time
						 * its selection changes.
						 */
						if(offset < region.getOffset() || offset > region.getOffset() + region.getLength())
							editor.selectAndReveal(region.getOffset(), region.getLength());
					}
				}
			}
		}
	}

	/** Set the input of the outline (the module definitions tree) */
	public void setInput(Object input) {
		this.input = input;
		this.update();
	}

	/** Update the outline */
	public void update() {
		TreeViewer viewer = this.getTreeViewer();
		if (viewer != null) {
			Tree tree = (Tree) viewer.getControl();
			if (tree != null && !tree.isDisposed()) {
				// to avoid flicker
				tree.setRedraw(false);
				viewer.setInput(this.input);
				tree.setRedraw(true);
			}
		}
	}

	/**
	 * Select the node in the tree which corresponds to the line and column selected in the editor (it it
	 * exists)
	 */
	public void synchronizeWithEditor(int line, int column) {
		TreeViewer viewer = this.getTreeViewer();
		if (viewer != null) {
			if (this.input instanceof Def) {
				Def def = (Def) this.input;

				Def element = findElementAt(def, line, column);
				if (element != null) {
					/*
					 * We don't want to be notified of a selection change in the outline when we collapse all.
					 * This caused a bug on Windows which created an infinite loop.
					 */
					viewer.removeSelectionChangedListener(this);
					viewer.collapseAll();
					viewer.addSelectionChangedListener(this);
					TreePath treePath = new TreePath(new Object[] { element });
					viewer.setSelection(new TreeSelection(treePath), true);
					//viewer.reveal(treePath);
				}
			}
		}
	}

	/**
	 * Recursively search for the element which is on line number <code>line</code>.
	 * 
	 * @return the element if it exists or <code>null</code>.
	 */
	private Def findElementAt(Def def, int line, int column) {
		if (Def.getLine(def.posStart) == line && Def.getColumn(def.posStart) <= column
				&& Def.getColumn(def.posEnd) >= column - 1)
			return def;

		for (Def d : def.children) {
			Def element = findElementAt(d, line, column);
			if (element != null)
				return element;
		}

		return null;
	}
}