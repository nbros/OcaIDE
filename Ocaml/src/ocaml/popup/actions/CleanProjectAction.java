package ocaml.popup.actions;

import ocaml.build.makefile.OcamlMakefileBuilder;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/** This action is called when the user clicks on the "Clean Project" contextual pop-up menu item on OCaml makefile projects */
public class CleanProjectAction implements IObjectActionDelegate {

	private IProject project = null;

	public CleanProjectAction() {
	}

	public void run(IAction action) {
		if (project != null) {
			Job job = new Job("Cleaning Project") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					OcamlMakefileBuilder builder = new OcamlMakefileBuilder();
					builder.clean(project, monitor);
					return Status.OK_STATUS;
				}
			};
			
			// open the "OCaml compiler output" view to show the output of the make
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
