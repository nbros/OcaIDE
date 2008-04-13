package ocaml.preferences;

import java.io.File;

import ocaml.OcamlPlugin;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Implements a preference page (Windows->Preferences->OCaml->Paths) that allows the user to parameter the
 * paths of the O'Caml tools (compilers, toplevel, documentation generator...)
 */
public class PathsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PathsPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		this.setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
		this.setDescription("Please fill in all fields with the correct paths on your system");
	}

	FileFieldEditor ocamlField;
	FileFieldEditor ocamlcField;
	FileFieldEditor ocamloptField;
	FileFieldEditor ocamldepField;
	FileFieldEditor ocamllexField;
	FileFieldEditor ocamlyaccField;
	FileFieldEditor ocamldocField;
	FileFieldEditor ocamldebugField;
	FileFieldEditor camlp4Field;
	FileFieldEditor ocamlbuildField;
	FileFieldEditor omakeField;

	BooleanFieldEditor ocamlbuildPathsOverride;

	Text pathText;
	private Label warningLabel;

	@Override
	public void createFieldEditors() {

		warningLabel = new Label(getFieldEditorParent(), SWT.NONE);
		warningLabel.setText("You must manually recompile your projects after changing paths");

		Group toolsGroup = new Group(getFieldEditorParent(), SWT.NONE);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		toolsGroup.setLayoutData(data);

		toolsGroup.setText("Ocaml tools");

		Composite group = new Composite(toolsGroup, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 4;
		group.setLayout(layout);
		GridData gdata = new GridData(GridData.FILL, GridData.FILL, true, false);
		gdata.horizontalSpan = 4;
//		gdata.verticalAlignment = GridData.FILL;
//		gdata.horizontalAlignment = GridData.FILL;
//		gdata.grabExcessHorizontalSpace = true;
//		gdata.horizontalSpan = 3;
		group.setLayoutData(gdata);

		// group.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		final Label binLabel = new Label(group, SWT.NONE);
		binLabel.setText("O'Caml Binaries Directory:");
		GridData binLabelData = new GridData();
		binLabelData.horizontalSpan = 4;
		binLabel.setLayoutData(binLabelData);

		// final Label rootLabel = new Label(group, SWT.NONE);
		// rootLabel.setText("Root:");

		IPath path = new Path(OcamlPlugin.getOcamlcFullPath());
		path = path.removeLastSegments(1);
		final String currentOcamlPath = path.toOSString();

		pathText = new Text(group, SWT.SINGLE | SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
		pathText.setText(currentOcamlPath);

		final Button browseButton = new Button(group, SWT.PUSH);
		browseButton.setText("Browse...");
		// GridData browseButtonData = new GridData(75,25);
		// browseButton.setLayoutData(browseButtonData);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(browseButton.getShell());
				dialog.setText("Select Directory");
				dialog.setMessage("Select O'Caml Binaries Directory");
				dialog.setFilterPath(currentOcamlPath);
				String dir = dialog.open();
				if (dir != null)
					pathText.setText(dir);
			}
		});

		final Button applyButton = new Button(group, SWT.PUSH);
		applyButton.setText("Apply");
		GridData applyButtonData = new GridData();
		applyButtonData.widthHint = 60;
		applyButton.setLayoutData(applyButtonData);

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				applyPathPrefix();
			}

		});

		ocamlbuildPathsOverride = new BooleanFieldEditor(
				PreferenceConstants.P_OCAMLBUILD_COMPIL_PATHS_OVERRIDE, "Override default ocamlbuild paths",
				toolsGroup);
		this.addField(ocamlbuildPathsOverride);

		ocamlField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAML, "oca&ml:", toolsGroup);
		this.addField(ocamlField);

		ocamlcField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLC, "ocaml&c:", toolsGroup);
		this.addField(ocamlcField);

		ocamloptField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLOPT, "ocaml&opt:",
				toolsGroup);
		this.addField(ocamloptField);

		ocamldepField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDEP, "ocaml&dep:",
				toolsGroup);
		this.addField(ocamldepField);

		ocamllexField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLLEX, "ocamlle&x:",
				toolsGroup);
		this.addField(ocamllexField);

		ocamlyaccField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLYACC, "ocaml&yacc:",
				toolsGroup);
		this.addField(ocamlyaccField);

		ocamldocField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDOC, "oc&amldoc:",
				toolsGroup);
		this.addField(ocamldocField);

		ocamldebugField = new FileFieldEditor(PreferenceConstants.P_COMPIL_PATH_OCAMLDEBUG, "ocamldebu&g:",
				toolsGroup);
		this.addField(ocamldebugField);

		camlp4Field = new FileFieldEditor(PreferenceConstants.P_PATH_CAMLP4, "camlp4:", toolsGroup);
		this.addField(camlp4Field);

		ocamlbuildField = new FileFieldEditor(PreferenceConstants.P_PATH_OCAMLBUILD, "ocamlbuild:",
				toolsGroup);
		this.addField(ocamlbuildField);

		Group otherGroup = new Group(getFieldEditorParent(), SWT.NONE);
		otherGroup.setText("Other tools");
		GridData odata = new GridData();
		odata.verticalAlignment = GridData.FILL;
		odata.grabExcessHorizontalSpace = true;
		odata.horizontalAlignment = GridData.FILL;
		odata.horizontalSpan = 3;
		otherGroup.setLayoutData(odata);

		this.addField(new FileFieldEditor(PreferenceConstants.P_MAKE_PATH, "make:", otherGroup));
		this.addField(new FileFieldEditor(PreferenceConstants.P_OMAKE_PATH, "omake:", otherGroup));
		this
				.addField(new DirectoryFieldEditor(PreferenceConstants.P_LIB_PATH, "OCaml &lib path:",
						otherGroup));

	}

	public void init(IWorkbench workbench) {
	}

	private void applyPathPrefix() {

		warningLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));

		String path = pathText.getText().trim();
		if (!path.endsWith(File.separator))
			path = path + File.separator;

		if (Platform.getOS().equals(Platform.OS_WIN32)) {

			if (new File(path + "ocaml.exe").exists())
				ocamlField.setStringValue(path + "ocaml.exe");
			else
				ocamlField.setStringValue("");

			if (new File(path + "ocamlc.exe").exists())
				ocamlcField.setStringValue(path + "ocamlc.exe");
			else
				ocamlcField.setStringValue("");

			if (new File(path + "ocamlopt.exe").exists())
				ocamloptField.setStringValue(path + "ocamlopt.exe");
			else
				ocamloptField.setStringValue("");

			if (new File(path + "ocamldep.exe").exists())
				ocamldepField.setStringValue(path + "ocamldep.exe");
			else
				ocamldepField.setStringValue("");

			if (new File(path + "ocamllex.exe").exists())
				ocamllexField.setStringValue(path + "ocamllex.exe");
			else
				ocamllexField.setStringValue("");

			if (new File(path + "ocamlyacc.exe").exists())
				ocamlyaccField.setStringValue(path + "ocamlyacc.exe");
			else
				ocamlyaccField.setStringValue("");

			if (new File(path + "ocamldoc.exe").exists())
				ocamldocField.setStringValue(path + "ocamldoc.exe");
			else
				ocamldocField.setStringValue("");

			if (new File(path + "camlp4.exe").exists())
				camlp4Field.setStringValue(path + "camlp4.exe");
			else
				camlp4Field.setStringValue("");

			if (new File(path + "ocamlbuild.exe").exists())
				ocamlbuildField.setStringValue(path + "ocamlbuild.exe");
			else
				ocamlbuildField.setStringValue("");

			if (new File(path + "ocamldebug.exe").exists())
				ocamldebugField.setStringValue(path + "ocamldebug.exe");
			else
				ocamldebugField.setStringValue("");

			return;
		}

		String filename;

		// ocaml
		filename = path + "ocaml";
		if (new File(filename).exists())
			ocamlField.setStringValue(path + "ocaml");
		else
			ocamlField.setStringValue(path);

		// ocamlc(.opt)
		filename = path + "ocamlc.opt";
		if (new File(filename).exists())
			ocamlcField.setStringValue(path + "ocamlc.opt");
		else {
			filename = path + "ocamlc";
			if (new File(filename).exists())
				ocamlcField.setStringValue(path + "ocamlc");
			else
				ocamlcField.setStringValue(path);
		}

		// ocamlopt(.opt)
		filename = path + "ocamlopt.opt";
		if (new File(filename).exists())
			ocamloptField.setStringValue(path + "ocamlopt.opt");
		else {
			filename = path + "ocamlopt";
			if (new File(filename).exists())
				ocamloptField.setStringValue(path + "ocamlopt");
			else
				ocamloptField.setStringValue(path);
		}

		// ocamldep(.opt)
		filename = path + "ocamldep.opt";
		if (new File(filename).exists())
			ocamldepField.setStringValue(path + "ocamldep.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				ocamldepField.setStringValue(path + "ocamldep");
			else
				ocamldepField.setStringValue(path);
		}

		// ocamllex(.opt)
		filename = path + "ocamllex.opt";
		if (new File(filename).exists())
			ocamllexField.setStringValue(path + "ocamllex.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				ocamllexField.setStringValue(path + "ocamllex");
			else
				ocamllexField.setStringValue(path);
		}

		// ocamlyacc(.opt)
		filename = path + "ocamlyacc.opt";
		if (new File(filename).exists())
			ocamlyaccField.setStringValue(path + "ocamlyacc.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				ocamlyaccField.setStringValue(path + "ocamlyacc");
			else
				ocamlyaccField.setStringValue(path);
		}

		// ocamldoc(.opt)
		filename = path + "ocamldoc.opt";
		if (new File(filename).exists())
			ocamldocField.setStringValue(path + "ocamldoc.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				ocamldocField.setStringValue(path + "ocamldoc");
			else
				ocamldocField.setStringValue(path);
		}

		// ocamldebug(.opt)
		filename = path + "ocamldebug.opt";
		if (new File(filename).exists())
			ocamldebugField.setStringValue(path + "ocamldebug.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				ocamldebugField.setStringValue(path + "ocamldebug");
			else
				ocamldebugField.setStringValue(path);
		}

		// ocamlbuild
		filename = path + "ocamlbuild";
		if (new File(filename).exists())
			ocamlbuildField.setStringValue(path + "ocamlbuild");
		else
			ocamlbuildField.setStringValue(path);

		// camlp4(.opt)
		filename = path + "camlp4.opt";
		if (new File(filename).exists())
			camlp4Field.setStringValue(path + "camlp4.opt");
		else {
			filename = path + "ocamldep";
			if (new File(filename).exists())
				camlp4Field.setStringValue(path + "camlp4");
			else
				camlp4Field.setStringValue(path);
		}
	}
}
