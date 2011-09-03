package ocaml.editors.lex;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.editors.util.OcamlCharacterPairMatcher;
import ocaml.natures.OcamlNatureMakefile;
import ocaml.popup.actions.CompileProjectAction;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.PaintManager;
import org.eclipse.jface.text.source.MatchingCharacterPainter;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.part.FileEditorInput;

/** Configures the editor for OCaml lex files */
public class OcamllexEditor extends TextEditor {

	protected PaintManager paintManager;
	protected MatchingCharacterPainter matchingCharacterPainter;

	public static final String ID = "ocaml.editors.mllEditor";

	public OcamllexEditor() {
		this.setSourceViewerConfiguration(new OcamllexSourceViewerConfig(this));
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

}
