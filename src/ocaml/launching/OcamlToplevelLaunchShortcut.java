package ocaml.launching;

import java.util.Random;

import ocaml.OcamlPlugin;
import ocaml.views.toplevel.OcamlCustomToplevelView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class OcamlToplevelLaunchShortcut implements ILaunchShortcut {

	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;

			Object first = sel.getFirstElement();
			if (first != null && first instanceof IFile) {
				IFile file = (IFile) first;
				IPath location = file.getLocation();

				if (location == null) {
					OcamlPlugin.logError("OcamlToplevelLaunchShortcut:launch : location is null");
					return;
				}

				String path = location.toOSString();
				
				try {
					String secondaryId = "ocamlcustomtoplevelview" + new Random().nextInt();
					
					IWorkbenchPage page =
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart viewPart = page.showView(OcamlCustomToplevelView.ID, secondaryId,
						IWorkbenchPage.VIEW_ACTIVATE);
					
					if (viewPart instanceof OcamlCustomToplevelView) {
						OcamlCustomToplevelView view = (OcamlCustomToplevelView) viewPart;
						view.setSecondaryId(secondaryId);
						view.start(path);
					}
				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}


			}
		}	}

	public void launch(IEditorPart editor, String mode) {
		// TODO Auto-generated method stub

	}

}
