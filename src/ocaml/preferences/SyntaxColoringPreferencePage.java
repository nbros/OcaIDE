package ocaml.preferences;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlEditorColors;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Implements a preference page for syntax coloring preferences. */
public class SyntaxColoringPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public SyntaxColoringPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("OCaml editor syntax coloring configuration\n"
				+ "(close and reopen the editors to update)");
	}

	private ColorFieldEditor colorFieldKeywords;
	
	private ColorFieldEditor colorFieldLetinKeywords;
	
	private ColorFieldEditor colorFieldFunFunctionKeywords;

	private ColorFieldEditor colorFieldComment;

	private ColorFieldEditor colorFieldDocComment;

	private ColorFieldEditor colorFieldString;

	private ColorFieldEditor colorFieldConstant;

	private ColorFieldEditor colorFieldInteger;

	private ColorFieldEditor colorFieldDecimal;

	private ColorFieldEditor colorFieldCharacter;

	private ColorFieldEditor colorFieldAnnotationComment;

	private ColorFieldEditor colorFieldYaccDefinition;
	
	private ColorFieldEditor colorFieldPunctuation;

	@Override
	public void createFieldEditors() {
		colorFieldLetinKeywords = new ColorFieldEditor(PreferenceConstants.P_LETIN_COLOR, "'let' and 'in' Color:",
				this.getFieldEditorParent());
		colorFieldFunFunctionKeywords = new ColorFieldEditor(PreferenceConstants.P_FUN_COLOR, "'fun' and 'function' Color:",
				this.getFieldEditorParent());
		colorFieldKeywords = new ColorFieldEditor(PreferenceConstants.P_KEYWORD_COLOR, "All other &keywords Color:",
				this.getFieldEditorParent());
		colorFieldComment = new ColorFieldEditor(PreferenceConstants.P_COMMENT_COLOR, "&Comment Color:", this
				.getFieldEditorParent());
		colorFieldDocComment = new ColorFieldEditor(PreferenceConstants.P_DOC_COMMENT_COLOR,
				"&Documentation Comment Color:", this.getFieldEditorParent());
		colorFieldAnnotationComment = new ColorFieldEditor(PreferenceConstants.P_DOC_ANNOTATION_COLOR,
				"&Tag (@param...) in Documentation Comment Color:", this.getFieldEditorParent());
		colorFieldString = new ColorFieldEditor(PreferenceConstants.P_STRING_COLOR, "&String Color:", this
				.getFieldEditorParent());
		colorFieldConstant = new ColorFieldEditor(PreferenceConstants.P_CONSTANT_COLOR, "C&onstant Color:",
				this.getFieldEditorParent());

		colorFieldInteger = new ColorFieldEditor(PreferenceConstants.P_INTEGER_COLOR, "&Integer Color:", this
				.getFieldEditorParent());
		colorFieldDecimal = new ColorFieldEditor(PreferenceConstants.P_DECIMAL_COLOR, "&Decimal Color:", this
				.getFieldEditorParent());
		colorFieldCharacter = new ColorFieldEditor(PreferenceConstants.P_CHARACTER_COLOR,
				"C&haracter Color:", this.getFieldEditorParent());

		colorFieldYaccDefinition = new ColorFieldEditor(PreferenceConstants.P_YACC_DEFINITION_COLOR,
				"&Yacc Definition Color:", this.getFieldEditorParent());

		colorFieldPunctuation = new ColorFieldEditor(PreferenceConstants.P_PUNCTUATION_COLOR,
				"Punctuation Color:", this.getFieldEditorParent());

		this.addField(colorFieldKeywords);
		this.addField(colorFieldLetinKeywords);
		this.addField(colorFieldFunFunctionKeywords);
		this.addField(colorFieldComment);
		this.addField(colorFieldDocComment);
		this.addField(colorFieldAnnotationComment);
		this.addField(colorFieldString);
		this.addField(colorFieldConstant);
		this.addField(colorFieldInteger);
		this.addField(colorFieldDecimal);
		this.addField(colorFieldCharacter);
		this.addField(colorFieldYaccDefinition);
		this.addField(colorFieldPunctuation);
		

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_KEYWORDS, "Bold k&eywords", this
				.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_COMMENTS, "Bold co&mments", this
				.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_STRINGS, "Bold stri&ngs", this
				.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_CONSTANTS, "Bold con&stants", this
				.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_NUMBERS, "Bold n&umbers", this
				.getFieldEditorParent()));
		this.addField(new BooleanFieldEditor(PreferenceConstants.P_BOLD_CHARACTERS, "Bold ch&aracters", this
				.getFieldEditorParent()));

	}

	/** This method is required by the interface */
	public void init(IWorkbench workbench) {

	}

	@Override
	public boolean performOk() {
		super.performOk();
		OcamlEditorColors.reset();
		return true;
	}
}
