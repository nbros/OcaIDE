package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** A rule to match annotations (ex: "@param") in ocamldoc comments */
public class DocumentAnnotationRule  implements IRule {
	
	/** The token we return if the rule matched on the input */
	private final IToken tokenAnnot;
	/** The token we return if the rule didn't match on the input */
	private final IToken tokenDocComment;
	
	public DocumentAnnotationRule (IToken tokenAnnot, IToken tokenDocComment) {
		this.tokenAnnot = tokenAnnot;
		this.tokenDocComment = tokenDocComment;
	}

	public IToken evaluate(ICharacterScanner scanner) {

		int ch = ' ';
		
		if(scanner.getColumn() > 1)
		{
			scanner.unread();
			ch = scanner.read();
			// escaping of the "@"
			if(ch == '\\')
			{
				scanner.read();
				return tokenDocComment;
			}
		}
			
		
		ch = scanner.read();
		int nRead = 1;
		
		if(ch == '@')
		{
			ch = scanner.read();
			nRead++;
			
			if(ch == -1)
				return Token.UNDEFINED;
			if(!Character.isLetter(ch))
				return tokenDocComment;
			
			while(Character.isLetter(ch))
			{
				ch = scanner.read();
				nRead++;
			}

			return tokenAnnot;
		}
		
		if(ch != -1)
			return tokenDocComment;
		else
			return Token.UNDEFINED;
	}
}
