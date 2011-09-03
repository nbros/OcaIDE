package ocaml.editors.lex;

/** The reserved language words in an OCaml Yacc file */
public interface ILexLanguageWords {
	String[] keywords =
	{ "and", "as", "assert", "asr"/*?*/, "begin", "class", "constraint", "do", "done", "downto",
			"else", "end", "exception", "external", "false", "for", "fun", "function",
			"functor", "if", "in", "include", "inherit", "initializer", "land"/*?*/, "lazy",
			"let", "lor"/*?*/, "lsl"/*?*/, "lsr"/*?*/, "lxor"/*?*/, "match", "method", "mod", "module",
			"mutable", "new", "object", "of", "open", "or", "private", "rec", "sig",
			"struct", "then", "to", "true", "try", "type", "val", "virtual", "when",
			"while", "with", "rule", "parse", "shortest", "eof" };
}
