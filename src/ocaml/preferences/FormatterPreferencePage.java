package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Formatter) that allows the user to parameter the
 * integrated code formatter
 */
public class FormatterPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public FormatterPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Preferences for the code formatter");
	}

	@Override
	public void createFieldEditors() {

		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_BEGIN,
				"Indent after 'begin'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_STRUCT,
				"Indent after 'struct'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_SIG,
				"Indent after 'sig'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_IN,
				"Indent after 'in'", this.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_FORMATTER_INDENT_LET_IN,
				"Indent consecutive 'let in's", this.getFieldEditorParent()));
		
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_DEF,
				"Indent after definition", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FOR,
				"Indent after 'for'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_THEN,
				"Indent after 'then'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_ELSE,
				"Indent after 'else'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_WHILE,
				"Indent after 'while'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_MATCH_ACTION,
				"Indent for match action (after '->')", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FIRST_MATCH_CASE,
				"Indent for first match case", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FUNCTOR,
				"Indent for functor", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_TRY,
				"Indent after 'try'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_WITH,
				"Indent after 'with'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_OBJECT,
				"Indent after 'object'", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_APPLICATION,
				"Indent for arguments in function application", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_RECORD,
				"Indent for record elements", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FIRST_CONSTRUCTOR,
				"Indent for first constructor", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_PAREN,
				"Indent after '('", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FIRST_CATCH,
				"Indent for first catch case", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_FUN_ARGS,
				"Indent for function arguments in function definition", this.getFieldEditorParent()));
		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_MODULE_CONSTRAINT,
				"Indent for module constraints", this.getFieldEditorParent()));

		
		
		
		
		
		/*this.addField(new BooleanFieldEditor(PreferenceConstants.P_FORMATTER_INDENT_IN,
				"Indent after 'in'", this.getFieldEditorParent()));

		
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_FORMATTER_INDENT_IN,
				"Indent after 'in'", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_FORMATTER_INDENT_IN_LETS,
				"Indent after 'in' in consecutive 'let's", this.getFieldEditorParent()));

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_FORMATTER_FORMAT_COMMENTS,
				"Format comments (that do not start with \"(*|\")", this.getFieldEditorParent()));

		
		IntegerFieldEditor maxCommentWidth = 
		new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_COMMENT_WIDTH,
				"Maximum comment width", this.getFieldEditorParent());
		maxCommentWidth.setValidRange(10, 1000);		
		this.addField(maxCommentWidth);*/

		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_MAX_BLANK_LINES,
				"Maximum number of blank lines to keep", this.getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
}
