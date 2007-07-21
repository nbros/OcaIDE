package ocaml.build.graph;

import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;

/** A vertex from a dependencies graph for IFiles. */
public class Vertex {

	public final static int DEFAULTTYPE = 0;

	/**
	 * Type d'un sommet correspondant à un fichier .ml
	 */
	public final static int MLTYPE = 1;

	/**
	 * Type d'un sommet correspondnat à un fichier .mli
	 */
	public final static int MLITYPE = 2;

	/**
	 * Type d'un sommet correspondnat à un fichier .mll
	 */
	public final static int MLLTYPE = 3;

	/**
	 * Type d'un sommet correspondnat à un fichier .mly
	 */
	public final static int MLYTYPE = 4;

	/**
	 * le fichier associé au sommet
	 */
	private IFile file;

	/**
	 * les fichiers requis pour traiter notre fichier, les "dépendances". Ces fichiers doivent être traité
	 * avant notre fichier.
	 */
	private List<Vertex> neededFiles;

	/**
	 * les fichiers affectés par un changement sur notre fichier, les "fichiers qui nous utilisent". Ces
	 * fichiers doivent être traités après notre fichier, si notre fichier a subit une modification qui les
	 * affecte.
	 */
	private List<Vertex> affectedFiles;

	/**
	 * les exécutables qui utilisent ce fichier.<br>
	 * Ils doivent être reliés lorsque le fichier objet change.<br>
	 */
	private List<Vertex> affectedExe;

	/**
	 * Contient tous les fichiers à lier pour la création d'un executable.<br>
	 * Ceci est en fait une liste de toutes les dépendances récursivement.<br>
	 */
	private List<Vertex> allFilesToLink;

	/**
	 * Ce champ correspond à la propriété persistante make_exe d'un fichier.<br>
	 * Il vaut null s'il n'y a pas de telle propriété, et vaut le nom de l'exécutable sinon.<br>
	 */
	private String exeName;

	/**
	 * le graphe auquel le sommet est rattaché.
	 */
	private LayersGraph graph;

	/**
	 * la "couche de dépendances" à laquelle appartient le fichier. Pour plus de détails voir la description
	 * de la classe du graphe.
	 */
	private int layer;

	/**
	 * Un booléen permettant de savoir si un fichier est externe auquel cas on ne le compile pas
	 */
	private boolean externalFile;

	/**
	 * Le type du fichier associé au sommet.
	 */
	private int type;

	/**
	 * Fichier objet généré lors de la compilation.<br>
	 * C'est ce fichier qui faudra fournir pour la liaison (.cmo ,.cmx, .cma ..)<br>
	 * Ce champ est <code>null</code> s'il n'y a rien à fournir lors de la liaison (ex : interface)<br>
	 * Note : pour les fichiers externes, voir uniquement la méthode getExternalObjectFiles
	 */
	private IFile objectFile;

	/**
	 * Fichier objet représentant l'interface d'un fichier .ml (.cmi).<br>
	 * C'est l'identité de ce fichier après compilation que l'on doit tester afin de détecter un changement à
	 * propager dans les fichiers affectés.<br>
	 * Ce champ est <code>null</code> si la compilation du fichier ne génère pas de .cmi (ex : si ce fichier
	 * possède une interface.)
	 * 
	 */
	private IFile interfaceObjectFile;

	/**
	 * Un constructeur initialisant à vide les fichiers affectés, requis, les exécutables affectés et les
	 * fichiers à lier pour la création d'un executable et spécifiant la couche 0 par défaut.<br>
	 * Attention : cette méthode ne rattache pas de graphe au sommet, ceci est fait lors de la méthode d'ajout
	 * à un graphe.<br>
	 * Par défaut, le fichier est supposé interne au projet.
	 * 
	 * @param file
	 *            le fichier à associer
	 * @see Vertex#Vertex(IFile, boolean) si le fichier est externe au projet
	 */
	public Vertex(IFile file) {
		this.file = file;
		this.affectedFiles = new ArrayList<Vertex>();
		this.neededFiles = new ArrayList<Vertex>();
		this.affectedExe = new ArrayList<Vertex>();
		this.allFilesToLink = new ArrayList<Vertex>();
		this.exeName = null;
		this.layer = 0;
		this.externalFile = false;
		this.type = DEFAULTTYPE;
		this.interfaceObjectFile = null;
		this.objectFile = null;
	}

	/**
	 * @see Vertex#Vertex(IFile)
	 * @param file
	 *            le fichier à associer
	 * @param external
	 *            si le fichier est externe au projet
	 */
	public Vertex(IFile file, boolean external) {
		this.file = file;
		this.affectedFiles = new ArrayList<Vertex>();
		this.neededFiles = new ArrayList<Vertex>();
		this.affectedExe = new ArrayList<Vertex>();
		this.allFilesToLink = new ArrayList<Vertex>();
		this.exeName = null;
		this.layer = 0;
		this.externalFile = external;
		this.type = DEFAULTTYPE;
		this.interfaceObjectFile = null;
		this.objectFile = null;
	}

	/**
	 * renvoit le numéro de couche de dépendances à laquelle appartient ce sommet.
	 * 
	 * @return le numéro de la couche
	 */
	public int getLayerID() {
		return layer;
	}

	/**
	 * Détermine si le fichier associé est externe au projet
	 * 
	 * @return true si le fichier associé est externe au projet
	 */
	public boolean isExternalFile() {
		return externalFile;
	}

	/**
	 * Spécifie si le fichier associé est un .ml, .mli etc...<br>
	 * 
	 * @return la constante représentant le type (voir les constantes dans la classe Vertex)<br>
	 * @see Vertex#MLITYPE , Vertex#MLTYPE ...
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Cette méthode détermine le type d'un sommet en fonction de l'extension du fichier associé.<br>
	 * L'existence du fichier est sensée avoir été testée avant l'appel de cette méthode.<br>
	 * 
	 */
	public void setType() {
		final String ext = file.getFileExtension();
		if (ext != null) {
			if (ext.matches("ml")) {
				type = MLTYPE;
			}
			if (ext.matches("mli")) {
				type = MLITYPE;
			}
			if (ext.matches("mll")) {
				type = MLLTYPE;
			}
			if (ext.matches("mly")) {
				type = MLYTYPE;
			}

		}
	}

	/**
	 * Renvoit le fichier objet compilé associé à ce fichier, à utiliser lors de la liaison.
	 * 
	 * @return le fichier objet .cmo, .cmx ou autre à utiliser lors de la liaison.<code>null</code> Si un
	 *         tel fichier n'existe pas (typiquement si le fichier associé au sommet est une interface, auquel
	 *         cas le fichier qui ressort de la compilation est un .cmi)
	 */
	public IFile getObjectFile() {
		return this.objectFile;
	}

	/**
	 * Associe le fichier objet à utiliser pour la phase de liaison.
	 * 
	 * @param objFile
	 *            le fichier à associer.
	 */
	public void setObjectFile(IFile objFile) {
		this.objectFile = objFile;
	}

	/**
	 * Renvoit le fichier objet compilé associé à une interface.<br>
	 * C'est à dire que si le fichier associé au sommet est une interface, ce sera le fichier obtenu lors de
	 * la compilation (.cmi) si le fichier associé est un fichier source .ml qui n'a pas d'interface, ceci
	 * sera le fichier .cmi correspondant à l'exportation de toutes les valeurs déclarées dans le fichier
	 * source.
	 * 
	 * @return le fichier .cmi s'il existe. <code>null</code> s'il n'y pas de tel fichier. (C'est à dire
	 *         qu'il y a un sommet associé à une interface non généré automatiquement correspondant à ce
	 *         fichier qui contient le bon .cmi.)
	 */
	public IFile getInterfaceObjectFile() {
		return this.interfaceObjectFile;
	}

	/**
	 * Associe un fichier objet compilé correspondant à une interface (.cmi) à ce sommet.
	 * 
	 * @param itfObjFile
	 *            le fichier à associer.
	 */
	public void setInterfaceObjectFile(IFile itfObjFile) {
		this.interfaceObjectFile = itfObjFile;
	}

	/**
	 * Rafraichit le nom de l'exécutable.<br>
	 * C'est à dire récupérer directement la propriété persistante, donc si elle n'existe pas, exeName vaudra
	 * <code>null</code>.<br>
	 * Ceci signale s'il y a eu un changement au niveau du nom de l'exécutable.<br>
	 * 
	 * @return true si la propriété make_exe a changé de valeur, false sinon.
	 * 
	 */
	public boolean refreshExeName() {
		String temp = Misc.getResourceProperty(file, Misc.MAKE_EXE);
		final boolean tempIsEmpty = temp.equals("");
		// Si les noms sont les même, ou n'existent pas, renvoyer faux.
		if (((tempIsEmpty) && (exeName == null)) || ((exeName != null) && (temp.equals(exeName)))) {
			return false;
		}
		// Sinon les deux noms sont forcément différents
		else {
			// Renvoyer null si temp est vide
			exeName = tempIsEmpty ? null : temp;
			return true;
		}

	}

	/**
	 * Déterminer la couche à laquelle ce sommet appartient, en fonction des couches des fichiers requis. On
	 * prend la plus élevée et on ajoute 1. Puis on propage le changement sur les fichiers affectés si la
	 * couche a effectivement changé.<br>
	 * Cette méthode modifie le champ de cet objet et la couche du graphe.
	 * 
	 * @return la couche déterminée.
	 * 
	 */
	public int refreshLayer() {
		// Le maximum des couches des noeud "parents" (ou prédécesseurs : les
		// noeud requis).
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
		// changements
		// + 1 car on est dans une couche supérieure à celle de son
		// prédécesseur.
		if ((maxParentLayer + 1) != this.layer) {
			// Signaler au graphe de nous changer de place.
			graph.moveVertex(this, maxParentLayer + 1);
			// Mettre à jour notre propre numéro de couche.
			this.layer = maxParentLayer + 1;

			for (Vertex affectedFile : affectedFiles) {
				// Demander le recalcul de la couche. Si un numéro plus petit
				// que le notre est renvoyé, erreur ! (Impossible!)
				if (affectedFile.refreshLayer() < this.layer) {
					OcamlPlugin.logError("affected file found a lower layer's number",
							new DependenciesGraphException());
				}
			}
		}
		return layer;

	}

	/**
	 * Parcours les fichiers requis d'un sommet en profondeur. Effectue le traitement "visit" à la "remontée"
	 * c'est à dire que l'on visite un sommet une fois ses dépendances traitées. Va chercher le sommet le plus
	 * profond, puis le visite, et passe à un autre sommet.<br>
	 * Note : Il n'y a pas moyen ici d'utiliser le retour de visit afin de visiter ou non les dépendances
	 * puisque visit est appelée après avoir été appelé sur les dépendances...
	 * 
	 * @param visitor
	 *            le visiteur du graphe, contient dans sa méthode visit le traitement à appliquer à chaque
	 *            sommet.
	 * 
	 * @see Vertex#accept(IPostNeededFilesVisitor) pour les méthode pop et push (détection de cycle ou autre)
	 */
	public void accept(IPreNeededFilesVisitor visitor) {
		visitor.pushVertex(this);
		for (Vertex neededFile : neededFiles) {
			neededFile.accept(visitor);
		}
		visitor.popVertex(this);
		visitor.visit(this);
	}

	/**
	 * Parcours les fichiers affectés d'un sommet en profondeur. Effectue le traitement "visit" à la
	 * "descente" c'est à dire que l'on visite un sommet avant ses sommets affectés. Visite le sommet, si
	 * visit renvoit true, visite tous les sommets affectés.<br>
	 * 
	 * Cette méthode empile le sommet à la liste des sommets en cours de visite du visiteur avant de visiter
	 * les dépendances afin qu'il puisse détecter les cycles. Le sommet est dépilé à la fin de la visite des
	 * dépendances.<br>
	 * 
	 * 
	 * @param visitor
	 *            le visiteur du graphe, contient dans sa méthode visit le traitement à appliquer à chaque
	 *            sommet.
	 */
	public void accept(IPostAffectedFilesVisitor visitor) {
		if (!visitor.visit(this))
			return;
		visitor.pushVertex(this);
		for (Vertex affectedFile : affectedFiles) {
			affectedFile.accept(visitor);
		}
		visitor.popVertex(this);
	}

	/**
	 * Parcours similaire à la méthode accept(IPreNeededFilesVisitor) sauf que l'on traite le sommet avant ses
	 * fichiers requis. Ceci est très utile par exemple pour déterminer les dépendances.<br>
	 * <br>
	 * Cette méthode empile le sommet à la liste des sommets en cours de visite du visiteur avant de visiter
	 * les dépendances afin qu'il puisse détecter les cycles. Le sommet est dépilé à la fin de la visite des
	 * dépendances.<br>
	 * 
	 * @param visitor
	 *            le visiteur ayant besoin de traiter un sommet avant ses dépendances.
	 */
	public void accept(IPostNeededFilesVisitor visitor) {
		if (!visitor.visit(this))
			return;
		visitor.pushVertex(this);
		for (Vertex neededFile : neededFiles) {
			neededFile.accept(visitor);
		}
		visitor.popVertex(this);
	}

	/**
	 * @return le chemin complet vers l'exécutable relatif au projet, <code>null</code> si le fichier ne
	 *         doit pas être un exécutable.<br>
	 */
	public String getExeName() {
		if (exeName == null)
			return null;
		else
			return file.getProjectRelativePath().removeLastSegments(1).addTrailingSeparator().toOSString()
					+ this.exeName;
	}

	/**
	 * Pour récupérer le fichier (type IFile) associé au sommet
	 * 
	 * @return le fichier associé
	 */
	public IFile getFile() {
		return file;
	}

	/**
	 * Pour récupérer le graphe auquel est rattaché le sommet
	 * 
	 * @return le graphe auquel est rattaché le sommet
	 */
	public LayersGraph getGraph() {
		return graph;
	}

	/**
	 * Change le graphe auquel est affecté le sommet
	 * 
	 * @param graph
	 *            le nouveau graphe auquel doit etre rattaché le sommet.<br>
	 *            <br>
	 *            NOTE : ceci ne change pas le sommet de graphe.
	 */
	public void setGraph(LayersGraph graph) {
		this.graph = graph;
	}

	/**
	 * Ajoute un fichier requis à la liste uniquement s'il n'y est pas déjà.<br>
	 * 
	 * @param newFile
	 *            fichier à ajouter à la liste
	 */
	public void addNeededFile(Vertex newFile) {
		if (!neededFiles.contains(newFile)) {
			neededFiles.add(newFile);
		}
	}

	/**
	 * Enlève un fichier à la liste des fichiers requis de ce fichier.<br>
	 * Attention : ceci n'enlève pas ce fichier des fichiers affectés du fichier requis passé en paramètre.<br>
	 * 
	 * @param fileToRemove
	 *            fichier à retirer
	 * @return true si le fichier à été retiré, false sinon
	 */
	public boolean removeNeededFile(Vertex fileToRemove) {
		return neededFiles.remove(fileToRemove);
	}

	/**
	 * Ajoute un fichier affecté à la liste uniquement s'il n'y est pas déjà.<br>
	 * 
	 * @param newFile
	 *            fichier à ajouter à la liste.
	 */
	public void addAffectedFile(Vertex newFile) {
		if (!affectedFiles.contains(newFile)) {
			affectedFiles.add(newFile);
		}
	}

	/**
	 * Enlève un fichier à la liste des fichiers affectés de ce fichier.<br>
	 * Attention : ceci n'enlève pas ce fichier des fichiers requis du fichier affecté passé en paramètre.<br>
	 * 
	 * @param fileToRemove
	 *            fichier à retirer
	 * @return true si tout s'est bien passé, false sinon.
	 */
	public boolean removeAffectedFile(Vertex fileToRemove) {
		if (!affectedFiles.remove(fileToRemove)) {
			return false;
		}
		// Si on est un fichier externe et que l'on est plus utilisé, s'enlever
		// du graphe, et donc s'effacer de externalFiles
		// Attention : lorsque l'on supprime un sommet, on enlève tous ces
		// fichiers affectés, on peut donc arriver ici lors de la suppression et
		// on demande de recommencer, ceci ne va pas marcher, il faut donc
		// regarder si l'on est pas déjà en train d'etre supprimé.
		if (isExternalFile() && (affectedFiles.size() == 0)) {
			return graph.suppressVertex(this);
		}
		return true;
	}

	/**
	 * Ajouter un exécutable à la liste uniquement s'il n'est pas déjà dedans.<br>
	 * 
	 * @param exe
	 *            l'exécutable à rajouter.<br>
	 */
	public void addAffectedExe(Vertex exe) {
		if (!affectedExe.contains(exe)) {
			affectedExe.add(exe);
		}
	}

	/**
	 * Enlève un exécutable de la liste.<br>
	 * 
	 * @param exe
	 *            l'exécutable à enlever
	 * @return false si l'exécutable n'a pas été trouvé, true sinon.
	 */
	public boolean removeAffectedExe(Vertex exe) {
		return affectedExe.remove(exe);
	}

	/**
	 * Ajouter un fichier à lier à la liste uniquement s'il n'est pas déjà dedans.<br>
	 * 
	 * @param file
	 *            le fichier à lier à rajouter.<br>
	 */
	public void addFileToLink(Vertex file) {
		if (!allFilesToLink.contains(file)) {
			allFilesToLink.add(file);
		}
	}

	/**
	 * Enlève un fichier de la liste des fichiers à lier.<br>
	 * 
	 * @param file
	 *            le fichier à enlever
	 * @return false si lle fichier n'a pas été trouvé, true sinon.
	 */
	public boolean removeFileToLink(Vertex file) {
		return allFilesToLink.remove(file);
	}

	/**
	 * Supprimer le sommet de la hierarchie des dépendances.<br>
	 * Après l'appel de cette méthode, ce sommet n'a plus aucun lien avec aucun autre, mais il est toujours
	 * dans le graphe.<br>
	 * Note : cette méthode doit donc être appelée par le graphe uniquement.<br>
	 * <br>
	 * 
	 * @return true si tout s'est bien passé, false sinon.
	 */
	public boolean suppress() {
		// Supprimer toutes nos dépendances
		for (Vertex neededVertex : neededFiles) {
			// Se supprimer de la liste des fichiers affectés
			if (!neededVertex.removeAffectedFile(this)) {
				return false;
			}
		}
		// Supprimer tous nos fichiers affectés
		for (Vertex affectedVertex : affectedFiles) {
			// Se supprimer de leur liste de fichier requis
			if (!affectedVertex.removeNeededFile(this)) {
				return false;
			}
		}
		// Supprimer les exécutables
		for (Vertex exe : affectedExe) {
			// Se supprimer de leurs fichiers à lier
			if (!exe.removeFileToLink(this)) {
				return false;
			}
		}
		// Supprimer les fichiers à lier
		for (Vertex file : allFilesToLink) {
			// Se supprimer de leur liste d'exécutables
			if (!file.removeAffectedExe(this)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Pour récupérer la liste des fichiers affectés
	 * 
	 * @return la liste des fichiers affectés
	 */
	public List<Vertex> getAffectedFiles() {
		return affectedFiles;
	}

	/**
	 * Pour récupérer la liste des fichiers requis
	 * 
	 * @return la liste des fichiers requis
	 */
	public List<Vertex> getNeededFiles() {
		return neededFiles;
	}

	/**
	 * @return la liste des exécutables affectés.
	 */
	public List<Vertex> getAffectedExe() {
		return affectedExe;
	}

	/**
	 * @return la liste des fichiers à lier.
	 */
	public List<Vertex> getAllFilesToLink() {
		return allFilesToLink;
	}

	/**
	 * Change la couche à laquelle appartient le sommet.<br>
	 * Cette méthode ne doit être appelée que par la méthode moveVertice du graphe
	 * 
	 * @param layer
	 *            le nouveau numéro de couche
	 */
	public void setLayer(int layer) {
		this.layer = layer;
	}

	/**
	 * @return le sommet associé au fichier ml correspondant, <code>null</code> s'il ne le trouve pas où si
	 *         on est pas sur un mli.
	 */
	public Vertex getMLVertex() {
		if (getType() != Vertex.MLITYPE) {
			OcamlPlugin.logError("error in Vertex:getMLVertex : not a mli file");
			return null;
		}
		for (Vertex affectedVertex : affectedFiles) {
			if ((affectedVertex.getType() == Vertex.MLTYPE)
					&& (affectedVertex.getFile().getFullPath().removeFileExtension().addFileExtension("mli"))
							.equals(getFile().getFullPath())) {
				return affectedVertex;
			}
		}
		return null;
	}

	/**
	 * @return le sommet associé au fichier mli correspondant, <code>null</code> s'il ne le trouve pas où si
	 *         on est pas sur un ml.
	 */
	public Vertex getMLIVertex() {
		if (getType() != Vertex.MLTYPE) {
			OcamlPlugin.logError("error in Vertex:getMLIVertex : not a ml file");
			return null;
		}
		for (Vertex neededVertex : neededFiles) {
			if ((neededVertex.getType() == Vertex.MLITYPE)
					&& (neededVertex.getFile().getFullPath().removeFileExtension().addFileExtension("ml"))
							.equals(getFile().getFullPath())) {
				return neededVertex;
			}
		}
		return null;
	}

	/**
	 * @return une liste ordonnée de fichiers objets externes au projet
	 */
	public List<String> getExternalObjectFiles() {
		return OcamlBuilder.getExternalObjectFiles(this.file);
	}

}
