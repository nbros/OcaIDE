package ocaml.editors.yacc.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
import ocaml.parser.Def;
import ocaml.preferences.PreferenceConstants;
import ocaml.util.ImageRepository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.BooleanPropertyAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/** Implements the outline view for the O'Caml editor */
public final class OcamlYaccOutlineControl extends ContentOutlinePage {

	protected Object input;

	protected OcamlyaccEditor editor;

	public OcamlYaccOutlineControl(OcamlyaccEditor editor) {
		super();
		this.editor = editor;
	}
	
	/** Install the content provider and label provider */
	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = this.getTreeViewer();
		viewer.setContentProvider(new OcamlYaccOutlineContentProvider());
		viewer.setLabelProvider(new OcamlYaccOutlineLabelProvider());
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
			if (element instanceof YaccDef) {
				YaccDef def = (YaccDef) element;

				IRegion region = new Region(def.start, def.end - def.start);
				
					ISelection editorSel = editor.getSelectionProvider().getSelection();
					if (editorSel instanceof TextSelection) {
						editor.selectAndReveal(region.getOffset(), region.getLength());
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
				viewer.expandAll();
				tree.setRedraw(true);
			}
		}
	}
}