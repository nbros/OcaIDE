package ocaml.build.graph;

import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.build.util.CycleException;
import ocaml.exec.CommandRunner;
import ocaml.util.FileUtil;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;



/**
 * Un visiteur très spécial : il visite d'abord un sommet et ensuite traite ses
 * fichiers requis, ceci est utile lorsque l'on veut calculer toutes les
 * dépendances à partir d'un fichier. <br>
 * <br>
 * Ce visiteur modifie la liste verticesVisited, à la fin cette liste contient
 * tous les sommets visités avec toutes leurs dépendances.<br>
 * <br>
 * Ce visiteur peut aussi gérer un graphe, lorsque l'on veut calculer les
 * dépendances de fichiers qui peuvent déjà se trouver dans un graphe.<br>
 * Ceci sera très utile lorsque l'on ne veut travailler que sur des fichiers
 * modifiés (liste changedFiles fournie en paramètre dans le constructeur), la
 * liste verticesVisited ne contiendra alors que les fichiers et les dépendances
 * qui étaient dans changedFiles, et les fichiers liés à d'autres dans le
 * système de fichier (ressources externes), voir la compilation incrémentale.<br>
 * <br>
 * 
 * Ce visiteur s'occupe des fichiers mli générés automatiquement par la
 * compilation : il les supprime avant le lancement de la commande ocamldep,
 * ainsi ils n'apparaissent pas dans le graphe de dépendances.
 * 
 * 
 */

/**
 * A visitor to visit a graph vertex first, and then handle its required files (using ocamldep). This is
 * useful to compute all the dependencies from a file.
 */
public class DependenciesSetter implements IPostNeededFilesVisitor {

	/**
	 * Une liste se rappelant les fichiers parcourus par ce visiteur. <br>
	 * Ceci détecte les cycles.
	 */
	private List<Vertex> filesBeingVisited;

	/**
	 * la liste des fichiers visités. <br>
	 * Lorsque l'on tombe sur un fichier qui appartient à cette liste, on sait
	 * qu'il a déjà été visité et que ses dépendances on été traitées.<br>
	 * C'est ce paramètre qui sera utilisé par celui qui a instancié ce
	 * visiteur.
	 */
	private List<Vertex> visitedVertices;

	
	/**
	 * La liste des sommets créés.<br>
	 * Ceci pour ne pas créer deux fois un sommet qui a déjà été créé.<br>
	 * Le protocole est le suivant :<br>
	 * Pour chaque fichier, chercher un sommet correspondant dans un graphe
	 * éventuellement existant. Si on en trouve pas, regarder dans la liste des
	 * sommets créés par ce visiteur, et si on ne trouve encore rien, il faut
	 * créer un nouveau sommet.<br>
	 */
	private List<Vertex> createdVertices;

	/**
	 * La liste des fichiers modifiés ou créés.<br>
	 * Si un fichier n'est pas dans cette liste (et si ce n'est pas une
	 * ressource externe) c'est qu'il appartient à un graphe qui a du être passé
	 * en paramètre, il ne faut donc pas l'ajouter à la liste des fichiers
	 * visités.
	 */
	private List<IFile> changedFiles;

	/**
	 * Un graphe dans lequel rechercher des liens avec des fichiers déjà existant.
	 */
	private LayersGraph graph;

	/**
	 * Visite tous les fichiers et les place dans visitedVertices avec toutes
	 * leurs dépendances.
	 * 
	 * @param visitedVertices
	 *            la liste des sommets visités que le visiteur va modifier
	 *            suivant l'évolution de son parcours.
	 */
	public DependenciesSetter(List<Vertex> visitedVertices) {
		this.filesBeingVisited = new ArrayList<Vertex>();
		this.createdVertices = new ArrayList<Vertex>();
		this.graph = null;
		this.visitedVertices = visitedVertices;
		// S'il n'y a pas de graphe, alors tous les fichiers seront
		// créés, ce paramètre devient inutile :
		this.changedFiles = null;
	}

	/**
	 * Visite tous les fichiers qui ont changé, en traitant éventuellement des
	 * liens de dépendances avec des sommets du graphe.
	 * 
	 * @param graph
	 *            graphe contenant des fichiers dont peuvent dépendre les
	 *            nouveaux.
	 * @param verticesVisited
	 *            la liste des sommets visités que le visiteur va modifier
	 *            suivant l'évolution de son parcours.
	 * @param changedFiles
	 *            la liste des fichiers modifiés, c'est à dire les seuls que le
	 *            visiteur doit traiter
	 */
	public DependenciesSetter(LayersGraph graph, List<Vertex> verticesVisited,
			List<IFile> changedFiles) {
		this.filesBeingVisited = new ArrayList<Vertex>();
		this.createdVertices = new ArrayList<Vertex>();
		this.graph = graph;
		this.visitedVertices = verticesVisited;
		this.changedFiles = changedFiles;
	}

	

	/**
	 * Empiler un sommet sur la pile des sommets en cours de visite avant de
	 * visiter les dépendances du sommet (détection de cycle)
	 */
	public void pushVertex(Vertex v) {
		// Astuce pour détecter lorsque le sommet v n'est pas construit par
		// DependenciesSetter, mais par l'appelant (addFilesToGraph), auquel cas
		// il faut l'ajouter ici aux sommets créés.
		// Note : bien sur, filesBeingVisited est vide si et seulement si on
		// vient d'être accepté sur un nouveau sommet par addFilesToGraph
		if (filesBeingVisited.isEmpty()) {
			if (!createdVertices.contains(v)) {
				createdVertices.add(v);
			}
		}
		filesBeingVisited.add(v);
	}

	/**
	 * Dépiler un sommet de la pile des sommets en cours de visite une fois le
	 * sommet (et ses dépendances) complètement visité.
	 */
	public void popVertex(Vertex v) {
		if (!filesBeingVisited.remove(v)) {
			OcamlPlugin.logError("error in DependenciesSetter:"
					+ "popVertex: vertex not found");
		}
	}

	
	
	
	
	/**
	 * Visite d'abord le sommet puis traite ses fichiers requis.<br>
	 * Cette méthode détecte les cycles.
	 * 
	 * @param v
	 *            le sommet visité
	 * @return true pour continuer la visite des fichiers requis, false pour
	 *         s'arrêter là.
	 */
	public boolean visit(Vertex v) {

		// Ne pas chercher les dépendances des fichiers externes, c'est à
		// l'utilisateur de donner les bons fichiers objets.
		if (v.isExternalFile()) {
			// Attention, si le sommet externe à été créé par ce visiteur, il
			// faut quand même l'ajouter au graphe (sinon il est dans l'ancien
			// graphe, on l'y laisse)
			if(createdVertices.contains(v)){
				if (!visitedVertices.contains(v)){
					visitedVertices.add(v);
				}
			}
			return false;
		}
		
		// Récupérer le fichier associé
		final IFile file = v.getFile();

		// Si le fichier est déjà en train d'être visité, c'est un cycle.
		if (filesBeingVisited.contains(v)) {
			OcamlPlugin.logError("error in DependenciesSetter:visit: "
					+ "cycle detection :" + file.getName(),
					new CycleException());
			Misc.popupErrorMessageBox("cycle detection : file :"
					+ file.getName() + " met twice\nfix it and re-build", "cycle detection");
			//Enlever le sommet des sommets visités car il ne faut pas le traiter.
			visitedVertices.remove(v);
			return false;
		}

		// Les fichiers qui ne sont pas dans changedFiles et qui
		// appartiennent déjà au graphe ne sont visités que pour détecter un
		// nouveau cycle. (Sinon on visite tout fichier qui n'est pas déjà dans
		// le graphe qu'il soit modifié ou non ( les fichiers externes ne sont
		// jamais modifiés par exemple) et tout fichier qui est modifié.)
		if (changedFiles != null && !changedFiles.contains(file)
				&& graph != null && graph.findVertex(file) != null) {
			// On va juste voir récursivement toutes les dépendances pour
			// détecter un cycle.
			return true;
		}

		
		// Si on visite un fichier déjà visité par ce même visiteur, alors ce
		// n'est pas la peine de recommencer.
		if (visitedVertices.contains(v)) {
			return false;
		}

		

		// Récupérer les dépendances.
		
		// Ceci teste si le fichier associé existe (car il va lancer la commande
		// ocamldep), il serait donc bon d'en profiter pour associer le bon type
		// (ml, mli etc..)au sommet juste après. (Qui a besoin que le fichier
		// existe pour trouver son extension)
		final List<IFile> neededFiles = runDependenciesCommand(file);
		//Associer la bonne extension à ce sommet.
		v.setType();
		// Si neededFiles est null, c'est que le fichier sur lequel la commande
		// a été lancée n'existe pas.
		if (neededFiles == null) {
			OcamlPlugin.logError("error in DependenciesSetter:visit: "
					+ "file not found :" + file.getName(),
					new DependenciesGraphException());
			return false;
		}
		
		

		// Il faut parcourir la liste actuelle de dépendances afin de détecter
		// les dépendances qui deviennent inutilisées (auquel cas il faut
		// s'enlever des fichiers requis de ces dépendances, et enlever les
		// fichiers externes inutilisés) et celles qui existent déjà auquel cas
		// on ne les revisite pas.
		final List<Vertex> actualNeededVertices = v.getNeededFiles();
		final List<Vertex> dependenciesToRemove = new ArrayList<Vertex>(
				actualNeededVertices.size());

		// Pour chaque sommet dans les dépendances actuelles
		for (Vertex vertex : actualNeededVertices) {
			// S'il n'est pas dans les dépendances réelles (qui viennent d'etre
			// calculées) c'est qu'il ne sert plus, il faut alors le considérer
			// comme "à supprimer", s'il est dans les dépendances réelles, alors
			// il faut le supprimer de cette liste car on ne va pas le traiter
			// deux fois (il est à la fois dans les dépendances réelles et
			// acutelles, donc c'est bon).
			// Note : remove renvoit true si l'objet est trouvé et effacé, false
			// s'il n'est pas trouvé.
			if (!neededFiles.remove(vertex.getFile())) {
				dependenciesToRemove.add(vertex);
			}
		}
		
		for (Vertex vertex : dependenciesToRemove) {
			if (!vertex.removeAffectedFile(v)) {
				// Si v n'était pas dans les fichiers affectés c'est que les
				// dépendances n'était alors pas cohérentes.
				OcamlPlugin.logError("error in DependenciesSetter:" + "visit: "
						+ "non reciprocal dependency found",
						new DependenciesGraphException());
			}
			// Sinon la suppression s'est bien passée
			else {
				// On enlève alors le sommet des dépendances actuelles. Ce
				// serait gros que le fichier ne soit pas trouvé ici puisqu'on
				// l'a ajouté il y a 3 lignes..
				actualNeededVertices.remove(vertex);
				// Gérer le cas particulier des fichiers externes :
				// Si c'est un sommet externe que maintenant plus personne
				// n'utilise, il faut le supprimer du graphe (ceci effacera le
				// lien dans le navigateur)
				if (vertex.isExternalFile()
						&& (vertex.getAffectedFiles().size() == 0)) {
					// Remarque : ici on est sur que le graph existe.
					graph.suppressVertex(vertex);
				}
			}
		}

		
		
		// Pour chaque fichier requis (qui n'était pas déjà dans les
		// dépendances.)
		for (IFile neededFile : neededFiles) {

			// Trouver le sommet correspondant dans le graphe s'il existe ou
			// dans les sommets déjà créés ou le créer s'il n'existe pas
			Vertex neededVertex = null;

			// S'il y a un graphe
			if (graph != null) {
				//Prendre le sommet du graphe s'il existe
				neededVertex = graph.findVertex(neededFile);
			}
			// si le sommet n'existe pas ou s'il n'y a pas
			// de graphe
			if (neededVertex == null) {
				// Chercher si on a pas déjà créé un sommet correspondant à ce
				// fichier requis
				for (Vertex ver : createdVertices) {
					if (ver.getFile().equals(neededFile))
						neededVertex = ver;
				}
				// s'il n'a pas été créé par ce visiteur, le faire, en gérant
				// le fait qu'il puisse référencer un fichier externe au projet.
				if (neededVertex == null) {
					neededVertex = new Vertex(neededFile, neededFile
							.isLinked());
					//Et l'ajouter aux sommets déjà créés
					createdVertices.add(neededVertex);
				}
			}

			// Ajouter dans la liste des fichiers requis de v et
			// ajouter v dans la liste des fichiers affectés du sommet requis.
			v.addNeededFile(neededVertex);
			neededVertex.addAffectedFile(v);

		}
		// Ajouter ce sommet aux sommets visités.
		if (!visitedVertices.contains(v)) {
			visitedVertices.add(v);
		}
		// Tous les fichiers requis ont été examinés et on leur a signalé qu'ils
		// affectaient le sommet visité. Il ne reste plus qu'à visiter ces
		// fichiers requis.
		return true;

	}

	/**
	 * Trouver les dépendances d'un fichier en lançant la commande ocamldep.<br>
	 * Supprime les fichiers mli générés automatiquement rencontrés dans les
	 * dépendances et lorsqu'il réfèrent à d'autre fichiers source, ajoute ces
	 * fichiers sources à la place à la liste des dépendances.<br>
	 * 
	 * @param file
	 *            fichier dont on veut trouver les dépendances.
	 * @return un tableau contenant les fichiers dont file dépend,
	 *         <code>null</code> si un fichier n'existe pas. <br>
	 * 
	 * NOTE : ceci test si le fichier existe avant de lancer la commande.
	 */
	private List<IFile> runDependenciesCommand(IFile file) {

		// Récupérer la commande ocamldep
		final String OCAMLDEP = OcamlPlugin.getOcamldepFullPath();
		// Si la commande est vide, on ne fait rien
		if (OCAMLDEP.equals("")) {
			OcamlPlugin.logError("error in DependenciesSetter:"
					+ "runDependenciesCommand : ocamldep not found");
			return null;
		}
		// Le projet
		final IProject project = file.getProject();

		// On ajoute les chemins séléctionnés par l'utilisateur
		final ArrayList<String> foldersPaths = Misc.getProjectPaths(project);

		// Le chemin vers le fichier
		final IPath fileWorkspacePath = file.getFullPath().makeRelative();
		final String fileNameWithPath = fileWorkspacePath.toOSString();

		// La commande à lancer
		final ArrayList<String> command = new ArrayList<String>();
		command.add(OCAMLDEP);
		command.addAll(foldersPaths);
		command.add(fileNameWithPath);

		// Avant de lancer la commande, vérifier que le fichier existe.
		if (!file.exists())
			return null;

		// Et supprimer le fichier mli généré automatiquement, afin de ne pas
		// l'avoir dans la liste des dépendances.
		// On enlève le premier segment pour être relatif au projet
		final IFile autoGeneratedMli = project.getFile(fileWorkspacePath
				.removeFirstSegments(1).removeFileExtension().addFileExtension(
						"mli"));
		if ((autoGeneratedMli.exists())
				&& (Misc.isGeneratedFile(autoGeneratedMli))) {
			FileUtil.deleteFile(autoGeneratedMli);
		}

		// Executer commandRunner à la racine du workspace
		final CommandRunner cmdDep = new CommandRunner(command
				.toArray(new String[0]), project.getWorkspace().getRoot()
				.getLocation().toOSString());
		final String msg = cmdDep.getStdout();

		if (msg.length() != 0) {// si le message n'est pas vide
			// Récupérer toutes les lignes
			final String[] lines = (msg.split("\\n"));

			// Petit "algorithme" permettant de concaténer une "ligne" étendue
			// sur plusieurs lignes (avec le \ à la fin) en une seule ligne
			// normale (sans \).
			int i = 1;
			while (lines[0].endsWith("\\")) {
				// On enlève le \ final et on ajoute la ligne suivante
				lines[0] = lines[0].substring(0, (lines[0].length() - 2))
						.concat(lines[i]);
				i++;
			}

			// Liste des fichiers dans cette ligne (\s signifie "whitespace
			// character").
			final String[] allFiles = lines[0].split("\\s");

			// De la même façon que précédemment, il faut concaténer les noms
			// avec espaces : "nom\ du\ fichier.ml"
			// NOTE : incrémentation manuelle de j;
			for (int j = 0; j < allFiles.length;) {
				int k = 1;
				while (allFiles[j].endsWith("\\")) {
					// On enlève le \ final et on ajoute " " +le mot suivant
					allFiles[j] = allFiles[j].substring(0,
							(allFiles[j].length() - 1)).concat(" " +allFiles[j + k]);
					// Mettre allFiles[j+k] à "" afin de ne pas le traiter comme
					// un nom de fichier.
					allFiles[j + k] = "";
					k++;
				}
				//j doit passer directement à j+k
				j = j+k;

			}


			// Récupérer dans l'ordre inverse tous les fichiers sauf le
			// premier et changer les .cmo et .cmi en .ml et .mli
			// NOTA : on ne considère pas les .cmx, ils renvoient de toutes
			// façon au même fichier .ml
			// TODO ici, que faire pour les mll et mly ?
			final ArrayList<String> orderedDeps = new ArrayList<String>(
					allFiles.length - 1);
			for (int j = allFiles.length - 1; j > 0; j--) {
				// Ignorer les noms de fichiers vides, ceci peut arriver suite à
				// la concaténation des noms de fichiers avec espaces.
				if (!allFiles[j].equals("")) {
					String tempFileName = allFiles[j].replaceAll("\\.cmi\\z",
							".mli");
					tempFileName = tempFileName.replaceAll("\\.cmo\\z", ".ml");
					orderedDeps.add(tempFileName);
				}
			}

			// transformer ce tableau de String en tableau de IFile
			final ArrayList<IFile> orderedDepsAsIFile = new ArrayList<IFile>(
					orderedDeps.size());
			// Attention : ici tempFileName n'est pas forcément le nom avec le
			// chemin relatif à la racine du projet, il faut le rendre ainsi.
			for (String tempFileName : orderedDeps) {

				IPath tempFilePath = new Path(tempFileName);

				// Trouver le fichier correspondant au chemin (lie les fichiers
				// externes).
				IFile tempFile = findFile(project, tempFilePath);

				// Si le fichier existe
				if (tempFile != null && tempFile.exists()) {
					boolean autoGeneratedMliToSkip = false;
					// Tester si c'est un mli généré automatiquement local (non
					// lié)
					final String fileExt = tempFile.getFileExtension();
					if (fileExt != null && fileExt.matches("mli")) {
						if (Misc.isGeneratedFile(tempFile)
								&& !tempFile.isLinked()) {
							// On regarde si on a à faire à un mli
							// correspondant au fichier (dans ce cas
							// l'ignorer via autoGeneratedMliToSkip) sinon,
							// remplacer une dépendance à ce mli généré auto
							// par une dépendance au ml correspondant.
							final IPath tempFilePathNoExt = tempFilePath
									.removeFileExtension();
							final IPath filePathNoExt = file.getFullPath()
									.makeRelative().removeFileExtension();
							// Si les chemins sans extensions sont
							// différents, alors il faut considérer une
							// dépendance au ml correspondant
							if (!tempFilePathNoExt.equals(filePathNoExt)) {
								// On enlève le premier segment pour
								// rendre le chemin relatif au projet au
								// lieu du workspace
								final IPath tempFileMlPath = tempFilePathNoExt
										.addFileExtension("ml")
										.removeFirstSegments(1);
								tempFile = project.getFile(tempFileMlPath);
							} else {
								autoGeneratedMliToSkip = true;
							}

						}
					}
					// Ne pas ajouter les fichiers mli générés automatiquement.
					if (!autoGeneratedMliToSkip) {
						orderedDepsAsIFile.add(tempFile);
					}
				} else {
					OcamlPlugin.logError("error in DependenciesSetter:"
							+ "error file not found : "
							+ (tempFile != null ? tempFile.getName() : ""),
							new DependenciesGraphException());
				}
			}
			return orderedDepsAsIFile;
		}
		// Sinon le message était vide
		else
			return new ArrayList<IFile>(0);
	}

	/**
	 * Détermine un "handle" pour le chemin passé en paramètre.<br>
	 * Ceci ne test pas l'existence du fichier.<br>
	 * Permet de gérer des ressources externes, auquelle cas les ajoute dans le
	 * dossier portant le nom {@link OcamlBuilder#EXTERNALFILES}.<br>
	 * Les chemins qui ne sont pas absolus sont considérer comme étant relatif
	 * au workspace et ayant comme premier segment le nom du projet sur lequel
	 * on travail.<br>
	 * Tout autre chemin est à priori absolu (c'est comme ça qu'il est entré
	 * dans les pages de propriétés du projet) et tout chemin absolu n'étant pas
	 * un préfixe du chemin du projet sera lié en ressource externe.<br>
	 * 
	 * @param project
	 *            le project dans lequel on travail
	 * @param filePath
	 *            le chemin absolu vers un fichier se trouvant n'importe où dans
	 *            le système de fichier local ou relatif (et alors ayant comme
	 *            premier segment le nom du projet) vers un fichier dans le
	 *            projet
	 * @return un "handle" pour le chemin passé en paramètre.
	 */
	private IFile findFile(IProject project, IPath filePath) {

		// Le workspace
		final IWorkspaceRoot workspace = project.getWorkspace().getRoot();

		// Si le chemin a pour préfixe le chemin du projet, récupérer uniquement
		// le chemin relatif au workspace (car on renvoit un handle à partir du
		// workspace et non du projet)
		if (project.getLocation().isPrefixOf(filePath)) {
			filePath = filePath.removeFirstSegments(workspace.getLocation()
					.segmentCount());
		}
		// Sinon, si c'est quand même un chemin absolu (donc externe au projet),
		// lier les ressources au projet en ressources externes
		else if (filePath.isAbsolute()) {

			final IFolder externalFiles = project
					.getFolder(OcamlBuilder.EXTERNALFILES);
			// Récupérer le nom du fichier (ce path a forcément un dernier
			// segment)
			final String fileName = filePath.lastSegment();

			try {
				// Si le dossier n'existe pas, le créer en forçant
				if (!externalFiles.exists()) {
					externalFiles.create(true, true, null);
					/*
					 * Met l'attribut lecture seule au répertoire. Ceci n'empêche pas sa suppression
					 * dans Eclipse, mais affiche un message supplémentaire de confirmation avant la
					 * suppression.
					 */
					/*ResourceAttributes attributes = externalFiles.getResourceAttributes();
					if (attributes != null) {
						attributes.setReadOnly(true);
						externalFiles.setResourceAttributes(attributes);
					} else
						OcamlPlugin.logError("cannot set 'External files' as read only");
					*/
				}

				final IFile file = externalFiles.getFile(fileName);
				//Rafraichir pour voir les fichiers supprimés il y a peu de temps.
				file.refreshLocal(IResource.DEPTH_ZERO, null);
				// Créer le lien, sans faire de remplacement
				// Note : isLinked teste l'existence et renvoit false si le
				// fichier n'existe pas
				if (!file.isLinked() || !file.getLocation().equals(filePath)) {
					file.createLink(filePath, IFile.NONE, null);
				}
				// mettre à jour le chemin vers le fichier, relatif au workspace
				filePath = file.getFullPath().makeRelative();

			} catch (CoreException ce) {
				OcamlPlugin.logError("error in linking external resource", ce);
				return null;
			}
		}
		// Si le chemin était relatif au workspace, on arrive directement ici.
		// retourner le fichier
		return workspace.getFile(filePath);
	}

}
