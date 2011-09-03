package ocaml.views;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

import ocaml.OcamlPlugin;
import ocaml.parser.Def;
import ocaml.parsers.OcamlNewInterfaceParser;
import ocaml.preferences.PreferenceConstants;
import ocaml.util.ImageRepository;
import ocaml.views.outline.OcamlOutlineLabelProvider;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

/**
 * Implements the OCaml browser view, which allows the user to browse OCaml libraries, and see all
 * the definitions found in the library mli files.
 * <p>
 * The creation of nodes is done lazily by using a virtual tree. When a node becomes visible, the
 * tree requests information, which triggers a parsing of the corresponding file if it is not
 * already done
 */
public class OcamlBrowserView extends ViewPart {

	/** The data for a directory item in the tree */
	class DirItemData {
		public String[] paths;
	}

	public final static String ID = "ocaml.ocamlBrowserView";

	/** The movable separator between the left and right panes */
	private Sash sash;

	/** The description of the selected element */
	private StyledText text;

	/** a container for the other components */
	private Composite composite;

	/** The SWT tree of found definitions */
	private Tree tree;

	/** The paths in which to look for interfaces. The ocaml library if empty. */
	private ArrayList<String> paths = new ArrayList<String>();

	public OcamlBrowserView() {
	}

	private static FilenameFilter mliFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".mli");
		}
	};

	private void loadPaths() {
		IPreferenceStore preferenceStore = OcamlPlugin.getInstance().getPreferenceStore();
		String strPaths = preferenceStore.getString(PreferenceConstants.P_BROWSER_PATHS);

		if ("".equals(strPaths))
			strPaths = OcamlPlugin.getLibFullPath();

		this.paths = new ArrayList<String>();

		String[] paths = strPaths.split("\\n");
		for (String path : paths) {
			File dir = new File(path);
			if (dir.exists() && dir.isDirectory())
				this.paths.add(path);
		}
	}

	private void savePaths() {
		IPreferenceStore preferenceStore = OcamlPlugin.getInstance().getPreferenceStore();

		StringBuilder stringBuilder = new StringBuilder();
		// add a starting "\n" so that the preference is never empty
		stringBuilder.append("\n");
		for (String path : this.paths) {
			File dir = new File(path);
			if (dir.exists() && dir.isDirectory())
				stringBuilder.append(path + "\n");

		}

		preferenceStore.setValue(PreferenceConstants.P_BROWSER_PATHS, stringBuilder.toString());
	}

	private void buildTree(Tree tree) {

		loadPaths();

		tree.removeAll();

		for (String path : this.paths) {

			TreeItem dirItem = new TreeItem(tree, SWT.NONE);

			dirItem.setImage(ImageRepository.getImage(ImageRepository.ICON_BROWSE));
			dirItem.setText(path);

			File dir = new File(path);
			if (!dir.exists() || !dir.isDirectory()) {
				OcamlPlugin.logError("Wrong path in browser view: " + path);
			} else {

				String[] mliFiles = dir.list(mliFilter);
				for (int i = 0; i < mliFiles.length; i++)
					mliFiles[i] = dir.getAbsolutePath() + File.separatorChar + mliFiles[i];
				Arrays.sort(mliFiles);

				DirItemData dirItemData = new DirItemData();
				dirItemData.paths = mliFiles;
				dirItem.setData(dirItemData);
				dirItem.setItemCount(mliFiles.length);
			}

		}

	}

	/*
	 * private void buildBranch(TreeItem item, Def definition) { for (Def childDefinition :
	 * definition.children) { TreeItem childItem = new TreeItem(item, SWT.NONE);
	 * childItem.setText(childDefinition.name); childItem.setData(childDefinition);
	 * childItem.setImage(OcamlOutlineLabelProvider.retrieveImage(childDefinition));
	 * 
	 * buildBranch(childItem, childDefinition); // childItem.setExpanded(true); } }
	 */

	/*
	 * private Image findImage(Def definition) { Type type = definition.getType();
	 * 
	 * if (type.equals(Type.DefVal)) return ImageRepository.getImage(ImageRepository.ICON_VALUE); if
	 * (type.equals(Type.DefType)) return ImageRepository.getImage(ImageRepository.ICON_TYPE); if
	 * (type.equals(Type.DefClass)) return ImageRepository.getImage(ImageRepository.ICON_CLASS); if
	 * (type.equals(Type.DefException)) return
	 * ImageRepository.getImage(ImageRepository.ICON_EXCEPTION); if (type.equals(Type.DefExternal))
	 * return ImageRepository.getImage(ImageRepository.ICON_EXTERNAL); if
	 * (type.equals(Type.DefModule)) return
	 * ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE); if (type.equals(Type.DefSig))
	 * return ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE_TYPE); if
	 * (type.equals(Type.DefConstructor)) return ImageRepository.getImage(ImageRepository.ICON_C);
	 * 
	 * return null; }
	 */

	/**
	 * Create the components in the view
	 * 
	 * @see ViewPart#createPartControl
	 */
	@Override
	public void createPartControl(Composite parent) {
		this.composite = new Composite(parent, SWT.BORDER);

		this.tree = new Tree(composite, SWT.BORDER | SWT.MULTI | SWT.VIRTUAL);
		this.tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				treeItemSelected();
			}
		});

		/* lazy creation of branches */
		this.tree.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				TreeItem item = (TreeItem) event.item;
				TreeItem parentItem = item.getParentItem();
				Object parentData = parentItem.getData();

				if (parentData instanceof Def) {
					Def parentDef = (Def) parentData;

					int index = parentItem.indexOf(item);
					if (index != -1) {

						Def def = parentDef.children.get(index);

						item.setText(def.name);
						item.setData(def);
						item.setImage(OcamlOutlineLabelProvider.retrieveImage(def));

						item.setItemCount(def.children.size());
					} else
						OcamlPlugin
								.logError("Lazy creation of tree in OcamlBrowserView: index = -1");
				} else if (parentData instanceof DirItemData) {
					DirItemData dirItemData = (DirItemData) parentData;
					int index = parentItem.indexOf(item);
					if (index != -1) {
						OcamlNewInterfaceParser parser = OcamlNewInterfaceParser.getInstance();

						String mliFile = dirItemData.paths[index];
						File file = new File(mliFile);
						Def definition = parser.parseFile(file);
						
						if(definition == null){
							item.setText("<Parsing error>");
							item.setImage(ImageRepository
									.getImage(ImageRepository.ICON_MODULE_PARSER_ERROR));
						}

						// remove the ".mli" extension
						String name = file.getName();
						name = name.substring(0, name.length() - 4);
						// change the first character to uppercase
						name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

						item.setText(name);
						item.setData(definition);

						if (definition.type == Def.Type.Module)
							item.setImage(ImageRepository
									.getImage(ImageRepository.ICON_OCAML_MODULE));
						else
							item.setImage(ImageRepository
									.getImage(ImageRepository.ICON_MODULE_PARSER_ERROR));

						// buildBranch(item, definition);

						item.setData(definition);
						item.setItemCount(definition.children.size());
					}
				}
			}
		});

		buildTree(tree);

		text = new StyledText(this.composite, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
		text.setEditable(false);

		this.text.setText("Ocaml Browser");

		Menu browserPopupMenu = new Menu(this.tree.getShell(), SWT.POP_UP);
		MenuItem itemAdd = new MenuItem(browserPopupMenu, SWT.PUSH);
		itemAdd.setText("Add a location");
		itemAdd.setImage(ImageRepository.getImage(ImageRepository.ICON_ADD));
		itemAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addLocation();
			}

		});

		MenuItem itemAddRec = new MenuItem(browserPopupMenu, SWT.PUSH);
		itemAddRec.setText("Add a location (recursively)");
		itemAddRec.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addLocationRec();
			}

		});

		MenuItem itemRemove = new MenuItem(browserPopupMenu, SWT.PUSH);
		itemRemove.setText("Remove selected location(s)");
		itemRemove.setImage(ImageRepository.getImage(ImageRepository.ICON_DELETE));
		itemRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeLocation();
			}

		});

		MenuItem itemRefresh = new MenuItem(browserPopupMenu, SWT.PUSH);
		itemRefresh.setText("Refresh");
		itemRefresh.setImage(ImageRepository.getImage(ImageRepository.ICON_REFRESH_TREE));
		itemRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildTree(tree);
			}

		});

		this.tree.setMenu(browserPopupMenu);

		this.sash = new Sash(this.composite, SWT.VERTICAL | SWT.SMOOTH);

		// the separator is being dragged
		this.sash.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// if (event.detail != SWT.DRAG) {
				OcamlBrowserView.this.sash.setBounds(event.x, event.y, event.width, event.height);
				OcamlBrowserView.this.separatorMoved();
				// }
			}
		});

		// resize the OcamlBrowserView
		this.composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				OcamlBrowserView.this.resized();
			}
		});

		/*
		 * IActionBars actionBars = this.getViewSite().getActionBars(); // IMenuManager dropDownMenu =
		 * actionBars.getMenuManager(); IToolBarManager toolBarManager =
		 * actionBars.getToolBarManager();
		 * 
		 * ImageDescriptor iconAdd =
		 * ImageRepository.getImageDescriptor(ImageRepository.ICON_BROWSE); Action actionBrowse =
		 * new Action("Browse", iconAdd) { @Override public void run() { try { DirectoryDialog
		 * dirDialog = new DirectoryDialog(composite.getShell()); path = dirDialog.open(); if (path ==
		 * null) path = ""; buildTree(tree); } catch (Exception e) { OcamlPlugin.logError("ocaml
		 * plugin error", e); } } };
		 * 
		 * toolBarManager.add(actionBrowse);
		 */

	}

	/** Add a path in the browser tree */
	protected void addLocation() {
		DirectoryDialog dirDialog = new DirectoryDialog(composite.getShell());
		String path = dirDialog.open();
		if (path != null) {
			this.paths.add(path);
			savePaths();
			buildTree(tree);
		}
	}

	/** Add a path and its subdirectories (those containing mli files) in the browser tree */
	protected void addLocationRec() {
		DirectoryDialog dirDialog = new DirectoryDialog(composite.getShell());
		String path = dirDialog.open();
		if (path != null) {
			File dir = new File(path);

			ArrayList<String> paths = new ArrayList<String>();

			findPathsRec(dir, paths);

			for (String p : paths) {
				this.paths.add(p);

				savePaths();
				buildTree(tree);
			}
		}
	}

	/** Add all the sub-paths from <code>dir</code> containing mli files in <code>paths</code> */
	private void findPathsRec(File dir, ArrayList<String> paths) {
		if (dir.exists()) {
			if (dir.isDirectory()) {
				String[] mliFiles = dir.list(mliFilter);
				if (mliFiles != null && mliFiles.length > 0)
					paths.add(dir.getPath());

				String[] all = dir.list();
				if (all == null)
					return;
				for (String f : all) {
					File d = new File(dir.getAbsolutePath() + File.separatorChar + f);
					if (d.exists() && d.isDirectory())
						findPathsRec(d, paths);
				}
			}
		}

	}

	/** Remove a path in the browser tree */
	protected void removeLocation() {
		boolean removed = false;
		TreeItem[] items = this.tree.getSelection();
		
		if(items.length == 0){
			MessageDialog.openInformation(this.tree.getShell(), "Nothing selected",
			"Please select the locations you want to remove first.");
			return;
		}
		
		for (TreeItem item : items) {
			if (item.getParent() == this.tree) {
				String path = item.getText();
				File dir = new File(path);

				for (String p : this.paths) {
					File d = new File(p);
					if (dir.equals(d)) {
						this.paths.remove(p);
						removed = true;
						break;
					}
				}
			}
		}

		if (removed) {
			savePaths();
			buildTree(tree);
		} else {
			MessageDialog.openInformation(this.tree.getShell(), "Can only remove locations",
					"You can only remove locations from the tree, not random elements");
		}
	}

	protected void treeItemSelected() {
		if (tree.getSelectionCount() != 1)
			return;
		TreeItem item = tree.getSelection()[0];

		// retrieve the definition associated with the selected element
		Object data = item.getData();

		if (data instanceof Def) {
			Def def = (Def) data;
			addFormatedText(text, def);
		} else {
			text.setText("Folder: \"" + item.getText() + "\"");
		}
	}

	private void addFormatedText(StyledText text, Def def) {
		String body = def.body;
		String comment = def.comment;
		String filename = def.filename;
		// String name = def.getName();
		String parentName = def.parentName;
		String sectionComment = def.sectionComment;

		try {

			text.setRedraw(false);

			final Display display = Display.getDefault();
			final Color colorSection = new Color(display, 150, 50, 191);
			final Color colorParent = new Color(display, 191, 100, 50);
			final Color colorCode = new Color(display, 0, 0, 255);
			final Color colorLink = new Color(display, 40, 150, 70);
			// final Color colorFilename = new Color(display, 64, 64, 64);

			/*
			 * Font font = new Font(display, "Arial", 11, SWT.NONE); text.setFont(font);
			 */
			StringBuilder stringBuilder = new StringBuilder();
			ArrayList<StyleRange> styleRanges = new ArrayList<StyleRange>();

			int offset = 0;
			stringBuilder.append(body);
			styleRanges.add(new StyleRange(offset, body.length(), null, null, SWT.BOLD));
			offset += body.length();

			if (!parentName.equals("")) {
				String txt = "   (constructor of type " + parentName + ")";
				stringBuilder.append(txt);
				styleRanges.add(new StyleRange(offset, txt.length(), colorParent, null, SWT.BOLD));
				offset += txt.length();
			}

			stringBuilder.append("\n\n");
			offset += 2;

			boolean bEscape = false;
			int codeNestingLevel = 0;
			boolean bRemoveChar = false;

			boolean bInLink = false;

			int styleCodeBegin = 0;
			int styleLinkBegin = 0;

			for (int i = 0; i < comment.length(); i++) {
				char ch = comment.charAt(i);
				bRemoveChar = false;

				// lien {!Module.element}
				if (ch == '{' && !bEscape && i < comment.length() - 1
						&& comment.charAt(i + 1) == '!') {
					i++;
					bRemoveChar = true;
					bInLink = true;
					styleLinkBegin = offset;
				}

				if (ch == '}' && !bEscape && bInLink) {
					bRemoveChar = true;
					bInLink = false;
					styleRanges.add(new StyleRange(styleLinkBegin, offset - styleLinkBegin,
							colorLink, null, SWT.NONE));
				}

				if (ch == '[' && !bEscape && !bInLink) {
					codeNestingLevel++;

					if (codeNestingLevel == 1) {
						bRemoveChar = true;
						styleCodeBegin = offset;
					}
				}

				if (ch == ']' && !bEscape && codeNestingLevel > 0) {
					codeNestingLevel--;

					if (codeNestingLevel == 0) {
						bRemoveChar = true;
						styleRanges.add(new StyleRange(styleCodeBegin, offset - styleCodeBegin,
								colorCode, null, SWT.NONE));
					}
				}

				if (!bRemoveChar) {
					stringBuilder.append(ch);
					offset++;
				}

				if (ch == '\\')
					bEscape = !bEscape;
				else
					bEscape = false;

			}

			if (!comment.trim().equals("")) {
				stringBuilder.append("\n\n");
				offset += 2;
			}

			if (!sectionComment.trim().equals("")) {
				stringBuilder.append(sectionComment);
				styleRanges.add(new StyleRange(offset, sectionComment.length(), colorSection, null,
						SWT.NONE));
				offset += sectionComment.length();

				stringBuilder.append("\n\n");
				offset += 2;
			}

			stringBuilder.append(filename);
			styleRanges.add(new StyleRange(offset, filename.length(), null, null, SWT.ITALIC));
			offset += filename.length();

			text.setText(stringBuilder.toString());
			text.setStyleRanges(styleRanges.toArray(new StyleRange[0]));

			text.setRedraw(true);

			// do NOT call "dispose" on colors

		} catch (Throwable t) {
			OcamlPlugin.logError("error styling " + filename + " <<<" + body + ">>>", t);
		}
	}

	private final double defaultRatio = 0.3;

	/**
	 * The view was resized. Reposition the components in the view.
	 */
	private void resized() {

		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		int treeWidth = (int) (width * defaultRatio);

		tree.setBounds(0, 0, treeWidth, height);
		text.setBounds(treeWidth + 3, 0, width - treeWidth - 3, height);
		sash.setBounds(treeWidth, 0, treeWidth + 3, height);

	}

	/**
	 * Reposition the components after the separator between the tree and the text was dragged.
	 */
	private void separatorMoved() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		Rectangle sashBounds = sash.getBounds();

		tree.setBounds(0, 0, sashBounds.x, height);
		text.setBounds(sashBounds.x + 3, 0, width - sashBounds.x - 3, height);
		sash.setBounds(sashBounds.x, 0, sashBounds.x + 3, height);
	}

	@Override
	public void setFocus() {
		tree.setFocus();
	}
}
