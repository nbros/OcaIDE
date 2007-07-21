package ocaml.perspectives;

import ocaml.debugging.views.OcamlBreakpointsView;
import ocaml.debugging.views.OcamlCallStackView;
import ocaml.debugging.views.OcamlWatchView;
import ocaml.views.OcamlBrowserView;
import ocaml.views.OcamlCompilerOutput;
import ocaml.views.toplevel.OcamlToplevelView;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/**
 * Configures the default positioning of views inside the O'Caml debug perspective, associates the debug
 * actions, and create shortcuts to often used views
 */
public class OcamlDebugPerspective implements IPerspectiveFactory {

	/** The identifier of the O'Caml debug perspective */
	public static String ID = "ocaml.perspectives.OcamlDebugPerspective";

	public void createInitialLayout(IPageLayout layout) {

		// Add debug buttons in the main menu and in the tool-bar
		layout.addActionSet("Ocaml_debuggingActionSet");

		// add the "source" menu items
		layout.addActionSet("Ocaml_sourceActionSet");

		// add shortcuts that allow the user to open a view more quickly
		layout.addShowViewShortcut(OcamlCompilerOutput.ID);
		layout.addShowViewShortcut(OcamlBrowserView.ID);
		layout.addShowViewShortcut(OcamlToplevelView.ID);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
		layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");

		layout.addShowViewShortcut(OcamlCallStackView.ID);
		layout.addShowViewShortcut(OcamlBreakpointsView.ID);

		IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, layout.getEditorArea());
		right.addView(OcamlCallStackView.ID);

		IFolderLayout right2 = layout.createFolder("right2", IPageLayout.BOTTOM, 0.30f, "right");
		right2.addView(OcamlBreakpointsView.ID);

		IFolderLayout right3 = layout.createFolder("right3", IPageLayout.BOTTOM, 0.50f, "right2");
		right3.addView(OcamlWatchView.ID);

		// bottom : console
		IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.80f, layout
				.getEditorArea());
		bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
	}
}
