package ocaml.views.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * This is a listener that gets notified of "post build" events, so as to trigger a rebuilding of
 * the outline to update it with the types inferred by the OCaml compiler
 */
public class OutlineBuildListener implements IResourceChangeListener {

	boolean bAnnotModified = false;

	public void resourceChanged(IResourceChangeEvent event) {

		if (!OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
				PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE))
			return;

		if (event.getType() == IResourceChangeEvent.POST_BUILD) {

			bAnnotModified = false;

			IResourceDelta delta = event.getDelta();
			try {
				delta.accept(new IResourceDeltaVisitor() {

					public boolean visit(IResourceDelta delta) {

						final IResource resource = delta.getResource();

						if (resource == null)
							return true;

						if (resource.getType() != IResource.FILE)
							return true;

						IPath path = resource.getLocation();

						if (path == null)
							return true;

						if ("annot".equals(path.getFileExtension())) {
							bAnnotModified = true;
							return false;
						}

						return true;
					}
				});
			} catch (CoreException e) {
				OcamlPlugin.logError("outline post build listener", e);
				return;
			}

			if (!bAnnotModified)
				return;

			/*
			 * try {
			 * 
			 * IResourceDelta delta = event.getDelta(); delta. if (delta == null) return;
			 * 
			 * IPath path = delta.getResource().getLocation(); if (path == null) return;
			 * 
			 * if(!"annot".equals(path.getFileExtension())) return; } catch (Exception e) {
			 * OcamlPlugin.logError("post build", e); return; }
			 */

			// rebuild the outline to update it with the types inferred by the compilation
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					try {
						IWorkbench workbench = PlatformUI.getWorkbench();
						IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
						if (workbenchWindow == null)
							return;
						IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
						if (workbenchPage == null)
							return;
						IEditorPart editorPart = workbenchPage.getActiveEditor();
						if (editorPart instanceof OcamlEditor)
							((OcamlEditor) editorPart).rebuildOutline(500);
					} catch (Throwable e) {
						OcamlPlugin.logError("rebuilding outline after build", e);
					}
				}
			});
		}
	}
}
