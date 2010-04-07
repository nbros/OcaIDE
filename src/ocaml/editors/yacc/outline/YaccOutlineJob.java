package ocaml.editors.yacc.outline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;


public class YaccOutlineJob extends Job {

	public YaccOutlineJob(String name) {
		super(name);
	}

	/** The outline. Can be <code>null</code> if the outline view is closed */
	private OcamlYaccOutlineControl outline;

	private IDocument document;

	//private OcamlyaccEditor editor;

	public void setDoc(IDocument doc) {
		this.document = doc;
	}

	public void setOutline(OcamlYaccOutlineControl outline) {
		this.outline = outline;
	}

	/*public void setEditor(OcamlyaccEditor editor) {
		this.editor = editor;
	}*/
	
	Pattern patternNonTerminal = Pattern.compile("(?:\\A|\\n)((?:\\w|\\d|'|_)*)\\s*:");

	@Override
	public synchronized IStatus run(IProgressMonitor monitor) {
		
		String doc = document.get();
		
		Matcher matcher = patternNonTerminal.matcher(doc);
		
		YaccDef root = new YaccDef("<root>", 0, 0);
		
		while(matcher.find()){
			String name = matcher.group(1);
			int start = matcher.start(1);
			int end = matcher.end(1);
			
			YaccDef def = new YaccDef(name, start, end);
			root.addChild(def);
		}
		

		final YaccDef fOutlineDefinitions = root;

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				// give the definitions tree to the outline
				outline.setInput(fOutlineDefinitions);
			}
		});

		return Status.OK_STATUS;
	}
}
