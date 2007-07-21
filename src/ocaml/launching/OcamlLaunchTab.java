package ocaml.launching;

import java.io.File;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Implements a properties page that will appear amongst the O'Caml launch configuration tabs and that will
 * allow the user to configure the name of the executable, the name of the project, and the command-line
 * arguments of the executable.
 */
public class OcamlLaunchTab extends AbstractLaunchConfigurationTab {

	public static final String ATTR_FULLPATH = "attr_ocaml_launch_full_path";

	public static final String ATTR_PROJECTNAME = "attr_ocaml_launch_project_name";

	public static final String ATTR_ARGS = "attr_ocaml_launch_args";

	Composite composite;

	Text textExePath;

	Text textProjectName;

	Text textArguments;

	public void createControl(Composite parent) {

		composite = new Composite(parent, SWT.NONE);
		setControl(composite);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		composite.setLayout(gridLayout);

		Label label1 = new Label(composite, SWT.NONE);
		label1.setText("Project name:");
		// dummy label, because we have two columns
		new Label(composite, SWT.NONE);
		textProjectName = new Text(composite, SWT.BORDER);
		new Label(composite, SWT.NONE);

		Label label2 = new Label(composite, SWT.NONE);
		label2.setText("Full path to the executable file:");
		// dummy label, because we have two columns
		new Label(composite, SWT.NONE);
		textExePath = new Text(composite, SWT.BORDER);

		GridData layoutData = new GridData(SWT.FILL, SWT.TOP, true, false);
		layoutData.widthHint = 200;
		textExePath.setLayoutData(layoutData);
		textExePath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		textProjectName.setLayoutData(layoutData);
		textProjectName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		Button buttonBrowse = new Button(composite, SWT.PUSH);
		buttonBrowse.setText("Browse...");
		buttonBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				browse();
			}
		});

		Label label3 = new Label(composite, SWT.NONE);
		label3.setText("Command line arguments (separated by spaces)\n"
				+ "You can use \" \" and \\ to quote strings");
		// dummy label, because we have two columns
		new Label(composite, SWT.NONE);
		textArguments = new Text(composite, SWT.BORDER);
		textArguments.setLayoutData(layoutData);
		textArguments.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

	}

	/** Browse button was clicked */
	protected void browse() {
		FileDialog fileDialog = new FileDialog(composite.getShell());
		try {
			fileDialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}

		String path = fileDialog.open();
		if (path != null) {
			textExePath.setText(path.trim());
		}
	}

	/** This method was copied from the JDT plug-in */
	protected void updateLaunchConfigurationDialog() {
		if (getLaunchConfigurationDialog() != null) {
			/*
			 * order is important here due to the call to refresh the tab viewer in updateButtons() which
			 * ensures that the messages are up to date
			 */
			getLaunchConfigurationDialog().updateButtons();
			getLaunchConfigurationDialog().updateMessage();
		}
	}

	public String getName() {
		return "Ocaml launch configuration";
	}

	public void initializeFrom(ILaunchConfiguration configuration) {
		String path = null;
		String project = null;
		String args = null;
		try {
			path = configuration.getAttribute(ATTR_FULLPATH, "");
			project = configuration.getAttribute(ATTR_PROJECTNAME, "");
			args = configuration.getAttribute(ATTR_ARGS, "");
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			path = "";
			project = "";
			args = "";
		}

		textExePath.setText(path);
		textProjectName.setText(project);
		textArguments.setText(args);

		setDirty(false);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_FULLPATH, textExePath.getText());
		configuration.setAttribute(ATTR_PROJECTNAME, textProjectName.getText());
		configuration.setAttribute(ATTR_ARGS, textArguments.getText());
		setDirty(false);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(ATTR_FULLPATH, "");
		configuration.setAttribute(ATTR_PROJECTNAME, "");
		configuration.setAttribute(ATTR_ARGS, "");
		setDirty(false);
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		String path = null;
		String project = null;
		try {
			path = launchConfig.getAttribute(ATTR_FULLPATH, "");
			project = launchConfig.getAttribute(ATTR_PROJECTNAME, "");
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			path = "";
			project = "";
		}

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		boolean bFound = false;
		for (IProject p : projects)
			if (p.getName().equals(project)) {
				bFound = true;
				break;
			}

		if (!bFound) {
			setErrorMessage("Invalid project name: " + project);
			return false;
		}

		File file = new File(path);

		if (file.exists() && file.isFile()) {
			setErrorMessage(null);
			return true;
		} else {
			setErrorMessage("Invalid filename: " + path);
			return false;
		}
	}

}
