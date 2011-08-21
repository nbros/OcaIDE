package ocaml.properties;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Properties page to allow the user to give a list of paths to use in the selected project. These paths are
 * used to find the files to compile, and the order is important in case of two files having the same name.
 * <p>
 * The paths are stored as a project persistent property, in a string:
 * 
 * <pre>
 *           &quot;/path/number/one&quot;;&quot;/path/number/two&quot;;...
 * </pre>
 */
public class OcamlProjectPathsProperties extends PropertyPage implements IWorkbenchPropertyPage {

	public OcamlProjectPathsProperties() {
	}

	private int BUTTON_WIDTH = 80;

	private IProject project = null;

	private Composite composite = null;

	private List pathsList;

	private Button addButton;

	private Button changeButton;

	private Button removeButton;

	private Button upButton;

	private Button downButton;

	private Button browseButton;

	private Text pathText;

	private Label pathLabel;

	/** Create the contents of the page */
	@Override
	protected Control createContents(Composite parent) {

		IResource element = (IResource) this.getElement();
		if (element instanceof IProject) {
			this.project = (IProject) element;
		} else
			OcamlPlugin.logError("Ocaml project paths property page: project=null");

		// use 3 columns
		this.composite = createDefaultComposite(parent, 3);

		pathText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		pathText.setToolTipText("Type in the absolute or relative "
				+ "path of a folder or click browse to find it");
		pathText.setEditable(true);

		addButton = new Button(composite, SWT.PUSH);
		addButton.setText("&Add");
		addButton.setToolTipText("Add this directory to the list");
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addClicked();
			}
		});
		addButton.setToolTipText("Adds the folder specified in the \"path\" textbox if it exists.");

		browseButton = new Button(composite, SWT.PUSH);
		browseButton.setImage(Misc.createIcon("browse.gif"));
		browseButton.setToolTipText("Browse for a directory");
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browseClicked();
			}
		});

		pathsList = new List(composite, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true, 0, 4);
		// listData.verticalAlignment = SWT.FILL;
		listData.heightHint = 300;
		pathsList.setLayoutData(listData);
		pathsList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listSelected();
			}
		});
		initPathsList();

		changeButton = new Button(composite, SWT.PUSH);
		changeButton.setText("&Change");
		final GridData changeData = new GridData(SWT.BEGINNING, SWT.TOP, false, false, 2, 0);
		changeData.widthHint = BUTTON_WIDTH;
		changeButton.setLayoutData(changeData);
		changeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changeClicked();
			}
		});
		changeButton
				.setToolTipText("Change the selected folder to the folder specified in the \"path\" textbox if it exists.");

		removeButton = new Button(composite, SWT.PUSH);
		removeButton.setText("&Remove");
		removeButton.setToolTipText("Remove the selected path from the list.");

		final GridData removeData = new GridData(SWT.BEGINNING, SWT.TOP, false, false, 2, 0);
		removeData.widthHint = BUTTON_WIDTH;
		removeButton.setLayoutData(changeData);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeClicked();
			}
		});
		upButton = new Button(composite, SWT.PUSH);
		upButton.setText("&Up");
		final GridData upData = new GridData(SWT.BEGINNING, SWT.TOP, false, false, 2, 0);
		upData.widthHint = BUTTON_WIDTH;
		upButton.setLayoutData(upData);
		upButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				upClicked();
			}
		});
		downButton = new Button(composite, SWT.PUSH);
		downButton.setText("&Down");
		final GridData downData = new GridData(SWT.BEGINNING, SWT.TOP, false, false, 2, 0);
		downData.widthHint = BUTTON_WIDTH;
		downButton.setLayoutData(downData);
		downButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				downClicked();
			}
		});

		pathLabel = new Label(composite, SWT.LEFT);
		pathLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));

		updateButtonsState();

		return composite;
	}

	/**
	 * Initialize a composite from its parent, and use a Grid layout with <code>numCols</code> columns.
	 */
	private Composite createDefaultComposite(Composite parent, int numCols) {
		final Composite composite = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = numCols;
		composite.setLayout(layout);
		GridData data = new GridData(SWT.FILL, SWT.TOP, true, true);
		composite.setLayoutData(data);
		return composite;
	}

	private void initPathsList() {
		pathsList.removeAll();
		OcamlPaths paths = new OcamlPaths(project);
		for (String path : paths.getPaths())
			pathsList.add(path);
		// the first time only
		if (pathsListStored == null) {
			pathsListStored = new ArrayList<String>(pathsList.getItemCount());
			int i;
			for (i = 0; i < pathsList.getItemCount(); i++)
				pathsListStored.add(pathsList.getItem(i));
		}

	}

	/** To detect changes when OK is clicked */
	protected java.util.List<String> pathsListStored = null;

	/** Selection of an item in the list */
	protected void listSelected() {
		int[] selection = pathsList.getSelectionIndices();

		if (selection.length > 0) {
			int iSelected = selection[0];
			if (iSelected >= 0) {
				String selected = pathsList.getItem(iSelected);
				pathText.setText(selected);
			}
		}

		updateButtonsState();
	}

	/** Click on the browse button: we display a directory dialog */
	protected void browseClicked() {
		DirectoryDialog directoryDialog = new DirectoryDialog(composite.getShell(), SWT.OPEN);
		directoryDialog.setFilterPath(this.project.getLocation().toOSString());
		String path = directoryDialog.open();
		if (path != null) {
			pathText.setText(path.trim());
		}
	}

	/** Click on add: we add the contents of the text box (if valid) to the list	 */
	protected void addClicked() {
		String path = pathText.getText().trim();

		if (path.equals("")) {
			MessageDialog.openInformation(composite.getShell(), "Empty field",
					"Please specify a directory first either by entering it manually\n"
							+ "in the \"path\" field or by clicking browse... ");
			return;
		}

		if (OcamlPaths.isValidPath(this.project, path)) {
			for (String item : pathsList.getItems())
				if (item.equals(path)) {
					MessageDialog.openInformation(composite.getShell(), "Directory already in the list",
							"The directory \"" + path + "\" is already in the list.");
					updateButtonsState();
					return;
				}
			pathsList.add(path, 0);
			pathsList.getVerticalBar().setSelection(0);
			// pathLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		} else
			MessageDialog.openError(composite.getShell(), "Not a valid directory", "\"" + path
					+ "\" is not a valid directory.");

		updateButtonsState();
	}

	/** Click on the "Change" button: we replace the element selected in the list by the path in the text box */
	protected void changeClicked() {
		String path = pathText.getText().trim();

		for (String item : pathsList.getItems())
			if (item.equals(path)) {
				MessageDialog.openInformation(composite.getShell(), "Directory already in the list",
						"The directory \"" + path + "\" is already in the list.");
				updateButtonsState();
				return;
			}

		if (!OcamlPaths.isValidPath(this.project, path))
			MessageDialog.openError(composite.getShell(), "Not a valid directory", "\"" + path
					+ "\" is not a valid directory.");
		else {
			int[] selection = pathsList.getSelectionIndices();
			if (selection.length > 0) {
				pathsList.setItem(selection[0], path);
				pathLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
			} else
				MessageDialog.openError(composite.getShell(), "Selection is empty",
						"Please select an item first.");
		}

		updateButtonsState();
	}

	/** Click on "up": move the list item up */
	protected void upClicked() {
		int[] selection = pathsList.getSelectionIndices();

		if (selection.length > 0) {
			int iSelected = selection[0];
			if (iSelected > 0) {
				String selected = pathsList.getItem(iSelected);
				String previous = pathsList.getItem(iSelected - 1);
				pathsList.setItem(iSelected - 1, selected);
				pathsList.setItem(iSelected, previous);
				pathsList.deselect(iSelected);
				pathsList.select(iSelected - 1);
			}
		} else
			MessageDialog.openError(composite.getShell(), "Selection is empty",
					"Please select an item first.");

		updateButtonsState();
	}

	/** Click on "down": move the list item down */
	protected void downClicked() {
		int[] selection = pathsList.getSelectionIndices();

		if (selection.length > 0) {
			int iSelected = selection[0];
			if (iSelected < pathsList.getItemCount() - 1) {
				String selected = pathsList.getItem(iSelected);
				String next = pathsList.getItem(iSelected + 1);
				pathsList.setItem(iSelected, next);
				pathsList.setItem(iSelected + 1, selected);
				pathsList.deselect(iSelected);
				pathsList.select(iSelected + 1);
			}
		} else
			MessageDialog.openError(composite.getShell(), "Selection is empty",
					"Please select an item first.");

		updateButtonsState();
	}

	/** Click on remove: remove the selected item */
	protected void removeClicked() {
		int[] selection = pathsList.getSelectionIndices();

		if (selection.length > 0) {
			int iSelected = selection[0];
			pathsList.remove(iSelected);
			if (pathsList.getItemCount() > 0)
				pathsList.select(Math.min(iSelected, pathsList.getItemCount() - 1));

		} else
			MessageDialog.openError(composite.getShell(), "Selection is empty",
					"Please select an item first.");

		updateButtonsState();
	}

	/** Activate or inactivate the buttons depending on the context */
	private void updateButtonsState() {
		int[] selection = pathsList.getSelectionIndices();

		upButton.setEnabled(selection.length > 0 && selection[0] > 0);
		downButton.setEnabled(selection.length > 0 && selection[0] < pathsList.getItemCount() - 1);
		removeButton.setEnabled(selection.length > 0);
		changeButton.setEnabled(selection.length > 0);
	}

	/** OK was clicked */
	@Override
	public boolean performOk() {
		OcamlPaths paths = new OcamlPaths(project);
		paths.setPaths(pathsList.getItems());

		// we compare the old and new list
		final java.util.List<String> newPathsList = new ArrayList<String>(pathsList.getItemCount());
		for (int i = 0; i < pathsList.getItemCount(); i++) {
			newPathsList.add(pathsList.getItem(i));
		}

		if (!newPathsList.equals(pathsListStored)) {
			/* If the flags are not the same, we recompile the whole project in a background thread */
			if (project != null) {
				Job job = new Job("Build project") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						} catch (CoreException e) {
							OcamlPlugin.logError("error in " + "OcamlProjectPathsProperties:performOk: "
									+ "error rebuilding project", e);
						}
						// refresh the file system
						Misc.refreshFileSystem(project, monitor);
						return Status.OK_STATUS;
					}

				};
				job.setPriority(Job.BUILD);
				job.setUser(true);
				job.schedule(500);
			}
		}

		// restore to null for next time this page is opened
		pathsListStored = null;
		return true;
	}

	/** restore the defaults: O'Caml library + project directories */
	@Override
	protected void performDefaults() {
		OcamlPaths paths = new OcamlPaths(project);
		paths.restoreDefaults();
		// reinitialize the list to update it
		initPathsList();
		super.performDefaults();
	}

}
