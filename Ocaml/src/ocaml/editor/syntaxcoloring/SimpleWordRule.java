package ocaml.editor.syntaxcoloring;

import java.util.HashSet;

import ocaml.util.Misc;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches words from the O'Caml keywords list */
public final class SimpleWordRule implements IRule {
	/** The hash set of all words this rule must recognize */
	HashSet<String> words;

	/** The token we return if the rule matched on the input */
	IToken keyword;
	/** The token for 'let' or 'in' keywords */
	IToken letin;
	/** The token for 'fun' or 'function' keywords */
	IToken fun;

	StringBuffer stringBuffer = new StringBuffer(30);

	public SimpleWordRule(String[] keywords, IToken keyword, IToken letin, IToken fun) {
		this.keyword = keyword;
		this.letin = letin;
		this.fun = fun;

		int size = keywords.length;
		this.words = new HashSet<String>(size * 2);
		for (String k : keywords)
			this.words.add(k);
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
		if (ch <= 0 || !Misc.isOcamlIdentifierChar((char) ch)) {
			ch = scanner.read();
			if (Misc.isOcamlIdentifierChar((char) ch)) {
				this.stringBuffer.setLength(0);
				this.stringBuffer.append((char) ch);
				for (;;) {
					ch = scanner.read();
					if (ch == ICharacterScanner.EOF || !Misc.isOcamlIdentifierChar((char) ch))
						break;
					this.stringBuffer.append((char) ch);
				}
				scanner.unread();
				String word = this.stringBuffer.toString();

				if (this.words.contains(word)) {
					if ("let".equals(word) || "in".equals(word))
						return this.letin;
					if ("fun".equals(word) || "function".equals(word))
						return this.fun;
					else
						return this.keyword;
				}

				if (word.length() > 0)
					scanner.unread();

			} else
				scanner.unread();
		}
		return Token.UNDEFINED;
	}
}
