package ocaml.editors;

import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;

/** Defines what to do when we double-click in the OCaml editor: select the double-clicked word, or select
 * everything from the double-clicked delimiter (bracket) to the corresponding one.*/
public class OcamlDoubleClickStrategy implements ITextDoubleClickStrategy {
	private final char[] openChars = { '(', '{', '[' };
	private final char[] closeChars = { ')', '}', ']' };

	public void doubleClicked(ITextViewer viewer) {

		int offset = viewer.getSelectedRange().x;
		IDocument document = viewer.getDocument();
		String doc = document.get();
		int length = doc.length();
		
		if(offset >= length)return;

		for (int nChar = 0; (nChar < openChars.length) && (nChar < closeChars.length); nChar++) {
			char open = openChars[nChar];
			char close = closeChars[nChar];

			if (offset >= 1 && doc.charAt(offset - 1) == open) {
				int nOpen = 1;
				for (int i = offset; i < length; i++) {
					if (doc.charAt(i) == open)
						nOpen++;
					else if (doc.charAt(i) == close)
						nOpen--;
					if (nOpen == 0) {
						viewer.setSelectedRange(offset, i - offset);
						return;
					}
				}
			}

			if (doc.charAt(offset) == close) {
				int nClosed = 1;
				for (int i = offset - 1; i >= 0; i--) {
					if (doc.charAt(i) == close)
						nClosed++;
					else if (doc.charAt(i) == open)
						nClosed--;
					if (nClosed == 0) {
						viewer.setSelectedRange(i + 1, offset - i - 1);
						return;
					}
				}
			}
		}
		
		new DefaultTextDoubleClickStrategy().doubleClicked(viewer);
	}
}
