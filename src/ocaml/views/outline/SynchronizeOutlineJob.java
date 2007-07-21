package ocaml.views.outline;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/** This Job is used to synchronize the selection in the outline with the current line in the editor, in a low priority thread. */
public class SynchronizeOutlineJob extends Job {

	private OcamlEditor editor;
	private OutlineJob outlineJob;

	public SynchronizeOutlineJob(String name) {
		super(name);
	}

	public void setEditor(OcamlEditor editor) {
		this.editor = editor;
	}

	public void setOutlineJob(OutlineJob outlineJob) {
		this.outlineJob = outlineJob;
	}

	/**
	 * This method is "synchronized" to ascertain that this Job will never be running more than one instance
	 * at any moment.
	 */
	@Override
	protected synchronized IStatus run(IProgressMonitor monitor) {
		
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				// let the outline finish its Job before we synchronize it
				while (outlineJob != null && outlineJob.getState() == Job.RUNNING)
					Thread.yield();

				if (editor.getOutline() == null)
					return;

				IEditorInput input = editor.getEditorInput();
				IDocumentProvider provider = editor.getDocumentProvider();
				if (provider != null) {
					IDocument document = provider.getDocument(input);
					if (document != null) {
						int offset = editor.getCaretOffset();
						int line, column;
						try {
							line = document.getLineOfOffset(offset);
							column = offset - document.getLineOffset(line);
						} catch (BadLocationException e) {
							OcamlPlugin.logError("ocaml plugin error", e);
							return;
						}
						
						editor.getOutline().synchronizeWithEditor(line, column);
					}
				}

			}
		});
		return Status.OK_STATUS;
	}

}
