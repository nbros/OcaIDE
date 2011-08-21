package ocaml.popup.actions;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.MakefileTargets;
import ocaml.exec.CommandRunner;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on "Create Documentation" in the pop-up menu for O'Caml makefile
 * projects. It calls the "make" command with the documentation target configured in the project properties
 */
public class GenDocAction implements IObjectActionDelegate {

	private IProject project = null;

	public GenDocAction() {
	}

	public void run(IAction action) {
		if (project != null) {
			Job job = new Job("Generating Documentation") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					MakefileTargets makefileTargets = new MakefileTargets(project);
					String[] targets = makefileTargets.getDocTargets();

					String[] command = new String[targets.length + 1];
					command[0] = "make";
					// add the makefile targets as arguments, after "make" in the table
					System.arraycopy(targets, 0, command, 1, targets.length);

					String path = project.getLocation().toOSString();

					CommandRunner commandRunner = new CommandRunner(command, path);

					final String out = commandRunner.getStdout().trim();
					final String err = commandRunner.getStderr().trim();

					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							OcamlCompilerOutput outputView = OcamlCompilerOutput.get();
							if (outputView != null) {
								outputView.clear();
								if (!err.equals(""))
									outputView.appendln(err);
								if (!out.equals(""))
									outputView.appendln(out);
							}
						}
					});

					try {
						project.refreshLocal(IProject.DEPTH_INFINITE, null);
					} catch (CoreException e1) {
						OcamlPlugin.logError("ocaml plugin error", e1);
					}

					return Status.OK_STATUS;
				}
			};

			// open the "O'Caml compiler output" view to show the output of the make command
			Misc.showView(OcamlCompilerOutput.ID);

			job.setPriority(Job.BUILD);
			job.setUser(true);
			job.schedule(500);

		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.project = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object o = sel.getFirstElement();
			if (o instanceof IProject)
				this.project = (IProject) o;
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}
}
