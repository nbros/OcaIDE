package ocaml.views.outline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;
import ocaml.parser.ErrorReporting;
import ocaml.parser.OcamlParser;
import ocaml.parser.OcamlScanner;
import ocaml.parsers.Camlp4Preprocessor;
import ocaml.preferences.PreferenceConstants;
import ocaml.typeHovers.OcamlAnnotParser;
import ocaml.typeHovers.TypeAnnotation;
import ocaml.util.Misc;

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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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

	public OutlineJob(String name, boolean syncWithEditor) {
		super(name);
		this.syncWithEditor = syncWithEditor;
	}

	/** The outline. Can be <code>null</code> if the outline view is closed */
	private OcamlOutlineControl outline;

	private IDocument doc;

	private OcamlEditor editor;
	
	private boolean syncWithEditor;

	public void setDoc(IDocument doc) {
		this.doc = doc;
	}

	public void setOutline(OcamlOutlineControl outline) {
		this.outline = outline;
	}

	public void setEditor(OcamlEditor editor) {
		this.editor = editor;
	}

	private File tempFileMl = null;
	private File tempFileMli = null;

	/**
	 * This method is "synchronized" to ascertain that this Job will never be running more than one
	 * instance at any moment.
	 */
	// int nJob = 0;
	@Override
	public synchronized IStatus run(IProgressMonitor monitor) {
		// System.err.println("outline job" + nJob++);

		// long before = System.currentTimeMillis();

		// the file in the workspace. null if an external file
		IFile file = editor.getFileBeingEdited();

		IPath filePath = editor.getPathOfFileBeingEdited();

		if (filePath == null)
			return Status.CANCEL_STATUS;

		String strDocument = doc.get();

		Camlp4Preprocessor preprocessor = new Camlp4Preprocessor(strDocument);

		if (preprocessor.mustPreprocess()) {
			// if (true) {
			// System.err.println("preprocessing");

			File tempFile = null;

			/* Create a temporary file so that we don't have to save the editor */
			try {
				String ext = filePath.getFileExtension();
				FileWriter writer;

				if ("mli".equals(ext)) {
					if (tempFileMli == null) {
						tempFileMli = File.createTempFile("mlp", ".mli");
						tempFileMli.deleteOnExit();
					}
					writer = new FileWriter(tempFileMli);
					tempFile = tempFileMli;

				}

				else {
					if (tempFileMl == null) {
						tempFileMl = File.createTempFile("mlp", ".ml");
						tempFileMl.deleteOnExit();
					}
					writer = new FileWriter(tempFileMl);
					tempFile = tempFileMl;
				}

				writer.append(strDocument);
				writer.flush();
				writer.close();
			} catch (IOException e) {
				OcamlPlugin
						.logError("couldn't create temporary file for formatting with camlp4", e);
			}

			if (tempFile == null) {
				OcamlPlugin.logError("Error creating temporary file for camlp4 preprocessing.");
				return Status.CANCEL_STATUS;
			}

			// preprocess the file with camlp4
			preprocessor.preprocess(tempFile, monitor);

			if (file != null) {
				try {
					// delete the previous error markers
					file.deleteMarkers("Ocaml.ocamlSyntaxErrorMarker", false, IResource.DEPTH_ZERO);
				} catch (Throwable e) {
					OcamlPlugin.logError("error deleting error markers", e);
				}
			}

			// camlp4 errors?
			Pattern patternErrors = Pattern
					.compile("File \".*?\", line (\\d+), characters (\\d+)-(\\d+):");

			String errorOutput = preprocessor.getErrorOutput();

			if (file != null) {
				Matcher matcher = patternErrors.matcher(errorOutput);
				if (!"".equals(errorOutput.trim())) {
					try {
						int line = 0;
						int colStart = 0;
						int colEnd = 1;
						String message;

						if (matcher.find()) {
							line = Integer.parseInt(matcher.group(1)) - 1;
							colStart = Integer.parseInt(matcher.group(2)) - 1;
							colEnd = Integer.parseInt(matcher.group(3)) - 1;
							message = errorOutput.substring(matcher.end()).trim();
						} else
							message = errorOutput;

						Hashtable<String, Integer> attributes = new Hashtable<String, Integer>();

						MarkerUtilities.setMessage(attributes, message);
						attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));

						int lineOffset = doc.getLineOffset(line);

						int offsetStart = lineOffset + colStart;
						int offsetEnd = lineOffset + colEnd + 1;

						MarkerUtilities.setCharStart(attributes, offsetStart);
						MarkerUtilities.setCharEnd(attributes, offsetEnd);
						MarkerUtilities.setLineNumber(attributes, line);

						MarkerUtilities.createMarker(file, attributes,
								"Ocaml.ocamlSyntaxErrorMarker");

					} catch (Throwable e) {
						OcamlPlugin.logError("error creating error markers", e);
					}
					return Status.OK_STATUS;
				}
			}

			// System.out.println(output);
			strDocument = preprocessor.getOutput();

			// System.err.println(output);
		}

		/*
		 * "Sanitize" the document by replacing extended characters, which otherwise would crash the
		 * parser
		 */
		// final StringBuilder str = new StringBuilder();
		// System.err.println("sanitizing");
		char[] sanitizedDocument = null;
		try {
			sanitizedDocument = new char[strDocument.length()];

			for (int i = 0; i < strDocument.length(); i++) {
				char c = strDocument.charAt(i);

				// replace it by an underscore
				if (c > 127)
					c = '_';
				sanitizedDocument[i] = c;
			}

			strDocument = String.copyValueOf(sanitizedDocument);
			sanitizedDocument = null;
		} catch (OutOfMemoryError e) {
			OcamlPlugin.logError("Not enough memory to parse the file " + filePath.toOSString(), e);
			return Status.CANCEL_STATUS;
		}

		final StringReader in = new StringReader(strDocument);
		final OcamlScanner scanner = new OcamlScanner(in);

		// Symbol s;
		// while (true) {
		//
		// try {
		// s = scanner.nextToken();
		// if (s.getId() == Terminals.EOF)
		// break;
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// break;
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// break;
		// }
		// System.err.println(OcamlParser.Terminals.NAMES[s.getId()]);
		// }

		// if(true)
		// return Status.OK_STATUS;
		// System.err.println("parsing");

		final OcamlParser parser = new OcamlParser();

		Def root = null;
		try {
			String extension = filePath.getFileExtension();

			if ("ml".equals(extension))
				root = (Def) parser.parse(scanner);
			else if ("mli".equals(extension))
				root = (Def) parser.parse(scanner, OcamlParser.AltGoals.interfaces);
			else if (!("ml4".equals(extension) || "mlp".equals(extension)))
				OcamlPlugin.logError(extension + " file extension has no associated parser.");
		} catch (Throwable e) {
			// OcamlPlugin.logError("error while parsing", e);
			// System.out.println("unrecoverable syntax error");
			// e.printStackTrace();
		}

		// for(long i = 0; i < 1000000000l; i++);

		/*
		 * recover pieces from the AST (which couldn't be built completely because of an
		 * unrecoverable error)
		 */
		if (root == null || !parser.errorReporting.errors.isEmpty()) {
			// System.err.println("recovering");
			// System.err.println("recovering AST");
			root = new Def("root", Def.Type.Root, 0, 0);

			for (Def def : parser.recoverDefs)
				if (def.bTop && def.name != null && !"".equals(def.name.trim())) {
					def.bTop = false;
					root.children.add(def);
				}
		}

		/*
		 * The source was preprocessed by camlp4: update the identifiers locations using the 'loc'
		 * comments leaved by camlp4
		 */
		if (preprocessor.mustPreprocess()) {
			// System.err.println("associate locations");

			Document document = new Document(strDocument);
			ArrayList<Camlp4Preprocessor.Camlp4Location> camlp4Locations = preprocessor
					.parseCamlp4Locations(this.doc, document);

			preprocessor.associateCamlp4Locations(this.doc, this.doc.get(), document,
					camlp4Locations, root, monitor);
		}

		if (root != null) {
			// System.err.println("cleaning tree");
			root.buildParents();
			root.buildSiblingOffsets();
			cleanTree(root);
		}

		final OcamlOutlineControl outline = this.outline;
		final Def definitions = root;

		definitions.setInInAttribute();

		Def outlineDefinitions = definitions.cleanCopy();
		// remove the definitions the user has chosen not to display
		initPreferences();
		cleanOutline(outlineDefinitions);

		if (file != null) {
			/*
			 * if the source hasn't been modified since the last compilation, try to get the types
			 * inferred by the compiler (in a ".annot" file)
			 */
			if (!editor.isDirty()
					&& OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
							PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE))
				addTypes(file, outlineDefinitions);
		}

		if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_OUTLINE_UNNEST_IN)) {
			outlineDefinitions.unnestIn();
		}

		outlineDefinitions.buildParents();

		final Def fOutlineDefinitions = outlineDefinitions;


		final IFile ifile = file;
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				/* Create error markers for syntax errors found while parsing */
				if (ifile != null) {
					try {
						// delete the previous error markers
						ifile.deleteMarkers("Ocaml.ocamlSyntaxErrorMarker", false,
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

								MarkerUtilities.createMarker(ifile, attributes,
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

				// to notify the hyperlink detector that the outline is
				// available
				synchronized (editor.outlineSignal) {
					editor.outlineSignal.notifyAll();
				}

				if (outline != null) {
					if (OcamlOutlineControl.bOutlineDebugButton
							&& OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
									PreferenceConstants.P_OUTLINE_DEBUG_MODE)) {
						// XXX DEBUG
						// OcamlNewInterfaceParser newParser =
						// OcamlNewInterfaceParser.getInstance();
						// try {
						// Def definitions =
						// newParser.parseModule(str.toString(), "<<<DEBUG>>>",
						// true);
						// outline.setInput(definitions);
						// } catch (Throwable e) {
						//							
						// e.printStackTrace();
						// }
						outline.setInput(definitions);
					} else
						outline.setInput(fOutlineDefinitions);

					// synchronize outline with editor
					if (syncWithEditor)
						editor.synchronizeOutline();
				}

			}
		});

		// long after = System.currentTimeMillis();
		// root.clean();
		// root.print(0);

		// System.out.println("built outline in " + (after - before) + " ms");
		// if(parser.errorReporting.bErrors)
		// System.out.println("Syntax errors reported");

		return Status.OK_STATUS;
	}

	/**
	 * Add OCaml types to the definitions if a ".annot" file is present and up-to-date
	 */
	private void addTypes(IFile file, Def root) {
		if (file == null || root == null)
			return;

		IPath filePath = file.getFullPath();
		File annotFile = Misc.getOtherFileFor(file.getProject(), filePath, ".annot");

		if (annotFile != null && annotFile.exists()) {
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

	private void addTypeRec(TypeAnnotation[] annotations, Def def, boolean root) {
		if (!root) {
			IRegion region = def.getRegion(doc);

			int startOffset = region.getOffset();
			int endOffset = startOffset + region.getLength() - 1;

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

	/** Collapse the "structure", "signature" and "object" nodes */
	public static void cleanTree(Def def) {
		if (def == null)
			return;

		// collapse the <structure> or <signature> node if it is the child of a
		// module or moduletype
		if (def.type == Def.Type.Module || def.type == Def.Type.ModuleType
				|| def.type == Def.Type.Functor) {
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
			return showLet && def.name != null && def.name.length() >= letMinChars;
		case LetIn:
			return showLetIn && def.name != null && def.name.length() >= letInMinChars;
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

	/**
	 * Remove the definitions the user doesn't want to see (outline preference page)
	 */

	private void cleanOutline(Def def) {
		if (def == null)
			return;

		ArrayList<Def> newChildren = new ArrayList<Def>();

		for (Def child : def.children) {
			if (child != null && showDef(child))
				newChildren.add(child);

			cleanOutline(child);
		}

		def.children = newChildren;
	}

	// Pattern patternLocation = Pattern
	// .compile("\\(\\*loc\\: \\[\".*?\"\\: (\\d+)\\:(\\d+)\\-(\\d+) \\d+\\:\\d+\\]\\*\\)");
	// final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'";
	//
	// /**
	// * Update the identifiers source code locations by reading the location comments left by
	// camlp4
	// * (using the '-add_locations' command-line switch)
	// */
	// private void associateCamlp4Locations(IDocument oldDocument, String strOldDocument,
	// IDocument newDocument, ArrayList<Camlp4Location> camlp4Locations, Def def,
	// IProgressMonitor monitor) {
	// // (*loc: ["a.ml": 1:10-11 1:11]*)
	//
	// if (monitor.isCanceled())
	// return;
	//
	// if (def.type != Def.Type.Root) {
	//
	// IRegion region = def.getRegion(newDocument);
	// // String doc = newDocument.get();
	//
	// int pos = region.getOffset() - 1;
	//
	// // parse whitespace
	// // while (pos >= 0 && Character.isSpaceChar(doc.charAt(pos)))
	// // pos--;
	//
	// // binary search
	// int min = 0;
	// int max = camlp4Locations.size() - 1;
	// int locationIndex = min + (max - min) / 2;
	// while (locationIndex > min) {
	// Camlp4Location loc = camlp4Locations.get(locationIndex);
	//
	// if (pos > loc.offset)
	// min = locationIndex;
	// else
	// max = locationIndex;
	//
	// locationIndex = min + (max - min) / 2;
	// }
	//
	// while (true) {
	// Camlp4Location location = camlp4Locations.get(locationIndex);
	//
	// // keep only the definition name instead of the whole definition
	// int lineOffset;
	// try {
	// lineOffset = oldDocument.getLineOffset(location.line - 1);
	// } catch (BadLocationException e) {
	// OcamlPlugin.logError("bad location while associating camlp4 locations", e);
	// return;
	// }
	//
	// int originalOffsetStart = lineOffset + location.colStart;
	// int originalOffsetEnd = lineOffset + location.colEnd;
	//
	// if (originalOffsetEnd > strOldDocument.length())
	// originalOffsetEnd = strOldDocument.length();
	// // def.posStart = 0;
	// // def.posEnd = 0;
	// // break;
	// // }
	//
	// String wholeDefinition = strOldDocument.substring(originalOffsetStart,
	// originalOffsetEnd);
	//
	// int nameIndex = wholeDefinition.indexOf(def.name);
	// int nameLength = def.name.length();
	//
	// while (nameIndex != -1) {
	// char prevChar = ' ';
	// if (nameIndex > 0)
	// prevChar = wholeDefinition.charAt(nameIndex - 1);
	// char nextChar = ' ';
	// if (nameIndex + nameLength < wholeDefinition.length())
	// nextChar = wholeDefinition.charAt(nameIndex + nameLength);
	//
	// /*
	// * if the definition name is preceded or followed by an ocaml identifier char,
	// * it is not a definition name
	// */
	// if (chars.contains("" + prevChar) || chars.contains("" + nextChar))
	// nameIndex = wholeDefinition.indexOf(def.name, nameIndex + 1);
	// else
	// break;
	// }
	//
	// /*
	// * Go to the previous comment if the range we found doesn't contain the definition
	// * name
	// */
	// if (nameIndex == -1) {
	// if (locationIndex > 0) {
	// locationIndex--;
	// continue;
	// } else {
	// def.posStart = 0;
	// def.posEnd = 0;
	// break;
	// }
	// // nameIndex = 0;
	// }
	//
	// // System.out.println(pos);
	//
	// originalOffsetStart += nameIndex;
	// originalOffsetEnd = originalOffsetStart + def.name.length();
	//
	// int lineStart;
	// int lineEnd;
	// int lineOffsetStart;
	// int lineOffsetEnd;
	// try {
	// lineStart = oldDocument.getLineOfOffset(originalOffsetStart);
	// lineEnd = oldDocument.getLineOfOffset(originalOffsetEnd);
	// lineOffsetStart = oldDocument.getLineOffset(lineStart);
	// lineOffsetEnd = oldDocument.getLineOffset(lineEnd);
	// } catch (BadLocationException e) {
	// OcamlPlugin.logError("bad location in camlp4 offsets", e);
	// return;
	// }
	//
	// def.posStart = Def.makePosition(lineStart, originalOffsetStart - lineOffsetStart);
	// def.posEnd = Def.makePosition(lineEnd, originalOffsetEnd - lineOffsetEnd - 1);
	//
	// break;
	//
	// // def.posStart = Def.makePosition(originalLine - 1, originalColStart);
	// // def.posEnd = Def.makePosition(originalLine - 1, originalColEnd - 1);
	// }
	// }
	//
	// for (Def child : def.children)
	// associateCamlp4Locations(oldDocument, strOldDocument, newDocument, camlp4Locations,
	// child, monitor);
	// }
	//
	// private ArrayList<Camlp4Location> parseCamlp4Locations(IDocument oldDoc, IDocument doc) {
	// ArrayList<Camlp4Location> camlp4Locations = new ArrayList<Camlp4Location>();
	//
	// String document = doc.get();
	//
	// Matcher matcher = patternLocation.matcher(document);
	// while (matcher.find()) {
	// int originalLine = Integer.parseInt(matcher.group(1));
	// int originalColStart = Integer.parseInt(matcher.group(2));
	// int originalColEnd = Integer.parseInt(matcher.group(3));
	//
	// camlp4Locations.add(new Camlp4Location(matcher.start(), originalLine, originalColStart,
	// originalColEnd));
	// }
	//
	// return camlp4Locations;
	// }
	//
	// /** A location found in camlp4 output when using the add_locations command-line switch */
	// class Camlp4Location {
	// public Camlp4Location(int offset, int line, int colStart, int colEnd) {
	// this.offset = offset;
	// this.line = line;
	// this.colStart = colStart;
	// this.colEnd = colEnd;
	// }
	//
	// /** the offset of the location in the camlp4 output */
	// int offset;
	//
	// int line;
	// int colStart;
	// int colEnd;
	// }
}
