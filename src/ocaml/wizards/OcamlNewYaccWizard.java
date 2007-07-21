package ocaml.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

/** A wizard to create new ocamlyacc files (mly) */
public final class OcamlNewYaccWizard extends BasicNewResourceWizard {
	private WizardNewFileCreationPage mainPage;

	@Override
	public void addPages() {
		super.addPages();
		this.mainPage = new WizardNewFileCreationPage("OcamlNewYaccWizardPage1", this.getSelection());
		this.mainPage.setTitle("New Yacc File");
		this.mainPage.setDescription("Create a new Yacc File");
		this.addPage(this.mainPage);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		this.setWindowTitle("New Yacc File");
		this.setNeedsProgressMonitor(true);
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		String iconPath = "icons/";//$NON-NLS-1$
		try {
			URL installURL = OcamlPlugin.getInstallURL();
			URL url = new URL(installURL, iconPath + "caml32x32.gif");//$NON-NLS-1$
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			this.setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	@Override
	public boolean performFinish() {
		if (!this.mainPage.getFileName().toLowerCase().endsWith(".mly"))
			this.mainPage.setFileName(this.mainPage.getFileName() + ".mly");
		IFile file = this.mainPage.createNewFile();
		if (file == null)
			return false;

		this.selectAndReveal(file);
		IWorkbenchWindow dw = this.getWorkbench().getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null)
					IDE.openEditor(page, file, true);
			}
		} catch (PartInitException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
		return true;
	}
}