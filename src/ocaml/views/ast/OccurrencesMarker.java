package ocaml.views.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;


public class OccurrencesMarker {


	public OccurrencesMarker() {
		
	}
		
	public IMarker createOccurrencesMarker(IFile resource) {
		   try {
		      IMarker marker = resource.createMarker("Ocaml.ocamlOccurrencesMarker");
		      return marker;
		   } catch (CoreException e) {
			   e.printStackTrace();
		   }
		return null;
	}
	
	public IMarker createWriteOccurrencesMarker(IFile resource) {
		   try {
		      IMarker marker = resource.createMarker("Ocaml.ocamlWriteOccurrencesMarker");
		      return marker;
		   } catch (CoreException e) {
			   e.printStackTrace();
		   }
		return null;
	}
	
	public void manipulateMarker(IMarker marker, int start, int end) {
		   if (!marker.exists())
		      return;
		   try {
		      //marker.setAttribute(IMarker.MESSAGE, "");
		      marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		      marker.setAttribute(IMarker.CHAR_START, start);
		      marker.setAttribute(IMarker.CHAR_END, end);
		   } catch (CoreException e) {
			   e.printStackTrace();
		   }
	}
	
		
	public void deleteMarkers(IResource target) {
		try {
			target.deleteMarkers("Ocaml.ocamlOccurrencesMarker", false, IResource.DEPTH_ZERO);
			target.deleteMarkers("Ocaml.ocamlWriteOccurrencesMarker", false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	/*
	
	class OccurrencesFinderJob extends Job {

		private final IDocument fDocument;
		private final ISelection fSelection;
		private final ISelectionValidator fPostSelectionValidator;
		private boolean fCanceled= false;
		private final OccurrenceLocation[] fLocations;

		public OccurrencesFinderJob(IDocument document, OccurrenceLocation[] locations, ISelection selection) {
			//super(JavaEditorMessages.JavaEditor_markOccurrences_job_name);
			fDocument= document;
			fSelection= selection;
			fLocations= locations;

			if (getSelectionProvider() instanceof ISelectionValidator)
				fPostSelectionValidator= (ISelectionValidator)getSelectionProvider();
			else
				fPostSelectionValidator= null;
		}

		// cannot use cancel() because it is declared final
		void doCancel() {
			fCanceled= true;
			cancel();
		}

		private boolean isCanceled(IProgressMonitor progressMonitor) {
			return fCanceled || progressMonitor.isCanceled()
				||  fPostSelectionValidator != null && !(fPostSelectionValidator.isValid(fSelection) || fForcedMarkOccurrencesSelection == fSelection)
				|| LinkedModeModel.hasInstalledModel(fDocument);
		}

		/*
		 * @see Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
	/*	public IStatus run(IProgressMonitor progressMonitor) {
			if (isCanceled(progressMonitor))
				return Status.CANCEL_STATUS;

			ITextViewer textViewer= getViewer();
			if (textViewer == null)
				return Status.CANCEL_STATUS;

			IDocument document= textViewer.getDocument();
			if (document == null)
				return Status.CANCEL_STATUS;

			IDocumentProvider documentProvider= getDocumentProvider();
			if (documentProvider == null)
				return Status.CANCEL_STATUS;

			IAnnotationModel annotationModel= documentProvider.getAnnotationModel(getEditorInput());
			if (annotationModel == null)
				return Status.CANCEL_STATUS;

			// Add occurrence annotations
			int length= fLocations.length;
			Map annotationMap= new HashMap(length);
			for (int i= 0; i < length; i++) {

				if (isCanceled(progressMonitor))
					return Status.CANCEL_STATUS;

				OccurrenceLocation location= fLocations[i];
				Position position= new Position(location.getOffset(), location.getLength());

				String description= location.getDescription();
				String annotationType= (location.getFlags() == IOccurrencesFinder.F_WRITE_OCCURRENCE) ? "org.eclipse.jdt.ui.occurrences.write" : "org.eclipse.jdt.ui.occurrences"; //$NON-NLS-1$ //$NON-NLS-2$

				annotationMap.put(new Annotation(annotationType, false, description), position);
			}

			if (isCanceled(progressMonitor))
				return Status.CANCEL_STATUS;

			synchronized (getLockObject(annotationModel)) {
				if (annotationModel instanceof IAnnotationModelExtension) {
					((IAnnotationModelExtension)annotationModel).replaceAnnotations(fOccurrenceAnnotations, annotationMap);
				} else {
					removeOccurrenceAnnotations();
					Iterator iter= annotationMap.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry mapEntry= (Map.Entry)iter.next();
						annotationModel.addAnnotation((Annotation)mapEntry.getKey(), (Position)mapEntry.getValue());
					}
				}
				fOccurrenceAnnotations= (Annotation[])annotationMap.keySet().toArray(new Annotation[annotationMap.keySet().size()]);
			}

			return Status.OK_STATUS;
		}
	}
	
	
	protected void installOccurrencesFinder(boolean forceUpdate) {
		fMarkOccurrenceAnnotations= true;

		fPostSelectionListenerWithAST= new ISelectionListenerWithAST() {
			public void selectionChanged(IEditorPart part, ITextSelection selection, CompilationUnit astRoot) {
				updateOccurrenceAnnotations(selection, astRoot);
			}
		};
		SelectionListenerWithASTManager.getDefault().addListener(this, fPostSelectionListenerWithAST);
		if (forceUpdate && getSelectionProvider() != null) {
			fForcedMarkOccurrencesSelection= getSelectionProvider().getSelection();
			ITypeRoot inputJavaElement= getInputJavaElement();
			if (inputJavaElement != null)
				updateOccurrenceAnnotations((ITextSelection)fForcedMarkOccurrencesSelection, SharedASTProvider.getAST(inputJavaElement, SharedASTProvider.WAIT_NO, getProgressMonitor()));
		}

		if (fOccurrencesFinderJobCanceler == null) {
			fOccurrencesFinderJobCanceler= new OccurrencesFinderJobCanceler();
			fOccurrencesFinderJobCanceler.install();
		}
	}

	protected void uninstallOccurrencesFinder() {
		fMarkOccurrenceAnnotations= false;

		if (fOccurrencesFinderJob != null) {
			fOccurrencesFinderJob.cancel();
			fOccurrencesFinderJob= null;
		}

		if (fOccurrencesFinderJobCanceler != null) {
			fOccurrencesFinderJobCanceler.uninstall();
			fOccurrencesFinderJobCanceler= null;
		}

		if (fPostSelectionListenerWithAST != null) {
			SelectionListenerWithASTManager.getDefault().removeListener(this, fPostSelectionListenerWithAST);
			fPostSelectionListenerWithAST= null;
		}

		removeOccurrenceAnnotations();
	}*/
}
