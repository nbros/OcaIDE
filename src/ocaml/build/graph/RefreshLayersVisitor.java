package ocaml.build.graph;

import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;

/**
 * This visitor determines what layer each vertex is on, and handles necessary changes. It is meant to be
 * started on leaves, or on vertices whose required vertices have a correct layer number, because it relies on
 * this information to compute the new layer number.
 * 
 * The visitor stops when a computed layer number is the same as the current vertex number, or when we have
 * visited all the affected files.
 */
public class RefreshLayersVisitor implements IPostAffectedFilesVisitor {

	/**
	 * Une liste se rappelant les fichiers parcourus par ce visiteur. <br>
	 * Ceci (Re)détecte les cycles et permet d'arreter le calcul du graphe.
	 */
	private List<Vertex> filesBeingVisited;

	/**
	 * Constructeur initialisant le tableau des fichiers déjà visité (détection de cycle).
	 * 
	 */
	public RefreshLayersVisitor() {
		filesBeingVisited = new ArrayList<Vertex>();
	}

	/**
	 * Empiler un sommet sur la pile des sommets en cours de visite avant de visiter les dépendances du sommet
	 * (détection de cycle)
	 */
	public void pushVertex(Vertex v) {
		filesBeingVisited.add(v);
	}

	/**
	 * Dépiler un sommet de la pile des sommets en cours de visite une fois le sommet (et ses dépendances)
	 * complètement visité.
	 */
	public void popVertex(Vertex v) {
		if (!filesBeingVisited.remove(v)) {
			OcamlPlugin.logError("error in RefreshLayersVisitor:" + "popVertex: vertex not found");
		}
	}

	public boolean visit(Vertex ver) {

		// Détecter les cycles pour ne pas rajouter des couches à l'infini
		if (filesBeingVisited.contains(ver)) {
			OcamlPlugin.logError("error in RefreshLayersVisitor:visit:"
					+ "  dependencies graph may be wrong because of a cycle");
			return false;
		}

		final List<Vertex> neededFiles = ver.getNeededFiles();
		// Rechercher le maximum des couches des noeud "parents" (ou
		// prédécesseurs : les noeud requis).
		// Par défaut on est dans la couche 0.
		int maxParentLayer = 0;
		// S'il n'y a pas de fichiers requis, alors le numéro de couche doit
		// etre mis à 0 par ce qui suit, il faut donc l'initialiser à -1.
		if (neededFiles.isEmpty()) {
			maxParentLayer = -1;
		} else {
			for (Vertex neededFile : neededFiles) {
				if (neededFile.getLayerID() > maxParentLayer) {
					maxParentLayer = neededFile.getLayerID();
				}
			}
		}
		// Si la valeur trouvée est différente de l'ancienne, propager les
		// changements.
		// + 1 car on est dans une couche supérieure à celle de son
		// prédécesseur.
		if ((maxParentLayer + 1) != ver.getLayerID()) {
			// Signaler au graphe auquel est rattaché le sommet de le changer de
			// place (et donc de changer le numéro de couche du sommet)
			ver.getGraph().moveVertex(ver, maxParentLayer + 1);

			// propager les changements :
			return true;

		}
		// Pas de changements, ne pas visiter les fichiers affectés.
		return false;
	}

}
