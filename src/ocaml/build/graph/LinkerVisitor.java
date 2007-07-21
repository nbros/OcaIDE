package ocaml.build.graph;

import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.build.ProblemMarkers;
import ocaml.exec.CommandRunner;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Handle the linking phase, that is:
 * <ul>
 * <li> determine for each executable the list of files to link, and for each of these files, associate the
 * executable affected by possible changes.
 * <li> start the link command that will create the executable by getting its name from the persistent
 * property.
 * </ul>
 * 
 * 
 */
public class LinkerVisitor implements IExecutablesVisitor {

	// Le final est nécessaire pour accéder à vertex dans le visiteur anonyme.
	public boolean visit(final Vertex vertex, final IProgressMonitor monitor) {

		if ((monitor != null) && monitor.isCanceled())
			return false;

		// Les anciens fichiers à lier (les recopier sinon ils vont etre
		// modifiés par la suite)
		final List<Vertex> oldFilesToLink = new ArrayList<Vertex>(vertex.getAllFilesToLink());

		// Il faut que les dépendances soient dans le bon ordre, il nous faut
		// donc un IPreNeededFilesVisitor, ainsi on ajoute en premier les
		// feuilles.
		// Ce visiteur anonyme prend beaucoup de place mais ce n'est que du
		// commentaire...
		final IPreNeededFilesVisitor filesFinder = new IPreNeededFilesVisitor() {

			public boolean visit(Vertex n) {

				// Il ne faut pas considérer le fichier que l'on visite (c'est à
				// dire ajouter main.cmo, il sera ajouté lors du lancement de la
				// commande) ni l'interface correspondante (on détecte ce cas si
				// le fichier ml correspondant est empilé dans la pile des
				// sommets en cours de visite, d'où l'utilité d'avoir cette
				// pile)
				final int nvertexType = n.getType();
				if ((nvertexType == Vertex.MLTYPE && n.equals(vertex))
						|| (nvertexType == Vertex.MLITYPE && filesBeingVisited.contains(n.getMLVertex()))) {
					return true;
				}

				// Si on est sur une interface (et qui ne soit pas un fichier
				// externe), le fichier objet à lier est avec le fichier ml
				// correspondant. Il faut aussi visiter toutes les dépendances
				// de ce fichier ml.
				if (!(n.isExternalFile()) && (nvertexType == Vertex.MLITYPE)) {
					final Vertex matchingMl = n.getMLVertex();
					if (matchingMl != null) {
						matchingMl.accept(this);
					} else {
						OcamlPlugin.logError("error in LinkerVisitor.FilesFinder"
								+ " : matchingMl not found for file : " + n.getFile().getName());
					}
					// On ajoute pas cette interface puisqu'on vient d'ajouter
					// tout ce qui concernait le ml correspondant.
					// Note : on aimerait bien ne pas visiter les dépendances
					// puisqu'à priori elles sont visités lors de la visite du
					// ml mais avec ce type de visiteur on ne peut pas retourner
					// false pour arreter la visite car il visite après la
					// descente récursive.
					return true;
				}

				// Ajouter le sommet requis (doit normalement posséder un
				// fichier objet).
				vertex.addFileToLink(n);
				// le retirer de la liste des anciens (si le sommet n'est
				// pas trouvé, remove renvoit false, on s'en fiche).
				oldFilesToLink.remove(n);
				// Et d'ajouter le sommet en tant qu'exécutable affecté
				n.addAffectedExe(vertex);
				return true;
			}

			// Deux méthodes pour détecter où l'on se trouve dans les
			// dépendances, particulièrement utile pour ne pas visiter le mli
			// d'un fichier ml.
			private List<Vertex> filesBeingVisited = new ArrayList<Vertex>();

			public void popVertex(Vertex v) {
				if (!filesBeingVisited.remove(v)) {
					OcamlPlugin.logError("error in LinkerVisitor:" + "popVertex: vertex not found");
				}
			}

			public void pushVertex(Vertex v) {
				filesBeingVisited.add(v);
			}

		};
		// On vide la liste des fichiers à lier car ils vont être recalculés.
		vertex.getAllFilesToLink().clear();
		// Visiter ce sommet
		vertex.accept(filesFinder);

		// Maintenant, il suffit d'enlever le sommet à la liste des exécutables
		// affectés pour tout fichier à lier inutilisé (c'est à dire, tout ce
		// qui reste dans oldFilesToLink).
		for (Vertex oldFileToLink : oldFilesToLink) {
			// Ici le sommet peut ne pas être trouvé, mais on l'ignore,
			// l'important c'est qu'il n'y soit plus ensuite.
			oldFileToLink.removeAffectedExe(vertex);
		}

		// A partir d'ici, les dépendances sont calculées correctement, il
		// suffit de récupérer si possible les nom des fichiers objets et de les
		// mettre dans les flags.

		// Le fichier associé
		final IFile file = vertex.getFile();

		// Récupérer les flags du projet puis du fichier
		final List<String> flags = OcamlBuilder.getResourceFlags(file.getProject());
		flags.addAll(OcamlBuilder.getResourceFlags(file));

		// Pour chaque fichier à lier
		for (Vertex fileToLink : vertex.getAllFilesToLink()) {

			// Traiter les fichiers externes : récupérer directement la liste
			// ordonnée des fichiers à lier.
			if (fileToLink.isExternalFile()) {
				final List<String> extObjFiles = fileToLink.getExternalObjectFiles();
				for (String objFile : extObjFiles) {
					if (!flags.contains(objFile)) {
						flags.add(objFile);
					}
				}
			} else {
				final IFile objectFile = fileToLink.getObjectFile();
				// Si le fichier objet associé n'est pas null (on se fiche qu'il
				// existe, on veut juste le nom)
				if (objectFile != null) {
					final String objFile = objectFile.getName();
					// Ajouter son nom dans la liste des flags
					if (!flags.contains(objFile)) {
						flags.add(objFile);
					}
				}
			}
		}

		// Il ne reste plus qu'à lancer la commande avec les flags et le fichier
		// objet rattaché.
		final IFile objectFile = vertex.getObjectFile();
		if (objectFile != null) {
			runLinkingCommand(file, objectFile, vertex.getExeName(), null, null, flags);
		} else {
			OcamlPlugin.logError("error in LinkerVisitor:" + "visit: no object file to link");
		}

		return true;
	}

	/**
	 * Lancer la commande de liaison (sur le fichier objet) à la racine du projet.<br>
	 * La commande inclut les chemins vers les repertoires donnés dans les propriétés du projet Ocaml.<br>
	 * Ceci affiche les messages sur la console ocaml et met à jour les marqueurs d'erreurs et warning, il
	 * faut donc passer le fichier en paramètre.<br>
	 * 
	 * @param file
	 *            le fichier source correspondant
	 * @param objectFile
	 *            le fichier objet à lier
	 * @param exeName
	 *            le nom de l'exécutable à créer
	 * @param stdout
	 *            le conteneur dans lequel on voudra récupérer les messages de sortie
	 * @param stderr
	 *            le conteneur dans lequel on voudra récupérer les messages d'erreurs
	 * @param flags
	 *            des flags à rajouter à la commande
	 * @return true s'il n'y a pas eu d'erreurs à la compilation, false sinon.
	 * @throws CoreException
	 */
	private boolean runLinkingCommand(final IFile file, final IFile objectFile, final String exeName,
			StringBuffer stdout, StringBuffer stderr, List<String> flags) {

		// Le projet associé
		final IProject project = file.getProject();

		// Le mode de compilation
		// Dans la suite, on testera si le buildmode est non nul, et s'il l'est
		// on utilise byte-code par défaut.
		// Ici on en profite pour mettre la propriété byte-code au projet par
		// défaut.
		// TODO ceci devrait pouvoir etre choisi par l'utilisateur.
		String buildMode = Misc.getProjectProperty(project, OcamlBuilder.COMPIL_MODE);
		if ((buildMode == null) || (buildMode.equals(""))) {
			// Mettre la propriété byte_code par défaut.
			Misc.setProjectProperty(project, OcamlBuilder.COMPIL_MODE, OcamlBuilder.BYTE_CODE);
			buildMode = OcamlBuilder.BYTE_CODE;
		}

		// Choix de la commande : ocamlc ou ocamlopt
		// Attention : pas de gestion des mll et mly pour l'instant !
		final String OCOMPILER = (buildMode.equals(OcamlBuilder.NATIVE) ? OcamlPlugin.getOcamloptFullPath()
				: OcamlPlugin.getOcamlcFullPath());

		if (OCOMPILER.equals("")) {
			OcamlPlugin.logError("error in LinkerVisitor:"
					+ "runLinkingCommand : ocamlc or ocamlopt not found");
			return false;
		}
		// Le chemin vers le fichier objet relatif au workspace et le nom correspondant
		final IPath objFileWorkspacePath = objectFile.getFullPath().makeRelative();
		final String objFileNameWithPath = objFileWorkspacePath.toOSString();

		// Les chemins à inclure
		final List<String> includedFolders = Misc.getProjectPaths(project);

		// la commande de compilation

		// initialiser les flags à zéro s'il n'y en a pas
		if (flags == null) {
			flags = new ArrayList<String>(0);
		}

		// Se protéger d'une NullPointerException
		if (exeName == null) {
			OcamlPlugin.logError("error in LinkerVisitor:runLinkingCommand: no exe name");
			return false;
		}

		// On créé un "handler", l'exécutable n'existe pas encore...
		final IFile exeFile = project.getFile(exeName);
		// Les flags sont bien ordonnés, on aura donc bien "-o exeName"
		flags.add("-o");
		flags.add(exeFile.getFullPath().makeRelative().toOSString());
		final List<String> command = new ArrayList<String>();
		command.add(OCOMPILER);
		command.addAll(includedFolders);
		command.addAll(flags);
		command.add(objFileNameWithPath);
		// Ici va commencer la compilation :

		// Afficher un message
		Misc.appendToOcamlConsole("Linking: " + objFileNameWithPath);

		// lancer la commande à la racine du workspace
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

		// Savoir s'il y a eu des erreurs
		boolean noErrors = true;

		// Affichage des messages et gestion des marqueurs.
		ProblemMarkers problemMarkers = null;

		if (stdout.length() != 0)
			Misc.appendToOcamlConsole(stdout.toString());

		if (stderr.length() != 0) {
			Misc.appendToOcamlConsole(stderr.toString());
			problemMarkers = new ProblemMarkers(project);
			problemMarkers.makeMarkers(stderr.toString());
		}
		// Sinon tout s'est bien passé, il faut mettre comme propriété
		// persistante à l'exécutable généré le mode de compilation
		else {
			try {
				exeFile.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				OcamlPlugin.logError("error refreshing file " + exeFile.getName(), e);
			}
			if (exeFile.exists()) {
				Misc.setFileProperty(exeFile, OcamlBuilder.COMPIL_MODE, buildMode);
			}

		}

		String sProjectErrorsFound = null;
		String sProjectWarningsFound = null;
		if (problemMarkers != null) {
			noErrors = !problemMarkers.projectErrorsFound();
			sProjectErrorsFound = problemMarkers.projectErrorsFound() ? "true" : null;
			sProjectWarningsFound = problemMarkers.projectWarningsFound() ? "true" : null;
		}
		// Afficher les erreurs pour le projet sans enlever ce qui a été mis
		// par CompilerVisitor

		if (Misc.getProjectProperty(project, OcamlBuilder.COMPILATION_ERRORS).equals(""))
			Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_ERRORS, sProjectErrorsFound);
		if (Misc.getProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS).equals(""))
			Misc.setProjectProperty(project, OcamlBuilder.COMPILATION_WARNINGS, sProjectWarningsFound);

		Misc.updateDecoratorManager();

		return noErrors;
	}

}
