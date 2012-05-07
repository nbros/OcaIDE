package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Paths) that allows the user to parameter the
 * paths of the OCaml tools (compilers, toplevel, documentation generator...)
 */
public class EditorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public EditorPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Preferences for automatic formatting in the editor as you type");
	}

	@Override
	public void createFieldEditors() {

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_AUTOCOMPLETION,
				"Enable completion auto activation", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_DISABLE_AUTOFORMAT,
				"Completely disable auto formatting", this.getFieldEditorParent()));

		IntegerFieldEditor tabWidth;
		tabWidth = new IntegerFieldEditor(PreferenceConstants.P_EDITOR_TABS,
				"Tab width in spaces", this.getFieldEditorParent());
		tabWidth.setValidRange(1, 16);
		this.addField(tabWidth);

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_SPACES_FOR_TABS,
				"Insert spaces instead of tabulations", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_SPELL_CHECKING,
				"Spell check comments", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_CONTINUE_COMMENTS,
				"Close and reopen comments on the next line", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_PIPE_AFTER_TYPE,
				"Automatically add a '|' on the next line after a type definition", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_PIPE_AFTER_WITH,
				"Automatically add a '|' on the next line after a 'with'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_PIPE_AFTER_FUN,
				"Automatically add a '|' on the next line after a 'fun' or 'function'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_AUTO_INDENT_CONT,
				"Automatically indent the next line if the current line ends with 'then', 'begin', '=', '->', etc.", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_KEEP_INDENT,
				"Keep the same indentation on the next line as the current line", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_REMOVE_PIPE,
				"Remove a '|' when I hit enter and it is alone on the current line", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_CONTINUE_PIPES,
				"Automatically add a '|' on the next line if the last line started with a '|' too", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_INDENT_IN,
				"Indent after 'in'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_INDENT_WITH,
				"Indent after 'with'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_DEDENT_SEMI_SEMI,
				"Dedent after ';;'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_INTELLIGENT_INDENT_START,
				"Intelligent indent when I hit <tab> at the beginning of a line", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_COLON_COLON_TAB,
				"::<tab> inserts []", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_FN_TAB,
				"fn<tab> inserts 'function'", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_TAB_ARROW,
				"<tab> in the middle of a line inserts '->'", this.getFieldEditorParent()));
		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_EDITOR_DOUBLEQUOTES,
				"Automatically add the closing ' \" '", this.getFieldEditorParent()));
		
	}

	public void init(IWorkbench workbench) {
	}
}
