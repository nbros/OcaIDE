package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches on strings, and take embedded \" and '"' into account */
public class OcamlStringRule implements IPredicateRule {

	/** The token we return if the rule matched on the input */
	private final IToken token;

	public OcamlStringRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {

		boolean bEscape = false;

		int before = ' ';

		if (scanner.getColumn() > 0) {
			scanner.unread();
			before = scanner.read();
		}
		if (before == '\'' || before == '\\') {
			// scanner.read();
			return Token.UNDEFINED;
		}

		int ch = scanner.read();

		if (ch == '"') {

			while (true) {
				ch = scanner.read();
				if (ch == ICharacterScanner.EOF)
					return token;

				if (ch == '"' && !bEscape)
					return token;

				if (ch == '\\')
					bEscape = !bEscape;
				else
					bEscape = false;
			}
		}

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
