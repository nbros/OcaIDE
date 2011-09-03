package ocaml.views;

import ocaml.OcamlPlugin;
import ocaml.util.ImageRepository;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * A view to display the output of the OCaml compiler, and the make command
 * 
 * @see ViewPart
 */
public class OcamlCompilerOutput extends ViewPart {
	public static final String ID = "ocaml.ocamlCompilerOutput";

	private TextViewer textViewer = null;

	public OcamlCompilerOutput() {
	}

	@Override
	public void createPartControl(Composite parent) {
		this.textViewer = new TextViewer(parent, SWT.WRAP | SWT.V_SCROLL);
		GridData viewerData = new GridData(GridData.FILL_BOTH);
		this.textViewer.getControl().setLayoutData(viewerData);
		this.textViewer.setEditable(false);
		this.textViewer.setDocument(new Document());

		IActionBars actionBars = this.getViewSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();

		ImageDescriptor iconClear = ImageRepository.getImageDescriptor(ImageRepository.ICON_CLEAR);
		Action actionClear = new Action("Clear", iconClear) {
			@Override
			public void run() {
				clear();
			}
		};

		toolBarManager.add(actionClear);

	}

	@Override
	public void setFocus() {
	}

	public void clear() {
		try {
			IDocument doc = this.textViewer.getDocument();
			doc.replace(0, doc.getLength(), "");
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public void appendln(String text) {
		try {
			IDocument doc = this.textViewer.getDocument();
			doc.replace(doc.getLength(), 0, text + "\n");
			this.textViewer.setTopIndex(doc.getNumberOfLines() - 1);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public void append(String text) {
		try {
			IDocument doc = this.textViewer.getDocument();
			doc.replace(doc.getLength(), 0, text);
			this.textViewer.setTopIndex(doc.getNumberOfLines() - 1);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public void newLine() {
		try {
			IDocument doc = this.textViewer.getDocument();
			doc.replace(doc.getLength(), 0, "\n");
			this.textViewer.setTopIndex(doc.getNumberOfLines() - 1);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	/*
	 * public void setOutputText(String text) { this.textViewer.setDocument(new Document()); }
	 */

	/**
	 * Get the OCaml compiler output view (must be called from a UI-Thread).
	 */
	public static OcamlCompilerOutput get() {
		OcamlCompilerOutput console = null;
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			console = (OcamlCompilerOutput) (page.findView(OcamlCompilerOutput.ID));
		} catch (NullPointerException ne) {
			OcamlPlugin.logError("error in OcamlConsole:get:"
					+ " error finding workbench, active window or active page", ne);
		}
		return console;
	}
}