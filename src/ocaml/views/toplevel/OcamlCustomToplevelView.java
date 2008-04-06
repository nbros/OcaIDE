package ocaml.views.toplevel;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/** Extends the O'Caml top-level view to provide custom toplevels */
public class OcamlCustomToplevelView extends OcamlToplevelView {

	public OcamlCustomToplevelView() {
		this.bStartWhenCreated = false;
	}

	public static final String ID = "Ocaml.ocamlCustomToplevelView";

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		this.setPartName("Custom Toplevel");
	}

	public void start(String path) {
		toplevel.setToplevelPath(path);
		toplevel.start();
		registerCloseOnExit();
	}

	public void setSecondaryId(String secondaryId) {
		this.secondaryId = secondaryId;
	}

	/** Add a workbench listener to know when the workbench is closing, and close this view. */
	private void registerCloseOnExit() {
		PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				IWorkbenchWindow[] workbenchWindows = workbench.getWorkbenchWindows();
				for (IWorkbenchWindow workbenchWindow : workbenchWindows) {
					IWorkbenchPage[] pages = workbenchWindow.getPages();
					for (IWorkbenchPage page : pages) {
						IViewReference[] viewReferences = page.getViewReferences();
						for (IViewReference viewReference : viewReferences) {
							if (ID.equals(viewReference.getId())
									&& secondaryId.equals(viewReference.getSecondaryId())) {
								page.hideView(viewReference);
							}
						}
					}
				}
				return true;
			}

			public void postShutdown(IWorkbench workbench) {
			}
		});
	}
}
