package ocaml.properties;

import ocaml.OcamlPlugin;
import ocaml.build.makefile.MakeUtility;
import ocaml.build.makefile.MakefileTargets;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Implements a property page to allow the user to define the makefile targets for the selected project. There
 * are 3 targets groups to define, each groups is a list of targets:
 * <ul>
 * <li>the "main" targets to build the project
 * <li>the "clean" targets which are executed when the project must be cleaned
 * <li>the "doc" targets that are executed when the documentation must be generated
 * </ul>
 */
public class OcamlMakefileProjectProperties extends PropertyPage {

	private IProject project = null;

	private Label makeLabel;

	private Button[] makeButton;

	private Label optionsLabel;

	private Text optionsText;

	private Label targetsLabel;

	private Text targetsText;

	private Label cleanTargetsLabel;

	private Text cleanTargetsText;

	private Label docTargetsLabel;

	private Text docTargetsText;

	@Override
	protected Control createContents(Composite parent) {
		IResource element = (IResource) this.getElement();
		if (element instanceof IProject) {
			this.project = (IProject) element;
		} else
			OcamlPlugin.logError("Ocaml makefile project property page: project=null");

		Composite composite = new Composite(parent, SWT.NULL);

		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		makeLabel = new Label(composite, SWT.LEFT);
		makeLabel.setText("make variant");

		makeButton = new Button[2];
		makeButton[0] = new Button(composite, SWT.RADIO);
		makeButton[0].setSelection(true);
		makeButton[0].setText("GNU make");
		makeButton[0].setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		makeButton[1] = new Button(composite, SWT.RADIO);
		makeButton[1].setText("OMake");
		makeButton[1].setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		optionsLabel = new Label(composite, SWT.LEFT);
		optionsLabel.setText("additional make options (separated by spaces):");

		optionsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		optionsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		targetsLabel = new Label(composite, SWT.LEFT);
		targetsLabel.setText("make targets for rebuild (separated by commas):");

		targetsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		targetsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		cleanTargetsLabel = new Label(composite, SWT.LEFT);
		cleanTargetsLabel.setText("make targets for clean (separated by commas):");

		cleanTargetsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		cleanTargetsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		docTargetsLabel = new Label(composite, SWT.LEFT);
		docTargetsLabel.setText("make targets for generating documentation (separated by commas):");

		docTargetsText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		docTargetsText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		load();

		return composite;
	}

	private void load() {
		// make variant
		MakeUtility makeUtility = new MakeUtility(project);
		switch (makeUtility.getVariant()) {
		case GNU_MAKE:
			makeButton[0].setSelection(true);
			makeButton[1].setSelection(false);
			break;
		case OMAKE:
			makeButton[0].setSelection(false);
			makeButton[1].setSelection(true);
			break;
		}

        //options
		String[] options = makeUtility.getOptions();
		
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < options.length - 1; i++)
			stringBuilder.append(options[i] + " ");
        if (options.length > 0)
   		    stringBuilder.append(options[options.length - 1]);

		optionsText.setText(stringBuilder.toString());
		
		// make
		MakefileTargets makefileTargets = new MakefileTargets(project);
		String[] targets = makefileTargets.getTargets();

		stringBuilder.setLength(0);
		for (int i = 0; i < targets.length - 1; i++)
			stringBuilder.append(targets[i] + ", ");
        if (targets.length > 0)
		    stringBuilder.append(targets[targets.length - 1]);

		targetsText.setText(stringBuilder.toString());

		// clean
		targets = makefileTargets.getCleanTargets();

		stringBuilder.setLength(0);
		for (int i = 0; i < targets.length - 1; i++)
			stringBuilder.append(targets[i] + ", ");
        if (targets.length > 0)
		    stringBuilder.append(targets[targets.length - 1]);

		cleanTargetsText.setText(stringBuilder.toString());

		// doc
		targets = makefileTargets.getDocTargets();

		stringBuilder.setLength(0);
		for (int i = 0; i < targets.length - 1; i++)
			stringBuilder.append(targets[i] + ", ");
        if (targets.length > 0)
		    stringBuilder.append(targets[targets.length - 1]);

		docTargetsText.setText(stringBuilder.toString());
	}

	@Override
	public boolean performOk() {
		MakeUtility.Variants clone;
		if (makeButton[1].getSelection()) clone = MakeUtility.Variants.OMAKE;
		else clone = MakeUtility.Variants.GNU_MAKE;

		MakeUtility makeUtility = new MakeUtility(project);
		makeUtility.setVariant(clone);
		
		String[] options = optionsText.getText().split(" +");
		makeUtility.setOptions(options);

		String[] targets = targetsText.getText().split(",");
		MakefileTargets makefileTargets = new MakefileTargets(this.project);
		makefileTargets.setTargets(targets);

		targets = cleanTargetsText.getText().split(",");
		makefileTargets.setCleanTargets(targets);

		targets = docTargetsText.getText().split(",");
		makefileTargets.setDocTargets(targets);

		super.performOk();
		return true;
	}

	@Override
	protected void performDefaults() {
		makeButton[0].setSelection(true);
		makeButton[1].setSelection(false);
		optionsText.setText("");
		targetsText.setText("all");
		cleanTargetsText.setText("clean");
		docTargetsText.setText("htdoc");

		super.performDefaults();
	}

}
