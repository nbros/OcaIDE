package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** A rule to match OCaml characters */
public class CharacterRule  implements IRule {
	
	/** The token we return if the rule matched on the input */
	private final IToken token;
	
	public CharacterRule(IToken ok) {
		this.token = ok;
	}

	public IToken evaluate(ICharacterScanner scanner) {

		int ch = scanner.read();
		int nRead = 1;
		
		if(ch == '\'')
		{
			ch = scanner.read();
			nRead++;
			if(ch == '\\')
			{
				ch = scanner.read();
				nRead++;
				
				// a character in the format '\123'
				if(Character.isDigit(ch))
				{
					do 
					{
						ch = scanner.read();
						nRead++;
					}
					while(Character.isDigit(ch));
					
					scanner.unread();
					nRead--;
				}
					
			}
				
			ch = scanner.read();
			nRead++;
			
			if(ch == '\'')
				return this.token;
		}
		// put back the characters that are not matched by this rule 
		for(int i = 0; i<nRead; i++)
			scanner.unread();

		return Token.UNDEFINED;
	}
	
	


}
