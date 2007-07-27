package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Outline) that allows the user to set
 * preferences relative to the outline
 */
public class OutlinePreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public OutlinePreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Preferences for the outline");
	}

	@Override
	public void createFieldEditors() {

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE,
				"Show the O'Caml types after each element in the outline", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_EXPAND_MODULES,
				"Always expand modules in the outline", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_EXPAND_CLASSES,
				"Always expand classes in the outline", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_EXPAND_ALL,
				"Always completely expand the outline", this
						.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_UNNEST_IN,
				"Unnest 'let in' definitions in outline", this
						.getFieldEditorParent()));

		this.addField(new IntegerFieldEditor(PreferenceConstants.P_OUTLINE_LET_MINIMUM_CHARS,
				"Show 'let' definitions with an identifier with at least (nb of characters)", this.getFieldEditorParent()));

		this.addField(new IntegerFieldEditor(PreferenceConstants.P_OUTLINE_LET_IN_MINIMUM_CHARS,
				"Show 'let in' definitions with an identifier with at least (nb of characters)", this.getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {
	}
}
