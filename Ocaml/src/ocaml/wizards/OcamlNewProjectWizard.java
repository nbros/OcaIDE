package ocaml.wizards;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.natures.OcamlNature;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/** A wizard to create new O'Caml managed projects, and associate them the ocamlnature nature */
public class OcamlNewProjectWizard extends BasicNewProjectResourceWizard {
	
	@Override
	public void addPages() {
		super.addPages();
		super.setWindowTitle("New OCaml Project");
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
			OcamlPlugin.logError("Error in OcamlNewProjectWizard:initializeDefaultPageImageDescriptor()", e);
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
			newNatures[0] = OcamlNature.ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
			
			project.setDefaultCharset("ISO-8859-1", null);
			
			OcamlPaths opaths = new OcamlPaths(project);
			opaths.restoreDefaults();
			
			// Associate the default compilation flags list to the Builder
			final List<String> defFlagsAsList = new ArrayList<String>(Misc.defaultProjectFlags.length);
			for (String flg : Misc.defaultProjectFlags) {
				defFlagsAsList.add(flg.trim());
			}
			OcamlBuilder.setResourceFlags(project, defFlagsAsList);
			
		} catch (Exception e) {
			OcamlPlugin.logError("Error in OcamlNewProjectWizard:performFinish()", e);
		}
		return finish;
	}
}