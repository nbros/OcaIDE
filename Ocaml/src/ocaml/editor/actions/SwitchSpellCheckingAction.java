package ocaml.editor.actions;

import ocaml.OcamlPlugin;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class SwitchSpellCheckingAction implements IWorkbenchWindowActionDelegate {

	public static final String ID = "Ocaml_sourceActions_SwitchSpellChecker";
	
	public void run(IAction action) {
		IPreferenceStore preferenceStore = OcamlPlugin.getInstance().getPreferenceStore();
		if (preferenceStore.getBoolean(PreferenceConstants.P_EDITOR_SPELL_CHECKING)) {
			preferenceStore.setValue(PreferenceConstants.P_EDITOR_SPELL_CHECKING, false);
			//action.setChecked(false);
		} else {
			preferenceStore.setValue(PreferenceConstants.P_EDITOR_SPELL_CHECKING, true);
			//action.setChecked(true);
		}
	}

	public void init(IWorkbenchWindow window) {
	}

	public void dispose() {
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
