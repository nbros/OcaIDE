package ocaml.build;

import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.graph.LayersGraph;
import ocaml.build.graph.Vertex;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * This class is responsible for managing the resources in a delta: <br>
 * <ul>
 * <li> deleted file: delete the corresponding vertex from the graph
 * <li> renamed file: rename the vertex file, and move it to the list of changed vertices
 * <li> anything else: add the vertices to the changed vertices list
 * </ul>
 * 
 * This class also manages modification or deletion of automatically generated mli files by the user. It also
 * detects sub-directories added to the project, so as to add them the the project's paths.
 */

public class SuitableFilesDeltaFinder implements IResourceDeltaVisitor {

	/** The files we found */
	private List<IFile> files;

	/** The new sub-folders, added to the project's paths */
	private List<IPath> paths;

	/** The graph from which we can delete vertices, or get existing vertices. */
	private LayersGraph existingGraph;

	/**
	 * @param files
	 *            where to store the results
	 * @param paths
	 *            where to store the new paths
	 * @param existingGraph
	 *            what to delete the vertices from
	 */
	public SuitableFilesDeltaFinder(final List<IFile> files, final List<IPath> paths,
			final LayersGraph existingGraph) {
		this.files = files;
		this.paths = paths;
		this.existingGraph = existingGraph;
	}

	/**
	 * Visit a delta. We act depending on the changes:
	 * <ul>
	 * <li> deleted: delete the vertex (compile the corresponding ml file if it was a mli file)
	 * <li> added or modified: add it to the changed files list, and detect when the user modified an mli
	 * file, in which case we add all the affected files.
	 * 
	 * Note: every modification of a filename (renaming, moving, copying) will trigger a deleting followed by
	 * an adding.
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {

		final IResource resource = delta.getResource();

		// Le projet associé
		final IProject project = resource.getProject();

		// Le type de modification
		final int kind = delta.getKind();

		// S'occuper des répertoires ajoutés
		if ((resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT)
				&& (kind == IResourceDelta.ADDED || kind == IResourceDelta.ADDED_PHANTOM)) {
			// Ajouter le chemin (On vérifiera lors de OcamlPath.addToPaths
			// qu'il ne correspond pas à un repertoire lié ou de sources
			// externes etc...)
			paths.add(resource.getProjectRelativePath());

			return true;
		}

		// On ne veut plus que des fichiers
		if (resource.getType() != IResource.FILE) {
			return true;
		}

		// Ce cast est maintenant sûr
		final IFile file = (IFile) resource;

		// On vérifie que c'est bien un fichier de source
		if (!Misc.isOCamlSourceFile(file))
			return true;

		// Traiter les suppressions des fichiers sources en premier
		if (kind == IResourceDelta.REMOVED) {
			final Vertex fileInGraph = existingGraph.findVertex(file);
			// Si le sommet était dans le graphe, il faut recompiler ses
			// fichiers affectés.
			// Note : ce n'est pas la peine à priori de relier les exécutables,
			// puisqu'il y aura des erreurs.
			if (fileInGraph != null) {
				for (Vertex ver : fileInGraph.getAffectedFiles()) {
					final IFile verFile = ver.getFile();
					if (!files.contains(verFile)) {
						files.add(verFile);
					}
				}
				existingGraph.suppressVertex(fileInGraph);
			}
			// Sinon si c'est un mli (généré automatiquement donc pas dans le
			// graphe) supprimé, compiler le ml correspondant afin de re-générer
			// un mli
			else if ("mli".equals(file.getFileExtension())) {
				// Essayer brutalement
				// Note : ici pas de NullPointerException puisque aucune de
				// ces méthode n'a besoin que le fichier existe.
				final Vertex matchingMLVertex = existingGraph.findVertex(project.getFile(file
						.getProjectRelativePath().removeFileExtension().addFileExtension("ml")));

				if (matchingMLVertex != null) {
					final IFile matchingML = matchingMLVertex.getFile();
					// On ajoute sans répétition
					if (!files.contains(matchingML)) {
						files.add(matchingML);
					}
				}
			}
			return true;
		}

		// On ne retient que les fichiers existants de sources ocaml et non
		// générés automatiquement (un généré automatiquement peut peut être
		// arriver jusqu'ici dans le code, s'il a été ajouté)
		if (!file.exists() || Misc.isGeneratedFile(file)) {
			return true;
		}

		// Ici on veut être sûr que l'on est dans le bon cas, même si on ne
		// traite pas les fantomes, si jamais un type REMOVED_PHANTOM survient,
		// on l'ignore, et si jamais de nouvelles constantes apparaissent, elles
		// seront ignorées.
		if (kind == IResourceDelta.ADDED || kind == IResourceDelta.ADDED_PHANTOM
				|| kind == IResourceDelta.CHANGED) {

			// Si c'est un fichier externe qui a été modifié (lors de touch dans
			// les pages de propriétés ?)
			if (file.isLinked() && (existingGraph != null)) {
				final Vertex extVertex = existingGraph.findVertex(file);
				if (extVertex != null) {
					// A priori, recompiler tous les fichiers et exécutables
					// affectés, mais en fait il suffit de relier les
					// exécutables puisque le seul moyen d'avoir un évènement
					// sur un fichier externe c'est lorsqu'on lui lie un fichier
					// objet ?
					for (Vertex v : extVertex.getAffectedFiles()) {
						if (!files.contains(v.getFile())) {
							files.add(v.getFile());
						}
					}
					// Ajouter les executables affectés à la liste de fichier à
					// compiler n'entrainera pas forcément la liaison, la
					// compilation certe, mais s'il n'y a pas de différences
					// entre les fichiers objets, alors pas de liaison. Ici on
					// triche, on enlève l'ancien fichier objet correspondant
					// pour forcer la re-liaison
					for (Vertex v : extVertex.getAffectedExe()) {
						v.setObjectFile(null);
						if (!files.contains(v.getFile())) {
							files.add(v.getFile());
						}
					}
				}
				return false;
			}
			// On ajoute sans répétition
			if (!files.contains(file)) {
				files.add(file);
			}

			// Il faut maintenant détecter si c'est un généré
			// automatiquement modifié par l'utilisateur.
			// Si IS_GEN vaut "false" alors c'est qu'il vient d'être modifié par
			// l'utilisateur.
			// Note : on ne peut pas avoir de fichier généré automatiquement
			// ici, car si jamais un fichier généré automatiquement est modifié
			// (condition pour etre dans le delta) alors il perd la propriété
			// généré automatiquement.
			if (Misc.getFileProperty(file, OcamlBuilder.IS_GEN).equals("false")) {
				// mettre la propriété à null (l'enlever) afin de ne pas
				// recommencer ceci.
				Misc.setFileProperty(file, OcamlBuilder.IS_GEN, null);
				// Il va falloir compiler le fichier
				if (!files.contains(file)) {
					files.add(file);
				}

				// Si jamais c'était un fichier mli généré automatiquement

				if ("mli".equals(file.getFileExtension())) {
					// Récupérer le sommet ml correspondant (qui est forcément
					// déjà dans le graphe car c'est à partir de lui que l'on a
					// généré le mli
					final IPath mlFilePath = file.getProjectRelativePath().removeFileExtension()
							.addFileExtension("ml");
					final IFile mlFile = project.getFile(mlFilePath);
					final Vertex mlVertex = existingGraph.findVertex(mlFile);
					// l'ajouter sans répétition
					if (mlVertex.getFile().equals(mlFile)) {
						if (!files.contains(mlFile)) {
							files.add(mlVertex.getFile());
						}
					} else {
						OcamlPlugin.logError("error in SuitableFilesDeltaFinder:" + "visit: error: "
								+ "mlFile and mlVertex.getFile() are not equals");
					}

					// Puis récupérer et ajouter tous ses fichiers affectés (qui
					// devront maintenant dépendre du nouveau mli)
					final List<Vertex> affectedFiles = mlVertex.getAffectedFiles();
					for (Vertex vertex : affectedFiles) {
						if (!files.contains(vertex.getFile())) {
							files.add(vertex.getFile());
						}
					}
				}

			}

		}
		return true;
	}

}
