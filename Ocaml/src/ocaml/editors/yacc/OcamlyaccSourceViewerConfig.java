package ocaml.editors.yacc;

import ocaml.editor.completion.OcamlCompletionProcessor;
import ocaml.editor.completion.OcamlInformationControlCreator;
import ocaml.editor.syntaxcoloring.OcamlEditorColors;
import ocaml.editors.OcamlAutoEditStrategy;
import ocaml.editors.OcamlDoubleClickStrategy;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;

/**
 * Configures the OCaml Yacc editor with a double-click strategy, auto-edit strategies, a document
 * partitioning, completion, a hyper-link detector, and syntax coloring.
 */
public class OcamlyaccSourceViewerConfig extends SourceViewerConfiguration {
	private OcamlyaccEditor ocamlyaccEditor;

	public OcamlyaccSourceViewerConfig(OcamlyaccEditor ocamlyaccEditor) {
		this.ocamlyaccEditor = ocamlyaccEditor;
	}

	/**
	 * Returns the double-click strategy
	 */
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new OcamlDoubleClickStrategy();
	}

	/**
	 * Returns the auto-edit strategies
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new OcamlAutoEditStrategy[] { new OcamlAutoEditStrategy() };
	}

	/** Get the name of the partitioning */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return OcamlyaccPartitionScanner.OCAMLYACC_PARTITIONING;
	}

	/** Get the names of the different partitions defined for this editor */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, OcamlyaccPartitionScanner.OCAMLYACC_STRING,
				OcamlyaccPartitionScanner.OCAMLYACC_CSTYLE_MULTILINE_COMMENT,
				OcamlyaccPartitionScanner.OCAMLYACC_MULTILINE_COMMENT };
	}

	/**
	 * "Reconciler" for the OCaml Yacc editor
	 */
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		int styleComment = ocaml.OcamlPlugin.getCommentIsBold() ? SWT.BOLD : SWT.NONE;
		int styleString = ocaml.OcamlPlugin.getStringIsBold() ? SWT.BOLD : SWT.NONE;

		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		// a damager-repairer for strings
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(
				OcamlEditorColors.getStringColor(), null, styleString)));
		reconciler.setDamager(dr, OcamlyaccPartitionScanner.OCAMLYACC_STRING);
		reconciler.setRepairer(dr, OcamlyaccPartitionScanner.OCAMLYACC_STRING);

		// a damager-repairer for comments
		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(OcamlEditorColors
				.getCommentColor(), null, styleComment)));
		reconciler.setDamager(dr, OcamlyaccPartitionScanner.OCAMLYACC_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, OcamlyaccPartitionScanner.OCAMLYACC_MULTILINE_COMMENT);

		// a damager-repairer for "C-like" comments
		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(OcamlEditorColors
				.getCommentColor(), null, styleComment)));
		reconciler.setDamager(dr, OcamlyaccPartitionScanner.OCAMLYACC_CSTYLE_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, OcamlyaccPartitionScanner.OCAMLYACC_CSTYLE_MULTILINE_COMMENT);

		// a damager-repairer for everything else
		dr = new DefaultDamagerRepairer(new OcamlyaccRuleScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		assistant.setContentAssistProcessor(new OcamlCompletionProcessor(ocamlyaccEditor,
				IDocument.DEFAULT_CONTENT_TYPE), IDocument.DEFAULT_CONTENT_TYPE);

		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(100);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(new OcamlInformationControlCreator());
		return assistant;
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] { new OcamlyaccHyperlinkDetector(this.ocamlyaccEditor) };
	}

}

class SingleTokenScanner extends BufferedRuleBasedScanner {
	public SingleTokenScanner(TextAttribute attribute) {
		setDefaultReturnToken(new Token(attribute));
	}
}
