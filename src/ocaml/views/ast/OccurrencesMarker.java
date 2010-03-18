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
		      marker.setAttribute(IMarker.MESSAGE, "");
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
}
