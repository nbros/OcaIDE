package ocaml.views;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.parsers.OcamlDefinition;
import ocaml.parsers.OcamlInterfaceParser;
import ocaml.parsers.OcamlDefinition.Type;
import ocaml.util.ImageRepository;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

/**
 * Implements the O'Caml browser view, which allows the user to browse the O'Caml library, and see all the
 * definitions found in the library mli files
 */
public class OcamlBrowserView extends ViewPart {

	public final static String ID = "ocaml.ocamlBrowserView";

	/** The movable separator between the left and right panes */
	private Sash sash;

	/** The description of the selected element */
	private StyledText text;

	/** a container for the other components */
	private Composite composite;

	/** The SWT tree of found definitions */
	private Tree tree;

	public OcamlBrowserView() {
	}

	private static FilenameFilter mliFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".mli");
		}
	};

	private void buildTree(Tree tree) {
		File dir = new File(OcamlPlugin.getLibFullPath());
		if (!dir.exists() || !dir.isDirectory()) {
			OcamlPlugin.logError("Parsing of mli files aborted : directory not found");
			return;
		}

		String[] mliFiles = dir.list(mliFilter);
		for (int i = 0; i < mliFiles.length; i++)
			mliFiles[i] = dir.getAbsolutePath() + File.separatorChar + mliFiles[i];

		OcamlInterfaceParser parser = OcamlInterfaceParser.getInstance();

		for (String mliFile : mliFiles) {
			File file = new File(mliFile);
			OcamlDefinition definition = parser.parseFile(file);

			TreeItem item = new TreeItem(tree, SWT.NONE);

			// remove the ".mli" extension
			String name = file.getName();
			name = name.substring(0, name.length() - 4);
			// change the first character to uppercase
			name = Character.toUpperCase(name.charAt(0)) + name.substring(1);

			item.setText(name);
			item.setData(definition);
			item.setImage(ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE));

			buildBranch(item, definition);

			// item.setExpanded(true);
		}

	}

	private void buildBranch(TreeItem item, OcamlDefinition definition) {
		for (OcamlDefinition childDefinition : definition.getChildren()) {
			TreeItem childItem = new TreeItem(item, SWT.NONE);
			childItem.setText(childDefinition.getName());
			childItem.setData(childDefinition);
			childItem.setImage(findImage(childDefinition));

			buildBranch(childItem, childDefinition);

			// childItem.setExpanded(true);
		}
	}

	private Image findImage(OcamlDefinition definition) {
		Type type = definition.getType();

		if (type.equals(Type.DefVal))
			return ImageRepository.getImage(ImageRepository.ICON_VALUE);
		if (type.equals(Type.DefType))
			return ImageRepository.getImage(ImageRepository.ICON_TYPE);
		if (type.equals(Type.DefClass))
			return ImageRepository.getImage(ImageRepository.ICON_CLASS);
		if (type.equals(Type.DefException))
			return ImageRepository.getImage(ImageRepository.ICON_EXCEPTION);
		if (type.equals(Type.DefExternal))
			return ImageRepository.getImage(ImageRepository.ICON_EXTERNAL);
		if (type.equals(Type.DefModule))
			return ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE);
		if (type.equals(Type.DefSig))
			return ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE_TYPE);
		if (type.equals(Type.DefConstructor))
			return ImageRepository.getImage(ImageRepository.ICON_C);

		return null;
	}

	/**
	 * Create the components in the view
	 * @see ViewPart#createPartControl
	 */
	@Override
	public void createPartControl(Composite parent) {
		this.composite = new Composite(parent, SWT.BORDER);

		this.tree = new Tree(composite, SWT.BORDER | SWT.SINGLE);
		this.tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				treeItemSelected();
			}
		});

		buildTree(tree);

		text = new StyledText(this.composite, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL);
		text.setEditable(false);

		this.text.setText("Ocaml Browser");

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

	}

	protected void treeItemSelected() {
		if (tree.getSelectionCount() != 1)
			return;
		TreeItem item = tree.getSelection()[0];

		// retrieve the definition associated with the selected element
		Object data = item.getData();

		if (data instanceof OcamlDefinition) {
			OcamlDefinition def = (OcamlDefinition) data;
			addFormatedText(text, def);
		}
	}

	private void addFormatedText(StyledText text, OcamlDefinition def) {
		String body = def.getBody();
		String comment = def.getComment();
		String filename = def.getFilename();
		// String name = def.getName();
		String parentName = def.getParentName();
		String sectionComment = def.getSectionComment();

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
				if (ch == '{' && !bEscape && i < comment.length() - 1 && comment.charAt(i + 1) == '!') {
					i++;
					bRemoveChar = true;
					bInLink = true;
					styleLinkBegin = offset;
				}

				if (ch == '}' && !bEscape && bInLink) {
					bRemoveChar = true;
					bInLink = false;
					styleRanges.add(new StyleRange(styleLinkBegin, offset - styleLinkBegin, colorLink, null,
							SWT.NONE));
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
						styleRanges.add(new StyleRange(styleCodeBegin, offset - styleCodeBegin, colorCode,
								null, SWT.NONE));
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
				styleRanges
						.add(new StyleRange(offset, sectionComment.length(), colorSection, null, SWT.NONE));
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