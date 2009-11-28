package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** This rule matches empty comments of this kind: (****) */
public class EmptyCommentRule implements IPredicateRule {

	/** The token we return if the rule matched on the input */
	private final IToken token;

	public EmptyCommentRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {

		int ch = '(';
		// must start by an open parenthesis unless we are continuing
		if (!resume)
			ch = scanner.read();
		int nRead = resume ? 0 : 1;

		if (ch == '(') {
			// at least a first star: (**)
			ch = scanner.read();
			nRead++;
			if (ch == '*') {
				ch = scanner.read();
				nRead++;
				// at least a second star
				if (ch == '*') {
					// then, as many stars as wanted (possibly zero)
					do {
						ch = scanner.read();
						nRead++;
					} while (ch == '*');

					// and finally, a closing parenthesis
					if (ch == ')')
						return this.token;
				}
			}
		}

		// put back the unmatched characters
		for (int i = 0; i < nRead; i++)
			scanner.unread();

		return Token.UNDEFINED;
	}

	public IToken getSuccessToken() {
		return this.token;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}
}
