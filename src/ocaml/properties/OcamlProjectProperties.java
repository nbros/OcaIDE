package ocaml.properties;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Implements a property page for an O'Caml managed project. It allows the user to choose what module files to
 * make executable (this can also be done through a property page on each individual ml file), and to choose
 * the compilation mode.
 */
public class OcamlProjectProperties extends PropertyPage {

	private static final String DATA_EXENAME = "exename";

	private Combo cCompilMode;

	/* The check boxes */
	private ArrayList<Button> buttons;

	public OcamlProjectProperties() {
		super();
		this.buttons = new ArrayList<Button>();
	}

	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}

	private void addFirstSection(Composite parent) {
		Composite composite = this.createDefaultComposite(parent);
		// Label for files field
		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Make executable for:");
		lbl = new Label(composite, SWT.NONE);
		lbl.setText("");

		IProject project = ((IResource) this.getElement()).getProject();
		IFile[] files = Misc.getProjectFiles(project);

		// detect if this is the first time this is done
		boolean firstTime = false;
		if (exeNamesStored == null) {
			exeNamesStored = new ArrayList<String>();
			firstTime = true;
		}
		for (IFile file : files) {
			String filename = file.getName();
			String ext = file.getFileExtension();
			if (ext != null && ext.equals("ml")) {
				Button b = new Button(composite, SWT.CHECK);
				b.setText(filename);
				b.setData(file);
				String makeexe = Misc.getFileProperty(file, Misc.MAKE_EXE);
				// the first time, remember all the executables names
				if (firstTime)
					exeNamesStored.add(makeexe);
				b.setSelection(!makeexe.equals(""));
				b.setData(DATA_EXENAME, makeexe);
				this.buttons.add(b);
			}
		}
	}

	/** To detect what files to recompile. */
	protected java.util.List<String> exeNamesStored = null;

	/** To remember the compilation mode */
	protected String compilModeStored = null;

	private void addSecondSection(Composite parent) {
		Composite composite = this.createDefaultComposite(parent);
		IProject project = ((IResource) this.getElement()).getProject();
		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Compilation mode:");
		// Add compilation mode
		String index = Misc.getProjectProperty(project, OcamlBuilder.COMPIL_MODE);
		if (compilModeStored == null) {
			if (index.equals(OcamlBuilder.NATIVE))
				compilModeStored = index;
			else
				compilModeStored = OcamlBuilder.BYTE_CODE;
		}
		this.cCompilMode = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		this.cCompilMode.setItems(new String[] { OcamlBuilder.BYTE_CODE, OcamlBuilder.NATIVE });
		if (index.equals("") || index.equals(OcamlBuilder.BYTE_CODE))
			this.cCompilMode.select(0);
		else
			this.cCompilMode.select(1);
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		this.addFirstSection(composite);

		this.addSeparator(composite);

		this.addSecondSection(composite);

		this.addSeparator(composite);

		return composite;
	}

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

	@Override
	protected void performDefaults() {
	}

	@Override
	public boolean performOk() {
		final IProject project = ((IResource) this.getElement()).getProject();

		int i = 0;
		for (Button button : buttons) {
			IFile file = (IFile) button.getData();
			// to know if we must "touch" the file
			boolean hasChanged = false;
			if (button.getSelection()) {

				String exeName = (String) button.getData(DATA_EXENAME);

				// by default, the executable name is [filename].exe for [filename].ml
				if (exeName.equals(""))
					exeName = file.getLocation().removeFileExtension().addFileExtension("exe").lastSegment();

				Misc.setFileProperty(file, Misc.MAKE_EXE, exeName);
				hasChanged = exeNamesStored == null || !exeNamesStored.get(i).equals(exeName);
			} else {
				hasChanged = exeNamesStored == null || !exeNamesStored.get(i).equals("");
				Misc.setFileProperty(file, Misc.MAKE_EXE, null);
			}

			if (hasChanged) {
				// we touch the file so that the compiler will be notified of changes
				try {
					file.touch(null);
				} catch (CoreException e) {
					OcamlPlugin.logError("error in OcamlProjectProperty" + ":performOk: error touching file",
							e);
				}
			}
			i++;
		}

		String newCompilMode = "";
		final int ind = cCompilMode.getSelectionIndex();
		// -1 or 0 : byte-code
		if (ind == -1 || ind == 0) {
			newCompilMode = OcamlBuilder.BYTE_CODE;
		} else if (ind == 1) {
			newCompilMode = OcamlBuilder.NATIVE;
			// we must remove the "-g" flag (debug mode is not supported on native files)
			final java.util.List<String> projectFlags = OcamlBuilder.getResourceFlags(project);
			if (projectFlags.remove("-g"))
				OcamlBuilder.setResourceFlags(project, projectFlags);

		}
		Misc.setProjectProperty(project, OcamlBuilder.COMPIL_MODE, newCompilMode);

		// update the decoration in the navigator view
		IDecoratorManager decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
		try {
			decoratorManager.setEnabled("Ocaml.makeExeDecorator", true);
		} catch (CoreException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
		decoratorManager.update("Ocaml.makeExeDecorator");

		/*
		 * if the compilation mode was modified, recompile the whole project after a "clean" (we must remove
		 * all the old object files)
		 */
		if (compilModeStored == null || !compilModeStored.equals(newCompilMode)) {
			if (project != null) {
				Job job = new Job("Build project") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
							project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
						} catch (CoreException e) {
							OcamlPlugin.logError("error in " + "OcamlProjectProperties:performOk: "
									+ "error rebuilding project", e);
						}

						Misc.refreshFileSystem(project, monitor);
						return Status.OK_STATUS;
					}

				};
				job.setPriority(Job.BUILD);
				job.setUser(true);
				job.schedule(500);
			}
		}

		// restore to null for next time this window is opened
		compilModeStored = null;
		exeNamesStored = null;
		return true;
	}
}