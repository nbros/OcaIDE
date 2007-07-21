package ocaml.launching;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Shortcut for launching an executable directly from the navigator, without having to create a launch
 * configuration manually.
 */
public class OcamlLaunchShortcut implements ILaunchShortcut {

	public static String OCAML_LAUNCH_CONFIGURATION_TYPE_ID = "Ocaml.launchConfigurationType";

	/** Launch the executable corresponding to the selection in the navigator */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;

			Object first = sel.getFirstElement();
			if (first != null && first instanceof IFile) {
				IFile file = (IFile) first;
				IPath location = file.getLocation();

				if (location == null) {
					OcamlPlugin.logError("OcamlLaunchShortcut:launch : location is null");
					return;
				}

				String path = location.toOSString();

				ILaunchConfiguration configuration = findExistingLaunchConfiguration(file);
				if (configuration == null) {
					configuration = createLaunchConfiguration(path, file.getProject().getName(), file
							.getProject().getName()
							+ ": " + location.lastSegment());
					if (configuration == null) {
						OcamlPlugin.logError("OcamlLaunchShortcut:launch : configuration is null");
						return;
					}
				}

				DebugUITools.launch(configuration, mode);
			}
		}
	}

	/**
	 * Search for the existing launch configurations with the same executable, so as to not create a new
	 * configuration if there is already one for the same executable.
	 * 
	 * @return the first matching configuration, or null if none was found
	 */
	private ILaunchConfiguration findExistingLaunchConfiguration(IFile file) {
		IPath filePath = file.getLocation();
		if (filePath == null) {
			OcamlPlugin.logError("null path in OcamlLaunchShortcut:findExistingLaunchConfigurations");
			return null;
		}

		String path = filePath.toOSString();

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager
				.getLaunchConfigurationType(OCAML_LAUNCH_CONFIGURATION_TYPE_ID);

		if (type != null) {
			ILaunchConfiguration[] configs = null;
			try {
				configs = manager.getLaunchConfigurations(type);
			} catch (CoreException e) {
				OcamlPlugin.logError("ocaml plugin error", e);
			}
			if (configs != null && configs.length > 0) {
				for (int i = 0; i < configs.length; i++) {
					ILaunchConfiguration configuration = configs[i];
					String configPath;
					try {
						configPath = configuration.getAttribute(OcamlLaunchTab.ATTR_FULLPATH, "");
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
						return null;
					}

					if (configPath.equals(path)) {
						// System.err.println(path + " reused");
						return configuration;
					}
				}
			}
		}
		return null;
	}

	public static ILaunchConfiguration createLaunchConfiguration(String filePath, String projectName,
			String name) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager
				.getLaunchConfigurationType(OCAML_LAUNCH_CONFIGURATION_TYPE_ID);

		ILaunchConfigurationWorkingCopy workingCopy;
		try {
			workingCopy = type.newInstance(null, name);
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		workingCopy.setAttribute(OcamlLaunchTab.ATTR_FULLPATH, filePath);
		workingCopy.setAttribute(OcamlLaunchTab.ATTR_PROJECTNAME, projectName);
		workingCopy.setAttribute(OcamlLaunchTab.ATTR_ARGS, "");

		// set the defaults on the common tab
		CommonTab tab = new CommonTab();
		tab.setDefaults(workingCopy);
		tab.dispose();

		try {
			return workingCopy.doSave();
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}
	}

	/**
	 * This is normally used to launch the contents of the current editor. We don't currently enable this
	 * feature.
	 */
	public void launch(IEditorPart editor, String mode) {
		
		
	}

}
