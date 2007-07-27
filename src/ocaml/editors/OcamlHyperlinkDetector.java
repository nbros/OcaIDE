package ocaml.editors;

import java.io.File;
import java.io.FilenameFilter;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.parser.Def;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Creates hyper-links that allow the user to jump in the editor to the definition of the clicked
 * element
 */
public class OcamlHyperlinkDetector implements IHyperlinkDetector {

	private OcamlEditor editor;

	public OcamlHyperlinkDetector(OcamlEditor editor) {
		this.editor = editor;
	}

	// private final String chars =
	// "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'";

	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
			boolean canShowMultipleHyperlinks) {

		// get the definitions from the current module
		final Def modulesDefinitionsRoot = editor.getDefinitionsTree();
		/*
		 * get the definitions from all the mli files in the project paths (which should include the
		 * ocaml standard library)
		 */
		final Def interfacesDefinitionsRoot = CompletionJob.buildDefinitionsTree(editor
				.getProject(), false);

		/* Find which definition in the tree is at the hovered offset */
		final Def searchedDef = findIdentAt(modulesDefinitionsRoot, region.getOffset(), textViewer
				.getDocument());

		if (searchedDef != null) {

			// don't hyperlink the underscore
			if (searchedDef.name.equals("_"))
				return null;

			if (searchedDef.type == Def.Type.Open || searchedDef.type == Def.Type.Include) {
				return makeOpenHyperlink(textViewer, searchedDef, interfacesDefinitionsRoot);
			}

			return new IHyperlink[] {

			new IHyperlink() {

				public void open() {

					Def target = findDefinitionOf(searchedDef, modulesDefinitionsRoot,
							interfacesDefinitionsRoot);

					if (target == null)
						return;

					IRegion region = target.getRegion(textViewer.getDocument());
					editor.selectAndReveal(region.getOffset(), region.getLength());
				}

				public String getTypeLabel() {
					return null;
				}

				public String getHyperlinkText() {
					return searchedDef.name;
				}

				public IRegion getHyperlinkRegion() {
					return searchedDef.getRegion(textViewer.getDocument());
				}

			}

			};

		}

		return null;
	}

	/**
	 * Find the definition of <code>searchedDef</code> in <code>modulesDefinitionsRoot</code>,
	 * and in <code>interfacesDefinitionsRoot</code>
	 */
	private Def findDefinitionOf(Def searchedDef, Def modulesDefinitionsRoot,
			Def interfacesDefinitionsRoot) {

		Def def = null;

		/*
		 * if this is a compound name "A.B.c", then we extract the first component to know what
		 * module to look for, and then we get the definition by entering the module
		 */
		if (searchedDef.name.indexOf('.') != -1) {
			String[] parts = searchedDef.name.split("\\.");
			if (parts.length > 1) {
				Def firstPart = lookForDefinitionUp(null, parts[0], searchedDef,
						interfacesDefinitionsRoot, parts, true);
				if (firstPart != null) {
					Def defFromPath = findDefFromPath(1, parts, firstPart, null);
					if (defFromPath != null)
						return defFromPath;
					else
						return firstPart;
				}

				// if we didn't find it in the current module, look in the other ones
				if (openDefInInterfaces(0, parts, interfacesDefinitionsRoot))
					return null;
			}

		} else {
			def = lookForDefinitionUp(searchedDef, searchedDef.name, searchedDef,
					interfacesDefinitionsRoot, new String[] { searchedDef.name }, true);

			// if we didn't find it, look in Pervasives (which is always opened by default)
			if (def == null) {
				String[] pervasivesPath = new String[] { "Pervasives", searchedDef.name };
				if (openDefInInterfaces(0, pervasivesPath, interfacesDefinitionsRoot))
					return null;
			}
		}

		return def;
	}

	/** Find the definition whose complete path is given, starting at <code>index</code> */
	private Def findDefFromPath(int index, String[] path, Def def, Def lastPartFound) {
		if (index == path.length)
			if (def.type != Def.Type.Root)
				return def;
			else
				return null;

		for (Def child : def.children) {
			/* skip the "false" nodes */
			if (child.type == Def.Type.Sig || child.type == Def.Type.Struct) {
				Def test = findDefFromPath(index, path, child, null);
				if (test != null)
					return test;
			}

			if (child.name.equals(path[index]))
				return findDefFromPath(index + 1, path, child, child);
		}

		return lastPartFound;
	}

	/**
	 * Look for a definition in the <code>node</code> node, its previous siblings, its associated
	 * nodes ("and"), and recurse on its parent
	 * 
	 * @param searchedNode
	 *            the node we are looking for
	 * @param name
	 *            the name of the definition to be found
	 * @param node
	 *            the node from which to start
	 * @param interfacesDefinitionsRoot
	 *            the root of the interfaces definitions tree
	 * @param fullpath
	 *            the full path of the searched definition (used to look for it in other modules)
	 * @param otherBranch
	 *            are we in another branch relative to the definition from which we started? (this
	 *            is used to manage the non-rec definitions)
	 */
	private Def lookForDefinitionUp(Def searchedNode, String name, Def node,
			Def interfacesDefinitionsRoot, String[] fullpath, boolean otherBranch) {
		Def test = null;
		
		if(node.type == Def.Type.In){
			/* If this is an 'in' node (in a 'let in'), go directly to the parent */
			return lookForDefinitionUp(searchedNode, name, node.parent, interfacesDefinitionsRoot,
					fullpath, false);
		}

		// is it this node?
		test = isDef(searchedNode, name, node, true, otherBranch);
		if (test != null)
			return test;

		// if it is an "open" node, look inside the interface of this module
		if (node.type == Def.Type.Open) {
			String[] path = new String[fullpath.length + 1];
			System.arraycopy(fullpath, 0, path, 1, fullpath.length);
			path[0] = node.name;

			if (openDefInInterfaces(0, path, interfacesDefinitionsRoot))
				return null;
		}

		/* if we are at root, we cannot go further up in this module. */
		if (node.type == Def.Type.Root) {
			// openDefInInterfaces(0, new String[] { name }, interfacesDefinitionsRoot);
			return null;
		}

		// look in the associated "and" nodes after it
		for (int i = node.getSiblingsOffset() + 1; i < node.parent.children.size(); i++) {
			Def after = node.parent.children.get(i);
			if (!after.bAnd)
				break;

			test = isDef(searchedNode, name, after, true, false);
			if (test != null)
				return test;
		}

		/* look in the associated "and" nodes that precede it and also in the other (not "and") */
		boolean bAnd = node.bAnd;
		boolean bStopAndAtNext = false;

		for (int i = node.getSiblingsOffset() - 1; i >= 0; i--) {
			Def before = node.parent.children.get(i);

			if (bStopAndAtNext)
				bAnd = false;
			if (!before.bAnd)
				bStopAndAtNext = true;

			// if it is an "open" node, look inside the interface of this module
			if (before.type == Def.Type.Open) {
				String[] path = new String[fullpath.length + 1];
				System.arraycopy(fullpath, 0, path, 1, fullpath.length);
				path[0] = before.name;

				if (openDefInInterfaces(0, path, interfacesDefinitionsRoot))
					return null;
			}

			test = isDef(searchedNode, name, before, bAnd, !bAnd);
			if (test != null)
				return test;
		}

		/* Now, go one step up */
		return lookForDefinitionUp(searchedNode, name, node.parent, interfacesDefinitionsRoot,
				fullpath, false);
	}

	/**
	 * look for a definition (defined by its "path": A.B.c) in the interfaces found in the project
	 * paths, and open and highlight it in an editor if it was found
	 */
	private boolean openDefInInterfaces(int index, final String[] path, Def interfaceDef) {

		if (index == path.length) {
			try {
				String filename = interfaceDef.filename;

				// open the file containing the definition
				IProject project = editor.getProject();

				IPath location = new Path(filename);

				IFolder folder = project.getFolder(".HyperlinksLinkedFiles");
				if (!folder.exists())
					folder.create(true, true, null);

				IFile file = folder.getFile(location.lastSegment());
				// if (file.exists())
				// file.delete(true, null);
				file.createLink(location, IResource.REPLACE, null);

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage();
				if (page != null) {
					IEditorPart part = page.openEditor(new FileEditorInput(file),
							OcamlEditor.ML_EDITOR_ID, true);

					if (part instanceof OcamlEditor) {
						final OcamlEditor editor = (OcamlEditor) part;
						
						
						ITextViewer textViewer = editor.getTextViewer();

						if (interfaceDef != null) {
							IRegion region = interfaceDef.getRegion(textViewer.getDocument());
							editor.selectAndReveal(region.getOffset(), region.getLength());
						}
					}
					return true;
				}
			} catch (Throwable e) {
				OcamlPlugin.logError("error trying to open file from hyperlink", e);
			}

			return false;
		}

		for (Def child : interfaceDef.children) {
			if (child.name.equals(path[index]))
				if (openDefInInterfaces(index + 1, path, child))
					return true;
		}

		/*
		 * if we couldn't go all the way down to the definition, but we could find the beginning of
		 * the path, open it
		 */
		if (index > 1)
			if (openDefInInterfaces(path.length, path, interfaceDef))
				return true;

		return false;
	}

	/**
	 * Is <code>node</code> the definition of <code>name</code>? If true, returns the node (or
	 * the constructor in a type). If false, returns null.
	 * 
	 * @param bIn
	 *            whether to accept "let in" (or parameter) nodes
	 * @param otherBranch
	 *            are we in another branch relative to the definition from which we started? (this
	 *            is used to manage the non-rec definitions)
	 */
	private Def isDef(Def searchedNode, String name, Def node, boolean bIn, boolean otherBranch) {

		// System.out.println(node.name);

		if (node.name.equals(name)) {
			switch (node.type) {
			case Let:
				if (node.bRec)
					return node;
				else {
					if (otherBranch)
						return node;
					else
						return null;
				}

			case TypeConstructor:
				return node;
			case RecordTypeConstructor:
				return node;
			case LetIn:
				if (!bIn)
					return null;
				/*
				 * If the node is a direct parent of the searched node, then it means it is not in
				 * the 'in' child
				 */
				if(!node.bRec && node.children.contains(searchedNode))
					return null;
				return node;

			case Parameter:
				return bIn ? node : null;
			case Type:
				return node;
			case Module:
				return node;
			case Exception:
				return node;
			case ModuleType:
				return node;
			case External:
				return node;
			case Class:
				return node;
			default:
				return null;
			}

		}

		/* if it's a type, look inside it for its constructors */
		if (node.type == Def.Type.Type) {
			// look at the type constructors
			for (Def child : node.children) {
				Def test = isDef(searchedNode, name, child, bIn, false);
				if (test != null)
					return test;
			}
		}

		/* if it is an open directive, look inside the corresponding interface */
		/*
		 * else if(node.type == Def.Type.Open){ openDefInInterfaces(0, new String[] { name },
		 * interfacesDefinitionsRoot); }
		 */

		return null;
	}

	/** Find an identifier (or an open directive) at a position in the document */
	private Def findIdentAt(Def def, int offset, IDocument doc) {

		if (def == null || doc == null)
			return null;

		IRegion region = def.getRegion(doc);

		int startOffset = region.getOffset();
		int endOffset = startOffset + region.getLength() - 1;

		if (startOffset <= offset && endOffset >= offset
				&& (def.type == Def.Type.Identifier || def.type == Def.Type.Open))
			return def;

		for (Def d : def.children) {
			Def cd = findIdentAt(d, offset, doc);
			if (cd != null)
				return cd;
		}

		return null;
	}

	/**
	 * make an hyperlink for an open directive (to open the interface in an editor)
	 */
	private IHyperlink[] makeOpenHyperlink(final ITextViewer textViewer, final Def searchedDef, final Def interfacesDefinitionsRoot) {

		return new IHyperlink[] {

		new IHyperlink() {

			public void open() {

				try {
					
					// extract the first part of a multipart name (A.B.C)
					String[] fragments = searchedDef.name.split("\\.");
					
					if(fragments.length < 1)
						return;
					else if(fragments.length > 1){
						openDefInInterfaces(0, fragments, interfacesDefinitionsRoot);
						return;
					}
					
					
					String searchedName = fragments[0];

					IProject project = editor.getProject();

					OcamlPaths opaths = new OcamlPaths(project);

					String[] paths = opaths.getPaths();

					for (String path : paths) {

						File dir = new File(project.getLocation().toOSString() + File.separator
								+ path);
						if (!dir.exists())
							dir = new File(path);

						File[] mliFiles = dir.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith(".mli");
							}
						});

						if (mliFiles != null) {
							for (File mliFile : mliFiles) {
								String name = mliFile.getName();
								// remove the extension
								String moduleName = name.substring(0, name.length() - 4);
								// capitalize the first letter
								moduleName = Character.toUpperCase(moduleName.charAt(0))
										+ moduleName.substring(1);

								if (moduleName.equals(searchedName)) {
									// create a link to the file in the workspace

									IPath location = new Path(mliFile.getAbsolutePath());

									IFolder folder = project.getFolder(".HyperlinksLinkedFiles");
									if (!folder.exists())
										folder.create(true, true, null);

									IFile ifile = folder.getFile(location.lastSegment());
									ifile.createLink(location, IResource.REPLACE, null);

									// open an editor on the file
									IWorkbenchPage page = PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow().getActivePage();
									if (page != null) {
										/* IEditorPart part = */page.openEditor(
												new FileEditorInput(ifile),
												OcamlEditor.ML_EDITOR_ID, true);
									}

									return;
								}
							}
						}
					}

				} catch (Throwable e) {
					OcamlPlugin.logError("error opening hyperlink for 'open' directive", e);
				}

			}

			public String getTypeLabel() {
				return null;
			}

			public String getHyperlinkText() {
				return searchedDef.name;
			}

			public IRegion getHyperlinkRegion() {
				return searchedDef.getRegion(textViewer.getDocument());
			}

		}

		};

	}

}
