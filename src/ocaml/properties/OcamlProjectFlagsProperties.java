package ocaml.properties;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Implements a properties page for projects, that allows the user to give compilation flags that will be
 * applied to the whole project.
 */
public class OcamlProjectFlagsProperties extends PropertyPage {

	/** The project associated with this properties page */
	private IProject project;

	/** The text field where the user can enter a new flag */
	private Text flagChoiceText;

	/** a button to add a flag */
	private Button addFlagButton;

	/** The list of already chosen flags */
	private List flagsList;

	/** The default height of the list */
	private int LIST_HEIGHT = 100;

	/** A button to remove a flag from the list */
	private Button removeFlagButton;

	private Button changeFlagButton;

	private Text txtChangeItem;

	private Button btnValidateChanges;

	private Button upButton;

	private Button downButton;

	/** Add the first section, to add flags */
	private void addFirstSection(Composite parent) {

		// the group for the first section
		final Group flagsSection = new Group(parent, SWT.NULL);
		flagsSection.setText("Add some command line flags to the whole project");
		flagsSection.setLayout(new GridLayout(2, false));
		flagsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		// the text box
		flagChoiceText = new Text(flagsSection, SWT.SINGLE | SWT.BORDER);
		flagChoiceText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		flagChoiceText.setEditable(true);

		// the add button
		addFlagButton = new Button(flagsSection, SWT.PUSH);
		addFlagButton.setText("A&dd");
		addFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addFlagButton.addSelectionListener(new SelectionAdapter() {
			// La seule chose à faire est de déplacer ce contenu du champ texte
			// dans la liste
			public void widgetSelected(SelectionEvent e) {
				flagsList.add(flagChoiceText.getText());
				flagChoiceText.setText("");
				flagChoiceText.setFocus();
			}
		});

		// the flags list
		flagsList = new List(flagsSection, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		final GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
		listData.heightHint = LIST_HEIGHT;
		flagsList.setLayoutData(listData);
		flagsList.addSelectionListener(new SelectionAdapter() {
			// we only activate the buttons when something is selected in the list
			public void widgetSelected(SelectionEvent e) {
				if (flagsList.getSelectionCount() == 0) {
					changeFlagButton.setEnabled(false);
					removeFlagButton.setEnabled(false);
					upButton.setEnabled(false);
					downButton.setEnabled(false);
				} else {
					if (flagsList.isSelected(0)) {
						upButton.setEnabled(false);
					} else {
						upButton.setEnabled(true);
					}
					if (flagsList.isSelected(flagsList.getItemCount() - 1)) {
						downButton.setEnabled(false);
					} else {
						downButton.setEnabled(true);
					}
					changeFlagButton.setEnabled(true);
					removeFlagButton.setEnabled(true);
				}
			}

		});

		// remove flag button
		removeFlagButton = new Button(flagsSection, SWT.PUSH);
		removeFlagButton.setText("&Remove");
		removeFlagButton.setEnabled(false);
		removeFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeFlagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flagsList.remove(flagsList.getSelectionIndices());
			}
		});

		// change flag button
		changeFlagButton = new Button(flagsSection, SWT.PUSH);
		changeFlagButton.setText("&Change");
		changeFlagButton.setEnabled(false);
		changeFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		changeFlagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final int selectionIndex = flagsList.getSelectionIndex();
				txtChangeItem.setText(flagsList.getItem(selectionIndex));
				txtChangeItem.setVisible(true);
				btnValidateChanges.setVisible(true);
				txtChangeItem.setFocus();
			}
		});

		upButton = new Button(flagsSection, SWT.PUSH);
		upButton.setText("&Up");
		upButton.setEnabled(false);
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		upButton.addSelectionListener(new SelectionAdapter() {
			// only works if the first item is not selected
			public void widgetSelected(SelectionEvent e) {
				final int[] indices = flagsList.getSelectionIndices();
				for (int i : indices) {
					final String tempFlag = flagsList.getItem(i - 1);
					flagsList.setItem(i - 1, flagsList.getItem(i));
					flagsList.setItem(i, tempFlag);
					flagsList.select(i - 1);
					flagsList.deselect(i);
				}
				// reactivate the buttons depending on the changes
				if (flagsList.isSelected(0)) {
					upButton.setEnabled(false);
				}
				if (!flagsList.isSelected(flagsList.getItemCount() - 1)) {
					downButton.setEnabled(true);
				}
				flagsList.setFocus();
			}
		});

		downButton = new Button(flagsSection, SWT.PUSH);
		downButton.setText("D&own");
		downButton.setEnabled(false);
		downButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		downButton.addSelectionListener(new SelectionAdapter() {
			// only works if the last item is not selected
			public void widgetSelected(SelectionEvent e) {
				final int[] indices = flagsList.getSelectionIndices();
				// we go through the list in reverse
				final int[] revertedIndices = new int[indices.length];
				for (int j = 0; j < indices.length; j++) {
					revertedIndices[indices.length - j - 1] = indices[j];
				}
				for (int i : revertedIndices) {
					final String tempFlag = flagsList.getItem(i + 1);
					flagsList.setItem(i + 1, flagsList.getItem(i));
					flagsList.setItem(i, tempFlag);
					flagsList.select(i + 1);
					flagsList.deselect(i);
				}
				// reactivate the buttons depending on the changes
				if (flagsList.isSelected(flagsList.getItemCount() - 1)) {
					downButton.setEnabled(false);
				}
				if (!flagsList.isSelected(0)) {
					upButton.setEnabled(true);
				}
				flagsList.setFocus();
			}
		});

		// These two widgets appear only when the user modifies the text of a flag

		txtChangeItem = new Text(flagsSection, SWT.SINGLE | SWT.BORDER);
		txtChangeItem.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		txtChangeItem.setEditable(true);
		txtChangeItem.setVisible(false);

		btnValidateChanges = new Button(flagsSection, SWT.PUSH);
		btnValidateChanges.setText("O&k");
		btnValidateChanges.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnValidateChanges.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flagsList.setItem(flagsList.getSelectionIndex(), txtChangeItem.getText());
				txtChangeItem.setVisible(false);
				btnValidateChanges.setVisible(false);
				flagsList.setFocus();
			}
		});
		btnValidateChanges.setVisible(false);

	}

	/** @see PreferencePage#createContents(Composite) */
	@Override
	protected Control createContents(Composite parent) {
		// Associate the file to this properties page

		// This cast should be safe
		this.project = (IProject) getElement();
		// create a composite from its parent, and define the layout
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		load();

		return composite;
	}

	/** Restore the components on the page to the default values */
	@Override
	protected void performDefaults() {
		final java.util.List<String> defFlags = new ArrayList<String>(Misc.defaultProjectFlags.length);
		for (String flg : Misc.defaultProjectFlags) {
			defFlags.add(flg.trim());
		}
		OcamlBuilder.setResourceFlags(project, defFlags);
		load();
	}

	/**
	 * Initialize the components from the persistent properties.
	 */
	protected void load() {
		flagsList.removeAll();
		final java.util.List<String> flgs = OcamlBuilder.getResourceFlags(project);
		// We only remember the flags when load is called for the first time
		if (this.flagsStored == null) {
			this.flagsStored = flgs;
		}
		for (String flag : flgs) {
			flagsList.add(flag);
		}
	}

	/**
	 * The previous flags list, to compare differences with the new one, to know if there are changes and we
	 * must recompile the project
	 */
	protected java.util.List<String> flagsStored = null;

	@Override
	public boolean performOk() {
		// add the flags as a persistent property
		final java.util.List<String> newFlags = new ArrayList<String>(flagsList.getItemCount());
		for (String newFlag : flagsList.getItems()) {
			newFlags.add(newFlag.trim());
		}
		OcamlBuilder.setResourceFlags(project, newFlags);

		if (!newFlags.equals(flagsStored)) {
			// if the flags changed, we have to recompile the project. This is done in a background Job
			if (project != null) {
				Job job = new Job("Build project") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						} catch (CoreException e) {
							OcamlPlugin.logError("error in " + "OcamlProjectFlagsProperties:performOk: "
									+ "error rebuilding project", e);
						}
						// Refresh the file system to see the new files appear
						Misc.refreshFileSystem(project, monitor);
						return Status.OK_STATUS;
					}

				};
				job.setPriority(Job.BUILD);
				job.setUser(true);
				job.schedule(500);
			}
		}

		// restore to null for the next time
		flagsStored = null;
		return true;
	}

}
