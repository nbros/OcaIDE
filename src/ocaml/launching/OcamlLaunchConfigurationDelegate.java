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

        String runpath = configuration.getAttribute(OcamlLaunchTab.ATTR_RUNPATH, "");
	    String exepath = configuration.getAttribute(OcamlLaunchTab.ATTR_BYTEPATH, "");
		String projectName = configuration.getAttribute(OcamlLaunchTab.ATTR_PROJECTNAME, "");
		String args = configuration.getAttribute(OcamlLaunchTab.ATTR_ARGS, "");
		
		String[] arguments = DebugPlugin.parseArguments(args);
		
		File runfile = new File(runpath);
		if (!runfile.exists()) {
			OcamlPlugin.logError(runpath + " is not a valid executable");
			return;
		}

		File bytefile;
		if (exepath.length() > 0) {
            bytefile = new File(exepath);
            if (!bytefile.exists()) {
                OcamlPlugin.logError(exepath + " is not a valid bytecode file");
                return;
            }
		} else {
		    bytefile = runfile;
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

			String ocamldebug = OcamlPlugin.getOcamldebugFullPath();
			File ocamldebugFile = new File(ocamldebug);
			if (!ocamldebugFile.exists()) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
								"Ocamldebug path is not correctly set in preferences.");
					}
				});
				
				return;
			}

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().saveAllEditors(false);
				}
			});

			boolean remoteDebugEnable = configuration.getAttribute(
				OcamlLaunchTab.ATTR_REMOTE_DEBUG_ENABLE, 
				OcamlLaunchTab.DEFAULT_REMOTE_DEBUG_ENABLE
			);
			int remoteDebugPort = Integer.parseInt(
				configuration.getAttribute(
					OcamlLaunchTab.ATTR_REMOTE_DEBUG_PORT,
					OcamlLaunchTab.DEFAULT_REMOTE_DEBUG_PORT
				)
			);

			String scriptFile = configuration.getAttribute(OcamlLaunchTab.ATTR_SCRIPTPATH, "");

			OcamlDebugger.getInstance().start(
				ocamldebug, runfile, bytefile, project, launch, arguments, remoteDebugEnable, remoteDebugPort,  scriptFile
			);
		}

		else if (mode.equals(ILaunchManager.RUN_MODE)) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					PlatformUI.getWorkbench().saveAllEditors(false);
				}
			});

			String[] commandLine = new String[arguments.length + 1];
			commandLine[0] = runpath;
			System.arraycopy(arguments, 0, commandLine, 1, arguments.length);

			Process process = DebugPlugin.exec(commandLine, runfile.getParentFile());
			IProcess iProcess = DebugPlugin.newProcess(launch, process, bytefile.getName());
			launch.addProcess(iProcess);

			// show the console view, so that the user can see the program outputs
			Misc.showView(IConsoleConstants.ID_CONSOLE_VIEW);
			
		}
	}
}