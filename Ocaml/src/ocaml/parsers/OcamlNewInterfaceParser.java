package ocaml.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.parser.Def;
import ocaml.parser.OcamlParser;
import ocaml.parser.OcamlScanner;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;

/**
 * An OCaml interface parser. Allows us to extract all the definitions from an
 * OCaml interface, together with the documentation associated to each
 * definition, so as to be able to display it to the user when needed. <br>
 * This class makes use of the singleton design pattern, to cache all searches.
 */
public class OcamlNewInterfaceParser {

	private OcamlNewInterfaceParser() {

	}

	/** Singleton instance of the parser (this allows us to use a cache) */
	static private OcamlNewInterfaceParser instance = null;

	/**
	 * singleton design pattern
	 * 
	 * @return the singleton instance of the OCaml interface parser
	 */
	public static OcamlNewInterfaceParser getInstance() {
		if (instance == null)
			instance = new OcamlNewInterfaceParser();
		return instance;
	}

	/** The cache of module definitions */
	private LinkedList<CachedDef> cache = new LinkedList<CachedDef>();

	/**
	 * ocamldoc section comment. We retrieve them and put them at the beginning
	 * of the following definitions
	 */
	private Pattern patternSectionComment = Pattern
			.compile("\\A *\\{\\d+ (.*)\\}((.|\\n)*)\\z");

	/**
	 * The comment intervals in the source code. They are used to avoid wrongly
	 * interpreting a keyword inside a comment, and to attach comments to
	 * definitions.
	 */
	private LinkedList<Comment> comments;

	/** Section comments */
	private LinkedList<Comment> sectionComments;

	/**
	 * Parse the OCaml interface to extract definitions and ocamldoc comments
	 * attached to them, and put the result in the cache.
	 * <p>
	 * We use a File instead of an IFile because the file can be out of the
	 * workspace (OCaml library files, typically)
	 * 
	 * @param file
	 *            the file to parse (interface or module)
	 * @param bInProject
	 *            This file is it part of the project? (this is used to put a
	 *            different icon on project modules and standard library
	 *            modules)
	 * 
	 * @return The module definition or <code>null</code> if the file can't be
	 *         read
	 */
	public synchronized Def parseFile(final File file) {

		String filename = "";
		try {
			filename = file.getCanonicalPath();
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		/*
		 * Table of stale entries, that we will remove from the cache. We can't
		 * do that in the next loop, because of concurrent access issues.
		 */
		ArrayList<CachedDef> toRemove = new ArrayList<CachedDef>();
		Def found = null;

		// First, see if the informations are in the cache
		for (CachedDef def : cache) {
			if (def.sameAs(file)) {
				// The entry is in the cache, and is still valid
				if (def.isMoreRecentThan(file))
					found = def.getDefinition();
				/*
				 * The cache entry is not valid anymore: we put it in the list
				 * of entries to delete from the cache
				 */
				else {
					toRemove.add(def);
				}
			}
		}

		// remove the stale entries from the cache
		for (CachedDef def : toRemove) {
			cache.remove(def);
		}

		// return the cache entry (if we found one)
		if (found != null)
			return found;

		if (!file.canRead())
			return null;
		final BufferedReader inputStream;

		try {
			inputStream = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		StringBuilder sbLines = new StringBuilder();
		// read the file, line by line
		{
			String line;
			try {
				while ((line = inputStream.readLine()) != null)
					sbLines.append(line + "\n");
			} catch (IOException e) {
				OcamlPlugin.logError("ocaml plugin error", e);
				return null;
			}
		}

		String lines = sbLines.toString();
		String unprocessedLines = lines;

		boolean bInterface = false;

		String moduleName = file.getName();
		// remove the extension
		if (moduleName.endsWith(".mli")) {
			bInterface = true;
			moduleName = moduleName.substring(0, moduleName.length() - 4);
		} else if (moduleName.endsWith(".ml")) {
			moduleName = moduleName.substring(0, moduleName.length() - 3);
			bInterface = false;
		}
		// else
		// return null;

		// capitalize the first letter
		if (moduleName.length() > 0)
			moduleName = Character.toUpperCase(moduleName.charAt(0))
					+ moduleName.substring(1);

		final Camlp4Preprocessor preprocessor = new Camlp4Preprocessor(lines);
		if (preprocessor.mustPreprocess()) {
			// preprocess the file with camlp4
			Job job = new Job("preprocessing " + file.getName()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					preprocessor.preprocess(file, monitor);
					return Status.OK_STATUS;
				}
			};

			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				OcamlPlugin.logError("interrupted", e);
			}

			String errors = preprocessor.getErrorOutput().trim();
			if (!"".equals(errors)) {
				Def def = new Def(moduleName, Def.Type.ParserError, 0, 0);
				def.filename = filename;

				def
						.setComment("ERROR: The camlp4 preprocessor encountered an error "
								+ "while parsing this file:\n" + errors);

				cache.addFirst(new CachedDef(file, def));
				return def;
			}

			lines = preprocessor.getOutput();

		}

		Def definition = null;
		// parse the .mli interface file or the .ml module file
		try {
			// System.err.println("parsing:" + filename);
			definition = parseModule(lines, moduleName, bInterface);
		} catch (Throwable e) {
			// if there was a parsing error, we log it and we continue on to the
			// next file
			// OcamlPlugin.logError("Error parsing '" + moduleName + "'", e);
			Def def = new Def(moduleName, Def.Type.ParserError, 0, 0);
			def.filename = filename;

			def
					.setComment("ERROR: The parser encountered an error while parsing this file.\n\n"
							+ "Please make sure that it is syntactically correct.\n\n");

			// System.err.println("ERROR:" + filename);

			cache.addFirst(new CachedDef(file, def));
			return def;
		}

		definition.setBody("module " + moduleName);

		/*
		 * The source was preprocessed by camlp4: update the identifiers
		 * locations using the 'loc' comments leaved by camlp4
		 */
		if (preprocessor.mustPreprocess()) {
			// System.err.println("associate locations");

			Document document = new Document(lines);
			Document oldDocument = new Document(unprocessedLines);
			ArrayList<Camlp4Preprocessor.Camlp4Location> camlp4Locations = preprocessor
					.parseCamlp4Locations(oldDocument, document);

			preprocessor.associateCamlp4Locations(oldDocument,
					unprocessedLines, document, camlp4Locations, definition,
					null);
		}

		setFilenames(definition, filename);

		// put the entry into the cache
		cache.addFirst(new CachedDef(file,
				definition));

		// definition.print(0);
		/*
		 * Return the module definition (root) that contains all this module's
		 * definitions (recursively)
		 */
		return definition;
	}

	private void setFilenames(Def definition, String filename) {
		definition.filename = filename;
		for (Def child : definition.children)
			setFilenames(child, filename);
	}

	private Def parseModule(String doc, String moduleName,
			boolean parseInterface) throws Throwable {

		/*
		 * "Sanitize" the document by replacing extended characters, which
		 * otherwise would crash the parser
		 */
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < doc.length(); i++) {
			char c = doc.charAt(i);

			// replace it by an underscore
			if (c > 127)
				c = '_';
			str.append(c);
		}

		final StringReader in = new StringReader(str.toString());
		final OcamlScanner scanner = new OcamlScanner(in);
		final OcamlParser parser = new OcamlParser();

		Def root = null;

		if (parseInterface)
			root = (Def) parser.parse(scanner, OcamlParser.AltGoals.interfaces);
		else
			root = (Def) parser.parse(scanner);

		root = root.cleanCopy();

		root.name = moduleName;

		// set the start offset from the packed (line, column) positions
		computeLinesStartOffset(doc);
		computeDefinitionsStartOffset(root);

		// parse the comments and remove them from the text
		if (parseInterface) {
			doc = parseComments(doc);

			// find the end of each definition (the parser only gives us the
			// start)
			root.defOffsetEnd = doc.length();
			findDefinitionsEnd(root, doc, 0, null);

			attachComments(root, root, doc);

		}

		setBodies(root, doc, parseInterface);

		root.unnestTypes(null, 0);

		if (parser.errorReporting.errors.size() != 0) {
			root.type = Def.Type.ParserError;
			root
					.setComment("ERROR: The parser encountered an error while parsing this file.\n\n"
							+ "Please make sure that it is syntactically correct.\n\n");
		} else
			root.type = Def.Type.Module;

		// associate the module comment
		if (comments.size() > 0 && parseInterface)
			root.setComment(comments.get(0).text);

		return root;
	}

	private void setBodies(Def def, String doc, boolean parseInterface) {
		if (def.type != Def.Type.Root) {
			if (parseInterface) {
				if (def.defOffsetStart <= def.defOffsetEnd) {
					def.setBody(doc.substring(def.defOffsetStart, def.defOffsetEnd));
				} else {
					OcamlPlugin.logWarning("Wrong offsets on " + def.type + " '" + def.name + "'");
				}
			} else {
				def.setBody(def.name);
			}
		}

		for (Def child : def.children)
			setBodies(child, doc, parseInterface);
	}

	ArrayList<Integer> lineOffsets = null;

	private void computeLinesStartOffset(String doc) {
		lineOffsets = new ArrayList<Integer>();
		lineOffsets.add(0);
		for (int i = 0; i < doc.length(); i++) {

			/*
			 * if(doc.charAt(i) == '\r') System.err.print("<R>\n");
			 * if(doc.charAt(i) == '\n') System.err.print("<N>\n"); else
			 * System.err.print(doc.charAt(i));
			 */

			if (doc.charAt(i) == '\n') {
				lineOffsets.add(i + 1);
				// System.err.println(i);
			}

		}
	}

	private void computeDefinitionsStartOffset(Def def) {

		// System.err.println(Def.getColumn(def.defPosStart));
		// int line = ;
		// System.err.println(line);
		int lineOffset = lineOffsets.get(Def.getLine(def.defPosStart));
		def.defOffsetStart = lineOffset + Def.getColumn(def.defPosStart);

		for (Def child : def.children)
			computeDefinitionsStartOffset(child);
	}

	/**
	 * Find the end of each definition, knowing the start of the next
	 * definition.
	 * 
	 * @param def
	 *            the definition to set
	 * @param doc
	 *            the text in which the definition appears
	 * @param parent
	 *            the parent of the definition in the tree
	 * @param index
	 *            the index of this definition in its parent
	 */
	private void findDefinitionsEnd(Def def, String doc, int index, Def parent) {

		// String name = def.name;

		if (def.type != Def.Type.Root) {
			if (index + 1 < parent.children.size()) {
				Def next = parent.children.get(index + 1);
				def.defOffsetEnd = next.defOffsetStart;
			} else {
				def.defOffsetEnd = parent.defOffsetEnd;
			}
		}

		boolean skip = false;
		if (def.defOffsetStart > def.defOffsetEnd || def.defOffsetStart < 0) {
			OcamlPlugin
					.logError("OcamlNewParser: findDefinitionsEnd: wrong offset ("
							+ def.name + ")");
			skip = true;
		}

		if (def.type != Def.Type.Root && !skip) {

			// remove the blank space at the end of the definition
			String defText = doc
					.substring(def.defOffsetStart, def.defOffsetEnd).trim();

			// remove the ";;" at the end
			if (defText.endsWith(";;"))
				defText = defText.substring(0, defText.length() - 2).trim();

			// remove the "end" in a module declaration
			if (parent.type == Def.Type.Module
					|| parent.type == Def.Type.ModuleType
					|| parent.type == Def.Type.Class
					|| parent.type == Def.Type.ClassType
					|| parent.type == Def.Type.Functor) {
				if (defText.endsWith("end"))
					defText = defText.substring(0, defText.length() - 3).trim();
			}

			// remove the "|" at the end
			if (def.type == Def.Type.TypeConstructor) {
				if (defText.endsWith("|"))
					defText = defText.substring(0, defText.length() - 1).trim();
			}

			if (def.type == Def.Type.RecordTypeConstructor) {
				if (defText.endsWith("}"))
					defText = defText.substring(0, defText.length() - 1).trim();

				if (defText.endsWith(";"))
					defText = defText.substring(0, defText.length() - 1).trim();
			}

			int realLength = defText.trim().length();
			def.defOffsetEnd = def.defOffsetStart + realLength;
		}

		for (int i = 0; i < def.children.size(); i++) {
			Def child = def.children.get(i);
			findDefinitionsEnd(child, doc, i, def);
		}
	}

	/**
	 * An ocamldoc comment: beginning, end, and body. This is used to associate
	 * ocamldoc comments with the corresponding definitions.
	 */
	private class Comment {
		public Comment(int begin, int end, String text) {
			this.begin = begin;
			this.end = end;
			this.text = text;
		}

		int begin;

		int end;

		String text;
	}

	/**
	 * Look for all the comment intervals in the text, and replace them by
	 * spaces. Keep all the comments in table <code>comments</code>. Look
	 * also for the section comments and put them in the
	 * <code>sectionComments</code> table.
	 */
	private String parseComments(String lines) {

		StringBuilder result = new StringBuilder(lines);
		comments = new LinkedList<Comment>();
		sectionComments = new LinkedList<Comment>();

		boolean bParL = false;
		boolean bStar = false;
		boolean bInComment = false;
		boolean bEscape = false;
		int commentStart = 0;
		int codeNestingLevel = 0;
		boolean bInOcamldocComment = false;

		for (int i = 0; i < lines.length(); i++) {
			char ch = lines.charAt(i);
			if (ch == '(' && !bInComment)
				bParL = true;
			else if (ch == '*' && bParL) {
				bInComment = true;
				bParL = false;
				commentStart = i + 1;
				bInOcamldocComment = false;
			} else if (ch == '*' && i == commentStart) {
				bInOcamldocComment = true;
			} else if (ch == '*' && bInComment && codeNestingLevel == 0)
				bStar = true;
			else if (ch == ')' && bStar && bInComment && codeNestingLevel == 0) {
				bInComment = false;

				// ocamldoc comment (the normal comments are not useful to us)
				if (commentStart + 1 < lines.length()
						&& lines.charAt(commentStart) == '*'
						&& lines.charAt(commentStart + 1) != '*') {

					String body = lines.substring(commentStart + 1, i - 1);
					Matcher matcherSectionComment = patternSectionComment
							.matcher(body);
					if (matcherSectionComment.find()) {
						String section = matcherSectionComment.group(1) + "\n"
								+ matcherSectionComment.group(2);
						sectionComments.add(new Comment(commentStart + 1,
								i - 1, section.trim()));
					} else
						comments
								.add(new Comment(commentStart + 1, i - 1, body));
				}

				// replace the comment by spaces (so as to preserve the offsets)
				for (int j = commentStart - 2; j < i + 1; j++)
					result.setCharAt(j, ' ');

				bStar = false;
				bParL = false;
			} else {
				bParL = false;
				bStar = false;

				if (ch == '\\')
					bEscape = !bEscape;
				else if (ch == '[' && !bEscape && bInOcamldocComment)
					codeNestingLevel++;
				else if (ch == ']' && !bEscape && codeNestingLevel > 0
						&& bInOcamldocComment)
					codeNestingLevel--;
				else
					bEscape = false;
			}
		}

		// return the text without the comments
		return result.toString();
	}

	private void attachComments(Def def, Def parent, String doc) {

		for (Def child : def.children)
			attachComments(child, parent, doc);

		if (def.type != Def.Type.Root) {
			// avoid the constructor to "steal" its type's comment
			if (def.type == Def.Type.RecordTypeConstructor)
				attachComment(def, doc, true, parent.defOffsetEnd, false);
			else if (def.type == Def.Type.TypeConstructor) {
				attachComment(def, doc, true, Integer.MAX_VALUE, false);
			} else
				attachComment(def, doc, false, Integer.MAX_VALUE, false);

			attachSectionComment(def);
		}
	}

	/**
	 * Retrieve the comment attached to the definition that spans from
	 * <code>begin</code> to <code>end</code>. Associate this comment to
	 * <code>definition</code> and delete it from the list. During the
	 * parsing, attach module comments to <code>module</code>.
	 */
	private void attachComment(Def definition, String doc, boolean onlyAfter,
			int maxOffset, boolean noNewLines) {

		int begin = definition.defOffsetStart;
		int end = definition.defOffsetEnd;

		end--;
		if (end < 0)
			end = 0;
		if (end > doc.length() - 1)
			end = doc.length() - 1;

		// discard the blanks at the beginning of the definition
		char ch = doc.charAt(end);
		while (end > 0 && ch <= ' ')
			end--;
		// end = the first character just after the end of the definition
		end++;

		ArrayList<Comment> toDelete = new ArrayList<Comment>();

		for (Comment comment : comments) {
			// a comment right before the definition
			if (nextTo(doc, comment.end, begin, noNewLines) && !onlyAfter) {
				definition.appendToComment(comment.text);
				toDelete.add(comment);
			}
			// a comment right after the definition
			else if (nextTo(doc, end, comment.begin, noNewLines)
					&& comment.end <= maxOffset) {
				definition.appendToComment(comment.text);
				toDelete.add(comment);
				break;
			} else if (comment.begin > end)
				break;
		}

		for (Comment comment : toDelete) {
			comments.remove(comment);
		}

	}

	/**
	 * Attach the first comment found inside the range going from
	 * <code>begin</code> to <code>end</code>. This is useful for type
	 * constructors.
	 */
	/*
	 * private void attachCommentIn(OcamlDefinition definition, int begin, int
	 * end) {
	 * 
	 * Comment toDelete = null;
	 * 
	 * for (Comment comment : comments) { if (comment.begin >= begin &&
	 * comment.end <= end) { definition.appendToComment(comment.text); toDelete =
	 * comment; break; } }
	 * 
	 * if (toDelete != null) comments.remove(toDelete); }
	 */

	/**
	 * Attach the first section comment before <code>definition</code>.
	 */
	private void attachSectionComment(Def definition) {

		/*
		 * look for the first comment that is past the offset, and take the one
		 * before it
		 */
		Comment previousComment = null;
		for (Comment comment : sectionComments) {
			if (comment.begin > definition.defOffsetStart)
				break;
			previousComment = comment;
		}

		if (previousComment != null)
			definition.setSectionComment(previousComment.text);

	}

	/**
	 * @return true if there are less than 2 newlines between
	 *         <code>offset1</code> and <code>offset2</code> in
	 *         <code>text</code>
	 */
	private boolean nextTo(String text, int offset1, int offset2,
			boolean noNewLines) {
		if (offset2 < offset1)
			return false;

		boolean bNewline = false;
		for (int i = offset1; i < offset2; i++) {
			if (text.charAt(i) == '\n') {
				if (bNewline)
					return false;
				else {
					if (noNewLines)
						return false;
					bNewline = true;
				}
			} else if (!Character.isWhitespace(text.charAt(i))
					&& text.charAt(i) != ';')
				return false;
		}

		return true;
	}
}
