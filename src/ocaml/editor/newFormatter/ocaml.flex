// java -jar lib/JFlex.jar src/ocaml/parser/ocaml.flex

// This lexer is a duplicate from ocaml.parser.ocaml.flex,
// but it generates the good symbols for OcamlFormatterParser

package ocaml.editor.newFormatter;

import java.util.Stack;

import beaver.Symbol;
import beaver.Scanner;

import ocaml.editor.newFormatter.OcamlFormatterParser.Terminals;

%%

%{
	enum eStringsComments{IN_INITIAL, IN_STRING, IN_COMMENT};
	Stack<eStringsComments> stackStringsComments = new Stack<eStringsComments>();
%}

%public

%class OcamlScanner
%extends Scanner
%function nextToken
%type Symbol
%yylexthrow Scanner.Exception
%eofval{
	return new Symbol(Terminals.EOF, "end-of-file");
%eofval}

%line
%column

%state STRING		
%state COMMENT

Newline = \r|\n|\r\n
Blank = [ \t\f]
Lowercase = [a-z] | _ //|[\223-\246] |[\248-\255]|_
Uppercase = [A-Z] //|[\192-\214] |[\216-\222]
Identchar = [A-Z]|[a-z]|[0-9]/*|[\192-\214]|[\216-\246]|[\248-\255]*/|\'|\_
Symbolchar = [\!\$\%\&\*\+\-\.\/\:\<\=\>\?\@\^\|\~]
Dec_literal = [0-9] ([0-9]|_)*
Hex_literal = 0 [xX] [0-9A-Fa-f] [0-9A-Fa-f\_]*
Oct_literal = 0 [oO] [0-7] [0-7\_]*
Bin_literal = 0 [bB] [0-1] [0-1\_]*
Int_literal = {Dec_literal} | {Hex_literal} | {Oct_literal} | {Bin_literal}
Float_literal = [0-9][0-9\_]*("."[0-9\_]*)?([eE][+-]?[0-9][0-9\_]*)?

 
%%

<STRING> {
    \" { 
    	eStringsComments type = stackStringsComments.pop();
    	if(type == eStringsComments.IN_COMMENT)
    		yybegin(COMMENT);
    	else if(type == eStringsComments.IN_INITIAL){
    		yybegin(YYINITIAL);
    		return new Symbol(Terminals.STRING);
    	}
    }
    
    \\ {Newline} [ \t]* {}
    \\ [\\\'\"ntbr] {}
    \\ [0-9] [0-9] [0-9] {}
    \\ "x" [0-9a-fA-F] [0-9a-fA-F] {}
    \\ . {}
    {Newline} {}
    . {}
}

<COMMENT> {
 
    "*)" { 
    	eStringsComments type = stackStringsComments.pop();
    	if(type == eStringsComments.IN_COMMENT)
    		yybegin(COMMENT);
    	else
    		yybegin(YYINITIAL);
    		
    	//return new Symbol(Terminals.STRING);
    }

    \'\"\' {}

    \" { stackStringsComments.push(eStringsComments.IN_COMMENT); yybegin(STRING); }
    
    "(*" { stackStringsComments.push(eStringsComments.IN_COMMENT); yybegin(COMMENT); }
    
    {Newline} {}
    . {}
}

<YYINITIAL> {
    "and" { return new Symbol(Terminals.AND, yyline, yycolumn, yytext().length(), yytext()); }
    "as" { return new Symbol(Terminals.AS, yyline, yycolumn, yytext().length(), yytext()); }
    "assert" { return new Symbol(Terminals.ASSERT, yyline, yycolumn, yytext().length(), yytext()); }
    "begin" { return new Symbol(Terminals.BEGIN, yyline, yycolumn, yytext().length(), yytext()); }
    "class" { return new Symbol(Terminals.CLASS, yyline, yycolumn, yytext().length(), yytext()); }
    "constraint" { return new Symbol(Terminals.CONSTRAINT, yyline, yycolumn, yytext().length(), yytext()); }
    "do" { return new Symbol(Terminals.DO, yyline, yycolumn, yytext().length(), yytext()); }
    "done" { return new Symbol(Terminals.DONE, yyline, yycolumn, yytext().length(), yytext()); }
    "downto" { return new Symbol(Terminals.DOWNTO, yyline, yycolumn, yytext().length(), yytext()); }
    "else" { return new Symbol(Terminals.ELSE, yyline, yycolumn, yytext().length(), yytext()); }
    "end" { return new Symbol(Terminals.END, yyline, yycolumn, yytext().length(), yytext()); }
    "exception" { return new Symbol(Terminals.EXCEPTION, yyline, yycolumn, yytext().length(), yytext()); }
    "external" { return new Symbol(Terminals.EXTERNAL, yyline, yycolumn, yytext().length(), yytext()); }
    "false" { return new Symbol(Terminals.FALSE, yyline, yycolumn, yytext().length(), yytext()); }
    "for" { return new Symbol(Terminals.FOR, yyline, yycolumn, yytext().length(), yytext()); }
    "fun" { return new Symbol(Terminals.FUN, yyline, yycolumn, yytext().length(), yytext()); }
    "function" { return new Symbol(Terminals.FUNCTION, yyline, yycolumn, yytext().length(), yytext()); }
    "functor" { return new Symbol(Terminals.FUNCTOR, yyline, yycolumn, yytext().length(), yytext()); }
    "if" { return new Symbol(Terminals.IF, yyline, yycolumn, yytext().length(), yytext()); }
    "in" { return new Symbol(Terminals.IN, yyline, yycolumn, yytext().length(), yytext()); }
    "include" { return new Symbol(Terminals.INCLUDE, yyline, yycolumn, yytext().length(), yytext()); }
    "inherit" { return new Symbol(Terminals.INHERIT, yyline, yycolumn, yytext().length(), yytext()); }
    "initializer" { return new Symbol(Terminals.INITIALIZER, yyline, yycolumn, yytext().length(), yytext()); }
    "lazy" { return new Symbol(Terminals.LAZY, yyline, yycolumn, yytext().length(), yytext()); }
    "let" { return new Symbol(Terminals.LET, yyline, yycolumn, yytext().length(), yytext()); }
    "match" { return new Symbol(Terminals.MATCH, yyline, yycolumn, yytext().length(), yytext()); }
    "method" { return new Symbol(Terminals.METHOD, yyline, yycolumn, yytext().length(), yytext()); }
    "module" { return new Symbol(Terminals.MODULE, yyline, yycolumn, yytext().length(), yytext()); }
    "mutable" { return new Symbol(Terminals.MUTABLE, yyline, yycolumn, yytext().length(), yytext()); }
    "new" { return new Symbol(Terminals.NEW, yyline, yycolumn, yytext().length(), yytext()); }
    "object" { return new Symbol(Terminals.OBJECT, yyline, yycolumn, yytext().length(), yytext()); }
    "of" { return new Symbol(Terminals.OF, yyline, yycolumn, yytext().length(), yytext()); }
    "open" { return new Symbol(Terminals.OPEN, yyline, yycolumn, yytext().length(), yytext()); }
    "or" { return new Symbol(Terminals.OR, yyline, yycolumn, yytext().length(), yytext()); }
    "private" { return new Symbol(Terminals.PRIVATE, yyline, yycolumn, yytext().length(), yytext()); }
    "rec" { return new Symbol(Terminals.REC, yyline, yycolumn, yytext().length(), yytext()); }
    "sig" { return new Symbol(Terminals.SIG, yyline, yycolumn, yytext().length(), yytext()); }
    "struct" { return new Symbol(Terminals.STRUCT, yyline, yycolumn, yytext().length(), yytext()); }
    "then" { return new Symbol(Terminals.THEN, yyline, yycolumn, yytext().length(), yytext()); }
    "to" { return new Symbol(Terminals.TO, yyline, yycolumn, yytext().length(), yytext()); }
    "true" { return new Symbol(Terminals.TRUE, yyline, yycolumn, yytext().length(), yytext()); }
    "try" { return new Symbol(Terminals.TRY, yyline, yycolumn, yytext().length(), yytext()); }
    "type" { return new Symbol(Terminals.TYPE, yyline, yycolumn, yytext().length(), yytext()); }
    "val" { return new Symbol(Terminals.VAL, yyline, yycolumn, yytext().length(), yytext()); }
    "virtual" { return new Symbol(Terminals.VIRTUAL, yyline, yycolumn, yytext().length(), yytext()); }
    "when" { return new Symbol(Terminals.WHEN, yyline, yycolumn, yytext().length(), yytext()); }
    "while" { return new Symbol(Terminals.WHILE, yyline, yycolumn, yytext().length(), yytext()); }
    "with" { return new Symbol(Terminals.WITH, yyline, yycolumn, yytext().length(), yytext()); }
    "mod" { return new Symbol(Terminals.INFIXOP3, yyline, yycolumn, yytext().length(), yytext()); }
    "land" { return new Symbol(Terminals.INFIXOP3, yyline, yycolumn, yytext().length(), yytext()); }
    "lor" { return new Symbol(Terminals.INFIXOP3, yyline, yycolumn, yytext().length(), yytext()); }
    "lxor" { return new Symbol(Terminals.INFIXOP3, yyline, yycolumn, yytext().length(), yytext()); }
    "lsl" { return new Symbol(Terminals.INFIXOP4, yyline, yycolumn, yytext().length(), yytext()); }
    "lsr" { return new Symbol(Terminals.INFIXOP4, yyline, yycolumn, yytext().length(), yytext()); }
    "asr" { return new Symbol(Terminals.INFIXOP4, yyline, yycolumn, yytext().length(), yytext()); }
    

    {Newline} {}
    {Blank} {}

    "_" { return new Symbol(Terminals.UNDERSCORE, yyline, yycolumn, yytext().length(), yytext()); }
    "~" { return new Symbol(Terminals.TILDE, yyline, yycolumn, yytext().length(), yytext()); }
    "~" {Lowercase} {Identchar}* ":" { return new Symbol(Terminals.LABEL, yyline, yycolumn, yytext().length(), yytext().substring(1,yylength()-1)); }
    "?" { return new Symbol(Terminals.QUESTION, yyline, yycolumn, yytext().length(), yytext()); }
    "??" { }
    "?" {Lowercase} {Identchar}* ":" { return new Symbol(Terminals.OPTLABEL, yyline, yycolumn, yytext().length(), yytext().substring(1,yylength()-1)); }
    {Lowercase} {Identchar}* { return new Symbol(Terminals.LIDENT, yyline, yycolumn, yytext().length(), yytext()); }
    {Uppercase} {Identchar}* { return new Symbol(Terminals.UIDENT, yyline, yycolumn, yytext().length(), yytext()); }
    "=" { return new Symbol(Terminals.EQUAL, yyline, yycolumn, yytext().length(), yytext()); }
    {Int_literal} [lLn]? { return new Symbol(Terminals.INT, yyline, yycolumn, yytext().length(), yytext()); }
    {Float_literal} { return new Symbol(Terminals.FLOAT, yyline, yycolumn, yytext().length(), yytext()); }
    
    "\"" { stackStringsComments.push(eStringsComments.IN_INITIAL); yybegin(STRING); }
    
    "'" [^\\\'\r\n] "'" { return new Symbol(Terminals.CHAR, yyline, yycolumn, yytext().length(), yytext()); }
    
    "'\\" [\\\'\"ntbr] "'" { return new Symbol(Terminals.CHAR, yyline, yycolumn, yytext().length(), yytext()); }
    
    "'\\" [0-9][0-9][0-9] "'" { return new Symbol(Terminals.CHAR, yyline, yycolumn, yytext().length(), yytext()); }

    "'\\" "x" [0-9a-fA-F][0-9a-fA-F] "'" { return new Symbol(Terminals.CHAR, yyline, yycolumn, yytext().length(), yytext()); }
    
    "(*" { stackStringsComments.push(eStringsComments.IN_INITIAL); yybegin(COMMENT); }
    
    "#" [ \t]* [0-9]+ [ \t]* ("\"" [^\r\n\"] "\"")? [^\r\n]* {Newline} {}
  
   "#"  { return new Symbol(Terminals.SHARP, yyline, yycolumn, yytext().length(), yytext()); }
   "&"  { return new Symbol(Terminals.AMPERSAND, yyline, yycolumn, yytext().length(), yytext()); }
   "&&" { return new Symbol(Terminals.AMPERAMPER, yyline, yycolumn, yytext().length(), yytext()); }
   "`"  { return new Symbol(Terminals.BACKQUOTE, yyline, yycolumn, yytext().length(), yytext()); }
   "'"  { return new Symbol(Terminals.QUOTE, yyline, yycolumn, yytext().length(), yytext()); }
   "("  { return new Symbol(Terminals.LPAREN, yyline, yycolumn, yytext().length(), yytext()); }
   ")"  { return new Symbol(Terminals.RPAREN, yyline, yycolumn, yytext().length(), yytext()); }
   "*"  { return new Symbol(Terminals.STAR, yyline, yycolumn, yytext().length(), yytext()); }
   ","  { return new Symbol(Terminals.COMMA, yyline, yycolumn, yytext().length(), yytext()); }
   "->" { return new Symbol(Terminals.MINUSGREATER, yyline, yycolumn, yytext().length(), yytext()); }
   "."  { return new Symbol(Terminals.DOT, yyline, yycolumn, yytext().length(), yytext()); }
   ".." { return new Symbol(Terminals.DOTDOT, yyline, yycolumn, yytext().length(), yytext()); }
   ":"  { return new Symbol(Terminals.COLON, yyline, yycolumn, yytext().length(), yytext()); }
   "::" { return new Symbol(Terminals.COLONCOLON, yyline, yycolumn, yytext().length(), yytext()); }
   ":=" { return new Symbol(Terminals.COLONEQUAL, yyline, yycolumn, yytext().length(), yytext()); }
   ":>" { return new Symbol(Terminals.COLONGREATER, yyline, yycolumn, yytext().length(), yytext()); }
   ";"  { return new Symbol(Terminals.SEMI, yyline, yycolumn, yytext().length(), yytext()); }
   ";;" { return new Symbol(Terminals.SEMISEMI, yyline, yycolumn, yytext().length(), yytext()); }
   "<"  { return new Symbol(Terminals.LESS, yyline, yycolumn, yytext().length(), yytext()); }
   "<-" { return new Symbol(Terminals.LESSMINUS, yyline, yycolumn, yytext().length(), yytext()); }
   "="  { return new Symbol(Terminals.EQUAL, yyline, yycolumn, yytext().length(), yytext()); }
   "["  { return new Symbol(Terminals.LBRACKET, yyline, yycolumn, yytext().length(), yytext()); }
   "[|" { return new Symbol(Terminals.LBRACKETBAR, yyline, yycolumn, yytext().length(), yytext()); }
   "[<" { return new Symbol(Terminals.LBRACKETLESS, yyline, yycolumn, yytext().length(), yytext()); }
   "[>" { return new Symbol(Terminals.LBRACKETGREATER, yyline, yycolumn, yytext().length(), yytext()); }
   "]"  { return new Symbol(Terminals.RBRACKET, yyline, yycolumn, yytext().length(), yytext()); }
   "{"  { return new Symbol(Terminals.LBRACE, yyline, yycolumn, yytext().length(), yytext()); }
   "{<" { return new Symbol(Terminals.LBRACELESS, yyline, yycolumn, yytext().length(), yytext()); }
   "|"  { return new Symbol(Terminals.BAR, yyline, yycolumn, yytext().length(), yytext()); }
   "||" { return new Symbol(Terminals.BARBAR, yyline, yycolumn, yytext().length(), yytext()); }
   "|]" { return new Symbol(Terminals.BARRBRACKET, yyline, yycolumn, yytext().length(), yytext()); }
   ">"  { return new Symbol(Terminals.GREATER, yyline, yycolumn, yytext().length(), yytext()); }
// ">]" { return new Symbol(Terminals.GREATERRBRACKET); } // not used
   "}"  { return new Symbol(Terminals.RBRACE, yyline, yycolumn, yytext().length(), yytext()); }
   ">}" { return new Symbol(Terminals.GREATERRBRACE, yyline, yycolumn, yytext().length(), yytext()); }

   "!=" 
        { return new Symbol(Terminals.INFIXOP0, yyline, yycolumn, yytext().length(), yytext()); }
   "+"  { return new Symbol(Terminals.PLUS, yyline, yycolumn, yytext().length(), yytext()); }
   "-"  { return new Symbol(Terminals.MINUS, yyline, yycolumn, yytext().length(), yytext()); }
   "-." { return new Symbol(Terminals.MINUSDOT, yyline, yycolumn, yytext().length(), yytext()); }

   "!" {Symbolchar} *
            { return new Symbol(Terminals.PREFIXOP, yyline, yycolumn, yytext().length(), yytext()); }
   [\~\?] {Symbolchar} +
            { return new Symbol(Terminals.PREFIXOP, yyline, yycolumn, yytext().length(), yytext()); }
   [\=\<\>\|\&\$] {Symbolchar} *
            { return new Symbol(Terminals.INFIXOP0, yyline, yycolumn, yytext().length(), yytext()); }
   [\@\^] {Symbolchar} *
            { return new Symbol(Terminals.INFIXOP1, yyline, yycolumn, yytext().length(), yytext()); }
   [\+\-] {Symbolchar} *
            { return new Symbol(Terminals.INFIXOP2, yyline, yycolumn, yytext().length(), yytext()); }
   "\*\*" {Symbolchar} *
            { return new Symbol(Terminals.INFIXOP4, yyline, yycolumn, yytext().length(), yytext()); }
   [\*\/\%] {Symbolchar} *
            { return new Symbol(Terminals.INFIXOP3, yyline, yycolumn, yytext().length(), yytext()); }

    
    
    
    //";;" { return new Symbol(Terminals.SEMISEMI, yyline, yycolumn, yytext().length(), yytext()); }

}

