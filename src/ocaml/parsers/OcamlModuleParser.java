//package ocaml.parsers;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import ocaml.OcamlPlugin;
//
///**
// * A parser for O'Caml modules (ml files) and interfaces (mli files). This is used to build an outline of the
// * code (mainly)
// */
//public class OcamlModuleParser {
//
//	Pattern patternLet = Pattern.compile("^\\s*let\\s+(rec\\s+)?((\\w|')+|(\\(.+?\\)))");
//
//	Pattern patternVal = Pattern.compile("^\\s*(val|value)\\s+(mutable\\s+)?(.+?)\\s*(:|=)");
//
//	// Pattern patternExternal = Pattern.compile("^\\s*external\\s+(.+?)\\s*:");
//	Pattern patternExternal = Pattern.compile("^\\s*external\\s+((\\w|')+|(\\(.+?\\)))");
//
//	Pattern patternType = Pattern.compile("(^\\s*type\\s+(.+?)\\s*=)|(^\\s*type\\s+(.+))");
//
//	Pattern patternAnd = Pattern.compile("^\\s*and\\W");
//
//	Pattern patternAndLet = Pattern.compile("^\\s*and\\s+((\\w|')+)");
//
//	Pattern patternAndType = Pattern.compile("^\\s*and\\s+(.+?)\\s*=");
//
//	/** module type aModule = */
//	private Pattern patternModuleType = Pattern.compile("^\\s*module\\s+type\\s+(\\w+?)\\s*=");
//
//	/** module aModule : */
//	private Pattern patternModule = Pattern.compile("^\\s*module\\s+(\\w+?)\\s*=");
//
//	/** module signature */
//	private Pattern patternModuleOther = Pattern.compile("^\\s*module\\s+(\\w+).*");
//
//	/** class definition */
//	private Pattern patternClass = Pattern.compile("^\\s*class\\s+(?:virtual\\s+)?(?:\\[.+?\\]\\s+)?(type\\s+)?(\\w+)");
//
//	/** class initializer */
//	private Pattern patternInitializer = Pattern.compile("^\\s*initializer\\W");
//
//	//private Pattern patternObject = Pattern.compile("^\\s*object\\s*(.*)");
//
//	private Pattern patternMethod = Pattern.compile("^\\s*method\\s+(?:private\\s+)?(?:virtual\\s+)?((\\w|')+)");
//
//	/** exception definition */
//	private Pattern patternException = Pattern.compile("^\\s*exception\\s+(\\w+)");
//
//	/** opening (or including, which is about the same as far as we're concerned) a module */
//	private Pattern patternOpen = Pattern.compile("^\\s*(open|include)\\s+(.*)");
//
//	/** Parse the document to extract definitions together with their line number */
//	public OcamlModuleDefinition parseFile(String doc) {
//
//		// delete all comments from the analysis
//		String cleanedDoc = clean(doc);
//
//		// split the document into lines
//		String[] lines = cleanedDoc.split("\\n");
//
//		// the root node of the definitions tree
//		OcamlModuleDefinition definition = null;
//
//		// parse the file
//		try {
//			definition = parseModule(lines);
//		} catch (Throwable e) {
//			// if there was a parsing error, we log it and we continue on to the next file
//			OcamlPlugin.logError("Error parsing module", e);
//			return null;
//		}
//
//		definition.setType(OcamlModuleDefinition.Type.DefModule);
//
//		// return the root node (remark: this node won't appear in the outline)
//		return definition;
//	}
//
//	/** the stack of definitions whose indentation is inferior to that of the current definition */
//	private LinkedList<OcamlModuleDefinition> stack;
//
//	/**
//	 * Parse the lines to extract definitions from them (they must start at the beginning of the line,
//	 * disregarding comments)
//	 */
//	public OcamlModuleDefinition parseModule(String[] lines) {
//		stack = new LinkedList<OcamlModuleDefinition>();
//
//		// put the root in the stack
//		OcamlModuleDefinition top = new OcamlModuleDefinition();
//		top.setIndent(-1);
//		stack.add(top);
//
//		// for each line
//		for (int i = 0; i < lines.length; i++) {
//			String line = lines[i];
//
//			// find the indentation of this line
//			int lineIndent = findIndent(line);
//
//			// if that's a "let"
//			Matcher matcher = patternLet.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(2).trim();
//
//				// to manage a special case in "pervasives.ml"
//				if (name.startsWith("(("))
//					name = name.substring(1);
//
//				// We remove all the more indented definitions from the stack
//				removeMoreIndented(lineIndent);
//
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefLet);
//				def.setName(name);
//
//				/*
//				 * Add this element as a child of that with immediately inferior indentation.
//				 */
//				stack.getLast().addChild(def);
//				// Add this element to the top of the stack
//				stack.addLast(def);
//				continue;
//			}
//
//			// If that's a type definition
//			matcher = patternType.matcher(line);
//			if (matcher.find()) {
//				String name = "";
//
//				if (matcher.group(2) != null)
//					name = matcher.group(2).trim();
//				else
//					name = matcher.group(4).trim();
//
//				if (name.endsWith(";;"))
//					name = name.substring(0, name.length() - 2);
//
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefType);
//				def.setName(name);
//
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//
//			matcher = patternModuleType.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefSig);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternModule.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefModule);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternModuleOther.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefModule);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternClass.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(2);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefClass);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			/*matcher = patternObject.matcher(line);
//			if (matcher.find()) {
//				String name = "object " + matcher.group(1);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefObject);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}*/
//			matcher = patternInitializer.matcher(line);
//			if (matcher.find()) {
//				String name = "initializer";
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefMethod);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternMethod.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefMethod);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternException.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1).trim();
//				if (name.endsWith(";;"))
//					name = name.substring(0, name.length() - 2);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefException);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternExternal.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(1).trim();
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefExternal);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//			matcher = patternOpen.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(2).trim();
//				// remove the trailing ";;"
//				if (name.endsWith(";;"))
//					name = name.substring(0, name.length() - 2);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.Open);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//
//			matcher = patternVal.matcher(line);
//			if (matcher.find()) {
//				String name = matcher.group(3);
//				removeMoreIndented(lineIndent);
//				OcamlModuleDefinition def = new OcamlModuleDefinition();
//				def.setLine(i);
//				def.setIndent(lineIndent);
//				def.setType(OcamlModuleDefinition.Type.DefLet);
//				def.setName(name);
//				stack.getLast().addChild(def);
//				stack.addLast(def);
//				continue;
//			}
//
//			/* "and" definition : we try to see if it refers to a "let" or a "type" */
//			matcher = patternAnd.matcher(line);
//			if (matcher.find()) {
//				// this function returns the last definition with the same indentation
//				OcamlModuleDefinition lastDef = removeMoreIndented(lineIndent);
//				// if that was a "let""
//				if (lastDef != null && lastDef.getType().equals(OcamlModuleDefinition.Type.DefLet)) {
//					matcher = patternAndLet.matcher(line);
//					if (matcher.find()) {
//						OcamlModuleDefinition def = new OcamlModuleDefinition();
//						def.setLine(i);
//						def.setIndent(lineIndent);
//						def.setType(OcamlModuleDefinition.Type.DefLet);
//
//						String name = matcher.group(1);
//						def.setName(name);
//						stack.getLast().addChild(def);
//						stack.addLast(def);
//						continue;
//					}
//				}
//				// if that was a "type"
//				if (lastDef != null && lastDef.getType().equals(OcamlModuleDefinition.Type.DefType)) {
//					matcher = patternAndType.matcher(line);
//					if (matcher.find()) {
//						OcamlModuleDefinition def = new OcamlModuleDefinition();
//						def.setLine(i);
//						def.setIndent(lineIndent);
//						def.setType(OcamlModuleDefinition.Type.DefType);
//
//						String name = matcher.group(1);
//						def.setName(name);
//						stack.getLast().addChild(def);
//						stack.addLast(def);
//						continue;
//					}
//				}
//			}
//		}
//		// return the root of the tree
//		return top;
//	}
//
//	/**
//	 * Delete from <code>stack</code> the definitions with an indentation greater or equal to
//	 * <code>indent</code>.
//	 * 
//	 * @return the last definition with indentation <code>indent</code>.
//	 */
//	private OcamlModuleDefinition removeMoreIndented(int indent) {
//		OcamlModuleDefinition sameIndent = null;
//
//		ArrayList<OcamlModuleDefinition> toRemove = new ArrayList<OcamlModuleDefinition>();
//		for (OcamlModuleDefinition def : stack)
//			if (def.getIndent() >= indent) {
//				if (def.getIndent() == indent)
//					sameIndent = def;
//				toRemove.add(def);
//			}
//
//		for (OcamlModuleDefinition def : toRemove)
//			stack.remove(def);
//
//		return sameIndent;
//	}
//
//	/** The size of a tab, in number of spaces */
//	private final int tabSize = ocaml.editors.OcamlEditor.getTabSize();
//
//	/** Get the indentation of a line, in number of spaces */
//	private int findIndent(String string) {
//		int indent = 0;
//		for (int i = 0; i < string.length(); i++) {
//			if (string.charAt(i) == ' ')
//				indent++;
//			else if (string.charAt(i) == '\t')
//				indent += tabSize;
//			else
//				break;
//		}
//		return indent;
//	}
//
//	/**
//	 * Remove the comments from the text, so that they won't be interpreted as definitions.
//	 */
//	private String clean(String doc) {
//		StringBuilder stringBuilder = new StringBuilder(doc);
//
//		boolean bParL = false;
//		boolean bStar = false;
//		boolean bInComment = false;
//		boolean bEscape = false;
//		int commentStart = 0;
//
//		int length = stringBuilder.length();
//
//		for (int i = 0; i < length; i++) {
//			char ch = stringBuilder.charAt(i);
//
//			if (ch == '(' && !bInComment)
//				bParL = true;
//			else if (ch == '*' && bParL) {
//				bInComment = true;
//				bParL = false;
//				commentStart = i + 1;
//			} else if (ch == '*' && bInComment)
//				bStar = true;
//			else if (bInComment && ((ch == ')' && bStar) || (i == length - 1))) {
//				bInComment = false;
//
//				// replace comments by spaces, while keeping newlines
//				for (int j = commentStart - 2; j < i + 1; j++)
//					if (stringBuilder.charAt(j) != '\n')
//						stringBuilder.setCharAt(j, ' ');
//
//				bStar = false;
//				bParL = false;
//			} else {
//				bParL = false;
//				bStar = false;
//
//				if (ch == '\\')
//					bEscape = !bEscape;
//				else
//					bEscape = false;
//			}
//		}
//
//		return stringBuilder.toString();
//	}
//}
