package ocaml.editor.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

/**
 * A very simple context type.
 */
public class OcamlFileContextType extends TemplateContextType {

	/** This context's id */
	public static final String OCAML_FILE_CONTEXT_TYPE= "ocaml.editors.templates.contextType.ocamlFile";

	@Override
	public String getId() {
		return OCAML_FILE_CONTEXT_TYPE;
	}
	
	/**
	 * Creates a new XML context type. 
	 */
	public OcamlFileContextType() {
		addGlobalResolvers();
	}

	private void addGlobalResolvers() {
		addResolver(new GlobalTemplateVariables.Cursor());
		addResolver(new GlobalTemplateVariables.WordSelection());
		addResolver(new GlobalTemplateVariables.LineSelection());
		addResolver(new GlobalTemplateVariables.Dollar());
		addResolver(new GlobalTemplateVariables.Date());
		addResolver(new GlobalTemplateVariables.Year());
		addResolver(new GlobalTemplateVariables.Time());
		addResolver(new GlobalTemplateVariables.User());
	}
}
