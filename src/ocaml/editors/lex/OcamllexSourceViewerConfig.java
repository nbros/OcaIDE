package ocaml.editors.lex;

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
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;

/** Configures the O'Caml Lex editor with a double-click strategy, auto-edit strategies, a partitioning, 
 * completion, and syntax coloring. */
public class OcamllexSourceViewerConfig extends SourceViewerConfiguration {
	private OcamllexEditor ocamllexEditor;

	public OcamllexSourceViewerConfig(OcamllexEditor ocamllexEditor) {
		this.ocamllexEditor = ocamllexEditor;
	}

	/**
	 * Returns the double-click strategy
	 */
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer,
			String contentType) {
		return new OcamlDoubleClickStrategy();
	}

	/**
	 * Returns the auto-edit strategies (the same ones as for the O'Caml editor)
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new OcamlAutoEditStrategy[] { new OcamlAutoEditStrategy() };
	}

	/** Get the name of the partitioning  */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return OcamllexPartitionScanner.OCAMLLEX_PARTITIONING;
	}

	/** Get the names of the defined partitions */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
				OcamllexPartitionScanner.OCAMLLEX_STRING,
				OcamllexPartitionScanner.OCAMLLEX_MULTILINE_COMMENT };
	}

	/**
	 * "Reconciler" for the O'Caml Lex editor
	 */
	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		int styleComment = ocaml.OcamlPlugin.getCommentIsBold() ? SWT.BOLD : SWT.NONE;
		int styleString = ocaml.OcamlPlugin.getStringIsBold() ? SWT.BOLD : SWT.NONE;

		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		// a damager-repairer for strings
		DefaultDamagerRepairer dr =
				new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(
					OcamlEditorColors.getStringColor(), null, styleString)));
		reconciler.setDamager(dr, OcamllexPartitionScanner.OCAMLLEX_STRING);
		reconciler.setRepairer(dr, OcamllexPartitionScanner.OCAMLLEX_STRING);

		// a damager-repairer for comments
		dr =
				new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(
					OcamlEditorColors.getCommentColor(), null, styleComment)));
		reconciler.setDamager(dr, OcamllexPartitionScanner.OCAMLLEX_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, OcamllexPartitionScanner.OCAMLLEX_MULTILINE_COMMENT);

		// a damager-repairer for everyting else
		dr = new DefaultDamagerRepairer(new OcamllexRuleScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		assistant.setContentAssistProcessor(new OcamlCompletionProcessor(ocamllexEditor,
			IDocument.DEFAULT_CONTENT_TYPE), IDocument.DEFAULT_CONTENT_TYPE);

		assistant.enableAutoInsert(true);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(100);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(new OcamlInformationControlCreator());
		return assistant;
	}

}

class SingleTokenScanner extends BufferedRuleBasedScanner {
	public SingleTokenScanner(TextAttribute attribute) {
		setDefaultReturnToken(new Token(attribute));
	}
}
