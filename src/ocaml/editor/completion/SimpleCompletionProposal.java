package ocaml.editor.completion;

import ocaml.parser.Def;
import ocaml.views.outline.OcamlOutlineLabelProvider;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A completion proposal for annotations in ocamldoc comments, and for definitions found in the currently
 * edited file.
 */
public class SimpleCompletionProposal implements ICompletionProposal, Comparable<Object> {
	private String replacementString;

	private int replacementOffset;

	private int replacementLength;

	private int cursorPosition;
	
	private Def def;

	public SimpleCompletionProposal(String replacementString, int replacementOffset, int replacementLength,
			int cursorPosition) {
		this.replacementString = replacementString;
		this.replacementOffset = replacementOffset;
		this.replacementLength = replacementLength;
		this.cursorPosition = cursorPosition;
	}

	public SimpleCompletionProposal(Def def, String replacementString, int replacementOffset, int replacementLength,
			int cursorPosition) {
		this.replacementString = replacementString;
		this.replacementOffset = replacementOffset;
		this.replacementLength = replacementLength;
		this.cursorPosition = cursorPosition;
		this.def = def;
	}

	public void apply(IDocument document) {
		try {
			document.replace(replacementOffset, replacementLength, replacementString);
		} catch (BadLocationException badlocationexception) {
		}
	}

	public Point getSelection(IDocument document) {
		return new Point(replacementOffset + cursorPosition, 0);
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public Image getImage() {
		// use the same image as in the outline
		return OcamlOutlineLabelProvider.retrieveImage(def);
	}

	public String getDisplayString() {
		return replacementString;
	}

	public String getAdditionalProposalInfo() {
		return null;
	}

	/**
	 * Implementation of the <code>Comparator</code> interface to sort the module's completion propositions
	 * and to avoid having the same proposition twice (by using a TreeSet, which guarantees that a name can
	 * appear only once)
	 */
	public int compareTo(Object o) {
		if (o instanceof SimpleCompletionProposal) {
			SimpleCompletionProposal e = (SimpleCompletionProposal) o;
			return replacementString.compareTo(e.replacementString);
		}

		throw new ClassCastException();
	}
}