package ocaml.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import ocaml.OcamlPlugin;
import ocaml.natures.OcamlNatureMakefile;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/** A wizard to create new make projects, and associate them the ocamlnatureMakefile nature */
public class OcamlNewMakeProjectWizard extends BasicNewProjectResourceWizard {

	@Override
	public void addPages() {
		super.addPages();
		super.setWindowTitle("New OCaml Makefile Project");
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
			OcamlPlugin.logError("Error in OcamlNewMakeProjectWizard:initializeDefaultPageImageDescriptor()",
					e);
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

			URL url = OcamlPlugin.getInstance().getBundle().getEntry("/resources/OCamlMakefile"); 

			IFile file = project.getFile("OCamlMakefile");
			file.create(url.openStream(), true, null);

			url = OcamlPlugin.getInstance().getBundle().getEntry("/resources/defaultMakefile"); 

			file = project.getFile("Makefile");
			file.create(url.openStream(), true, null);


		} catch (Exception e) {
			OcamlPlugin.logError("Error in OcamlNewMakeProjectWizard:performFinish()", e);
		}
		return finish;
	}
}
