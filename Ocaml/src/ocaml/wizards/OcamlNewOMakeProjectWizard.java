package ocaml.wizards;

import java.net.MalformedURLException;
import java.net.URL;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.MakefileProperties;
import ocaml.exec.CommandRunner;
import ocaml.natures.OcamlNatureMakefile;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/** A wizard to create new omake projects, and associate them the ocamlnatureMakefile nature */
public class OcamlNewOMakeProjectWizard extends BasicNewProjectResourceWizard {

	@Override
	public void addPages() {
		super.addPages();
		super.setWindowTitle("New OCaml OMake Project");
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
					"Error in OcamlNewOMakeProjectWizard:initializeDefaultPageImageDescriptor()", e);
		}
	}

	@Override
	public boolean performFinish() {
		boolean finish = super.performFinish();
		try {
			IProject project = this.getNewProject();

			/*
			 * We have to do this early as project.setDescription() implicitly calls build() which needs to
			 * know the variant of make
			 */
			MakefileProperties makefileProperties = new MakefileProperties(project);
			makefileProperties.setVariant(MakefileProperties.Variants.OMAKE);
			makefileProperties.save();

			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();
			String[] newNatures = new String[natures.length + 1];

			System.arraycopy(natures, 0, newNatures, 1, natures.length);
			newNatures[0] = OcamlNatureMakefile.ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);

			project.setDefaultCharset("ISO-8859-1", null);

			if (OcamlPlugin.getOMakeFullPath().trim().equals("")) {
				OcamlPlugin.logError("The omake command couldn't be found."
						+ " Please configure its path in the preferences.");
			} else {
				String[] cmdLine = { OcamlPlugin.getOMakeFullPath(), "--install" };
				new CommandRunner(cmdLine, project.getLocation().toOSString());
			}

			// Update navigator view
			project.getFile("OMakeroot");
			project.getFile("OMakefile");

			OcamlPaths opaths = new OcamlPaths(project);
			opaths.restoreDefaults();
		} catch (Exception e) {
			OcamlPlugin.logError("Error in OcamlNewOMakeProjectWizard:performFinish()", e);
		}
		return finish;
	}
}
