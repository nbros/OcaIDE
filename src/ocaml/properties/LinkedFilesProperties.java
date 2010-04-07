package ocaml.properties;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A property page which gives the user a way to tell the plug-in which object file to link with an external
 * file.
 */
public class LinkedFilesProperties extends PropertyPage {

	/** The default list height */
	private int LIST_HEIGHT = 50;

	/** The file associated with this property page */
	private IFile file;

	private Label lblChoose;

	/** A dialog box to choose a file */
	private FileDialog chooseFileDialog;

	private Button chooseFileButton;

	private Button removeFileButton;

	private Button upButton;

	private Button downButton;

	/** The list of already linked files */
	private List objectsList;

	private void addFirstSection(Composite parent) {
		final Composite composite = createDefaultComposite(parent);

		lblChoose = new Label(composite, SWT.NULL);
		lblChoose.setText("Choose the object files (cmo,cma,cmx,cmxa)\nto link with " + file.getName()
				+ " (list is ordered)");
		lblChoose.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		chooseFileDialog = new FileDialog(parent.getShell(), SWT.OPEN);
		// We are only looking for object files
		chooseFileDialog.setFilterExtensions(new String[] { "*.cmo", "*.cma", "*.cmx", "*.cmxa" });
		chooseFileDialog.setFilterPath(file.getLocation().toOSString());

		objectsList = new List(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		final GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
		listData.heightHint = LIST_HEIGHT;
		objectsList.setLayoutData(listData);
		objectsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (objectsList.getSelectionCount() == 0) {
					removeFileButton.setEnabled(false);
					upButton.setEnabled(false);
					downButton.setEnabled(false);
				} else {
					if (objectsList.isSelected(0)) {
						upButton.setEnabled(false);
					} else {
						upButton.setEnabled(true);
					}
					if (objectsList.isSelected(objectsList.getItemCount() - 1)) {
						downButton.setEnabled(false);
					} else {
						downButton.setEnabled(true);
					}
					removeFileButton.setEnabled(true);
				}
			}

		});

		chooseFileButton = new Button(composite, SWT.PUSH);
		chooseFileButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		chooseFileButton.setText("A&dd ...");
		chooseFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String str_path = chooseFileDialog.open();
				if (str_path != null) {
					final IPath path = new Path(str_path);
					// We only want the last segment (filename)
					objectsList.add(path.lastSegment());
				}
			}
		});

		// a button to remove a flag
		removeFileButton = new Button(composite, SWT.PUSH);
		removeFileButton.setText("&Remove");
		removeFileButton.setEnabled(false);
		removeFileButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeFileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				objectsList.remove(objectsList.getSelectionIndices());
			}
		});

		upButton = new Button(composite, SWT.PUSH);
		upButton.setText("&Up");
		upButton.setEnabled(false);
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		upButton.addSelectionListener(new SelectionAdapter() {
			// this only works if the first item is not selected
			public void widgetSelected(SelectionEvent e) {
				final int[] indices = objectsList.getSelectionIndices();
				for (int i : indices) {
					final String tempFile = objectsList.getItem(i - 1);
					objectsList.setItem(i - 1, objectsList.getItem(i));
					objectsList.setItem(i, tempFile);
					objectsList.select(i - 1);
					objectsList.deselect(i);
				}
				// reactivate buttons depending on changes
				if (objectsList.isSelected(0)) {
					upButton.setEnabled(false);
				}
				if (!objectsList.isSelected(objectsList.getItemCount() - 1)) {
					downButton.setEnabled(true);
				}
				objectsList.setFocus();
			}
		});

		downButton = new Button(composite, SWT.PUSH);
		downButton.setText("D&own");
		downButton.setEnabled(false);
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		downButton.addSelectionListener(new SelectionAdapter() {
			// this only works if the last item is not selected
			public void widgetSelected(SelectionEvent e) {
				final int[] indices = objectsList.getSelectionIndices();
				// We look through the list backwards
				final int[] revertedIndices = new int[indices.length];
				for (int j = 0; j < indices.length; j++) {
					revertedIndices[indices.length - j - 1] = indices[j];
				}
				for (int i : revertedIndices) {
					final String tempFile = objectsList.getItem(i + 1);
					objectsList.setItem(i + 1, objectsList.getItem(i));
					objectsList.setItem(i, tempFile);
					objectsList.select(i + 1);
					objectsList.deselect(i);
				}
				// reactivate buttons depending on changes
				if (objectsList.isSelected(objectsList.getItemCount() - 1)) {
					downButton.setEnabled(false);
				}
				if (!objectsList.isSelected(0)) {
					upButton.setEnabled(true);
				}
				objectsList.setFocus();
			}
		});
	}

	/**
	 * This method is called when the property page is opened.<br>
	 * Search and display the names of the object files to link, that must be in a persistent property
	 * 
	 */
	private void load() {
		objectsList.removeAll();
		final java.util.List<String> objFiles = OcamlBuilder.getExternalObjectFiles(file);
		if (objFilesStored == null) {
			objFilesStored = objFiles;
		}
		for (String objFile : objFiles) {
			objectsList.add(objFile);
		}
	}

	/** To detect changes */
	protected java.util.List<String> objFilesStored = null;

	/**
	 * Initialize a composite from its parent, and set the layout.
	 */
	private Composite createDefaultComposite(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		final GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	@Override
	protected Control createContents(Composite parent) {
		// Associate the selected file to this property page

		// This cast should be safe
		this.file = (IFile) getElement();
		// Create a composite from the parent, and set its layout
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		composite.setLayoutData(data);

		addFirstSection(composite);

		load();
		return composite;
	}

	@Override
	protected void performDefaults() {
		load();
	}

	/**
	 * Add the name as a persistent property.
	 */
	@Override
	public boolean performOk() {
		// The object files from the list, to add as a persistent property
		final java.util.List<String> objFiles = new ArrayList<String>(objectsList.getItemCount());
		for (String objFile : objectsList.getItems()) {
			objFiles.add(objFile);
		}
		OcamlBuilder.setExternalsObjectsFiles(file, objFiles);

		// We touch the file to notify the compiler of a change
		if (!objFiles.equals(objFilesStored)) {
			try {
				file.touch(null);
			} catch (CoreException e) {
				OcamlPlugin.logError("error in LinkedFilesProperties:performOk:" + " error touching file", e);

			}
		}

		// Set it back to null for next time
		objFilesStored = null;
		return true;

	}

}
