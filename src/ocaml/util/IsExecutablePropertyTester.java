package ocaml.util;

import ocaml.OcamlPlugin;
import ocaml.exec.CommandRunner;

import org.eclipse.core.resources.IFile;

/**
 * This test the property: "is this file executable" by using the "file" command on Linux/Mac OS X, or by
 * testing the extension on Windows
 */
public class IsExecutablePropertyTester extends org.eclipse.core.expressions.PropertyTester {

	public IsExecutablePropertyTester() {
	}

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals("isExecutable")) {
			if (receiver instanceof IFile) {
				IFile file = (IFile) receiver;

				if (Misc.isOCamlSourceFile(file))
					return false;

				if (OcamlPlugin.runningOnLinuxCompatibleSystem()) {
					String[] command = new String[2];
					command[0] = "file";
					command[1] = file.getLocation().toOSString();
					CommandRunner commandRunner = new CommandRunner(command, "/");
					String output = commandRunner.getStdout();
					if (output.contains("ocamlrun script"))
						return true;
					if (output.contains("executable"))
						return true;
				} else
					return file.getName().endsWith(".exe");
			}
		}
		return false;
	}

}
