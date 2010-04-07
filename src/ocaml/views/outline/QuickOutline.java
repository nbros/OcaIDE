package ocaml.views.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class QuickOutline extends PopupDialog {

	/** The control's text widget */
	private Text fFilterText;
	/** The control's tree widget */
	private TreeViewer fTreeViewer;
	/** The input of the outline tree viewer */
	private final Object input;
	private final OcamlEditor editor;

	public QuickOutline(Shell parent, Object input, OcamlEditor editor) {
		super(parent, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, true, true, true, true, true, null,
				null);
		this.input = input;
		this.editor = editor;
	}

	protected Text getFilterText() {
		return fFilterText;
	}

	protected Text createFilterText(Composite parent) {
		fFilterText = new Text(parent, SWT.NONE);
		Dialog.applyDialogFont(fFilterText);

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.CENTER;
		fFilterText.setLayoutData(data);

		fFilterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.CR) // return
					gotoSelectedElement();
				if (e.keyCode == SWT.ARROW_DOWN)
					fTreeViewer.getTree().setFocus();
				if (e.keyCode == SWT.ARROW_UP)
					fTreeViewer.getTree().setFocus();
				if (e.character == SWT.ESC)
					QuickOutline.this.close();
			}

			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// refresh viewer to re-filter
				fTreeViewer.getControl().setRedraw(false);
				fTreeViewer.refresh();
				fTreeViewer.expandAll();
				selectFirstMatch();
				fTreeViewer.getControl().setRedraw(true);
			}
		});
		return fFilterText;
	}
	
	/**
	 * Selects the first element in the tree which
	 * matches the current filter pattern.
	 */
	protected void selectFirstMatch() {
		final Tree tree= fTreeViewer.getTree();
		TreeItem element = findElement(tree.getItems());

		if (element != null) {
			tree.setSelection(element);
			tree.showItem(element);
		} else
			fTreeViewer.setSelection(StructuredSelection.EMPTY);
	}

	private TreeItem findElement(TreeItem[] items) {
		return findElement(items, null, true);
	}

	private TreeItem findElement(TreeItem[] items, TreeItem[] toBeSkipped, boolean allowToGoUp) {
		// First search at same level
		for (int i= 0; i < items.length; i++) {
			final TreeItem item= items[i];
			Def def = (Def)item.getData();
			if (def != null) {
				if(matchesFilter(def))
					return item;
			}
		}

		// Go one level down for each item
		for (int i= 0; i < items.length; i++) {
			final TreeItem item= items[i];
			TreeItem foundItem= findElement(selectItems(item.getItems(), toBeSkipped), null, false);
			if (foundItem != null)
				return foundItem;
		}

		if (!allowToGoUp || items.length == 0)
			return null;

		// Go one level up (parent is the same for all items)
		TreeItem parentItem= items[0].getParentItem();
		if (parentItem != null)
			return findElement(new TreeItem[] { parentItem }, items, true);

		// Check root elements
		return findElement(selectItems(items[0].getParent().getItems(), items), null, false);
	}
	
	private TreeItem[] selectItems(TreeItem[] items, TreeItem[] toBeSkipped) {
		if (toBeSkipped == null || toBeSkipped.length == 0)
			return items;

		int j= 0;
		for (int i= 0; i < items.length; i++) {
			TreeItem item= items[i];
			if (!canSkip(item, toBeSkipped))
				items[j++]= item;
		}
		if (j == items.length)
			return items;

		TreeItem[] result= new TreeItem[j];
		System.arraycopy(items, 0, result, 0, j);
		return result;
	}
	
	private boolean canSkip(TreeItem item, TreeItem[] toBeSkipped) {
		if (toBeSkipped == null)
			return false;
		
		for (int i= 0; i < toBeSkipped.length; i++) {
			if (toBeSkipped[i] == item)
				return true;
		}
		return false;
	}

	protected void gotoSelectedElement() {
		ITreeSelection selection = (ITreeSelection) fTreeViewer.getSelection();
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof Def) {
			Def def = (Def) firstElement;
			
			IDocument document = editor.getDocumentProvider().getDocument(
					editor.getEditorInput());
			IRegion region = def.getRegion(document);

			if (region != null) {
				editor.selectAndReveal(region.getOffset(), region.getLength());
				close();
			}
		}
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		fFilterText = createFilterText(parent);
		return fFilterText;
	}

	/**
	 * Create the main content for this information control.
	 * 
	 * @param parent
	 *            The parent composite
	 * @return The control representing the main content.
	 */
	protected Control createDialogArea(Composite parent) {
		fTreeViewer = createTreeViewer(parent);

		final Tree tree = fTreeViewer.getTree();
		tree.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.ESC)
					QuickOutline.this.close();
			}

			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		tree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// do nothing
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				gotoSelectedElement();
			}
		});

		tree.addMouseMoveListener(new MouseMoveListener() {
			TreeItem fLastItem = null;

			public void mouseMove(MouseEvent e) {
				if (tree.equals(e.getSource())) {
					Object o = tree.getItem(new Point(e.x, e.y));
					if (o instanceof TreeItem) {
						Rectangle clientArea = tree.getClientArea();
						if (!o.equals(fLastItem)) {
							fLastItem = (TreeItem) o;
							tree.setSelection(new TreeItem[] { fLastItem });
						} else if (e.y - clientArea.y < tree.getItemHeight() / 4) {
							// Scroll up
							Point p = tree.toDisplay(e.x, e.y);
							Item item = fTreeViewer.scrollUp(p.x, p.y);
							if (item instanceof TreeItem) {
								fLastItem = (TreeItem) item;
								tree.setSelection(new TreeItem[] { fLastItem });
							}
						} else if (clientArea.y + clientArea.height - e.y < tree.getItemHeight() / 4) {
							// Scroll down
							Point p = tree.toDisplay(e.x, e.y);
							Item item = fTreeViewer.scrollDown(p.x, p.y);
							if (item instanceof TreeItem) {
								fLastItem = (TreeItem) item;
								tree.setSelection(new TreeItem[] { fLastItem });
							}
						}
					}
				}
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {

				if (tree.getSelectionCount() < 1)
					return;

				if (e.button != 1)
					return;

				if (tree.equals(e.getSource())) {
					Object o = tree.getItem(new Point(e.x, e.y));
					TreeItem selection = tree.getSelection()[0];
					if (selection.equals(o))
						gotoSelectedElement();
				}
			}
		});

		return fTreeViewer.getControl();
	}

	protected TreeViewer createTreeViewer(Composite parent) {
		Tree tree = new Tree(parent, SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = tree.getItemHeight() * 12;
		tree.setLayoutData(gd);

		final TreeViewer treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new OcamlOutlineContentProvider());
		treeViewer.setLabelProvider(new OcamlOutlineDecoratingLabelProvider(
				new OcamlOutlineLabelProvider(), null));
		// viewer.addSelectionChangedListener(this);
		treeViewer.setInput(this.input);

		// filters the tree depending on the contents of the filter text field
		treeViewer.addFilter(new NamePatternFilter());

		treeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		
		return treeViewer;
	}
	
	/** Returns dialog settings used to persist size and location */
	protected IDialogSettings getDialogSettings() {
		String sectionName = "ocaml.views.outline.QuickOutline.dialogSettings";
		IDialogSettings settings = OcamlPlugin.getInstance().getDialogSettings().getSection(
				sectionName);
		if (settings == null)
			settings = OcamlPlugin.getInstance().getDialogSettings().addNewSection(sectionName);
		return settings;
	}


	/** Filters names that start with the filter, or contain it, if the filter starts with '*' */
	private boolean matchesFilter(Object element) {
		if(fTreeViewer == null)
			return true;
		
		if (element instanceof Def) {
			Def def = (Def) element;
			String matchName = ((ILabelProvider) fTreeViewer.getLabelProvider()).getText(def);
			String filterText = getFilterText().getText();
			
			if(matchName != null) {
				if(filterText.startsWith("*")) {
					return matchName.contains(filterText.substring(1));
				} else {
					return matchName.startsWith(filterText);
				}
			}
		}
		return false;
	}
	
	/**
	 * The NamePatternFilter filters the tree depending on the contents of the
	 * filter text field
	 */
	protected class NamePatternFilter extends ViewerFilter {

		public NamePatternFilter() {
		}

		/*
		 * @see
		 * org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers
		 * .Viewer, java.lang.Object, java.lang.Object)
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if(fTreeViewer == null)
				return true;
			
			if(matchesFilter(element))
				return true;
			
			return hasUnfilteredChild(fTreeViewer, element);
		}

		private boolean hasUnfilteredChild(TreeViewer viewer, Object element) {
			Object[] children = ((ITreeContentProvider) viewer.getContentProvider())
					.getChildren(element);
			for (int i = 0; i < children.length; i++)
				if (select(viewer, element, children[i]))
					return true;
			return false;
		}
	}

	public void setFocus() {
		getShell().forceFocus();
		fFilterText.setFocus();
	}
}
