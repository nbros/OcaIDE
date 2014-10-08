package ocaml.editors;

import java.util.Iterator;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.OcamlCompletionProcessor;
import ocaml.editor.completion.OcamlInformationControlCreator;
import ocaml.editor.formatting.OcamlFormattingStrategy;
import ocaml.editor.syntaxcoloring.DocumentAnnotationRule;
import ocaml.editor.syntaxcoloring.OcamlEditorColors;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.editor.syntaxcoloring.OcamlRuleScanner;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.formatter.MultiPassContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/**
 * Configures the OCaml code editor: auto edit strategies, formatter, partitioning, completion assistant,
 * hyper-link detector, ...
 */
public class OcamlSourceViewerConfig extends SourceViewerConfiguration {
	private OcamlEditor ocamlEditor;
	private boolean contentAssistantActived;

	public OcamlSourceViewerConfig(OcamlEditor ocamlEditor) {
		this.ocamlEditor = ocamlEditor;
		this.contentAssistantActived = false;
	}

	/**
	 * Returns the double-click strategy.
	 * 
	 * @see OcamlDoubleClickStrategy
	 */
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new OcamlDoubleClickStrategy();
	}

	/**
	 * Returns auto-edit strategies.
	 * 
	 * @see OcamlAutoEditStrategy
	 */
	@Override
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		return new OcamlAutoEditStrategy[] { new OcamlAutoEditStrategy() };
	}

	/** Add a content formatter to the editor */
	@Override
	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {

		MultiPassContentFormatter formatter = new MultiPassContentFormatter(
				getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE);
		formatter.setMasterStrategy(new OcamlFormattingStrategy());
		return formatter;
	}

	/** Return the name of the configured partitioning */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return OcamlPartitionScanner.OCAML_PARTITIONING;
	}

	/** Return the list of partitions defined for this editor */
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, OcamlPartitionScanner.OCAML_STRING,
				OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT,
				OcamlPartitionScanner.OCAML_MULTILINE_COMMENT };
	}

	/**
	 * Return the "Reconciler" for the OCaml editor (see Eclipse documentation)
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
		reconciler.setDamager(dr, OcamlPartitionScanner.OCAML_STRING);
		reconciler.setRepairer(dr, OcamlPartitionScanner.OCAML_STRING);

		// a damager-repairer for doc comments and doc annotations
		RuleBasedScanner scannerAnnot = new RuleBasedScanner();
		IToken tokenAnnot = new Token(new TextAttribute(OcamlEditorColors.getDocAnnotationColor(), null,
				styleComment));

		IToken tokenDocComment = new Token(new TextAttribute(OcamlEditorColors.getDocCommentColor(), null,
				styleComment));
		scannerAnnot.setRules(new IRule[] { new DocumentAnnotationRule(tokenAnnot, tokenDocComment) });
		dr = new DefaultDamagerRepairer(scannerAnnot);

		reconciler.setDamager(dr, OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT);
		reconciler.setRepairer(dr, OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT);

		// a damager-repairer for comments
		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(OcamlEditorColors
				.getCommentColor(), null, styleComment)));
		reconciler.setDamager(dr, OcamlPartitionScanner.OCAML_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, OcamlPartitionScanner.OCAML_MULTILINE_COMMENT);

		// a damager-repairer for everything else
		dr = new DefaultDamagerRepairer(new OcamlRuleScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	@Override
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		assistant.enablePrefixCompletion(true);
		assistant.setContentAssistProcessor(new OcamlCompletionProcessor(this.ocamlEditor,
				OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT),
				OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT);
		assistant.setContentAssistProcessor(new OcamlCompletionProcessor(this.ocamlEditor,
				IDocument.DEFAULT_CONTENT_TYPE), IDocument.DEFAULT_CONTENT_TYPE);

		assistant.addCompletionListener(new ICompletionListener() {
			@Override
			public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
				contentAssistantActived = true;
			}
			@Override
			public void assistSessionStarted(ContentAssistEvent event) {
				contentAssistantActived = true;
			}
			@Override
			public void assistSessionEnded(ContentAssistEvent event) {
				contentAssistantActived = false;
			}
		});
		
		assistant.setSorter(new ICompletionProposalSorter() {
			@Override
			public int compare(ICompletionProposal p1, ICompletionProposal p2) {
				return p1.getDisplayString().compareTo(p2.getDisplayString());
			}
		});

		boolean autoActivation = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_EDITOR_AUTOCOMPLETION);
		assistant.enableAutoActivation(autoActivation);
		assistant.setAutoActivationDelay(100);
		assistant.enableAutoInsert(true);
		assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(new OcamlInformationControlCreator());
		return assistant;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new OcamlTextHover(this.ocamlEditor);
	}

	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new OcamlAnnotationHover();
	}

	@Override
	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		return new IHyperlinkDetector[] { new OcamlHyperlinkDetector(this.ocamlEditor) };
	}

	@Override
	public IReconciler getReconciler(final ISourceViewer sourceViewer) {

		/*
		 * IPreferenceStore preferenceStore = EditorsPlugin.getDefault().getPreferenceStore();
		 * 
		 * if (preferenceStore == null ||
		 * !preferenceStore.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED)) return null;
		 */

		SpellingService spellingService = EditorsUI.getSpellingService();
		if (spellingService.getDefaultSpellingEngineDescriptor() == null)
			return null;

		IReconcilingStrategy strategy = new OcamlSpellingReconcileStrategy(sourceViewer, spellingService);
		final Reconciler reconciler = new Reconciler();
		reconciler.setDocumentPartitioning(OcamlPartitionScanner.OCAML_PARTITIONING);
		reconciler.setReconcilingStrategy(strategy, OcamlPartitionScanner.OCAML_MULTILINE_COMMENT);
		reconciler.setReconcilingStrategy(strategy, OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT);
		reconciler.setIsIncrementalReconciler(false);
		reconciler.setProgressMonitor(new NullProgressMonitor());
		reconciler.setDelay(500);

		OcamlPlugin.getInstance().getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty().equals(PreferenceConstants.P_EDITOR_SPELL_CHECKING)) {
							if (!event.getNewValue().equals(event.getOldValue())) {
								if (event.getNewValue().equals(Boolean.TRUE))
									reconciler.install(sourceViewer);
								else {
									reconciler.uninstall();

									IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
									if (annotationModel == null)
										return;
									@SuppressWarnings("unchecked")
									Iterator<Annotation> annotationIterator = annotationModel
											.getAnnotationIterator();

									while (annotationIterator.hasNext()) {
										Annotation annotation = annotationIterator.next();
										if (annotation.getType().equals(SpellingAnnotation.TYPE))
											annotationModel.removeAnnotation(annotation);
									}
								}

								//sourceViewer.invalidateTextPresentation();
							}
						}
					}

				});

		if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_EDITOR_SPELL_CHECKING))
			return reconciler;
		else
			return null;
	}
	
	public boolean isContentAssistantActive() {
		return contentAssistantActived;
	}

}

class SingleTokenScanner extends BufferedRuleBasedScanner {
	public SingleTokenScanner(TextAttribute attribute) {
		setDefaultReturnToken(new Token(attribute));
	}
}
