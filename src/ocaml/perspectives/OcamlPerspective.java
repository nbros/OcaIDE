package ocaml.perspectives;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

import ocaml.OcamlPlugin;
import ocaml.views.OcamlBrowserView;
import ocaml.views.OcamlCompilerOutput;
import ocaml.views.toplevel.OcamlToplevelView;

/**
 * Configures the default positioning of views inside the O'Caml main perspective, associates the debug
 * actions, and create shortcuts to often used views and wizards.
 */
public class OcamlPerspective implements IPerspectiveFactory {
	
	/** The view's unique identifier */
	public static String ID = "ocaml.perspectives.OcamlPerspective";

	/** Defines a default size and position for all the views inside the ocaml perspective */
	public void createInitialLayout(IPageLayout layout) {
		// get the editor area (the other views are placed relative to this one)
		String editorArea = layout.getEditorArea();
		
		//layout.addPerspectiveShortcut();
		
		// add shortcuts to the O'Caml wizards so that they appear in the File->New menu
		layout.addNewWizardShortcut("ocaml.wizards.OcamlNewModuleWizard");
		layout.addNewWizardShortcut("ocaml.wizards.OcamlNewInterfaceWizard");
		layout.addNewWizardShortcut("ocaml.wizards.OcamlNewYaccWizard");
		layout.addNewWizardShortcut("ocaml.wizards.OcamlNewLexWizard");
		layout.addNewWizardShortcut("ocaml.wizards.OcamlNewProjectWizard");
		
		
		if(OcamlPlugin.runningOnLinuxCompatibleSystem()){
			layout.addNewWizardShortcut("ocaml.wizards.OcamlNewMakefileProjectWizard");
			layout.addNewWizardShortcut("ocaml.wizards.OcamlNewEmptyMakefileProjectWizard");
		}
		
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");
		layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");
		
		// add shortcuts to the most often used views in an O'Caml project 
		layout.addShowViewShortcut(OcamlCompilerOutput.ID);
		layout.addShowViewShortcut(OcamlBrowserView.ID);
		layout.addShowViewShortcut(OcamlToplevelView.ID);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");
		
		
		// put the navigator on the left 
		IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea);
		left.addView(IPageLayout.ID_RES_NAV);
		
		// put the browser in a fast view 
		layout.addFastView(OcamlBrowserView.ID);

		// put the toplevel, console, compiler output, problem view and runtime log on the bottom 
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.70f, editorArea);
		bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottom.addView(OcamlToplevelView.ID);
		bottom.addView(OcamlCompilerOutput.ID);
		bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		bottom.addView("org.eclipse.pde.runtime.LogView");
				

		// put the outline on the right 
		IFolderLayout right =
				layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea);
		right.addView(IPageLayout.ID_OUTLINE);

		layout.addActionSet("org.eclipse.debug.ui.launchActionSet");

		// add the source menu items 
		layout.addActionSet("Ocaml_sourceActionSet");
	}
}