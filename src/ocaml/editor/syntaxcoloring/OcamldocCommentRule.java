package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches ocamldoc comments : (**......*)  */
public class OcamldocCommentRule implements IPredicateRule {

	/** The token we return if the rule matched on the input */
	private final IToken token;

	public OcamldocCommentRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner, boolean resume) {

		
		// must start by '(' unless we are resuming
		int ch = scanner.read();
		int nRead = 1;

		if (ch == '(') {
			// at least a first star (**)
			ch = scanner.read();
			nRead++;
			if (ch == '*') {
				ch = scanner.read();
				nRead++;
				// at least a second star
				if (ch == '*') {
					// anything but a star
					ch = scanner.read();
					nRead++;
					
					if(ch != '*')
					{
						boolean bStar = false;
						int codeNestingLevel = 0;
						boolean bEscapeNextChar = false;
						
						while(true){
							ch = scanner.read();
							nRead++;
							
							// end of file
							if(ch == -1)
								return token;
							
							if(ch == '[' && !bEscapeNextChar)
								codeNestingLevel ++;
							else if(ch == ']' && !bEscapeNextChar)
								codeNestingLevel --;
							
							bEscapeNextChar = false;
							
							if(ch == '*' && codeNestingLevel == 0)
								bStar = true;
							else if(ch == '\\')
								bEscapeNextChar = true;
							else if(ch == ')' && bStar)
								return token;
							else
								bStar = false;
								
						} 
						
					}
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
