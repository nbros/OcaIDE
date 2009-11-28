package ocaml.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import ocaml.OcamlPlugin;
import ocaml.natures.OcamlNatureMakefile;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * Define a wizard that allows the user to create a new empty make project. The created project gets the
 * ocamlnatureMakefile nature.
 */
public class OcamlNewEmptyMakeProjectWizard extends BasicNewProjectResourceWizard {

	@Override
	public void addPages() {
		super.addPages();
		IWizardPage[] pages = super.getPages();
		if (pages.length > 0)
			pages[0].setDescription("Create an empty OCaml Makefile project (useful for importing projects)");
		super.setWindowTitle("New Empty O'Caml Makefile Project");
	}

	@Override
	protected void initializeDefaultPageImageDescriptor() {
		super.initializeDefaultPageImageDescriptor();
		String iconPath = "icons/";
		try {
			URL installURL = OcamlPlugin.getInstallURL();
			URL url = new URL(installURL, iconPath + "caml32x32.gif");
			ImageDescriptor desc = ImageDescriptor.createFromURL(url);
			this.setDefaultPageImageDescriptor(desc);
		} catch (MalformedURLException e) {
			OcamlPlugin.logError(
					"Error in OcamlNewEmptyMakeProjectWizard:initializeDefaultPageImageDescriptor()", e);
		}
	}

	@Override
	public boolean performFinish() {
		boolean finish = super.performFinish();
		try {
			IProject project = this.getNewProject();
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];

			System.arraycopy(natures, 0, newNatures, 1, natures.length);
			newNatures[0] = OcamlNatureMakefile.ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);

			project.setDefaultCharset("ISO-8859-1", null);

			OcamlPaths opaths = new OcamlPaths(project);
			opaths.restoreDefaults();

		} catch (Exception e) {
			OcamlPlugin.logError("Error in OcamlNewEmptyMakeProjectWizard:performFinish()", e);
		}
		return finish;
	}
}
