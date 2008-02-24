package ocaml.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;

/* 
 * This class is copied from SpellingReconcileStrategy.
 * FIXME Replace this class by eclipse implementation once it works with regions. 
 */
@SuppressWarnings("unchecked")
/**
 * Reconcile strategy used for spell checking.
 * 
 * @since 3.3
 */
public class OcamlSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * Spelling problem collector.
	 */
	private class SpellingProblemCollector implements ISpellingProblemCollector {

		/** Annotation model. */
		private IAnnotationModel fAnnotationModel;

		/** Annotations to add. */
		private Map fAddAnnotations;

		/** Lock object for modifying the annotations. */
		private Object fLockObject;

		/**
		 * Initializes this collector with the given annotation model.
		 * 
		 * @param annotationModel
		 *            the annotation model
		 */
		public SpellingProblemCollector(IAnnotationModel annotationModel) {
			Assert.isLegal(annotationModel != null);
			fAnnotationModel = annotationModel;
			if (fAnnotationModel instanceof ISynchronizable)
				fLockObject = ((ISynchronizable) fAnnotationModel).getLockObject();
			else
				fLockObject = fAnnotationModel;
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
		 */
		public void accept(SpellingProblem problem) {
			try {
				IDocumentExtension3 extension = (IDocumentExtension3) fDocument;
				ITypedRegion partition = extension.getPartition(OcamlPartitionScanner.OCAML_PARTITIONING,
						problem.getOffset(), false);

				String partitionType = partition.getType();
				if (OcamlPartitionScanner.OCAML_MULTILINE_COMMENT.equals(partitionType)
						|| OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT.equals(partitionType))
					fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(),
							problem.getLength()));
			} catch (Throwable e) {
				OcamlPlugin.logError("Error while adding spelling problem", e);
			}
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
		 */
		public void beginCollecting() {
			fAddAnnotations = new HashMap();
		}

		/*
		 * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
		 */
		public void endCollecting() {

			List toRemove = new ArrayList();

			synchronized (fLockObject) {
				Iterator iter = fAnnotationModel.getAnnotationIterator();
				while (iter.hasNext())
					toRemove.add(iter.next());
				Annotation[] annotationsToRemove = (Annotation[]) toRemove.toArray(new Annotation[toRemove
						.size()]);

				if (fAnnotationModel instanceof IAnnotationModelExtension)
					((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(annotationsToRemove,
							fAddAnnotations);
				else {
					for (int i = 0; i < annotationsToRemove.length; i++)
						fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
					for (iter = fAddAnnotations.keySet().iterator(); iter.hasNext();) {
						Annotation annotation = (Annotation) iter.next();
						fAnnotationModel
								.addAnnotation(annotation, (Position) fAddAnnotations.get(annotation));
					}
				}
			}

			fAddAnnotations = null;
		}
	}

	/** Text content type */
	private static final IContentType TEXT_CONTENT_TYPE = Platform.getContentTypeManager().getContentType(
			IContentTypeManager.CT_TEXT);

	/** The text editor to operate on. */
	private ISourceViewer fViewer;

	/** The document to operate on. */
	private IDocument fDocument;

	/** The progress monitor. */
	private IProgressMonitor fProgressMonitor;

	private SpellingService fSpellingService;

	private ISpellingProblemCollector fSpellingProblemCollector;

	/** The spelling context containing the Java source content type. */
	private SpellingContext fSpellingContext;

	/**
	 * Creates a new comment reconcile strategy.
	 * 
	 * @param viewer
	 *            the source viewer
	 * @param spellingService
	 *            the spelling service to use
	 */
	public OcamlSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService) {
		Assert.isNotNull(viewer);
		Assert.isNotNull(spellingService);
		fViewer = viewer;
		fSpellingService = spellingService;
		fSpellingContext = new SpellingContext();
		fSpellingContext.setContentType(getContentType());

	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		reconcile(new Region(0, fDocument.getLength()));
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile(subRegion);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion region) {
		if (getAnnotationModel() == null || fSpellingProblemCollector == null)
			return;

		fSpellingService.check(fDocument, fSpellingContext, fSpellingProblemCollector, fProgressMonitor);
	}

	/**
	 * Returns the content type of the underlying editor input.
	 * 
	 * @return the content type of the underlying editor input or <code>null</code> if none could be
	 *         determined
	 */
	protected IContentType getContentType() {
		return TEXT_CONTENT_TYPE;
	}

	/**
	 * Returns the document which is spell checked.
	 * 
	 * @return the document
	 */
	protected final IDocument getDocument() {
		return fDocument;
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		fDocument = document;
		fSpellingProblemCollector = createSpellingProblemCollector();
	}

	/**
	 * Creates a new spelling problem collector.
	 * 
	 * @return the collector or <code>null</code> if none is available
	 */
	protected ISpellingProblemCollector createSpellingProblemCollector() {
		IAnnotationModel model = getAnnotationModel();
		if (model == null)
			return null;
		return new SpellingProblemCollector(model);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor = monitor;
	}

	/**
	 * Returns the annotation model to be used by this reconcile strategy.
	 * 
	 * @return the annotation model of the underlying editor input or <code>null</code> if none could be
	 *         determined
	 */
	protected IAnnotationModel getAnnotationModel() {
		return fViewer.getAnnotationModel();
	}

}
