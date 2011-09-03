package ocaml.editor.syntaxcoloring;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * Partitioning of an OCaml module file into 4 partitions:
 * <ul>
 * <li> strings " ... "
 * <li> comments (* ... *)
 * <li> documentation comments (** ... *)
 * <li> everything else
 * </ul>
 */
public class OcamlPartitionScanner extends RuleBasedPartitionScanner {

	public final static String OCAML_PARTITIONING = "__ocaml_partitioning"; //$NON-NLS-1$
	public final static String OCAML_MULTILINE_COMMENT = "__ocaml_multiline_comment"; //$NON-NLS-1$
	public final static String OCAML_DOCUMENTATION_COMMENT = "__ocaml_documentation_comment"; //$NON-NLS-1$
	public final static String OCAML_STRING = "__ocaml_string"; //$NON-NLS-1$
	public final static String[] OCAML_PARTITION_TYPES =
			new String[] { OCAML_DOCUMENTATION_COMMENT, OCAML_MULTILINE_COMMENT, OCAML_STRING };

	/**
	 * Create the partition and configure the partitioning rules
	 */
	public OcamlPartitionScanner() {
		super();

		IToken docCommentToken = new Token(OCAML_DOCUMENTATION_COMMENT);
		IToken commentToken = new Token(OCAML_MULTILINE_COMMENT);
		IToken stringToken = new Token(OCAML_STRING);

		// a documentation comment on several lines
		// TODO: {[ ]} in doc comments (keep as is everything between these delimiters)
		IPredicateRule docCommentRule =
				new OcamldocCommentRule(docCommentToken); 
		// a comment on several lines
		IPredicateRule commentRule = new OcamlCommentRule(commentToken);
		// a string on several lines (can have embedded '\"' (escaped double quote) )
		IPredicateRule stringRule = new OcamlStringRule(stringToken); 
			//new MultiLineRule("\"", "\"", stringToken, '\\', true); //$NON-NLS-1$ //$NON-NLS-2$
		
		IPredicateRule emptyCommentRule = new EmptyCommentRule(commentToken); //$NON-NLS-1$ //$NON-NLS-2$

		setPredicateRules(new IPredicateRule[] { emptyCommentRule, docCommentRule, commentRule, stringRule });
	}
}
