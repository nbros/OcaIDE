package ocaml.editor.syntaxcoloring;

/** The OCaml reserved language words */
public interface ILanguageWords {

	String[] keywords =
			{ "and", "as", "assert", "asr"/*?*/, "begin", "class", "constraint", "do", "done", "downto",
					"else", "end", "exception", "external", "false", "for", "fun", "function",
					"functor", "if", "in", "include", "inherit", "initializer", "land"/*?*/, "lazy",
					"let", "lor"/*?*/, "lsl"/*?*/, "lsr"/*?*/, "lxor"/*?*/, "match", "method", "mod", "module",
					"mutable", "new", "object", "of", "open", "or", "private", "rec", "sig",
					"struct", "then", "to", "true", "try", "type", "val", "virtual", "when",
					"while", "with"};

	/*
	 * String[] keywords = {"sig", "struct", "module", "type", "functor", "with", "and", "let",
	 * "in", "val", "method", "constraint", "class", "in", "inherit", "initializer", "let", "rec",
	 * "and", "begin", "object", "end", "as", "do", "ne", "wnto", "else", "for", "if", "let",
	 * "match", "mutable", "new", "parser", "private", "then", "when", "while", "with", "lazy",
	 * "virtual", "exception", "raise", "failwith", "assert", "fun", "function", "open", "external",
	 * "include"};
	 */
	//String[] constants = { "bidule", "as", "false", "and", "xor", "or", "mod", "not", "ref", "true", "unit" };
	//Object[] allWords = { keywords, constants };
}