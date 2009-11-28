package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** A rule to match punctuation characters */
public class PunctuationRule implements IRule {

	/**
	 * A string containing all the matching punctuation characters. Each character must be in the range
	 * [0-128] in the ASCII table
	 */
	final static String strPunctuationChars = "()[]{}<>&~#|\\^@=$%,;:?!+-*/`.";
	/** A table to speed up the predicate "is a given character a punctuation character" */
	final static boolean[] punctuationChars;

	static {
		punctuationChars = new boolean[128];
		for (int i = 0; i < strPunctuationChars.length(); i++) {
			punctuationChars[strPunctuationChars.charAt(i)] = true;

		}
	}

	/** The token we return if the rule matched on the input */
	private final IToken token;

	public PunctuationRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner) {

		int ch = scanner.read();

		if (ch >= 0 && ch <= 128 && punctuationChars[ch])
			return this.token;

		// put back the unmatched character
		scanner.unread();

		return Token.UNDEFINED;
	}
}
