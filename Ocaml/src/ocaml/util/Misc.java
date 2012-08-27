package ocaml.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.editor.syntaxcoloring.ILanguageWords;
import ocaml.preferences.PreferenceConstants;
import ocaml.views.OcamlCompilerOutput;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/** This class regroups static functions of general use */
public class Misc {

	public static boolean bNoUnicode = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
			PreferenceConstants.P_DISABLE_UNICODE_CHARS);

	static {
		OcamlPlugin.getInstance().getPreferenceStore().addPropertyChangeListener(
				new IPropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent event) {
						if (event.getProperty().equals(PreferenceConstants.P_DISABLE_UNICODE_CHARS)) {
							bNoUnicode = (Boolean) event.getNewValue();
						}
					}
				});
	}

	/**
	 * Replaces some characters/groups of characters by nicer looking equivalents in Unicode.
	 * (&rarr;, &times;, &alpha;, &beta;, &gamma;, &delta; ... instead of -&gt;, *, 'a, 'b, 'c, 'd
	 * ...)
	 */
	public static String beautify(String str) {

		if (bNoUnicode)
			return str.trim();

		String[] lines = str.split("\\r?\\n");

		StringBuilder builder = new StringBuilder();

		for (String line : lines) {
			builder.append((" " + line + " ").replaceAll("->", "\u2192")
					.replaceAll("\\*", "\u00d7").replaceAll("\\B'a\\b", "\u03b1").replaceAll(
							"\\B'b\\b", "\u03b2").replaceAll("\\B'c\\b", "\u03b3").replaceAll(
							"\\B'd\\b", "\u03b4").replaceAll("\\B'e\\b", "\u03b5").replaceAll(
							"\\B'f\\b", "\u03b6").replaceAll("\\B'g\\b", "\u03b7").replaceAll(
							"\\B'h\\b", "\u03b8").replaceAll("\\B'i\\b", "\u03b9").replaceAll(
							"\\B'j\\b", "\u03ba").replaceAll("\\B'k\\b", "\u03bb").replaceAll(
							"\\B'l\\b", "\u03bc").trim()
					+ OcamlPlugin.newline);
		}

		return builder.toString().trim();
	}

	/**
	 * Update the decorator manager (in a UI-Thread)
	 */
	public static void updateDecoratorManager() {

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				IDecoratorManager decoratorManager = PlatformUI.getWorkbench()
						.getDecoratorManager();
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
					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					activePage.showView(id);
				} catch (Exception e) {
					OcamlPlugin.logError("error in Misc:showview", e);
				}
			}
		});
	}

	/**
	 * Name of a persistent property that indicates whether a file must be made into an executable.
	 * The value of this property is the name of the executable or <code>null</code> if there is
	 * no executable to create for this file.
	 */
	public static final String MAKE_EXE = "make_exe";

	/** The default compilation flags to put on a new project. */
	public static final String[] defaultProjectFlags = { "-g" };

	public static final String DOC_TYPE = "doc_type";

	/** Get a list of folders from a project */
	public static IFolder[] getProjectFolders(final IProject project) {
		final List<IFolder> projectFolder = new ArrayList<IFolder>();
		try {
			if (project.isOpen())
				project.accept(new IResourceVisitor() {

					public boolean visit(IResource resource) throws CoreException {
						// do not visit external files folder or .settings
						if (resource.getType() == IResource.FOLDER
								&& (resource.getName().matches(OcamlBuilder.EXTERNALFILES) || resource
										.getName().equals(".settings")))
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
						// do not visit external files folder or .settings
						if (resource.getType() == IResource.FOLDER
								&& (resource.getName().equals(OcamlBuilder.EXTERNALFILES) || resource
										.getName().equals(".settings")))
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
	 * Keep all the mli files in the list and the ml files when there is no corresponding mli
	 * 
	 * @return the mli files and the ml files which don't have a corresponding mli file
	 */
	public static String[] filterInterfaces(String[] mlmliFiles) {
		ArrayList<String> listFiles = new ArrayList<String>();

		Arrays.sort(mlmliFiles);

		for (int i = 0; i < mlmliFiles.length; i++) {
			String filename = mlmliFiles[i];
			String next;
			if (i + 1 < mlmliFiles.length)
				next = mlmliFiles[i + 1];
			else
				next = "";

			if (filename.endsWith(".mli"))
				listFiles.add(filename);
			if (filename.endsWith(".ml") && !next.endsWith(".mli"))
				listFiles.add(filename);

		}

		String[] files = listFiles.toArray(new String[listFiles.size()]);
		return files;
	}

	/**
	 * Return the paths chosen by the user in the "OCaml project Paths" preference dialog, as a
	 * string made from a succession of "-I";"path" so that they can be used as a parameter for the
	 * OCaml compiler. Some paths are relative to the workspace, and some others are absolute.
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
	 * Return <code>true</code> if <code>file</code> is a cmo, cmi, cmx, exe, annot, o, or
	 * automatically generated file.
	 */
	public static boolean isGeneratedFile(IFile file) {

		final String fileName = file.getName();
		if (fileName.equals(".project") || fileName.equals(OcamlPaths.PATHS_FILE))
			return false;

		// Test the IS_GEN property first, because we can have extension-less automatically
		// generated files
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
			String value = file.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
					propertyName));
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
			String value = folder.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
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
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName),
					value);
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
			file.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName),
					value);
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
			folder.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, propertyName),
					value);
		} catch (Exception e) {
			OcamlPlugin.logError("error in Misc:setFolderProperty :", e);
		}
	}

	/**
	 * Append text to the OCaml compiler output (in a UI-Thread)
	 */
	public static void appendToOcamlConsole(final String msg) {

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				try {
					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
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
		String iconPath = "icons/";
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

	/**
	 * Get the file with the given extension corresponding to the given ml file.
	 * 
	 * @param project
	 *            the project in which the ml file resides
	 * @param mlPath
	 *            the path of the ml file relative to the workspace
	 * @param extension
	 *            the extension of the wanted file (".annot", ".cmo", ...)
	 */
	public static File getOtherFileFor(IProject project, IPath mlPath, String extension) {
		String projectPath = project.getLocation().toOSString() + File.separator;

		if (mlPath.segmentCount() < 2)
			return null;

		// remove the project name from the path
		mlPath = mlPath.removeFirstSegments(1);

		String filename = mlPath.lastSegment();

		if (filename.endsWith(".ml")) {
			String annotFilename = filename.substring(0, filename.length() - 3) + extension;

			IPath basePath = mlPath.removeLastSegments(1).append(annotFilename);

			// first, try with a .annot in the same directory
			File annotFile1 = new File(projectPath + basePath.toOSString());
			if (annotFile1.exists())
				return annotFile1;

			// then, try with a .annot in the _build/ directory + same directory
			File annotFile2 = new File(projectPath + "_build" + File.separator
					+ basePath.toOSString());
			if (annotFile2.exists())
				return annotFile2;
		}

		return null;
	}
	
	/**
	 * Get the file with the given extension corresponding to the given ml file.
	 * 
	 * @param mlPath
	 *            the full absolute filesystem path of the ml file
	 * @param extension
	 *            the extension of the wanted file (".annot", ".cmo", ...)
	 */
	public static File getOtherFileFor(IPath mlPath, String extension) {

		if (mlPath.segmentCount() < 1)
			return null;

		String filename = mlPath.lastSegment();

		if (filename.endsWith(".ml")) {
			String annotFilename = filename.substring(0, filename.length() - 3) + extension;

			IPath basePath = mlPath.removeLastSegments(1).append(annotFilename);

			// first, try with a .annot in the same directory
			File annotFile = new File(basePath.toOSString());
			if (annotFile.exists())
				return annotFile;
		}

		return null;
	}
	
	/** Remove carriage returns */
	public static String CRLFtoLF(String str){
		return str.replace("\r", "");
	}
	
	private static HashSet<String> keywordsHashset;
	
	/** Can this character be part of an OCaml identifier */
	public static boolean isOcamlIdentifierChar(char c) {
		return (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '\'');
	}

	/** Can this character be the first character of an OCaml identifier (lowercase)*/
	public static boolean isOcamlIdentifierFirstChar(char c) {
		return (c >= 'a' && c <= 'z' || c == '_');
	}

	/** Test whether this string is a valid OCaml identifier (lowercase) */
	public static boolean isValidOcamlIdentifier(String str) {
		
		if(str.length() < 1)
			return false;
		
		char[] chars = new char[str.length()];
		str.getChars(0, str.length(), chars, 0);
		
		if(!isOcamlIdentifierFirstChar(chars[0]))
			return false;

		for(int i = 1; i < chars.length; i++)
			if(!isOcamlIdentifierChar(chars[i]))
		return false;

		if(keywordsHashset == null) {
			keywordsHashset = new HashSet<String>(ILanguageWords.keywords.length);
			for(String word : ILanguageWords.keywords)
				keywordsHashset.add(word);
		}
		
		if(keywordsHashset.contains(str))
			return false;
		
		return true;
	}

	/** Save a persistent property in both <workspace>/.metadata/ and <project>/.settings/
	 * @param resource
	 * @param name
	 * @param value
	 */
	public static void setShareableProperty(IResource resource, String name, String value) {
		setShareableProperty(resource, new QualifiedName(OcamlPlugin.QUALIFIER, name), value);
	}
	
	public static void setShareableProperty(IResource resource, QualifiedName name, String value) {
		String preferenceName = name.getLocalName();
		IPath resourcePath = resource.getFullPath(); 
		try {
			// 1. save in <project>/.settings/ocaml.pref
			Preferences settings = getPreferences(resource.getProject(), preferenceName, true);
			if (value == null || value.trim().length() == 0)
				settings.remove(getKeyFor(resourcePath));
			else
				settings.put(getKeyFor(resourcePath), value);
			// TODO disable the listener (if any) so we don't react to changes made by ourselves
			settings.flush();
			
			// 2. also save in <workspace>/.medatada to decorate icons (see plugin.xml) 
			resource.setPersistentProperty(name, value);
			
		} catch (BackingStoreException e) {
			OcamlPlugin.logError("error in OcamlPlugin.setPersistentProperty", e);
		} catch (CoreException e) {
			
		}
	}

	/** Load a property from <project>/.settings/. 
	 *  If it does not exist, fallback to see <workspace>/.metadata/ */
	public static String getShareableProperty(IResource resource, String name) {
		return getShareableProperty(resource, new QualifiedName(OcamlPlugin.QUALIFIER, name));
	}

	public static String getShareableProperty(IResource resource, QualifiedName name) {
		String value = getShareablePropertyNull(resource, name);
		return value==null ? "" : value;
	}

	public static String getShareablePropertyNull(IResource resource, String name) {
		return getShareablePropertyNull(resource, new QualifiedName(OcamlPlugin.QUALIFIER, name));
	}

	public static String getShareablePropertyNull(IResource resource, QualifiedName name) {

		Preferences prefs = getPreferences(resource.getProject(), name.getLocalName(), false);
		if (prefs == null) {
			return migrateOldSettingIfAny(resource, name);
		}
		
		String value = prefs.get(getKeyFor(resource.getFullPath()), null);
		if(value!=null) {
			return value;
		} else {
			return migrateOldSettingIfAny(resource, name);
		}
	}
	
	// for backward compatibility
	static String migrateOldSettingIfAny(IResource resource, QualifiedName name) {
		String oldVerSetting = null;
		try {
			oldVerSetting = resource.getPersistentProperty(name);
		} catch(CoreException e) {
			OcamlPlugin.logError("error in OcamlPlugin.getPersistentProperty, reading old format", e);
		}
		return oldVerSetting;
	}

	private static Preferences getPreferences(IProject project, String preferenceName, boolean create) {
		if (create) {
			return new ProjectScope(project).getNode(OcamlPlugin.QUALIFIER).node(preferenceName);
		}
		// prevent creating non-existent path
		Preferences node = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE);
		try {
			if (!node.nodeExists(project.getName()))
				return null;
			node = node.node(project.getName());
			if (!node.nodeExists(OcamlPlugin.QUALIFIER))
				return null;
			node = node.node(OcamlPlugin.QUALIFIER);
			if (!node.nodeExists(preferenceName))
				return null;
			return node.node(preferenceName);
		} catch (BackingStoreException e) {
			OcamlPlugin.logError("error in OcamlPlugin.getPreferences", e);
		}
		return null;
	}
	
	private static String getKeyFor(IPath resourcePath) {
		return resourcePath.segmentCount() > 1 ? resourcePath.removeFirstSegments(1).toString() : "<project>";
	}

}
