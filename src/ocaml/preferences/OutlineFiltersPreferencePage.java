package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Outline) that allows the user to set
 * preferences relative to the outline
 */
public class OutlineFiltersPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public OutlineFiltersPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Choose which elements you want to see in the outline");
	}

	@Override
	public void createFieldEditors() {

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_LET,
				"Show the 'let' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_LET_IN,
				"Show the 'let in' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_VAL,
				"Show the 'val' definitions", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_EXTERNAL,
				"Show the 'external' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_TYPE,
				"Show the 'type' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_MODULE,
				"Show the 'module' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_MODULE_TYPE,
				"Show the 'module type' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_EXCEPTION,
				"Show the 'exception' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_OPEN,
				"Show the 'open' directives", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_METHOD,
				"Show the 'method' definitions", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_INCLUDE,
				"Show the 'include' directives", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_CLASS,
				"Show the 'class' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_CLASSTYPE,
				"Show the 'class type' definitions", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_INITIALIZER,
				"Show the 'initializer' definitions", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_VARIANT_CONS,
				"Show the variant type constructors", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_OUTLINE_SHOW_RECORD_CONS,
				"Show the record type constructors", this.getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
}
