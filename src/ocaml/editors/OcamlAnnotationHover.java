package ocaml.editors;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

/** Display a pop-up when the mouse hovers over an annotation in the margin of the O'Caml editor */
public class OcamlAnnotationHover implements IAnnotationHover {

	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		try {
			IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (workbenchWindow != null) {
				IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
				if (workbenchPage != null) {
					IEditorPart editorPart = workbenchPage.getActiveEditor();
					if (editorPart != null) {
						IEditorInput editorInput = editorPart.getEditorInput();
						if (editorInput instanceof FileEditorInput) {
							FileEditorInput fileEditorInput = (FileEditorInput) editorInput;

							IFile file = fileEditorInput.getFile();
							IMarker[] markers = file.findMarkers(null, true, 1);

							String message = "";

							for (IMarker marker : markers) {
								int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
								if (line - 1 == lineNumber) {
									String text =
											marker.getAttribute(IMarker.MESSAGE,
												"<Error: cannot read marker message>");
									if (!message.equals(""))
										message = message + "\n-----\n";
									message = message + text;
								}

							}
							
							return message;

						} else
							OcamlPlugin.logError("editorInput is not instanceof FileEditorInput");
					} else
						OcamlPlugin.logError("OcamlAnnotationHover: no active editor");
				} else
					OcamlPlugin.logError("OcamlAnnotationHover: no active workbench page");
			} else
				OcamlPlugin.logError("OcamlAnnotationHover: no active workbench window");

		} catch (Throwable e) {
			ocaml.OcamlPlugin.logError("Erreur dans OcamlTextHover:getHoverInfo", e);
		}
		return "";
	}

}
