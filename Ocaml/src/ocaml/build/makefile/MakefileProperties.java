package ocaml.build.makefile;

import ocaml.properties.OcamlProjectPropertiesSerialization;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * Read and write the makefile targets list (as a string) in the project persistent properties.
 */
public class MakefileProperties {

	public static final String SECTION = "MakefileSettings";
	
	public static final String MAKEFILE_TARGETS = "ocaml_makefile_targets";

	public static final String MAKEFILE_CLEAN_TARGETS = "ocaml_makefile_clean_targets";

	public static final String MAKEFILE_DOC_TARGETS = "ocaml_makefile_doc_targets";

	public static final String MAKE_VARIANT = "ocaml_make_variant";
	
	public static final String MAKE_OPTIONS = "ocaml_make_options";

	public static enum Variants {
		GNU_MAKE, OMAKE;
		public String getName() {
			switch (this) {
			case OMAKE:
				return "omake";
			default:
				return "make";
			}
		};
	}

	
	private IDialogSettings settings;

	private OcamlProjectPropertiesSerialization propertiesSerialization;

	public MakefileProperties(IProject project) {
		propertiesSerialization = new OcamlProjectPropertiesSerialization(project);
		settings = propertiesSerialization.load(SECTION);
	}

	/** Write the main targets (those used to build a project) */
	public void setTargets(String[] targets) {
		settings.put(MAKEFILE_TARGETS, targets);
	}

	/** Read the main targets */
	public String[] getTargets() {
		return getArrayFromSettings(MAKEFILE_TARGETS);
	}

	/** Write the "clean" targets (called when cleaning this project) */
	public void setCleanTargets(String[] targets) {
		settings.put(MAKEFILE_CLEAN_TARGETS, targets);
	}

	/** Read the "clean" targets (called when cleaning this project) */
	public String[] getCleanTargets() {
		return getArrayFromSettings(MAKEFILE_CLEAN_TARGETS);
	}

	/** Write the targets used to generate documentation */
	public void setDocTargets(String[] targets) {
		settings.put(MAKEFILE_DOC_TARGETS, targets);
	}

	/** Read the targets used to generate documentation */
	public String[] getDocTargets() {
		return getArrayFromSettings(MAKEFILE_DOC_TARGETS);
	}
	
	/** Write make variant */
	public void setVariant(Variants variant) {
		settings.put(MAKE_VARIANT, variant.name());
	}

	/** Read make variant */
	public Variants getVariant() {
		String strType = settings.get(MAKE_VARIANT);
		if(strType == null)
			return Variants.GNU_MAKE;
		return Variants.valueOf(strType);
	}

	/** Write make options */
	public void setOptions(String[] options) {
		settings.put(MAKE_OPTIONS, options);
	}

	/** Read make options */
	public String[] getOptions() {
		return getArrayFromSettings(MAKE_OPTIONS);
	}
	
	private String[] getArrayFromSettings(String key) {
		String[] result = settings.getArray(key);
		if (result == null) {
			return new String[0];
		} else if (result.length == 1 && result[0].isEmpty()) {
			return new String[0];
		} else {
			return result;
		}
	}
	
	public void save() {
		propertiesSerialization.save();
	}
}
