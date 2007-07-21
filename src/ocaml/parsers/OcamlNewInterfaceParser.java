/**
 * TODO: use the real parser (ocaml.parser.OcamlParser) instead of the hand-built one
 * (ocaml.parsers.OcamlInterfaceParser).
 * <p>
 * The tricky thing is to associate the ocamldoc comments to the correct definitions.
 */

// package ocaml.parsers;
//
// import java.io.BufferedReader;
// import java.io.File;
// import java.io.FileNotFoundException;
// import java.io.FileReader;
// import java.io.IOException;
// import java.util.ArrayList;
// import java.util.LinkedList;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
//
// import ocaml.OcamlPlugin;
// import ocaml.parsers.OcamlDefinition.Type;
//
// /**
// * An O'Caml interface parser. Allows us to extract all the definitions from an O'Caml interface, together
// * with the documentation associated to each definition, so as to be able to display it to the user when
// * needed. <br>
// * This class makes use of the singleton design pattern, to cache all searches.
// */
// public class OcamlNewInterfaceParser {
//
// private OcamlNewInterfaceParser() {
//
// }
//
// /** Singleton instance of the parser (this allows us to use a cache) */
// static private OcamlNewInterfaceParser instance = null;
//
// /**
// * singleton design pattern
// *
// * @return the singleton instance of the O'Caml interface parser
// */
// public static OcamlNewInterfaceParser getInstance() {
// if (instance == null)
// instance = new OcamlNewInterfaceParser();
// return instance;
// }
//
// /** The cache of module definitions */
// private LinkedList<CachedMli> cache = new LinkedList<CachedMli>();
//
// /** ocamldoc section comment. We retrieve them and put them at the beginning of the following definitions
// */
// private Pattern patternSectionComment = Pattern.compile("\\A *\\{\\d+ (.*)\\}((.|\\n)*)\\z");
//
// /**
// * The comment intervals in the source code. They are used to avoid wrongly interpreting a keyword inside
// * a comment, and to attach comments to definitions.
// */
// private LinkedList<Comment> comments;
//
// /** Section comments */
// private LinkedList<Comment> sectionComments;
//
// private String filename = "";
//
// /**
// * Do we discard all module comments after the first? Normally, the module would get all the comments that
// * are not attached to anything else. But in general, these comments are numerous and useless. So, we
// * discard them. TODO user preference?
// */
// private final boolean bOnlyOneModuleComment = true;
//
// /**
// * Parse the O'Caml interface to extract definitions and ocamldoc comments attached to them, and put the
// * result in the cache.
// * <p>
// * We use a File instead of an IFile because the file can be out of the workspace (O'Caml library files,
// * typically)
// *
// * @param interfaceFile
// * the interface file to parse
// * @param bInProject
// * This file is it part of the project? (this is used to put a different icon on project
// * modules and standard library modules)
// *
// * @return The module definition or <code>null</code> if the file can't be read
// */
// public synchronized OcamlDefinition parseFile(File interfaceFile) {
//
// try {
// filename = interfaceFile.getCanonicalPath();
// } catch (IOException e) {
// OcamlPlugin.logError("ocaml plugin error", e);
// return null;
// }
//
// /*
// * Table of stale entries, that we will remove from the cache. We can't do that in the next loop,
// * because of concurrent access issues.
// */
// ArrayList<CachedMli> toRemove = new ArrayList<CachedMli>();
// OcamlDefinition found = null;
//
// // First, see if the informations are in the cache
// for (CachedMli def : cache) {
// if (def.sameAs(interfaceFile)) {
// // The entry is in the cache, and is still valid
// if (def.isMoreRecentThan(interfaceFile))
// found = def.getModuleDefinition();
// /*
// * The cache entry is not valid anymore: we put it in the list of entries to delete from the
// * cache
// */
// else {
// toRemove.add(def);
// }
// }
// }
//
// // remove the stale entries from the cache
// for (CachedMli def : toRemove)
// cache.remove(def);
// // return the cache entry (if we found one)
// if (found != null) {
// return found;
//
// }
//
// if (!interfaceFile.canRead())
// return null;
// final BufferedReader inputStream;
//
// try {
// inputStream = new BufferedReader(new FileReader(interfaceFile));
// } catch (FileNotFoundException e) {
// OcamlPlugin.logError("ocaml plugin error", e);
// return null;
// }
//
// StringBuilder sbLines = new StringBuilder();
// // read the file, line by line
// {
// String line;
// try {
// while ((line = inputStream.readLine()) != null)
// sbLines.append(line + "\n");
// } catch (IOException e) {
// OcamlPlugin.logError("ocaml plugin error", e);
// return null;
// }
// }
//
// String lines = sbLines.toString();
//
// String moduleName = interfaceFile.getName();
// // remove the extension
// if(moduleName.endsWith(".mli"))
// moduleName = moduleName.substring(0, moduleName.length() - 4);
// else if(moduleName.endsWith(".ml"))
// moduleName = moduleName.substring(0, moduleName.length() - 3);
//		
// // capitalize the first letter
// if (moduleName.length() > 0)
// moduleName = Character.toUpperCase(moduleName.charAt(0)) + moduleName.substring(1);
//
//
//		
// OcamlDefinition definition = null;
// // parse the .mli interface file or the .ml module file
// try {
// definition = parseModule(lines, moduleName);
// } catch (Throwable e) {
// // if there was a parsing error, we log it and we continue on to the next file
// OcamlPlugin.logError("Error parsing '" + moduleName + "'", e);
// OcamlDefinition def = new OcamlDefinition(Type.DefModule);
// def.setName("<parser error:" + moduleName + ">");
// def.setComment("The parsing of this module lead to the following error: " + e.toString());
// cache.addFirst(new CachedMli(interfaceFile, def));
// return def;
// }
//
// definition.setFilename(filename);
// definition.setBody("module " + moduleName);
//
// // put the entry into the cache
// cache.addFirst(new CachedMli(interfaceFile, definition));
// /*
// * Return the module definition that contains all this module's definitions (recursively)
// */
// return definition;
// }
//
//
// /**
// * An ocamldoc comment: beginning, end, and body. This is used to associate ocamldoc comments with the
// * corresponding definitions.
// */
// private class Comment {
// public Comment(int begin, int end, String text) {
// this.begin = begin;
// this.end = end;
// this.text = text;
// }
//
// int begin;
//
// int end;
//
// String text;
// }
//
// /**
// * Look for all the comment intervals in the text, and replace them by spaces. Keep all the comments in
// * table <code>comments</code>. Look also for the section comments and put them in the
// * <code>sectionComments</code> table.
// */
// private String parseComments(String lines) {
//
// StringBuilder result = new StringBuilder(lines);
// comments = new LinkedList<Comment>();
// sectionComments = new LinkedList<Comment>();
//
// boolean bParL = false;
// boolean bStar = false;
// boolean bInComment = false;
// boolean bEscape = false;
// int commentStart = 0;
// int codeNestingLevel = 0;
// boolean bInOcamldocComment = false;
//
// for (int i = 0; i < lines.length(); i++) {
// char ch = lines.charAt(i);
// if (ch == '(' && !bInComment)
// bParL = true;
// else if (ch == '*' && bParL) {
// bInComment = true;
// bParL = false;
// commentStart = i + 1;
// bInOcamldocComment = false;
// } else if (ch == '*' && i == commentStart) {
// bInOcamldocComment = true;
// } else if (ch == '*' && bInComment && codeNestingLevel == 0)
// bStar = true;
// else if (ch == ')' && bStar && bInComment && codeNestingLevel == 0) {
// bInComment = false;
//
// // ocamldoc comment (the normal comments are not useful to us)
// if (commentStart + 1 < lines.length() && lines.charAt(commentStart) == '*'
// && lines.charAt(commentStart + 1) != '*') {
//
// String body = lines.substring(commentStart + 1, i - 1);
// Matcher matcherSectionComment = patternSectionComment.matcher(body);
// if (matcherSectionComment.find()) {
// String section = matcherSectionComment.group(1) + "\n"
// + matcherSectionComment.group(2);
// sectionComments.add(new Comment(commentStart + 1, i - 1, section.trim()));
// } else
// comments.add(new Comment(commentStart + 1, i - 1, body));
// }
//
// // replace the comment by spaces (so as to preserve the offsets)
// for (int j = commentStart - 2; j < i + 1; j++)
// result.setCharAt(j, ' ');
//
// bStar = false;
// bParL = false;
// } else {
// bParL = false;
// bStar = false;
//
// if (ch == '\\')
// bEscape = !bEscape;
// else if (ch == '[' && !bEscape && bInOcamldocComment)
// codeNestingLevel++;
// else if (ch == ']' && !bEscape && codeNestingLevel > 0 && bInOcamldocComment)
// codeNestingLevel--;
// else
// bEscape = false;
// }
// }
//
// if(lines.startsWith("\n\n(*abc*)"))
// System.err.println(result.toString());
//			
//			
// // return the text without the comments
// return result.toString();
// }
//
// /**
// * Retrieve the comment attached to the definition that spans from <code>begin</code> to
// * <code>end</code>. Associate this comment to <code>definition</code> and delete it from the list.
// * During the parsing, attach module comments to <code>module</code>.
// */
// private void attachComment(OcamlDefinition definition, OcamlDefinition module, String lines, int begin,
// int end) {
//
// end--;
// if (end < 0)
// end = 0;
// if (end > lines.length() - 1)
// end = lines.length() - 1;
//
// // discard the blanks at the beginning of the definition
// while (" \n\t\r".contains("" + lines.charAt(end)) && end > 0)
// end--;
// // end = the first character just after the end of the definition
// end++;
//
// ArrayList<Comment> toDelete = new ArrayList<Comment>();
//
// for (Comment comment : comments) {
// // a comment right before the definition
// if (nextTo(lines, comment.end, begin)) {
// definition.appendToComment(comment.text);
// toDelete.add(comment);
// }
// // a comment right after the definition
// else if (nextTo(lines, end, comment.begin)) {
// definition.appendToComment(comment.text);
// toDelete.add(comment);
// break;
// }
// /*
// * Since the comments are in order and we parse the file's definitions in order too, if this
// * comment is not attached to this definition, then it won't be with a further definition either.
// * So, this is a module comment.
// */
// else if (comment.end < begin) {
// if (!bOnlyOneModuleComment) {
// module.appendToComment(comment.text);
// toDelete.add(comment);
// }
// } else if (comment.begin > end)
// break;
// }
//
// for (Comment comment : toDelete) {
// comments.remove(comment);
// }
//
// }
//
// /**
// * Attach the first comment found inside the range going from <code>begin</code> to <code>end</code>.
// * This is useful for type constructors.
// */
// private void attachCommentIn(OcamlDefinition definition, int begin, int end) {
//
// Comment toDelete = null;
//
// for (Comment comment : comments) {
// if (comment.begin >= begin && comment.end <= end) {
// definition.appendToComment(comment.text);
// toDelete = comment;
// break;
// }
// }
//
// if (toDelete != null)
// comments.remove(toDelete);
// }
//
// /**
// * Attach the first section comment before <code>offset</code> to <code>definition</code>.
// */
// private void attachSectionComment(OcamlDefinition definition, int offset) {
//
// /*
// * look for the first comment that is past the offset, and take the one before it
// */
// Comment previousComment = null;
// for (Comment comment : sectionComments) {
// if (comment.begin > offset)
// break;
// previousComment = comment;
// }
//
// if (previousComment != null)
// definition.setSectionComment(previousComment.text);
//
// }
//
// /**
// * @return true if there are less than two newlines between <code>offset1</code> and
// * <code>offset2</code> in <code>text</code>
// */
// private boolean nextTo(String text, int offset1, int offset2) {
// if (offset2 < offset1)
// return false;
//
// boolean bNewline = false;
// for (int i = offset1; i < offset2; i++) {
// if (text.charAt(i) == '\n')
// if (bNewline)
// return false;
// else
// bNewline = true;
// }
//
// return true;
// }
// }
