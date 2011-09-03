package ocaml.util;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * This is used to test the property "is this resource a file in an OCaml managed project?". This property is
 * used by plugin.xml to choose what property pages to display.
 */
public class HasOcamlNaturePropertyTester extends org.eclipse.core.expressions.PropertyTester {

	public HasOcamlNaturePropertyTester() {
	}

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals("isInNature")) {
			if (receiver instanceof IFile && expectedValue instanceof String) {
				IFile file = (IFile) receiver;
				String nature = (String) expectedValue;
				try {
					if (file.getProject().getNature(nature) != null)
						return true;
				} catch (CoreException e) {
					OcamlPlugin.logError("ocaml plugin error", e);
					return false;
				}

			}
		}
		return false;
	}

}
