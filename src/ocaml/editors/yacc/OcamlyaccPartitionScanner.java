package ocaml.editors.yacc;

import ocaml.editor.syntaxcoloring.OcamlStringRule;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class OcamlyaccPartitionScanner extends RuleBasedPartitionScanner {
	public final static String OCAMLYACC_PARTITIONING = "__ocamlyacc_partitioning"; //$NON-NLS-1$

	public final static String OCAMLYACC_MULTILINE_COMMENT = "__ocamlyacc_multiline_comment"; //$NON-NLS-1$

	public final static String OCAMLYACC_CSTYLE_MULTILINE_COMMENT = "__ocamlyacc_cstyle_mutliline_comment"; //$NON-NLS-1$

	public final static String OCAMLYACC_STRING = "__ocamlyacc_string"; //$NON-NLS-1$

	public final static String[] OCAMLYACC_PARTITION_TYPES = new String[] { OCAMLYACC_MULTILINE_COMMENT,
			OCAMLYACC_CSTYLE_MULTILINE_COMMENT, OCAMLYACC_STRING };

	/**
	 * Create the partition and configures it with partitioning rules
	 */
	public OcamlyaccPartitionScanner() {
		super();

		IToken cstyleCommentToken = new Token(OCAMLYACC_CSTYLE_MULTILINE_COMMENT);
		IToken commentToken = new Token(OCAMLYACC_MULTILINE_COMMENT);
		IToken stringToken = new Token(OCAMLYACC_STRING);

		// a multi-line comment
		IPredicateRule commentRule = new MultiLineRule("(*", "*)", commentToken, (char) 0, true); //$NON-NLS-1$ //$NON-NLS-2$
		// a multi-line C comment
		IPredicateRule cstyleCommentRule = new MultiLineRule("/*", "*/", cstyleCommentToken, (char) 0, true); //$NON-NLS-1$ //$NON-NLS-2$

		// a multi-line string (can have embedded \" )
		IPredicateRule stringRule = new OcamlStringRule(stringToken);

		setPredicateRules(new IPredicateRule[] { cstyleCommentRule, commentRule, stringRule });
	}

}
