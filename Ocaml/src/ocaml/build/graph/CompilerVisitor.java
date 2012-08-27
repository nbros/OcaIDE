package ocaml.build.graph;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.build.ProblemMarkers;
import ocaml.exec.CommandRunner;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Visit a graph layer by layer, to compile all vertices in the right order.<br>
 * The algorithm is: save (rename by modifying the extension from cmi to cmi_old) the object file
 * corresponding to the interface associated to a vertex, so that any difference in the generated cmi will be
 * detected and will trigger a recompilation of all affected files.
 */
public class CompilerVisitor implements ILayersVisitor {

	/**
	 * The graph visited by this visitor. This is used to know where to add files we would want to visit in
	 * the next layer.
	 */
	private LayersGraph graph;

	/** Are we running on linux/Mac OS x */
	private boolean runOnLinux;

	/**
	 * Associate the graph to this visitor. If the compiling of a file in this graph has repercussions on
	 * files in another graph, then we must add all the files to this graph so as to handle them with the next
	 * layer.
	 */
	public CompilerVisitor(LayersGraph graph) {
		this.graph = graph;
		this.runOnLinux = OcamlPlugin.runningOnLinuxCompatibleSystem();
	}

	public boolean visit(final Vertex vertex, final IProgressMonitor monitor) {

		if ((monitor != null) && monitor.isCanceled())
			return false;

		// Tout d'abord, ignorer les fichiers externes (il n'y a rien à faire,
		// les fichiers objets à lier sont donnés par l'utilisateur dans une
		// propriété persistante.
		if (vertex.isExternalFile()) {
			return true;
		}

		// Le fichier à compiler
		final IFile file = vertex.getFile();

		// le projet
		final IProject project = file.getProject();

		// Récupérer Le mode de compilation (byte-code par défaut)
		String buildMode = Misc.getShareableProperty(project, OcamlBuilder.COMPIL_MODE);

		// Récupérer l'extension du fichier objet généré
		final String objectFileExt = (buildMode != null && buildMode.equals(OcamlBuilder.NATIVE) ? "cmx"
				: "cmo");

		// Son type (ml, mli etc..)
		final int fileType = vertex.getType();

		// le résultat du lancement de la commande (true s'il n'y a pas eu
		// d'erreurs)
		boolean noErrors = true;

		// Une liste de flags à spécifier dans chaque cas
		// Correspond aux flags associés en propriété persistante au projet
		// suivi de ceux du fichier + ceux que l'on ajoute.
		final List<String> flags = OcamlBuilder.getResourceFlags(project);
		flags.addAll(OcamlBuilder.getResourceFlags(file));

		// On fait maintenant une disjonction de cas selon le type du sommet
		// à visiter (Et on devrait plutot utiliser l'héritage...).

		// Traiter un fichier "ml" : détecter s'il a un changement de
		// propriété make_exe, sauvegarder le fichier objet s'il
		// existe, (et le fichier interface eventuellement)
		// lancer la compilation avec -dtypes, s'il n'y a pas
		// d'erreur, lier le fichier cm[ox] généré, comparer l'ancien et le
		// nouveau (tout changement entrainera la recompilation des
		// exécutables affectés et du fichier si c'est un exécutable), et,
		// s'il n'y a pas d'interface existante,
		// générer l'interface mli correspondante, lier le cmi généré, comparer
		// les changements et ajouter éventuellement les fichiers affectés au
		// graphe.
		if (fileType == Vertex.MLTYPE) {
			// refreshExeName indique si la propriété make_exe a changé
			if (vertex.refreshExeName()) {
				// S'il y a un changement et que le nom est null, c'est que
				// la propriété a été enlevée
				if (vertex.getExeName() == null) {
					graph.removeExe(vertex);
				}
				// Sinon il faut relier l'exécutable
				else {
					graph.addExe(vertex);
				}
			}

			// Supprimer le mli généré automatiquement afin qu'il ne gène pas la
			// compilation
			// Note : ce mli est supprimé par DependenciesSetter uniquement si
			// le fichier ml a été changé, et pas si il dépend d'un fichier
			// changé.
			final IFile mliFile = project.getFile(file.getProjectRelativePath().removeFileExtension()
					.addFileExtension("mli"));
			if (mliFile.exists() && Misc.isGeneratedFile(mliFile)) {
				deleteFile(mliFile);
			}
			// sauvegarder (en copiant) l'ancien fichier objet s'il existe et
			// l'ancien fichier interface (uniquement s'il n'y a pas de mli
			// généré auto).
			// Note : si le fichier possède un mli non généré automatiquement,
			// getInterfaceObjectFile renvoit null et rien ne se passe, sauf si
			// le mli est un ancien généré auto modifié par l'utilisateur.
			// Note 2: ce procédé pose des problèmes sous environnement windows
			IFile oldInterfaceObjectFile = null;
			IFile oldObjectFile = null;

			if (runOnLinux) {
				// Si ce test renvoit true ici, alors il y a un mli non généré
				// automatiquement.
				if (!mliFile.exists()) {
					oldInterfaceObjectFile = copyFile(vertex.getInterfaceObjectFile(), project);
				}

				oldObjectFile = copyFile(vertex.getObjectFile(), project);
			}

			flags.add("-dtypes");
			noErrors = runBuildingCommand(file, null, null, flags, false, true);
			// S'il n'y a pas d'erreurs
			if (noErrors) {
				// Associer le fichier objet.
				vertex.setObjectFile(project.getFile(file.getProjectRelativePath().removeFileExtension()
						.addFileExtension(objectFileExt)));

				// Détecter les changements entre les fichiers objets auquel
				// cas recompiler les exécutables affectés.
				try {
					if (!runOnLinux || !areSameFiles(oldObjectFile, vertex.getObjectFile())) {
						// TODO : ne vaut-il pas mieux remplacer addExe par
						// addAllExe ?
						// Ici on ajoute d'un coup tous les exe sans
						// utiliser addExe, ce qui n'est pas très propre. s
						graph.getExecutables().addAll(vertex.getAffectedExe());
						// Il faut aussi ajouter ce sommet si c'est un
						// exécutable
						if (vertex.getExeName() != null) {
							graph.addExe(vertex);
						}
					}
				} catch (IOException e) {
					OcamlPlugin.logError("error comparing object file", e);
				} catch (CoreException e) {
					OcamlPlugin.logError("error comparing object file", e);
				}

				// Si l'interface existe toujours, c'est qu'elle n'est pas
				// générée automatiquement, sinon il faut
				// la regénérer et associer le fichier cmi à ce sommet, il faut
				// aussi détecter les changements entre les cmi et effacer
				// l'ancien cmi.
				if (!mliFile.exists()) {

					// Générer l'interface
					final StringBuffer stdout = new StringBuffer();
					// -i pour récupérer les valeurs déclarées dans le
					// fichier source.
					flags.add("-i");

					// Afficher un message
					Misc.appendToOcamlConsole("Generating mli");

					if (!runBuildingCommand(file, stdout, null, flags, true, false)) {
						// Sauf si le système de fichier à changé, il est
						// impossible d'avoir une erreur.
						OcamlPlugin.logError("error in CompilerVisitor:visit:" + " unexpected error found");
						return false;
					}
					// On signale une erreur et on arrete la compilation si
					// la génération se passe mal.
					if (!generateMliFile(stdout.toString(), mliFile.getProjectRelativePath(), project)) {
						OcamlPlugin
								.logError("error in CompilerVisitor:visit: " + "error generating mli file");
						return false;
					}
					// Associer le fichier objet correspondant à
					// l'interface.
					final IFile interfaceObjectFile = project.getFile(file.getProjectRelativePath()
							.removeFileExtension().addFileExtension("cmi"));
					vertex.setInterfaceObjectFile(interfaceObjectFile);
					// Comparer
					try {
						if (!runOnLinux || !areSameFiles(oldInterfaceObjectFile, interfaceObjectFile)) {
							graph.addAll(vertex.getAffectedFiles());
						}
					} catch (IOException e) {
						OcamlPlugin.logError("error in CompilerVisitor:visit:" + " error in comparing files",
								e);
					} catch (CoreException e) {
						OcamlPlugin.logError("error in CompilerVisitor:visit:" + " error in comparing files",
								e);
					}

				}
				// Sinon, il y a un fichier mli non généré automatiquement.
				// Si ce mli était avant un "généré automatiquement" alors
				// le fichier ml possède encore le cmi, il faut donc le
				// mettre à null.
				else {
					// S'il y avait un fichier cmi, alors il a été récupéré
					// dans oldInterfaceObjectFile
					if (oldInterfaceObjectFile != null) {
						vertex.setInterfaceObjectFile(null);
					}
				}
			}// end if(noErrors)
			// Effacer si possible les fichiers objets correspondant.
			if (runOnLinux) {
				deleteFile(oldObjectFile);
				deleteFile(oldInterfaceObjectFile);
			}

		}
		// Traiter un fichier "mli" : lier le fichier cmi généré et détecter les
		// changements
		else if (fileType == Vertex.MLITYPE) {
			// Copier l'ancien fichier interface cmi afin de détecter les
			// changements
			IFile oldInterfaceObjectFile = null;
			if (runOnLinux) {
				oldInterfaceObjectFile = copyFile(vertex.getInterfaceObjectFile(), project);
			}

			noErrors = runBuildingCommand(file, null, null, null, false, true);
			if (noErrors) {
				// Associer le fichier objet correspondant à
				// l'interface.
				final IFile interfaceObjectFile = project.getFile(file.getProjectRelativePath()
						.removeFileExtension().addFileExtension("cmi"));
				vertex.setInterfaceObjectFile(interfaceObjectFile);
				// Comparer
				try {
					if (!runOnLinux || !areSameFiles(oldInterfaceObjectFile, interfaceObjectFile)) {
						graph.addAll(vertex.getAffectedFiles());
					}
				} catch (IOException e) {
					OcamlPlugin.logError("error in CompilerVisitor:visit:" + " error in comparing files", e);
				} catch (CoreException e) {
					OcamlPlugin.logError("error in CompilerVisitor:visit:" + " error in comparing files", e);
				}
			}
			// Effacer
			if (runOnLinux)
				deleteFile(oldInterfaceObjectFile);
		} else if (fileType == Vertex.MLLTYPE) {
			// TODO traiter les MLL lors de la compilation !
		} else if (fileType == Vertex.MLYTYPE) {
			// TODO traiter les MLY lors de la compilation !
		}

		return true;
	}

	/**
	 * Run the building command (without linking) at the project root. The command includes the paths given in
	 * the project properties. Display the error messages on the OCaml output view and refresh the error and
	 * warning markers.
	 * 
	 * @param file
	 *            the file to compile
	 * @param stdout
	 *            where to get the output from the compiler
	 * @param stderr
	 *            where to get the error output from the compiler
	 * @param flags
	 *            flags to add to the command
	 * @param ignoreWarning
	 *            whether the command must ignore warnings
	 * @return true if compilation succeeded without errors
	 * @throws CoreException
	 */
	private boolean runBuildingCommand(final IFile file, StringBuffer stdout, StringBuffer stderr,
			List<String> flags, final boolean ignoreWarning, boolean printMessages) {

		// Le projet associé
		final IProject project = file.getProject();

		// Le mode de compilation
		// Dans la suite, on testera si le buildmode est non nul, et s'il l'est
		// on utilise byte-code par défaut.
		// Ici on en profite pour mettre la propriété byte-code au projet par
		// défaut.
		// TODO ceci devrait pouvoir etre choisi par l'utilisateur.
		final String buildMode = Misc.getShareableProperty(project, OcamlBuilder.COMPIL_MODE);
		if ((buildMode == null) || (buildMode.equals(""))) {
			// Mettre la propriété byte_code par défaut.
			Misc.setShareableProperty(project, OcamlBuilder.COMPIL_MODE, OcamlBuilder.BYTE_CODE);
		}

		// supprimer les marqueurs d'erreur
		// ATTENTION : il ne faut pas le faire lorsque la commande est lancée
		// pour générer le mli, sinon on efface un éventuel ancien warning.
		if (!ignoreWarning) {
			try {
				file.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				OcamlPlugin.logError("error in deleting markers", e);
			}
		}

		// Choix de la commande : ocamlc ou ocamlopt
		// Attention : pas de gestion des mll et mly pour l'instant !
		final String OCOMPILER = (buildMode.equals(OcamlBuilder.NATIVE) ? OcamlPlugin.getOcamloptFullPath()
				: OcamlPlugin.getOcamlcFullPath());

		if (OCOMPILER.equals("")) {
			OcamlPlugin.logError("error in CompilerVisitor:"
					+ "runBuildingCommand : ocamlc or ocamlopt not found");
			return false;
		}

		// Le chemin vers le fichier relatif au workspace et le nom
		// correspondant
		final IPath fileWorkspacePath = file.getFullPath().makeRelative();
		final String fileNameWithPath = fileWorkspacePath.toOSString();

		// Les chemins à inclure
		final List<String> includedFolders = Misc.getProjectPaths(project);

		// la commande de compilation

		// initialiser les flags à zéro s'il n'y en a pas
		if (flags == null) {
			flags = new ArrayList<String>(0);
		}
		if (ignoreWarning) {// -w a pour ignorer tous les warnings
			flags.add("-w");
			flags.add("a");
		}
		final List<String> command = new ArrayList<String>();
		command.add(OCOMPILER);
		command.addAll(includedFolders);
		command.addAll(flags);
		command.add("-c");
		command.add(fileNameWithPath);
		// Ici va commencer la compilation :

		// Afficher un message
		Misc.appendToOcamlConsole("Building: " + fileNameWithPath);

		// lancer la commande à la racine du workspace.
		CommandRunner cmd = new CommandRunner(command.toArray(new String[0]), project.getWorkspace()
				.getRoot().getLocation().toOSString());
		// Et récupérer les messages
		// (Si stdout ou stderr étaient null, il faut les initialiser.)
		if (stdout == null) {
			stdout = new StringBuffer();
		}
		if (stderr == null) {
			stderr = new StringBuffer();
		}
		// On ajoute à la fin de stdout ou stderr.
		stdout.append(cmd.getStdout());
		stderr.append(cmd.getStderr());
		// Un booléen pour savoir s'il y a eu des erreurs
		boolean noErrors = true;

		// Gestion des marqueurs.
		ProblemMarkers problemMarkers = null;

		// Affichage des messages
		if (stdout.length() != 0 && printMessages)
			Misc.appendToOcamlConsole(stdout.toString());

		if (stderr.length() != 0 && printMessages) {
			Misc.appendToOcamlConsole(stderr.toString());
			problemMarkers = new ProblemMarkers(project);
			problemMarkers.makeMarkers(stderr.toString());
		}

		String sErrorsFound = null;
		String sWarningsFound = null;
		if (problemMarkers != null) {
			noErrors = !problemMarkers.errorsFound() && !problemMarkers.projectErrorsFound();
			sErrorsFound = problemMarkers.errorsFound() ? "true" : null;
			sWarningsFound = problemMarkers.warningsFound() ? "true" : null;
		}

		// Et sur le fichier de source
		Misc.setFileProperty(file, OcamlBuilder.COMPILATION_ERRORS, sErrorsFound);
		// Attention : uniquement si on a pas ignoré les warnings (sinon on
		// enlève ceux qui existent déjà)
		if (!ignoreWarning)
			Misc.setFileProperty(file, OcamlBuilder.COMPILATION_WARNINGS, sWarningsFound);

		Misc.updateDecoratorManager();
		return noErrors;
	}

	/**
	 * Generate an mli file from its content, its path relative to the project, and the project.
	 * 
	 * @return true if the generation succeeded
	 */
	private boolean generateMliFile(final String content, final IPath mliFileProjectPath,
			final IProject project) {

		// System.err.println("generating mli file");

		final IPath projectLocation = project.getLocation();

		// Récupérer l'emplacement prévu pour le fichier mli, puis créer le
		// fichier
		final String expectedMliFileLocation = projectLocation.addTrailingSeparator().append(
				mliFileProjectPath).toOSString();
		File mlif = new File(expectedMliFileLocation);

		// Remplir le fichier avec le contenu passé en paramètre
		try {
			mlif.createNewFile();
			BufferedWriter writer = new BufferedWriter(new FileWriter(mlif));
			writer.write(content);
			writer.close();

		} catch (IOException e) {
			OcamlPlugin.logError("error in CompilerVisitor:generateMliFile: "
					+ "error in writing new mliFile :" + expectedMliFileLocation, e);
			Misc.popupErrorMessageBox("Cannot create or write new file : " + expectedMliFileLocation,
					"writing rights");
			return false;
		}

		return true;

	}

	/**
	 * Delete a file
	 * 
	 * @return true if the file was really deleted
	 */
	private boolean deleteFile(final IFile file) {
		if (file != null && file.exists()) {
			boolean bDeleted = false;

			int retries = 5;

			while (!bDeleted && retries > 0) {
				try {
					retries--;
					file.delete(true, null);
					return true;
				} catch (CoreException e) {
					OcamlPlugin.logError("error deleting file : " + file.getName()
							+ ((retries > 0) ? " : retrying" : " : stop retrying"), e);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
				}

			}

		}
		return false;
	}

	/** Remove a file (asynchronously) */
	/*
	 * private boolean deleteFileAsync(final IFile file) { Job job = new Job("Deleting temporary files") {
	 * @Override protected IStatus run(IProgressMonitor monitor) {
	 * 
	 * if (file != null && file.exists()) { boolean bDeleted = false;
	 * 
	 * int retries = 5;
	 * 
	 * while (!bDeleted && retries > 0) { try { retries--; file.delete(true, null); bDeleted = true; } catch
	 * (CoreException e) { OcamlPlugin.logError("error deleting file : " + file.getName() + ((retries > 0) ? " :
	 * retrying" : " : stop retrying"), e); try { Thread.sleep(500); } catch (InterruptedException e1) { } } } }
	 * 
	 * return Status.OK_STATUS; } };
	 * 
	 * job.setPriority(Job.DECORATE); job.schedule(1000);
	 * 
	 * return true; }
	 */

	/**
	 * Copy the file by giving the copy the extension "._old". This is used to compare the two files later to
	 * see if there were changes.
	 * 
	 * @return the copy of the file or <code>null</code> if the file couldn't be copied
	 */
	private IFile copyFile(final IFile file, final IProject project) {
		if (file == null || !file.exists()) {
			return null;
		}
		try {
			// Ici copy prend comme chemin le chemin relatif à là où se
			// trouve la ressource, donc ici on a juste besoin de rejouter
			// _old à l'extension
			final IPath newPath = file.getProjectRelativePath().addFileExtension("_old");
			// S'il y a déjà un fichier à l'emplacement de destination, essayer
			// de le supprimer s'il est généré automatiquement, sinon ne rien
			// faire, on renvoit null
			final IFile newFile = project.getFile(newPath);
			// Si le fichier existe et n'est soit pas généré automatiquement,
			// soit pas supprimable renvoyer null
			if (newFile.exists() && (!Misc.isGeneratedFile(newFile) || !deleteFile(newFile))) {
				return null;
			}
			// Il faut donc récupérer uniquement le dernier segment de
			// newPath ce qui correspond au IPath représentant le nom.
			// On ne peut pas utiliser lastSegment qui renvoit une String.
			file.copy(newPath.removeFirstSegments(newPath.segmentCount() - 1), true, null);
			// Le déplacement à été effectué et des évènement correspondants
			// sont générés, mais pour l'instant file réfère toujours à l'ancien
			// fichier. Il faut récupérer directement le fichier généré à partir
			// du projet par exemple.
			return (project.getFile(newPath));
		} catch (CoreException e) {
			OcamlPlugin.logError("error in CompilerVisitor:copyFile:" + " error backing up file", e);
			return null;
		}

	}

	/**
	 * Compare two files byte by byte.
	 * 
	 * @return true if the two files exist and are the same
	 */
	private boolean areSameFiles(final IFile file1, final IFile file2) throws IOException, CoreException {

		// Si les deux fichiers sont nuls, retourner vrai
		if ((file1 == null) && (file2 == null)) {
			return true;
		}

		// Si un seul des deux fichiers est null, c'est faux.
		if ((file1 == null) || (file2 == null)) {
			return false;
		}

		// Rafraichir les fichiers (inévitable)
		file1.refreshLocal(IResource.DEPTH_ZERO, null);
		file2.refreshLocal(IResource.DEPTH_ZERO, null);

		// Si les deux fichiers n'existent pas, retourner vrai
		if ((!file1.exists()) && (!file2.exists())) {
			return true;
		}

		// Si un seul des deux fichiers n'existe pas, c'est faux.
		if (!file1.exists() || (!file2.exists())) {
			return false;
		}

		final BufferedInputStream input1 = new BufferedInputStream(file1.getContents());
		final BufferedInputStream input2 = new BufferedInputStream(file2.getContents());

		int readValue1 = input1.read();
		int readValue2 = input2.read();

		// Tant que les valeurs sont égales ou bien qu'on est arrivé à la fin
		while ((readValue1 == readValue2) && ((readValue1 != -1) || (readValue2 != -1))) {
			readValue1 = input1.read();
			readValue2 = input2.read();
		}

		// Si les fichiers sont les mêmes, ces deux valeurs finales sont égales
		// et valent -1 (fin de fichier). Sinon, alors les valeurs sont
		// différente et l'une est éventuellement à -1.
		return (readValue1 == readValue2);
	}

}
