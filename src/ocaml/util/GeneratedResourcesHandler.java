package ocaml.util;

import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.build.OcamlBuilder;
import ocaml.natures.OcamlNature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Cette classe gère les fichiers générés automatiquement.<br>
 * Précisément, elle détecte les fichiers générés lors de la compilation, leur met l'attribut
 * IS_GEN, et enlève cet attribut si jamais un utilisateur vient à modifier un fichier généré
 * automatiquement.<br>
 * 
 */
public class GeneratedResourcesHandler implements IResourceChangeListener {

	/**
	 * ce sont les ressources générées automatiquement.
	 */
	private ArrayList<IResource> generatedResources;

	/**
	 * Constructeur par défaut.<br>
	 * Initialise le tableau des ressources générées automatiquement.<br>
	 * 
	 */
	public GeneratedResourcesHandler() {
		this.generatedResources = new ArrayList<IResource>();
	}

	/**
	 * Ajoute une ressource à la liste, uniquement si elle n'y est pas déjà.<br>
	 * 
	 * @param resource
	 *            ressource à ajouter
	 */
	public void addGeneratedResource(IResource resource) {
		if (!generatedResources.contains(resource))
			generatedResources.add(resource);
	}

	/**
	 * Associe la propriété IS_GEN à toutes les ressources générées pendant la compilation.<br>
	 * Note : elle effectue un rafraichissement pour chaque fichier (Inévitable).
	 * 
	 * @return true si tout s'est bien passé, false sinon
	 * @throws CoreException
	 *             S'il y a une erreur lors d'un rafraichissement.
	 */
	public boolean setIS_GENProperty() throws CoreException {
		// Traiter chaque ressource
		for (IResource resource : generatedResources) {
			// Rafraichir la ressource
			resource.refreshLocal(IResource.DEPTH_ZERO, null);
			// Si la ressource existe
			if (resource.exists()) {

				// Le nom qualifié de la propriété IS_GEN
				final QualifiedName isGenQualName = new QualifiedName(OcamlPlugin.QUALIFIER,
						OcamlBuilder.IS_GEN);
				// Note : Ceci met la propriété également aux répertoires
				// générés automatiquement.
				resource.setPersistentProperty(isGenQualName, "true");
			}
			// la ressource n'existe pas
			else {
				return false;
			}
		}
		return true;
	}

	/**
	 * méthode appelée lorsqu'un changement est détecté. On aimerait ne détecter que les changements
	 * qui surviennent lors de la compilation, mais le problème est qu'un rafraichissement du
	 * système de fichiers provoque un évènement quelques instants après, juste le temps que la
	 * compilation se termine.
	 * 
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		// Récupérer les infos

		// Debug
		// final Object eventSource = event.getSource();
		// final int buildKind = event.getBuildKind();
		final int eventType = event.getType();

		final IResourceDelta delta = event.getDelta();

		// On écoute les évènements POST_CHANGE pour détecter les
		// changements effectués par l'utilisateur sur les fichiers mli
		// générés automatiquement.
		// Attention, ne rien faire sur un POST_CHANGE survenu après un
		// POST_BUILD car il contient les modifications survenues lors de la
		// compilation (ce POST_CHANGE survient apparament si et seulement si il
		// y a des changements lors de la compilation)
		if (eventType == IResourceChangeEvent.POST_CHANGE) {
			if (changesInBuilding) {
				changesInBuilding = false;
			} else {
				try {
					delta.accept(new UserChangedGeneratedFilesHandler());
				} catch (CoreException e) {
					OcamlPlugin.logError("error when trying to remove" + " IS_GEN property", e);
				}
			}
		}
		// On écoute les évènement PRE_BUILD uniquement pour récupérer le
		// delta afin de faire la différence lors de POST_BUILD
		else if (eventType == IResourceChangeEvent.PRE_BUILD) {

			// Il faut parcourir tout le delta, et vérifier si la ressource
			// associée à un changement ou à un ajout existe bien (si l'on
			// supprime un fichier pendant que le projet est fermé, à
			// l'ouverture, il va toujours considérer que ce fichier est ajouté)
			this.deltaPreBuild = new ArrayList<IResourceDelta>();
			try {
				delta.accept(new IResourceDeltaVisitor() {

					public boolean visit(IResourceDelta delta) throws CoreException {

						final IResource resource = delta.getResource();

						// Se protéger des projets fermés
						if (resource.getType() == IResource.PROJECT
								&& !((IProject) resource).isOpen())
							return false;

						// Eviter les dossiers de sources externes
						if (resource != null && resource.exists()
								&& resource.getName().equals(OcamlBuilder.EXTERNALFILES))
							return false;

						// Si le changement correspond à un ajout ou une
						// modification (sous entendu, pas une suppression)
						if ((delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.CHANGED)
								&& (resource != null)) {
							// On est obligé de rafraichir le système de fichier
							// si des fichiers ont été supprimés par un
							// mécanisme autre que celui d'eclipse (typiquement,
							// quand on efface tous les fichiers générés
							// automatiquement pendant qu'un projet se ferme)
							resource.refreshLocal(IResource.DEPTH_ZERO, null);
							if (resource.exists() && resource.getType() == IResource.FILE) {
								// La resource doit exister réellement pour
								// être prise en compte
								deltaPreBuild.add(delta);

							}
						} else
							// On garde tous les autres cas
							deltaPreBuild.add(delta);
						return true;
					}

				});
			} catch (CoreException e) {
				OcamlPlugin.logError("error saving changements before compilation", e);
			}

		}
		// Lors d'un POST_BUILD, on va visiter tous les fichiers modifiés
		// depuis le dernier POST_BUILD et on ne va retenir que les
		// différences avec le delta du PRE_BUILD. (Ces différences
		// correspondent a ce qui s'est passé strictement pendant la
		// compilation.)
		else if (eventType == IResourceChangeEvent.POST_BUILD) {

			// faire visiter les fichiers par un visiteur adéquat
			try {
				delta.accept(new NewFilesDeltaVisitor());

				// Et réinitialiser le delta correspondant à PRE_BUILD.
				this.deltaPreBuild = null;
				// Puis mettre la propriété
				if (!setIS_GENProperty()) {
					// Signaler une erreur s'il y a eu un problème
					OcamlPlugin.logError("error in GeneratedResourcesHandler:"
							+ "finish: return's value false");
				}
			} catch (CoreException e) {
				OcamlPlugin.logError("error when trying to set " + "IS_GEN property ", e);
			}
			// Dans tous les cas, réinitialiser la liste.
			generatedResources.clear();

		}
		// Avant de fermer un projet, on supprime tout ce qui a été généré
		// automatiquement.
		else if (eventType == IResourceChangeEvent.PRE_CLOSE) {
			// Un évènement close arrive forcément sur un projet
			final IProject project = (IProject) event.getResource();
			if (project == null)
				OcamlPlugin.logError("error  in GeneratedResourcesHandler:"
						+ "resourceChanged: project is null", new NullPointerException());
			final List<IFile> filesToDelete = new ArrayList<IFile>();
			// Parcourir le projet pour trouver tous les fichiers générés
			// automatiquement
			try {
				project.accept(new IResourceVisitor() {

					public boolean visit(IResource resource) throws CoreException {
						// Même règles pour CleanVisitor. On ne peut pas
						// l'utiliser car on ne peut pas modifier le workspace
						// pour l'instant
						if (!resource.exists() || resource.isLinked())
							return false;

						if ((resource.getType() == IResource.FILE)
								&& Misc.isGeneratedFile((IFile) resource))
							filesToDelete.add((IFile) resource);
						return true;
					}

				});
			} catch (CoreException e) {
				OcamlPlugin.logError("error when cleaning closed project", e);
			}
			// Lancer l'effacement des ressources en arrière plan
			Job job = new Job("cleaning closed project") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					for (IFile file : filesToDelete) {
						// file.delete(true,monitor) ne marche pas (surement
						// parce que le projet est fermé).
						monitor.subTask("deleting " + file.getProjectRelativePath());
						// On efface directement via java.io.File
						new java.io.File(file.getLocation().toOSString()).delete();
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT);
			job.setUser(true);
			// Attendre une demi seconde sinon il y a un ralentissement
			job.schedule(500);
		}
	}

	/**
	 * Un moyen de se souvenir des changements avant le début de la compilation afin de pouvoir
	 * faire la différence et détecter les fichiers générés automatiquement.
	 */
	private List<IResourceDelta> deltaPreBuild = null;

	/**
	 * Afin de savoir s'il a des ressources modifiées pendant la compilation.
	 */
	private boolean changesInBuilding = false;

	/**
	 * Classe interne représentant un visiteur pour les fichiers modifiés durant la compilation.<br>
	 * Ca parait assez peu pertinent de créer un visiteur en tant que classe interne de cette
	 * manière mais on a pas d'autre manière d'explorer facilement un delta...
	 * 
	 */
	protected class NewFilesDeltaVisitor implements IResourceDeltaVisitor {

		/**
		 * parcours les ressources modifiées et ajoute à la liste des fichiers qui doivent recevoir
		 * la propriété IS_GEN uniquement si la ressource a été ajoutée ou remplacée lors de la
		 * compilation.<br>
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {

			// La ressource associée
			final IResource resource = delta.getResource();

			// Passer sur la racine du workspace
			if (resource.getType() == IResource.ROOT)
				return true;
			// N'examiner que les ressources existantes.
			if (!resource.exists()) {
				return false;
			}

			// Se protéger des projets fermés ou n'ayant pas la bonne nature
			final IProject project = resource.getProject();
			if (project == null || !project.isOpen() || project.getNature(OcamlNature.ID) == null)
				return false;

			// Ignorer les EXTERNALFILES
			if ((project != null && project.isOpen() && project.getNature(OcamlNature.ID) == null)
					|| resource.getName().equals(OcamlBuilder.EXTERNALFILES)) {
				return false;
			}

			// Ne garder que les fichiers
			if (resource.getType() != IResource.FILE)
				// Il faut renvoyer true, car si l'on est sur un répertoire, on
				// veut aller visiter ses membres.
				return true;

			// Ignorer le fichier ".paths"
			if (resource.getName().equals(OcamlPaths.PATHS_FILE))
				return false;

			// Regarder en premier si la ressource était dans le deltaPreBuild
			// avec le même kind, auquel cas elle ne correspond pas à quelque
			// chose qui s'est passé pendant la compilation.
			if (deltaPreBuild != null) {
				for (IResourceDelta deltaInPreBuild : deltaPreBuild) {
					if (deltaInPreBuild.getResource().equals(resource)
							&& deltaInPreBuild.getKind() == delta.getKind())
						return true;
				}
			}

			// Si on arrive ici c'est qu'une ressource a été modifiée lors de la
			// compilation.
			// Note mieux vaut un test qu'une affectation du booléen à chaque
			// fois ?
			if (!changesInBuilding) {
				changesInBuilding = true;
			}

			// Si c'est un ajout ou alors remplacement (suppression puis ajout
			// au cours de la compilation)
			if ((delta.getKind() == IResourceDelta.ADDED)
					|| ((delta.getFlags() & IResourceDelta.REPLACED) != 0)) {
				// Ajouter aux fichiers générés
				generatedResources.add(resource);

			}
			return true;
		}

	}

	/**
	 * Un visiteur pour les mli générés automatiquement et modifiés par l'utilisateur.<br>
	 * Voir les remarques pour {@link NewFilesDeltaVisitor}
	 * 
	 */
	protected class UserChangedGeneratedFilesHandler implements IResourceDeltaVisitor {

		public boolean visit(IResourceDelta delta) throws CoreException {

			// Si le delta correspond à une suppression, on ne fait rien bien
			// sur.
			if (delta.getKind() == IResourceDelta.REMOVED) {
				return true;
			}

			final IResource resource = delta.getResource();
			final int resourceType = resource.getType();

			// Si on est sur un projet fermé, arrêter !
			if ((resourceType == IResource.PROJECT) && !(((IProject) resource).isOpen())) {
				return false;
			}

			// Ignorer les ressources non existantes ou ayant l'attribut
			// EXTERNAL_SOURCES_FOLDER
			if (!resource.exists()) {
				return false;
			}

			// On s'interesse à tout fichier généré automatiquement.
			if ((resourceType != IResource.FILE) || (!Misc.isGeneratedFile((IFile) resource))) {
				return true;
			}

			// On veut intervenir pour toute modification, mais ni suppression,
			// ni ajout.
			// On retient donc les delta de type 4 (changed)
			if (delta.getKind() == IResourceDelta.CHANGED) {
				// Il n'y a qu'a enlever la propriété. Lors de la prochaine
				// compilation, ce fichier sera pris en compte mais pas
				// forcément le ml correspondant, il faut donc forcer ceci
				// dans SuitableFilesDeltaFinder.
				Misc.setFileProperty((IFile) resource, OcamlBuilder.IS_GEN, "false");

				// Mettre à jour le decoratorManager.
				Misc.updateDecoratorManager();
			}
			return true;
		}

	}
}
