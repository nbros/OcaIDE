package ocaml.preferences;

import ocaml.OcamlPlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Debugger) that allows the user to
 * parameter the debugger checkpoints
 */
public class DebuggerPreferencePage extends FieldEditorPreferencePage implements
		IWorkbenchPreferencePage {

	public DebuggerPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Preferences for the debugger");
	}

	IntegerFieldEditor fieldEditorSmallStep;
	IntegerFieldEditor fieldEditorBigStep;
	IntegerFieldEditor fieldEditorProcessCount;

	@Override
	public void createFieldEditors() {

		this.addField(new BooleanFieldEditor(PreferenceConstants.P_DEBUGGER_CHECKPOINTS,
				"Activate checkpoints", this.getFieldEditorParent()));

		fieldEditorSmallStep = new IntegerFieldEditor(PreferenceConstants.P_DEBUGGER_SMALL_STEP,
				"Number of events between two checkpoints for a small step", this
						.getFieldEditorParent());
		fieldEditorSmallStep.setValidRange(1, 1000000000);
		this.addField(fieldEditorSmallStep);

		fieldEditorBigStep = new IntegerFieldEditor(PreferenceConstants.P_DEBUGGER_BIG_STEP,
				"Number of events between two checkpoints for a big step", this
						.getFieldEditorParent());
		fieldEditorBigStep.setValidRange(1, 1000000000);
		this.addField(fieldEditorBigStep);

		fieldEditorProcessCount = new IntegerFieldEditor(
				PreferenceConstants.P_DEBUGGER_PROCESS_COUNT,
				"Maximum number of processes used for checkpoints", this.getFieldEditorParent());
		fieldEditorProcessCount.setValidRange(1, 1000000000);
		this.addField(fieldEditorProcessCount);
	}

	public void init(IWorkbench workbench) {
	}
}
