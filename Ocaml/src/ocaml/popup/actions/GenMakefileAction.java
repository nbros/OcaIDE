package ocaml.popup.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.build.graph.IExecutablesVisitor;
import ocaml.build.graph.ILayersVisitor;
import ocaml.build.graph.LayersGraph;
import ocaml.build.graph.Vertex;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This action is called when the user clicks on the "Generate Makefile" menu item in the pop-up for the
 * OCaml managed projects.
 * 
 * We visit the graph layers, and generate the makefile as we go along.
 * 
 */

public class GenMakefileAction extends Object implements IObjectActionDelegate {

	/**
	 * constants for variables to write in the makefile
	 */
	final String CR = "\n";

	final String SP = " ";

	final String TAB = "\t";

	final String RM = "rm -rf";

	final String INCLUDEDPATHS = "INCLUDEDPATHS";

	final String OCAMLC = "OCAMLC";

	final String PROJECTFLAGS = "PROJECTFLAGS";

	final String OPT = "OPT";

	final String ANNOT = "ANNOT";

	final String SRC = "SRC";

	final String MLFILES = "MLFILES";

	final String EXEC = "EXEC";

	final String OBJ = "OBJ";

	final String ITFOBJ = "ITFOBJ";

	final String TYPE = "TYPE";

	/**
	 * The project for which we want to generate a Makefile
	 */
	private IProject project = null;

	/**
	 * Initialization method, does nothing
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

	/**
	 * Start a compilation, and then visit the graph layers to generate the makefile. <br>
	 * remark : the compilation is mandatory to force the compiler to become instantiated if it wasn't yet.
	 */
	public void run(IAction action) {
		if (project != null) {
			Job job = new Job("Build project and generate makefile") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
					} catch (CoreException e) {
						OcamlPlugin.logError("error in building project" + " in order to generate makefile",
								e);
					}

					final LayersGraph depsGraph = OcamlBuilder.getDependenciesGraph(project);
					if (depsGraph != null) {
						// Initialization
						BufferedWriter writer = makefileInit(depsGraph, project);
						// Rules for all ml files
						depsGraph.accept(new CompileStageGenerator(writer), monitor);
						// Rules to link executables
						depsGraph.accept(new LinkStageGenerator(writer), monitor);
						try {
							writer.close();
						} catch (IOException e) {
							OcamlPlugin.logError("error trying to close writer", e);
						}
					} else {
						OcamlPlugin.logError("find a null graph even after "
								+ "a build when generating makefile");
					}
					// Refresh the file system to see the makefile appear
					Misc.refreshFileSystem(project, monitor);
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.BUILD);
			job.setUser(true);
			// Wait for the window to close before starting the Job
			job.schedule(500);
		}
	}

	/**
	 * Get the contents of the selection: the project and the build mode
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			Object o = sel.getFirstElement();
			if (o instanceof IProject) {
				this.project = (IProject) o;
			}

		}
	}

	/**
	 * Initialize a writer for the makefile.
	 * 
	 * Fill in all the basic fields, the project properties, the source files, the executables, the
	 * interfaces, the sanitizing rules...
	 * 
	 * @param graph
	 *            the graph on which to base the makefile
	 * @param project
	 *            the project in which we search for informations (compilation mode, included paths, flags...)
	 * @return a writer or <code>null</code> if there was a problem
	 */
	protected BufferedWriter makefileInit(final LayersGraph graph, final IProject project) {
		// We create the Makefile a the root of the project
		final java.io.File makefile = new File(project.getLocation().addTrailingSeparator().toOSString()
				+ "Makefile");
		try {
			makefile.createNewFile();
			// The writer we use to write in the makefile
			final BufferedWriter writer = new BufferedWriter(new FileWriter(makefile));

			// Start by the project properties

			// compilation mode
			writer.write("#Builder to use:" + CR);
			// native code
			if (Misc.getShareableProperty(project, OcamlBuilder.COMPIL_MODE).equals(OcamlBuilder.NATIVE)) {
				writer.write(OCAMLC + "= " + OcamlPlugin.getOcamloptFullPath());
				writer.write(CR);
				writer.write("#Is it native?" + CR);
				writer.write(OPT + "= yes");
			}
			// byte-code
			else {
				writer.write(OCAMLC + "= " + OcamlPlugin.getOcamlcFullPath());
				writer.write(CR);
				writer.write("#Is it native?" + CR);
				writer.write(OPT + "= no");
			}

			writer.write(CR);

			// Do we generate type informations (.annot files)
			writer.write("#Generate type information?" + CR);
			writer.write(ANNOT + "= yes");

			writer.write(CR);

			// Include paths
			final List<String> paths = Misc.getProjectPaths(project);
			writer.write("#Paths to include with each command: " + CR);
			writer.write(INCLUDEDPATHS + "= ");
			for (String path : paths) {
				// Make the paths absolute (they are relative to the project by default)
				if (path.startsWith(project.getName()))
					writer.write(project.getLocation().removeLastSegments(1).addTrailingSeparator()
							.toOSString()
							+ path);
				else
					writer.write(path);
				writer.write(SP);
			}

			writer.write(CR);

			// the project flags
			writer.write("#Flags for the project: " + CR);
			writer.write(PROJECTFLAGS + "= ");
			final List<String> flags = OcamlBuilder.getResourceFlags(project);
			for (String flag : flags) {
				writer.write(flag);
				writer.write(SP);
			}

			writer.write(CR);
			writer.write(CR);

			// Now, we write the source and executable files list
			final List<String> sources = new ArrayList<String>();
			final List<String> executables = new ArrayList<String>();
			graph.accept(new ILayersVisitor() {

				// Get the names of the source and executable files
				public boolean visit(final Vertex vertex, final IProgressMonitor monitor) {
					if (!vertex.isExternalFile()) {
						// The filename is relative to the project
						final String fileName = vertex.getFile().getProjectRelativePath().toOSString();
						final int vtype = vertex.getType();
						if ((vtype == Vertex.MLTYPE) || (vtype == Vertex.MLITYPE)) {
							sources.add(fileName);
							if (vertex.getExeName() != null) {
								executables.add(vertex.getExeName());
							}
						}

					}
					return true;
				}

			}, null);

			writer.write("#Source files list: " + CR);
			writer.write(SRC + "= ");
			for (String source : sources) {
				writer.write(source);
				writer.write(SP);
			}
			writer.write(CR);
			// Get the ml files only
			writer.write("#Find .ml files:" + CR);
			writer.write(MLFILES + "= $(filter %.ml,$(" + SRC + "))" + CR);
			writer.write("#Executables to produce:" + CR);
			writer.write(EXEC + "= ");
			for (String exe : executables) {
				writer.write(exe);
				writer.write(SP);
			}
			writer.write(CR);
			writer.write(CR);

			// Now we define the lists of files to delete
			// For the clean operation, this is:
			// ITFOBJ=$(MLFILES:.ml=.cmi)
			// ifeq ($(OPT),yes)
			// OBJ=$(MLFILES:.ml=.cmx)
			// else
			// OBJ=$(MLFILES:.ml=.cmo)
			// endif
			//		
			// ifeq ($(ANNOT),yes)
			// TYPE=$(MLFILES:.ml=.annot)
			// endif
			final String filesToRemove = ITFOBJ + "=$(" + MLFILES + ":.ml=.cmi)" + CR + "ifeq ($(" + OPT
					+ "),yes)" + CR + TAB + OBJ + "=$(" + MLFILES + ":.ml=.cmx)" + CR + "else" + CR + TAB
					+ OBJ + "=$(" + MLFILES + ":.ml=.cmo)" + CR + "endif" + CR + CR + "ifeq ($(" + ANNOT
					+ "),yes)" + CR + TAB + TYPE + "=$(" + MLFILES + ":.ml=.annot)" + CR + "endif" + CR + CR;
			writer.write("#Find out which files to erase with clean: " + CR);
			writer.write(filesToRemove);

			// The sanitizing rules:
			// clean:
			// rm -rf $(OBJ) $(ITFOBJ) $(TYPE)
			//
			// mrproper: clean
			// rm -rf $(EXEC)
			final String cleanRules = "clean:" + CR + TAB + RM + " $(" + OBJ + ") $(" + ITFOBJ + ") $("
					+ TYPE + ")" + CR + CR + "mrproper: clean" + CR + TAB + RM + " $(" + EXEC + ")" + CR + CR;
			writer.write("#Some cleaning rules : " + CR);
			writer.write(cleanRules);

			// Rules for compiling sources, executables, or both (redundant)
			// allExe : $(EXEC)
			//
			// allSrc : $(OBJ)
			//
			// all : allSrc allExe
			final String miscRules = "allExe: $(" + EXEC + ") " + CR + CR + "allSrc: $(" + OBJ + ") " + CR
					+ CR + "all: allSrc allExe" + CR + CR;
			writer.write("#Rules to build project : " + CR);
			writer.write(miscRules);

			return writer;
		} catch (IOException e) {
			OcamlPlugin.logError("error writing in Makefile", e);
			return null;
		}
	}

	/**
	 * A graph layers visitor, that compiles only these layers (-c flag)
	 */
	protected class CompileStageGenerator implements ILayersVisitor {

		private BufferedWriter writer;

		protected CompileStageGenerator(final BufferedWriter writer) {
			this.writer = writer;
		}

		/**
		 * Visit the graph layers. For each vertex, write the compilation command only (-c flag) with the
		 * included paths and the compilation flags
		 */
		public boolean visit(final Vertex vertex, final IProgressMonitor monitor) {
			Misc.appendToOcamlConsole("CompileStageGenerator visiting : " + vertex.getFile().getName());
			try {
				if (!vertex.isExternalFile()) {

					String objName = "";
					final int vtype = vertex.getType();
					if (vtype == Vertex.MLITYPE) {
						objName = vertex.getInterfaceObjectFile().getProjectRelativePath().toOSString();
					} else if (vtype == Vertex.MLTYPE) {
						objName = vertex.getObjectFile().getProjectRelativePath().toOSString();
					}
					writer.write(objName);
					writer.write(": ");
					// The name of each dependency
					for (Vertex depVertex : vertex.getNeededFiles()) {
						if (!depVertex.isExternalFile()) {
							String oname = "";
							final int depVType = depVertex.getType();
							if (depVType == Vertex.MLITYPE) {
								oname = depVertex.getInterfaceObjectFile().getProjectRelativePath()
										.toOSString();
							} else if (depVType == Vertex.MLTYPE) {
								oname = depVertex.getObjectFile().getProjectRelativePath().toOSString();
							}
							writer.write(oname + " ");
						}
					}
					writer.write(CR + TAB);
					// The command to launch, with its flags, and "-c" to compile only (no linking)
					writer.write("$(" + OCAMLC + ") $(" + INCLUDEDPATHS + ") $(" + PROJECTFLAGS + ") ");
					for (String fileFlag : OcamlBuilder.getResourceFlags(vertex.getFile())) {
						writer.write(fileFlag + " ");
					}
					writer.write("-c " + vertex.getFile().getProjectRelativePath().toOSString());
					writer.write(CR + CR);

				}
			} catch (IOException e) {
				OcamlPlugin.logError("error writing in Makefile", e);
			}
			monitor.worked(1);
			return true;
		}

	}

	/**
	 * A visitor for executables in the graph, that is responsible for the linking phase only (it adds "-o
	 * name")
	 */
	protected class LinkStageGenerator implements IExecutablesVisitor {

		private BufferedWriter writer;

		/**
		 * A constructor that asks for the writer as a parameter.
		 */
		public LinkStageGenerator(final BufferedWriter writer) {
			this.writer = writer;
		}

		/**
		 * Visit a vertex. Get the list of all required vertices, and add them to the command line.
		 */
		public boolean visit(final Vertex vertex, final IProgressMonitor monitor) {

			final String exeName = vertex.getExeName();
			try {

				// First, get the list of files to link in order to create the executable

				// objectFiles will contain all the object files to link
				final List<String> objectFiles = new ArrayList<String>();
				/*
				 * neededObjectFiles contains only the files to compile before the linking phase (in
				 * principle, all those from d'objectFiles that are not external)
				 */
				final List<String> neededObjectFiles = new ArrayList<String>();
				/*
				 * Add all the object files (no need for a project-relative path, the name suffices). It's the
				 * same code as LinkerVisitor.
				 */

				// For each file to link
				for (Vertex fileToLink : vertex.getAllFilesToLink()) {

					/* For external files: get the ordered list of files to link */
					if (fileToLink.isExternalFile()) {
						final List<String> extObjFiles = fileToLink.getExternalObjectFiles();
						for (String objFile : extObjFiles) {
							if (!objectFiles.contains(objFile)) {
								objectFiles.add(objFile);
							}
						}
					} else {
						final IFile objectFile = fileToLink.getObjectFile();
						/*
						 * If the associated object file is not null (it doesn't need to actually exist, we
						 * only want its name)
						 */
						if (objectFile != null) {
							final IPath objFilePath = objectFile.getProjectRelativePath();
							// Add its name to the list of flags
							if (!objectFiles.contains(objFilePath.lastSegment())) {
								objectFiles.add(objFilePath.lastSegment());
							}
							// And to the list of required object files
							if (!neededObjectFiles.contains(objFilePath.toOSString())) {
								neededObjectFiles.add(objFilePath.toOSString());
							}
						}
					}
				}

				writer.write(exeName + ": ");
				// Write the name of the object file
				writer.write(vertex.getObjectFile().getProjectRelativePath().toOSString() + " ");
				// And the list of object files to compile before it
				for (String neededObjFile : neededObjectFiles) {
					writer.write(neededObjFile + " ");
				}

				writer.write(CR + TAB);
				// The command to launch with the flags
				writer.write("$(" + OCAMLC + ") $(" + INCLUDEDPATHS + ") $(" + PROJECTFLAGS + ") ");
				for (String fileFlag : OcamlBuilder.getResourceFlags(vertex.getFile())) {
					writer.write(fileFlag + " ");
				}

				// Write the list of files to link
				for (String objFile : objectFiles) {
					writer.write(objFile + " ");
				}

				// The -o flag and the executable name
				writer.write("-o " + exeName + " ");
				// followed by the object file name
				writer.write(vertex.getObjectFile().getProjectRelativePath().toOSString() + CR + CR);

			} catch (IOException e) {
				OcamlPlugin.logError("error writing in Makefile", e);
			}

			return true;
		}

	}

}
