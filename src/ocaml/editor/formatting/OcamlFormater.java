package ocaml.editor.formatting;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.preferences.PreferenceConstants;

/**
 * This class is responsible for formating O'Caml code. It indents code, adds spaces where they are missing,
 * delete those that are redundant, splits comments on several lines if they are too long, merge consecutive
 * comments if they are too short, turn multi-line comments into single-line comments, and remove embedded
 * "(*" and "*)" in comments (because they hinder the plug-in parsers).
 */
public class OcamlFormater {

	/** A semicolon not preceded by another semicolon, at the end of a line */
	private Pattern patternSequence = Pattern.compile("[^;];$");

	/**
	 * Words that when found at the end of a line prevent the following line from being unindented, and indent
	 * it.
	 */
	private final Pattern lineContinuatorsPattern = Pattern
			.compile("(\\Winitializer$)|(\\Wthen$)|(\\Welse$)|(\\Wdo$)|(\\Wbegin$)|(\\Wmodule$)|"
					+ "(\\Wstruct$)|(\\Wtry$)|(\\Wsig$)|(\\Wobject$)|(\\Wof$)|(=$)|(->$)|(\\{$)|(\\($)|(\\[$)");

	/** The beginning of a new definition : the indentation is decremented on the following line */
	private Pattern newDefitionStartPatterns = Pattern
			.compile("^initializer\\W|^inherit\\W|^val\\W|^method\\W|^type\\W|^exception\\W|^open\\W|^module\\W|^external\\W|^class\\W");

	/** A pattern to match keywords that must go in a pair */
	private Pattern patternBeginOrEnd = Pattern
			.compile("(\\Wobject\\W)|(\\Wstruct\\W)|(\\Wsig\\W)|(\\Wdone\\W)|(\\Wdo\\W)|(\\Wbegin\\W)|(\\Wend\\W)|\\(|\\)|\\{|\\}|\\[|\\]");

	private Pattern patternBegin = Pattern
			.compile("(\\Wobject\\W)|(\\Wstruct\\W)|(\\Wsig\\W)|(\\Wdo\\W)|(\\Wbegin\\W)|\\(|\\{|\\[");

	/**
	 * A pattern to match the keywords "let", "type", "and", and "in" which must have the same indentation
	 */
	private Pattern patternLetTypeAndIn = Pattern.compile("(\\Wlet\\W)|(\\Wtype\\W)|(\\Win\\W)|(\\Wand\\W)");

	private Pattern patternLet = Pattern.compile("(\\Wlet\\W)");

	private Pattern patternType = Pattern.compile("(\\Wtype\\W)");

	private Pattern patternAnd = Pattern.compile("(\\Wand\\W)");

	/**
	 * A pattern to match the "try" and "with" keywords that must have the same indentation. We filter out
	 * matching "match" and "with" keywords in the corresponding rule.
	 */
	private Pattern patternTryMatchWith = Pattern.compile("(\\Wtry\\W)|(\\Wmatch\\W)|(\\Wwith\\W)");

	private Pattern patternTry = Pattern.compile("(\\Wtry\\W)");

	private Pattern patternMatch = Pattern.compile("(\\Wmatch\\W)");

	/**
	 * A pattern to match the corresponding "if", "then" and "else" keywords, that must have the same
	 * indentation.
	 */
	private Pattern patternIfThenElse = Pattern.compile("(\\Wif\\W)|(\\Wthen\\W)|(\\Welse\\W)");

	private Pattern patternIf = Pattern.compile("(\\Wif\\W)");

	private Pattern patternThen = Pattern.compile("(\\Wthen\\W)");

	/**
	 * A pattern to match a comment. The "*?" quantifier is "reluctant", that is it doesn't match "(* *) (*
	 * *)" in a single match, and instead gives us two matches, one for each comment.
	 */
	private Pattern patternComment = Pattern.compile("\\(\\*.*?\\*\\)");

	/**
	 * A whole line comment (no code on this line). The group inside is the body of the comment (groups are
	 * what's inside parenthesis in regular expressions).
	 */
	private Pattern patternWholeLineComment = Pattern.compile("^\\s*\\(\\*(.*)\\*\\)\\s*$");

	/** a string (that can have embedded '\"' (escaped double quote character) ) */
	private Pattern patternString = Pattern.compile("\"(\\\\\"|.)*?\"");

	/** A pattern which matches O'Caml characters we want to remove because they hinder the parser */
	private Pattern patternAnnoyingChars = Pattern.compile("'\\('|'\\)'|'\\{'|'\\}|'\\['|'\\]'");

	/** A comment opened on this line, but not closed on this line (which we want to delete in the analysis) */
	private Pattern patternCommentEOL = Pattern.compile("\\(\\*.*");

	/** A comment closed on this line, but not opened on this line (to delete) */
	private Pattern patternCommentBOL = Pattern.compile(".*\\*\\)");

	// private Pattern patternCommentOpen = Pattern.compile("\\(\\*");

	/** Indentation levels stack, for keywords that go in a pair */
	private LinkedList<Integer> stackPairs = new LinkedList<Integer>();

	/** Indentation levels stack, for the "let" and "in" keywords */
	private LinkedList<Integer> stackLetIn = new LinkedList<Integer>();

	/** Indentation levels stack, for the "try" and "with" keywords */
	private LinkedList<Integer> stackTryWith = new LinkedList<Integer>();

	/** Indentation levels stack, for the "if", "then" and "else" keywords */
	private LinkedList<Integer> stackIfThenElse = new LinkedList<Integer>();

	/** Indentation levels stack, for the "|" (pipe) */
	private LinkedList<Integer> pipeIndent = new LinkedList<Integer>();

	/** Nesting levels stack, for the "|" */
	private LinkedList<Integer> pipeNestingLevel = new LinkedList<Integer>();

	/** Nesting levels stack for the "if" keyword (to manage correctly the "if" without "else" special case) */
	private LinkedList<Integer> ifNestingLevel = new LinkedList<Integer>();

	/** the current nesting level */
	private int nestingLevel;

	/** the characters to space */
	private final String spacing = "\\+|\\-|\\*|\\/|\\=|\\||&|\\:|\\<|\\>|\\{|\\}";

	/** the characters that must have a space before them */
	private Pattern patternSpacingLeft = Pattern.compile("((\\w|\"|\\))(" + spacing + "))");

	/** the characters that must have a space after them */
	private Pattern patternSpacingRight = Pattern.compile("((" + spacing + "|,|;)(\\(|\\w|\"))");

	/** At least two consecutive spaces */
	private Pattern patternSpaces = Pattern.compile("\\s\\s+");

	// private Pattern patternSpacesBeforeComment = Pattern.compile("^(\\s)*\\(\\*");

	/** true if the comment begins with "(*|" or "(**" => we don't format those */
	private boolean bDoNotFormatComment = false;

	/** Are we currently reading a multi-line comment */
	private boolean bInMultilineComment = false;

	/** The current line number */
	private int currentLine;

	/** the entire document split into lines */
	private String[] lines;

	/** The output of the formatter : formated document */
	private StringBuilder result;

	/** The current line being formated */
	private String line;

	/**
	 * Format the string <code>text</code> and return the result
	 * 
	 * @param text
	 *            the text to format
	 * @return the formatted text
	 * 
	 */
	public String format(String text) {

		boolean bInComment = false;

		clearStacks();

		result = new StringBuilder(text.length());

		// split the document into lines
		lines = text.split("\\n");

		if (lines.length == 0)
			return text;

		// to start, get the indentation of the first line
		int indent = getLineIndent(lines[0]);

		/** true if the next line cannot be a new definition */
		boolean bContinueLine = false;

		// number of consecutive blank lines
		int nConsecutiveBlankLines = 0;

		// TODO: preferences
		/* number of consecutive blank lines to keep */
		int nMaxBlankLines = 1;

		bDoNotFormatComment = false;

		bInMultilineComment = false;

		mainLoop: for (currentLine = 0; currentLine < lines.length; currentLine++) {

			line = lines[currentLine];
			String trimmed = line.trim();

			// delete redundant blank lines
			if (trimmed.equals("")) {
				nConsecutiveBlankLines++;
				if (nConsecutiveBlankLines > nMaxBlankLines)
					continue;
			} else
				nConsecutiveBlankLines = 0;

			/*
			 * reformat comments that start after some source code and continue onto the next line
			 */
			if (reformatMultilineComments()) {
				/*
				 * The reformatMultilineComments modified the current line number so that the formatter will
				 * take its modifications into account (when return value = true)
				 */
				/*
				 * we decrement the current line number because the loop will immediately be incremented after
				 * the "continue"
				 */
				currentLine--;
				continue mainLoop;
			}

			/*
			 * We find back code or a blank line: we can restart formatting the next comments
			 */
			if (!trimmed.startsWith("(*") && !bInComment)
				bDoNotFormatComment = false;

			// format the comment and change context
			if (!bDoNotFormatComment) {
				if (formatComment())
					continue mainLoop;
			}

			/*
			 * We delete everything inside comments and inside strings, so that the parser will not be
			 * hindered by false keywords. Here, we do it for single line comments and strings only. (we don't
			 * actually remove the comments and strings from the source code, only from the parser's input
			 * 
			 */

			/* Remove the contents of strings */
			Matcher matcher = patternString.matcher(trimmed);
			trimmed = matcher.replaceAll("\"\"");

			// delete single line comments from the parser's input
			matcher = patternComment.matcher(trimmed);
			trimmed = matcher.replaceAll("");

			// remove the characters '(', ')', '{', '}', '[' and ']' from the parser's input
			matcher = patternAnnoyingChars.matcher(trimmed);
			trimmed = matcher.replaceAll("''");

			// delete the end of a multi-line comment
			matcher = patternCommentEOL.matcher(trimmed);
			if (matcher.find()) {
				bInComment = true;
				trimmed = matcher.replaceFirst("");
			}

			// delete the beginning of a multi-line comment
			matcher = patternCommentBOL.matcher(trimmed);
			if (matcher.find()) {
				bInComment = false;
				// bInMultilineComment = false;
				trimmed = matcher.replaceFirst("");
			}

			// correct the spacing between characters on this line
			if (!bDoNotFormatComment)
				line = spacing(line);

			// if we are inside a multi-line comment, this line is empty of code
			if (bInComment)
				trimmed = "";

			trimmed = trimmed.trim();

			// if this line is empty, then we skip it
			if (trimmed.equals("")) {
				result.append(line + "\n");
				continue;
			}

			int prevNestingLevel = nestingLevel;

			// in "match-with" and type definitions
			if (startsWith(trimmed, "\\|") && !pipeIndent.isEmpty()) {
				indent = pipeIndent.getLast();
			}

			// cancel the indentation we just did (to align the "object", "sig" or "struct" with the "end")
			if ((trimmed.startsWith("object") || trimmed.startsWith("sig") || trimmed.startsWith("struct"))
					&& indent > 0)
				indent--;

			/*
			 * Parse all the groups of keywords. These functions return the indentation of the current line
			 * depending on the keywords found on this line.
			 */
			indent = parsePair(patternBeginOrEnd, patternBegin, trimmed, stackPairs, indent);
			indent = parseLetTypeAndIn(patternLetTypeAndIn, patternLet, patternType, patternAnd, trimmed,
					stackLetIn, indent);
			indent = parseTryMatchWith(patternTryMatchWith, patternTry, patternMatch, trimmed, stackTryWith,
					indent);
			indent = parseIfThenElse(patternIfThenElse, patternIf, patternThen, trimmed, indent);

			// *********************************************************
			// *** tests that determine the CURRENT line indentation ***
			// *********************************************************

			/*
			 * indent the first constructor (a constructor starts with a capital letter) or "[]" after a
			 * "with" or "function" if it does not have a leading "| ", so that it will get aligned with the
			 * following ones
			 */
			if (currentLine >= 1) {
				String prevLine = lines[currentLine - 1].trim();

				if (!trimmed.startsWith("|") && trimmed.length() > 0
						&& startsWith(trimmed, "\\(*(?:(?:\\[\\])|(?:[A-Z]))")) {
					if (endsWith(prevLine, "(?:\\Wwith)|(?:\\Wfunction)"))
						indent++;
					else if (startsWith(prevLine, "(?:type\\W)") && prevLine.endsWith("="))
						indent++;
					else if (bLastDefIsType && startsWith(prevLine, "(?:and\\W)") && prevLine.endsWith("="))
						indent++;

				}

			}

			// this is a new definition, if the previous line wasn't asking for a continuation
			if (startsWith(trimmed, "let\\W") && !bContinueLine) {
				if (nestingLevel == 0) {
					indent = 0;
					clearStacks();
				} else
					indent = prevNestingLevel;
			}

			/*
			 * we have two rules for the ";;" : one for the current line indentation and another for the
			 * indentation of the next line.
			 */
			else if (startsWith(trimmed, ";;")) {
				if (nestingLevel == 0) {
					indent = 0;
					clearStacks();
				} else
					indent = prevNestingLevel;
			}

			// the start of a new definition (indentation is decremented)
			matcher = newDefitionStartPatterns.matcher(trimmed);
			if (matcher.find()) {
				if (nestingLevel == 0) {
					indent = 0;
					clearStacks();
				} else
					indent = prevNestingLevel;
			}

			bContinueLine = false;

			// build the line with 'indent' heading tabs and append the result in 'result'
			for (int k = 0; k < indent; k++)
				result.append('\t');
			line = line.trim();
			result.append(line);

			/*
			 * do not add a newline after the last line, or else we would add a new line after each time the
			 * formatter is invoked
			 * 
			 */
			if (currentLine != lines.length - 1)
				result.append('\n');

			// ***********************************************************
			// *** tests that determine the FOLLOWING line indentation ***
			// ***********************************************************

			if (endsWith(trimmed, "\\Wwith")) {
				indent++;
				pipeIndent.addLast(indent);
				pipeNestingLevel.addLast(nestingLevel);
				bContinueLine = true;
			} else if (endsWith(trimmed, "\\Wfunction")) {
				indent++;
				pipeIndent.addLast(indent);
				pipeNestingLevel.addLast(nestingLevel);
				bContinueLine = true;
			} else if (endsWith(trimmed, "\\Wfun")) {
				indent++;
				pipeIndent.addLast(indent);
				pipeNestingLevel.addLast(nestingLevel);
				bContinueLine = true;
			} else if (endsWith(trimmed, "\\Win")) {
				if (OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_EDITOR_INDENT_IN)) {
					if (currentLine < lines.length - 1) {
						String nextLine = lines[currentLine + 1].trim();
						if (trimmed.equals("in") || !startsWith(nextLine, "let\\W")
								|| !startsWith(trimmed, "let\\W"))
							indent++;
					}
				}

				bContinueLine = true;
			} else if (endsWith(trimmed, ";;")) {
				if (nestingLevel == 0) {
					indent = 0;
					clearStacks();
				} else
					indent = prevNestingLevel;
			}

			/*
			 * Words that when they appear at the end of a line, mean that the current line must be continued
			 * on the following line, with an incremented indentation.
			 */
			matcher = lineContinuatorsPattern.matcher("$" + trimmed);
			if (matcher.find()) {
				indent++;
				bContinueLine = true;
			}

			// sequence semicolon
			matcher = patternSequence.matcher("$" + trimmed);
			if (matcher.find())
				bContinueLine = true;

			// type definition : we memorize the indentation for corresponding "|"
			if (startsWith(trimmed, "type\\W")) {
				pipeIndent.addLast(indent);
				pipeNestingLevel.addLast(nestingLevel);
			}

			// if this "and" is a type definition, then we memorize its indentation for the coming "|"
			if (startsWith(trimmed, "and\\W") && bLastDefIsType) {
				pipeIndent.addLast(indent);
				pipeNestingLevel.addLast(nestingLevel);
			}

			// indent the line after constructors in a match
			if (/* !bContinueLine && */startsWith(trimmed, "\\|\\s*\\(*(?:(?:\\[\\])|(?:[A-Z])|_)")) {
				indent++;
			}

		}

		// returns the formated source code
		return result.toString();
	}

	/**
	 * If the line has an opened but not closed comment, then we put back three lines into the pending lines:
	 * what's before the comment on its first line, the entire body of the comment (we delete embedded
	 * comments), and what's after the multi-line comment on its last line.
	 * 
	 * @return true if the loop must jump back
	 */
	private boolean reformatMultilineComments() {

		String curLine = line.trim();

		// abort the operation?
		boolean bAbort = false;
		// the level of nested comments
		int nestedCommentsLevel = 0;
		// a comment until the end of the line
		Matcher matcherCommentEOL = patternCommentEOL.matcher(curLine);
		if (matcherCommentEOL.find()) {

			if (matcherCommentEOL.start() + 2 < curLine.length()) {
				char firstChar = curLine.charAt(matcherCommentEOL.start() + 2);
				// this character means "do not reformat this comment"
				if (firstChar == '*' || firstChar == '|') {
					bDoNotFormatComment = true;
					bAbort = true;
				}
			}

			// check that this comment is not closed (or else, we would loop indefinitely)
			Matcher matcherComment = patternComment.matcher(curLine);
			while (matcherComment.find())
				if (matcherComment.start() == matcherCommentEOL.start())
					bAbort = true;

			// if we are inside a string, do nothing
			Matcher matcherString = patternString.matcher(curLine);
			while (matcherString.find()) {
				if ((matcherString.start() < matcherCommentEOL.start())
						&& (matcherCommentEOL.start() < matcherString.end()))
					bAbort = true;
			}

			if (!bAbort) {

				nestedCommentsLevel = 1;

				// look for the end of the comment
				Pattern patternCommentBeginEnd = Pattern.compile("(\\(\\*)|(\\*\\))");

				// all the lines of the comment, concatenated
				String commentLines = curLine.substring(matcherCommentEOL.start() + 2);

				// what's before the comment on its first line
				String beforeComment = curLine.substring(0, matcherCommentEOL.start());

				// what's left after the comment on its last line
				String afterComment = "";

				// the index of the current line
				int l;
				getWholeComment: for (l = currentLine; l < lines.length; l++) {

					String commentLine;
					// first line
					if (l == currentLine) {
						commentLine = commentLines;
						commentLines = "";
					} else
						commentLine = lines[l].trim();

					Matcher matcherCommentBeginEnd = patternCommentBeginEnd.matcher(commentLine);

					/*
					 * parse all the delimiters, and delete the nested ones, while keeping the body of the
					 * comment
					 */
					while (matcherCommentBeginEnd.find()) {

						// check that we are not inside a string
						Matcher matcherString2 = patternString.matcher(commentLine);
						boolean bInString = false;

						while (matcherString2.find()) {

							if (matcherString2.start() <= matcherCommentBeginEnd.start()
									&& matcherCommentBeginEnd.start() < matcherString2.end()) {
								bInString = true;
								break;
							}
						}
						if (!bInString) {

							if (matcherCommentBeginEnd.group().equals("(*"))
								nestedCommentsLevel++;
							else
								nestedCommentsLevel--;

							// delete the comment delimiter from the body of the comment
							if (nestedCommentsLevel != 0) {
								String before = commentLine.substring(0, matcherCommentBeginEnd.start());
								String after = commentLine.substring(matcherCommentBeginEnd.start() + 2);
								commentLine = before + after;
							}

							if (nestedCommentsLevel == 0) {

								commentLines = commentLines + " "
										+ commentLine.substring(0, matcherCommentBeginEnd.start());
								afterComment = commentLine.substring(matcherCommentBeginEnd.start() + 2);

								break getWholeComment;
							}

							// we modified the string: we have to restart the matcher
							matcherCommentBeginEnd = patternCommentBeginEnd.matcher(commentLine);
						}
					}
					commentLines = commentLines + " " + commentLine;
				}

				// if we are at the beginning, we must insert blank lines (or else we would access out of
				// bounds)
				if (l < 3) {
					String[] lines2 = new String[lines.length + 2];
					for (int k = 0; k < lines.length; k++)
						lines2[k + 2] = lines[k];
					lines = lines2;
					l += 2;
				}

				// if we didn't go through at least one iteration of the loop
				if (l >= lines.length)
					l--;

				/*
				 * Now, we put all the modified lines in the table, and we modify the current line so that the
				 * formatter will take the modifications into account.
				 */

				// Do not put blank lines
				if (beforeComment.trim().equals("") && afterComment.trim().equals("")) {
					lines[l] = "(*" + commentLines + "*)";
					currentLine = l;
					return true;
				} else if (beforeComment.trim().equals("")) {
					lines[l - 1] = "(*" + commentLines + "*)";
					lines[l] = afterComment;
					currentLine = l - 1;
					return true;
				} else if (afterComment.trim().equals("")) {
					lines[l - 1] = beforeComment;
					lines[l] = "(*" + commentLines + "*)";
					currentLine = l - 1;
					return true;
				} else {
					// the source code before the comment
					lines[l - 2] = beforeComment;
					// the body of the comment
					lines[l - 1] = "(*" + commentLines + "*)";
					// the source code after the comment
					lines[l] = afterComment;
					// the line on which we will continue formatting
					currentLine = l - 2;
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Formating comments: read words separated by spaces and split the comment onto several lines so that the
	 * length of each line is inferior to the width of the edit window.
	 * 
	 * <p>
	 * The formating of comments can be disabled in a selective manner, by starting the comment by "(*|"
	 * instead of just "(*". This allows the user to keep source code in a comment, or draw some ASCII-art.
	 * 
	 * @return true if the loop must jump back to continue formatting
	 */
	private boolean formatComment() {

		/* The list of remaining words to continue on the next line */
		LinkedList<String> commentWords = new LinkedList<String>();

		// TODO preferences
		int maxLineLength = 78;

		int nCommentLine = currentLine;

		bInMultilineComment = false;

		String firstCommentLine = line;

		/* Read all the following lines having a single comment */
		boolean bWholeLineComment;
		do {
			if (bDoNotFormatComment)
				break;

			bWholeLineComment = false;

			String commentLine = lines[nCommentLine];

			String trimmed2 = commentLine.trim();
			/*
			 * Matcher matcherString = patternString.matcher(trimmed); trimmed =
			 * matcherString.replaceAll("\"\"");
			 */

			/*
			 * Count the number of comments on the line, because patternWholeLineComment matches several
			 * comments in a single match. If there are several comments on the same line, then we leave them
			 * as is.
			 */
			Matcher matcherComment = patternComment.matcher(trimmed2);
			boolean bMoreThanOneComment = matcherComment.find() && matcherComment.find();

			if (!bMoreThanOneComment) {

				/*
				 * Read all the words (anything separated by spaces) of this comment, and add them to the
				 * list.
				 */
				Matcher matcherWholeLineComment = patternWholeLineComment.matcher(trimmed2);

				String commentBody = null;

				if (trimmed2.startsWith("(*") && !trimmed2.contains("*)"))
					bInMultilineComment = true;

				if (matcherWholeLineComment.find() || bInMultilineComment) {

					if (bInMultilineComment) {
						if (trimmed2.endsWith("*)")) {
							bInMultilineComment = false;
							commentBody = trimmed2.substring(0, trimmed2.length() - 2);
						}

						else if (trimmed2.startsWith("(*")) {
							commentBody = trimmed2.substring(2, trimmed2.length());
						} else
							commentBody = trimmed2;
					}

					if (commentBody == null)
						commentBody = matcherWholeLineComment.group(1);

					// Special character that means: do not format this comment
					// We don't format documentation comments either
					if (commentBody.startsWith("|") || commentBody.startsWith("*")) {
						bDoNotFormatComment = true;
						break;
					}

					String[] words = commentBody.split("\\s");

					// add the words to the list
					for (String w : words)
						if (!w.trim().equals(""))
							commentWords.addLast(w);

					bWholeLineComment = true;

					nCommentLine++;
					if (nCommentLine >= lines.length)
						break;
				}
			}

		} while (bWholeLineComment);

		int nLastCommentLine = nCommentLine - 1;

		/* If we found at least one comment */
		if (!commentWords.isEmpty()) {
			/*
			 * Get the indentation of the first comment line. The following ones will get the same
			 * indentation.
			 */
			int firstCommentLineIndent = getLineIndent(firstCommentLine);

			int currentOffset = 0;
			int tabSize = ocaml.editors.OcamlEditor.getTabSize();

			/* Now that we have a list of words, we spread it onto the following lines */

			// indentation in number of spaces from the beginning of the line
			int leadingSpace = 0;
			for (int j = 0; j < firstCommentLineIndent; j++) {
				result.append("\t");
				currentOffset += tabSize;
				leadingSpace += tabSize;
			}

			result.append("(* ");
			currentOffset += 3;

			int nCommentLines = 1;
			// for each word of the comment
			for (String word : commentWords) {
				// if the word fits into the remaining space on the line or if the line is empty
				if ((currentOffset + word.length() + 3 < maxLineLength || currentOffset == firstCommentLineIndent + 3)) {
					result.append(word + " ");
					currentOffset += word.length() + 1;
				}

				/* if the word doesn't fit on the remaining space on the line, then we create a new line */
				else {
					nCommentLines++;

					while (currentOffset++ < maxLineLength - 3)
						result.append(" ");

					result.append("*)\n");

					currentOffset = 0;
					for (int j = 0; j < firstCommentLineIndent; j++) {
						result.append("\t");
						currentOffset += tabSize;
					}

					result.append("(* " + word + " ");
					currentOffset += word.length() + 4;
				}
			}

			/*
			 * if there are several comment lines, we put the ending "*)" against the margin, so that the
			 * comment endings are all aligned
			 */
			if (nCommentLines != 1)
				while (currentOffset++ < maxLineLength - 3)
					result.append(" ");

			result.append("*)\n");

			/*
			 * We're done with this comment: analyze the remaining lines: continue at i (the +1 is done by the
			 * loop)
			 */
			currentLine = nLastCommentLine;
			// true means: jump back
			return true;
		}
		// continue normally (do not jump in the loop)
		return false;
	}

	/** Add or remove spaces on this line */
	private String spacing(String line) {

		/* Remove redundant spaces (keep only one space between two words) */
		int limit = 0;
		Matcher matcher = patternSpaces.matcher(line);

		String oldLine = line;

		remSpaces: while (matcher.find()) {

			int offset = matcher.start();

			/*
			 * if the spaces (or tabs) are at the beginning of a line or before a comment, then we keep them
			 */
			/*
			 * Matcher matcherSpacesBeforeComment = patternSpacesBeforeComment.matcher(line); if
			 * (matcherSpacesBeforeComment.find()) { if (offset < matcherSpacesBeforeComment.end()) continue
			 * remSpaces; }
			 */

			// if the spaces are before a comment, then we keep them
			String afterSpaces = line.substring(matcher.end());
			if (afterSpaces.startsWith("(*"))
				continue remSpaces;

			// If we are inside a string, then we skip this replacing
			Matcher matcherString = patternString.matcher(line);
			while (matcherString.find()) {
				if (matcherString.start() <= offset && offset < matcherString.end())
					continue remSpaces;
			}

			// If we are inside a comment, then we skip this replacing
			Matcher matcherComment = patternComment.matcher(line);
			while (matcherComment.find()) {
				if (matcherComment.start() <= offset && offset < matcherComment.end())
					continue remSpaces;
			}

			line = line.substring(0, offset) + " " + line.substring(matcher.end());

			// we have to reset the matcher, because we modified the line
			matcher = patternSpaces.matcher(line);

			// we put a limit, just in case we would get into an infinite loop
			if (limit++ > 10000) {
				OcamlPlugin.logError("Infinite loop detected in formatter during spacing (1): <<" + oldLine
						+ ">>");
				break;
			}

		}

		// remove spaces before commas
		line = line.replaceAll(" ,", ",");
		// remove spaces before semicolons
		line = line.replaceAll(" ;", ";");

		for (int nPattern = 0; nPattern <= 1; nPattern++) {
			Pattern patternSpacing;
			if (nPattern == 0)
				patternSpacing = patternSpacingRight;
			else
				patternSpacing = patternSpacingLeft;

			/* We put a limit to the number of replacements, just in case we would get into an infinite loop */
			limit = 0;
			// add spaces before some characters
			matcher = patternSpacing.matcher(line);

			oldLine = line;

			addSpaces: while (matcher.find()) {

				int offset = matcher.start() + 1;
				if (offset > line.length() - 1 || offset < 0) {
					ocaml.OcamlPlugin.logError("OcamlIndenter:format :  position invalide");
					break;
				}

				// if we are inside a string, then we skip this replacement
				Matcher matcherString = patternString.matcher(line);
				while (matcherString.find()) {
					if (matcherString.start() <= offset && offset < matcherString.end())
						continue addSpaces;
				}

				// Skip replacement if in comment
				Matcher matcherComment = patternComment.matcher(line);
				while (matcherComment.find()) {
					if (matcherComment.start() <= offset && offset < matcherComment.end())
						continue addSpaces;
				}

				line = line.substring(0, offset) + " " + line.substring(offset);

				// we have to reset the matcher, because we modified the line
				matcher = patternSpacing.matcher(line);
				if (limit++ > 10000) {
					OcamlPlugin.logError("Infinite loop detected in formatter during spacing (2): <<"
							+ oldLine + ">>");
					break;
				}
			}
		}

		return line;

	}

	/** Clear all the context stacks */
	private void clearStacks() {
		stackPairs.clear();
		stackLetIn.clear();
		stackTryWith.clear();
		stackIfThenElse.clear();
		pipeIndent.clear();
		pipeNestingLevel.clear();
		ifNestingLevel.clear();
		nestingLevel = 0;
	}

	/** Does the string <code>str</code> begin with the regex <code>start</code>? */
	private boolean startsWith(String str, String start) {
		str = str + "$";
		Pattern pattern = Pattern.compile("^" + start);
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}

	/** Does the string <code>str</code> end with the regex <code>end</code>? */
	private boolean endsWith(String str, String end) {
		str = "$" + str;
		Pattern pattern = Pattern.compile(end + "$");
		Matcher matcher = pattern.matcher(str);
		return matcher.find();
	}

	/**
	 * Do the parsing of the "if", "then", and "else" keywords.<br>
	 * The stackIfThenElcse variable is global because it is modified by parsePair to manage the "if without
	 * else" special case.<br>
	 * We are using a stack of indentation levels for the 'if' so that we can align the corresponding "then"
	 * and "else" correctly.
	 * 
	 */
	private int parseIfThenElse(Pattern patternIfThenElse, Pattern patternIf, Pattern patternThen,
			String str, int indent) {
		str = "$" + str + "$";

		int newIndent = indent;
		Matcher matcherIfThenElse = patternIfThenElse.matcher(str);

		// parse all the "if", "then" and "else" of the line
		while (matcherIfThenElse.find()) {
			Matcher matcherIf = patternIf.matcher(matcherIfThenElse.group());
			Matcher matcherThen = patternThen.matcher(matcherIfThenElse.group());

			/*
			 * We found a "if": keep the indentation of its line in the 'stackIfThenElse' stack.
			 * "ifNestingLevel" is the nesting level of this "if". As soon as we get out of the current
			 * lexical scope, we will remove the indentation of this "if" from the stack (in parsePair).
			 */
			if (matcherIf.find()) {
				ifNestingLevel.addLast(nestingLevel);
				stackIfThenElse.addLast(newIndent);
			}
			/*
			 * We found a "then": if the "then" keyword is the first of the line, then the indentation of the
			 * line will be the same as that of the corresponding "if".
			 */
			else if (matcherThen.find() && !stackIfThenElse.isEmpty()) {
				int i = stackIfThenElse.getLast();
				if (matcherIfThenElse.start() == 0)
					newIndent = i;
			}
			/*
			 * We found an "else": if it's the first keyword on the line then we indent it; and we remove it
			 * from the stack (an "if" has only one corresponding "else").
			 */
			else if (!stackIfThenElse.isEmpty()) {
				int i = stackIfThenElse.removeLast();
				if (matcherIfThenElse.start() == 0)
					newIndent = i;
				ifNestingLevel.removeLast();
			}
		}
		return newIndent;
	}

	/** Parse the "try with" and remove the "with" associated with a "match" (they are managed in another rule) */
	private int parseTryMatchWith(Pattern patternTryMatchWith, Pattern patternTry, Pattern patternMatch,
			String str, LinkedList<Integer> stack, int indent) {
		str = "$" + str + "$";

		int newIndent = indent;
		Matcher matcherTryWith = patternTryMatchWith.matcher(str);

		while (matcherTryWith.find()) {
			Matcher matcherTry = patternTry.matcher(matcherTryWith.group());
			Matcher matcherMatch = patternMatch.matcher(matcherTryWith.group());

			if (matcherTry.find())
				stack.addLast(newIndent);
			else if (matcherMatch.find()) {
				// a special value to indicate that this is a "match"
				stack.addLast(-999);
			} else if (!stack.isEmpty()) {
				int i = stack.removeLast();
				/*
				 * if the indentation in the stack has the special value "-999" then this with is associated
				 * with a "match" and not a "try", so we skip it.
				 */
				if (matcherTryWith.start() == 0 && i != -999)
					newIndent = i;
			}

		}

		return newIndent;
	}

	/** If the last definition is a type (instead of a "let"), then we don't manage the "and" in the same way */
	private boolean bLastDefIsType = false;

	/** The indentation of the last "type" encountered. */
	private int lastTypeIndent = 0;

	/** Do the parsing of the "let", "type", "and", and "in" keywords */
	private int parseLetTypeAndIn(Pattern patternLetTypeAndIn, Pattern patternLet, Pattern patternType,
			Pattern patternAnd, String str, LinkedList<Integer> stack, int indent) {
		str = "$" + str + "$";

		int newIndent = indent;
		Matcher matcherLetTypeAndIn = patternLetTypeAndIn.matcher(str);

		while (matcherLetTypeAndIn.find()) {
			Matcher matcherLet = patternLet.matcher(matcherLetTypeAndIn.group());
			Matcher matcherType = patternType.matcher(matcherLetTypeAndIn.group());
			Matcher matcherAnd = patternAnd.matcher(matcherLetTypeAndIn.group());

			if (matcherLet.find()) {
				stack.addLast(newIndent);
				bLastDefIsType = false;
			} else if (matcherType.find()) {
				bLastDefIsType = true;
				lastTypeIndent = newIndent;
			} else if (matcherAnd.find()) {

				if (bLastDefIsType)
					newIndent = lastTypeIndent;
				else {
					if (!stack.isEmpty())
						newIndent = stack.getLast();
					else
						newIndent = 0;
				}
			} else if (!stack.isEmpty()) {
				int i = stack.removeLast();
				// if the "in" keyword is the first of the line
				if (matcherLetTypeAndIn.start() == 0)
					newIndent = i;
			} else
				newIndent = 0;
		}
		return newIndent;
	}

	/**
	 * Parse a pair of delimiters (begin-end, do-done) and return the indentation of the current line.
	 */
	private int parsePair(Pattern patternBeginOrEnd, Pattern patternBegin, String str,
			LinkedList<Integer> stack, int indent) {
		str = "$" + str + "$";

		int newIndent = indent;
		Matcher matcherBeginEnd = patternBeginOrEnd.matcher(str);

		/*
		 * We use an index for the matcher, because of the "\W" surrounding keywords. If we didn't use this
		 * index, "begin(" would only match once (it should match twice: once for "begin" and once for "(" ).
		 */
		int matcherIndex = 0;

		while (matcherBeginEnd.find(matcherIndex)) {
			Matcher matcherBegin = patternBegin.matcher(matcherBeginEnd.group());

			if (matcherBegin.find()) {
				nestingLevel++;
				stack.addLast(newIndent);

			} else if (!stack.isEmpty()) {
				int i = stack.removeLast();

				// if it's the first keyword on the line
				if (matcherBeginEnd.start() <= 1)
					newIndent = i;

				// we remove one level of nesting for the "|"
				if (!pipeNestingLevel.isEmpty() && pipeNestingLevel.getLast() == nestingLevel) {
					pipeNestingLevel.removeLast();
					pipeIndent.removeLast();
				}

				// we are getting out of the lexical scope of an "if then else"
				// (this manages the special case of an "if" without an "else")
				if (!ifNestingLevel.isEmpty() && ifNestingLevel.getLast() == nestingLevel) {
					ifNestingLevel.removeLast();
					stackIfThenElse.removeLast();
				}

				nestingLevel--;
			}

			if (matcherBeginEnd.end() - matcherBeginEnd.start() > 1)
				matcherIndex = matcherBeginEnd.end() - 1;
			else
				matcherIndex = matcherBeginEnd.end();
		}
		return newIndent;
	}

	/** Get the indentation of a line */
	public static int getLineIndent(String string) {
		int tabSize = ocaml.editors.OcamlEditor.getTabSize();
		int indent = 0;
		for (int i = 0; i < string.length(); i++) {
			if (string.charAt(i) == ' ')
				indent++;
			else if (string.charAt(i) == '\t')
				indent += tabSize;
			else
				break;
		}

		return indent / tabSize;
	}

}
