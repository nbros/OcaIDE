package ocaml.editors.yacc;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.editors.util.OcamlCharacterPairMatcher;
import ocaml.editors.yacc.outline.OcamlYaccOutlineControl;
import ocaml.editors.yacc.outline.YaccOutlineJob;
import ocaml.natures.OcamlNatureMakefile;
import ocaml.popup.actions.CompileProjectAction;
import ocaml.preferences.PreferenceConstants;
import ocaml.views.outline.OutlineJob;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/** The O'Caml Yacc editor (for .mly files) */
public class OcamlyaccEditor extends TextEditor {
	protected PaintManager paintManager;
	protected MatchingCharacterPainter matchingCharacterPainter;

	public static final String ID = "ocaml.editors.mlyEditor";

	public OcamlyaccEditor() {
		this.setSourceViewerConfiguration(new OcamlyaccSourceViewerConfig(this));
	}

	@Override
	protected void createActions() {
		super.createActions();

		paintManager = new PaintManager(getSourceViewer());
		matchingCharacterPainter =
				new MatchingCharacterPainter(getSourceViewer(), new OcamlCharacterPairMatcher());
		matchingCharacterPainter.setColor(new Color(Display.getCurrent(), new RGB(160, 160, 160)));
		paintManager.addPainter(matchingCharacterPainter);

		OcamlPlugin.getInstance().checkPaths();

		// effectue le parsing des bibliothèques ocaml en arrière plan
		CompletionJob job = new CompletionJob("Parsing ocaml library mli files", null);
		job.setPriority(CompletionJob.DECORATE);
		job.schedule();
		
		
		this.getSourceViewer().addTextListener(new ITextListener() {

			public void textChanged(TextEvent event) {
				if (event.getDocumentEvent() != null)
					rebuildOutline(500);
			}
		});
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		StyledText styledText = this.getSourceViewer().getTextWidget();
		styledText.setTabs(getTabSize());
	}

	@Override
	/** Initialise le contexte pour les raccourcis clavier de l'éditeur ocaml */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "Ocaml.editor.context" });
	}

	public IProject getProject() {
		FileEditorInput editorInput = (FileEditorInput) this.getEditorInput();
		if (editorInput == null || editorInput.getFile() == null) {
			return null;
		}
		return editorInput.getFile().getProject();
	}

	public static int getTabSize() {
		return OcamlPlugin.getInstance().getPreferenceStore().getInt(PreferenceConstants.P_EDITOR_TABS);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);

		boolean bMakefileNature = false;
		try {
			bMakefileNature = this.getProject().getNature(OcamlNatureMakefile.ID) != null;
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}

		/*
		 * Si ce projet est un projet avec makefile, alors on compile manuellement à chaque fois
		 * qu'on sauvegarde
		 */
		if (bMakefileNature) {
			IWorkspace ws = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription desc = ws.getDescription();
			if (desc.isAutoBuilding())
				CompileProjectAction.compileProject(this.getProject());
		}
	}
	
	
	private OcamlYaccOutlineControl outline;
	private YaccOutlineJob outlineJob = null;
	
	/**
	 * We give the outline to Eclipse when it asks for an adapter with the outline class.
	 */
	@Override
	public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (this.outline == null)
				this.outline = new OcamlYaccOutlineControl(this);
			rebuildOutline(100);
			return this.outline;
		}
		return super.getAdapter(required);
	}
	
	public void rebuildOutline(int delay) {

		IEditorInput input = this.getEditorInput();
		IDocument document = this.getDocumentProvider().getDocument(input);
		// String doc = document.get();

		if (outlineJob == null)
			outlineJob = new YaccOutlineJob("Rebuilding outline for mly editor");
		else if (outlineJob.getState() == OutlineJob.RUNNING)
			return;
		// only one Job at a time
		else
			outlineJob.cancel();

		outlineJob.setPriority(CompletionJob.DECORATE);
		outlineJob.setOutline(this.outline);
		outlineJob.setDoc(document);

		outlineJob.schedule(delay);
	}
	
	
}
