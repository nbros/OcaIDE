package ocaml.editors.lex;

import java.util.ArrayList;

import ocaml.editor.syntaxcoloring.CharacterRule;
import ocaml.editor.syntaxcoloring.OcamlEditorColors;
import ocaml.editor.syntaxcoloring.OcamlNumberRule;
import ocaml.editor.syntaxcoloring.PunctuationRule;
import ocaml.editor.syntaxcoloring.SimpleWordRule;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.SWT;

/** Define rules for the syntax coloring of OCaml Lex files */
public class OcamllexRuleScanner extends RuleBasedScanner implements ILexLanguageWords {
	public OcamllexRuleScanner() {

		int styleKeyword = ocaml.OcamlPlugin.getKeywordIsBold() ? SWT.BOLD : SWT.NONE;
		int styleCharacter = ocaml.OcamlPlugin.getCharacterIsBold() ? SWT.BOLD : SWT.NONE;
		int styleNumber = ocaml.OcamlPlugin.getNumberIsBold() ? SWT.BOLD : SWT.NONE;

		IToken keyword = new Token(new TextAttribute(OcamlEditorColors.getKeywordColor(), null,
				styleKeyword));
		IToken letin = new Token(new TextAttribute(OcamlEditorColors.getLetInColor(), null,
				styleKeyword));
		IToken fun = new Token(new TextAttribute(OcamlEditorColors.getFunColor(), null,
				styleKeyword));

		IToken character = new Token(new TextAttribute(OcamlEditorColors.getCharacterColor(), null,
				styleCharacter));
		IToken integer = new Token(new TextAttribute(OcamlEditorColors.getIntegerColor(), null,
				styleNumber));

		IToken decimal = new Token(new TextAttribute(OcamlEditorColors.getDecimalColor(), null,
				styleNumber));

		IToken punctuation =
			new Token(new TextAttribute(OcamlEditorColors.getPunctuationColor()));

		java.util.List<IRule> rules = new ArrayList<IRule>();

		rules.add(new CharacterRule(character));
		rules.add(new OcamlNumberRule(integer, decimal));
		rules.add(new PunctuationRule(punctuation));
		rules.add(new SimpleWordRule(keywords, keyword, letin, fun));

		this.setRules(rules.toArray(new IRule[] {}));
	}
}
