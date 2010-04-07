package ocaml.build.makefile;

import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;

public class MakeUtility {
	public static final String MAKE_VARIANT = "ocaml_make_variant";
	
	public static final String MAKE_OPTIONS = "ocaml_make_options";

	private final String SEPARATOR = "\u00a0";

	public static enum Variants {
		GNU_MAKE, OMAKE
	}

	private final IProject project;

	public MakeUtility(IProject project) {
		this.project = project;
	}

	/** Printable name */
	public static String getName(Variants variant) {
		switch (variant) {
		case OMAKE: return "omake";
		default: return "make";
		}
	}

	/** Write make variant */
	public void setVariant(Variants variant) {
		Misc.setProjectProperty(project, MAKE_VARIANT, getName(variant));
	}

	/** Read make variant */
	public Variants getVariant() {
		String strType = Misc.getProjectProperty(project, MAKE_VARIANT);
		if (strType.equals("omake")) return Variants.OMAKE;
		else return Variants.GNU_MAKE;
	}

	/** Write make options */
	public void setOptions(String[] options) {
		StringBuilder builder = new StringBuilder();
		for (String option : options)
			builder.append(option.trim() + SEPARATOR);

		Misc.setProjectProperty(project, MAKE_OPTIONS, builder.toString());
	}

	/** Read make options */
	public String[] getOptions() {
		String strOptions = Misc.getProjectProperty(project, MAKE_OPTIONS);
		String[] options = strOptions.split(SEPARATOR);

		String[] result = new String[options.length];

		for (int i = 0; i < options.length; i++)
			result[i] = options[i].trim();

		return result;
	}
}
