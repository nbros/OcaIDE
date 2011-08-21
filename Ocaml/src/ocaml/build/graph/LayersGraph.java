package ocaml.build.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Represents a dependencies graph by layers between IFiles.<br>
 * A file that doesn't depend of any other is on layer 0.<br>
 * A file that depends on files on layer 0 is on layer 1.<br>
 * A file that depends from files of layers 0 or 1 (at least one from layer 1) is on layer 2. And so on...<br>
 * A leaf on this tree is a file from layer 0.<br>
 * We determine the layer of a file by computing the longest path from this file to a leaf.<br>
 * This allows us to handle dependencies in the right order: first layer 0 files, then layer 1 files...<br>
 * We also have a list of executables to link again those whose content was modified.
 */
public class LayersGraph {

	/**
	 * Un tableau à deux dimensions contenant tous les sommets du graphe.<br>
	 * Ce tableau est ordonné par couche : <code>layers[i]</code> représente la couche i
	 */
	private List<List<Vertex>> layers;

	/**
	 * Un tableau contenant les exécutables à créer.
	 */
	private List<Vertex> executables;

	/**
	 * Constructeur par défaut : alloue la place nécessaire pour les couches et les exécutables.<br>
	 * Note : l'accès aux couches est synchronisé.
	 * 
	 */
	public LayersGraph() {
		// Initialiser un tableau de couches
		layers = Collections.synchronizedList(new ArrayList<List<Vertex>>());
		// Et créer une seule couche pour l'instant : la couche 0.
		layers.add(0, Collections.synchronizedList(new ArrayList<Vertex>()));
		// Initialiser la liste d'exécutables.
		executables = Collections.synchronizedList(new ArrayList<Vertex>());
	}

	/**
	 * On alloue directement un certain nombre de couche.<br>
	 * Très utile pour les graphes "delta", il faut qu'ils aient par défaut autant de couches que le graphe de
	 * référence car si l'on vient lors de son parcours à ajouter des sommets dont la couche n'existe pas on
	 * aura une modification concurrente de la liste des couches.<br>
	 * 
	 * @param layersCount
	 *            le nombre de couches initiales à mettre dans le graphe. Doit etre positif (sinon 1 est pris
	 *            par défaut).
	 */
	public LayersGraph(int layersCount) {
		if (layersCount < 1) {
			layersCount = 1;
		}
		// Initialiser un tableau de couches.
		layers = Collections.synchronizedList(new ArrayList<List<Vertex>>());
		for (int i = 0; i < layersCount; i++) {
			// Créer autant de couches que nécessaire
			layers.add(i, Collections.synchronizedList(new ArrayList<Vertex>()));
		}
		// Initialiser la liste d'exécutables.
		executables = Collections.synchronizedList(new ArrayList<Vertex>());
	}

	/**
	 * Renvoit la couche spécifiée. <code>null</code> si la couche n'existe pas
	 * 
	 * @param index
	 *            le numéro de la couche renvoyée
	 * @return la couche si elle existe, <code>null</code> sinon.
	 */
	public List<Vertex> getLayer(int index) {
		// Vérifier si la couche existe
		if (index >= layers.size())
			return null;
		return layers.get(index);
	}

	/**
	 * @return la liste des exécutables.
	 */
	public List<Vertex> getExecutables() {
		return this.executables;
	}

	/**
	 * Cette méthod renvoit la taille du tableau de couches du graphe, soit le nombre de couches qu'il
	 * possède.<br>
	 * A priori la première et la dernière couche sont forcément non vide.<br>
	 * 
	 * @return le nombre de couches dans le graphe.
	 */
	public int getLayersCount() {
		return layers.size();
	}

	/**
	 * Ajoute une couche vide au graphe (à la fin).<br>
	 * 
	 */
	public void addEmptyLayer() {
		// add ajoute à la fin, c'est le comportement voulu.
		layers.add(Collections.synchronizedList(new ArrayList<Vertex>()));
	}

	/**
	 * Ajouter un sommet à une couche, s'il n'y est pas déjà.<br>
	 * Le sommet est sensé déjà connaitre sa couche, d'autre part cette méthode ne controle pas le fait que ce
	 * sommet soit rattaché au bon graphe.<br>
	 * Cette méthode n'est pas sensée être appelée à l'exterieur de cette classe.<br>
	 * Après avoir ajouter un sommet, il faut effectuer un recalcul des couches des sommets ajoutés.<br>
	 * 
	 * @param vertex
	 *            le sommet à ajouter.
	 */
	protected void addVertex(Vertex vertex) {
		// Récupérer la couche où il doit être insérer
		final int layerID = vertex.getLayerID();
		// Si on demande une couche qui n'existe pas encore, il
		// faut ajouter assez de nouvelles couches.
		while (getLayersCount() <= layerID) {
			addEmptyLayer();
		}
		// On est maintenant sûr que la couche existe.
		final List<Vertex> layer = getLayer(layerID);
		// On ajoute le sommet uniquement s'il n'est pas déjà dans la couche
		// (note : il ne peut etre ailleurs car un sommet est toujours dans
		// la couche qui correspond à son numéro de couche.)
		if (!layer.contains(vertex)) {
			layer.add(vertex);
		}
	}

	/**
	 * Ajouter toute une liste de sommets au graphe.<br>
	 * Cette liste est censée contenir toutes les dépendances des sommets de la liste, si un sommet dépend
	 * d'un sommet qui n'est pas dans la liste, ce dernier est supposé être dans ce graphe ou dans un graphe
	 * ayant servi de référence pour construire ce graphe. Il faut qu'en tous cas ces sommets aient des bons
	 * numéros de couche.<br>
	 * Cette méthode effectue un rafraichissement des couches du graphe, on peut supposer que le graphe est
	 * ordonné après sa terminaison.<br>
	 * <br>
	 * Si des sommets appartenaient à un autre graphe, ceci les enlève de ce graphe, mais ils sont placés dans
	 * la même couche (peu importe le graphe auquel appartient le sommet, sa place dans la hierarchie de
	 * dépendances reste la même).<br>
	 * 
	 * @param newVertices
	 *            la liste des sommets et de leur dépendances à ajouter au graphe.
	 */
	public void addAll(List<Vertex> newVertices) {
		// Une liste des sommets à déplacer (c'est à dire des sommets que l'on
		// devra supprimer de l'ancien graphe avant de les ajouter à celui-là)
		// Indispensable pour éviter la modification concurrente
		final List<Vertex> verticesToMove = new ArrayList<Vertex>(newVertices.size());
		// Traiter chaque sommet à ajouter
		for (Vertex vertex : newVertices) {
			addVertex(vertex);
			// le graphe auquel était rattaché le sommet
			final LayersGraph verGraph = vertex.getGraph();
			// Si le sommet n'appartenait à aucun graphe, il faut juste le
			// rattacher à celui ci sinon s'il appartenait à un autre graphe, il
			// va falloir le supprimer de cet ancien graphe et changer son
			// graphe de rattachement.
			if (verGraph != this) {
				if (verGraph != null) {
					verticesToMove.add(vertex);
				} else {
					vertex.setGraph(this);
				}
			}
		}
		for (Vertex vertex : verticesToMove) {
			// Note : le graphe est non null
			vertex.getGraph().removeVertex(vertex);
			vertex.setGraph(this);
		}
		refreshLayer(newVertices);
	}

	/**
	 * Ajoute un exécutable à la liste des exécutables uniquement si celui-ci n'y est pas déjà.<br>
	 * 
	 */
	public void addExe(Vertex exe) {
		if (!executables.contains(exe)) {
			executables.add(exe);
		}
	}

	/**
	 * Enlève un exécutable de la liste des exécutables, et enlève cet exécutable de la liste des executables
	 * affectés de chaque fichier à lier.<br>
	 * 
	 * @param exe
	 * @return <code>true</code> si tout s'est bien passé, ou <code>false</code> sinon (particulièrement
	 *         si l'exe n'a pas été trouvé dans une liste d'exe - celle du graphe ou alors une affectedExe).<br>
	 */
	public boolean removeExe(Vertex exe) {
		if (!executables.contains(exe)) {
			return false;
		}
		// Enlever le sommet de la liste des exe affectés de chaque fichier à
		// lier.
		for (Vertex fileToLink : exe.getAllFilesToLink()) {
			if (!fileToLink.removeAffectedExe(exe)) {
				return false;
			}
		}
		// réinitialiser la liste des fichiers à lier.
		exe.getAllFilesToLink().clear();
		return true;
	}

	/**
	 * Rafraichit le numéro de couche d'un ensemble de sommet ainsi que de tous les sommets affectés (et
	 * récursivement) lorsque le numéro est réellement changé.<br>
	 * Déplace les sommets s'ils ne sont pas dans la bonne couche.<br>
	 * Attention : ne pas passer une couche du graphe en paramètre, car elle serait modifiée en cours
	 * d'utilisation... Dans ce cas précis il faut faire une copie de la couche et passer cette copie en
	 * paramètre.<br>
	 * 
	 * @param verticesSet
	 *            Un ensemble de sommet à rafraichir (qui n'est pas une couche du graphe).
	 */
	public void refreshLayer(List<Vertex> verticesSet) {
		// Créer un visiteur qui traitera chaque sommet et ensuite ses fichiers
		// affectés.
		final IPostAffectedFilesVisitor refresher = new RefreshLayersVisitor();

		// Visiter chaque sommet de l'ensemble.
		for (Vertex vertex : verticesSet) {
			// Visiter le sommet
			vertex.accept(refresher);
		}

	}

	/**
	 * Cherche un sommet dans le graphe.<br>
	 * Parcours le graphe par couche.<br>
	 * 
	 * @param file
	 *            le fichier à rechercher
	 * @return le sommet s'il est trouvé, null sinon.
	 */
	public Vertex findVertex(IFile file) {
		for (List<Vertex> layer : layers) {
			for (Vertex vertex : layer) {
				if (vertex.getFile().equals(file))
					return vertex;
			}
		}
		return null;
	}

	/**
	 * Cette méthode cherche le sommet à partir du nom du fichier, et l'efface.<br>
	 * Voir la méthode de même nom s'appliquant directement à un sommet.<br>
	 */
	public boolean suppressVertex(IFile file) {
		final Vertex vertex = findVertex(file);
		if (vertex == null) {
			return false;
		}
		return suppressVertex(vertex);

	}

	/**
	 * Supprimer un sommet du graphe.<br>
	 * l'enlève de la hierarchie des dépendances, récupère tous ses fichiers affectés, le supprime du graphe,
	 * et rafraichit la couche de tous les fichiers affectés.
	 * 
	 * @param vertex
	 *            le sommet à supprimer
	 * @return true si tout s'est bien passé, false sinon.
	 */
	public boolean suppressVertex(Vertex vertex) {

		final List<Vertex> layer = getLayer(vertex.getLayerID());

		// Récupérer les fichiers affectés et les copier car il faudra à la fin
		// rafraichir leur numéro de couche.
		final List<Vertex> affectedFiles = vertex.getAffectedFiles();
		final List<Vertex> filesToRefreshLayer = new ArrayList<Vertex>(affectedFiles.size());
		for (Vertex v : affectedFiles) {
			filesToRefreshLayer.add(v);
		}

		if (!vertex.suppress()) {
			return false;
		}
		if (!layer.remove(vertex)) {
			return false;
		}
		// Regarder si le sommet était un exécutable
		if (vertex.getExeName() != null) {
			// Si oui il faut le supprimer de la liste des exécutables
			if (!executables.remove(vertex)) {
				return false;
			}
		}
		// Regarder si le sommet était externe auquel cas effacer le fichier lié correspondant.
		if (vertex.isExternalFile()) {
			try {
				// forcer
				vertex.getFile().delete(true, null);
			} catch (CoreException e) {
				OcamlPlugin.logError("error in LayersGraph:suppressVertex:" + " error deleting "
						+ vertex.getFile().getName(), e);
			}
		}
		// Si on enlève un sommet ml du graphe, qui avait un mli généré
		// automatiquement, supprimer ce mli (si getMLIVertex renvoit null alors
		// il n'y avait pas de mli donc à priori un généré automatiquement.)
		if ((vertex.getType() == Vertex.MLTYPE) && (vertex.getMLIVertex() == null)) {
			final IProject project = vertex.getFile().getProject();
			final IPath autoGeneratedMliPath = vertex.getFile().getProjectRelativePath()
					.removeFileExtension().addFileExtension("mli");
			try {
				final IFile autoGeneratedMli = project.getFile(autoGeneratedMliPath);
				if (autoGeneratedMli.exists()) {
					autoGeneratedMli.delete(true, null);
				}
			} catch (CoreException e) {
				OcamlPlugin.logError("error in LayersGraph : suppressVertex: "
						+ "error deleting matching autogenerated mli", e);
			}
		}
		// Rafraichir le numéro de couche des fichiers affectés
		refreshLayer(filesToRefreshLayer);

		return true;
	}

	/**
	 * Déplacer un sommet d'une couche dans une autre.<br>
	 * Attention : la couche actuelle du sommet doit correspondre à l'ancienne couche.<br>
	 * Attention : cette méthode modifie le champ "layer" du sommet.<br>
	 * Cette méthode n'est pas sensée être appelée par l'utilisateur, pour déplacer un sommet utiliser plutot
	 * la méthode {@link LayersGraph#refreshLayer(List)}.
	 * 
	 * @param v
	 *            le sommet possédant encore le numéro de l'ancienne couche
	 * @param newLayer
	 *            le numéro de la nouvelle couche.
	 */
	public void moveVertex(Vertex v, int newLayer) {
		// Enlever le sommet de sa couche actuelle (erreur si on le trouve pas)
		if (!getLayer(v.getLayerID()).remove(v)) {
			OcamlPlugin.logError("error in LayersGraph:moveVertice :" + " vertice not found",
					new DependenciesGraphException());
		}
		// Changer le numéro de couche
		v.setLayer(newLayer);
		// Ajouter le sommet dans le graphe pour son numéro de couche actuel.
		addVertex(v);

	}

	/**
	 * Enlever un sommet du graphe, ce n'est pas le supprimer, il existe toujours dans la hierarchie des
	 * dépendances, il n'est juste plus dans le graphe.<br>
	 * 
	 * @param v
	 *            le sommet à enlever
	 */
	public void removeVertex(Vertex v) {
		if (!getLayer(v.getLayerID()).remove(v)) {
			OcamlPlugin.logError("error in removing vertex" + " from graph : vertex isn't in his layer");
		} else {
			// Le sommet n'est rattaché à rien
			v.setGraph(null);
		}
	}

	/**
	 * Fusionner un autre graphe à celui ci.<br>
	 * Ceci ajoute chacune des couches de l'autre graphe dans ce graphe.<br>
	 * On ajoute que des sommets qui ne sont pas déjà présents.<br>
	 * L'ajout entraine le changement du champ dans un sommet indiquant à quel graphe il appartient, et
	 * entraine un rafraichissement des couches.<br>
	 * Il faut également fusionner la liste des exécutables.<br>
	 * 
	 * @param otherGraph
	 *            l'autre graphe avec qui l'on doit fusionner.
	 */
	public void mergeWith(LayersGraph otherGraph) {
		// récupérer le nombre de couches de l'autre graphe
		final int nbLayers = otherGraph.getLayersCount();
		// Ajouter chaque couche au graphe
		for (int i = 0; i < nbLayers; i++) {
			addAll(otherGraph.getLayer(i));
		}
		// Fusionner les exécutables sans répétition.
		for (Vertex exe : otherGraph.getExecutables()) {
			addExe(exe);
		}
	}

	/**
	 * Efface les couches vides finales du graphe.
	 * 
	 */
	public void removeEmptyFinalLayers() {
		// Ce booléen permet de détecter si l'on a commencé à enlever des
		// couches. Normalement, on ne doit enlever que des couches qui se
		// trouvent à la fin.
		boolean hasAlreadyLostLayers = false;
		// Les couches qui seront supprimées plus tard
		final List<List<Vertex>> layersToRemove = new ArrayList<List<Vertex>>();
		// Pour chaque couche
		for (List<Vertex> layer : layers) {
			final boolean isEmpty = layer.isEmpty();
			// Si la couche n'est pas vide alors qu'on a déjà supprimé des
			// couches, ne pas supprimer ces couches.
			if (!isEmpty && hasAlreadyLostLayers) {
				layersToRemove.clear();
			} else if (isEmpty) {
				// layers.remove(layer);
				// On ne peut pas écrire remove car on a alors une modification
				// concurrente de la liste avec l'itérateur de la boucle for.
				layersToRemove.add(layer);
				hasAlreadyLostLayers = true;
			}
		}
		// On peut désormais retirer les couches sans problème d'accès
		// concurrent.
		// Les couches dans ce tableau sont obligatoirement en fin de tableau de
		// couches.
		layers.removeAll(layersToRemove);
	}

	/**
	 * Visiter tous les fichiers de toutes les couches de la première à la dernière.<br>
	 * Ceci respecte donc l'ordre des dépendances.<br>
	 * Si le visiteur return false lors d'une visite, le traitement s'arrete et les autres fichiers ne sont
	 * pas traités.
	 * 
	 * @param visitor
	 *            le visiteur devant visiter chaque sommet du graphe.
	 * @param monitor
	 */
	public void accept(ILayersVisitor visitor, IProgressMonitor monitor) {
		if ((monitor != null) && monitor.isCanceled())
			return;
		for (List<Vertex> layer : layers) {
			for (Vertex vertex : layer) {
				if (!visitor.visit(vertex, monitor)) {
					return;
				}
			}
		}
	}

	/**
	 * Visiter tous les exécutables liés au graphe.<br>
	 * Ceci ne revient qu'à parcourir le tableau.<br>
	 * Si le visiteur ne retourne pas true, la visite s'arrete.<br>
	 */
	public void accept(IExecutablesVisitor visitor, IProgressMonitor monitor) {
		if ((monitor != null) && monitor.isCanceled())
			return;
		for (Vertex exe : executables) {
			if (!visitor.visit(exe, monitor)) {
				return;
			}
		}
	}

}
