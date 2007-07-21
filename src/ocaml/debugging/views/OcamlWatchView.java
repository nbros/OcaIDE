package ocaml.debugging.views;

import ocaml.OcamlPlugin;
import ocaml.debugging.OcamlDebugger;
import ocaml.util.ImageRepository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;

/**
 * Implements a view to show and add variables to watch while debugging an O'Caml program. After each step of
 * the debugger, the value of each of these variables will be evaluated and shown in this view. This can be
 * slow if there are too many variables, so the user is advised to delete unneeded variables from this list.
 */
public class OcamlWatchView extends ViewPart {
	public static final String ID = "Ocaml.WatchView";

	private Composite composite;

	private List list;

	private Text text;

	/** Return the list of variables to watch */
	public String[] getVariables() {
		String items[] = list.getItems();
		String variables[] = new String[items.length];

		for (int i = 0; i < items.length; i++) {
			String element = items[i];

			int idx = element.indexOf(':');
			String var = "";
			if (idx != -1) {
				var = element.substring(0, idx).trim();
			}

			variables[i] = var;
		}

		return variables;
	}

	/**
	 * Fill the list of variables with the associated values.
	 * 
	 * @param variables
	 *            a list of strings (format: "var: type = value") or error messages from the debugger
	 */
	public void setVariables(String[] variables) {
		if (variables.length != list.getItems().length) {
			OcamlPlugin.logError("OcamlWatchView:setVariables wrong length");
			return;
		}

		for (int i = 0; i < variables.length; i++) {
			String message = variables[i];
			int idx = message.indexOf(':');
			String var = "";
			if (idx != -1)
				var = message.substring(0, idx).trim();

			String oldMessage = list.getItem(i);
			String oldvar = "";
			idx = oldMessage.indexOf(':');
			if (idx != -1)
				oldvar = oldMessage.substring(0, idx).trim();

			if (var.equals(oldvar))
				list.setItem(i, message.trim());
			else {
				if (message.trim().startsWith("Unbound identifier "))
					list.setItem(i, oldvar + " : ");
				else
					list.setItem(i, oldvar + " : " + message.trim());
			}
		}
	}

	public void clearVariables() {
		for (int i = 0; i < list.getItemCount(); i++) {
			String item = list.getItem(i);

			int idx = item.indexOf(':');
			String var = "";
			if (idx != -1)
				var = item.substring(0, idx).trim();

			if (!var.equals(""))
				list.setItem(i, var + " : ");
		}
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

		list = new List(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		list.addMouseListener(new MouseAdapter() {

			public void mouseDoubleClick(MouseEvent e) {
				String[] sel = list.getSelection();
				if (sel.length > 0) {
					MessageDialog.openInformation(null, "Value", sel[0]);
				}
			}
		});

		text = new Text(composite, SWT.NONE);

		text.setBackground(new Color(text.getDisplay(), 255, 255, 220));

		text.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {

				if (e.character == '\r') {
					String var = text.getText().trim();
					if (!var.equals("") && !var.contains(":")) {
						list.add(var + " : ");
						OcamlDebugger.getInstance().addWatchVariable(var);

						if (OcamlDebugger.getInstance().isStarted())
							OcamlDebugger.getInstance().displayWatchVars();
					}
					text.setText("");
				}
			}
		});

		IActionBars actionBars = this.getViewSite().getActionBars();
		// IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		ImageDescriptor iconDelete = ImageRepository.getImageDescriptor(ImageRepository.ICON_DELETE);
		Action actionDelete = new Action("Delete Watch", iconDelete) {
			@Override
			public void run() {
				OcamlDebugger.getInstance().removeWatchVariables(list.getSelectionIndices());
				list.remove(list.getSelectionIndices());
			}
		};

		ImageDescriptor iconDeleteAll = ImageRepository.getImageDescriptor(ImageRepository.ICON_DELETEALL);
		Action actionDeleteAll = new Action("Delete All Watches", iconDeleteAll) {
			@Override
			public void run() {
				OcamlDebugger.getInstance().removeAllWatchVariables();
				list.removeAll();
			}
		};

		toolBarManager.add(actionDelete);
		toolBarManager.add(actionDeleteAll);
	}

	protected void resized() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		list.setBounds(0, 0, width, height - 20);
		text.setBounds(0, height - 19, width, 19);
	}

	@Override
	public void setFocus() {
		list.setFocus();
	}

}
