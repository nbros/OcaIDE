package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches ocaml comments : (*......*) (which can be nested) */
public class OcamlCommentRule implements IPredicateRule {

	/** The token we return if the rule matched on the input */
	private final IToken token;

	public OcamlCommentRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {

		int nestingLevel = 0;

		// must start by '(' unless we are resuming
		int ch = scanner.read();
		int nRead = 1;

		if (ch == '(') {
			// at least a first star (*
			ch = scanner.read();
			nRead++;
			if (ch == '*') {

				nestingLevel = 1;

				boolean bStar = false;
				boolean bPar = false;

				while (ch != -1) {

					ch = scanner.read();
					nRead++;

					if (ch == -1)
						return token;

					if (ch == ')') {
						if (bStar) {
							nestingLevel--;
							if (nestingLevel <= 0)
								return token;
						}
						bStar = false;
						bPar = false;
					}

					if (ch == '*') {
						if (bPar)
							nestingLevel++;
						bStar = true;
					} else
						bStar = false;

					bPar = (ch == '(');

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
