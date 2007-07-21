package ocaml.views.outline;

import java.io.File;
import java.io.StringReader;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * This job is used to rebuild the outline in a low-priority thread, so as to not slow down everything else.
 * <p>
 * Note: even if the outline is not displayed (the user closed the view), its content must be computed,
 * because it is also used by the completion (TODO:refactor this).
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
	protected synchronized IStatus run(IProgressMonitor monitor) {

		//long before = System.currentTimeMillis();

		String strDocument = doc.get();

		/* "Sanitize" the document by replacing extended characters, which otherwise would crash the parser */
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

			if ("ml".equals(extension) || "mlp".equals(extension))
				root = (Def) parser.parse(scanner);
			else if ("mli".equals(extension))
				root = (Def) parser.parse(scanner, OcamlParser.AltGoals.interfaces);
			else
				OcamlPlugin.logError(extension + " file extension has no associated parser.");
		} catch (Throwable e) {
			// OcamlPlugin.logError("error while parsing", e);
			// System.out.println("unrecoverable syntax error");
			//e.printStackTrace();
		}

		// recover pieces from the AST (which couldn't be built completely because of an unrecoverable error)
		if (root == null || !parser.errorReporting.errors.isEmpty()) {
			//System.err.println("recovering AST");
			root = new Def("root", Def.Type.Root, 0, 0);

			for (Def def : parser.recoverDefs)
				if (def.bTop && def.name != null && !"".equals(def.name.trim()))
					root.children.add(def);
		}

		final IFile file = editor.getFileBeingEdited();
		/*
		 * if the source hasn't been modified since the last compilation, try to get the types inferred by the
		 * compiler (in a ".annot" file)
		 */
		if (!editor.isDirty() && OcamlPlugin.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE))
			addTypes(file, root);

		if (root != null) {
			root.buildParents();
			root.buildSiblingOffsets();
			cleanTree(root);
		}

		final OcamlOutlineControl outline = this.outline;
		final Def definitions = root;

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				/* Create error markers for syntax errors found while parsing */
				if (file != null) {
					try {
						// delete the previous error markers
						file.deleteMarkers("Ocaml.ocamlSyntaxErrorMarker", false, IResource.DEPTH_ZERO);
					} catch (Throwable e) {
						OcamlPlugin.logError("error deleting error markers", e);
					}

					// create the error markers
					if (parser.errorReporting != null) {
						for (ErrorReporting.Error error : parser.errorReporting.errors) {
							try {
								Hashtable<String, Integer> attributes = new Hashtable<String, Integer>();
								MarkerUtilities.setMessage(attributes, error.message);
								attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));

								int lineOffset = doc.getLineOffset(error.lineStart);

								MarkerUtilities.setCharStart(attributes, lineOffset + error.columnStart);
								MarkerUtilities.setCharEnd(attributes, lineOffset + error.columnEnd + 1);
								MarkerUtilities.setLineNumber(attributes, error.lineStart + 1);

								MarkerUtilities
										.createMarker(file, attributes, "Ocaml.ocamlSyntaxErrorMarker");

							} catch (Throwable e) {
								OcamlPlugin.logError("error creating error markers", e);
							}
						}
					}
				}
				
				Def outlineDefinitions = definitions.cleanCopy();

				// give the definitions tree to the editor
				editor.setDefinitionsTree(definitions);
				editor.setOutlineDefinitionsTree(outlineDefinitions);

				// to notify the hyperlink detector that the outline is available 
				synchronized(editor.outlineSignal){
					editor.outlineSignal.notifyAll();
				}

				if (outline != null) {
					outline.setInput(outlineDefinitions);
					//outline.setInput(definitions);
					editor.synchronizeOutline();
				}
				
			}
		});

		//long after = System.currentTimeMillis();
		// root.clean();
		// root.print(0);

		//System.out.println("parsed file in " + (after - before) + " ms");
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
			for(int i = 0; i < def.children.size(); i++){
				Def child = def.children.get(i);
				
				if (child.type == Def.Type.Struct || child.type == Def.Type.Sig){
					for(Def child2 : child.children)
						def.children.add(child2);
					def.children.remove(i);
				}
			}
		}

		// collapse the <object> node if it is the child of a class or classtype
		if (def.type == Def.Type.Class || def.type == Def.Type.ClassType) {
			for(int i = 0; i < def.children.size(); i++){
				Def child = def.children.get(i);
				
				if (child.type == Def.Type.Object){
					for(Def child2 : child.children)
						def.children.add(child2);
					def.children.remove(i);
				}
			}
		}

		for (Def child : def.children)
			cleanTree(child);
	}
}
