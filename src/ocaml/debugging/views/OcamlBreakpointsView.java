package ocaml.debugging.views;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.debugging.OcamlDebugger;
import ocaml.util.ImageRepository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

/** Implements a view to show defined breakpoints and delete them */
public class OcamlBreakpointsView extends ViewPart {

	public static final String ID = "Ocaml.BreakpointsView";
	
	private Composite composite;
	private List list;
	
	Pattern patternBreakpoint = Pattern.compile("(\\d+) .*? \\(\\d+: \\d+-\\d+\\)");
	
	public void addBreakpoint(int num, int address, String filename, int line, int charBegin, int charEnd){
		list.add(num + " " + filename + " (" + line + ": " + charBegin + "-" + charEnd + ")");
	}
	
	public void removeAllBreakpoints() {
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
		
		IActionBars actionBars = this.getViewSite().getActionBars();
		//IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		
		ImageDescriptor iconDelete = ImageRepository.getImageDescriptor(ImageRepository.ICON_DELETE);
		Action actionDelete = new Action("Delete Breakpoint", iconDelete) {
			@Override
			public void run() {
				String[] selection = list.getSelection();
				if(selection.length >= 1){
					Matcher matcher = patternBreakpoint.matcher(selection[0]);
					if(matcher.find()){
						OcamlDebugger debugger = OcamlDebugger.getInstance();
						if(debugger.isReady()){
							debugger.removeBreakpoint(Integer.parseInt(matcher.group(1)));
							list.remove(list.getSelectionIndex());
						}
					}
				}
			}
		};

		ImageDescriptor iconDeleteAll = ImageRepository.getImageDescriptor(ImageRepository.ICON_DELETEALL);
		Action actionDeleteAll = new Action("Delete All Breakpoints", iconDeleteAll) {
			@Override
			public void run() {
				OcamlDebugger debugger = OcamlDebugger.getInstance();
				debugger.removeBreakpoints();
			}
		};

		toolBarManager.add(actionDelete);
		toolBarManager.add(actionDeleteAll);
		
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
