package ocaml.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.preferences.PreferenceConstants;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/** This class regroups static functions of general use */
public class Misc {
	
	public static boolean bNoUnicode = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.P_DISABLE_UNICODE_CHARS);
	
	/**
	 * Replaces some characters/groups of characters by nicer looking equivalents in Unicode. (&rarr;,
	 * &times;, &alpha;, &beta;, &gamma;, &delta; ... instead of -&gt;, *, 'a, 'b, 'c, 'd ...)
	 */
	public static String beautify(String str) {
		if(bNoUnicode)
			return str.trim();
		else
			return (" " + str + " ").replaceAll("->", "\u2192").replaceAll("\\*", "\u00d7").replaceAll(
				"\\B'a\\b", "\u03b1").replaceAll("\\B'b\\b", "\u03b2").replaceAll("\\B'c\\b", "\u03b3")
				.replaceAll("\\B'd\\b", "\u03b4").replaceAll("\\B'e\\b", "\u03b5").replaceAll("\\B'f\\b",
						"\u03b6").replaceAll("\\B'g\\b", "\u03b7").replaceAll("\\B'h\\b", "\u03b8")
				.replaceAll("\\B'i\\b", "\u03b9").replaceAll("\\B'j\\b", "\u03ba").replaceAll("\\B'k\\b",
						"\u03bb").replaceAll("\\B'l\\b", "\u03bc").trim();
	}

	/**
	 * Update the decorator manager (in a UI-Thread)
	 */
	public static void updateDecoratorManager() {

		Display.getDefault().syncExec(new Runnable() {
			public void run() {

				IDecoratorManager decoratorManager = PlatformUI.getWorkbench().getDecoratorManager();
				try {
					decoratorManager.setEnabled("Ocaml.errorInSourceDecorator", true);
					decoratorManager.setEnabled("Ocaml.warningInSourceDecorator", true);
					decoratorManager.setEnabled("Ocaml.errorInProjectDecorator", true);
					decoratorManager.setEnabled("Ocaml.warningInProjectDecorator", true);

					decoratorManager.update("Ocaml.errorInSourceDecorator");
					decoratorManager.update("Ocaml.warningInSourceDecorator");
					decoratorManager.update("Ocaml.errorInProjectDecorator");
					decoratorManager.update("Ocaml.warningInProjectDecorator");

					// TODO: is it useful to refresh this one?
					decoratorManager.update("Ocaml.generatedDecorator");
				} catch (CoreException e) {
					OcamlPlugin.logError("error in Misc:updateDecoratorManager: ", e);
				}
			}
		});
	}

	public static void showView(final String id) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					activePage.showView(id);
				} catch (Exception e) {
					OcamlPlugin.logError("error in Misc:showview", e);
				}
			}
		});
	}

	/**
	 * Name of a persistent property that indicates whether a file must be made into an executable. The value
	 * of this property is the name of the executable or <code>null</code> if there is no executable to
	 * create for this file.
	 */
	public static final String MAKE_EXE = "make_exe";

	/**
	 * Name of a persistent property put on the external sources folder, which is used by the debugger to
	 * display the current position in the code. This folder must be ignored by the Builder.
	 */
	public static final String EXTERNAL_SOURCES_FOLDER = "ocaml_external_sources_folder";

	/**
	 * The name of the hyperlinked files temporary directory, which is used to create links in the workspace
	 * to external files anywhere on the file system
	 */
	public final static String HYPERLINKSDIR = ".HyperlinksLinkedFiles";

	/** The default compilation flags to put on a new project. */
	public static final String[] defaultProjectFlags = { "-g" };

	public static final String DOC_TYPE = "doc_type";

	/** Get a list of folder from a project */
	public static IFolder[] getProjectFolders(final IProject project) {
		final List<IFolder> projectFolder = new ArrayList<IFolder>();
		try {
			if (project.isOpen())
				project.accept(new IResourceVisitor() {

					public boolean visit(IResource resource) throws CoreException {
						// do not visit external sources folders of .settings
						if (resource.getType() == IResource.FOLDER
								&& (!Misc.getResourceProperty(resource, EXTERNAL_SOURCES_FOLDER).equals("")
										|| resource.getName().matches(OcamlBuilder.EXTERNALFILES) || resource
										.getName().equals(".settings")|| resource.getName()
										.equals(Misc.HYPERLINKSDIR)))
							return false;

						// all the remaining folders are OK
						if (resource.getType() == IResource.FOLDER)
							projectFolder.add((IFolder) resource);

						return true;
					}

				});
		} catch (CoreException e) {
			OcamlPlugin.logError("error in Misc:getProjectFiles", e);
		}

		return projectFolder.toArray(new IFolder[projectFolder.size()]);
	}

	/** Get a list of files from a project */
	public static IFile[] getProjectFiles(final IProject project) {
		final List<IFile> projectFiles = new ArrayList<IFile>();
		try {
			if (project.isOpen())
				project.accept(new IResourceVisitor() {

					public boolean visit(IResource resource) throws CoreException {
						// do not visit external sources folders or .settings
						if (resource.getType() == IResource.FOLDER
								&& (!Misc.getResourceProperty(resource, EXTERNAL_SOURCES_FOLDER).equals("")
										|| resource.getName().equals(OcamlBuilder.EXTERNALFILES)
										|| resource.getName().equals(".settings") || resource.getName()
										.equals(Misc.HYPERLINKSDIR)))
							return false;

						// ignore .project and .paths
						if (resource.getName().equals(".project")
								|| resource.getName().equals(OcamlPaths.PATHS_FILE))
							return false;

						// all the remaining files are OK
						// TODO ignore automatically generated files too?
						if (resource.getType() == IResource.FILE)
							projectFiles.add((IFile) resource);

						return true;
					}

				});
		} catch (CoreException e) {
			OcamlPlugin.logError("error in Misc:getProjectFiles", e);
		}

		return projectFiles.toArray(new IFile[projectFiles.size()]);
	}

	/** Get all the mli files from a project */
	public static IFile[] getMliFiles(final IProject project) {
		final List<IFile> allFiles = new ArrayList<IFile>();
		for (IFile file : getProjectFiles(project)) {
			final String ext = file.getFileExtension();
			if ((ext != null) && (ext.equals("mli")))
				allFiles.add(file);
		}
		return allFiles.toArray(new IFile[allFiles.size()]);
	}

	/**
	 * Return the paths chosen by the user in the "O'Caml project Paths" preference dialog, as a string made
	 * from a succession of "-I";"path" so that they can be used as a parameter for the O'Caml compiler. Some
	 * paths are relative to the workspace, and some others are absolute.
	 */
	public static ArrayList<String> getProjectPaths(IProject project) {
		final ArrayList<String> strPaths = new ArrayList<String>();
		OcamlPaths opaths = new OcamlPaths(project);

		for (String strPath : opaths.getPaths()) {
			strPaths.add("-I");
			final IPath path = new Path(strPath);
			if (!path.isAbsolute()) {
				strPath = project.getName() + File.separator + strPath;
			}
			strPaths.add(strPath);
		}

		return strPaths;
	}

	/**
	 * Return <code>true</code> if <code>file</code> is an ml, mli, mll or mly file.
	 */
	public static boolean isOCamlSourceFile(IFile file) {
		String ext = file.getFileExtension();
		return ((ext != null) && (ext.matches("ml[liy]?")));
	}

	/**
	 * Return <code>true</code> if <code>file</code> is a cmo, cmi, cmx, exe, annot, o, or automatically
	 * generated file.
	 */
	public static boolean isGeneratedFile(IFile file) {

		final String fileName = file.getName();
		if (fileName.equals(".project") || fileName.equals(OcamlPaths.PATHS_FILE))
			return false;

		// Test the IS_GEN property first, because we can have extension-less automatically generated files
		if (Misc.getFileProperty(file, OcamlBuilder.IS_GEN).equals("true")) {
			return true;
		}

		final String ext = file.getFileExtension();
		if (ext == null)
			return false;

		return (ext.equals("cmo") || ext.equals("cmi") || ext.equals("cmx") || ext.equals("exe")
				|| ext.equals("annot") || ext.equals("o"));
	}

	/**
	 * Return a persistent property associated with the project. The qualified name uses the "ocaml"
	 * qualifier.
	 * 
	 * @return the value of the property, or an empty string if it couldn't be retrieved
	 */
	public static String getProjectProperty(IProject project, String propertyName) {
		try {
			String value = project.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
					propertyName));
			if (value == null)
				return "";
			return value;
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:getProjectProperty :", e);
			return "";
		}
	}

	/**
	 * Return a file persistent property
	 * 
	 * @see getProjectProperty
	 */
	public static String getFileProperty(IFile file, String propertyName) {
		try {
			String value = file.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName));
			if (value == null)
				return "";
			return value;
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:getFileProperty :", e);
			return "";
		}
	}

	/**
	 * Return a folder persistent property
	 * 
	 * @see getProjectProperty
	 */
	public static String getFolderProperty(IFolder folder, String propertyName) {
		try {
			String value = folder
					.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName));
			if (value == null)
				return "";
			return value;
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:getFolderProperty :", e);
			return "";
		}
	}

	/**
	 * Same thing as getFileProperty and getFolderProperty, but works for both.
	 */
	public static String getResourceProperty(IResource resource, String propertyName) {
		try {
			String value = resource.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
					propertyName));
			if (value == null)
				return "";
			return value;
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:getFolderProperty :", e);
			return "";
		}
	}

	/**
	 * set a project persistent property
	 * 
	 * @see getProjectProperty
	 */
	public static void setProjectProperty(IProject project, String propertyName, String value) {
		try {
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName), value);
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:setProjectProperty :", e);
		}
	}

	/**
	 * set a file persistent property
	 * 
	 * @see getProjectProperty
	 */
	public static void setFileProperty(IFile file, String propertyName, String value) {
		try {
			file.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName), value);
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:setFileProperty :", e);
		}
	}

	/**
	 * set a folder persistent property
	 * 
	 * @see getProjectProperty
	 */
	public static void setFolderProperty(IFolder folder, String propertyName, String value) {
		try {
			folder.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName), value);
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:setFolderProperty :", e);
		}
	}

	/**
	 * Append text to the O'Caml compiler output (in a UI-Thread)
	 */
	public static void appendToOcamlConsole(final String msg) {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				try {
					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					OcamlCompilerOutput console = (OcamlCompilerOutput) activePage
							.findView(OcamlCompilerOutput.ID);
					if (console == null) {
						console = (OcamlCompilerOutput) activePage.showView(OcamlCompilerOutput.ID);

					}
					console.appendln(msg);
				} catch (PartInitException pe) {
					OcamlPlugin.logError("error in Misc:updateOcamlConsole: "
							+ "error opening OcamlCompilerOutput", pe);
				} catch (NullPointerException ne) {
					OcamlPlugin.logError("error in Misc:updateOcamlConsole: "
							+ "error finding workbench, active window" + " or active page", ne);
				}
			}
		});
	}

	/**
	 * display an error dialog box
	 */
	public static void popupErrorMessageBox(final String msg, final String title) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				final MessageBox msgBox = new MessageBox(shell, SWT.ICON_ERROR);
				msgBox.setMessage(msg);
				msgBox.setText(title);
				msgBox.open();
			}
		});
	}

	/**
	 * Refresh the file system for a project
	 */
	public static void refreshFileSystem(IProject proj) {
		try {
			proj.refreshLocal(IProject.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			OcamlPlugin.logError("Misc:refreshFileSystem", e);
		}
	}

	/**
	 * Refresh the file system for a project, with a progress monitor
	 */
	public static void refreshFileSystem(IProject proj, IProgressMonitor monitor) {
		try {
			proj.refreshLocal(IProject.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
			OcamlPlugin.logError("Misc:refreshFileSystem", e);
		}
	}

	/**
	 * Create an icon from a filename
	 * 
	 * @return an image, or <code>null</code> if there was an error
	 */
	public static Image createIcon(String name) {
		String iconPath = "icons" + File.separatorChar;
		try {
			URL installURL = OcamlPlugin.getInstallURL();
			URL url = new URL(installURL, iconPath + name);
			ImageDescriptor icon = ImageDescriptor.createFromURL(url);
			return icon.createImage();
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error in createIcon", e);
		}
		return null;
	}

}
