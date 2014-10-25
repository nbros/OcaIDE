package ocaml.debugging.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
		int selectionIndex = list.getSelectionIndex();
		list.removeAll();
		for(String e : elements)
			list.add(e);
		list.setRedraw(true);
		if (selectionIndex >= 0 && selectionIndex < elements.length) {
			list.setSelection(selectionIndex);
		}
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
		list.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int currentIndex = list.getFocusIndex();
				String currentItem = list.getItem(currentIndex);
				OcamlDebugger debugger = OcamlDebugger.getInstance();

				Matcher userFriendlyMatcher = OcamlCallStackView.patternCallstack.matcher(currentItem);
				if (userFriendlyMatcher.find()) {
					int frame = Integer.parseInt(userFriendlyMatcher.group(1));
					String module = userFriendlyMatcher.group(2);
					int line = Integer.parseInt(userFriendlyMatcher.group(3));
					int column = Integer.parseInt(userFriendlyMatcher.group(4));
					String filename = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
					debugger.highlight(filename, line, column);
					debugger.setFrame(frame);
					return;
				}

				Matcher rawMatcher = OcamlDebugger.patternCallstack.matcher(currentItem);
				if (rawMatcher.find()) {
					int frame = Integer.parseInt(rawMatcher.group(1));
					String module = rawMatcher.group(2);
					int offset = Integer.parseInt(rawMatcher.group(3));
					String filename = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
					debugger.highlight(filename, offset);
					debugger.setFrame(frame);
					return;
				}

				debugger.errorMessage("Cannot jump to stack frame.");
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

	private static final Pattern patternCallstack =
			Pattern.compile("\\A#(\\d+)\\s+-\\s+(\\w+)\\.\\w+\\s+-\\s+\\((\\d+)\\s*:\\s*(\\d+)\\)");
}
