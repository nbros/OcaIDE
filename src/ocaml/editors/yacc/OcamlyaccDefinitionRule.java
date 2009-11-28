package ocaml.editors.yacc;

import ocaml.util.Misc;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** This rule matches definitions of the kind : "nonterminal : ..." */
public class OcamlyaccDefinitionRule implements IRule {
	/** The token to return if this rule matches the input */
	IToken token;

	public OcamlyaccDefinitionRule(IToken ok) {
		this.token = ok;
	}

	/** Return the first token corresponding to the input */
	public IToken evaluate(ICharacterScanner scanner) {
		int ch;

		if (scanner.getColumn() != 0) {
			return Token.UNDEFINED;
		}

		int readCount = 0;

		for (;;) {
			ch = scanner.read();
			readCount++;
			if (!Misc.isOcamlIdentifierChar((char) ch))
				break;
		}

		while (ch == ' ') {
			ch = scanner.read();
			readCount++;
		}

		if (ch == ':' && readCount > 1)
			return token;

		for (int i = 0; i < readCount; i++)
			scanner.unread();

		return Token.UNDEFINED;

	}
}
