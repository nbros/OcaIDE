package ocaml.editors;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.parser.Def;
import ocaml.parsers.OcamlNewInterfaceParser;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

// TODO: refactor hyperlinks : find and number all variable references

/**
 * Creates hyper-links that allow the user to jump in the editor to the definition of the clicked
 * element
 */
public class OcamlHyperlinkDetector implements IHyperlinkDetector {

	private OcamlEditor editor;

	public OcamlHyperlinkDetector(OcamlEditor editor) {
		this.editor = editor;
	}

	/** Caching to speed up consecutive lookups */
	private long lastTime = 0;
	private int lastOffset = -1;
	private Def lastDef = null;

	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, final IRegion region,
			boolean canShowMultipleHyperlinks) {

		IProject project = editor.getProject();

		// get the definitions from the current module
		final Def modulesDefinitionsRoot = editor.getDefinitionsTree();
		/*
		 * get the definitions from all the mli files in the project paths (which should include the
		 * ocaml standard library)
		 */
		// long before = System.currentTimeMillis();
		
		// long after = System.currentTimeMillis();
		// System.err.println("parsing for hyperlinks: " + (after - before) + " ms");
		
		final Def interfacesDefinitionsRoot;
		
		if(project != null)
			interfacesDefinitionsRoot = CompletionJob.buildDefinitionsTree(project, false);
		/* If the project is null, that means the file is external (not in a project). In this case, 
		 * parse only the file to be able to show hyperlinks for this file.
		 */ 
		/* TODO: Provide hyperlinks to other modules referenced by this file */  
		else {
			interfacesDefinitionsRoot = new Def("<root>", Def.Type.Root, 0, 0);
			OcamlNewInterfaceParser parser = OcamlNewInterfaceParser.getInstance();
			
			File file = editor.getPathOfFileBeingEdited().toFile();
			Def def = parser.parseFile(file);
			if (def != null)
				interfacesDefinitionsRoot.children.add(def);
			else
				return null;
		}
		

		long time = System.currentTimeMillis();

		/* Find which definition in the tree is at the hovered offset */
		final Def searchedDef = (time - lastTime < 1000 && lastOffset == region.getOffset() && lastDef != null) ? lastDef
				: findIdentAt(modulesDefinitionsRoot, region.getOffset(), textViewer.getDocument());

		lastTime = time;

		lastOffset = region.getOffset();
		lastDef = searchedDef;

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
				Def firstPart = lookForDefinitionUp(null, parts[0], searchedDef, interfacesDefinitionsRoot, parts, true);
				// don't find it in the current module, look in the other ones 
				if (firstPart == null) {
					if (openDefInInterfaces(0, parts, interfacesDefinitionsRoot))
						return null;
				}
				else {
					// find the original definition of firstPart in current module
					while ((firstPart != null) && (firstPart.type == Def.Type.Module)) {
						// if firstPart is another module
						List<Def> childs = firstPart.children;
						if ((childs.size() == 1) && (childs.get(0).type == Def.Type.Identifier)) {
							Def child = childs.get(0);
							parts[0] = child.name;
							searchedDef.name = parts[0];
							for (int i = 1; i < parts.length; i++)
								searchedDef.name = "." + searchedDef.name; 
							firstPart = lookForDefinitionUp(null, parts[0], searchedDef, interfacesDefinitionsRoot, parts, true);
						} else 
							break;
					}
					// if the original definition of firstPart is not in current module, look in the other ones
					if (firstPart == null) {
						if (openDefInInterfaces(0, parts, interfacesDefinitionsRoot))
							return null;
						
					} 
					// look for the whole parts in current module.
					else {
						Def defFromPath = findDefFromPath(1, parts, firstPart, null);
						if (defFromPath != null)
							return defFromPath;
						else
							return firstPart;
					}
				}
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

			// if it still wasn't found, try to open it as a module
			if (def == null && searchedDef.name.length() > 0
					&& Character.isUpperCase(searchedDef.name.charAt(0))) {
				IHyperlink[] hyperlinks = makeOpenHyperlink(null, searchedDef,
						interfacesDefinitionsRoot);
				if (hyperlinks.length > 0)
					hyperlinks[0].open();
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

		if (node.type == Def.Type.In) {
			/* If this is an 'in' node (in a 'let in'), go directly to the parent */
			return lookForDefinitionUp(searchedNode, name, node.parent, interfacesDefinitionsRoot,
					fullpath, false);
		}

		// is it this node?
		test = isDef(searchedNode, name, node, true, otherBranch);
		if (test != null)
			return test;

		// if it is an "open" node, look inside the interface of this module
		if (node.type == Def.Type.Open || node.type == Def.Type.Include) {
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
			if (before.type == Def.Type.Open || before.type == Def.Type.Include) {
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
				if (project == null)
					return false;

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage();

				if (page != null) {
					
					File file = new File(filename);
					
					final IFileStore fileStore;
					try {
						URI uri = file.toURI();
						fileStore = EFS.getStore(uri);
					} catch (CoreException e) {
						OcamlPlugin.logError("OcamlHyperlinkDetector.openDefInInterfaces()", e);
						return false;
					}

					IEditorPart part = IDE.openEditorOnFileStore(page, fileStore);
					
					if (part instanceof OcamlEditor) {
						final OcamlEditor editor = (OcamlEditor) part;

						ITextViewer textViewer = editor.getTextViewer();

						if (interfaceDef != null) {
							IRegion region = interfaceDef.getRegion(textViewer.getDocument());
							editor.selectAndReveal(region.getOffset(), region.getLength());
						}
					} else
						return false;

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
				if (node.bRec)
					return node;
				else if (!searchedNode.bInIn) {
					// see if there is an 'in' on the branch from this node to its parent
					Def d = searchedNode;
					boolean inin = false;
					while (d.parent != null && d != node) {
						if (d.type == Def.Type.In) {
							inin = true;
							break;
						}
						d = d.parent;
					}
					if (!inin)
						return null;
				}
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

		if (region == null)
			return null;

		int startOffset = region.getOffset();
		int endOffset = startOffset + region.getLength() - 1;

		if (startOffset <= offset
				&& endOffset >= offset
				&& (def.type == Def.Type.Identifier || def.type == Def.Type.Open || def.type == Def.Type.Include))
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
	private IHyperlink[] makeOpenHyperlink(final ITextViewer textViewer, final Def searchedDef,
			final Def interfacesDefinitionsRoot) {

		return new IHyperlink[] {

		new IHyperlink() {

			public void open() {

				try {

					// extract the first part of a multipart name (A.B.C)
					String[] fragments = searchedDef.name.split("\\.");

					if (fragments.length < 1)
						return;
					else if (fragments.length > 1) {
						openDefInInterfaces(0, fragments, interfacesDefinitionsRoot);
						return;
					}

					String searchedName = fragments[0];

					IProject project = editor.getProject();
					if (project == null)
						return;

					OcamlPaths opaths = new OcamlPaths(project);

					String[] paths = opaths.getPaths();

					for (String path : paths) {

						File dir = new File(project.getLocation().toOSString() + File.separator
								+ path);
						if (!dir.exists())
							dir = new File(path);

						String[] mlmliFiles = dir.list(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith(".mli") || name.endsWith(".ml");
							}
						});

						mlmliFiles = Misc.filterInterfaces(mlmliFiles);

						if (mlmliFiles != null) {
							for (String mlmliFile : mlmliFiles) {

								File file = new File(dir.getAbsolutePath() + File.separatorChar
										+ mlmliFile);

								String name = file.getName();

								// remove the extension
								String moduleName = null;

								if (name.endsWith(".ml"))
									moduleName = name.substring(0, name.length() - 3);
								else if (name.endsWith(".mli"))
									moduleName = name.substring(0, name.length() - 4);
								else
									continue;

								// capitalize the first letter
								moduleName = Character.toUpperCase(moduleName.charAt(0))
										+ moduleName.substring(1);

								if (moduleName.equals(searchedName)) {
									IWorkbenchPage page = PlatformUI.getWorkbench()
											.getActiveWorkbenchWindow().getActivePage();

									if (page == null)
										return;

									final IFileStore fileStore;
									try {
										URI uri = file.toURI();
										fileStore = EFS.getStore(uri);
									} catch (CoreException e) {
										OcamlPlugin.logError(
												"OcamlHyperlinkDetector.makeOpenHyperlink()", e);
										return;
									}

									IDE.openEditorOnFileStore(page, fileStore);

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
