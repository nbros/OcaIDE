package ocaml.editor.actions;

import java.util.concurrent.TimeUnit;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.OcamlMakefileBuilder;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/** This action activates completion in the OCaml editor. */
public class CleanProjectAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				IProject project = null;
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;
					project = editor.getProject();

				} else if (editorPart instanceof OcamllexEditor) {
					OcamllexEditor editor = (OcamllexEditor) editorPart;
					project = editor.getProject();

				} else if (editorPart instanceof OcamlyaccEditor) {
					OcamlyaccEditor editor = (OcamlyaccEditor) editorPart;
					project = editor.getProject();
				}
				
				if (project == null)
					return;
				
				final IProject buildProject = project;
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
						builder.clean(buildProject, monitor);
						return Status.OK_STATUS;
					}
				};

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
							String time = "";
							if (minutes > 1)
								time = time + String.valueOf(minutes) + " mins";
							else
								time = time + String.valueOf(minutes) + " min";
							if (seconds > 1)
								time = time + ", " + String.valueOf(seconds) + " secs";
							else
								time = time + ", " + String.valueOf(seconds) + " sec";
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

			}else
				OcamlPlugin.logError("ContentAssistAction: editorPart is null");
		} else
			OcamlPlugin.logError("ContentAssistAction: page is null");

	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
