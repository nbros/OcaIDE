package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** A rule to match punctuation characters */
public class PunctuationRule  implements IRule {
	
	/** The token we return if the rule matched on the input */
	private final IToken token;
	
	public PunctuationRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner) {

		int ch = scanner.read();
		
		if("()[]{}<>&~#|\\^@=$%,;:?!+-*/`".contains(""+((char)ch)))
			return this.token;
		
		// put back the unmatched character
		scanner.unread();

		return Token.UNDEFINED;
	}
}
