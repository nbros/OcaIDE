package ocaml.editor.syntaxcoloring;

import java.util.HashSet;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches words from the O'Caml keywords list */
public final class SimpleWordRule implements IRule {
	/** The hash set of all words this rule must recognize */
	HashSet<String> words;

	/** The token we return if the rule matched on the input */
	IToken token;

	StringBuffer stringBuffer = new StringBuffer(30);

	public SimpleWordRule(String[] keywords, IToken ok) {
		this.token = ok;
		int size = keywords.length;
		this.words = new HashSet<String>(size * 2);
		for (String keyword : keywords)
			this.words.add(keyword);
	}

	/** Return the first token that matches on the input */
	public IToken evaluate(ICharacterScanner scanner) {
		// we read one character backwards to make sure we are at the beginning of a word
		int ch;
		try {
			scanner.unread();
			ch = scanner.read();
		} catch (Throwable t) {
			ch = -1;
		}
		/*
		 * the character before the word musn't be an identifier part, or else we are not at the beginning of
		 * a word
		 */
		if (ch <= 0 || !Character.isJavaIdentifierPart((char) ch)) {
			ch = scanner.read();
			if (Character.isJavaIdentifierStart((char) ch)) {
				this.stringBuffer.setLength(0);
				this.stringBuffer.append((char) ch);
				for (;;) {
					ch = scanner.read();
					if (ch == ICharacterScanner.EOF || !Character.isJavaIdentifierPart((char) ch))
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
		}
		return Token.UNDEFINED;
	}
}