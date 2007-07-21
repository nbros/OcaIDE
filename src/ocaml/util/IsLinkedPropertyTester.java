package ocaml.util;


import ocaml.OcamlPlugin;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;

/**
 * This test whether a file is "linked" (in Eclipse terminology).
 */

public class IsLinkedPropertyTester extends PropertyTester {
	
	public IsLinkedPropertyTester() {
	}

	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		final IFile file = (IFile) receiver;

		if ("isLinkedFile".equals(property)) {
			return file.isLinked();
		}
		else
			OcamlPlugin.logError("IsLinkedPropertyTester : unknown property (" + property + ")");

		return false;
	}

}
