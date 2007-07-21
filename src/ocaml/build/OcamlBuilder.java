package ocaml.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ocaml.OcamlPlugin;
import ocaml.build.graph.CompilerVisitor;
import ocaml.build.graph.DependenciesSetter;
import ocaml.build.graph.IPostNeededFilesVisitor;
import ocaml.build.graph.LayersGraph;
import ocaml.build.graph.LinkerVisitor;
import ocaml.build.graph.Vertex;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;

/**
 * This class is responsible for building a standard O'Caml project (without makefile).<br>
 * It is instantiated when the project is created or opened. There is an instance of this class for each
 * standard O'Caml project.
 * 
 */
public class OcamlBuilder extends IncrementalProjectBuilder {

	/**
	 * The dependencies graph associated with a project. It has the same life cycle as the compiler.
	 */
	private LayersGraph dependenciesGraph;

	/**
	 * The name of the external resources directory, automatically created when computing dependencies
	 */
	public final static String EXTERNALFILES = "External files";

	/**
	 * One of the values that can be taken by the persistent property COMPIL_MODE, that indicates that the
	 * project must be compiled in "native" mode
	 * 
	 * @see COMPIL_MODE
	 */
	public static final String NATIVE = "native";

	/**
	 * One of the values that can be taken by the persistent property COMPIL_MODE, that indicates that the
	 * project must be compiled in "byte-code" mode
	 * 
	 * @see COMPIL_MODE
	 */
	public static final String BYTE_CODE = "byte-code";

	/**
	 * The compilation mode for the project: either NATIVE or BYTE_CODE
	 */
	public static final String COMPIL_MODE = "compil_mode";

	/**
	 * Name of a file persistent property. This property, when present (value=true) on an O'Caml file, means
	 * that the file contains warnings from the compiler.
	 */
	public static final String COMPILATION_WARNINGS = "compilation_warnings";

	/**
	 * Name of a file persistent property. This property, when present (value=true) on an O'Caml file, means
	 * that the file contains errors from the compiler.
	 */
	public static final String COMPILATION_ERRORS = "compilation_errors";

	/**
	 * Persistent property that identifies automatically generated resources during compilation.
	 */
	public static final String IS_GEN = "is_gen";

	/**
	 * @see FLAGS
	 */
	public static final String FLAGS_SEPARATOR = "\u00a0";

	/**
	 * Name of a persistent property corresponding to the flags to add to a resource for its compilation.<br>
	 * The associated value is a list of flags delimited by FLAGS_SEPARATOR.
	 */
	public static final String FLAGS = "flags";

	/**
	 * Name of a persistent property corresponding to the name of the object file to link for an external file
	 * during compilation.
	 */
	public static final String OBJECTFILE = "object_file_to_link";

	/**
	 * Identifier for the session property corresponding to the dependencies graph associated to the project.
	 */
	public static final String DEPENDENCIESGRAPH = "dependenciesGraph";

	/** This constructor is mandatory according to the documentation */
	public OcamlBuilder() {
		// TODO : useless since startupOnInitialize will initialize the graph
		dependenciesGraph = new LayersGraph();
	}

	/**
	 * This method is executed one the instantiation and initialization are done: that's here we build the
	 * initial dependencies graph.
	 * 
	 * @see IncrementalProjectBuilder#startupOnInitialize
	 */
	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();

		/*
		 * We discard the previous compiler state to force a full recompilation. This must be done because
		 * when the project is closed we delete all the automatically generated files, so we must regenerate
		 * them when it is opened.
		 */
		forgetLastBuiltState();
		// initialize the dependencies graph
		dependenciesInitializing(getProject());
	}

	/**
	 * Initialize the dependencies graph from scratch, by computing all the dependencies in the project, and
	 * modify the graph associated with this Builder.<br>
	 * Caution: this method mustn't be used by any other method than clean or startupOnInitialize, because it
	 * discards the existing graph!
	 * 
	 * @param project
	 *            the project in which to compute dependencies
	 */
	private void dependenciesInitializing(final IProject project) {

		// reinitialize the graph
		this.dependenciesGraph = new LayersGraph();
		// the files to process. They are found by the next visitor.
		final List<IFile> files = new ArrayList<IFile>();
		// The paths to add to the project
		final List<IPath> paths = new ArrayList<IPath>();

		try {
			project.accept(new SuitableFilesFinder(files, paths));
		} catch (CoreException e) {
			OcamlPlugin.logError("error in OcamlBuilder:dependenciesInitializing:"
					+ " error finding files to add in graph building", e);
			Misc.popupErrorMessageBox("unexpected error in file's system browsing : "
					+ "project's cleaning should be wiser", "error in file's system browsing");
		}

		// add the paths we found
		if (!paths.isEmpty())
			OcamlPaths.addToPaths(paths, project);

		// add the files to the graph
		addFilesToGraph(this.dependenciesGraph, files);

	}

	/**
	 * Clean a project: remove the automatically generated mli files, and the cmo, cmi, cmx, and cmxa files.
	 * Reset the dependencies graph and remove problem markers.
	 */
	@Override
	protected void clean(IProgressMonitor monitor) throws CoreException {

		monitor.beginTask("cleaning", IProgressMonitor.UNKNOWN);

		super.clean(monitor);

		final IProject project = getProject();

		// remove the project markers
		monitor.subTask("deleting project's makers");
		project.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
		Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_ERRORS, null);
		Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS, null);
		monitor.worked(1);

		project.accept(new CleanVisitor(monitor));

		monitor.subTask("dependencies recomputing");
		// reinitialize the dependencies graph
		dependenciesInitializing(project);
		monitor.worked(1);

		monitor.done();
	}

	/**
	 * 
	 * compile the project depending on <code>kind</code>:
	 * 
	 * 10 -- INCREMENTAL_BUILD : only compile modified files (most common case)<br>
	 * 6 -- FULL_BUILD: compile the whole project, either at its creation or after a clean <br>
	 * 9 -- AUTO_BUILD : called after each save if the automatic building is activated <br>
	 * 15 -- CLEAN_BUILD: it doesn't seem to be called, instead we have clean and then fullBuild<br>
	 * 
	 * associate the dependencies graph to the project at each compilation
	 */
	private static int nBuild = 1;

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {

		Misc.appendToOcamlConsole("===== Build n°" + nBuild++ + " =====");

		getProject().deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);

		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			/*
			 * If null is returned, then the documentation says we must consider that any change might have
			 * happened. So, we do a full build.
			 */
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}

		// Re-associate the graph to the project
		OcamlBuilder.setDependenciesGraph(getProject(), dependenciesGraph);
		return null;
	}

	/**
	 * The compilation is divided in two phases:
	 * <ul>
	 * <li> 1 - Compile without linking all the files. This compiles all the files with their dependencies, in
	 * the right order. This creates cmo, cmi and cmx files.
	 * <li> 2 - Link: find the files with the "make_exe" property (see {@link Misc#MAKE_EXE} ) and compile
	 * them with the dependencies and an executable name.
	 * </ul>
	 */
	protected void fullBuild(IProgressMonitor monitor) {

		final IProject project = getProject();

		monitor.beginTask("Building", 4);

		// Phase 1 : compile the graph layers; done by this visitor
		monitor.subTask("Compiling graph layers");
		dependenciesGraph.accept(new CompilerVisitor(dependenciesGraph), monitor);
		updateProjectErrorDecorator();
		monitor.worked(1);

		// Phase 2 : refresh the file system so that the linker will see all the cm* files that were created
		monitor.subTask("Refreshing file system");
		Misc.refreshFileSystem(project, monitor);
		monitor.worked(1);

		// Phase 3: link the executable files
		monitor.subTask("Linking executables");
		dependenciesGraph.accept(new LinkerVisitor(), monitor);
		monitor.worked(1);

		// Phase 4 : refresh the file system again, so that the generated exe files will get the "IS_GEN"
		// property
		monitor.subTask("Refreshing file system");
		Misc.refreshFileSystem(project, monitor);
		monitor.worked(1);

		monitor.done();

	}

	/** Incremental build: compile only modified resources. */
	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
		try {
			final IProject project = getProject();

			monitor.beginTask("Building", 7);

			// Phase 1 : find the files to compile (the delta)
			// Note : this will certainly delete files from the graph
			monitor.subTask("finding suitable files");

			final List<IFile> files = new ArrayList<IFile>();
			final List<IPath> paths = new ArrayList<IPath>();

			delta.accept(new SuitableFilesDeltaFinder(files, paths, dependenciesGraph));

			if (!paths.isEmpty())
				OcamlPaths.addToPaths(paths, project);

			monitor.worked(1);

			// Phase 2 : build a dependencies graph for the modified files
			monitor.subTask("computing dependencies");
			final LayersGraph deltaGraph = new LayersGraph(dependenciesGraph.getLayersCount());

			addFilesToGraph(dependenciesGraph, deltaGraph, files);
			// addFilesToGraph recomputes graph layers, so we must make sure there are enough layers
			while (deltaGraph.getLayersCount() < dependenciesGraph.getLayersCount()) {
				deltaGraph.addEmptyLayer();
			}

			monitor.worked(1);

			// Phase 3 : compile without linking
			/*
			 * We visit the graph layer by layer. If a layer has changes, all the files affected by files from
			 * this layer will be added to the graph being visited, and will be handled by the following
			 * layers.
			 */
			monitor.subTask("Compiling graph layers");
			deltaGraph.accept(new CompilerVisitor(deltaGraph), monitor);
			updateProjectErrorDecorator();
			monitor.worked(1);

			// Phase 4 : refresh the file system
			monitor.subTask("refreshing file system");
			Misc.refreshFileSystem(project, monitor);
			monitor.worked(1);

			// Phase 5 : Re-link the old affected executables, and the new ones which were created
			monitor.subTask("Linking executables ");
			deltaGraph.accept(new LinkerVisitor(), monitor);
			monitor.worked(1);

			// Phase 6: merge the two graphs
			monitor.subTask("Merging graphs");
			dependenciesGraph.mergeWith(deltaGraph);
			monitor.worked(1);

			// Phase 7 : refresh the file system again to find files generated by the linking phase
			monitor.subTask("refreshing file system");
			Misc.refreshFileSystem(project, monitor);
			monitor.worked(1);

			monitor.done();

		} catch (CoreException e) {
			OcamlPlugin.logError("error in OcamlBuilder:incrementalBuild :", e);
		}

	}

	protected void addFilesToGraph(LayersGraph graphToFill, List<IFile> filesToAdd) {
		this.addFilesToGraph(null, graphToFill, filesToAdd);
	}

	/**
	 * Add vertices (corresponding to files) to a graph. The vertices contain the files dependencies, and they
	 * are arranged by layers. We start by looking for references in <code>referenceGraph</code> if it
	 * exists. This way, we get two graphs in the end, with a graph containing only the added files, arranged
	 * by layers.
	 * 
	 * @param referenceGraph
	 *            the graph where to look for already existing required files
	 * @param graphToFill
	 *            the graph to fill with the files to add
	 * @param filesToAdd
	 *            the files to add to the graph (chosen by {@link SuitableDeltaFilesFinder}).
	 */
	protected void addFilesToGraph(LayersGraph referenceGraph, LayersGraph graphToFill, List<IFile> filesToAdd) {
		// The visitor that is going to compute dependencies
		IPostNeededFilesVisitor depsCreatorVisitor;

		// The vertices returned by this visitor
		final List<Vertex> verticesToAdd = new ArrayList<Vertex>(filesToAdd.size());

		// The possible external files already present in the "External Files" directory
		final List<IFile> oldExternalFiles = new ArrayList<IFile>();

		// If there is no reference graph, we build our graph automatically, and we have to update the files
		// in "External Files"*/
		if (referenceGraph == null) {
			// Récupérer tous les fichiers qui sont dans le dossier external
			// files afin de comparer ce qui ne sert plus.
			final IFolder externalFiles = getProject().getFolder(OcamlBuilder.EXTERNALFILES);

			try {
				// Ne rien faire si le dossier n'existe pas (il sera créé si
				// besoin lors du calcul des dépendances)
				if (externalFiles.exists()) {
					final IResource[] oldExtFilesAsResource = externalFiles.members();
					for (IResource oldFile : oldExtFilesAsResource) {
						if (oldFile.getType() != IResource.FILE) {
							OcamlPlugin.logError("error in OcamlBuilder:" + "addFilesToGraph: "
									+ "external non file found");
						} else {
							// On ajoute tous les fichiers du dossier
							oldExternalFiles.add((IFile) oldFile);
						}
					}
				}

			} catch (CoreException e1) {
				OcamlPlugin.logError("error in OcamlBuilder:" + "addFilesToGraph: error finding members", e1);
			}

			depsCreatorVisitor = new DependenciesSetter(verticesToAdd);
		} else {
			// Sinon il faut tenir compte du graphe passé pour chercher
			// certaines références, et spécifier aussi les fichiers que l'on
			// traite afin d'ignorer les autres
			depsCreatorVisitor = new DependenciesSetter(referenceGraph, verticesToAdd, filesToAdd);
		}

		// On traite chacun des fichiers sauf ceux déjà visités par le visiteur.
		// Ce dernier est également capable de reconnaitre un fichier déjà
		// visité, et ne le visitera pas. (Indispensable si deux fichiers
		// dépendent d'un fichier qui n'est pas dans filesToAdd : il ne faut pas
		// le visiter deux fois.)
		for (IFile handledFile : filesToAdd) {
			// Regarder en premier si le visiteur n'a pas déjà visité ce
			// fichier, auquel cas ne pas le visiter.
			boolean hasBeenVisited = false;
			for (Vertex vertex : verticesToAdd) {
				if (vertex.getFile().equals(handledFile)) {
					hasBeenVisited = true;
				}
			}
			// S'il n'a pas été visité faire :
			if (!hasBeenVisited) {
				// récupérer si elle existe la référence du sommet correspondant
				// dans le graphe de référence s'il existe
				Vertex handledVertex = null;
				if (referenceGraph != null) {
					// findVertex renvoit le sommet s'il existe, null sinon
					handledVertex = referenceGraph.findVertex(handledFile);
				}
				// Si on a rien trouvé dans le graphe ou s'il n'y en a pas
				if (handledVertex == null) {
					// Créer un sommet en gérant le fait qu'il puisse référencer
					// un fichier externe au projet
					handledVertex = new Vertex(handledFile, handledFile.isLinked());
				}
				// Visiter le sommet (ceci construira tous les sommets
				// dépendants et les mettra dans verticesToAdd
				handledVertex.accept(depsCreatorVisitor);
			}
		}

		// Supprimer les fichiers externes qui ne sont plus utilisés (si l'on
		// avait un graphe de référence, cette liste est vide).
		for (Vertex vertex : verticesToAdd) {
			if (vertex.isExternalFile()) {
				oldExternalFiles.remove(vertex.getFile());
			}
		}
		// A partir d'ici, tout ce qui reste dans oldExternalFiles est à
		// supprimer
		for (IFile extFile : oldExternalFiles) {
			try {
				extFile.delete(true, null);
			} catch (CoreException e) {
				OcamlPlugin.logError("error in OcamlBuilder:" + "addFilesToGraph: error deleting "
						+ extFile.getName(), e);
			}

		}

		// Il ne reste plus qu'à ajouter tous ces sommets au nouveau graphe
		// (Ceci va les mettre dans la bonne couche)
		graphToFill.addAll(verticesToAdd);

	}

	/**
	 * Refresh the project error decorator.<br>
	 * We go through all the project's files. If we find a file with a warning, we display it. If we find one
	 * with an error, we display it an we stop visiting the files.
	 */
	protected void updateProjectErrorDecorator() {
		final IProject project = getProject();
		final IResourceVisitor visitor = new IResourceVisitor() {

			public boolean visit(IResource resource) throws CoreException {
				// S'il y a des erreurs de compilation, répercuter le changement
				// sur le projet et arreter la visite.
				if (!Misc.getResourceProperty(resource, OcamlBuilder.COMPILATION_ERRORS).equals("")) {
					Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_ERRORS, "true");
					Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS, null);
					return false;
				}
				// S'il y a des des warnings, répercuter le changement sur le
				// projet uniquement s'il n'y a pas d'erreur et continuer la
				// visite.
				if (!Misc.getResourceProperty(resource, OcamlBuilder.COMPILATION_WARNINGS).equals("")
						&& Misc.getResourceProperty(resource, OcamlBuilder.COMPILATION_ERRORS).equals(""))
					Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS, "true");

				return true;
			}

		};
		try {
			// Commencer par enlever les marqueurs sur le projet
			Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_ERRORS, null);
			Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS, null);
			// Les ajuster comme il faut
			project.accept(visitor);
		} catch (CoreException e) {
			OcamlPlugin.logError("error in " + "CompilerVisitor:updateProjectErrorDecorator:"
					+ " error visiting resource", e);
		}
	}

	/** Set the dependencies graph on a project */
	public static void setDependenciesGraph(IProject project, LayersGraph graph) {
		final QualifiedName depGraphName = new QualifiedName(OcamlPlugin.QUALIFIER, DEPENDENCIESGRAPH);
		try {
			project.setSessionProperty(depGraphName, graph);
		} catch (CoreException e) {
			OcamlPlugin.logError("error retrieving dependencies graph", e);
		}
	}

	/**
	 * Get the dependencies graph from a project.
	 * 
	 * @return the dependencies graph, or <code>null</code> if it doesn't exist.
	 */
	public static LayersGraph getDependenciesGraph(IProject project) {
		final QualifiedName depGraphName = new QualifiedName(OcamlPlugin.QUALIFIER, DEPENDENCIESGRAPH);
		Object oGraph = null;
		try {
			oGraph = project.getSessionProperty(depGraphName);
		} catch (CoreException e) {
			OcamlPlugin.logError("error retrieving dependencies graph", e);
		}
		if (oGraph instanceof LayersGraph) {
			return (LayersGraph) oGraph;
		}
		return null;
	}

	/** Associate a list of object files to a resource as a persistent property. */
	public static void setExternalsObjectsFiles(IResource resource, List<String> files) {
		final StringBuffer allFilesInString = new StringBuffer();
		for (String file : files) {
			allFilesInString.append(file + FLAGS_SEPARATOR);
		}
		try {
			resource.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, OBJECTFILE),
					allFilesInString.toString());
		} catch (CoreException e) {
			OcamlPlugin.logError("error in OCamlProject:setExternalsObjectsFiles:"
					+ " error setting persitent property", e);
		}
	}

	/** Associate a list of flags to a resource as a persistent property. */
	public static void setResourceFlags(IResource resource, List<String> flags) {
		final StringBuffer allFlagsInString = new StringBuffer();
		for (String flag : flags) {
			allFlagsInString.append(flag + FLAGS_SEPARATOR);
		}
		try {
			resource.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, FLAGS), allFlagsInString
					.toString());
		} catch (CoreException e) {
			OcamlPlugin.logError("error in OCamlProject:setResourceFlags:"
					+ " error setting flags persitent property", e);
		}
	}

	/** Get the list of external files associated with an external resource. */
	public static List<String> getExternalObjectFiles(IResource resource) {
		final List<String> files = new ArrayList<String>();
		if (!resource.exists()) {
			return files;
		}

		String allFilesInString;
		try {
			allFilesInString = resource.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
					OBJECTFILE));
		} catch (CoreException e) {
			OcamlPlugin.logError("error in OCamlProject:getExternalObjectFiles:"
					+ " error getting persitent property", e);
			return files;
		}

		if (allFilesInString == null) {
			return files;
		}
		// On se sert du même séparateur que pour les flags
		for (String file : allFilesInString.split(FLAGS_SEPARATOR)) {
			if (!file.equals("")) {
				files.add(file);
			}
		}

		return files;
	}

	/** Get the list of resource flags associated with a resource. */
	public static List<String> getResourceFlags(IResource resource) {
		final List<String> flags = new ArrayList<String>();
		if (!resource.exists()) {
			return flags;
		}

		String allFlagsInString;
		try {
			allFlagsInString = resource
					.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, FLAGS));
		} catch (CoreException e) {
			OcamlPlugin.logError("error in OCamlProject:getResourceFlags:"
					+ " error getting flags persitent property", e);
			return flags;
		}

		if (allFlagsInString == null) {
			return flags;
		}
		for (String flag : allFlagsInString.split(FLAGS_SEPARATOR)) {
			if (!flag.equals("")) {
				flags.add(flag);
			}
		}

		return flags;
	}
}
