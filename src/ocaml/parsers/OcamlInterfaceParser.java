//package ocaml.parsers;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import ocaml.OcamlPlugin;
//import ocaml.parsers.OcamlDefinition.Type;
//
///**
// * An O'Caml interface parser. Allows us to extract all the definitions from an O'Caml interface, together
// * with the documentation associated to each definition, so as to be able to display it to the user when
// * needed. <br>
// * This class makes use of the singleton design pattern, to cache all searches.
// */
//public class OcamlInterfaceParser {
//
//	private OcamlInterfaceParser() {
//
//	}
//
//	/** Singleton instance of the parser (this allows us to use a cache) */
//	static private OcamlInterfaceParser instance = null;
//
//	/**
//	 * singleton design pattern
//	 * 
//	 * @return the singleton instance of the O'Caml interface parser
//	 */
//	public static OcamlInterfaceParser getInstance() {
//		if (instance == null)
//			instance = new OcamlInterfaceParser();
//		return instance;
//	}
//
//	/** The cache of module definitions */
//	private LinkedList<CachedMli> cache = new LinkedList<CachedMli>();
//
//	/** The keywords, that we use as delimiters to find definitions */
//	private Pattern patternDelimiters = Pattern
//			.compile("\\W(type|val|external|exception|module|sig|end|method|class|object|and)\\W");
//
//	/** ocamldoc section comment. We retrieve them and put them at the beginning of the following definitions */
//	private Pattern patternSectionComment = Pattern.compile("\\A *\\{\\d+ (.*)\\}((.|\\n)*)\\z");
//
//	/** A type definition of the kind: "type aType = A | B of int |..." */
//	private Pattern patternType = Pattern
//			.compile("\\A((type|and) *(\\(?.*?\\)?)? *((\\w|')+) *=(\\s*(\\w|\\.|')+\\s*=)*)");
//
//	/** A type definition of the kind: "type aType = {A:int; B:truc} */
//
//	/** A type definition of the kind: "type 'a t" */
//	private Pattern patternRawType = Pattern
//			.compile("\\A((type|and) *(\\(.*?\\)) *(.*))|((type|and) *.* +(.*))");
//
//	/** A documentation comment */
//	// private Pattern patternComment = Pattern.compile("\\(\\*\\*((.|\\n)*)\\*\\)");
//	/** module type aName = */
//	private Pattern patternModuleType = Pattern.compile("\\Amodule\\s+type\\s+(\\w+?)\\s*=");
//
//	/** module aModule : */
//	private Pattern patternModule = Pattern.compile("\\Amodule\\s+(\\w+?)\\s*:");
//
//	/** functor */
//	private Pattern patternModuleOther = Pattern.compile("\\Amodule\\s+(\\w+).*");
//
//	/**
//	 * alternative syntax: we don't try to parse these alternative syntax interfaces, because it would crash
//	 * the parser
//	 */
//	private Pattern patternValue = Pattern.compile("(^|\\n)value");
//
//	/** class aClass : */
//	private Pattern patternClass = Pattern.compile("\\Aclass\\s+(\\w+)\\s*:");
//
//	/**
//	 * The comment intervals in the source code. They are used to avoid wrongly interpreting a keyword inside
//	 * a comment, and to attach comments to definitions.
//	 */
//	private LinkedList<Comment> comments;
//
//	/** Section comments */
//	private LinkedList<Comment> sectionComments;
//
//	/** The nesting level of "object-end" definitions (classes) */
//	private int objectNestingLevel;
//
//	private String filename = "";
//
//	/**
//	 * Do we discard all module comments after the first? Normally, the module would get all the comments that
//	 * are not attached to anything else. But in general, these comments are numerous and useless. So, we
//	 * discard them. TODO user preference?
//	 */
//	private final boolean bOnlyOneModuleComment = true;
//
//	/**
//	 * Parse the O'Caml interface to extract definitions and ocamldoc comments attached to them, and put the
//	 * result in the cache.
//	 * <p>
//	 * We use a File instead of an IFile because the file can be out of the workspace (O'Caml library files,
//	 * typically)
//	 * 
//	 * @param interfaceFile
//	 *            the interface file to parse
//	 * @param bInProject
//	 *            This file is it part of the project? (this is used to put a different icon on project
//	 *            modules and standard library modules)
//	 * 
//	 * @return The module definition or <code>null</code> if the file can't be read
//	 */
//	public synchronized OcamlDefinition parseFile(File interfaceFile) {
//
//		try {
//			filename = interfaceFile.getCanonicalPath();
//		} catch (IOException e) {
//			OcamlPlugin.logError("ocaml plugin error", e);
//			return null;
//		}
//
//		/*
//		 * Table of stale entries, that we remove now from the cache. We can't do that in the next loop,
//		 * because of concurrent access issues.
//		 */
//		ArrayList<CachedMli> toRemove = new ArrayList<CachedMli>();
//		OcamlDefinition found = null;
//
//		// First, see if the informations are in the cache
//		for (CachedMli def : cache) {
//			if (def.sameAs(interfaceFile)) {
//				// The entry is in the cache, and is still valid
//				if (def.isMoreRecentThan(interfaceFile))
//					found = def.getModuleDefinition();
//				/*
//				 * The cache entry is not valid anymore: we put it in the list of entries to delete from the
//				 * cache
//				 */
//				else {
//					toRemove.add(def);
//				}
//			}
//		}
//
//		// remove the stale entries from the cache
//		for (CachedMli def : toRemove)
//			cache.remove(def);
//		// return the cache entry (if we found one)
//		if (found != null) {
//			return found;
//
//		}
//
//		if (!interfaceFile.canRead())
//			return null;
//		final BufferedReader inputStream;
//
//		try {
//			inputStream = new BufferedReader(new FileReader(interfaceFile));
//		} catch (FileNotFoundException e) {
//			OcamlPlugin.logError("ocaml plugin error", e);
//			return null;
//		}
//
//		StringBuilder sbLines = new StringBuilder();
//		// to avoid bounds problems
//		sbLines.append("\n\n");
//		// read the file, line by line
//		{
//			String line;
//			try {
//				while ((line = inputStream.readLine()) != null)
//					sbLines.append(line + "\n");
//			} catch (IOException e) {
//				OcamlPlugin.logError("ocaml plugin error", e);
//				return null;
//			}
//		}
//
//		// we add a dummy delimiter so that the main loop will see the last (real) definition
//		sbLines.append("\n\n\nval ");
//
//		String lines = sbLines.toString();
//
//		String moduleName = interfaceFile.getName();
//		// remove the l'extension
//		moduleName = moduleName.replaceAll("\\.mli$", "");
//		if (moduleName.length() > 0)
//			moduleName = "" + Character.toUpperCase(moduleName.charAt(0)) + moduleName.substring(1);
//
//		// test if this file uses the alternative syntax (value instead of val, among other things)
//		Matcher matcher = patternValue.matcher(lines);
//		if (matcher.find()) {
//			OcamlDefinition def = new OcamlDefinition(Type.DefModule);
//			def.setName("<weird syntax:" + moduleName + ".mli>");
//			def.setComment("This interface uses a syntax which is not understood by the parser.");
//			cache.addFirst(new CachedMli(interfaceFile, def));
//			return def;
//		}
//
//		OcamlDefinition definition = null;
//		// parse the .mli interface file
//		try {
//			definition = parseModule(lines, moduleName);
//		} catch (Throwable e) {
//			// if there was a parsing error, we log it and we continue on to the next file
//			OcamlPlugin.logError("Error parsing '" + moduleName + "'", e);
//			OcamlDefinition def = new OcamlDefinition(Type.DefModule);
//			def.setName("<parser error:" + moduleName + ">");
//			def.setComment("The parsing of this module lead to the following error: " + e.toString());
//			cache.addFirst(new CachedMli(interfaceFile, def));
//			return def;
//		}
//
//		definition.setFilename(filename);
//		definition.setBody("module " + moduleName);
//
//		// put the entry into the cache
//		cache.addFirst(new CachedMli(interfaceFile, definition));
//		/*
//		 * Return the module definition that contains all this module's definitions (recursively)
//		 */
//		return definition;
//	}
//
//	/**
//	 * Parse a module and return its definition
//	 * 
//	 * @param lines
//	 *            the contents of the interface
//	 * @param moduleName
//	 *            the module name (must start by a capital letter)
//	 * @return the module definition extracted from the interface
//	 */
//	private OcamlDefinition parseModule(String lines, String moduleName) {
//		lines = parseComments(lines);
//		objectNestingLevel = 0;
//
//		/*
//		 * The nested definitions stack: the element on top of the stack is the one nested deepest
//		 */
//		LinkedList<OcamlDefinition> stack = new LinkedList<OcamlDefinition>();
//
//		// last read definition
//		OcamlDefinition lastDefinition = null;
//
//		// the start offset of the definition which was just read
//		int lastDefinitionStartOffset = 0;
//
//		// current definition (is it a val? an exception? ...)
//		boolean bReadingVal = false;
//		boolean bReadingExternal = false;
//		boolean bReadingException = false;
//		boolean bReadingType = false;
//
//		// was the last read definition a "type"? a val? (to associate the "and" correctly)
//		boolean bLastIsType = false;
//		boolean bLastIsVal = false;
//
//		/*
//		 * Parse all the delimiters and analyze what's in between each pair of delimiters
//		 */
//		Matcher matcherDelimiters = patternDelimiters.matcher(lines);
//		// the matcher position inside the file (this is used to skip blocks)
//		int matcherPosition = 0;
//
//		/*
//		 * Add the top-level module (that will contain all the definitions found at the top level)
//		 */
//		stack.clear();
//		stack.addLast(new OcamlDefinition(OcamlDefinition.Type.DefModule));
//
//		stack.getLast().setName(moduleName);
//
//		while (matcherDelimiters.find(matcherPosition)) {
//
//			String delimiter = matcherDelimiters.group(1);
//			int delimiterStart = matcherDelimiters.start(1);
//
//			/*
//			 * The last delimiter was a "val", "external" or "exception". Now that we have the next delimiter,
//			 * we can read the body of the definition.
//			 */
//
//			if (bReadingVal || bReadingExternal || bReadingException) {
//				String definitionBody = lines.substring(lastDefinitionStartOffset, delimiterStart).trim();
//
//				OcamlDefinition def = null;
//
//				if (bReadingVal)
//					def = parseVal(definitionBody);
//				else if (bReadingExternal)
//					def = parseExternal(definitionBody);
//				else if (bReadingException) {
//					def = new OcamlDefinition(Type.DefException);
//					String[] words = definitionBody.split(":|\\s+");
//
//					// the constructor name is the second word
//					if (words.length > 1)
//						def.setName(words[1]);
//				}
//
//				def.setBody(definitionBody);
//				def.setFilename(filename);
//				attachSectionComment(def, lastDefinitionStartOffset);
//
//				attachComment(def, stack.getLast(), lines, lastDefinitionStartOffset, delimiterStart);
//
//				lastDefinition = def;
//
//				bReadingVal = false;
//				bReadingExternal = false;
//				bReadingException = false;
//				stack.getLast().addChild(def);
//			}
//
//			if (bReadingType) {
//				// the definition body
//				String definitionBody = lines.substring(lastDefinitionStartOffset, delimiterStart);
//
//				// extract the type constructors and their respective comments
//				parseType(lines, lastDefinitionStartOffset, definitionBody, stack.getLast());
//
//				OcamlDefinition def = new OcamlDefinition(OcamlDefinition.Type.DefType);
//				definitionBody = definitionBody.replaceAll("\\n\\s+", "\n");
//				def.setBody(definitionBody);
//				def.setFilename(filename);
//				attachSectionComment(def, lastDefinitionStartOffset);
//
//				attachComment(def, stack.getLast(), lines, lastDefinitionStartOffset, delimiterStart);
//
//				// extract the type name
//				Matcher matcher = patternType.matcher(definitionBody);
//				if (matcher.find())
//					def.setName(matcher.group(4));
//				else {
//					matcher = patternRawType.matcher(definitionBody);
//					if (matcher.find()) {
//						if (matcher.group(4) != null)
//							def.setName(matcher.group(4));
//						else
//							def.setName(matcher.group(7));
//					}
//				}
//
//				lastDefinition = def;
//				bReadingType = false;
//				stack.getLast().addChild(def);
//			}
//
//			if (delimiter.equals("type") || delimiter.equals("and") && bLastIsType) {
//				lastDefinitionStartOffset = delimiterStart;
//				bReadingType = true;
//				bLastIsType = true;
//				bLastIsVal = false;
//
//			} else if ((delimiter.equals("val") || delimiter.equals("and") && bLastIsVal)
//					&& objectNestingLevel == 0) {
//				lastDefinitionStartOffset = delimiterStart;
//				bReadingVal = true;
//				bLastIsVal = true;
//				bLastIsType = false;
//			} else if (delimiter.equals("external")) {
//				lastDefinitionStartOffset = delimiterStart;
//				bReadingExternal = true;
//
//			} else if (delimiter.equals("exception")) {
//				lastDefinitionStartOffset = delimiterStart;
//				bReadingException = true;
//
//			} else if (delimiter.equals("sig")) {
//				stack.addLast(lastDefinition);
//				lastDefinitionStartOffset = delimiterStart;
//			} else if (delimiter.equals("end")) {
//				if (objectNestingLevel == 0) {
//					// we always leave the "module" element on the stack
//					if (stack.size() > 1)
//						lastDefinition = stack.removeLast();
//					// we found more "end"s than "sig"s: error
////					else
////						OcamlPlugin.logWarning("keyword 'end' has no corresponding 'sig' (" + moduleName + ")");
//				} else if (objectNestingLevel > 0)
//					objectNestingLevel--;
//
//				lastDefinitionStartOffset = delimiterStart;
//			} else if (delimiter.equals("object")) {
//				objectNestingLevel++;
//				lastDefinitionStartOffset = delimiterStart;
//
//			} else if (delimiter.equals("module")) {
//				// what's after the "module" delimiter
//				String rest = lines.substring(delimiterStart);
//				Matcher matcherModuleType = patternModuleType.matcher(rest);
//				if (matcherModuleType.find()) {
//					String definitionBody = matcherModuleType.group();
//
//					OcamlDefinition def = new OcamlDefinition(OcamlDefinition.Type.DefType);
//					def.setBody(definitionBody);
//					def.setFilename(filename);
//					attachSectionComment(def, lastDefinitionStartOffset);
//					attachComment(def, stack.getLast(), lines, lastDefinitionStartOffset, delimiterStart);
//					def.setName(matcherModuleType.group(1));
//
//					stack.getLast().addChild(def);
//					lastDefinition = def;
//
//					matcherPosition = matcherModuleType.end() + delimiterStart - 1;
//					continue;
//				} else {
//					Matcher matcherModule = patternModule.matcher(rest);
//					if (matcherModule.find()) {
//						String definitionBody = matcherModule.group();
//
//						OcamlDefinition def = new OcamlDefinition(OcamlDefinition.Type.DefModule);
//						def.setBody(definitionBody);
//						def.setFilename(filename);
//						attachSectionComment(def, lastDefinitionStartOffset);
//						def.setName(matcherModule.group(1));
//						attachComment(def, stack.getLast(), lines, lastDefinitionStartOffset, delimiterStart);
//
//						stack.getLast().addChild(def);
//						lastDefinition = def;
//
//						matcherPosition = matcherModule.end() + delimiterStart - 1;
//						continue;
//					} else {
//						Matcher matcherModuleOther = patternModuleOther.matcher(rest);
//						if (matcherModuleOther.find()) {
//							String definitionBody = matcherModuleOther.group();
//
//							OcamlDefinition def = new OcamlDefinition(OcamlDefinition.Type.DefModule);
//							def.setBody(definitionBody);
//							def.setFilename(filename);
//							attachSectionComment(def, lastDefinitionStartOffset);
//							attachComment(def, stack.getLast(), lines, lastDefinitionStartOffset,
//									delimiterStart);
//							def.setName(matcherModuleOther.group(1));
//
//							stack.getLast().addChild(def);
//							lastDefinition = def;
//
//							matcherPosition = matcherModuleOther.end() + delimiterStart - 1;
//							continue;
//						} else
//							OcamlPlugin.logWarning("cannot understand module definition (in " + moduleName
//									+ ")");
//					}
//				}
//
//				lastDefinitionStartOffset = delimiterStart;
//			} else if (delimiter.equals("class")) {
//				// what's after the "class" delimiter
//				String rest = lines.substring(delimiterStart);
//				Matcher matcherClass = patternClass.matcher(rest);
//				if (matcherClass.find()) {
//					String name = matcherClass.group(1);
//
//					OcamlDefinition def = new OcamlDefinition(OcamlDefinition.Type.DefClass);
//					def.setBody("class " + name);
//					def.setFilename(filename);
//					attachSectionComment(def, lastDefinitionStartOffset);
//					attachComment(def, stack.getLast(), lines, delimiterStart, delimiterStart + 5);
//					def.setName(name);
//
//					stack.getLast().addChild(def);
//					lastDefinition = def;
//				}
//			}
//
//			matcherPosition = matcherDelimiters.end();
//		}
//
//		/*
//		 * Normally, the stack should have only one element now, if all the opened definitions are correctly
//		 * closed.
//		 */
////		if (stack.size() != 1)
////			OcamlPlugin.logWarning("Ocaml Plugin interface parser error: stack has " + stack.size()
////					+ " elements! (it should have 1) (" + moduleName + ".mli)");
//
//		/*
//		 * Put all the remaining documentation comments in this module's comment (or only the first, depending
//		 * on the preference)
//		 */
//		OcamlDefinition top = stack.getFirst();
//
//		if (bOnlyOneModuleComment) {
//			if (top.getComment().equals("") && comments.size() > 0)
//				top.appendToComment(comments.getFirst().text);
//		} else
//			for (Comment comment : comments)
//				top.appendToComment(comment.text);
//
//		return top;
//	}
//
//	/**
//	 * An ocamldoc comment: beginning, end, and body. This is used to associate ocamldoc comments with the
//	 * corresponding definitions.
//	 */
//	private class Comment {
//		public Comment(int begin, int end, String text) {
//			this.begin = begin;
//			this.end = end;
//			this.text = text;
//		}
//
//		int begin;
//
//		int end;
//
//		String text;
//	}
//
//	/**
//	 * Look for all the comment intervals in the text, and replace them by spaces. Keep all the comments in
//	 * table <code>comments</code>. Look also for the section comments and put them in the
//	 * <code>sectionComments</code> table.
//	 */
//	private String parseComments(String lines) {
//
//		StringBuilder result = new StringBuilder(lines);
//		comments = new LinkedList<Comment>();
//		sectionComments = new LinkedList<Comment>();
//
//		boolean bParL = false;
//		boolean bStar = false;
//		boolean bInComment = false;
//		boolean bEscape = false;
//		int commentStart = 0;
//		int codeNestingLevel = 0;
//		boolean bInOcamldocComment = false;
//
//		for (int i = 0; i < lines.length(); i++) {
//			char ch = lines.charAt(i);
//			if (ch == '(' && !bInComment)
//				bParL = true;
//			else if (ch == '*' && bParL) {
//				bInComment = true;
//				bParL = false;
//				commentStart = i + 1;
//				bInOcamldocComment = false;
//			} else if (ch == '*' && i == commentStart) {
//				bInOcamldocComment = true;
//			} else if (ch == '*' && bInComment && codeNestingLevel == 0)
//				bStar = true;
//			else if (ch == ')' && bStar && bInComment && codeNestingLevel == 0) {
//				bInComment = false;
//
//				// ocamldoc comment (the normal comments are not useful to us)
//				if (commentStart + 1 < lines.length() && lines.charAt(commentStart) == '*'
//						&& lines.charAt(commentStart + 1) != '*') {
//
//					String body = lines.substring(commentStart + 1, i - 1);
//					Matcher matcherSectionComment = patternSectionComment.matcher(body);
//					if (matcherSectionComment.find()) {
//						String section = matcherSectionComment.group(1) + "\n"
//								+ matcherSectionComment.group(2);
//						sectionComments.add(new Comment(commentStart + 1, i - 1, section.trim()));
//					} else
//						comments.add(new Comment(commentStart + 1, i - 1, body));
//				}
//
//				// replace the comment by spaces (so as to preserve the offsets)
//				for (int j = commentStart - 2; j < i + 1; j++)
//					result.setCharAt(j, ' ');
//
//				bStar = false;
//				bParL = false;
//			} else {
//				bParL = false;
//				bStar = false;
//
//				if (ch == '\\')
//					bEscape = !bEscape;
//				else if (ch == '[' && !bEscape && bInOcamldocComment)
//					codeNestingLevel++;
//				else if (ch == ']' && !bEscape && codeNestingLevel > 0 && bInOcamldocComment)
//					codeNestingLevel--;
//				else
//					bEscape = false;
//			}
//		}
//
//		/*if(lines.startsWith("\n\n(*abc*)"))
//			System.err.println(result.toString());*/
//			
//			
//		// return the text without the comments
//		return result.toString();
//	}
//
//	/**
//	 * Retrieve the comment attached to the definition that spans from <code>begin</code> to
//	 * <code>end</code>. Associate this comment to <code>definition</code> and delete it from the list.
//	 * During the parsing, attach module comments to <code>module</code>.
//	 */
//	private void attachComment(OcamlDefinition definition, OcamlDefinition module, String lines, int begin,
//			int end) {
//
//		end--;
//		if (end < 0)
//			end = 0;
//		if (end > lines.length() - 1)
//			end = lines.length() - 1;
//
//		// discard the blanks at the beginning of the definition
//		while (" \n\t\r".contains("" + lines.charAt(end)) && end > 0)
//			end--;
//		// end = the first character just after the end of the definition
//		end++;
//
//		ArrayList<Comment> toDelete = new ArrayList<Comment>();
//
//		for (Comment comment : comments) {
//			// a comment right before the definition
//			if (nextTo(lines, comment.end, begin)) {
//				definition.appendToComment(comment.text);
//				toDelete.add(comment);
//			}
//			// a comment right after the definition
//			else if (nextTo(lines, end, comment.begin)) {
//				definition.appendToComment(comment.text);
//				toDelete.add(comment);
//				break;
//			}
//			/*
//			 * Since the comments are in order and we parse the file's definitions in order too, if this
//			 * comment is not attached to this definition, then it won't be with a further definition either.
//			 * So, this is a module comment.
//			 */
//			else if (comment.end < begin) {
//				if (!bOnlyOneModuleComment) {
//					module.appendToComment(comment.text);
//					toDelete.add(comment);
//				}
//			} else if (comment.begin > end)
//				break;
//		}
//
//		for (Comment comment : toDelete) {
//			comments.remove(comment);
//		}
//
//	}
//
//	/**
//	 * Attach the first comment found inside the range going from <code>begin</code> to <code>end</code>.
//	 * This is useful for type constructors.
//	 */
//	private void attachCommentIn(OcamlDefinition definition, int begin, int end) {
//
//		Comment toDelete = null;
//
//		for (Comment comment : comments) {
//			if (comment.begin >= begin && comment.end <= end) {
//				definition.appendToComment(comment.text);
//				toDelete = comment;
//				break;
//			}
//		}
//
//		if (toDelete != null)
//			comments.remove(toDelete);
//	}
//
//	/**
//	 * Attach the first section comment before <code>offset</code> to <code>definition</code>.
//	 */
//	private void attachSectionComment(OcamlDefinition definition, int offset) {
//
//		/*
//		 * look for the first comment that is past the offset, and take the one before it
//		 */
//		Comment previousComment = null;
//		for (Comment comment : sectionComments) {
//			if (comment.begin > offset)
//				break;
//			previousComment = comment;
//		}
//
//		if (previousComment != null)
//			definition.setSectionComment(previousComment.text);
//
//	}
//
//	/**
//	 * @return true if there are less than two newlines between <code>offset1</code> and
//	 *         <code>offset2</code> in <code>text</code>
//	 */
//	private boolean nextTo(String text, int offset1, int offset2) {
//		if (offset2 < offset1)
//			return false;
//
//		boolean bNewline = false;
//		for (int i = offset1; i < offset2; i++) {
//			if (text.charAt(i) == '\n')
//				if (bNewline)
//					return false;
//				else
//					bNewline = true;
//		}
//
//		return true;
//	}
//
//	/**
//	 * This allows us to find comments that are placed after the semicolon, and put them back before the
//	 * semicolon where they belong, so that they can be extracted correctly. <br>
//	 * (for example: type t = {A; (**A comment*) B} )
//	 */
//	Pattern patternSemicolonComment = Pattern.compile(";\\s*?(\\(\\*\\*(.|\\n)*?\\*\\))");
//
//	/**
//	 * Parse a type definition, and extract its constructors if this type is a variant type.
//	 * 
//	 * @param lines
//	 *            the text in which the type definition appears
//	 * @param offset
//	 *            the offset of the body in the text
//	 * @param definitionBody
//	 *            the definition body to parse
//	 * @param parent
//	 *            the element (module) into which to add the type constructors found
//	 * @return true if the type was parsed
//	 */
//	private boolean parseType(String lines, int offset, String definitionBody, OcamlDefinition parent) {
//		Matcher matcherType = patternType.matcher(definitionBody);
//		// Matcher matcherRecordType = patternRecordType.matcher(definitionBody);
//
//		if (matcherType.find()) {
//			String typeName = matcherType.group(4);
//
//			// remove the 'type t =' from the definition
//			definitionBody = definitionBody.substring(matcherType.end(1));
//			offset += matcherType.end(1);
//
//			// count the number of spaces on the left, so as to keep the right offset after the trim()
//			int nSpacesLeft = 0;
//			for (int i = 0; i < definitionBody.length(); i++)
//				if (" \t\n".contains("" + definitionBody.charAt(i)))
//					nSpacesLeft++;
//				else
//					break;
//
//			// count the number of spaces on the right, so as to keep the right offset after the trim()
//			int nSpacesRight = 0;
//			for (int i = definitionBody.length() - 1; i >= 0; i--)
//				if (" \t\n".contains("" + definitionBody.charAt(i)))
//					nSpacesRight++;
//				else
//					break;
//
//			definitionBody = definitionBody.trim();
//			offset += nSpacesLeft;
//
//			if (definitionBody.contains("|")
//					|| (definitionBody.startsWith("{") && definitionBody.endsWith("}"))) {
//
//				boolean bRecordType = false;
//				if (definitionBody.startsWith("{") && definitionBody.endsWith("}")) {
//					bRecordType = true;
//					// there is a '}' on the right to be used as a delimiter after a trim()
//					nSpacesRight = 0;
//					definitionBody = definitionBody.substring(1, definitionBody.length() - 1);
//					offset++;
//				}
//
//				// get the constructors by cutting between the '|' or ';'
//				int lastIndex = -1;
//				int index = -1;
//
//				char separator = bRecordType ? ';' : '|';
//				// to get the last one
//				definitionBody = definitionBody + "  " + separator;
//
//				while ((index = definitionBody.indexOf(separator, index + 1)) != -1) {
//					String def = definitionBody.substring(lastIndex + 1, index);
//
//					String trimmedDef = def.trim();
//					if (trimmedDef.equals("")) {
//						lastIndex = index;
//						continue;
//					}
//
//					OcamlDefinition definition = new OcamlDefinition(OcamlDefinition.Type.DefConstructor);
//
//					int offsetBegin = offset + lastIndex + 1;
//					int offsetEnd = offset + index + 1;
//					/*
//					 * If that's the last constructor, we add the length that was truncated by the trim(), so
//					 * as to get the comment (knowing that comments are replaced by blanks from the start)
//					 */
//					if (index == definitionBody.length() - 1)
//						offsetEnd += nSpacesRight;
//					/*
//					 * the comments are after semicolons : parse all the spaces (that is: deleted comment)
//					 * until the next constructor
//					 */
//					else if (bRecordType) {
//						int length = definitionBody.length();
//						while (offsetEnd - offset < length
//								&& " \t\n".contains("" + definitionBody.charAt(offsetEnd - offset)))
//							offsetEnd++;
//					}
//
//					attachCommentIn(definition, offsetBegin, offsetEnd);
//
//					String[] words = trimmedDef.split("\\s+|:");
//					// the constructor name is the first word
//					if (words.length > 0) {
//						if (words[0].equals("mutable")) {
//							if (words.length > 1)
//								definition.setName(words[1]);
//						} else
//							definition.setName(words[0]);
//					}
//
//					definition.setBody(def);
//					definition.setFilename(filename);
//					definition.setParentName(typeName);
//
//					parent.addChild(definition);
//
//					lastIndex = index;
//				}
//				return true;
//			}
//		}
//		return false;
//	}
//
//	private Pattern patternVal = Pattern
//			.compile("(?:val|and)(?:(?: *\\(((?:\\w|')+) *\\) *:)|(?: *([^:]+):))");
//
//	/** Find the value name in the definition */
//	private OcamlDefinition parseVal(String body) {
//		OcamlDefinition def = new OcamlDefinition(Type.DefVal);
//		Matcher matcher = patternVal.matcher(body);
//
//		if (matcher.find()) {
//			if (matcher.group(1) != null)
//				def.setName(matcher.group(1));
//			else
//				def.setName(matcher.group(2));
//		}
//
//		return def;
//	}
//
//	private Pattern patternExternal = Pattern.compile("external(?:(?: *\\( *(\\w+) *\\) *:)|(?: *(.+) *:))");
//
//	/** Find the value name in the definition */
//	private OcamlDefinition parseExternal(String body) {
//		OcamlDefinition def = new OcamlDefinition(Type.DefExternal);
//		Matcher matcher = patternExternal.matcher(body);
//
//		if (matcher.find()) {
//			if (matcher.group(1) != null)
//				def.setName(matcher.group(1));
//			else
//				def.setName(matcher.group(2));
//		}
//
//		return def;
//	}
//
//}
