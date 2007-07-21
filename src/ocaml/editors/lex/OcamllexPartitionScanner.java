package ocaml.editors.lex;

import ocaml.editor.syntaxcoloring.OcamlStringRule;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/** Configure partitioning in the O'Caml lex editor */
public class OcamllexPartitionScanner extends RuleBasedPartitionScanner {
	public final static String OCAMLLEX_PARTITIONING = "__ocamllex_partitioning"; //$NON-NLS-1$

	public final static String OCAMLLEX_MULTILINE_COMMENT = "__ocamllex_multiline_comment"; //$NON-NLS-1$

	// public final static String OCAMLLEX_CSTYLE_MULTILINE_COMMENT = "__ocamllex_cstyle_mutliline_comment";
	// //$NON-NLS-1$
	public final static String OCAMLLEX_STRING = "__ocamllex_string"; //$NON-NLS-1$

	public final static String[] OCAMLLEX_PARTITION_TYPES = new String[] { OCAMLLEX_MULTILINE_COMMENT,
			OCAMLLEX_STRING };

	/**
	 * Create the partitioning and configure the partitioning rules
	 */
	public OcamllexPartitionScanner() {
		super();

		IToken commentToken = new Token(OCAMLLEX_MULTILINE_COMMENT);
		IToken stringToken = new Token(OCAMLLEX_STRING);

		// a multi-line comment
		IPredicateRule commentRule = new MultiLineRule("(*", "*)", commentToken, (char) 0, true); //$NON-NLS-1$ //$NON-NLS-2$
		// a multi-line string (can have embedded \" )
		IPredicateRule stringRule = new OcamlStringRule(stringToken);

		setPredicateRules(new IPredicateRule[] { commentRule, stringRule });
	}
}
