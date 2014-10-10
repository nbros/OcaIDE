package ocaml.popup.actions;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.OcamlMakefileBuilder;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/** This action is called when the user clicks on the "Clean Project" contextual pop-up menu item on OCaml makefile projects */
public class CleanProjectPopupAction implements IObjectActionDelegate {

	private IProject project = null;

	public CleanProjectPopupAction() {
	}

	public void run(IAction action) {
		if (project != null) {
			final String jobName = "Cleaning project " + project.getName();
			Job job = new Job(jobName) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// save progress monitor for later use
					OcamlPlugin.ActiveBuildJobs.put(jobName, monitor);	

					OcamlMakefileBuilder builder = new OcamlMakefileBuilder();
					builder.clean(project, monitor);
					return Status.OK_STATUS;
				}
			};

			// open the "OCaml compiler output" view to show the output of the make
			Misc.showView(OcamlCompilerOutput.ID);

			job.setPriority(Job.BUILD);
			/*
			 * If the action is directly called by the user, then we display a dialog box. Else, the launch is silent.
			 */
			job.setUser(action != null);
			job.schedule(500);
			job.addJobChangeListener(new IJobChangeListener() {
				@Override
				public void sleeping(IJobChangeEvent event) {
				}
				
				@Override
				public void scheduled(IJobChangeEvent event) {
				}
				
				@Override
				public void running(IJobChangeEvent event) {
				}
				
				@Override
				public void done(IJobChangeEvent event) {
					// remove finished job from store
					OcamlPlugin.ActiveBuildJobs.remove(jobName);
				}
				
				@Override
				public void awake(IJobChangeEvent event) {
				}
				
				@Override
				public void aboutToRun(IJobChangeEvent event) {
				}
			});
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
