package ocaml.editor.completion;

import ocaml.OcamlPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/** Represents a context information box associated with an element, that can format its text (with colors
 * and styles) and say when it is no longer needed. */
public class OcamlContextInformation implements IContextInformationValidator,
		IContextInformationPresenter {

	private int offset = 0;
	private ITextViewer viewer;
	private IContextInformation contextInformation;

	public boolean isContextInformationValid(int offset) {

		//System.err.println(offset);

		IDocument doc = viewer.getDocument();
		String text = doc.get();
		int length = text.length();

		if (this.offset == length)
			return true;
		if (this.offset > length)
			return false;

		

		/*
		 * If we find a dot between the completion position and the current position, then we don't 
		 * need informations about the element before the dot.
		 */
		
		for (int i = this.offset; i <= offset - 1 ; i++) {
			char ch = text.charAt(i);
			//System.err.println(ch);
			if (ch == '.')
				return false;
			else if (!Character.isLetter(ch))
				break;
		}

		int nPar = 0;
		int nCurlyBrackets = 0;
		int nSquareBrackets = 0;
		for (int i = this.offset; i <= offset - 1; i++) {
			char ch = text.charAt(i);
			if (ch == '(')
				nPar++;
			else if (ch == ')')
				nPar--;
			else if (ch == '[')
				nSquareBrackets++;
			else if (ch == ']')
				nSquareBrackets--;
			else if (ch == '{')
				nCurlyBrackets++;
			else if (ch == '}')
				nCurlyBrackets--;
			else if(ch == ';' && nPar == 0 && nSquareBrackets == 0 && nCurlyBrackets == 0)
				return false;
		}

		/*
		 * If we closed more delimiters than we opened, it means we exited the lexical scope in which
		 * the definition of the element is needed.
		 */

		if (nPar < 0)
			return false;

		try {
			int newLine = doc.getLineInformationOfOffset(offset).getOffset();
			int oldLine = doc.getLineInformationOfOffset(this.offset).getOffset();

			return oldLine == newLine;

		} catch (BadLocationException e) {
			OcamlPlugin.logError("IContextInformationValidator: bad location", e);
		}

		return false;
	}

	public void install(IContextInformation info, ITextViewer viewer, int offset) {
		this.viewer = viewer;
		this.offset = offset;
		this.contextInformation = info;

	}

	/**
	 * Formats the popup text with text attributes and colors. The different parts to format are
	 * delimited by unbreakable spaces (in OcamlCompletionProcessor).
	 */
	public boolean updatePresentation(int offset, TextPresentation presentation) {

		final Color colorSection = new Color(Display.getDefault(), 150, 50, 191);
		final Color colorLink = new Color(Display.getDefault(), 64, 160, 64);
		final Color colorCode = new Color(Display.getDefault(), 0, 0, 255);
		// final Color colorFilename = new Color(Display.getDefault(), 64, 64, 64);

		String text = contextInformation.getInformationDisplayString();

		int delim1 = text.indexOf('\u00A0');
		int delim2 = text.indexOf('\u00A0', delim1 + 1);
		int delim3 = text.indexOf('\u00A0', delim2 + 1);

		// System.err.println("1:" + delim1 + " 2:"+delim2 + " 3:" + delim3);

		if (delim1 == -1 || delim2 == -1 || delim3 == -1)
			return false;

		presentation.addStyleRange(new StyleRange(0, delim1, null, null, SWT.BOLD));

		int codeNestingLevel = 0;
		boolean bEscape = false;
		boolean bLink = false;

		for (int i = delim1 + 1; i < delim2; i++) {
			char ch = text.charAt(i);

			if (ch == '[' && !bEscape)
				codeNestingLevel++;

			if (ch == '{' && !bEscape && codeNestingLevel == 0 && i < delim2 - 1
					&& text.charAt(i + 1) == '!')
				bLink = true;

			if (bLink)
				presentation.addStyleRange(new StyleRange(i, 1, colorLink, null, SWT.NONE));

			if (ch == '}' && !bEscape)
				bLink = false;

			if (codeNestingLevel > 0)
				presentation.addStyleRange(new StyleRange(i, 1, colorCode, null, SWT.NONE));

			if (ch == ']' && !bEscape)
				codeNestingLevel--;

			if (ch == '\\')
				bEscape = !bEscape;
			else
				bEscape = false;

			/*
			 * else presentation.addStyleRange(new StyleRange(i, 1, colorCode, null, SWT.NONE));
			 */

		}

		if (delim2 - delim1 > 0)
			presentation.addStyleRange(new StyleRange(delim2, delim3 - delim2, colorSection, null,
				SWT.NONE));
		if (delim3 - delim2 > 0)
			presentation.addStyleRange(new StyleRange(delim3, text.length() - delim3, null, null,
				SWT.ITALIC));

		// crashes if we return true, so...
		return false;
	}

}
