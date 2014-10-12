package ocaml.popup.actions;

import java.util.concurrent.TimeUnit;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.OcamlMakefileBuilder;
import ocaml.util.Misc;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IProject;
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
			
			final long[] executedTime = new long[1];
			executedTime[0] = -1;

			Job job = new Job(jobName) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// save progress monitor for later use
					OcamlPlugin.ActiveBuildJobs.put(jobName, monitor);

					// cleaning
					executedTime[0] = System.currentTimeMillis();
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
					// cleaning job was cancelled
					if (!OcamlPlugin.ActiveBuildJobs.containsKey(jobName)) {
						Misc.appendToOcamlConsole("Cleaning was cancelled!");
					}
					// cleaning job terminates normally
					else {
						OcamlPlugin.ActiveBuildJobs.remove(jobName);
					}
					
					// time
					long cleaningTime = -1;
					if (executedTime[0] > 0) 
						cleaningTime = System.currentTimeMillis() - executedTime[0];
					if (cleaningTime >= 0) {
						long minutes = TimeUnit.MILLISECONDS.toMinutes(cleaningTime);
						long seconds = TimeUnit.MILLISECONDS.toSeconds(cleaningTime) - 
							    TimeUnit.MINUTES.toSeconds(minutes);
						String time = String.format("%d min, %d sec", minutes, seconds); 
						Misc.appendToOcamlConsole("Time: " + time);
					}
					else
						Misc.appendToOcamlConsole("Time: unknown");
					
					Misc.appendToOcamlConsole("");
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
