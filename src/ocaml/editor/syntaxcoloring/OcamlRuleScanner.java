package ocaml.editor.syntaxcoloring;

import java.util.ArrayList;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

/** Configure the lexeme detection rules */ 
public class OcamlRuleScanner extends RuleBasedScanner implements ILanguageWords {
	public OcamlRuleScanner() {

		int styleKeyword = ocaml.OcamlPlugin.getKeywordIsBold() ? SWT.BOLD : SWT.NONE;
		int styleCharacter = ocaml.OcamlPlugin.getCharacterIsBold() ? SWT.BOLD : SWT.NONE;
		int styleNumber = ocaml.OcamlPlugin.getNumberIsBold() ? SWT.BOLD : SWT.NONE;

		IToken keyword =
				new Token(new TextAttribute(OcamlEditorColors.getKeywordColor(), null, styleKeyword));
		IToken character =
				new Token(
						new TextAttribute(OcamlEditorColors.getCharacterColor(), null, styleCharacter));
		IToken integer =
				new Token(new TextAttribute(OcamlEditorColors.getIntegerColor(), null, styleNumber));

		IToken decimal =
			new Token(new TextAttribute(OcamlEditorColors.getDecimalColor(), null, styleNumber));

		java.util.List<IRule> rules = new ArrayList<IRule>();
		
		rules.add(new CharacterRule(character));
		rules.add(new OcamlNumberRule(integer, decimal));
		rules.add(new SimpleWordRule(keywords, keyword));
		
		this.setRules(rules.toArray(new IRule[] {}));
	}
}