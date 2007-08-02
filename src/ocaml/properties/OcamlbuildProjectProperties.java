package ocaml.properties;

import ocaml.OcamlPlugin;
import ocaml.build.ocamlbuild.OcamlbuildFlags;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class OcamlbuildProjectProperties extends PropertyPage {

	private IProject project;
	private Composite composite;
	private Text txtTargets;
	private Text txtLibs;
	private Text txtCFlags;
	private Text txtLFlags;

	@Override
	protected Control createContents(Composite parent) {
		this.project = (IProject) getElement();
		
		OcamlbuildFlags ocamlbuildFlags = new OcamlbuildFlags(project);
		ocamlbuildFlags.load();
		
		composite = new Composite(parent, SWT.NONE);
		
		GridLayout gridLayout = new GridLayout(1, true);
		composite.setLayout(gridLayout);
		
		Label lblHeader = new Label(composite, SWT.NONE);
		lblHeader.setText("Set the parameters for compiling your project with ocamlbuild.\n" +
				"Use commas to separate values in the following boxes:");
		lblHeader.setLayoutData(new GridData(500, 45));
		
		
		Label lblTargets = new Label(composite, SWT.NONE);
		lblTargets.setText("Targets (.native, .byte, .d.byte, .cma, ...)");
		lblTargets.setToolTipText("Set the name of the targets you want to build (ex: mymodule.byte, a.native, ...)");
		lblTargets.setLayoutData(newGridDataLabel());
		
		txtTargets = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtTargets.setTextLimit(2000);
		txtTargets.setLayoutData(newGridDataText());
		txtTargets.setText(ocamlbuildFlags.getTargets());
		
		Label lblLibs = new Label(composite, SWT.NONE);
		lblLibs.setText("Libraries (-libs)  (ex: bigarray, dynlink, graphics, nums, str, unix, ...)");
		lblLibs.setToolTipText("Put the names of the libraries needed by your project. Ex: nums,unix");
		lblLibs.setLayoutData(newGridDataLabel());

		
		txtLibs = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtLibs.setTextLimit(2000);
		txtLibs.setLayoutData(newGridDataText());
		txtLibs.setText(ocamlbuildFlags.getLibs());

		Label lblCFlags = new Label(composite, SWT.NONE);
		lblCFlags.setText("Compiler flags (-cflags)");
		lblCFlags.setToolTipText("Compiler flags that must be applied to each source file in your project.");
		lblCFlags.setLayoutData(newGridDataLabel());
		
		txtCFlags = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtCFlags.setTextLimit(2000);
		txtCFlags.setLayoutData(newGridDataText());
		txtCFlags.setText(ocamlbuildFlags.getCFlags());
		
		
		Label lblLFlags = new Label(composite, SWT.NONE);
		lblLFlags.setToolTipText("Linker flags that must be applied while linking object files in your project.");
		lblLFlags.setText("Linker flags (-lflags)");
		lblLFlags.setLayoutData(newGridDataLabel());
		
		txtLFlags = new Text(composite, SWT.BORDER | SWT.MULTI);
		txtLFlags.setTextLimit(2000);
		txtLFlags.setLayoutData(newGridDataText());
		txtLFlags.setText(ocamlbuildFlags.getLFlags());
		
		
		return composite;
	}
	
	private GridData newGridDataLabel(){
		GridData data = new GridData(150, 20);
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = SWT.FILL;
		return data;
	}

	private GridData newGridDataText(){
		GridData data = new GridData(150, 60);
		data.grabExcessHorizontalSpace = true;
		data.horizontalAlignment = SWT.FILL;
		return data;
	}
	
	@Override
	public boolean performOk() {
		OcamlbuildFlags ocamlbuildFlags = new OcamlbuildFlags(project);
		
		ocamlbuildFlags.setTargets(txtTargets.getText());
		ocamlbuildFlags.setLibs(txtLibs.getText());
		ocamlbuildFlags.setCFlags(txtCFlags.getText());
		ocamlbuildFlags.setLFlags(txtLFlags.getText());
		
		ocamlbuildFlags.save();
		
		Job job = new Job("Cleaning Project after changing project properties") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
				} catch (CoreException e) {
					OcamlPlugin.logError("error cleaning project after changing ocamlbuild project properties", e);
				}
				return Status.OK_STATUS;
			}
		};
		
		job.schedule(500);
		
		return super.performOk();
	}
}
