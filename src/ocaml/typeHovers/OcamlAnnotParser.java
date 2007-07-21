package ocaml.typeHovers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A parser for ".annot" files (that contain type informations related to the same-name .ml file, generated
 * when we pass the -dtypes option to the compiler).
 */
public class OcamlAnnotParser {

	private static final Pattern patternAnnot = Pattern
			.compile("\".*.\" (\\d+) (\\d+) (\\d+) \".*?\" (\\d+) (\\d+) (\\d+)\\ntype\\(\\n((?:.|\\n)*?)\\n\\)\\n");

	/** The type definitions cache */
	private static LinkedList<CachedTypeAnnotations> cache = new LinkedList<CachedTypeAnnotations>();

	/**
	 * Parse the type annotations file (.annot) <code>file</code> and create a list of annotations.
	 * 
	 * @param file
	 *            the annotations file to parse
	 * @param document
	 *            the document associated to the file whose annotations must be parsed
	 * 
	 * @return the type annotations found, or <code>null</code> if the file couldn't be read
	 * @throws BadLocationException
	 */
	public static TypeAnnotation[] parseFile(File file, IDocument document) throws BadLocationException {

		/* Table of entries to remove from the cache. */
		ArrayList<CachedTypeAnnotations> toRemove = new ArrayList<CachedTypeAnnotations>();
		TypeAnnotation[] found = null;

		// first, see if the informations are in the cache
		for (CachedTypeAnnotations info : cache) {
			if (info.sameAs(file)) {
				// the entry is in the cache and is still valid
				if (info.isMoreRecentThan(file))
					found = info.getAnnotations();
				// the entry in the cache is not valid anymore: we delete it from the cache
				else {
					toRemove.add(info);
				}
			}
		}

		// remove the stale entries from the cache
		for (CachedTypeAnnotations def : toRemove)
			cache.remove(def);
		// return the entry from the cache if we found it
		if (found != null) {
			return found;

		}

		if (!file.canRead())
			return null;

		final BufferedReader inputStream;

		try {
			inputStream = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		StringBuilder text = new StringBuilder();
		// read the file, line by line
		String line;
		try {
			while ((line = inputStream.readLine()) != null)
				text.append(line + "\n");
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return null;
		}

		ArrayList<TypeAnnotation> annotations = new ArrayList<TypeAnnotation>();

		// read all the type annotations from the file with a regex
		Matcher matcher = patternAnnot.matcher(text);

		while (matcher.find()) {
			/*
			 * Sometimes, the offset gets erroneously shifted. To correct it, we shift the offset back by the
			 * difference between the expected line offset and the offset retrieved.
			 */
			int beginLine = Integer.parseInt(matcher.group(1));
			int beginLineOffset = Integer.parseInt(matcher.group(2));
			int beginOffset = Integer.parseInt(matcher.group(3));

			int endLine = Integer.parseInt(matcher.group(4));
			int endLineOffset = Integer.parseInt(matcher.group(5));
			int endOffset = Integer.parseInt(matcher.group(6));

			beginOffset += document.getLineOffset(beginLine - 1) - beginLineOffset;
			endOffset += document.getLineOffset(endLine - 1) - endLineOffset;

			String type = matcher.group(7);

			TypeAnnotation annot = new TypeAnnotation(beginOffset, endOffset, type);
			annotations.add(annot);
		}

		TypeAnnotation[] typeAnnotations = annotations.toArray(new TypeAnnotation[0]);
		CachedTypeAnnotations cacheEntry = new CachedTypeAnnotations(file);
		for (TypeAnnotation t : typeAnnotations)
			cacheEntry.addAnnotation(t);

		cache.addFirst(cacheEntry);

		// return the table of annotations from the file
		return typeAnnotations;
	}
}
