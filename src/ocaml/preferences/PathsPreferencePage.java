package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Paths) that allows the user to parameter the
 * paths of the O'Caml tools (compilers, toplevel, documentation generator...)
 */
public class PathsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PathsPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Please fill in all fields with the correct paths on your system");
	}

	@Override
	public void createFieldEditors() {
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAML, "oca&ml:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLC, "ocaml&c:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLOPT, "ocaml&opt:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDEP, "ocaml&dep:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLLEX, "ocamlle&x:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLYACC, "ocaml&yacc:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDOC, "oc&amldoc:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDEBUG, "ocamldebu&g:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_PATH_CAMLP4, "camlp4:", this
				.getFieldEditorParent()));
		this.addField(new FileFieldEditor(PreferenceConstants.P_MAKE_PATH, "make:", this
				.getFieldEditorParent()));
		this.addField(new DirectoryFieldEditor(PreferenceConstants.P_LIB_PATH, "OCaml &lib path:", this
				.getFieldEditorParent()));
		// this.addField(new DirectoryFieldEditor(PreferenceConstants.P_INCLUDE_PATH, "&Include lib path:",
		// this.getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {
	}
}
