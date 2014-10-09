package ocaml.editor.completion;

import java.io.File;
import java.util.ArrayList;

import ocaml.OcamlPlugin;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This class implements a completion help box, that displays the ocamldoc comment associated with
 * the element selected in the completion box. It formats text so as to make it fit inside the
 * width of the text box, and it colors pieces of code delimited by "[" and "]".
 */
public class OcamlInformationPresenter implements DefaultInformationControl.IInformationPresenter {

	public String updatePresentation(Display display, String infoText,
			TextPresentation presentation, int maxWidth, int maxHeight) {

		final Color colorSection = new Color(display, 150, 50, 191);
		final Color colorParent = new Color(display, 191, 100, 50);
		final Color colorCode = new Color(display, 0, 0, 255);
		final Color colorModuleName = new Color(display, 119, 131, 112);
		final Color colorFilename = new Color(display, 64, 64, 64);

		String[] infos = infoText.split("\\$\\@\\|");

		// templates don't respect the same format
		if (infos.length != 5)
			infos = new String[] { "", "", "", infoText, "" };

		// the offset in the generated text, in number of characters
		int offset = 0;

		// the result string
		StringBuilder result = new StringBuilder();

		String text;

		String parentName = infos[0].trim();
		String body = infos[1].trim();
		String sectionComment = infos[2].trim();
		String comment = infos[3].trim();
		String filename = infos[4].trim();

		if (!body.equals("")) {
			text = body + "\n";
			result.append(text);

			// Color colorBody = new Color(display, 50, 150, 200);
			presentation.addStyleRange(new StyleRange(offset, text.length(), null, null, SWT.BOLD));
			offset += text.length();
		}

		if (!parentName.equals("")) {
			text = "In " + parentName + "\n";
			result.append(text);

			presentation.addStyleRange(new StyleRange(offset, text.length(), colorParent, null,
				SWT.NONE));
			offset += text.length();
		}

		infoText = comment;

		// leave some room for the scrollbar width
		maxWidth = maxWidth - 25;

		Shell shell = display.getActiveShell();
		if (shell == null) {
			OcamlPlugin.logError("OcamlInformationPresenter: no active shell");
			return "<Error: No active SWT shell>";
		}

		/*
		 * The GC (graphics context) is used to measure strings to know when we must insert a newline so 
		 * that the text fits in the width of the completion help box.
		 */
		final GC gc = new GC(shell);

		/*
		 * Assigns the default font to the GC, so that it can measure words in pixels.
		 */
		gc.setFont(null);

		// Color colorCode = new Color(display, 63, 95, 255);

		// remove redundant spaces
		infoText = infoText.replaceAll("( |\\t)( |\\t)+", " ");
		// parse the text into words
		ArrayList<String> words = this.split(infoText);

		// offset from the start of the generated line in pixels
		int offsetInPixels = 0;

		// an interval with a text style
		StyleRange styleRange;
		/*
		 * the delimiters "[" and "]" are used to color code in ocamldoc comments
		 * 
		 */
		int braceNestingLevel = 0;
		String prevWord = "";

		// a comment annotation is a word starting by an "@"
		boolean bAnnotation = false;

		// true if we are at the beginning of a line in the original text
		boolean bNewLineBegin = false;
		// whether the next character must be escaped ("\[" for example)
		boolean bEscapeNextChar = false;

		for (int i = 0; i < words.size(); i++) {

			String word = words.get(i);

			String nextWord = "";
			if (i < words.size() - 1)
				nextWord = words.get(i + 1);

			// this word's first character must be escaped
			if (bEscapeNextChar) {
				result.append(word.charAt(0));

				// add the correct style to this character
				if (braceNestingLevel > 0)
					presentation
						.addStyleRange(new StyleRange(offset, 1, colorCode, null, SWT.NONE));
				else
					presentation.addStyleRange(new StyleRange(offset, 1, null, null, SWT.NONE));

				// make a guess at the width (so that we don't have to use the GC just for that...)
				offsetInPixels += 10;
				offset++;
				word = word.substring(1);
				bEscapeNextChar = false;
			}

			if (word.equals("\\")
					&& (nextWord.startsWith("[") || nextWord.startsWith("]")
							|| nextWord.startsWith("{") || nextWord.startsWith("}") || nextWord
						.startsWith("@"))) {
				bEscapeNextChar = true;
				continue;
			}

			if (word.startsWith("[")) {
				if (braceNestingLevel == 0)
					word = word.substring(1);
				braceNestingLevel++;
			} else if (word.startsWith("]")) {
				if (braceNestingLevel == 1)
					word = word.substring(1);
				braceNestingLevel--;
			}

			if (word.equals("\n")) {
				bNewLineBegin = true;

				// new paragraph
				if (prevWord.equals("\n")) {
					result.append("\n\n");
					offset += 2;
					offsetInPixels = 0;
					prevWord = "";
					continue;
				}
				/* replace newlines by spaces */
				else {
					result.append(" ");
					offset++;
					offsetInPixels += gc.stringExtent(" ").x;
					prevWord = "\n";
					continue;
				}
			}

			// an element in a list
			if (word.startsWith("-") && bNewLineBegin) {
				result.append("\n");
				offset++;
				offsetInPixels = 0;
				bNewLineBegin = false;
			}

			if (word.startsWith("@")) {
				bAnnotation = true;
				result.append("\n");
				offset++;
				offsetInPixels = 0;
				word = word.substring(1);
			} else
				bAnnotation = false;

			// we are in code
			if (braceNestingLevel > 0)
				styleRange = new StyleRange(offset, word.length(), colorCode, null, SWT.NONE);
			else if (bAnnotation)
				styleRange = new StyleRange(offset, word.length(), null, null, SWT.BOLD);
			else
				styleRange = new StyleRange(offset, word.length(), null, null, SWT.NONE);

			// the word's width in pixels
			int length = gc.stringExtent(word).x;

			// annotations are bold (so they are larger than normal text) : make a guess at the width...
			if (bAnnotation)
				length *= 1.17;

			// if the word would get out of sight, we put it on the next line
			if (offsetInPixels + length > maxWidth) {
				// we remove the starting space on this word (see split() )
				if (word.startsWith(" "))
					word = "\n" + word.substring(1);
				else {
					word = "\n" + word;
					styleRange.length++;
				}

				offsetInPixels = length;
			} else
				offsetInPixels += length;

			// add this word to the result
			result.append(word);

			// ajouter cet intervalle stylé dans la présentation
			presentation.addStyleRange(styleRange);

			// on s'est déplacé de la longueur du mot
			offset += word.length();

			prevWord = word;
			if (!word.trim().equals(""))
				bNewLineBegin = false;

		}

		if (!sectionComment.equals("")) {
			text = "\nSection:\n" + sectionComment.trim() + "\n";
			result.append(text);

			presentation.addStyleRange(new StyleRange(offset, text.length(), colorSection, null,
				SWT.NONE));
			offset += text.length();
		}

		if (!filename.equals("")) {
			// attach module name
			String[] parts = filename.split(File.separator);
			String moduleName = "";
			if (parts.length > 1) {
				moduleName = "Module: " + parts[parts.length - 1];
			}
			String strResult = result.toString();
			if (strResult.endsWith("\n\n"))
				text = moduleName;
			else if (strResult.endsWith("\n"))
				text = "\n" + moduleName;
			else
				text = "\n\n" + moduleName;

			result.append(text);
			presentation.addStyleRange(new StyleRange(offset, text.length(), colorModuleName, null,
				SWT.ITALIC));
			offset += text.length();

			// attach file name
			text = "\n" + filename;
			result.append(text);
			presentation.addStyleRange(new StyleRange(offset, text.length(), colorFilename, null,
				SWT.ITALIC));
			offset += text.length();
		}

		// do NOT dispose colors here (or else it will crash)
		// destroy the GC
		gc.dispose();

		return result.toString();
	}

	/** split a text into words while keeping separators (unlike String.split() ) */
	private ArrayList<String> split(String text) {
		ArrayList<String> words = new ArrayList<String>();
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			// cut before delimiter
			if (" ()[]{}=@<>&|+*/\\".contains("" + ch)) {
				if (word.length() != 0)
					words.add(word.toString());
				word.setLength(1);
				word.setCharAt(0, ch);
			}
			// cut after delimiter
			else if (",;:.-".contains("" + ch)) {
				word.append(ch);
				if (word.length() != 0)
					words.add(word.toString());
				word.setLength(0);
			}
			// end of line
			else if (ch == '\n') {
				if (word.length() != 0)
					words.add(word.toString());

				// a word "newline"
				word.setLength(1);
				word.setCharAt(0, '\n');
				words.add(word.toString());

				word.setLength(0);
			} else
				word.append(text.charAt(i));
		}

		// add the last word
		if (word.length() != 0)
			words.add(word.toString());

		return words;
	}
}
