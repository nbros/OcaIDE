package ocaml.launching;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/** Represents the tabs on the options panel for the configuration of an O'Caml launch configuration */
public class OcamlLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

	public OcamlLaunchConfigurationTabGroup() {
	}

	/** Create the tabs (the "common tab" must always be there, according to Eclipse help) */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new OcamlLaunchTab(),
				new OcamlDebugTab(),
				new CommonTab()		
			};
		this.setTabs(tabs);	
		
	}

}
