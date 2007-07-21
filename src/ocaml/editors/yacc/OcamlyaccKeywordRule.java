package ocaml.editors.yacc;

import java.util.HashSet;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** A rule to match O'Caml Yacc keywords like "%token" */
public class OcamlyaccKeywordRule implements IRule {
	/** The hash set that contains all the keywords */
	HashSet<String> words;
	/** The token we return if the rule matches on the input */
	IToken token;
	StringBuffer stringBuffer = new StringBuffer(30);

	public OcamlyaccKeywordRule(String[] keywords, IToken ok) {
		this.token = ok;
		int size = keywords.length;
		this.words = new HashSet<String>(size * 2);
		for (String keyword : keywords)
			this.words.add(keyword);
	}

	/** Return the first token matching the input */
	public IToken evaluate(ICharacterScanner scanner) {
		int ch;

		ch = scanner.read();
		if (ch == '%') {
			this.stringBuffer.setLength(0);
			this.stringBuffer.append((char) ch);
			for (;;) {
				ch = scanner.read();
				if (ch == ICharacterScanner.EOF || !Character.isLetter((char) ch))
					break;
				this.stringBuffer.append((char) ch);
			}
			scanner.unread();
			String word = this.stringBuffer.toString();

			if (this.words.contains(word))
				return this.token;

			if (word.length() > 0)
				scanner.unread();

		} else
			scanner.unread();

		return Token.UNDEFINED;

	}
}
