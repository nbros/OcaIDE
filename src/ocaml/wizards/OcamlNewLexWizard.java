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

/** A wizard that allows the user to create new ocamlyacc (mly) files */
public final class OcamlNewLexWizard extends BasicNewResourceWizard {
	private WizardNewFileCreationPage mainPage;
	@Override
	public void addPages() {
		super.addPages();
		this.mainPage = new WizardNewFileCreationPage("OcamlNewLexWizardPage1", this
				.getSelection());
		this.mainPage.setTitle("New Lex File");
		this.mainPage.setDescription("Create a new Lex File");
		this.addPage(this.mainPage);
	}
	@Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		this.setWindowTitle("New Lex File");
		this.setNeedsProgressMonitor(true);
	}
	@Override
	protected void initializeDefaultPageImageDescriptor() {
		String iconPath = "icons/";
		try {
			URL installURL = OcamlPlugin.getInstallURL();
		    URL url = new URL(installURL, iconPath + "caml32x32.gif");
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			this.setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}
	@Override
	public boolean performFinish() {
		if (!this.mainPage.getFileName().toLowerCase().endsWith(".mll"))
			this.mainPage.setFileName(this.mainPage.getFileName() + ".mll");
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