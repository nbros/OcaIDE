package ocaml.build.makefile;

import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;

/**
 * Read and write the makefile targets list (as a string) in the project persistent properties.
 */
public class MakefileTargets {

	public static final String MAKEFILE_TARGETS = "ocaml_makefile_targets";

	public static final String MAKEFILE_CLEAN_TARGETS = "ocaml_makefile_clean_targets";

	public static final String MAKEFILE_DOC_TARGETS = "ocaml_makefile_doc_targets";

	private final IProject project;

	private final String SEPARATOR = "\u00a0";

	public MakefileTargets(IProject project) {
		this.project = project;
	}

	/** Write the main targets (those used to build a project) */
	public void setTargets(String[] targets) {
		StringBuilder builder = new StringBuilder();
		for (String target : targets)
			builder.append(target.trim() + SEPARATOR);

		Misc.setProjectProperty(project, MAKEFILE_TARGETS, builder.toString());
	}

	/** Read the main targets */
	public String[] getTargets() {
		String strTargets = Misc.getProjectProperty(project, MAKEFILE_TARGETS);
		if (strTargets.equals(""))
			strTargets = "all";

		String[] targets = strTargets.split(SEPARATOR);

		String[] result = new String[targets.length];

		for (int i = 0; i < targets.length; i++)
			result[i] = targets[i].trim();

		return result;
	}

	/** Write the "clean" targets (called when cleaning this project) */
	public void setCleanTargets(String[] targets) {
		StringBuilder builder = new StringBuilder();
		for (String target : targets)
			builder.append(target.trim() + SEPARATOR);

		Misc.setProjectProperty(project, MAKEFILE_CLEAN_TARGETS, builder.toString());
	}

	/** Read the "clean" targets (called when cleaning this project) */
	public String[] getCleanTargets() {
		String strTargets = Misc.getProjectProperty(project, MAKEFILE_CLEAN_TARGETS);
		if (strTargets.equals(""))
			strTargets = "clean";

		String[] targets = strTargets.split(SEPARATOR);

		String[] result = new String[targets.length];

		for (int i = 0; i < targets.length; i++)
			result[i] = targets[i].trim();

		return result;
	}

	/** Write the targets used to generate documentation */
	public void setDocTargets(String[] targets) {
		StringBuilder builder = new StringBuilder();
		for (String target : targets)
			builder.append(target.trim() + SEPARATOR);

		Misc.setProjectProperty(project, MAKEFILE_DOC_TARGETS, builder.toString());
	}

	/** Read the targets used to generate documentation */
	public String[] getDocTargets() {
		String strTargets = Misc.getProjectProperty(project, MAKEFILE_DOC_TARGETS);
		if (strTargets.equals(""))
			strTargets = "htdoc";

		String[] targets = strTargets.split(SEPARATOR);

		String[] result = new String[targets.length];

		for (int i = 0; i < targets.length; i++)
			result[i] = targets[i].trim();

		return result;
	}
}
