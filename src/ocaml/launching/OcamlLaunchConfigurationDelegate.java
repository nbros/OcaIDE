package ocaml.launching;

import java.io.File;

import ocaml.OcamlPlugin;
import ocaml.debugging.OcamlDebugger;
import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;

/** Launch an O'Caml executable in normal or debug mode, using the previously created launch configuration */
public class OcamlLaunchConfigurationDelegate implements ILaunchConfigurationDelegate {

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch,
			IProgressMonitor monitor) throws CoreException {

		String path = configuration.getAttribute(OcamlLaunchTab.ATTR_FULLPATH, "");
		String projectName = configuration.getAttribute(OcamlLaunchTab.ATTR_PROJECTNAME, "");
		String args = configuration.getAttribute(OcamlLaunchTab.ATTR_ARGS, "");
		
		String[] arguments = DebugPlugin.parseArguments(args);
		
		File file = new File(path);
		if (!file.exists()) {
			OcamlPlugin.logError(path + " is not a valid file name");
			return;
		}

		IProject project = null;
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject p : projects)
			if (p.getName().equals(projectName)) {
				project = p;
				break;
			}

		if (project == null) {
			OcamlPlugin.logError(project + " is not a valid project name");
			return;
		}

		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			
			if(!OcamlPlugin.runningOnLinuxCompatibleSystem()) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						MessageDialog.openInformation(null, "Ocaml Plugin",
						"Sorry, the debugger only works on Linux or Mac");
					}
				});
				return;
			}
			
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().saveAllEditors(false);
				}
			});

			
			OcamlDebugger.getInstance().start(file, project, launch, arguments);
		}

		else if (mode.equals(ILaunchManager.RUN_MODE)) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().saveAllEditors(false);
				}
			});

			String[] commandLine = new String[arguments.length + 1];
			commandLine[0] = path;
			System.arraycopy(arguments, 0, commandLine, 1, arguments.length);

			Process process = DebugPlugin.exec(commandLine, file.getParentFile());
			IProcess iProcess = DebugPlugin.newProcess(launch, process, file.getName());
			launch.addProcess(iProcess);

			// show the console view, so that the user can see the program outputs
			Misc.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			
		}
	}
}
