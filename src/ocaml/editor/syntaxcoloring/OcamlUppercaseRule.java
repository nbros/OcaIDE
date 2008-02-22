package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches on uppercase identifiers */
public class OcamlUppercaseRule implements IPredicateRule {

	private final IToken uppercase;
	private final IToken pointedUppercase;

	public OcamlUppercaseRule(IToken uppercase, IToken pointedUppercase) {
		this.uppercase = uppercase;
		this.pointedUppercase = pointedUppercase;
	}
	
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		
		int ch = scanner.read();
		int nRead = 1;
		
		if(ch >= 'A' && ch <= 'Z'){
			
			while(ch != scanner.EOF){
				ch = scanner.read();
				nRead++;
				
				if(ch == '.'){
					scanner.unread();
					
					return pointedUppercase;
				}

				if(!(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_' || ch == '\''))
				{
					scanner.unread();
					return uppercase;
				}				
			}
		}
			
		
		while(nRead > 0){
			scanner.unread();
			nRead--;
		}
		
		return Token.UNDEFINED;
	}

	public IToken getSuccessToken() {
		return this.uppercase;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		return evaluate(scanner, false);
	}
}
