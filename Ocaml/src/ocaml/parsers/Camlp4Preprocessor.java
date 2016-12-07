package ocaml.parsers;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.exec.CommandRunner;
import ocaml.parser.Def;
import ocaml.util.Misc;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Preprocess a file with camlp4.
 * <p>
 * Look at the first comment in the file for a preprocessing directive that looks like:
 * <code>"(* pp: ___ *)"</code>
 * <p>
 * Example: -parser OcamlRevised <br>
 * Or: -parser Camlp4OcamlRevisedParser -parser Camlp4QuotationCommon -parser Camlp4OCamlRevisedQuotationExpander
 */

public class Camlp4Preprocessor {
	private Pattern patternPreprocess = Pattern.compile("\\A\\s*\\(\\*\\s*pp\\s*:(.*?)\\*\\)");
	private boolean bPreprocess;
	private Matcher matcherPreprocess;
	private String output;
	private String errorOutput;

	// private ArrayList<Camlp4Location> camlp4Locations;

	public Camlp4Preprocessor(String document) {
		matcherPreprocess = patternPreprocess.matcher(document);
		bPreprocess = matcherPreprocess.find();
	}

	public boolean mustPreprocess() {
		return bPreprocess;
	}

	/**
	 * Preprocess this file by using the preprocessing comment found in the document supplied in the
	 * constructor.
	 * 
	 * @return false if the operation was cancelled, false otherwise
	 */
	public boolean preprocess(File file, IProgressMonitor monitor) {

		if (monitor == null)
			monitor = new NullProgressMonitor();

		final String parentPath = file.getParent();
		final String filename = file.getName();

		// String path = editor.getProject().getLocation().toOSString();
		// String filepath =
		// editor.getFileBeingEdited().getLocation().toOSString();
		ArrayList<String> command = new ArrayList<String>();
		command.add(OcamlPlugin.getCamlp4FullPath());

		// if (strDocument.startsWith("(*pp:revised*)")) {
		// command.add("-parser");
		// command.add("OCamlRevised");
		// } else if (strDocument.startsWith("(*pp:quotations*)")) {
		// command.add("-parser");
		// command.add("Camlp4OcamlRevisedParser");
		// command.add("-parser");
		// command.add("Camlp4QuotationCommon");
		// command.add("-parser");
		// command.add("Camlp4OCamlRevisedQuotationExpander");
		// } else {
		// command.add("-parser");
		// command.add("Ocaml");
		// }

		String strParams = matcherPreprocess.group(1);
		String[] params = DebugPlugin.parseArguments(strParams);

		for (String param : params) {
			command.add(param);
		}

		command.add("-printer");
		command.add("Ocaml");
		command.add("-add_locations");
		command.add(filename);

		final String[] cmd = command.toArray(new String[command.size()]);

		class Camlp4Job extends Job {

			Camlp4Job(String name) {
				super(name);
			}

			public CommandRunner commandRunner = null;
			public String output;
			public String errorOutput;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// TODO set OCAMLLIB env var
				commandRunner = new CommandRunner(cmd, parentPath);
				commandRunner.getExitValue();
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;

				output = commandRunner.getStdout();
				errorOutput = commandRunner.getStderr();

				return Status.OK_STATUS;
			}
		}

		Camlp4Job job = new Camlp4Job("camlp4");
		job.setPriority(Job.DECORATE);
		job.schedule();

		while (job.getResult() == null) {
			if (monitor.isCanceled()) {
				if (job.commandRunner != null)
					job.commandRunner.kill();
				return false;
			}
			try {
				monitor.worked(1);
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		output = job.output;
		errorOutput = job.errorOutput;

		return true;
	}

	public String getErrorOutput() {
		if (errorOutput == null)
			return "";
		else
			return errorOutput;
	}

	public String getOutput() {
		if (output == null)
			return "";
		else
			return output;
	}

	/** A location found in camlp4 output when using the add_locations command-line switch */
	public class Camlp4Location {
		public Camlp4Location(int offset, int line, int colStart, int colEnd) {
			this.offset = offset;
			this.line = line;
			this.colStart = colStart;
			this.colEnd = colEnd;
		}

		/** the offset of the location in the camlp4 output */
		int offset;

		int line;
		int colStart;
		int colEnd;
	}

	Pattern patternLocation = Pattern
			.compile("\\(\\*loc\\: \\[\".*?\"\\: (\\d+)\\:(\\d+)\\-(\\d+) \\d+\\:\\d+\\]\\*\\)");

	/**
	 * Update the identifiers source code locations by reading the location comments left by camlp4
	 * (using the '-add_locations' command-line switch)
	 */
	public void associateCamlp4Locations(IDocument oldDocument, String strOldDocument,
			IDocument newDocument, ArrayList<Camlp4Location> camlp4Locations, Def def,
			IProgressMonitor monitor) {
		// (*loc: ["a.ml": 1:10-11 1:11]*)

		if (monitor == null)
			monitor = new NullProgressMonitor();

		if (monitor.isCanceled())
			return;

		if (def.type != Def.Type.Root) {

			IRegion region = def.getNameRegion(newDocument);
			// String doc = newDocument.get();

			int pos = region.getOffset() - 1;

			// parse whitespace
			// while (pos >= 0 && Character.isSpaceChar(doc.charAt(pos)))
			// pos--;

			// binary search
			int min = 0;
			int max = camlp4Locations.size() - 1;
			int locationIndex = min + (max - min) / 2;
			while (locationIndex > min) {
				Camlp4Location loc = camlp4Locations.get(locationIndex);

				if (pos > loc.offset)
					min = locationIndex;
				else
					max = locationIndex;

				locationIndex = min + (max - min) / 2;
			}

			while (true) {
				Camlp4Location location = camlp4Locations.get(locationIndex);

				// keep only the definition name instead of the whole definition
				int lineOffset;
				try {
					lineOffset = oldDocument.getLineOffset(location.line - 1);
				} catch (BadLocationException e) {
					OcamlPlugin.logError("bad location while associating camlp4 locations", e);
					return;
				}

				int originalOffsetStart = lineOffset + location.colStart;
				int originalOffsetEnd = lineOffset + location.colEnd;

				if (originalOffsetEnd > strOldDocument.length())
					originalOffsetEnd = strOldDocument.length();
				// def.posStart = 0;
				// def.posEnd = 0;
				// break;
				// }

				String wholeDefinition = strOldDocument.substring(originalOffsetStart,
						originalOffsetEnd);

				int nameIndex = wholeDefinition.indexOf(def.name);
				int nameLength = def.name.length();

				while (nameIndex != -1) {
					char prevChar = ' ';
					if (nameIndex > 0)
						prevChar = wholeDefinition.charAt(nameIndex - 1);
					char nextChar = ' ';
					if (nameIndex + nameLength < wholeDefinition.length())
						nextChar = wholeDefinition.charAt(nameIndex + nameLength);

					/*
					 * if the definition name is preceded or followed by an ocaml identifier char,
					 * it is not a definition name
					 */
					if (Misc.isOcamlIdentifierChar(prevChar) || Misc.isOcamlIdentifierChar(nextChar))
						nameIndex = wholeDefinition.indexOf(def.name, nameIndex + 1);
					else
						break;
				}

				/*
				 * Go to the previous comment if the range we found doesn't contain the definition
				 * name
				 */
				if (nameIndex == -1) {
					if (locationIndex > 0) {
						locationIndex--;
						continue;
					} else {
						def.posStart = 0;
						def.posEnd = 0;
						break;
					}
					// nameIndex = 0;
				}

				// System.out.println(pos);

				originalOffsetStart += nameIndex;
				originalOffsetEnd = originalOffsetStart + def.name.length();

				int lineStart;
				int lineEnd;
				int lineOffsetStart;
				int lineOffsetEnd;
				try {
					lineStart = oldDocument.getLineOfOffset(originalOffsetStart);
					lineEnd = oldDocument.getLineOfOffset(originalOffsetEnd);
					lineOffsetStart = oldDocument.getLineOffset(lineStart);
					lineOffsetEnd = oldDocument.getLineOffset(lineEnd);
				} catch (BadLocationException e) {
					OcamlPlugin.logError("bad location in camlp4 offsets", e);
					return;
				}

				def.posStart = Def.makePosition(lineStart, originalOffsetStart - lineOffsetStart);
				def.posEnd = Def.makePosition(lineEnd, originalOffsetEnd - lineOffsetEnd - 1);

				break;

				// def.posStart = Def.makePosition(originalLine - 1, originalColStart);
				// def.posEnd = Def.makePosition(originalLine - 1, originalColEnd - 1);
			}
		}

		for (Def child : def.children)
			associateCamlp4Locations(oldDocument, strOldDocument, newDocument, camlp4Locations,
					child, monitor);
	}

	public ArrayList<Camlp4Location> parseCamlp4Locations(IDocument oldDoc, IDocument doc) {
		ArrayList<Camlp4Location> camlp4Locations = new ArrayList<Camlp4Location>();

		String document = doc.get();

		Matcher matcher = patternLocation.matcher(document);
		while (matcher.find()) {
			int originalLine = Integer.parseInt(matcher.group(1));
			int originalColStart = Integer.parseInt(matcher.group(2));
			int originalColEnd = Integer.parseInt(matcher.group(3));

			camlp4Locations.add(new Camlp4Location(matcher.start(), originalLine, originalColStart,
					originalColEnd));
		}

		return camlp4Locations;
	}

}
