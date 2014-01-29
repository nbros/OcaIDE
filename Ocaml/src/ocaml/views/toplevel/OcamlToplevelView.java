package ocaml.views.toplevel;

import java.util.Random;

import ocaml.OcamlPlugin;
import ocaml.util.ImageRepository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/** Implements the OCaml top-level view */
public class OcamlToplevelView extends ViewPart {

	/**
	 * Start the toplevel immediately when the view is created. Overridden in the CustomToplevel class to
	 * launch custom toplevels
	 */
	protected boolean bStartWhenCreated = true;

	public static final String ID = "Ocaml.ocamlToplevelView";
	protected String secondaryId = null;
	
	public static int nLastPageOpen = 1;

	public OcamlToplevelView() {
	}

	/** Change the view's tab title */
	public void setTabTitle(String title) {
		setPartName(title);
	}

	public static void eval(String expression) {
		final OcamlToplevelView lastFocusedInstance = OcamlPlugin.getLastFocusedToplevelInstance();
		if (lastFocusedInstance == null) {
			Shell shell = Display.getDefault().getActiveShell();
			MessageDialog.openInformation(shell, "Ocaml Plugin", "Please open a toplevel view first.");
			return;
		}

		lastFocusedInstance.toplevel.eval(expression);
	}

	private Composite composite;
	private StyledText userText;
	private StyledText resultText;
	private Sash sash;

	protected Toplevel toplevel;

	@Override
	public void createPartControl(Composite parent) {

		this.setPartName("Ocaml Toplevel");

		composite = new Composite(parent, SWT.BORDER);
		composite.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				resized();
			}
		});

		userText = new StyledText(this.composite, SWT.V_SCROLL);
		userText.setWordWrap(true);
		resultText = new StyledText(this.composite, SWT.V_SCROLL);
		resultText.setWordWrap(true);

		sash = new Sash(composite, SWT.HORIZONTAL | SWT.SMOOTH);

		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sash.setBounds(e.x, e.y, e.width, e.height);
				layout();
			}
		});

		Color c = Display.findDisplay(Thread.currentThread()).
				getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		sash.setBackground(c);

		toplevel = new Toplevel(this, userText, resultText);

		IActionBars actionBars = this.getViewSite().getActionBars();
		IMenuManager dropDownMenu = actionBars.getMenuManager();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		ImageDescriptor iconAdd = ImageRepository.getImageDescriptor(ImageRepository.ICON_ADD);
		Action actionNewToplevel = new Action("New toplevel", iconAdd) {
			@Override
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					OcamlToplevelView.this.secondaryId = "ocamltoplevelview" + new Random().nextInt();
					page.showView(ID, OcamlToplevelView.this.secondaryId,
							IWorkbenchPage.VIEW_ACTIVATE);
				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		};

		ImageDescriptor iconReset = ImageRepository.getImageDescriptor(ImageRepository.ICON_RESET);
		Action actionReset = new Action("Reset", iconReset) {
			@Override
			public void run() {
				toplevel.reset();
			}
		};

		ImageDescriptor iconInterrupt = ImageRepository.getImageDescriptor(ImageRepository.ICON_INTERRUPT);
		Action actionBreak = new Action("Interrupt", iconInterrupt) {
			@Override
			public void run() {
				toplevel.interrupt();
			}
		};

		ImageDescriptor iconClear = ImageRepository.getImageDescriptor(ImageRepository.ICON_CLEAR);
		Action actionClear = new Action("Clear", iconClear) {
			@Override
			public void run() {
				toplevel.clear();
			}
		};

		ImageDescriptor iconHelp = ImageRepository.getImageDescriptor(ImageRepository.ICON_HELP);
		Action actionHelp = new Action("Help", iconHelp) {
			@Override
			public void run() {
				toplevel.help();
			}
		};

		Action actionUseTopfind = new Action("Use Topfind") {
			@Override
			public void run() {
				toplevel.eval("#use \"topfind\";;");
			}
		};

		dropDownMenu.add(actionUseTopfind);

		toolBarManager.add(actionBreak);
		toolBarManager.add(actionClear);
		toolBarManager.add(actionReset);
		toolBarManager.add(actionNewToplevel);
		toolBarManager.add(actionHelp);

		if (bStartWhenCreated)
			toplevel.start();

		OcamlPlugin.setLastFocusedToplevelInstance(this);
	}

	private final double defaultRatio = 0.8;

	protected void resized() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		int userTextHeight = (int) (height * (1 - defaultRatio));
		if (userTextHeight < 25)
			userTextHeight = 25;

		resultText.setBounds(0, 0, width, height - userTextHeight - 3);
		userText.setBounds(0, height - userTextHeight, width, userTextHeight);
		sash.setBounds(0, height - userTextHeight - 3, width, 3);
	}

	protected void layout() {
		Rectangle parentArea = this.composite.getClientArea();
		int width = parentArea.width;
		int height = parentArea.height;

		Rectangle sashBounds = sash.getBounds();

		resultText.setBounds(0, 0, width, sashBounds.y);
		userText.setBounds(0, sashBounds.y + 3, width, height - sashBounds.y - 3);
		sash.setBounds(0, sashBounds.y, width, 3);
	}

	@Override
	public void init(IViewSite site) throws PartInitException {
		OcamlPlugin.setLastFocusedToplevelInstance(this);
		super.init(site);
	}

	@Override
	public void setFocus() {
		OcamlPlugin.setLastFocusedToplevelInstance(this);
		userText.setFocus();
	}

	@Override
	public void dispose() {
		toplevel.kill();
		toplevel.dispose();
		super.dispose();

		if (this.equals(OcamlPlugin.getLastFocusedToplevelInstance()))
			OcamlPlugin.setLastFocusedToplevelInstance(null);
	}
	
	public String getSecondaryId() {
		return secondaryId;
	}
}
