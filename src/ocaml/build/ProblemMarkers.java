package ocaml.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * Parse error messages returned by the O'Caml compiler, and create error markers for the corresponding
 * resources in the project.
 */
public class ProblemMarkers {

	private final IProject project;

	private boolean bErrorsFound;

	private boolean bWarningsFound;

	private boolean bProjectErrorsFound;

	private boolean bProjectWarningsFound;

	private final TreeSet<ErrorFile> errorFiles = new TreeSet<ErrorFile>();

	private final TreeSet<WarningFile> warningFiles = new TreeSet<WarningFile>();

	public ProblemMarkers(IProject project) {
		this.project = project;
		bErrorsFound = false;
		bWarningsFound = false;
		bProjectErrorsFound = false;
		bProjectWarningsFound = false;
	}

	private final Pattern patternErrorMessages = Pattern.compile("^(?:" + "Values do not match|"
			+ "Type declarations do not match|" + "Exception declarations do not match|"
			+ "Modules do not match|" + "Module type declarations do not match|"
			+ "Illegal permutation of structure fields|" + "The implementation |"
			+ "Class type declarations do not match|" + "Class declarations do not match|"
			+ "Unbound module type )");

	/** regex: File [f] line [l], characters [a]-[b]: */
	private final Pattern patternFile = Pattern
			.compile("^File \"(.+?)\", line (\\d+), characters (\\d+)-(\\d+):");

	/** Create the markers for error messages coming from the O'Caml compiler. */
	public void makeMarkers(String compilerOutput) {

		String[] lines = compilerOutput.split("\\r?\\n");
		String currentMessage = "";
		int charStart = 0;
		int charEnd = 0;
		int lineNumber = 0;
		String filename = "";

		for (String line : lines) {

			Matcher matcher = patternFile.matcher(line);
			if (matcher.find()) {

				if (!currentMessage.equals("")) {
					writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage.trim());
					currentMessage = "";
				}

				filename = matcher.group(1);
				lineNumber = Integer.parseInt(matcher.group(2));
				charStart = Integer.parseInt(matcher.group(3));
				charEnd = Integer.parseInt(matcher.group(4));

				/* if the start and end positions are equal, the marker doesn't appear */
				if (charStart == charEnd && charStart > 0)
					charStart--;
			} else if (line.startsWith("Error while linking") && (!currentMessage.equals(""))) {
				writeProblemMarker("", 0, 0, 0, currentMessage.trim());
				currentMessage = line;
			} else {
				currentMessage = currentMessage + "\n" + line;
			}
		}

		currentMessage = currentMessage.trim();
		if (!currentMessage.equals(""))
			writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage);
	}

	/** Create error markers for the error messages returned by the make command */
	public void makeMarkers2(String makeOutput) {

		String[] lines = makeOutput.split("\\r?\\n");
		String currentMessage = "";
		int charStart = 0;
		int charEnd = 0;
		int lineNumber = 0;
		String filename = "";

		boolean skip = true;

		for (String line : lines) {
			if (line.matches("^-?(?:make|o?caml|rm |mkdir |cp |mv |cd |for |if |gcc |/|\\.).*")) {
				if (!currentMessage.trim().equals("")) {
					writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage.trim());
					currentMessage = "";
				}
				skip = true;
			}

			Matcher matcherError = patternErrorMessages.matcher(line);
			if (matcherError.find()) {
				skip = false;

				if (!currentMessage.trim().equals("")) {
					writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage.trim());
					currentMessage = "";
				}

				filename = "";
				lineNumber = 0;
				charStart = 0;
				charEnd = 0;
				currentMessage = line;
				continue;
			}

			Matcher matcher = patternFile.matcher(line);
			if (matcher.find()) {
				skip = false;

				if (!currentMessage.trim().equals("")) {
					writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage.trim());
					currentMessage = "";
				}
				filename = matcher.group(1);
				lineNumber = Integer.parseInt(matcher.group(2));
				charStart = Integer.parseInt(matcher.group(3));
				charEnd = Integer.parseInt(matcher.group(4));

			} else if (!skip) {
				currentMessage = currentMessage + "\n" + line;
			}
		}

		currentMessage = currentMessage.trim();
		if (!currentMessage.equals(""))
			writeProblemMarker(filename, lineNumber, charStart, charEnd, currentMessage);
	}

	/**
	 * Write a problem marker for the file <code>fullProjectRelativeFilePath</code> that generated the error
	 * message <code>msg</code> on the line <code>lineNumber</code> that starts at character
	 * <code>charStart</code> and ends at character <code>charEnd</code>.
	 * 
	 */
	private void writeProblemMarker(String fullWorkspaceRelativePath, int lineNumber, int charStart,
			int charEnd, String msg) {

		String projectRelativePath;

		String projectPath = project.getName() + File.separatorChar;
		if (fullWorkspaceRelativePath.startsWith(projectPath))
			projectRelativePath = fullWorkspaceRelativePath.substring(projectPath.length());
		else
			projectRelativePath = fullWorkspaceRelativePath;

		// if the project relative path is empty, then this is the project itself
		if (projectRelativePath.equals("")) {
			try {
				// create a marker for the project
				IMarker m = project.createMarker(IMarker.PROBLEM);
				m.setAttribute(IMarker.MESSAGE, msg);
				m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				if (msg.startsWith("Warning")) {
					m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
					bProjectWarningsFound = true;
				} else /* if (msg.startsWith("Error")) */{
					m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					bProjectErrorsFound = true;
				}

			} catch (CoreException e) {
				OcamlPlugin.logError("ProblemMarkers:writeProblemMarkers", e);
			}
			return;
		}
		IResource fileAsResource = project.findMember(projectRelativePath);
		if ((fileAsResource == null) || (fileAsResource.getType() != IResource.FILE)) {
			/*
			 * If the resource wasn't found. This is certainly because the makefile changed the directory. So,
			 * we have to search for the file among all the project files.
			 */

			// only keep the filename
			int idx = fullWorkspaceRelativePath.lastIndexOf(File.separatorChar);
			if (idx != -1)
				fullWorkspaceRelativePath = fullWorkspaceRelativePath.substring(idx + 1);

			for (IFile file : Misc.getProjectFiles(project)) {
				if (file.getName().equals(fullWorkspaceRelativePath)) {
					fileAsResource = file;
					break;
				}
			}

			if (fileAsResource == null) {
				OcamlPlugin.logError("ProblemMarkers:writeProblemMarkers: can't find " + projectRelativePath);
				return;
			}
		}
		final IFile file = (IFile) fileAsResource;

		int lineOffset = getLineOffset(lineNumber, file);

		Hashtable<String, Integer> attributes = new Hashtable<String, Integer>();
		MarkerUtilities.setMessage(attributes, msg.toString());

		if (msg.startsWith("Warning")) {
			attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
			bWarningsFound = true;
			warningFiles.add(new WarningFile(file));
		} else /* if (msg.startsWith("Error")) */{
			attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
			bErrorsFound = true;
			errorFiles.add(new ErrorFile(file));
		}

		if (lineNumber > 0 && charStart >= 0 && charEnd >= charStart) {
			MarkerUtilities.setCharStart(attributes, lineOffset + charStart);
			MarkerUtilities.setCharEnd(attributes, lineOffset + charEnd);
			MarkerUtilities.setLineNumber(attributes, lineNumber);
		}
		try {
			MarkerUtilities.createMarker(file, attributes, IMarker.PROBLEM);
		} catch (CoreException e) {
			OcamlPlugin.logError("error in ProblemMarkers:writeProblemMarkers", e);
		}
	}

	/**
	 * Get the absolute position in <code>file</code>, in number of characters from the beginning of line
	 * <code>lineNumber</code>.
	 */
	private static int getLineOffset(int lineNumber, IFile file) {
		BufferedReader in = null;
		if (!file.exists()) {
			OcamlPlugin
					.logError("ProblemMarkesr:getLineOffset:" + file.getProjectRelativePath() + " does not exist"); //$NON-NLS-1$ //$NON-NLS-2$
			return 0;
		}
		try {
			in = new BufferedReader(new InputStreamReader(file.getContents()));
		} catch (CoreException e) {
			OcamlPlugin.logError("Error 1 in OcamlBuilder:getLineOffset()", e);
		}
		int ch = 0;
		int charCount = 0;
		int lineCount = 0;
		while ((lineCount < lineNumber - 1) && (ch != -1)) {
			try {
				ch = in.read();
			} catch (IOException e1) {
				OcamlPlugin.logError("Error in 2 OcamlBuilder:getLineOffset()", e1);
			}
			if (ch != -1) {
				charCount++;
				if (ch == '\n') {
					lineCount++;
				}
			}
		}
		return charCount;
	}

	public boolean errorsFound() {
		return bErrorsFound;
	}

	public boolean warningsFound() {
		return bWarningsFound;
	}

	public boolean projectErrorsFound() {
		return bProjectErrorsFound;
	}

	public boolean projectWarningsFound() {
		return bProjectWarningsFound;
	}

	public IFile[] getFilesWithErrors() {
		ArrayList<IFile> filesWithErrors = new ArrayList<IFile>();
		for (ErrorFile f : errorFiles)
			filesWithErrors.add(f.getFile());

		return filesWithErrors.toArray(new IFile[filesWithErrors.size()]);
	}

	/** Return the files that have at least a warning, but no error */
	public IFile[] getFilesWithWarnings() {
		ArrayList<IFile> filesWithWarnings = new ArrayList<IFile>();
		for (WarningFile f : warningFiles)
			if (!errorFiles.contains(f)) {
				filesWithWarnings.add(f.getFile());
			}

		return filesWithWarnings.toArray(new IFile[filesWithWarnings.size()]);
	}

	private class WarningFile implements Comparable {
		final IFile file;

		public WarningFile(IFile file) {
			this.file = file;
		}

		public IFile getFile() {
			return file;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof WarningFile) {
				return ((WarningFile) obj).file.getLocation().toOSString().equals(
						file.getLocation().toOSString());
			} else
				return super.equals(obj);
		}

		public int compareTo(Object o) {
			if (o instanceof WarningFile) {
				WarningFile file2 = (WarningFile) o;
				return file.getLocation().toOSString().compareTo(file2.file.getLocation().toOSString());
			} else if (o instanceof ErrorFile) {
				ErrorFile file2 = (ErrorFile) o;
				return file.getLocation().toOSString().compareTo(file2.file.getLocation().toOSString());
			} else
				throw new ClassCastException();

		}

	}

	private class ErrorFile implements Comparable {
		final IFile file;

		public ErrorFile(IFile file) {
			this.file = file;
		}

		public IFile getFile() {
			return file;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ErrorFile) {
				return ((ErrorFile) obj).file.getLocation().toOSString().equals(
						file.getLocation().toOSString());
			} else
				return super.equals(obj);
		}

		public int compareTo(Object o) {
			if (o instanceof ErrorFile) {
				ErrorFile file2 = (ErrorFile) o;
				return file.getLocation().toOSString().compareTo(file2.file.getLocation().toOSString());
			} else
				throw new ClassCastException();

		}

	}
}
