package ocaml.editor.templates;

import java.io.IOException;

import ocaml.OcamlPlugin;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

public class OcamlTemplateAccess {
	/** Key to store custom templates. */
	private static final String CUSTOM_TEMPLATES_KEY = "ocaml.editors.templates.customtemplates";

	/** The shared instance. */
	private static OcamlTemplateAccess instance;

	/** The template store. */
	private TemplateStore fStore;

	/** The context type registry. */
	private ContributionContextTypeRegistry fRegistry;

	private OcamlTemplateAccess() {
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static OcamlTemplateAccess getDefault() {
		if (instance == null) {
			instance = new OcamlTemplateAccess();
		}
		return instance;
	}

	/**
	 * Returns this plug-in's template store.
	 * 
	 * @return the template store of this plug-in instance
	 */
	public TemplateStore getTemplateStore() {
		if (fStore == null) {
			fStore = new ContributionTemplateStore(getContextTypeRegistry(), OcamlPlugin
					.getInstance().getPreferenceStore(), CUSTOM_TEMPLATES_KEY);
			try {
				fStore.load();
			} catch (IOException e) {
				OcamlPlugin.logError(e);
			}
		}
		return fStore;
	}

	/**
	 * Returns this plug-in's context type registry.
	 * 
	 * @return the context type registry for this plug-in instance
	 */
	public ContextTypeRegistry getContextTypeRegistry() {
		if (fRegistry == null) {
			// create and configure the contexts available in the template editor
			fRegistry = new ContributionContextTypeRegistry();
			fRegistry.addContextType(OcamlFileContextType.OCAML_FILE_CONTEXT_TYPE);
		}
		return fRegistry;
	}
}