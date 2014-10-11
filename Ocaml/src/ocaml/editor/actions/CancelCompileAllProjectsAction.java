package ocaml.editor.actions;

import java.util.Collection;

import ocaml.OcamlPlugin;
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
public class CancelCompileAllProjectsAction implements IWorkbenchWindowActionDelegate {

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
				
				final String jobName = "Cancelling compiling jobs";
				
				Job job = new Job(jobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						if (!OcamlPlugin.ActiveBuildJobs.isEmpty()) {
							Misc.appendToOcamlConsole("");
							Misc.appendToOcamlConsole("Cancelling all compiling jobs...");
							Collection<IProgressMonitor> monitors = OcamlPlugin.ActiveBuildJobs.values();
							for (IProgressMonitor m: monitors) {
								m.setCanceled(true);
							}
							Misc.appendToOcamlConsole("Cancellation finished!");
						}
						return Status.OK_STATUS;
					}
				};

				// open the "OCaml compiler output" view to show the output of the make
				Misc.showView(OcamlCompilerOutput.ID);

				job.setPriority(Job.BUILD);
				job.setUser(action != null);
				job.schedule(50);
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
