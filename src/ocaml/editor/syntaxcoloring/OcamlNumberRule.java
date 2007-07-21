package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/** Matches O'Caml numbers (either integers or decimals) */
public class OcamlNumberRule  implements IRule {
	
	/** The token we return if the rule matched an integer */
	private final IToken integer;
	/** The token we return if the rule matched a decimal */
	private final IToken decimal;
	
	public OcamlNumberRule(IToken integer, IToken decimal) {
		this.integer = integer;
		this.decimal = decimal;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		
		boolean bFloat = false;

		int ch = scanner.read();
		int nRead = 1;
		
		if(Character.isDigit(ch))
		{
			// remark: this rule is less strict than the O'Caml grammar
			do
			{
				ch = scanner.read();
				nRead++;						
				if(ch == '.')bFloat = true;
			}while("0123456789abcdefABCDEF_xXoObB.".contains(Character.toString((char)ch)));
			
			scanner.unread();

			if(bFloat)
				return this.decimal;
			else
				return this.integer;
		}
		
		scanner.unread();
		
		return Token.UNDEFINED;
	}
	
	


}
