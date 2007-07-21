package ocaml.editors.yacc;

import java.util.ArrayList;

import ocaml.editor.syntaxcoloring.CharacterRule;
import ocaml.editor.syntaxcoloring.ILanguageWords;
import ocaml.editor.syntaxcoloring.OcamlEditorColors;
import ocaml.editor.syntaxcoloring.OcamlNumberRule;
import ocaml.editor.syntaxcoloring.SimpleWordRule;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

/** Configures the rules to recognize the O'Caml Yacc syntax elements to color */
public class OcamlyaccRuleScanner  extends RuleBasedScanner implements ILanguageWords, IYaccKeywords{
	public OcamlyaccRuleScanner() {

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

		IToken definition =
			new Token(new TextAttribute(OcamlEditorColors.getYaccDefinitionColor(), null, SWT.BOLD));

		java.util.List<IRule> rules = new ArrayList<IRule>();
		
		
		rules.add(new CharacterRule(character));
		rules.add(new OcamlNumberRule(integer, decimal));
		rules.add(new OcamlyaccDefinitionRule(definition));
		rules.add(new SimpleWordRule(keywords, keyword));
		rules.add(new OcamlyaccKeywordRule(yacckeywords, keyword));
		
		
		this.setRules(rules.toArray(new IRule[] {}));
	}

}
