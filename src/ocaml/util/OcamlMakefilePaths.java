package ocaml.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * Manages the paths used by the "make" command, for each makefile project
 */
public class OcamlMakefilePaths {

	public static final String PATHS_FILE = ".makefilepaths";

	private final IProject project;

	public OcamlMakefilePaths(IProject project) {
		this.project = project;
	}

	public void setPaths(String[] paths) {
		IPath pathsPath = project.getLocation().append(PATHS_FILE);

		File file = pathsPath.toFile();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));

			for (String path : paths)
				if (path != null)
					writer.write(path + "\n");

			writer.close();
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return;
		}
	}

	public void restoreDefaults() {
		HashSet<String> paths = new HashSet<String>();
		paths.add(".");

		// add the paths of all the tools (defined in the preferences)
		String[] strToolsPaths = { OcamlPlugin.getOcamlcFullPath(), OcamlPlugin.getOcamldebugFullPath(),
				OcamlPlugin.getOcamldepFullPath(), OcamlPlugin.getOcamldocFullPath(),
				OcamlPlugin.getOcamlFullPath(), OcamlPlugin.getOcamllexFullPath(),
				OcamlPlugin.getOcamloptFullPath(), OcamlPlugin.getOcamlyaccFullPath(), 
				OcamlPlugin.getMakeFullPath()};

		for (String toolPath : strToolsPaths) {
			IPath path = new Path(toolPath);
			path = path.removeLastSegments(1);
			paths.add(path.toOSString());
		}
		
		if(OcamlPlugin.runningOnLinuxCompatibleSystem()){
			File dir = new File("/bin");
			if(dir.exists())
				paths.add("/bin");
		}
		
		paths.add(OcamlPlugin.getLibFullPath());

		this.setPaths(paths.toArray(new String[paths.size()]));
	}

	public String[] getPaths() {
		ArrayList<String> paths = new ArrayList<String>();

		IPath pathsPath = project.getLocation().append(PATHS_FILE);

		File file = pathsPath.toFile();

		if (!file.exists())
			restoreDefaults();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));

			String path = "";
			while ((path = reader.readLine()) != null)
				paths.add(path);

			reader.close();

		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return new String[0];
		}

		return paths.toArray(new String[paths.size()]);
	}

	/** @return true if the path is valid in this project */
	public static boolean isValidPath(IProject project, String strPath) {
		if (strPath == null || strPath.equals(""))
			return false;

		if (isRelativePath(project, strPath))
			return true;

		File file = new File(strPath);
		return file.exists() && file.isDirectory();
	}

	/** @return true if this path is relative to the project */
	public static boolean isRelativePath(IProject project, String strPath) {
		IPath path = Path.fromOSString(strPath);
		path = path.makeRelative();

		IResource resource = project.findMember(path);

		if (resource != null) {
			if (resource instanceof IFolder) {
				IFolder folder = (IFolder) resource;
				if (folder.exists())
					return true;
			}
			if (resource instanceof IProject) {
				IProject proj = (IProject) resource;
				if (proj.exists())
					return true;
			}

		}

		return false;
	}
}
