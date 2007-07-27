package ocaml.editors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.editor.formatting.OcamlFormater;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

/**
 * Implements a set of automatic edit strategies. These "strategies" are rules for replacing strings by other
 * strings while typing, and we use them in particular to indent automatically while entering code.
 */
public class OcamlAutoEditStrategy implements IAutoEditStrategy {

	/** keywords than when found at the end of a line indent the next line */
	private final Pattern lineContinuatorsPattern = Pattern
			.compile("\\Winitializer$|\\Wthen$|\\Welse$|\\Wbegin$|\\Wdo$|\\Wmodule$|"
					+ "\\Wstruct$|\\Wtry$|\\Wsig$|=$|->$|\\{$|\\($|\\[$");

	/** Definition of a new type by "type name = ¶" */
	private final Pattern patternTypeDef = Pattern.compile("^type\\s+\\w*\\s*=$");

	/** A comment opened at the beginning of the line */
	private Pattern patternCommentOpen = Pattern.compile("^\\(\\*");

	/** A "no-format" comment opened at the beginning of the line */
	private Pattern patternCommentOpenNoFormat = Pattern.compile("^\\(\\*\\|");

	/** A documentation comment opened at the beginning of the line */
	private Pattern patternDocCommentOpen = Pattern.compile("^\\(\\*\\*");

	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {

		if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_EDITOR_DISABLE_AUTOFORMAT))
			return;

		// String doc = document.get();
		final String eol = TextUtilities.getDefaultLineDelimiter(document);

		IRegion lineRegion;
		IRegion previousLineRegion;
		try {
			lineRegion = document.getLineInformationOfOffset(command.offset);

			int prevOffset = lineRegion.getOffset();
			if (lineRegion.getOffset() > 0)
				prevOffset--;
			previousLineRegion = document.getLineInformationOfOffset(prevOffset);

		} catch (BadLocationException e) {
			ocaml.OcamlPlugin.logError("error 1 in OcamlAutoEditStrategy:customizeDocumentCommand", e);
			return;
		}

		String previousLine;
		String line;
		try {
			line = document.get(lineRegion.getOffset(), lineRegion.getLength());
			previousLine = document.get(previousLineRegion.getOffset(), previousLineRegion.getLength());
		} catch (BadLocationException e) {
			ocaml.OcamlPlugin.logError("error 2 in OcamlAutoEditStrategy:customizeDocumentCommand", e);
			return;
		}

		int indent = OcamlFormater.getLineIndent(line);
		int lineLength = line.length();
		// int prevLineLength = previousLine.length();
		int previousIndent = OcamlFormater.getLineIndent(previousLine);
		String trimmed = line.trim();
		previousLine = previousLine.trim();

		int offsetInLine = command.offset - lineRegion.getOffset();

		String beforeCursor = line.substring(0, offsetInLine).trim();
		String afterCursor = line.substring(offsetInLine, line.length()).trim();
		
		try {
			ITypedRegion region = document.getPartition(offsetInLine);
			if(OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT.equals(region.getType())||
					OcamlPartitionScanner.OCAML_MULTILINE_COMMENT.equals(region.getType()))
				return;
		} catch (BadLocationException e) {
			OcamlPlugin.logError("bad location in OcamlAutoEditStrategy", e);
			return;
		}
		
		// automatically add the second '"'
		if (command.text.equals("\"") && OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_EDITOR_DOUBLEQUOTES)) {
			//document.getPartition(offsetInLine) == OcamlPartitionScanner.OCAML_MULTILINE_COMMENT)
			
			if(beforeCursor.length() > 0){
				char lastChar = beforeCursor.charAt(beforeCursor.length() - 1);
				if(lastChar == '\\' || lastChar == '\'')
					return;
			}
			
			// if there is already one " then keep it and don't add a second one
			if(line.length() > offsetInLine){
				char nextChar = line.charAt(offsetInLine);
				if(nextChar == '"'){
					command.text = "";
					command.shiftsCaret = false;
	                command.caretOffset = command.offset + 1;
	                return;
				}
					
			}
			
			// only complete " when there is nothing after it 
			if(!"".equals(afterCursor))
				return;
			
			
			// determine whether this is the end of a string or a new string
			boolean bOpen = false;
			boolean bEscape = false;
			for(int i = 0; i < beforeCursor.length(); i++){
				if(beforeCursor.charAt(i) == '"' && !bEscape)
					bOpen = !bOpen;
				
				bEscape = (beforeCursor.charAt(i) == '\\');
			}
			
			if(!bOpen){
				// transform the single " into two ""
				command.text = "\"\"";
				command.shiftsCaret = false;
                command.caretOffset = command.offset + 1;
                return;
			}
		}
		

		/*
		 * <enter> : parse the line to determine the logical indentation of the next line, and to add some
		 * O'Caml language constructs automatically
		 */

		else if (command.text.equals(eol)) {

			/*
			 * A comment opened at the beginning of the line: if it is not closed, we close it and we open it
			 * again on the next line.
			 */
			if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_CONTINUE_COMMENTS)) {
				Matcher matcher = patternCommentOpenNoFormat.matcher(beforeCursor);
				if (matcher.find() && !beforeCursor.contains("*)")) {
					command.text = "*)" + eol + makeIndent(indent) + "(*| ";
					return;
				}

				// We don't touch documentation comments
				matcher = patternDocCommentOpen.matcher(beforeCursor);
				if (matcher.find())
					return;

				matcher = patternCommentOpen.matcher(beforeCursor);
				if (matcher.find() && !beforeCursor.contains("*)")) {
					command.text = "*)" + eol + makeIndent(indent) + "(* ";
					return;
				}
			}

			int cont = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_AUTO_INDENT_CONT) ? 1 : 0;

			boolean pipeType = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_PIPE_AFTER_TYPE);

			boolean pipeWith = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_PIPE_AFTER_WITH);

			boolean pipeFun = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_PIPE_AFTER_FUN);

			boolean dedentSemiSemi = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_DEDENT_SEMI_SEMI);

			boolean indentIn = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_INDENT_IN);

			boolean removePipe = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_REMOVE_PIPE);

			boolean continuePipes = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_EDITOR_CONTINUE_PIPES);

			Matcher matcher = patternTypeDef.matcher(beforeCursor);
			if (matcher.find()) {
				command.text = eol + makeIndent(indent + cont) + (pipeType ? "| " : "");
				return;
			}

			matcher = lineContinuatorsPattern.matcher("$" + beforeCursor);
			if (matcher.find())
				command.text = eol + makeIndent(indent + cont);
			else {
				if (endsWith("$" + beforeCursor, "\\Wwith"))
					command.text = eol + makeIndent(indent + 1) + (pipeWith ? "| " : "");
				else if (endsWith("$" + beforeCursor, "\\Wfun"))
					command.text = eol + makeIndent(indent + 1) + (pipeFun ? "| " : "");
				else if (endsWith("$" + beforeCursor, "\\Wfunction"))
					command.text = eol + makeIndent(indent + 1) + (pipeFun ? "| " : "");
				else if (beforeCursor.endsWith(";;") && dedentSemiSemi)
					command.text = eol;
				else if (endsWith("$" + beforeCursor, "\\Win") && indentIn)
					command.text = eol + makeIndent(indent + 1);
				// if the previous line is blank, stop the "|" (and remove the previous one)
				else if (trimmed.equals("|") && removePipe) {
					command.offset = lineRegion.getOffset();
					command.length = lineRegion.getLength();
					command.text = eol;
				}

				else if (trimmed.startsWith("|") && trimmed.equals(beforeCursor)) {
					if(!trimmed.endsWith(";") && continuePipes)
						command.text = eol + makeIndent(indent) + "| ";
					else
						command.text = eol + makeIndent(indent + 1);
				}

				else if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_KEEP_INDENT))
					command.text = eol + makeIndent(indent);

			}
		} else if (command.text.equals("\t")) {
			/*
			 * If we are at the beginning of a line and we type <tab>, then we add the same indentation as the
			 * previous line.
			 */
			if (offsetInLine == 0 && previousIndent != 0 && trimmed.equals("")) {
				if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_INTELLIGENT_INDENT_START)) {

					Matcher matcher = lineContinuatorsPattern.matcher("$" + previousLine);
					if (matcher.find())
						command.text = makeIndent(previousIndent + 1);
					else
						command.text = makeIndent(previousIndent);

					command.length = lineLength;
				}
			}
			/* ::<tab> inserts [] (empty list in O'Caml) */
			else if (offsetInLine > 2 && line.charAt(offsetInLine - 1) == ':'
					&& line.charAt(offsetInLine - 2) == ':') {
				if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_COLON_COLON_TAB)) {
					command.text = "[]";
				}
				/* complete fn<tab> into "function " */
			} else if (offsetInLine > 3 && line.charAt(offsetInLine - 2) == 'f'
					&& line.charAt(offsetInLine - 1) == 'n'
					&& !Character.isJavaIdentifierPart(line.charAt(offsetInLine - 3))) {
				if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_FN_TAB)) {
					command.offset--;
					command.length = 1;
					command.text = "unction ";
				}
			}

			/*
			 * The <tab> key is also used to insert the "->" characters if we passed the initial indentation
			 */
			else if (!beforeCursor.equals("")) {
				if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_TAB_ARROW)) {
					command.text = " -> ";
				}
			}
		}
	}

	/** Build a string of <code>nTabs</code> tabulations */
	private String makeIndent(int nTabs) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < nTabs; i++)
			result.append('\t');
		return result.toString();
	}

	/** Does the string <code>str</code> end by the regular expression <code>end</code>? */
	private boolean endsWith(String str, String end) {
		Pattern pattern = Pattern.compile(end + "$");
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}
}
