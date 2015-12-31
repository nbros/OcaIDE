package ocaml.preferences;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This is the root page for all OCaml preferences.
 */

public class RootPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public RootPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("OcaIDE general preferences");
	}

	@Override
	public void createFieldEditors() {
		this.addField(new BooleanFieldEditor(
				PreferenceConstants.P_DISABLE_UNICODE_CHARS,
				"Disable Unicode characters (check this if your system "
						+ "doesn't display Unicode characters correctly)", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(
				PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE,
				"Show the types in the outline", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(
				PreferenceConstants.P_SHOW_TYPES_IN_STATUS_BAR,
				"Show the types in the editor's status bar", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(
				PreferenceConstants.P_SHOW_TYPES_IN_POPUPS,
				"Show the types in popups when hovering over the editor", this
						.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(
				PreferenceConstants.P_SHOW_MARKERS_IN_STATUS_BAR,
				"Show markers' information in the editor's status bar", this
						.getFieldEditorParent()));
	}
	
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		Misc.bNoUnicode = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.P_DISABLE_UNICODE_CHARS);
		return result;
	}

	public void init(IWorkbench workbench) {
	}
}