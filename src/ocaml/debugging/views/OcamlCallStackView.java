package ocaml.debugging.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.part.ViewPart;

/** Implements a view to show the call stack while debugging an O'Caml program */
public class OcamlCallStackView extends ViewPart{

	public static final String ID = "Ocaml.CallStackView";
	
	private Composite composite;
	private List list;
	
	public void setCallStack(String elements[]){
		list.setRedraw(false);
		list.removeAll();
		for(String e : elements)
			list.add(e);
		list.setRedraw(true);
	}
	
	public void empty(){
		list.removeAll();
	}

	@Override
	public void createPartControl(Composite parent) {
		composite = new Composite(parent, SWT.NONE);
		composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				resized();
			}
		});

		list = new List(composite, SWT.SINGLE | SWT.V_SCROLL);
	}

	protected void resized() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		list.setBounds(0, 0, width, height);
	}

	@Override
	public void setFocus() {
		list.setFocus();
	}

}
