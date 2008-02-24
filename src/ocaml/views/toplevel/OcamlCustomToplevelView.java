package ocaml.views.toplevel;

import org.eclipse.swt.widgets.Composite;

/** Extends the O'Caml top-level view to provide custom toplevels */
public class OcamlCustomToplevelView extends OcamlToplevelView {
	
	public OcamlCustomToplevelView() {
		this.bStartWhenCreated = false;
	}

	public static final String ID = "Ocaml.ocamlCustomToplevelView";

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		this.setPartName("Custom Toplevel");
	}
	
	public void start(String path){
		toplevel.setToplevelPath(path);
		toplevel.start();
	}

	public void setSecondaryId(String secondaryId) {
		this.secondaryId = secondaryId;
	}
}
