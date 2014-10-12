package ocaml.popup.actions;

import java.util.concurrent.TimeUnit;

import ocaml.OcamlPlugin;
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

/**
 * This action is called when the user clicks on the "make project" menu item in the contextual menu for
 * OCaml makefile projects. Calls the Builder to build the project.
 */
public class CompileProjectPopupAction implements IObjectActionDelegate {
	private IProject project = null;

	public CompileProjectPopupAction() {
	}

	/** This method is used to call the action directly, without having to instantiate it */
	public static void compileProject(IProject project) {
		CompileProjectPopupAction compileProjectAction = new CompileProjectPopupAction();
		compileProjectAction.project = project;
		compileProjectAction.run(null);
	}

	public void run(IAction action) {
		if (project != null) {
			final long[] executedTime = new long[1];
			executedTime[0] = -1;

			final String jobName = "Compiling project " + project.getName();
			
			Job job = new Job(jobName) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						// save progress monitor for later use
						OcamlPlugin.ActiveBuildJobs.put(jobName, monitor);
						
						// compile
						executedTime[0] = System.currentTimeMillis();
						project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					} catch (CoreException e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}
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
					// compiling job was cancelled
					if (!OcamlPlugin.ActiveBuildJobs.containsKey(jobName)) {
						Misc.appendToOcamlConsole("Compilation was cancelled!");
					}
					// compiling job terminates normally
					else {
						OcamlPlugin.ActiveBuildJobs.remove(jobName);
					}
					
					// time
					long compilingTime = -1;
					if (executedTime[0] > 0) 
						compilingTime = System.currentTimeMillis() - executedTime[0];
					if (compilingTime >= 0) {
						long minutes = TimeUnit.MILLISECONDS.toMinutes(compilingTime);
						long seconds = TimeUnit.MILLISECONDS.toSeconds(compilingTime) - 
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
