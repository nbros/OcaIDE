package ocaml.properties;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Implements a property page (accessible by opening the properties through the pop-up menu of an OCaml
 * module) that allows the user to specify whether he wants to create an executable from this module, to give
 * the name of this executable, and to specify the list of options that should be passed to the compiler while
 * compiling this file.
 */
public class OcamlFileProperties extends PropertyPage {

	/** The width of text fields */
	private static final int TEXT_FIELD_WIDTH = 25;

	private Button makeExeCheckbox;

	/**
	 * The file associated with this property page
	 */
	private IFile file;

	private Text exeNameText;

	private Text flagChoiceText;

	private Button addFlagButton;

	private List flagsList;

	private int LIST_HEIGHT = 100;

	private Button removeFlagButton;

	private Button changeFlagButton;

	/** The text field for changing a list item (appears only when needed) */
	private Text txtChange;

	/** A button to validate the changes of an item (appears only when needed) */
	private Button btnValidateChanges;

	private Button upButton;

	private Button downButton;

	private Text projectFlagsText;

	/**
	 * Add a separator to the properties page
	 */
	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addFirstSection(Composite parent) {

		final Composite composite = this.createDefaultComposite(parent);

		// The file extension. Only .ml files can be made executable
		final String ext = file.getFileExtension();

		if (ext != null && ext.equals("ml")) {
			// Create the button
			this.makeExeCheckbox = new Button(composite, SWT.CHECK);
			this.makeExeCheckbox.setText("Make executable named");

			// text field
			this.exeNameText = new Text(composite, SWT.SINGLE | SWT.BORDER);
			exeNameText.setEditable(true);

			// the text field has a fixed width
			final GridData gd = new GridData();
			gd.widthHint = this.convertWidthInCharsToPixels(TEXT_FIELD_WIDTH);
			exeNameText.setLayoutData(gd);

			// We add a selection listener to the button, to gray it or not
			makeExeCheckbox.addSelectionListener(new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
					makeExeCheckbox.setSelection(false);
					exeNameText.setEnabled(false);
				}

				public void widgetSelected(SelectionEvent e) {
					exeNameText.setEnabled(makeExeCheckbox.getSelection());
				}
			});

		}

	}

	/** Add the second section, for adding flags */
	private void addSecondSection(Composite parent) {

		// The group for adding flags in the second section
		final Group flagsSection = new Group(parent, SWT.NULL);
		flagsSection.setText("Add some command line flags to this module");
		flagsSection.setLayout(new GridLayout(2, false));
		flagsSection.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		// the choice text field
		flagChoiceText = new Text(flagsSection, SWT.SINGLE | SWT.BORDER);
		flagChoiceText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		flagChoiceText.setEditable(true);

		addFlagButton = new Button(flagsSection, SWT.PUSH);
		addFlagButton.setText("A&dd");
		addFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		addFlagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flagsList.add(flagChoiceText.getText());
				flagChoiceText.setText("");
				flagChoiceText.setFocus();
			}
		});

		flagsList = new List(flagsSection, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		final GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4);
		listData.heightHint = LIST_HEIGHT;
		flagsList.setLayoutData(listData);
		flagsList.addSelectionListener(new SelectionAdapter() {
			// we activate the buttons only when something is selected in the list
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

		removeFlagButton = new Button(flagsSection, SWT.PUSH);
		removeFlagButton.setText("&Remove");
		removeFlagButton.setEnabled(false);
		removeFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		removeFlagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flagsList.remove(flagsList.getSelectionIndices());
			}
		});

		changeFlagButton = new Button(flagsSection, SWT.PUSH);
		changeFlagButton.setText("&Change");
		changeFlagButton.setEnabled(false);
		changeFlagButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		changeFlagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final int selectionIndex = flagsList.getSelectionIndex();
				txtChange.setText(flagsList.getItem(selectionIndex));
				txtChange.setVisible(true);
				btnValidateChanges.setVisible(true);
				txtChange.setFocus();
			}
		});

		upButton = new Button(flagsSection, SWT.PUSH);
		upButton.setText("&Up");
		upButton.setEnabled(false);
		upButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		upButton.addSelectionListener(new SelectionAdapter() {
			// this only works if the first item is not selected
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
			// this only works if the last item is not selected
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
				// reactivate the buttons depending on changes
				if (flagsList.isSelected(flagsList.getItemCount() - 1)) {
					downButton.setEnabled(false);
				}
				if (!flagsList.isSelected(0)) {
					upButton.setEnabled(true);
				}
				flagsList.setFocus();
			}
		});

		// These two widgets are only made visible when the user needs to modify the text of a flag

		txtChange = new Text(flagsSection, SWT.SINGLE | SWT.BORDER);
		txtChange.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		txtChange.setEditable(true);
		txtChange.setVisible(false);

		btnValidateChanges = new Button(flagsSection, SWT.PUSH);
		btnValidateChanges.setText("O&k");
		btnValidateChanges.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		btnValidateChanges.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				flagsList.setItem(flagsList.getSelectionIndex(), txtChange.getText());
				txtChange.setVisible(false);
				btnValidateChanges.setVisible(false);
				flagsList.setFocus();
			}
		});
		btnValidateChanges.setVisible(false);

		// The group for project flags
		final Group projectFlags = new Group(parent, SWT.NULL);
		projectFlags.setText("Project flags");
		projectFlags.setLayout(new GridLayout(1, false));
		projectFlags.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));

		// the text box for project flags
		projectFlagsText = new Text(projectFlags, SWT.MULTI | SWT.H_SCROLL);
		projectFlagsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		projectFlagsText.setEditable(false);

	}

	/** @see PreferencePage#createContents(Composite) */
	@Override
	protected Control createContents(Composite parent) {
		// associate the file to this properties page
		
		// This cast should be safe
		this.file = (IFile) getElement();
		// create a composite from the parent, and define its layout
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		// add the different sections to the page
		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);

		load();

		return composite;
	}

	/** Initialize the main composite */
	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		return composite;
	}

	/**
	 * This method is called when the user clicks on "restore defaults"
	 * We remove the make_exe property and clear the flags list 
	 */
	@Override
	protected void performDefaults() {
		Misc.setShareableProperty(file, Misc.MAKE_EXE, null);
		OcamlBuilder.setResourceFlags(file, new ArrayList<String>(0));
		load();
	}

	/** Initialize the fields of the properties page from persistent properties */
	protected void load() {
		// Retrieve the MAKE_EXE property, that contains the name of the future executable
		final String exeName = Misc.getShareableProperty(file, Misc.MAKE_EXE);
		exeNameText.setText(exeName);

		// save the name of the executable
		if (this.exeNameStored == null) {
			this.exeNameStored = exeName;
		}

		// if there is no property, getFileProperty returns ""
		if (exeName.equals("")) {
			// We change the extension to ".exe"
			exeNameText.setText(file.getFullPath().removeFileExtension().addFileExtension("exe")
					.lastSegment());

			makeExeCheckbox.setSelection(false);
			exeNameText.setEnabled(false);
		} else {
			makeExeCheckbox.setSelection(true);
			exeNameText.setEnabled(true);
		}

		flagsList.removeAll();
		final java.util.List<String> flags = OcamlBuilder.getResourceFlags(file);

		// save the flags list
		if (flagsStored == null) {
			flagsStored = flags;
		}

		for (String flag : flags) {
			flagsList.add(flag);
		}

		// display the project flags
		projectFlagsText.setText("");
		final java.util.List<String> projectFlags = OcamlBuilder.getResourceFlags(file.getProject());
		for (String pflag : projectFlags) {
			projectFlagsText.append(pflag + " ");
		}
	}

	/** To remember the executable name when the properties page is opened */
	protected String exeNameStored = null;

	/** To remember the compilation flags when the properties page is opened */
	protected java.util.List<String> flagsStored = null;

	@Override
	public boolean performOk() {

		// Whether changes happened, so that we must recompile the file
		boolean changes = false;

		// the user wants to create an executable
		if (makeExeCheckbox.getSelection()) {
			final String exeName = exeNameText.getText();

			// compare the old and new name
			changes = changes || !exeNameStored.equals(exeName);

			if (exeName.equals("")) {
				Misc.setShareableProperty(file, Misc.MAKE_EXE, null);
				// uncheck the check box and gray the text field
				makeExeCheckbox.setSelection(false);
				exeNameText.setEnabled(false);
			} else {
				Misc.setShareableProperty(file, Misc.MAKE_EXE, exeName);
			}
		} else {
			changes = changes || !exeNameStored.equals("");

			Misc.setShareableProperty(file, Misc.MAKE_EXE, null);
		}

		final java.util.List<String> flags = new ArrayList<String>(flagsList.getItemCount());
		for (String flag : flagsList.getItems()) {
			flags.add(flag.trim());
		}
		OcamlBuilder.setResourceFlags(file, flags);
		// compare the two lists
		changes = changes || !flagsStored.equals(flags);

		// we touch the file if there are changes, so that the compiler will recompile it
		if (changes) {
			try {
				file.touch(null);
			} catch (CoreException e) {
				OcamlPlugin.logError("error in OcamlFileProperties:performOk:" + " error touching file", e);

			}

			// update the decoration in the navigator view
			IDecoratorManager decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
			try {
				decoratorManager.setEnabled("Ocaml.makeExeDecorator", true);
			} catch (CoreException e) {
				OcamlPlugin.logError("ocaml plugin error", e);
			}
			decoratorManager.update("Ocaml.makeExeDecorator");
		}

		// set back to null for next time
		exeNameStored = null;
		flagsStored = null;

		return true;
	}
}