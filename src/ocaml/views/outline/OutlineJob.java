package ocaml.views.outline;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;
import ocaml.parser.ErrorReporting;
import ocaml.parser.OcamlParser;
import ocaml.parser.OcamlScanner;
import ocaml.preferences.PreferenceConstants;
import ocaml.typeHovers.OcamlAnnotParser;
import ocaml.typeHovers.TypeAnnotation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * This job is used to rebuild the outline in a low-priority thread, so as to not slow down
 * everything else.
 * <p>
 * Note: even if the outline is not displayed (the user closed the view), its content must be
 * computed, because it is also used by the completion and hyperlinks (TODO:refactor this).
 */
public class OutlineJob extends Job {

	public OutlineJob(String name) {
		super(name);
	}

	/** The outline. Can be <code>null</code> if the outline view is closed */
	private OcamlOutlineControl outline;

	private IDocument doc;

	private OcamlEditor editor;

	public void setDoc(IDocument doc) {
		this.doc = doc;
	}

	public void setOutline(OcamlOutlineControl outline) {
		this.outline = outline;
	}

	public void setEditor(OcamlEditor editor) {
		this.editor = editor;
	}

	/**
	 * This method is "synchronized" to ascertain that this Job will never be running more than one instance
	 * at any moment.
	 */
	@Override
	public synchronized IStatus run(IProgressMonitor monitor) {
		
		//System.err.println("outline job" + nJob++);

		 //long before = System.currentTimeMillis();

		String strDocument = doc.get();

		/*
		 * "Sanitize" the document by replacing extended characters, which otherwise would crash the
		 * parser
		 */
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < strDocument.length(); i++) {
			char c = strDocument.charAt(i);

			// replace it by an underscore
			if (c > 127)
				c = '_';
			str.append(c);
		}

		final StringReader in = new StringReader(str.toString());
		final OcamlScanner scanner = new OcamlScanner(in);
		final OcamlParser parser = new OcamlParser();

		Def root = null;
		try {
			String extension = editor.getFileBeingEdited().getFullPath().getFileExtension();

			if ("ml".equals(extension))
				root = (Def) parser.parse(scanner);
			else if ("mli".equals(extension))
				root = (Def) parser.parse(scanner, OcamlParser.AltGoals.interfaces);
			else if(!("ml4".equals(extension) || "mlp".equals(extension)))
				OcamlPlugin.logError(extension + " file extension has no associated parser.");
		} catch (Throwable e) {
			// OcamlPlugin.logError("error while parsing", e);
			// System.out.println("unrecoverable syntax error");
			// e.printStackTrace();
		}
		
		//for(long i = 0; i < 1000000000l; i++);

		// recover pieces from the AST (which couldn't be built completely because of an
		// unrecoverable error)
		if (root == null || !parser.errorReporting.errors.isEmpty()) {
			// System.err.println("recovering AST");
			root = new Def("root", Def.Type.Root, 0, 0);

			for (Def def : parser.recoverDefs)
				if (def.bTop && def.name != null && !"".equals(def.name.trim()))
					root.children.add(def);
		}

		if (root != null) {
			root.buildParents();
			root.buildSiblingOffsets();
			cleanTree(root);
		}

		final OcamlOutlineControl outline = this.outline;
		final Def definitions = root;

		Def outlineDefinitions = definitions.cleanCopy();
		// remove the definitions the user has chosen not to display
		initPreferences();
		cleanOutline(outlineDefinitions);
		

		final IFile file = editor.getFileBeingEdited();
		/*
		 * if the source hasn't been modified since the last compilation, try to get the types
		 * inferred by the compiler (in a ".annot" file)
		 */
		if (!editor.isDirty()
				&& OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE))
			addTypes(file, outlineDefinitions);		
		
		
		if(OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_OUTLINE_UNNEST_IN))
			outlineDefinitions.unnestIn(null, 0);
		
		final Def fOutlineDefinitions = outlineDefinitions;


		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				/* Create error markers for syntax errors found while parsing */
				if (file != null) {
					try {
						// delete the previous error markers
						file.deleteMarkers("Ocaml.ocamlSyntaxErrorMarker", false,
								IResource.DEPTH_ZERO);
					} catch (Throwable e) {
						OcamlPlugin.logError("error deleting error markers", e);
					}

					// create the error markers
					if (parser.errorReporting != null) {
						for (ErrorReporting.Error error : parser.errorReporting.errors) {
							try {
								Hashtable<String, Integer> attributes = new Hashtable<String, Integer>();
								MarkerUtilities.setMessage(attributes, error.message);
								attributes.put(IMarker.SEVERITY,
										new Integer(IMarker.SEVERITY_ERROR));

								int lineOffset = doc.getLineOffset(error.lineStart);

								int offsetStart = lineOffset + error.columnStart;
								int offsetEnd = lineOffset + error.columnEnd + 1;
								int lineNumber = error.lineStart + 1;

								if ("unexpected token \"end-of-file\"".equals(error.message)) {
									// find the last non-blank character
									int offset = 0;
									for (offset = doc.getLength() - 1; offset >= 0; offset--)
										if (!Character.isWhitespace(doc.getChar(offset)))
											break;

									lineNumber = doc.getLineOfOffset(offset);
									offsetStart = offset;
									offsetEnd = offset + 1;
									if (offsetEnd > doc.getLength())
										offsetEnd = doc.getLength();
									MarkerUtilities
											.setMessage(attributes, "unexpected end of file");
								}

								MarkerUtilities.setCharStart(attributes, offsetStart);
								MarkerUtilities.setCharEnd(attributes, offsetEnd);
								MarkerUtilities.setLineNumber(attributes, lineNumber);

								MarkerUtilities.createMarker(file, attributes,
										"Ocaml.ocamlSyntaxErrorMarker");

							} catch (Throwable e) {
								OcamlPlugin.logError("error creating error markers", e);
							}
						}
					}
				}

				// give the definitions tree to the editor
				editor.setDefinitionsTree(definitions);
				editor.setOutlineDefinitionsTree(fOutlineDefinitions);

				// to notify the hyperlink detector that the outline is available
				synchronized (editor.outlineSignal) {
					editor.outlineSignal.notifyAll();
				}

				if (outline != null) {
					if(OcamlOutlineControl.bOutlineDebugButton && OcamlPlugin.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.P_OUTLINE_DEBUG_MODE))
						outline.setInput(definitions);
					else
						outline.setInput(fOutlineDefinitions);
					
					editor.synchronizeOutline();
				}

			}
		});

		 //long after = System.currentTimeMillis();
		// root.clean();
		// root.print(0);

		 //System.out.println("built outline in " + (after - before) + " ms");
		// if(parser.errorReporting.bErrors)
		// System.out.println("Syntax errors reported");

		return Status.OK_STATUS;
	}

	/** Add O'Caml types to the definitions if a ".annot" file is present and up-to-date */
	private void addTypes(IFile file, Def root) {
		if (file == null || root == null)
			return;

		IPath filePath = file.getLocation();

		String fileName = filePath.lastSegment();
		if (fileName.endsWith(".ml")) {
			String annotFilename = fileName.substring(0, fileName.length() - 3) + ".annot";

			File annotFile = filePath.removeLastSegments(1).append(annotFilename).toFile();

			if (annotFile.exists()) {
				boolean bUpToDate = filePath.toFile().lastModified() <= annotFile.lastModified();

				if (bUpToDate) {
					TypeAnnotation[] annotations;
					try {
						annotations = OcamlAnnotParser.parseFile(annotFile, doc);
						Arrays.sort(annotations, new AnnotationsComparator());
					} catch (BadLocationException e) {
						OcamlPlugin.logError("parsing annot file for adding types in outline", e);
						return;
					}
					if (annotations != null)
						addTypeRec(annotations, root, true);

				}
			}
		}
	}

	private void addTypeRec(TypeAnnotation[] annotations, Def def, boolean root) {
		if (!root) {
			int lineOffset;
			try {
				lineOffset = doc.getLineOffset(Def.getLine(def.posStart));
			} catch (BadLocationException e) {
				OcamlPlugin.logError("adding types in outline", e);
				return;
			}

			int startOffset = lineOffset + Def.getColumn(def.posStart);
			int endOffset = lineOffset + Def.getColumn(def.posEnd);

			TypeAnnotation key = new TypeAnnotation(startOffset, endOffset + 1, "");
			int index = Arrays.binarySearch(annotations, key, new AnnotationsComparator());

			if (index >= 0) {
				TypeAnnotation annot = annotations[index];
				String type = annot.getType().replaceAll("\r?\n", " ");
				def.ocamlType = type;
			}
		}

		for (Def child : def.children)
			addTypeRec(annotations, child, false);
	}

	/** Comparator to sort TypeAnnotations */
	class AnnotationsComparator implements Comparator<TypeAnnotation> {
		public int compare(TypeAnnotation t1, TypeAnnotation t2) {
			int a = t1.getBegin() - t2.getBegin();
			if (a != 0)
				return a;
			else
				return t1.getEnd() - t2.getEnd();
		}

	}

	private void cleanTree(Def def) {
		if (def == null)
			return;

		// collapse the <structure> or <signature> node if it is the child of a module or moduletype
		if (def.type == Def.Type.Module || def.type == Def.Type.ModuleType) {
			for (int i = 0; i < def.children.size(); i++) {
				Def child = def.children.get(i);

				if (child.type == Def.Type.Struct || child.type == Def.Type.Sig) {
					for (Def child2 : child.children)
						def.children.add(child2);
					def.children.remove(i);
				}
			}
		}

		// collapse the <object> node if it is the child of a class or classtype
		if (def.type == Def.Type.Class || def.type == Def.Type.ClassType) {
			for (int i = 0; i < def.children.size(); i++) {
				Def child = def.children.get(i);

				if (child.type == Def.Type.Object) {
					for (Def child2 : child.children)
						def.children.add(child2);
					def.children.remove(i);
				}
			}
		}

		for (Def child : def.children)
			cleanTree(child);
	}

	private boolean showLet;
	private boolean showLetIn;
	private boolean showType;
	private boolean showModule;
	private boolean showModuleType;
	private boolean showException;
	private boolean showExternal;
	private boolean showClass;
	private boolean showOpen;
	private boolean showMethod;
	private boolean showInclude;
	private boolean showVal;
	private boolean showInitializer;
	private boolean showClassType;
	private boolean showVariantCons;
	private boolean showRecordCons;
	private int letMinChars;
	private int letInMinChars;

	private void initPreferences() {
		final IPreferenceStore ps = OcamlPlugin.getInstance().getPreferenceStore();
		showLet = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_LET);
		showLetIn = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_LET_IN);
		showType = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_TYPE);
		showModule = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_MODULE);
		showModuleType = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_MODULE_TYPE);
		showException = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_EXCEPTION);
		showExternal = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_EXTERNAL);
		showClass = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_CLASS);
		showOpen = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_OPEN);
		showMethod = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_METHOD);
		showInclude = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_INCLUDE);
		showVal = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_VAL);
		showInitializer = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_INITIALIZER);
		showClassType = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_CLASSTYPE);
		showVariantCons = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_VARIANT_CONS);
		showRecordCons = ps.getBoolean(PreferenceConstants.P_OUTLINE_SHOW_RECORD_CONS);
		
		letMinChars = ps.getInt(PreferenceConstants.P_OUTLINE_LET_MINIMUM_CHARS);
		letInMinChars = ps.getInt(PreferenceConstants.P_OUTLINE_LET_IN_MINIMUM_CHARS);

	}

	private boolean showDef(Def def) {
		
		switch (def.type) {
		case Let:
			return showLet && def.name.length() >= letMinChars;
		case LetIn:
			return showLetIn && def.name.length() >= letInMinChars;
		case Type:
			return showType;
		case Module:
			return showModule;
		case ModuleType:
			return showModuleType;
		case Exception:
			return showException;
		case External:
			return showExternal;
		case Class:
			return showClass;
		case Open:
			return showOpen;
		case Method:
			return showMethod;
		case Include:
			return showInclude;
		case Val:
			return showVal;
		case Initializer:
			return showInitializer;
		case ClassType:
			return showClassType;
		case TypeConstructor:
			return showVariantCons;
		case RecordTypeConstructor:
			return showRecordCons;
		default:
			return true;
		}
	}

	/** Remove the definitions the user doesn't want to see (outline preference page) */

	private void cleanOutline(Def def) {
		if (def == null)
			return;

		ArrayList<Def> newChildren = new ArrayList<Def>();

		for (Def child : def.children) {
			if (showDef(child))
				newChildren.add(child);

			cleanOutline(child);
		}

		def.children = newChildren;
	}

}
