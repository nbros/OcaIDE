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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;

/** This action activates completion in the OCaml editor. */
public class CancelCompileAllProjectsAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				IProject project = null;
				FileEditorInput editorInput = (FileEditorInput) editorPart.getEditorInput();
				if (editorInput != null && editorInput.getFile() != null) {
					project = editorInput.getFile().getProject();
				}

				if (project == null)
					return;

				// show compiler output
				Misc.showView(OcamlCompilerOutput.ID);
				// then activate current editor (to resolve shortcut-key issues)
				page.activate(editorPart);

				final String jobName = "Cancelling compiling jobs";

				Job job = new Job(jobName) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						if (!OcamlPlugin.ActiveBuildJobs.isEmpty()) {
							// cancel all jobs
							Collection<IProgressMonitor> monitors = OcamlPlugin.ActiveBuildJobs.values();
							for (IProgressMonitor m: monitors) {
								m.setCanceled(true);
							}
							// clear them from store
							OcamlPlugin.ActiveBuildJobs.clear();
						}
						return Status.OK_STATUS;
					}
				};

				job.setPriority(Job.BUILD);
				job.setUser(action != null);
				job.schedule(50);
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
