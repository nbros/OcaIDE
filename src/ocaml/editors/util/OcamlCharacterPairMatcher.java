package ocaml.editors.util;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

/** Find the corresponding delimiter to the delimiter whose position is given */
public class OcamlCharacterPairMatcher implements ICharacterPairMatcher {

	private final char[] openChars = { '(', '{', '[' };
	private final char[] closeChars = { ')', '}', ']' };

	public IRegion match(IDocument document, int offset) {
		
		if(offset < 1)return null;
		
		String doc = document.get();
		int length = doc.length();
		
		for (int nChar = 0; (nChar < openChars.length) && (nChar < closeChars.length); nChar++) {
			char open = openChars[nChar];
			char close = closeChars[nChar];

			if (doc.charAt(offset - 1) == open) {
				int nOpen = 1;
				for (int i = offset; i < length; i++) {
					if (doc.charAt(i) == open)
						nOpen++;
					else if (doc.charAt(i) == close)
						nOpen--;
					if (nOpen == 0) {
						return new Region(i, 1);
					}
				}
			}

			if (doc.charAt(offset-1) == close) {
				int nClosed = 1;
				for (int i = offset - 2; i >= 0; i--) {
					if (doc.charAt(i) == close)
						nClosed++;
					else if (doc.charAt(i) == open)
						nClosed--;
					if (nClosed == 0) {
						return new Region(i, 1);
					}
				}
			}
		}

		return null;
	}

	/** We always anchor to the left (as the JDT editor) */
	public int getAnchor() {
		return ICharacterPairMatcher.LEFT;
	}
	/** This method is required by the interface, but we don't use it */
	public void clear() {
	}
	/** This method is required by the interface, but we don't use it */
	public void dispose() {
	}

	
}
