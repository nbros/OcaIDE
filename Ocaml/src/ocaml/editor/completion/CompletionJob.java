package ocaml.editor.completion;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.parser.Def;
import ocaml.parsers.OcamlNewInterfaceParser;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

/**
 * This Eclipse Job is used to parse the OCaml library files in the background with a progress
 * indicator, so that the first completion is fast (the following ones are always going to be fast,
 * because we cache the results).
 * 
 * <p>
 * This class is also used in a static way to immediately compute completions (without running
 * another thread).
 */
public class CompletionJob extends Job {

	private static boolean bParsingInterfacesDone = false;

	private static FilenameFilter mlmliFilter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			return name.endsWith(".mli") || name.endsWith(".ml");
		}
	};

	private final IProject project;

	public CompletionJob(String name, IProject project) {
		super(name);
		this.project = project;
	}

	public static boolean isParsingFinished() {
		return bParsingInterfacesDone;
	}

	// we cache the definitions "super-tree" for a short time
	private static long lastTime = 0;

	private static Def lastDefinitionsRoot = null;

	private static IProject lastProject = null;

	/**
	 * The duration in milliseconds during which the definitions super-tree is kept in memory. If
	 * another completion is asked during this time the search won't get executed, and so the
	 * completion box will appear immediately but without the latest modifications done during this
	 * duration.
	 */
	private static final int cacheTime = 2000;

	/**
	 * Build the super-tree of all definitions found in mli files in the project directories (this
	 * normally includes the OCaml standard library). This method is defined in class
	 * CompletionJob, but it is not a job, it is executed in the same thread as the caller.
	 * 
	 * @param bUsingEditor
	 *            use the editor to add opened modules to the definitions tree
	 */
	public static Def buildDefinitionsTree(IProject project, boolean bUsingEditor) {
		
		// TODO: use a change listener instead of testing each file for modifications each time
		
		Def definitionsRoot = null;

		/*
		 * We use the super-tree in cache instead of rebuilding it if the last completion dates from
		 * less than a few seconds. This allows us to speed up typing in the completion box (if we
		 * didn't do this, the super-tree would be rebuilt for each character typed in the
		 * completion box)
		 */

		if (lastDefinitionsRoot != null && project.equals(lastProject)
				&& System.currentTimeMillis() - lastTime < cacheTime)
			definitionsRoot = lastDefinitionsRoot;
		else {
			// System.err.println("building definitions tree");
			/*
			 * Build a super-tree from all the definitions trees so as to be able to use a recursive
			 * function. The root of this super-tree contains all the modules accessible from the
			 * project + the members of opened modules ("Pervasives" being always opened by default)
			 */
			definitionsRoot = new Def("<root>", Def.Type.Root, 0, 0);
			OcamlNewInterfaceParser parser = OcamlNewInterfaceParser.getInstance();

			OcamlPaths ocamlPaths = new OcamlPaths(project);

			String[] paths = ocamlPaths.getPaths();
			// for each path in the project
			for (String path : paths) {

				if (path.equals(".")) {
					IPath projectPath = project.getLocation();
					if (projectPath != null)
						path = projectPath.toOSString();
					else {
						OcamlPlugin
								.logError("Error in CompletionJob:buildDefinitionsTree : project location is null");
						continue;
					}

				}

				// try with a path relative to the project location
				// Go through the IResources API to follow possibly linked resources.
				File dir = null;
				try {
					IFolder folder = project.getFolder(path);
					IPath location = folder.getLocation();
					if (location != null)
						dir = new File(location.toOSString());
				} catch (Throwable e) {
					OcamlPlugin.logError("Error trying relative path in completion job: " + path, e);
				}

				// try with an absolute path
				if (!(dir != null && dir.exists() && dir.isDirectory())) {
					dir = new File(path);
				}

				if (!(dir.exists() && dir.isDirectory())) {
					OcamlPlugin.logError("Wrong path:" + dir.toString() + " (in project:"
							+ project.getName() + ")");
					continue;
				}

				// get all the ml and mli files from the directory
				String[] mlmliFiles = dir.list(mlmliFilter);

				/*
				 * keep all the mli files, and discard the ml files when there is a mli file with
				 * the same name
				 */
				String[] files = Misc.filterInterfaces(mlmliFiles);

				// for each file
				for (String mlmlifile : files) {
					File file = new File(dir.getAbsolutePath() + File.separatorChar + mlmlifile);
					Def def = parser.parseFile(file, false);
					if (def != null)
						definitionsRoot.children.add(def);
				}
			}

			if (bUsingEditor) {

				// find all the opened modules and add their definitions tree to the root of the
				// super-tree
				String[] openModules = null;
				try {

					IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage().getActiveEditor();
					if (editorPart instanceof OcamlEditor) {
						OcamlEditor editor = (OcamlEditor) editorPart;

						IFile ifile = editor.getFileBeingEdited();
						if (ifile != null) {

							String file = ifile.getName();
							String moduleName = null;
							if (file.endsWith(".ml")) {
								// remove the file extension and change the first character to
								// uppercase
								moduleName = "" + Character.toUpperCase(file.charAt(0))
										+ file.substring(1, file.length() - 3);
							}

							// get the text of the document currently opened in the editor
							String doc = editor.getDocumentProvider().getDocument(
									editor.getEditorInput()).get();
							openModules = findOpenModules(doc, moduleName);
						}
					}
				} catch (Exception e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}

				if (openModules != null) {
					for (String module : openModules) {
						Def defModule = null;

						for (Def def : definitionsRoot.children)
							if (def.type == Def.Type.Module && def.name.equals(module)) {
								defModule = def;
								break;
							}

						if (defModule != null) {
							for (Def def : defModule.children)
								definitionsRoot.children.add(def);
						}
					}
				}
			}

		}

		lastDefinitionsRoot = definitionsRoot;
		lastTime = System.currentTimeMillis();
		lastProject = project;

		return definitionsRoot;
	}

	static final Pattern patternOpen = Pattern.compile("(\\A|\\n) *open +(\\w*)");

	/** Find the opened modules by looking for "open moduleName" directives in the source code */
	private static String[] findOpenModules(String doc, String moduleName) {
		TreeSet<String> openModules = new TreeSet<String>();

		if (moduleName != null)
			openModules.add(moduleName);

		Matcher matcher = patternOpen.matcher(doc);
		while (matcher.find()) {
			String name = matcher.group(2).trim();
			if (name.endsWith(";;"))
				name = name.substring(0, name.length() - 2);
			openModules.add(name);
		}

		openModules.add("Pervasives");

		return openModules.toArray(new String[0]);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try {

			OcamlNewInterfaceParser parser = OcamlNewInterfaceParser.getInstance();

			// get all the mli files from the directory
			String[] mlmliFiles = null;

			// no project => parse OCaml library files
			if (this.project == null) {
				File dir = new File(OcamlPlugin.getLibFullPath());
				if (!dir.exists() || !dir.isDirectory()) {
					OcamlPlugin.logWarning("Parsing of mli files aborted : directory not found");
					return Status.CANCEL_STATUS;
				}

				mlmliFiles = Misc.filterInterfaces(dir.list(mlmliFilter));
				for (int i = 0; i < mlmliFiles.length; i++)
					mlmliFiles[i] = dir.getAbsolutePath() + File.separatorChar + mlmliFiles[i];

			} else {
				IFile[] projectFiles = Misc.getProjectFiles(project);

				ArrayList<String> strFiles = new ArrayList<String>();
				for (IFile file : projectFiles) {
					IPath path = file.getLocation();
					String ext = file.getFileExtension();
					if (path != null && ("ml".equals(ext) || "mli".equals(ext)))
						strFiles.add(path.toOSString());
				}

				mlmliFiles = strFiles.toArray(new String[0]);
				mlmliFiles = Misc.filterInterfaces(mlmliFiles);
			}

			if (mlmliFiles == null)
				return Status.CANCEL_STATUS;

			monitor.beginTask("Parsing mli files", mlmliFiles.length);

			Thread.yield();

			// for each ml or mli file
			for (String mlmliFile : mlmliFiles) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;

				monitor.subTask(mlmliFile);

				File file = new File(mlmliFile);
				parser.parseFile(file, false);

				// for(int i = 0; i < 1000000000; i++);

				monitor.worked(1);
				Thread.yield();
			}
		} finally {

			monitor.done();
			bParsingInterfacesDone = true;
		}

		return Status.OK_STATUS;
	}
}
