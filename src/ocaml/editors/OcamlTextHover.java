package ocaml.editors;

import java.io.File;
import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.debugging.OcamlDebugger;
import ocaml.preferences.PreferenceConstants;
import ocaml.typeHovers.OcamlAnnotParser;
import ocaml.typeHovers.TypeAnnotation;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextViewer;

/**
 * Choose the message to display in the pop-up that appears when the user hovers over the contents
 * of the editor. Depending on context, we display type informations, the value of the variable
 * under the mouse cursor (if the debugger is active), the error marker if there is one there, or a
 * combination of these elements.
 */
public class OcamlTextHover implements ITextHover {
	private OcamlEditor ocamlEditor;

	public OcamlTextHover(OcamlEditor ocamlEditor) {
		this.ocamlEditor = ocamlEditor;
	}

	/**
	 * Returns the string to display in a pop-up which gives informations about the element
	 * currently under the mouse cursor in the editor
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		IFile file = ocamlEditor.getFileBeingEdited();
		if(file == null)
			return "<null file>";
		
		if (hoverRegion == null) {
			ocaml.OcamlPlugin.logError("OcamlTextHover:getHoverInfo null region");
			return "";
		}

		try {
			// error message string, if we found one in the hovered region
			String errorMessage = "";

			int hoverOffset = hoverRegion.getOffset();
			IMarker[] markers = file.findMarkers(null, true, 1);
			for (IMarker marker : markers) {
				int markerStart = marker.getAttribute(IMarker.CHAR_START, -1);
				int markerEnd = marker.getAttribute(IMarker.CHAR_END, -1);

				if (hoverOffset >= markerStart && hoverOffset <= markerEnd) {
					String message = marker.getAttribute(IMarker.MESSAGE, "<Error reading marker>");
					errorMessage = errorMessage + message.trim() + "\n";
				}
			}

			// if the debugger is started, we ask it for the value of the variable under the cursor
			if (OcamlDebugger.getInstance().isStarted()) {
				String text = textViewer.getDocument().get();
				String expression = expressionAtOffset(text, hoverOffset).trim();

				if (!expression.equals("")) {
					String value = OcamlDebugger.getInstance().display(expression).trim();

					if (!value.equals(""))
						return (errorMessage + Misc.beautify(value)).trim();
				}
			}

			if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_SHOW_TYPES_IN_POPUPS)) {
				IPath filePath = file.getFullPath();

				File annotFile = Misc.getOtherFileFor(file.getProject(), filePath, ".annot");

				if (annotFile != null && annotFile.exists()) {
					boolean bUpToDate = filePath.toFile().lastModified() <= annotFile
							.lastModified();

					if (!ocamlEditor.isDirty() && bUpToDate) {
						ArrayList<TypeAnnotation> found = new ArrayList<TypeAnnotation>();

						TypeAnnotation[] annotations = OcamlAnnotParser.parseFile(annotFile,
								textViewer.getDocument());
						if (annotations != null) {
							for (TypeAnnotation annot : annotations)
								if (annot.getBegin() <= hoverOffset && hoverOffset < annot.getEnd())
									found.add(annot);

							/*
							 * Search for the smallest hovered type annotation
							 */
							TypeAnnotation annot = null;
							int minSize = Integer.MAX_VALUE;

							for (TypeAnnotation a : found) {
								int size = a.getEnd() - a.getBegin();
								if (size < minSize) {
									annot = a;
									minSize = size;
								}
							}

							if (annot != null) {

								String doc = ocamlEditor.getDocumentProvider().getDocument(
										ocamlEditor.getEditorInput()).get();
								String expr = doc.substring(annot.getBegin(), annot.getEnd());
								String[] lines = expr.split("\\n");
								if (expr.length() < 50 && lines.length <= 6)
									return (errorMessage + expr + ": " + annot.getType()).trim();
								else if (lines.length > 6) {
									int l = lines.length;

									return (errorMessage + lines[0] + "\n" + lines[1] + "\n"
											+ lines[2] + "\n" + "..." + (l - 6)
											+ " more lines...\n" + lines[l - 3] + "\n"
											+ lines[l - 2] + "\n" + lines[l - 1] + "\n:" + annot
											.getType()).trim();
								} else
									return (errorMessage + expr + "\n:" + annot.getType()).trim();
							}
						}
					}
				}
			}

			return errorMessage.trim();
			/*
			 * if (!this.ocamlEditor.isDirty()) return
			 * this.ocamlEditor.getTypeInfoAt(hoverOffset).trim();
			 */

		} catch (Throwable e) {
			ocaml.OcamlPlugin.logError("Erreur dans OcamlTextHover:getHoverInfo", e);
		}

		return "";
	}

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}

	/** Return the expression under the cursor */
	private String expressionAtOffset(String text, int offset) {
		int endOffset = text.length();
		int length = text.length();

		final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'";

		for (int i = offset; i < length; i++) {
			if (!chars.contains("" + text.charAt(i))) {
				endOffset = i;
				break;
			}
		}

		int startIndex = offset;
		for (int i = endOffset - 1; i >= 0; i--) {
			if (i == 0) {
				startIndex = 0;
				break;
			}

			if (!chars.contains("" + text.charAt(i))) {
				startIndex = i + 1;
				break;
			}
		}

		if (startIndex > endOffset)
			return "";

		return text.substring(startIndex, endOffset);
	}

	/**
	 * Get a type annotation for the currently edited ocaml file, at <code>offset</code>
	 * <p>
	 * Put all the information on a single line (so that it can be displayed in the satus bar)
	 */
	public static String getAnnotAt(OcamlEditor editor, TextViewer viewer, int offset) {
		IFile file = editor.getFileBeingEdited();
		if(file == null)
			return "<null file>";
		
		IPath filePath = file.getLocation();

		String fileName = filePath.lastSegment();
		if (fileName.endsWith(".ml")) {
			String annotFilename = fileName.substring(0, fileName.length() - 3) + ".annot";

			File annotFile = filePath.removeLastSegments(1).append(annotFilename).toFile();

			if (annotFile.exists()) {
				boolean bUpToDate = filePath.toFile().lastModified() <= annotFile.lastModified();

				if (!editor.isDirty() && bUpToDate) {
					ArrayList<TypeAnnotation> found = new ArrayList<TypeAnnotation>();

					TypeAnnotation[] annotations;
					try {
						annotations = OcamlAnnotParser.parseFile(annotFile, viewer.getDocument());
					} catch (BadLocationException e) {
						OcamlPlugin.logError("getting type annotation", e);
						return "";
					}
					if (annotations != null) {
						for (TypeAnnotation annot : annotations)
							if (annot.getBegin() <= offset && offset <= annot.getEnd())
								found.add(annot);

						/*
						 * Search for the smallest type annotation at this offset
						 */
						TypeAnnotation annot = null;
						int minSize = Integer.MAX_VALUE;

						for (TypeAnnotation a : found) {
							int size = a.getEnd() - a.getBegin();
							if (size < minSize) {
								annot = a;
								minSize = size;
							}
						}

						if (annot != null) {

							String doc = editor.getDocumentProvider().getDocument(
									editor.getEditorInput()).get();

							int begin = annot.getBegin();
							int end = annot.getEnd();

							if (end >= doc.length())
								return "";

							String expr = doc.substring(begin, end);
							String[] lines = expr.split("\\r?\\n");

							expr = "";
							for (String line : lines)
								expr = expr + " " + line;

							if (expr.length() > 30)
								expr = expr.substring(0, 15) + "..."
										+ expr.substring(expr.length() - 15, expr.length());

							return (expr + ": " + annot.getType()).trim();
						}
					}
				}
			}
		}

		return "";

	}
}
