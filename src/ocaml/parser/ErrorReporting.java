package ocaml.parser;

import java.util.ArrayList;

import beaver.Parser;
import beaver.Scanner;
import beaver.Symbol;

/**
 * Override the default error reporting behavior by just remembering whether there was an error. We don't need
 * error reporting in this parser, since this will be done by the Ocaml compiler.
 */
public class ErrorReporting extends Parser.Events {
	
	public class Error{
		public int lineStart, columnStart, lineEnd, columnEnd;
		public String message;
	}
	
	public ArrayList<Error> errors = new ArrayList<Error>();
	

	public int line, column;

	public void scannerError(Scanner.Exception e)
	{
		Error error = new Error();
		error.message = e.getMessage();
		
		if (e.line > 0)
		{
			error.lineStart = e.line;
			error.lineEnd = e.line;
			error.columnStart = e.column;
			error.columnEnd = e.column;
		}
		
		errors.add(error);
	}
	public void syntaxError(Symbol token)
	{
		Error error = new Error();
		error.message = "unexpected token" + ((token.value != null)? " \"" +  token.value + "\"" : "");
			
		
		error.lineStart = Symbol.getLine(token.getStart());
		error.columnStart = Symbol.getColumn(token.getStart());
		error.lineEnd = Symbol.getLine(token.getEnd());
		error.columnEnd = Symbol.getColumn(token.getEnd());
		
		errors.add(error);
	}
	public void unexpectedTokenRemoved(Symbol token)
	{
	}
	public void missingTokenInserted(Symbol token)
	{
	}
	public void misspelledTokenReplaced(Symbol token)
	{
	}
	public void errorPhraseRemoved(Symbol error)
	{
	}
}
