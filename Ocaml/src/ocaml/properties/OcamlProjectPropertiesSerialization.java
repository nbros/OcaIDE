package ocaml.properties;

import java.io.IOException;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;

public class OcamlProjectPropertiesSerialization {
	public static final String SETTINGS_FILE = ".projectSettings";
	public static final String SECTION = "OcamlProjectSettings";
	private final IProject project;
	private DialogSettings dialogSettings;

	public OcamlProjectPropertiesSerialization(IProject project) {
		this.project = project;
	}

	public IDialogSettings load(String sectionName) {
		dialogSettings = new DialogSettings(SECTION);
		IFile settingsFile = this.project.getFile(SETTINGS_FILE);
		if (settingsFile.exists()) {
			try {
				dialogSettings.load(settingsFile.getLocation().toString());
			} catch (IOException e) {
				OcamlPlugin.logError("Cannot load settings", e);
			}
		}
		IDialogSettings section = dialogSettings.getSection(sectionName);
		if (section == null) {
			section = dialogSettings.addNewSection(sectionName);
		}
		return section;
	}

	public void save() {
		if(dialogSettings == null) {
			throw new IllegalStateException("The settings must be loaded first");
		}
		IFile settingsFile = this.project.getFile(SETTINGS_FILE);
		try {
			dialogSettings.save(settingsFile.getLocation().toString());
		} catch (IOException e) {
			OcamlPlugin.logError("Cannot save settings", e);
			return;
		}
		try {
			settingsFile.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
		} catch (CoreException e) {
			OcamlPlugin.logError(e);
		}
	}

}
