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
		this.addField(maxCommentWidth);

		this.addField(new IntegerFieldEditor(PreferenceConstants.P_FORMATTER_MAX_BLANK_LINES,
				"Maximum number of blank lines to keep", this.getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
}
