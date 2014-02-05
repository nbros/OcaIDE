package ocaml.debugging.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.part.ViewPart;

import ocaml.debugging.OcamlDebugger;
/** Implements a view to show the call stack while debugging an OCaml program */
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

		// TRUNG: add hyper-link feature for the Call Stack View
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				int currentIndex = list.getFocusIndex();
				String currentItem = list.getItem(currentIndex);
				Pattern p = Pattern.compile("#[\\d]+[\\s]+Pc:[\\s]+[\\d]+[\\s]+([a-zA-Z0-9_\\.]*)[\\s]+char[\\s]+([\\d]+)");
				Matcher matcher = p.matcher(currentItem);
				if (matcher.find()) {
					String module = matcher.group(1);
					int offset = Integer.parseInt(matcher.group(2));;
					String filename = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
					OcamlDebugger debugger = OcamlDebugger.getInstance();
					debugger.highlight(filename, offset);
				}
			}
		});
	}

	protected void resized() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		list.setBounds(0, 0, width, height);
	}

	@Override
	public void setFocus() {
		//list.setFocus();
	}

}
