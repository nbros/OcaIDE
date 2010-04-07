package ocaml.editor.newFormatter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedList;

import ocaml.parser.ErrorReporting;
import beaver.Parser.Exception;

public class Test {
	public static void main(String[] args) {

		String doc = "let a = 2 in\na + 1;;";

		String[] lines = doc.split("\\n");

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
		final OcamlFormatterParser parser = new OcamlFormatterParser();

		try {
			Pos pos = (Pos) parser.parse(scanner);

			System.err.println("(" + pos.getStartLine() + ","
					+ pos.getStartColumn() + ") - (" + pos.getEndLine() + ","
					+ pos.getEndColumn() + ")");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (ErrorReporting.Error error : parser.errorReporting.errors) {
			System.err.println(error.message);
		}

//		System.err.println("before sorting");
//		for (IndentHint hint : parser.indentHints) {
//			System.err.println(hint);
//		}

		LinkedList<IndentHint> indentHints = new LinkedList<IndentHint>();
		indentHints.addAll(parser.indentHints);

		IndentHint.HintComparator hintComparator = new IndentHint.HintComparator();
		Collections.sort(indentHints, hintComparator);

		for (IndentHint hint : indentHints) {
			System.err.println(hint);
		}

		StringBuilder result = new StringBuilder();
		int indent = 0;

		for (int lineNumber = 0; lineNumber < lines.length; lineNumber++) {
			String line = lines[lineNumber];

			// look for the first real character
			int firstColumn;
			for (firstColumn = 0; firstColumn < line.length(); firstColumn++) {
				if (!Character.isWhitespace(line.charAt(firstColumn)))
					break;
			}

			while (!indentHints.isEmpty()) {
				IndentHint hint = indentHints.getFirst();
				if (hint.getLine() < lineNumber || hint.getLine() == lineNumber
						&& hint.getColumn() <= firstColumn) {
					indentHints.removeFirst();
					if (hint.getType().name().startsWith("INDENT")) {
						indent++;
					}
					if (hint.getType().name().startsWith("DEDENT")) {
						indent--;
					}
				} else
					break;

			}

			for (int k = 0; k < indent; k++)
				result.append('\t');
			result.append(line.trim() + "\n");

		}

		System.err.println(result);

	}

}
