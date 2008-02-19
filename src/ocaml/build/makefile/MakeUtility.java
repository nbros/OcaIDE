package ocaml.build.makefile;

import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;

public class MakeUtility {
	public static final String MAKE_VARIANT = "ocaml_make_variant";

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
}
