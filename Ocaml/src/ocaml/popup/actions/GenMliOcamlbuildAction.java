package ocaml.popup.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.build.ocamlbuild.OcamlbuildBuilder;
import ocaml.exec.CommandRunner;
import ocaml.util.FileUtil;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on the "Generate Interface" menu
 * item in the pop-up for a module in an ocamlbuild project.
 */
public class GenMliOcamlbuildAction implements IObjectActionDelegate {

	private IFile file = null;

	public GenMliOcamlbuildAction() {
	}

	public void run(IAction action) {
		if (file != null) {

			String filename = file.getName();
			String inferredMliFilename = null;
			String mliFilename = null;

			if (filename.endsWith(".ml")) {
				inferredMliFilename = filename.substring(0, filename.length() - 3)
						+ ".inferred.mli";
				mliFilename = filename.substring(0, filename.length() - 3) + ".mli";
			} else
				return;

			final IProject project = file.getProject();
			if (project == null)
				return;

			ArrayList<String> commandLine = OcamlbuildBuilder.buildCommandLine(project, true);
			if (commandLine == null)
				return;

			IPath filepath = file.getFullPath();
			// remove the project name from the path
			filepath = filepath.removeFirstSegments(1);

			// remove the filename from the path
			filepath = filepath.removeLastSegments(1);

			String strInferredMliFilePath = null;
			String strMliFilePath = null;
			String strResultPath = null;

			if (filepath.segmentCount() == 0) {
				strInferredMliFilePath = inferredMliFilename;
				strMliFilePath = mliFilename;
				strResultPath = "_build" + File.separator + inferredMliFilename;
			} else {
				strInferredMliFilePath = filepath.toOSString() + File.separator
						+ inferredMliFilename;
				strMliFilePath = filepath.toOSString() + File.separator + mliFilename;
				strResultPath = "_build" + File.separator + filepath.toOSString() + File.separator
						+ inferredMliFilename;
			}

			File mliFile = new File(project.getLocation().toOSString() + File.separator
					+ strMliFilePath);
			File inferredMliFile = new File(project.getLocation().toOSString() + File.separator
					+ strResultPath);

			if (mliFile.exists()) {
				if (!MessageDialog.openConfirm(null, "Replace interface file?", mliFile.getName()
						+ " already exists. Are you sure you want to replace it?"))
					return;
			}

			commandLine.add(strInferredMliFilePath);

			String[] strCommandLine = commandLine.toArray(new String[commandLine.size()]);

			String path = project.getLocation().toOSString();
			CommandRunner commandRunner = new CommandRunner(strCommandLine, path);

			String output = commandRunner.getStdout();
			String errorOutput = commandRunner.getStderr();

			OcamlCompilerOutput outputView = OcamlCompilerOutput.get();
			outputView.clear();
			if (outputView != null) {
				if (!"".equals(output))
					outputView.append(output);
				if (!"".equals(errorOutput))
					outputView.append(errorOutput);
			}

			if (!inferredMliFile.exists())
				return;

			FileInputStream in = null;
			FileOutputStream out = null;
			FileChannel inChan = null;
			FileChannel outChan = null;
			try {
				in = new FileInputStream(inferredMliFile);
				inChan = in.getChannel();
				out = new FileOutputStream(mliFile);
				outChan = out.getChannel();
				inChan.transferTo(0, inChan.size(), outChan);
			} catch (Exception e) {
				OcamlPlugin.logError("Error trying to copy inferred mli file", e);
			} finally {
				FileUtil.closeResource(inChan);
				FileUtil.closeResource(outChan);
				FileUtil.closeResource(in);
				FileUtil.closeResource(out);
			}

			// refresh the workspace
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						project.refreshLocal(IProject.DEPTH_INFINITE, null);
					} catch (CoreException e1) {
						OcamlPlugin.logError("ocaml plugin error", e1);
					}
				}
			});
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.file = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object o = sel.getFirstElement();
			if (o instanceof IFile)
				this.file = (IFile) o;
		}
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

}
