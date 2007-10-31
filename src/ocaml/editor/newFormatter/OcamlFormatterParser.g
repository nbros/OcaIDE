// java -jar ~/Desktop/lib/beaver.jar -t -c src/ocaml/parser/OcamlParser.g 


%package "ocaml.editor.newFormatter";

%import "java.util.ArrayList";
%import "ocaml.parser.ErrorReporting";

%embed {:
	public ErrorReporting errorReporting; 
	public ArrayList<IndentHint> indentHints = new ArrayList<IndentHint>();
	
	
	private void addHint(IndentHint.Type type, int indentPos, int dedentPos)
	{
    	IndentHint hint1 = new IndentHint( type, true, indentPos ); 
    	IndentHint hint2 = new IndentHint( type, false, dedentPos );
    	
    	hint1.counterpart = hint2;
    	hint2.counterpart = hint1;
    	indentHints.add(hint1);
    	indentHints.add(hint2);
    }
	
:};

%init {:
	// override the default error reporting: do not print errors on stderr
	this.report = errorReporting = new ErrorReporting();
:};

/* Tokens */

%terminals AMPERAMPER;
%terminals AMPERSAND;
%terminals AND;
%terminals AS;
%terminals ASSERT;
%terminals BACKQUOTE;
%terminals BAR;
%terminals BARBAR;
%terminals BARRBRACKET;
%terminals BEGIN;
%terminals CHAR;
%terminals CLASS;
%terminals COLON;
%terminals COLONCOLON;
%terminals COLONEQUAL;
%terminals COLONGREATER;
%terminals COMMA;
%terminals CONSTRAINT;
%terminals DO;
%terminals DONE;
%terminals DOT;
%terminals DOTDOT;
%terminals DOWNTO;
%terminals ELSE;
%terminals END;
//%terminals EndOF;
%terminals EQUAL;
%terminals EXCEPTION;
%terminals EXTERNAL;
%terminals FALSE;
%terminals FLOAT;
%terminals FOR;
%terminals FUN;
%terminals FUNCTION;
%terminals FUNCTOR;
%terminals GREATER;
%terminals GREATERRBRACE;
//%terminals GREATERRBRACKET;
%terminals IF;
%terminals IN;
%terminals INCLUDE;
%terminals INFIXOP0;
%terminals INFIXOP1;
%terminals INFIXOP2;
%terminals INFIXOP3;
%terminals INFIXOP4;
%terminals INHERIT;
%terminals INITIALIZER;
%terminals INT;
%terminals INT32;
%terminals INT64;
%terminals LABEL;
%terminals LAZY;
%terminals LBRACE;
%terminals LBRACELESS;
%terminals LBRACKET;
%terminals LBRACKETBAR;
%terminals LBRACKETLESS;
%terminals LBRACKETGREATER;
%terminals LESS;
%terminals LESSMINUS;
%terminals LET;
%terminals LIDENT;
%terminals LPAREN;
%terminals MATCH;
%terminals METHOD;
%terminals MINUS;
%terminals MINUSDOT;
%terminals MINUSGREATER;
%terminals MODULE;
%terminals MUTABLE;
%terminals NATIVEINT;
%terminals NEW;
%terminals OBJECT;
%terminals OF;
%terminals OPEN;
%terminals OPTLABEL;
%terminals OR;
%terminals PLUS;
%terminals PREFIXOP;
%terminals PRIVATE;
%terminals QUESTION;
//%terminals QUESTIONQUESTION;
%terminals QUOTE;
%terminals RBRACE;
%terminals RBRACKET;
%terminals REC;
%terminals RPAREN;
%terminals SEMI;
%terminals SEMISEMI;
%terminals SHARP;
%terminals SIG;
%terminals STAR;
%terminals STRING;
%terminals STRUCT;
%terminals THEN;
%terminals TILDE;
%terminals TO;
%terminals TRUE;
%terminals TRY;
%terminals TYPE;
%terminals UIDENT;
%terminals UNDERSCORE;
%terminals VAL;
%terminals VIRTUAL;
%terminals WHEN;
%terminals WHILE;
%terminals WITH;














%nonassoc BACKQUOTE, BEGIN, CHAR, FALSE, FLOAT, INT, INT32, INT64, LBRACE, LBRACELESS, LBRACKET, LBRACKETBAR, LIDENT, LPAREN, NEW, NATIVEINT, PREFIXOP, STRING, TRUE, UIDENT;
%nonassoc DOT;
%nonassoc below_DOT;
%nonassoc SHARP;
%nonassoc below_SHARP;
%nonassoc prec_constr_appl;
%nonassoc prec_constant_constructor;
%nonassoc prec_unary_minus;
%right    INFIXOP4;
%left     INFIXOP3, STAR;
%left     INFIXOP2, PLUS, MINUS, MINUSDOT;
%right    COLONCOLON;
%right    INFIXOP1;
%left     INFIXOP0, EQUAL, LESS, GREATER;
%nonassoc below_EQUAL;
%right    AMPERSAND, AMPERAMPER;
%right    OR, BARBAR;
%right    MINUSGREATER;
%left     COMMA;
%nonassoc below_COMMA;
%left     BAR;
%nonassoc AS;
%right    COLONEQUAL;
%nonassoc LESSMINUS;
%nonassoc ELSE;
%nonassoc THEN;
%nonassoc AND;
%nonassoc FUNCTION, WITH;
%nonassoc below_WITH;
%nonassoc LET;
%nonassoc SEMI;
%nonassoc below_SEMI;
%nonassoc IN;


%goal implementation;
%goal interfaces;
%goal toplevel_phrase;
%goal use_file;


implementation =
    structure.s    
    {: 
    	return s;
    :}
;

interfaces =
    signature.s                  /*      { ArrayList.$1 }*/
    {: 
    	return s;
   	:}
;


toplevel_phrase=
    top_structure.t SEMISEMI.s             /*{ Ptop_def $1 }*/
    {: return new Pos(t, s); :}
  | seq_expr.a SEMISEMI.b                    /*{ Ptop_def[ghstrexp $1] }*/
    {: return new Pos(a, b); :}
  | toplevel_directive.a SEMISEMI.b          /*{ $1 }*/
    {: return new Pos(a, b); :}
  
;

top_structure=
    structure_item.i                       /* { [$1] } */
    {: return i; :}
  | structure_item.a top_structure.b         /* { $1 :: $2 } */
  	{: return new Pos(a,b); :}
;
use_file=
    use_file_tail.a                        /* { $1 } */
    {: return a; :}
  | seq_expr.a use_file_tail.b               /* { Ptop_def[ghstrexp $1] :: $2 } */
  	{: return new Pos(a,b); :}
;
use_file_tail=
    SEMISEMI.s                                     /* { [] } */
  	{: return new Pos(s); :}
  | SEMISEMI.s seq_expr.a use_file_tail.b             /* { Ptop_def[ghstrexp $2] :: $3 } */
  	{: return new Pos(s, b); :}
  | SEMISEMI.s structure_item.a use_file_tail.b       /* { Ptop_def[$2] :: $3 } */
  	{: return new Pos(s, b); :}
  | SEMISEMI.s toplevel_directive.a use_file_tail.b   /* { $2 :: $3 } */
  	{: return new Pos(s, b); :}
  | structure_item.a use_file_tail.b                /* { Ptop_def[$1] :: $2 } */
  	{: return new Pos(a, b); :}
  | toplevel_directive.a use_file_tail.b            /* { $1 :: $2 } */
  	{: return new Pos(a, b); :}
;

/* Module expressions */

module_expr=
    mod_longident.a
    {: return a; :}
  | STRUCT.s structure.b END.e
    {: 
    	addHint(IndentHint.Type.STRUCT, s.getEnd(), e.getStart());
    	return new Pos(s, e);
    :}

  | FUNCTOR.a LPAREN UIDENT.i COLON module_type RPAREN MINUSGREATER.c module_expr.b
    {: 
    	addHint(IndentHint.Type.FUNCTOR, b.getStart(), b.getEnd());
		return new Pos(a, b);
    :}
  | module_expr.a LPAREN module_expr RPAREN.b
  	{: return new Pos(a, b); :}
  | LPAREN.a module_expr COLON module_type RPAREN.b
  	{: return new Pos(a, b); :}
  | LPAREN.a module_expr RPAREN.b
  	{: return new Pos(a, b); :}
;

structure=
    structure_tail.s  
    {: 
    	return s;
    :}
  | seq_expr.a structure_tail.b   
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return a;

    	 
    :}
;

structure_tail=
    /* empty */  
    {: return Pos.NONE; :}
  | SEMISEMI.s     
    {: return new Pos(s); :}
  | SEMISEMI.a seq_expr.c structure_tail.b 
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);
    		 
    :}
  | SEMISEMI.a structure_item.c structure_tail.b
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);
    :}
  | structure_item.a structure_tail.b         
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return a;
    :}
;

structure_item=
    LET.a rec_flag let_bindings.b 
	{: 
		return new Pos(a, b); 
	:}
  | EXTERNAL.a val_ident_colon core_type EQUAL.c primitive_declaration.b
    {: 
    	addHint(IndentHint.Type.DEF, c.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
  | TYPE.a type_declarations.b
    {: return new Pos(a, b); :}
  | EXCEPTION.a UIDENT.c constructor_arguments.b
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);
    :}
  | EXCEPTION.a UIDENT.id EQUAL.e constr_longident.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
		return new Pos(a, b);    
    :}
  | MODULE.a UIDENT.id module_binding.b
    {: 
		return new Pos(a, b);
    :}
  | MODULE.a REC module_rec_bindings.b
  	{: return new Pos(a, b); :}
  | MODULE.a TYPE ident.id EQUAL.e module_type.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
  | OPEN.a mod_longident.b
    {: 
		return new Pos(a, b);    
	:}
  | CLASS.a class_declarations.b
  	{: return new Pos(a, b); :}
  | CLASS.a TYPE class_type_declarations.b
  	{: return new Pos(a, b); :}
  | INCLUDE.a module_expr.b
    {: 
    	return new Pos(a, b);
    :}
;

module_binding=
    EQUAL.a module_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | COLON.a module_type EQUAL.e module_expr.b
  	{: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
  		return new Pos(a, b); 
  	:}
  | LPAREN.a UIDENT COLON module_type RPAREN.c module_binding.b
   	{: 
   		addHint(IndentHint.Type.MODULECONSTRAINT, a.getStart(), c.getEnd());
   		return new Pos(a, b); 
   	:}
;
module_rec_bindings=
    module_rec_binding.a                          
    {: return a; :}
  | module_rec_bindings.a AND module_rec_binding.b
  	{: 
  		return new Pos(a, b);
  	:}
;
module_rec_binding=
    UIDENT.a COLON module_type EQUAL.e module_expr.b    
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
;

/* Module types */

module_type=
    mty_longident.a
    {: return a; :}
  | SIG.a signature.c END.b
    {: 
    	addHint(IndentHint.Type.SIG, c.getStart(), c.getEnd());
    	return new Pos(a, b);
    :}
  | FUNCTOR.a LPAREN UIDENT COLON module_type RPAREN MINUSGREATER module_type.b
  	@ below_WITH
    {: 
    	addHint(IndentHint.Type.FUNCTOR, b.getStart(), b.getEnd());
    	return new Pos(a, b);
    :}
  | module_type.a WITH.c with_constraints.b
   	{: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);
   	:}
  | LPAREN.a module_type RPAREN.b
  	{: return new Pos(a, b); :}
;
signature=
    /* empty */                                 /*{ [] }*/
  	{: return Pos.NONE; :}
  | signature.a signature_item.b                    /*{ $2 :: $1 }*/
   	{: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return b;
   	:}
  | signature.a signature_item SEMISEMI.b           /*{ $2 :: $1 }*/
   	{: 
   		return new Pos(a, b); 
   	:}
;
signature_item=
    VAL.a val_ident_colon core_type.b
    {: 
    	return new Pos(a, b);
    :}
  | EXTERNAL.a val_ident_colon core_type EQUAL.e primitive_declaration.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
  | TYPE.a type_declarations.b
  	{: 
  		return new Pos(a, b);
  	:}
  | EXCEPTION.a UIDENT.c constructor_arguments.b
    {: 
    	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);
    :}
  | MODULE.a UIDENT module_declaration.b
    {: 
    	return new Pos(a, b);
    :}
  | MODULE.a REC module_rec_declarations.b
  	{: 
  		return new Pos(a, b);
  	:}
  | MODULE.a TYPE ident.b
    {: 
    	return new Pos(a, b);
    :}
  | MODULE.a TYPE ident.id EQUAL.e module_type.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
  | OPEN.a mod_longident.b
    {: 
    	return new Pos(a, b);
    :}
  | INCLUDE.a module_type.b
    {: 
    	return new Pos(a, b);
    :}
  | CLASS.a class_descriptions.b
  	{: 
  		return new Pos(a, b);
  	:}
  | CLASS.a TYPE class_type_declarations.b
  	{: 
  		return new Pos(a, b);
  	:}
;

module_declaration=
    COLON.a module_type.b
    {: 
    	return new Pos(a, b); 
    :}
  | LPAREN.a UIDENT COLON module_type RPAREN module_declaration.b
    {: 
    	return new Pos(a, b); 
    :}
;
module_rec_declarations=
    module_rec_declaration.a
    {: return a; :}
  | module_rec_declarations.a AND.n module_rec_declaration.b
  	{: 
  		return new Pos(a, b); 
  	:}
;
module_rec_declaration=
    UIDENT.a COLON module_type.b
    {: 
    	return new Pos(a, b); 
    :}
;

/* Class expressions */

class_declarations=
    class_declarations.a AND class_declaration.b
  	{: 
  		return new Pos(a, b);
  	:}
  | class_declaration.a                           
    {: 
    	return a; 
    :}
;
class_declaration=
    virtual_flag.a class_type_parameters.c LIDENT class_fun_binding.b
    {: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
;
class_fun_binding=
    EQUAL.a class_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | COLON.a class_type EQUAL.e class_expr.b
  	{: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
  		return new Pos(a, b); 
  	:}
  | labeled_simple_pattern.a class_fun_binding.b
  	{: 
  		return new Pos(a, b); 
  	:}
;
class_type_parameters=
    /*empty*/      
    {: return Pos.NONE; :}
  | LBRACKET.a type_parameter_list RBRACKET.b
  	{: 
  		return new Pos(a, b); 
  	:}
;
class_fun_def=
    labeled_simple_pattern.a MINUSGREATER class_expr.b
    {: 
    	return new Pos(a, b); 
    :}
  | labeled_simple_pattern.a class_fun_def.b
  	{: 
  		return new Pos(a, b);
  	:}
;
class_expr=
    class_simple_expr.a
    {: return a; :}
  | FUN.a class_fun_def.b
    {: return new Pos(a, b); :}
  | class_simple_expr.a simple_labeled_expr_list.b
    {: return new Pos(a, b); :}
  | LET.a rec_flag let_bindings IN class_expr.b
  	{: 
    	addHint(IndentHint.Type.IN, b.getStart(), b.getEnd());
  		return new Pos(a, b); 
  	:}
;
class_simple_expr=
    LBRACKET.a core_type_comma_list RBRACKET class_longident.b
    {: 
    	return new Pos(a, b); 
    :}
  | class_longident.a
    {: return a; :}
  | OBJECT.a class_structure.c END.b
    {: 
		addHint(IndentHint.Type.OBJECT, c.getStart(), c.getEnd());
		return new Pos(a, b);
    :}
  | LPAREN.a class_expr COLON class_type RPAREN.b
    {: return new Pos(a, b); :}
  | LPAREN.a class_expr RPAREN.b
    {: return new Pos(a, b); :}
;
class_structure=
    class_self_pattern.a class_fields.b
    {: 
    	if(a != Pos.NONE && b != Pos.NONE)
    		return new Pos(a, b);
    	else if(a != Pos.NONE && b == Pos.NONE)
    		return a;
    	else if(a == Pos.NONE && b != Pos.NONE)
    		return b;
    	else
    		return Pos.NONE;
    :}
;
class_self_pattern=
    LPAREN.a pattern RPAREN.b
    {: return new Pos(a, b); :}
  | LPAREN.a pattern COLON core_type RPAREN.b
    {: return new Pos(a, b); :}
  | /* empty */
    {: return Pos.NONE; :}
;
class_fields=
    /* empty */
    {: return Pos.NONE; :}
  | class_fields.a INHERIT.c class_expr.d parent_binder.b
  	{: 
    	if(a != Pos.NONE && b != Pos.NONE)
    		return new Pos(a, b);
    	else if(a != Pos.NONE && b == Pos.NONE)
    		return new Pos(a, d);
    	else if(a == Pos.NONE && b != Pos.NONE)
    		return new Pos(c, b);
    	else
    		return new Pos(c, d);
  	:}
  | class_fields.a VAL.c virtual_value.b
    {: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
  | class_fields.a VAL.c value.b
    {: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
  | class_fields.a virtual_method.b
  	{: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return b;
  	:}
  | class_fields.a concrete_method.b
  	{: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return b;
  	:}
  | class_fields.a CONSTRAINT.c constrain.b
    {: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
  | class_fields.a INITIALIZER.c seq_expr.b
    {: 
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
;
parent_binder=
    AS.a LIDENT.b
    {: 
    	return new Pos(a, b);
    :}
  | /* empty */
  	{: return Pos.NONE; :}
;
virtual_value=
    MUTABLE.a VIRTUAL label.id COLON core_type.b
    {: 
    	return new Pos(a, b);
    :}
  | VIRTUAL.a mutable_flag.m label.id COLON core_type.b
    {: 
    	return new Pos(a, b);
    :}
;
value=
    mutable_flag.a label.c EQUAL.e seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
  | mutable_flag.a label.c type_constraint EQUAL.e seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());

    	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);
    :}
;
virtual_method=
    METHOD.a PRIVATE VIRTUAL label.id COLON poly_type.b
    {: 
    	return new Pos(a, b);
    :}
  | METHOD.a VIRTUAL private_flag.p label.id COLON poly_type.b
    {: 
    	return new Pos(a, b);
    :}
;
concrete_method =
    METHOD.a private_flag.p label.id strict_binding.b
    {: 
    	return new Pos(a, b);
    :}
  | METHOD.a private_flag.p label.id COLON poly_type EQUAL.e seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
  | METHOD.a private_flag.p LABEL.id poly_type EQUAL.e seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b);
    :}
;


/* Class types */

class_type=
    class_signature.s
    {: return s; :}
  | QUESTION.a LIDENT COLON simple_core_type_or_tuple MINUSGREATER class_type.b
  	{: 
  		return new Pos(a, b); 
  	:}
  | OPTLABEL.a simple_core_type_or_tuple MINUSGREATER class_type.b
  	{: 
  		return new Pos(a, b); 
  	:}
  | LIDENT.a COLON simple_core_type_or_tuple MINUSGREATER class_type.b
  	{: 
  		return new Pos(a, b);
  	:}
  | simple_core_type_or_tuple.a MINUSGREATER class_type.b
  	{: 
  		return new Pos(a, b); 
  	:}
;
class_signature=
    LBRACKET.a core_type_comma_list RBRACKET clty_longident.b
  	{: 
  		return new Pos(a, b);
  	:}
  | clty_longident.a
  	{: return a; :}
  | OBJECT.a class_sig_body.c END.b
    {: 	
		addHint(IndentHint.Type.OBJECT, c.getStart(), c.getEnd());
    	return new Pos(a, b);
    :}
;
class_sig_body=
    class_self_type.a class_sig_fields.b
    {: 
     	if(a != Pos.NONE && b != Pos.NONE)
    		return new Pos(a, b);
    	else if(a != Pos.NONE && b == Pos.NONE)
    		return a;
    	else if(a == Pos.NONE && b != Pos.NONE)
    		return b;
    	else
    		return Pos.NONE;
    :}
;
class_self_type=
    LPAREN.a core_type RPAREN.b
    {: 
    	return new Pos(a, b); 
    :}
  | /* empty */
    {: return Pos.NONE; :}
;
class_sig_fields=
    /* empty */                                 /*{ [] }*/
    {: return Pos.NONE; :}
  | class_sig_fields.a INHERIT.c class_signature.b    /*{ Pctf_inher $3 :: $1 }*/
  	{: 
     	if(a != Pos.NONE && b != Pos.NONE)
    		return new Pos(a, b);
    	else if(a != Pos.NONE && b == Pos.NONE)
    		return new Pos(a, c);
    	else if(a == Pos.NONE && b != Pos.NONE)
    		return new Pos(c, b);
    	else
    		return new Pos(c);
  	:}
  | class_sig_fields.a VAL.c value_type.b             /*{ Pctf_val   $3 :: $1 }*/
  	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
    :}
  | class_sig_fields.a virtual_method.b             /*{ Pctf_virt  $2 :: $1 }*/
  	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return b;  	
  	:}
  | class_sig_fields.a method_type.b                /*{ Pctf_meth  $2 :: $1 }*/
  	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return b;  	
  	:}
  | class_sig_fields.a CONSTRAINT.c constrain.b       /*{ Pctf_cstr  $3 :: $1 }*/
  	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
  	:}
;
value_type=
    VIRTUAL.a mutable_flag.m label.id COLON core_type.b
    {: 
    	return new Pos(a, b);
    :}
  | MUTABLE.a virtual_flag label.id COLON core_type.b
    {: 
    	return new Pos(a, b);
    :}
  | label.a COLON core_type.b
    {: 
    	return new Pos(a, b);
    :}
;
method_type=
    METHOD.a private_flag label.id COLON poly_type.b
    {: 
    	return new Pos(a, b);
    :}
;
constrain=
	core_type.a EQUAL.e core_type.b
	{: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
		return new Pos(a, b); 
	:}
;
class_descriptions=
    class_descriptions.a AND.n class_description.b
  	{: 
  		return new Pos(a, b); 
  	:}
  | class_description.a
  	{: return a; :}
;
class_description=
    virtual_flag.a class_type_parameters.c LIDENT COLON class_type.b
    {: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
    :}
;
class_type_declarations=
    class_type_declarations.a AND.n class_type_declaration.b
  	{: 
  		return new Pos(a, b);
  	:}
  | class_type_declaration.a 
  	{: return a; :}
;	
class_type_declaration=
    virtual_flag.a class_type_parameters.c LIDENT.id EQUAL.e class_signature.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());

     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
    :}
;

/* Core expressions */

seq_expr=
   expr.a       @ below_SEMI
   {: return a; :}
  | expr.a SEMI.b
   {: return new Pos(a, b); :}
  | expr.a SEMI seq_expr.b
   {: return new Pos(a, b); :}
  
;
labeled_simple_pattern=
    QUESTION.a LPAREN label_let_pattern opt_default RPAREN.b
    {: return new Pos(a, b); :}
  | QUESTION.a label_var.b
    {: return new Pos(a, b); :}
  | OPTLABEL.a LPAREN let_pattern opt_default RPAREN.b
    {: 
    	return new Pos(a, b);
    :}
  | OPTLABEL.a pattern_var.b
    {: 
    	return new Pos(a, b);
    :}
  | TILDE.a LPAREN label_let_pattern RPAREN.b
  	{: 
  		return new Pos(a, b); 
  	:}
  | TILDE.a label_var.b
    {: 
    	return new Pos(a, b);
    :}
  | LABEL.a simple_pattern.b
    {: 
    	return new Pos(a, b);
    :}
      
  | simple_pattern.a
  	{: return a; :}
;
pattern_var=
    LIDENT.a            /*{ mkpat(Ppat_var $1) }*/
    {: 
    	return a;
    :}
  | UNDERSCORE.a        /*{ mkpat Ppat_any }*/
    {: 
    	return a;
    :}
;
opt_default=
    /* empty */                         /*{ None }*/
    {: return Pos.NONE; :}
  | EQUAL.a seq_expr.b                      /*{ Some $2 }*/
  	{: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
  		return new Pos(a, b); 
  	:}
;
label_let_pattern=
    label_var.a
    {: return a; :}
  | label_var.a COLON core_type.b
	{: return new Pos(a, b); :}
;
label_var=
    LIDENT.a
    {: 
    	return new Pos(a);
    :}
    
;
let_pattern=
    pattern.a
    {: return a; :}
  | pattern.a COLON core_type.b
  	{: return new Pos(a, b); :}
;
expr=
    simple_expr.a  @ below_SHARP
    {: return a; :} 
  | simple_expr.a simple_labeled_expr_list.b
  	{: 
  		addHint(IndentHint.Type.APP, b.getStart(), b.getEnd());
  		return new Pos(a, b); 
  	:}
  | LET.a rec_flag let_bindings IN seq_expr.b
  	{: 
    	addHint(IndentHint.Type.IN, b.getStart(), b.getEnd());
  		return new Pos(a, b);
  	:}
  | LET.a MODULE UIDENT.id module_binding IN seq_expr.b
    {: 
    	addHint(IndentHint.Type.IN, b.getStart(), b.getEnd());
    	return new Pos(a, b);
    :}
  | FUNCTION.a opt_bar.c match_cases.b
  	{: 
	    if(c == Pos.NONE)
	    	addHint(IndentHint.Type.FIRST_MATCH_CASE, b.getStart(), b.getStart() + 1);
  		return new Pos(a, b); 
  	:}
  | FUN.a labeled_simple_pattern fun_def.b
  	{: 
  		return new Pos(a, b);
  	:}
  | MATCH.a seq_expr WITH opt_bar.d match_cases.b
    {: 
    	if(d == Pos.NONE){
	    	addHint(IndentHint.Type.WITH, b.getStart(), b.getEnd());
	    	addHint(IndentHint.Type.FIRST_MATCH_CASE, b.getStart(), b.getStart() + 1);
	    }
    	else
    		addHint(IndentHint.Type.WITH, d.getStart(), b.getEnd());
    	
    	return new Pos(a, b); 
    :}  
  | TRY.a seq_expr.c WITH opt_bar.d match_cases.b
    {: 
    	addHint(IndentHint.Type.TRY, c.getStart(), c.getEnd());

    	if(d == Pos.NONE){
	    	addHint(IndentHint.Type.WITH, b.getStart(), b.getEnd());
	    	addHint(IndentHint.Type.FIRSTCATCH, b.getStart(), b.getStart() + 1);
	    }
    	else
    		addHint(IndentHint.Type.WITH, d.getStart(), b.getEnd());

    	return new Pos(a, b);
    :}
  | expr_comma_list.a @ below_COMMA
    {: return a; :}
  | constr_longident.a simple_expr.b @ below_SHARP 
    {: return new Pos(a, b); :}
  | name_tag.a simple_expr.b @ below_SHARP 
    {: return new Pos(a, b); :}
  | IF.a seq_expr THEN expr.c ELSE expr.b
    {: 
    	addHint(IndentHint.Type.THEN, c.getStart(), c.getEnd());
    	addHint(IndentHint.Type.ELSE, b.getStart(), b.getEnd());

    	return new Pos(a, b); 
    :}
  | IF.a seq_expr THEN expr.b
    {: 
    	addHint(IndentHint.Type.THEN, b.getStart(), b.getEnd());
    	return new Pos(a, b); 
    :}
  | WHILE.a seq_expr DO seq_expr.c DONE.b
    {: 
    	addHint(IndentHint.Type.WHILE, c.getStart(), c.getEnd());
    	return new Pos(a, b); 
    :}
  | FOR.a val_ident EQUAL seq_expr direction_flag seq_expr DO seq_expr.c DONE.b
    {:
    	addHint(IndentHint.Type.FOR, c.getStart(), c.getEnd());
    	return new Pos(a, b); 
    :}
  | expr.a COLONCOLON expr.b
    {: return new Pos(a, b); :}
  | LPAREN.a COLONCOLON RPAREN LPAREN expr COMMA expr RPAREN.b
    {: return new Pos(a, b); :}
  | expr.a INFIXOP0 expr.b
    {: return new Pos(a, b); :}
  | expr.a INFIXOP1 expr.b
    {: return new Pos(a, b); :}
  | expr.a INFIXOP2 expr.b
    {: return new Pos(a, b); :}
  | expr.a INFIXOP3 expr.b
    {: return new Pos(a, b); :}
  | expr.a INFIXOP4 expr.b
    {: return new Pos(a, b); :}
  | expr.a PLUS expr.b
    {: return new Pos(a, b); :}
  | expr.a MINUS expr.b
    {: return new Pos(a, b); :}
  | expr.a MINUSDOT expr.b
    {: return new Pos(a, b); :}
  | expr.a STAR expr.b
    {: return new Pos(a, b); :}
  | expr.a EQUAL expr.b
    {: return new Pos(a, b); :}
  | expr.a LESS expr.b
    {: return new Pos(a, b); :}
  | expr.a GREATER expr.b
    {: return new Pos(a, b); :}
  | expr.a OR expr.b
    {: return new Pos(a, b); :}
  | expr.a BARBAR expr.b
    {: return new Pos(a, b); :}
  | expr.a AMPERSAND expr.b
    {: return new Pos(a, b); :}
  | expr.a AMPERAMPER expr.b
    {: return new Pos(a, b); :}
  | expr.a COLONEQUAL expr.b
    {: return new Pos(a, b); :}
  | subtractive expr.a @ prec_unary_minus
    {: return a; :}
  | simple_expr.a DOT label_longident LESSMINUS expr.b
    {: return new Pos(a, b); :}
  | simple_expr.a DOT LPAREN seq_expr RPAREN LESSMINUS expr.b
    {: return new Pos(a, b); :}
  | simple_expr.a DOT LBRACKET seq_expr RBRACKET LESSMINUS expr.b
  	{: return new Pos(a, b); :}
  | simple_expr.a DOT LBRACE expr RBRACE LESSMINUS expr.b
	{: return new Pos(a,b); :}
  | label.a LESSMINUS expr.b
    {: return new Pos(a, b); :}
  | ASSERT.a simple_expr.b @ below_SHARP
    {: return new Pos(a, b); :}  
  | LAZY.a simple_expr.b @ below_SHARP 
    {: return new Pos(a, b); :}  
  | OBJECT.a class_structure.c END.b
    {: 
		addHint(IndentHint.Type.OBJECT, c.getStart(), c.getEnd());
    	return new Pos(a, b);
    :}
;
simple_expr=
    val_longident.a
    {: return a; :}
  | constant.a
  	{: return a; :}
  | constr_longident.a @ prec_constant_constructor 
    {: return a; :}
  | name_tag.a @ prec_constant_constructor 
    {: return a; :}
  | LPAREN.a seq_expr.c RPAREN.b
    {: 
    	addHint(IndentHint.Type.PAREN, a.getEnd() + 1, b.getStart());
    	return new Pos(a, b); 
    :}
  | BEGIN.a seq_expr.c END.b
    {: 
    	addHint(IndentHint.Type.BEGIN, a.getEnd() + 1, b.getStart());
    	return new Pos(a, b); 
    :}
  | BEGIN.a END.b
    {: 
    	return new Pos(a, b); 
    :}
  | LPAREN.a seq_expr type_constraint RPAREN.b
    {: return new Pos(a, b); :}
  | simple_expr.a DOT label_longident.b
    {: return new Pos(a, b); :}
  | simple_expr.a DOT LPAREN seq_expr RPAREN.b
  	{: return new Pos(a, b); :}
  | simple_expr.a DOT LBRACKET seq_expr RBRACKET.b
    {: return new Pos(a, b); :}
  | simple_expr.a DOT LBRACE expr RBRACE.b
    {: return new Pos(a, b); :}
  | LBRACE.a record_expr.c RBRACE.b
    {: 
    	addHint(IndentHint.Type.RECORD, c.getStart(), c.getEnd());
    	return new Pos(a, b); 
    :}
  | LBRACKETBAR.a expr_semi_list opt_semi BARRBRACKET.b
    {: return new Pos(a, b); :}
  | LBRACKETBAR.a BARRBRACKET.b
    {: return new Pos(a, b); :}
  | LBRACKET.a expr_semi_list opt_semi RBRACKET.b
    {: return new Pos(a, b); :}
  | PREFIXOP.a simple_expr.b
    {: return new Pos(a, b); :}
  | NEW.a class_longident.b
    {: return new Pos(a, b); :}
  | LBRACELESS.a field_expr_list opt_semi GREATERRBRACE.b
    {: return new Pos(a, b); :}
  | LBRACELESS.a GREATERRBRACE.b
    {: return new Pos(a, b); :}
  | simple_expr.a SHARP label.b
    {: return new Pos(a, b); :}
;
simple_labeled_expr_list=
    labeled_simple_expr.a
    {: return a; :}
  | simple_labeled_expr_list.a labeled_simple_expr.b
    {: return new Pos(a, b); :}
;
labeled_simple_expr=
    simple_expr.a @ below_SHARP
    {: return a; :}
  | label_expr.a
    {: return a; :}
;
label_expr=
    LABEL.a simple_expr.b @ below_SHARP
    {: return new Pos(a, b); :}
  | TILDE.a label_ident.b
  	{: return new Pos(a, b); :}
  | QUESTION.a label_ident.b
 	{: return new Pos(a, b); :}
  | OPTLABEL.a simple_expr.b @ below_SHARP 
    {: return new Pos(a, b); :}
;
label_ident=
    LIDENT.a
    {:
    	return new Pos(a);
    :}
;
let_bindings=
    let_binding.a
    {: return a; :}                      
  | let_bindings.a AND let_binding.b                
  	{: 
  		return new Pos(a, b);
  	:}
;
let_binding=
    val_ident.a fun_binding.b
    {: 
    	return new Pos(a, b);
    :}
  | pattern.a EQUAL.e seq_expr.b
  	{: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
  		return new Pos(a, b);
  	:}
;
fun_binding=
    strict_binding.a
    {: return a; :}
  | type_constraint.a EQUAL.e seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
;
strict_binding=
    EQUAL.a seq_expr.b
    {: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | labeled_simple_pattern.a fun_binding.b
  	{: 
  		addHint(IndentHint.Type.FUNARGS, a.getStart(), a.getEnd());
  		return new Pos(a, b);
  	:}
;
match_cases=
    pattern.a match_action.b
  	{: 
		return new Pos(a, b);  		
  	:} 
  | match_cases.a BAR pattern match_action.b
  	{: 	
  		return new Pos(a, b);
  	:} 
;
fun_def=
    match_action.a            
    {: return a; :}
  | labeled_simple_pattern.a fun_def.b
  	{: 	
  		return new Pos(a, b);
  	:} 
;
match_action=
    MINUSGREATER.a seq_expr.b
    {: 
    	addHint(IndentHint.Type.MATCH_ACTION, b.getStart(), b.getEnd());
    	return new Pos(a, b); 
    :}
  | WHEN.a seq_expr MINUSGREATER seq_expr.b
  	{: 
    	addHint(IndentHint.Type.MATCH_ACTION, b.getStart(), b.getEnd());
  		return new Pos(a, b); 
  	:}
;
expr_comma_list=
    expr_comma_list.a COMMA expr.b
    {: return new Pos(a, b); :}
  | expr.a COMMA expr.b     
  	{: return new Pos(a, b); :}
;
record_expr=
    simple_expr.a WITH lbl_expr_list.c opt_semi.b
    {: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);  	
    :}
  | lbl_expr_list.a opt_semi.b  
  	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return a;  	
  	:}
;
lbl_expr_list=
    label_longident.a EQUAL.e expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | lbl_expr_list.a SEMI label_longident EQUAL.e expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a,b); 
    :}
;
field_expr_list=
    label.a EQUAL.e expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | field_expr_list.a SEMI label EQUAL.e expr.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a,b); 
    :}
;
expr_semi_list=
    expr.a
    {: return a; :}                   
  | expr_semi_list.a SEMI expr.b
  	{: return new Pos(a, b); :}
;
type_constraint=
    COLON.a core_type.b
    {: return new Pos(a, b); :}
  | COLON.a core_type COLONGREATER core_type.b
  	{: return new Pos(a, b); :}
  | COLONGREATER.a core_type.b
  	{: return new Pos(a, b); :}
;

/* Patterns */

pattern=
    simple_pattern.a
    {: return a; :}
  | pattern.a AS val_ident.b
  	{: return new Pos(a, b); :}
  | pattern_comma_list.a  @ below_COMMA
  	{: return a; :} 
  | constr_longident.a pattern.b @ prec_constr_appl 
    {: return new Pos(a, b); :}
  | name_tag.a pattern.b @ prec_constr_appl 
    {: return new Pos(a, b); :}
  | pattern.a COLONCOLON pattern.b
  	{: return new Pos(a, b); :}
  | LPAREN.a COLONCOLON RPAREN LPAREN pattern COMMA pattern RPAREN.b
  	{: return new Pos(a, b); :}
  | pattern.a BAR pattern.b
	{: return new Pos(a, b); :}
;
simple_pattern=
    val_ident.a @ below_EQUAL 
    {: return a; :}
  | UNDERSCORE.a
  	{: return new Pos(a); :}
  | signed_constant.a
    {: return a; :}
  | CHAR.a DOTDOT CHAR.b
  	{: return new Pos(a, b); :}
  | constr_longident.a
  	{: return a; :}
  | name_tag.a
    {: return a; :}
  | SHARP.a type_longident.b
    {: return new Pos(a, b); :}
  | LBRACE.a lbl_pattern_list.c opt_semi RBRACE.b
    {: 
    	addHint(IndentHint.Type.RECORD, c.getStart(), c.getEnd());
    	return new Pos(a, b); 
   	:}
  | LBRACKET.a pattern_semi_list opt_semi RBRACKET.b
    {: return new Pos(a, b); :}
  | LBRACKETBAR.a pattern_semi_list opt_semi BARRBRACKET.b
    {: return new Pos(a, b); :}
  | LBRACKETBAR.a BARRBRACKET.b
    {: return new Pos(a, b); :}
  | LPAREN.a pattern RPAREN.b
    {: return new Pos(a, b); :}
  | LPAREN.a pattern COLON core_type RPAREN.b
    {: return new Pos(a, b); :}
;

pattern_comma_list=
    pattern_comma_list.a COMMA pattern.b 
    {: return new Pos(a, b); :}
  | pattern.a COMMA pattern.b  
    {: return new Pos(a, b); :}
;
pattern_semi_list=
    pattern.a
  	{: return a; :}
  | pattern_semi_list.a SEMI pattern.b
    {: return new Pos(a, b); :}
;
lbl_pattern_list=
    label_longident.a EQUAL.e pattern.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | lbl_pattern_list.a SEMI label_longident EQUAL.e pattern.b
    {: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  
;

/* Primitive declarations */

primitive_declaration=
    STRING.a               
    {: return new Pos(a); :}
  | STRING.a primitive_declaration.b
   	{: return new Pos(a, b); :}
;

/* Type declarations */

type_declarations=
    type_declaration.a                            /*{ [$1] }*/
    {: return a; :}
  | type_declarations.a AND.n type_declaration.b      /*{ $3 :: $1 }*/
  	{: 
  		return new Pos(a, b); 
  	:}
;
type_declaration=
    type_parameters.a LIDENT.c type_kind.d constraints.b
    {: 
    	Pos first;
    	Pos last;
    	
    	if(a != Pos.NONE)
    		first = (Pos)a;
    	else 
    		first = new Pos(c);
    	
    	if(b != Pos.NONE)
    		last = (Pos)b;
    	else if(b == Pos.NONE && d != Pos.NONE)
    		last = (Pos)d;
    	else
    		last = new Pos(c);
    		
    	if(first == last)
    		return first;
    	else
    		return new Pos(first, last);
    :}
;
constraints=
    constraints.a CONSTRAINT.c constrain.b        /*{ $3 :: $1 }*/
	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
	:}
  | /* empty */                             /*{ [] }*/
    {: return Pos.NONE; :}
;
type_kind=
    /*empty*/
    {: return Pos.NONE; :}
  | EQUAL.a core_type.b
    {: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a constructor_declarations.b
    {: 
	    addHint(IndentHint.Type.FIRST_CONTRUCTOR, b.getStart(), b.getStart() + 1);
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a PRIVATE.c constructor_declarations.b
    {: 
	    addHint(IndentHint.Type.FIRST_CONTRUCTOR, b.getStart(), b.getStart() + 1);
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a private_flag.c BAR.d constructor_declarations.b
    {: 
	    addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a private_flag.c LBRACE.d label_declarations opt_semi RBRACE.b
    {: 
   		addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd() + 1);
    	addHint(IndentHint.Type.RECORD, d.getEnd() + 1, b.getStart());
    	return new Pos(a, b); 
    :}
  | EQUAL.a core_type EQUAL.c private_flag opt_bar.o constructor_declarations.b
    {: 
    	if(o == Pos.NONE)
    		addHint(IndentHint.Type.FIRST_CONTRUCTOR, b.getStart(), b.getStart() + 1);
    		
    	addHint(IndentHint.Type.DEF, c.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a core_type.t EQUAL.e private_flag LBRACE.l label_declarations.c opt_semi RBRACE.b
    {: 
    	if(t == Pos.NONE)
    		addHint(IndentHint.Type.DEF, e.getEnd() + 1, l.getStart());
    	else
    		addHint(IndentHint.Type.DEF, t.getStart(), l.getStart());
    	
    	addHint(IndentHint.Type.RECORD, c.getStart(), c.getEnd());
    	return new Pos(a, b); 
    :}
  | EQUAL.a PRIVATE.c core_type.b
    {: 
    	addHint(IndentHint.Type.DEF, a.getEnd() + 1, b.getEnd());
    	return new Pos(a, b); 
    :}
;
type_parameters=
    /*empty*/                                   /*{ [] }*/
    {: return Pos.NONE; :}
  | type_parameter.a                              /*{ [$1] }*/
    {: return a; :}
  | LPAREN.a type_parameter_list RPAREN.b           /*{ List.rev $2 }*/
    {: return new Pos(a, b); :}
;
type_parameter=
    type_variance.a QUOTE.c ident.b                   /*{ $3, $1 }*/
    {: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
    :}
;
type_variance=
    /* empty */                                 /*{ false, false }*/
    {: return Pos.NONE; :}
  | PLUS.a                                        /*{ true, false }*/
    {: return new Pos(a); :}
  | MINUS.a                                       /*{ false, true }*/
    {: return new Pos(a); :}
;
type_parameter_list=
    type_parameter.a                              /*{ [$1] }*/
    {: return a; :}
  | type_parameter_list.a COMMA type_parameter.b    /*{ $3 :: $1 }*/
    {: return new Pos(a, b); :}
;

/* variant type constructors */
constructor_declarations=
    constructor_declaration.a
    {: return a; :}
  | constructor_declarations.a BAR constructor_declaration.b
  	{: return new Pos(a, b); :}
;
constructor_declaration=
    constr_ident.a constructor_arguments.b
    {: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return a;  	
    :}
;
constructor_arguments=
    /*empty*/                                   /*{ [] }*/
	{: return Pos.NONE; :}    
  | OF.a core_type_list.b                           /*{ List.rev $2 }*/
	{: return new Pos(a, b); :}    
;

/* record type constructors */
label_declarations=
    label_declaration.a             
	{: return a; :}    
  | label_declarations.a SEMI label_declaration.b
	{: return new Pos(a, b); :}    
;

label_declaration=
    mutable_flag.a label.c COLON poly_type.b 
	{: 
     	if(a != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(c, b);  	
	:}    

;

/* "with" constraints (additional type equations over signature components) */

with_constraints=
    with_constraint.a                             /*{ [$1] }*/
	{: return a; :}    
  | with_constraints.a AND with_constraint.b        /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
with_constraint=
    TYPE.a type_parameters label_longident with_type_binder core_type.c constraints.b
	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);  	
	:}    
  | MODULE.a mod_longident EQUAL.e mod_ext_longident.b
	{: 
    	addHint(IndentHint.Type.DEF, e.getEnd() + 1, b.getEnd());
		return new Pos(a, b); 
	:}    
;
with_type_binder=
    EQUAL.a   
	{: return a; :}    
  | EQUAL.a PRIVATE.b
	{: return new Pos(a, b); :}    
;

/* Polymorphic types */

typevar_list=
    QUOTE.a ident.b                             /*{ [$2] }*/
	{: return new Pos(a, b); :}    
  | typevar_list.a QUOTE ident.b                /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
poly_type=
    core_type.a
	{: return a; :}    
  | typevar_list.a DOT core_type.b
	{: return new Pos(a, b); :}    
;

/* Core types */

core_type=
    core_type2.a
	{: return a; :}    
  | core_type2.a AS QUOTE ident.b
	{: return new Pos(a, b); :}    
;
core_type2=
    simple_core_type_or_tuple.a
	{: return a; :}    
  | QUESTION.a LIDENT COLON core_type2 MINUSGREATER core_type2.b
	{: return new Pos(a, b); :}    
  | OPTLABEL.a core_type2 MINUSGREATER core_type2.b
	{: return new Pos(a, b); :}    
  | LIDENT.a COLON core_type2 MINUSGREATER core_type2.b
	{: return new Pos(a, b); :}    
  | core_type2.a MINUSGREATER core_type2.b
	{: return new Pos(a, b); :}    
;

simple_core_type=
    simple_core_type2.a  @ below_SHARP
	{: return a; :}    
  | LPAREN.a core_type_comma_list RPAREN.b @ below_SHARP 
	{: return new Pos(a, b); :}    
;
simple_core_type2=
    QUOTE.a ident.b
	{: return new Pos(a, b); :}    
  | UNDERSCORE.a
	{: return a; :}    
  | type_longident.a
	{: return a; :}    
  | simple_core_type2.a type_longident.b
	{: return new Pos(a, b); :}    
  | LPAREN.a core_type_comma_list RPAREN type_longident.b
	{: return new Pos(a, b); :}    
  | LESS.a meth_list GREATER.b
	{: return new Pos(a, b); :}    
  | LESS.a GREATER.b
	{: return new Pos(a, b); :}    
  | SHARP.a class_longident.c opt_present.b
	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);  	
	:}   
  | simple_core_type2.a SHARP class_longident.c opt_present.b
	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);  	
	:}    
  | LPAREN.a core_type_comma_list RPAREN SHARP class_longident.c opt_present.b
	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return new Pos(a, c);  	
	:}    
  | LBRACKET.a tag_field RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKET.a BAR row_field_list RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKET.a row_field BAR row_field_list RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKETGREATER.a opt_bar row_field_list RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKETGREATER.a RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKETLESS.a opt_bar row_field_list RBRACKET.b
	{: return new Pos(a, b); :}    
  | LBRACKETLESS.a opt_bar row_field_list GREATER name_tag_list RBRACKET.b
	{: return new Pos(a, b); :}    
;
row_field_list=
    row_field.a                                   /*{ [$1] }*/
	{: return a; :}    
  | row_field_list.a BAR row_field.b                /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
row_field=
    tag_field.a                                   /*{ $1 }*/
	{: return a; :}    
  | simple_core_type2.a                           /*{ Rinherit $1 }*/
	{: return a; :}    
;
tag_field=
    name_tag.a OF opt_ampersand amper_type_list.b
	{: return new Pos(a, b); :}    
  | name_tag.a
	{: return a; :}    
;
opt_ampersand=
    AMPERSAND.a                                   /*{ true }*/
	{: return a; :}    
  | /* empty */                                 /*{ false }*/
	{: return Pos.NONE; :}    
;
amper_type_list=
    core_type.a                                   /*{ [$1] }*/
	{: return a; :}    
  | amper_type_list.a AMPERSAND core_type.b         /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
opt_present=
    LBRACKETGREATER.a name_tag_list RBRACKET.b      /*{ List.rev $2 }*/
	{: return new Pos(a, b); :}    
  | /* empty */                                 /*{ [] }*/
	{: return Pos.NONE; :}    
;
name_tag_list=
    name_tag.a                                    /*{ [$1] }*/
	{: return a; :}    
  | name_tag_list.a name_tag.b                      /*{ $2 :: $1 }*/
	{: return new Pos(a, b); :}    
;
simple_core_type_or_tuple=
    simple_core_type.a                            /*{ $1 }*/
	{: return a; :}    
  | simple_core_type.a STAR core_type_list.b
	{: return new Pos(a, b); :}    
      /*{ mktyp(Ptyp_tuple($1 :: List.rev $3)) }*/
;
core_type_comma_list=
    core_type.a                                   /*{ [$1] }*/
	{: return a; :}    
  | core_type_comma_list.a COMMA core_type.b        /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
core_type_list=
    simple_core_type.a                            /*{ [$1] }*/
	{: return a; :}    
  | core_type_list.a STAR simple_core_type.b        /*{ $3 :: $1 }*/
	{: return new Pos(a, b); :}    
;
meth_list=
    field.a SEMI meth_list.b                        /*{ $1 :: $3 }*/
	{: return new Pos(a, b); :}    
  | field.a opt_semi.b                              /*{ [$1] }*/
	{: 
     	if(b != Pos.NONE)
    		return new Pos(a, b);
    	else
    		return a;  	
	:}    
  | DOTDOT.a                                      /*{ [mkfield Pfield_var] }*/
	{: return a; :}    
;
field=
    label.a COLON poly_type.b                       /*{ mkfield(Pfield($1, $3)) }*/
	{: return new Pos(a, b); :}    
;
label=
    LIDENT.a                                      /*{ $1 }*/
	{: return new Pos(a); :}    
;

/* Constants */

constant=
    INT.a                                         /*{ Const_int $1 }*/
	{: return new Pos(a); :}    
  | CHAR.a                                        /*{ Const_char $1 }*/
	{: return new Pos(a); :}    
  | STRING.a                                      /*{ Const_string $1 }*/
	{: return new Pos(a); :}    
  | FLOAT.a                                       /*{ Const_float $1 }*/
	{: return new Pos(a); :}    
  | INT32.a                                       /*{ Const_int32 $1 }*/
	{: return new Pos(a); :}    
  | INT64.a                                       /*{ Const_int64 $1 }*/
	{: return new Pos(a); :}    
  | NATIVEINT.a                                   /*{ Const_nativeint $1 }*/
	{: return new Pos(a); :}    
;
signed_constant=
    constant.a                                    /*{ $1 }*/
	{: return a; :}    
  | MINUS.a INT.b                                   /*{ Const_int(- $2) }*/
	{: return new Pos(a, b); :}    
  | MINUS.a FLOAT.b                                 /*{ Const_float("-" ^ $2) }*/
	{: return new Pos(a, b); :}    
  | MINUS.a INT32.b                                 /*{ Const_int32(Int32.neg $2) }*/
	{: return new Pos(a, b); :}    
  | MINUS.a INT64.b                                 /*{ Const_int64(Int64.neg $2) }*/
	{: return new Pos(a, b); :}    
  | MINUS.a NATIVEINT.b                             /*{ Const_nativeint(Nativeint.neg $2) }*/
	{: return new Pos(a, b); :}    
;
/* Identifiers and long identifiers */

ident=
    UIDENT.a                                      /*{ $1 }*/
	{: return new Pos(a); :}    
  | LIDENT.a                                      /*{ $1 }*/
	{: return new Pos(a); :}    
;
val_ident=
    LIDENT.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | LPAREN.a operator.o RPAREN.b                      /*{ $2 }*/
	{: return new Pos(a, b); :}    
;
val_ident_colon=
    LIDENT.a COLON.b                                /*{ $1 }*/
	{: return new Pos(a, b); :}    
  | LPAREN.a operator.o RPAREN COLON.b                /*{ $2 }*/
	{: return new Pos(a, b); :}    
  | LABEL.a                                       /*{ $1 }*/
	{: return new Pos(a); :}    
;
operator=
    PREFIXOP.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | INFIXOP0.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | INFIXOP1.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | INFIXOP2.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | INFIXOP3.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | INFIXOP4.a                                    /*{ $1 }*/
	{: return new Pos(a); :}    
  | PLUS.a                                        /*{ "+" }*/
	{: return new Pos(a); :}    
  | MINUS.a                                       /*{ "-" }*/
	{: return new Pos(a); :}    
  | MINUSDOT.a                                    /*{ "-." }*/
	{: return new Pos(a); :}    
  | STAR.a                                        /*{ "*" }*/
	{: return new Pos(a); :}    
  | EQUAL.a                                       /*{ "=" }*/
	{: return new Pos(a); :}    
  | LESS.a                                        /*{ "<" }*/
	{: return new Pos(a); :}    
  | GREATER.a                                     /*{ ">" }*/
	{: return new Pos(a); :}    
  | OR.a                                          /*{ "or" }*/
	{: return new Pos(a); :}    
  | BARBAR.a                                      /*{ "||" }*/
	{: return new Pos(a); :}    
  | AMPERSAND.a                                   /*{ "&" }*/
	{: return new Pos(a); :}    
  | AMPERAMPER.a                                  /*{ "&&" }*/
	{: return new Pos(a); :}    
  | COLONEQUAL.a                                  /*{ ":=" }*/
	{: return new Pos(a); :}    
;
constr_ident=
    UIDENT.a                                      /*{ $1 }*/
	{: return new Pos(a); :}    
  | LPAREN.a RPAREN.b                               /*{ "()" }*/
	{: return new Pos(a, b); :}    
  | COLONCOLON.a                                  /*{ "::" }*/
	{: return new Pos(a); :}    
  | FALSE.a                                       /*{ "false" }*/
	{: return new Pos(a); :}    
  | TRUE.a                                        /*{ "true" }*/
	{: return new Pos(a); :}    
;

val_longident=
    val_ident.a
    {: return a; :}
  | mod_longident.a DOT val_ident.b
	{: return new Pos(a, b); :}    
;
constr_longident=
    mod_longident.a       @ below_DOT             /*{ $1 }*/
    {: return a; :}
  | LBRACKET.a RBRACKET.b                           /*{ Lident "[]" }*/
	{: return new Pos(a, b); :}    
  | LPAREN.a RPAREN.b                               /*{ Lident "()" }*/
	{: return new Pos(a, b); :}    
  | FALSE.a                                       /*{ Lident "false" }*/
	{: return new Pos(a); :}    
  | TRUE.a                                        /*{ Lident "true" }*/
	{: return new Pos(a); :}    
;
label_longident=
    LIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_longident.a DOT LIDENT.b                    /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;
type_longident=
    LIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_ext_longident.a DOT LIDENT.b                /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;
mod_longident=
    UIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_longident.a DOT UIDENT.b                    /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;
mod_ext_longident=
    UIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_ext_longident.a DOT UIDENT.b                /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
  | mod_ext_longident.a LPAREN mod_ext_longident RPAREN.b /*{ Lapply($1, $3) }*/
	{: return new Pos(a, b); :}    
;
mty_longident=
    ident.a                                       /*{ Lident $1 }*/
    {: return a; :}
  | mod_ext_longident.a DOT ident.b                 /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;
clty_longident=
    LIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_ext_longident.a DOT LIDENT.b                /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;
class_longident=
    LIDENT.a                                      /*{ Lident $1 }*/
	{: return new Pos(a); :}    
  | mod_longident.a DOT LIDENT.b                    /*{ Ldot($1, $3) }*/
	{: return new Pos(a, b); :}    
;

/* Toplevel directives */

toplevel_directive=
    SHARP.a ident.b                 /*{ Ptop_dir($2, Pdir_none) }*/
	{: return new Pos(a, b); :}    
  | SHARP.a ident STRING.b          /*{ Ptop_dir($2, Pdir_string $3) }*/
	{: return new Pos(a, b); :}    
  | SHARP.a ident INT.b             /*{ Ptop_dir($2, Pdir_int $3) }*/
	{: return new Pos(a, b); :}    
  | SHARP.a ident val_longident.b   /*{ Ptop_dir($2, Pdir_ident $3) }*/
	{: return new Pos(a, b); :}    
  | SHARP.a ident FALSE.b           /*{ Ptop_dir($2, Pdir_bool false) }*/
	{: return new Pos(a, b); :}    
  | SHARP.a ident TRUE.b            /*{ Ptop_dir($2, Pdir_bool true) }*/
	{: return new Pos(a, b); :}    
;

/* Miscellaneous */

name_tag=
    BACKQUOTE.a ident.b                             /*{ $2 }*/
	{: return new Pos(a, b); :}    
;
rec_flag=
    /* empty */                                 /*{ Nonrecursive }*/
    {: return Pos.NONE; :}
  | REC.a                                         /*{ Recursive }*/
	{: return new Pos(a); :}    
;
direction_flag=
    TO.a                                          /*{ Upto }*/
	{: return new Pos(a); :}    
  | DOWNTO.a                                      /*{ Downto }*/
	{: return new Pos(a); :}    
;
private_flag=
    /* empty */                                 /*{ Public }*/
    {: return Pos.NONE; :}
  | PRIVATE.a                                     /*{ Private }*/
	{: return new Pos(a); :}    
;
mutable_flag=
    /* empty */                                 /*{ Immutable }*/
    {: return Pos.NONE; :}
  | MUTABLE.a                                     /*{ Mutable }*/
	{: return new Pos(a); :}    
;
virtual_flag=
    /* empty */                                 /*{ Concrete }*/
    {: return Pos.NONE; :}
  | VIRTUAL.a                                   /*{ Virtual }*/
	{: return new Pos(a); :}    
;
opt_bar=
    /* empty */                                 /*{ () }*/
    {: return Pos.NONE; :}
  | BAR.a                                         /*{ () }*/
	{: return new Pos(a); :}    
;
opt_semi=
   /* empty */                                 /*{ () }*/
    {: return Pos.NONE; :}
  | SEMI.a                                        /*{ () }*/
	{: return new Pos(a); :}    
;
subtractive=
    MINUS.a                                       /*{ "-" }*/
	{: return new Pos(a); :}    
  | MINUSDOT.a                                    /*{ "-." }*/
	{: return new Pos(a); :}    
;
