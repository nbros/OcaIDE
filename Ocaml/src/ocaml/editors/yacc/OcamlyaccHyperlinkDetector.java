package ocaml.editors.yacc;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

/** Creates hyper-links to jump to the definition of a token or a nonterminal */
public class OcamlyaccHyperlinkDetector implements IHyperlinkDetector {

	private OcamlyaccEditor ocamlyaccEditor;

	public OcamlyaccHyperlinkDetector(OcamlyaccEditor ocamlyaccEditor) {
		this.ocamlyaccEditor = ocamlyaccEditor;
		definitions = new ArrayList<Definition>();
	}

	// a pattern to match the definitions in an OCaml Yacc file
	private final Pattern patternDefinition = Pattern
			.compile("(?:^ *(\\w+) *:)|(?:^ *%(?:token|nonassoc|left|right) *(?:<.*?> *)?((?:\\w| )+))");

	private static String lastDoc = "";
	private static ArrayList<Definition> definitions = new ArrayList<Definition>();

	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer, IRegion region,
			boolean canShowMultipleHyperlinks) {
		String doc = textViewer.getDocument().get();

		int startLine;

		try {
			startLine = textViewer.getDocument().getLineOfOffset(region.getOffset());
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		int length = doc.length();

		int begin = -1;
		int end = -1;
		int offset = region.getOffset();

		for (begin = offset; begin >= 0; begin--) {
			char ch = doc.charAt(begin);

			if (!(Misc.isOcamlIdentifierChar(ch))) {
				begin++;
				break;
			}
		}

		for (end = offset; end < length - 1; end++) {
			char ch = doc.charAt(end);
			if (!(Misc.isOcamlIdentifierChar(ch)))
				break;
		}

		if (begin < 0 || end < 0 || begin > length - 1 || end > length || begin >= end)
			return null;

		String word = doc.substring(begin, end);

		parseDocument(doc);

		for (Definition def : definitions) {
			if (word.equals(def.getWord()) && def.getLine() != startLine) {

				final int fBegin = begin, fEnd = end;
				final int lineNumber = def.getLine();
				final String name = word;

				return new IHyperlink[] {

				new IHyperlink() {

					public void open() {
						try {
							IRegion region = textViewer.getDocument().getLineInformation(lineNumber);
							ocamlyaccEditor.selectAndReveal(region.getOffset(), region.getLength());
						} catch (BadLocationException e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}

					public String getTypeLabel() {
						return null;
					}

					public String getHyperlinkText() {
						return name;
					}

					public IRegion getHyperlinkRegion() {
						return new Region(fBegin, fEnd - fBegin);
					}

				}

				};

			}
		}

		return null;
	}

	private void parseDocument(String doc) {
		if (doc.equals(lastDoc))
			return;

		// System.err.println("parsing document");
		definitions.clear();

		String[] lines = doc.split("\\n");

		for (int l = 0; l < lines.length; l++) {
			String line = lines[l];
			Matcher matcher = patternDefinition.matcher(line);
			if (matcher.find()) {
				if (matcher.group(1) != null) {
					definitions.add(new Definition(l, matcher.group(1)));
				} else {
					/*
					 * The group number 2 matches all the tokens defined on the same line
					 */
					String[] defs = matcher.group(2).split(" +");
					for (String def : defs)
						definitions.add(new Definition(l, def));
				}

			}
		}

		lastDoc = doc;
	}
}

class Definition {
	public Definition(int line, String word) {
		this.line = line;
		this.word = word;
	}

	private final int line;
	private final String word;

	public String getWord() {
		return word;
	}

	public int getLine() {
		return line;
	}
}
