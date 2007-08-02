package ocaml.views.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
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
public final class OcamlOutlineControl extends ContentOutlinePage {

	protected Object input;

	protected OcamlEditor editor;

	/** Whether to always expand modules in the outline */
	private boolean expandModules;

	/** Whether to always expand classes in the outline */
	private boolean expandClasses;

	/** Whether to always fully expand the outline */
	private boolean expandAll;
	
	public static final boolean bOutlineDebugButton = false;

	/**
	 * Creates a content outline page using the given provider and the given editor.
	 */
	public OcamlOutlineControl(OcamlEditor editor) {
		super();
		this.editor = editor;
	}
	
	@Override
	public void init(IPageSite pageSite) {
		super.init(pageSite);

		
		/* Create an action in the outline toolbar to switch between "expand all" mode and normal mode */
		IToolBarManager toolBarManager = pageSite.getActionBars().getToolBarManager();
		
		ImageDescriptor iconExpandAll = ImageRepository.getImageDescriptor(ImageRepository.ICON_EXPAND_ALL);
		
		final OcamlOutlineControl outline = this;
		Action actionExpandAll = new BooleanPropertyAction("Expand All", OcamlPlugin.getInstance().getPreferenceStore(), PreferenceConstants.P_OUTLINE_EXPAND_ALL){
			@Override
			public void run() {
				super.run();
				outline.update();
			}
		};
		
		actionExpandAll.setImageDescriptor(iconExpandAll);
		
		toolBarManager.add(actionExpandAll);
		

		if(bOutlineDebugButton){
			/* Create an action in the outline toolbar to switch between debug mode and normal mode */
			Action actionDebug = new BooleanPropertyAction("Debug", OcamlPlugin.getInstance().getPreferenceStore(), PreferenceConstants.P_OUTLINE_DEBUG_MODE){
				@Override
				public void run() {
					super.run();
					editor.rebuildOutline(0);
					outline.update();
				}
			};
			toolBarManager.add(actionDebug);
		}
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

				IRegion region = def.getRegion(document);
				
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
						
						// XXX DEBUG
						//if(offset < region.getOffset() || offset > region.getOffset() + region.getLength())
						
						//IRegion region2 = def.getFullRegion(document);
						//if(def.defPosStart != 0)
						//editor.selectAndReveal(def.defOffsetStart, def.defOffsetEnd - def.defOffsetStart);
						//System.err.println("-*-*-*-*-*\n" + def.comment);
						//System.err.println("-+-+-+-+\n" + def.sectionComment);
						//editor.selectAndReveal(def.defOffsetStart, 1);
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
				
				lookPreferences();
				if(expandAll)
					viewer.expandAll();
				else
					expandChosenElements(viewer);
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
					
					lookPreferences();
					if(expandAll)
						viewer.expandAll();
					else{
						viewer.collapseAll();
						expandChosenElements(viewer);
					}
					viewer.addSelectionChangedListener(this);
					TreePath treePath = new TreePath(new Object[] { element });
					viewer.setSelection(new TreeSelection(treePath), true);
					//viewer.reveal(treePath);
				}
			}
		}
	}


	/** Expands some elements the user has chosen to always expand in the outline */
	private void expandChosenElements(TreeViewer viewer) {
		if(editor == null)
			return;
		
		Def root = editor.getOutlineDefinitionsTree();
		
		expandChosenElementsAux(viewer, root);
		
	}

	/** Recursive helper function */
	private void expandChosenElementsAux(TreeViewer viewer, Def def) {
		if(def.type == Def.Type.Module && expandModules)
			viewer.expandToLevel(def, 1);
		
		else if(def.type == Def.Type.Class && expandClasses)
			viewer.expandToLevel(def, 1);
		
		for(Def child: def.children)
			expandChosenElementsAux(viewer, child);
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
	
	/** Set the boolean variables from the user preferences */
	private void lookPreferences() {
		IPreferenceStore preferenceStore = OcamlPlugin.getInstance().getPreferenceStore();
		
		expandAll = preferenceStore.getBoolean(PreferenceConstants.P_OUTLINE_EXPAND_ALL);
		expandModules = preferenceStore.getBoolean(PreferenceConstants.P_OUTLINE_EXPAND_MODULES);
		expandClasses = preferenceStore.getBoolean(PreferenceConstants.P_OUTLINE_EXPAND_CLASSES);
		
	}
}