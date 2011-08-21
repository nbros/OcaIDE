// java -jar lib/beaver.jar -t -c src/ocaml/parser/OcamlParser.g


%package "ocaml.parser";

%import "java.util.ArrayList";

%embed {:
	public ErrorReporting errorReporting;


	/** This list of definitions is used as a backup in case the parsing cannot build the AST up to
		the root node (typically, when the user is changing code). In this case, we use this
		tree instead of the root returned by the parser to build the outline. <p>
		We must take care not to add the children of elements which are also in this list. So,
		we use the "bTop" boolean to know if this definition should be added to the outline */
	public ArrayList<Def> recoverDefs = new ArrayList<Def>();

	/** backup a node, so as to be able to later recover from a parsing error */
	private void backup(Def def){
		recoverDefs.add(def);
		for(Def child: def.children)
			//child.bTop = false;
			unsetTop(child);
	}

	private void unsetTop(Def def){
		def.bTop = false;
		for(Def child: def.children)
			unsetTop(child);
	}

	/** Can this variable name be a parameter? (that is, it starts with a lowercase letter) */
	public boolean isParameter(String name) {
		return name.length() > 0 && Character.isLowerCase(name.charAt(0));
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
%terminals BANG;
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
%terminals PLUSDOT;
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














%nonassoc BACKQUOTE, BANG, BEGIN, CHAR, FALSE, FLOAT, INT, INT32, INT64, LBRACE, LBRACELESS, LBRACKET, LBRACKETBAR, LIDENT, LPAREN, NEW, NATIVEINT, PREFIXOP, STRING, TRUE, UIDENT;
%nonassoc DOT;
%nonassoc below_DOT;
%nonassoc SHARP;
%nonassoc below_SHARP;
%nonassoc prec_constr_appl;
%nonassoc prec_constant_constructor;
%nonassoc prec_unary_minus, prec_unary_plus;
%right    INFIXOP4;
%left     INFIXOP3, STAR;
%left     INFIXOP2, PLUS, PLUSDOT, MINUS, MINUSDOT;
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
    	Def root = new Def("root", Def.Type.Root, 0, 0);
    	root.add(s);
    	root.collapse();
    	return root;
    :}

  | structure.s error
    {:
    	Def root = new Def("root", Def.Type.Root, 0, 0);
    	root.add(s);
    	root.collapse();
    	return root;
    :}
;

interfaces=
    signature.s                  /*      { ArrayList.$1 }*/
    {:
    	Def root = new Def("root", Def.Type.Root, 0, 0);
    	root.add(s);
    	root.collapse();
    	return root;
   	:}
;


toplevel_phrase=
    top_structure.t SEMISEMI               /*{ Ptop_def $1 }*/
    {: return t; :}
  | seq_expr.s SEMISEMI                    /*{ Ptop_def[ghstrexp $1] }*/
    {: return s; :}
  | toplevel_directive.d SEMISEMI          /*{ $1 }*/
    {: return d; :}

;

top_structure=
    structure_item.i                       /* { [$1] } */
    {: return i; :}
  | structure_item.a top_structure.b         /* { $1 :: $2 } */
  	{: return Def.root(a,b); :}
;
use_file=
    use_file_tail.a                        /* { $1 } */
    {: return a; :}
  | seq_expr.a use_file_tail.b               /* { Ptop_def[ghstrexp $1] :: $2 } */
  	{: return Def.root(a,b); :}
;
use_file_tail=
    SEMISEMI                                     /* { [] } */
  	{: return new Def(); :}
  | SEMISEMI seq_expr.a use_file_tail.b             /* { Ptop_def[ghstrexp $2] :: $3 } */
  	{: return Def.root(a,b); :}
  | SEMISEMI structure_item.a use_file_tail.b       /* { Ptop_def[$2] :: $3 } */
  	{: return Def.root(a,b); :}
  | SEMISEMI toplevel_directive.a use_file_tail.b   /* { $2 :: $3 } */
  	{: return Def.root(a,b); :}
  | structure_item.a use_file_tail.b                /* { Ptop_def[$1] :: $2 } */
  	{: return Def.root(a,b); :}
  | toplevel_directive.a use_file_tail.b            /* { $1 :: $2 } */
  	{: return Def.root(a,b); :}
;

/* Module expressions */

module_expr=
    mod_longident.a
    {: return a; :}
  | STRUCT.s structure.b END
    {:
    	Def def = new Def("<structure>", Def.Type.Struct, s.getStart(), s.getEnd());
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | STRUCT.a structure.s error
    {:
    	Def def = new Def("<structure>", Def.Type.Struct, a.getStart(), a.getEnd());
    	def.add(s);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | FUNCTOR LPAREN UIDENT.i COLON module_type.a RPAREN MINUSGREATER module_expr.b
    {:
    	Def def = new Def("<functor>", Def.Type.Functor, i.getStart(), i.getEnd());
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | module_expr.a LPAREN module_expr.b RPAREN
  	{: return Def.root(a,b); :}
  | module_expr LPAREN module_expr error
    {: return new Def(); :}
  | LPAREN module_expr.a COLON module_type.b RPAREN
  	{: return Def.root(a,b); :}
  | LPAREN module_expr COLON module_type error
    {: return new Def(); :}
  | LPAREN module_expr.a RPAREN
  	{: return a; :}
  | LPAREN module_expr error
    {: return new Def(); :}
  | LPAREN VAL expr.a COLON package_type.b RPAREN
  	{: return Def.root(a,b); :}
  | LPAREN VAL expr COLON error
    {: return new Def(); :}
;

structure=
    structure_tail.s
    {: return s; :}
  | seq_expr.a structure_tail.b
    {: return Def.root(a,b); :}
;

structure_tail=
    /* empty */
    {: return new Def(); :}
  | SEMISEMI
    {: return new Def(); :}
  | SEMISEMI seq_expr.a structure_tail.b
    {: return Def.root(a,b); :}
  | SEMISEMI structure_item.a structure_tail.b
    {: return Def.root(a,b); :}
  | structure_item.a structure_tail.b
    {: return Def.root(a,b); :}
  | error
    {: return new Def(); :}
;

structure_item=
    LET.l rec_flag.r let_bindings.a
	{:
		Def da = (Def)a;
		Def rec = (Def)r;

  		// add the "rec" flag
  		ArrayList<Def> lets = new ArrayList<Def>();
  		da.findLets(lets);
  		for(Def let: lets)
  			let.bRec = rec.bRec;

		return a;
	:}
  | EXTERNAL val_ident.i COLON core_type.a EQUAL primitive_declaration.b
    {:
		Def ident = (Def)i;
    	Def def = new Def(ident.name, Def.Type.External, ident.posStart, ident.posEnd);
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | TYPE type_declarations.t
    {: return t; :}
  | EXCEPTION UIDENT.id constructor_arguments.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Exception, id.getStart(), id.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | EXCEPTION UIDENT.id EQUAL constr_longident.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Exception, id.getStart(), id.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | MODULE UIDENT.id module_binding.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Module, id.getStart(), id.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | MODULE REC module_rec_bindings.a
  	{: return a; :}
  | MODULE TYPE ident.id EQUAL module_type.a
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.ModuleType, ident.posStart, ident.posEnd);
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | OPEN mod_longident.id
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.Open, ident.posStart, ident.posEnd);
    	backup(def);
    	return def;
    :}
  | CLASS class_declarations.a
  	{: return a; :}
  | CLASS TYPE class_type_declarations.a
  	{: return a; :}
  | INCLUDE module_expr.id
    {:
    	Def ident = (Def)id;
    	if(ident.type == Def.Type.Identifier){
    		Def def = new Def(ident.name, Def.Type.Include, ident.posStart, ident.posEnd);
    		backup(def);
	    	return def;
	    }
	    return new Def();
    :}
//  |	error
//  	{: return new Def(); :}

;
module_binding=
    EQUAL module_expr.a
    {: return a; :}
  | COLON module_type.a EQUAL module_expr.b
  	{: return Def.root(a,b); :}
  | LPAREN UIDENT COLON module_type.a RPAREN module_binding.b
   	{: return Def.root(a,b); :}
;
module_rec_bindings=
    module_rec_binding.a
    {: return a; :}
  | module_rec_bindings.a AND module_rec_binding.b
  	{:
  		Def db = (Def)b;
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
;
module_rec_binding=
    UIDENT.id COLON module_type.a EQUAL module_expr.b
    {:
    	Def def = new Def((String)id.value, Def.Type.Module, id.getStart(), id.getEnd());
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
;

/* Module types */

module_type=
    mty_longident.id
    {: return id; :}
  | SIG.s signature.a END
    {:
    	Def def = new Def("<signature>", Def.Type.Sig, s.getStart(), s.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | SIG signature error
  	{: return new Def(); :}
  | FUNCTOR LPAREN UIDENT.id COLON module_type.a RPAREN MINUSGREATER module_type.b
  	@ below_WITH
    {:
    	Def def = new Def((String)id.value, Def.Type.Functor, id.getStart(), id.getEnd());
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | module_type.a WITH with_constraints.b
   	{: return Def.root(a,b); :}
  | MODULE TYPE OF module_expr.a
  	{: return a; :}
  | LPAREN module_type.a RPAREN
  	{: return a; :}
  | LPAREN module_type error
  	{: return new Def(); :}
;
signature=
    /* empty */                                 /*{ [] }*/
  	{: return new Def(); :}
  | signature.a signature_item.b                    /*{ $2 :: $1 }*/
   	{: return Def.root(a,b); :}
  | signature.a signature_item.b SEMISEMI           /*{ $2 :: $1 }*/
   	{: return Def.root(a,b); :}
;
signature_item=
    VAL.v val_ident.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.Let, ident.posStart, ident.posEnd);
    	def.add(a);
    	def.collapse();

  		// add the start position of the definition
  		def.defPosStart = v.getStart();

    	backup(def);
    	return def;
    :}
  | EXTERNAL.e val_ident.id COLON core_type.a EQUAL primitive_declaration.b
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.External, ident.posStart, ident.posEnd);
  		def.defPosStart = e.getStart();
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | TYPE.t type_declarations.a
  	{:
  		Def da = (Def)a;
  		Def first = da.findFirstOfType(Def.Type.Type);
  		first.defPosStart = t.getStart();

  		return a;
  	:}
  | EXCEPTION.e UIDENT.id constructor_arguments.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Exception, id.getStart(), id.getEnd());
    	def.defPosStart = e.getStart();
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | MODULE.m UIDENT.id module_declaration.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Module, id.getStart(), id.getEnd());
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | MODULE.m REC module_rec_declarations.a
  	{:
  		Def da = (Def)a;
  		Def first = da.findFirstOfType(Def.Type.Module);
  		first.defPosStart = m.getStart();

  		return a;
  	:}
  | MODULE.m TYPE ident.id
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.ModuleType, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	backup(def);
    	return def;
    :}
  | MODULE.m TYPE ident.id EQUAL module_type.a
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.ModuleType, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | OPEN.o mod_longident.id
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.Open, ident.posStart, ident.posEnd);
    	def.defPosStart = o.getStart();
    	backup(def);
    	return def;
    :}
  | INCLUDE.i module_type.id
    {:
    	Def ident = (Def)id;
    	if(ident.type == Def.Type.Identifier){
    		Def def = new Def(ident.name, Def.Type.Include, ident.posStart, ident.posEnd);
    		def.defPosStart = i.getStart();
    		backup(def);
	    	return def;
	    }
	    return new Def();
    :}
  | CLASS.c class_descriptions.a
  	{:
  		Def da = (Def)a;
  		Def first = da.findFirstOfType(Def.Type.Class);
  		first.defPosStart = c.getStart();

  		return a;
  	:}
  | CLASS.c TYPE class_type_declarations.a
  	{:
  		Def da = (Def)a;
  		Def first = da.findFirstOfType(Def.Type.ClassType);
  		first.defPosStart = c.getStart();

  		return a;
  	:}
//  | error
//  	{: return new Def(); :}
;

module_declaration=
    COLON module_type.a
    {: return a; :}
  | LPAREN UIDENT COLON module_type.a RPAREN module_declaration.b
    {: return Def.root(a,b); :}
;
module_rec_declarations=
    module_rec_declaration.a
    {: return a; :}
  | module_rec_declarations.a AND.n module_rec_declaration.b
  	{:
  		Def db = (Def)b;
  		db.defPosStart = n.getStart();
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
;
module_rec_declaration=
    UIDENT.id COLON module_type.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Module, id.getStart(), id.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
	    return def;
    :}
;

/* Class expressions */

class_declarations=
    class_declarations.a AND class_declaration.b
  	{:
  		Def db = (Def)b;
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
  | class_declaration.a
    {: return a; :}
;
class_declaration=
    virtual_flag class_type_parameters.a LIDENT.id class_fun_binding.b
    {:
    	Def def = new Def((String)id.value, Def.Type.Class, id.getStart(), id.getEnd());
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
	    return def;
    :}
;
class_fun_binding=
    EQUAL class_expr.a
    {: return a; :}
  | COLON class_type.a EQUAL class_expr.b
  	{: return Def.root(a,b); :}
  | labeled_simple_pattern.a class_fun_binding.b
  {: return Def.root(a,b); :}
;
class_type_parameters=
    /*empty*/
    {: return new Def(); :}
  | LBRACKET type_parameter_list.a RBRACKET
  	{: return a; :}
;
class_fun_def=
    labeled_simple_pattern.a MINUSGREATER class_expr.b
    {: return Def.root(a,b); :}
  | labeled_simple_pattern.a class_fun_def.b
  	{: return Def.root(a,b); :}
;
class_expr=
    class_simple_expr.a
    {: return a; :}
  | FUN class_fun_def.a
    {: return a; :}
  | class_simple_expr.a simple_labeled_expr_list.b
    {: return Def.root(a,b); :}
  | LET rec_flag.r let_bindings.a IN class_expr.b
  	{:
  		Def da = (Def)a;
  		Def rec = (Def)r;

  		// transform the "lets" into "let ins" and add the "rec" flag
  		ArrayList<Def> lets = new ArrayList<Def>();
  		da.findLets(lets);
  		for(Def let: lets) {
  			let.type = Def.Type.LetIn;
  			let.bRec = rec.bRec;
  		}

  		if(lets.size() > 0){
  			Def last = lets.get(lets.size() - 1);
  			Def in = new Def("<in>", Def.Type.In, 0, 0);

  			in.add(b);
  			in.collapse();
  			last.children.add(in);
  			last.collapse();
  			backup(last);
  			return a;
  		}

  		return Def.root(a,b);
  	:}
;
class_simple_expr=
    LBRACKET core_type_comma_list.a RBRACKET class_longident.b
    {: return Def.root(a,b); :}
  | class_longident.a
    {: return a; :}
  | OBJECT.o class_structure.a END
    {:
    	Def def = new Def("<object>", Def.Type.Object, o.getStart(), o.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}

  | OBJECT class_structure error
  	{: return new Def(); :}
  | LPAREN class_expr.a COLON class_type.b RPAREN
    {: return Def.root(a,b); :}
  | LPAREN class_expr COLON class_type error
  	{: return new Def(); :}
  | LPAREN class_expr.a RPAREN
    {: return a; :}
  | LPAREN class_expr error
    {: return new Def(); :}
;
class_structure=
    class_self_pattern.a class_fields.b
    {: return Def.root(a,b); :}
;
class_self_pattern=
    LPAREN pattern.a RPAREN
    {: return a; :}
  | LPAREN pattern.a COLON core_type.b RPAREN
    {: return Def.root(a,b); :}
  | /* empty */
    {: return new Def(); :}
;
class_fields=
    /* empty */
    {: return new Def(); :}
  | class_fields.a INHERIT override_flag.b  class_expr.c parent_binder.d
  	{: return Def.root(a,b,c,d); :}
  | class_fields.a VAL.v virtual_value.id
    {:
    	Def ident = (Def)id;
    	if(ident.type == Def.Type.Identifier){
    		Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    		def.defPosStart = v.getStart();
    		backup(def);
	    	return Def.root(a, def);
	    }
	    return Def.root(a,id);
    :}
  | class_fields.a VAL.v value.id
    {:
    	Def ident = (Def)id;
    	if(ident.type == Def.Type.Identifier){
    		Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    		def.defPosStart = v.getStart();
    		backup(def);
	    	return Def.root(a, def);
	    }
	    return Def.root(a,id);
    :}
  | class_fields.a virtual_method.b
  	{: return Def.root(a,b); :}
  | class_fields.a concrete_method.b
  	{: return Def.root(a,b); :}
  | class_fields.a CONSTRAINT.c constrain.b
    {:
    	Def def = new Def("<constraint>", Def.Type.Constraint, c.getStart(), c.getEnd());
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return Def.root(a, def);
    :}
  | class_fields.a INITIALIZER.i seq_expr.b
    {:
    	Def def = new Def("initializer", Def.Type.Initializer, i.getStart(), i.getEnd());
    	def.defPosStart = i.getStart();
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return Def.root(a, def);
    :}
;
parent_binder=
    AS LIDENT.id
    {:
    	return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    :}
  | /* empty */
  	{: return new Def(); :}
;
virtual_value=
    override_flag.o MUTABLE.m VIRTUAL label.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    	int pos = ((Def)o).posStart;
    	def.defPosStart = (pos != 0 ? pos : m.getStart());
    	def.add(a);
    	def.collapse();
    	def.bAlt = true;
    	backup(def);
	    return def;
    :}
  | VIRTUAL.v mutable_flag.m label.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    	def.defPosStart = v.getStart();
    	def.add(a);
    	def.collapse();
    	def.bAlt = ((Def)m).bAlt;
    	backup(def);
	    return def;
    :}
;
value=
    override_flag.o mutable_flag.m label.id EQUAL seq_expr.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    	int pos1 = ((Def)o).posStart;
    	int pos2 = ((Def)m).posStart;
    	def.defPosStart = (pos1 != 0 ? pos1 : (pos2 != 0 ? pos2 : ident.posStart));
    	def.add(a);
    	def.collapse();
    	def.bAlt = ((Def)o).bAlt || ((Def)m).bAlt;
    	backup(def);
	    return def;
    :}
  | override_flag.o mutable_flag.m label.id type_constraint.a EQUAL seq_expr.b
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
    	int pos1 = ((Def)o).posStart;
    	int pos2 = ((Def)m).posStart;
    	def.defPosStart = (pos1 != 0 ? pos1 : (pos2 != 0 ? pos2 : ident.posStart));
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	def.bAlt = ((Def)o).bAlt || ((Def)m).bAlt;
    	backup(def);
	    return def;
    :}
;
virtual_method=
    METHOD.m override_flag.o PRIVATE VIRTUAL label.id COLON poly_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.collapse();
    	def.bAlt = true;
    	backup(def);
	    return def;
    :}
  | METHOD.m override_flag.o VIRTUAL private_flag.p label.id COLON poly_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.collapse();
    	def.bAlt = ((Def)o).bAlt || ((Def)p).bAlt;
    	backup(def);
	    return def;
    :}
;
concrete_method =
    METHOD.m override_flag.o private_flag.p label.id strict_binding.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.collapse();
    	def.bAlt = ((Def)o).bAlt || ((Def)p).bAlt;
    	backup(def);
	    return def;
    :}
  | METHOD.m override_flag.o private_flag.p label.id COLON poly_type.a EQUAL seq_expr.b
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	def.bAlt = ((Def)o).bAlt || ((Def)p).bAlt;
    	backup(def);
	    return def;
    :}
;


/* Class types */

class_type=
    class_signature.s
    {: return s; :}
  | QUESTION LIDENT COLON simple_core_type_or_tuple MINUSGREATER class_type.c
  	{: return c; :}
  | OPTLABEL simple_core_type_or_tuple MINUSGREATER class_type.c
  	{: return c; :}
  | LIDENT COLON simple_core_type_or_tuple MINUSGREATER class_type.c
  	{: return c; :}
  | simple_core_type_or_tuple MINUSGREATER class_type.c
  	{: return c; :}
;
class_signature=
    LBRACKET core_type_comma_list RBRACKET clty_longident
  	{: return new Def(); :}
  | clty_longident
  	{: return new Def(); :}
  | OBJECT.o class_sig_body.a END
    {:
    	Def def = new Def("<object>", Def.Type.Object, o.getStart(), o.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | OBJECT class_sig_body error
  	{: return new Def(); :}
;
class_sig_body=
    class_self_type.a class_sig_fields.b
    {: return Def.root(a,b); :}
;
class_self_type=
    LPAREN core_type.a RPAREN
    {: return Def.root(a); :}
  | /* empty */
    {: return new Def(); :}
;
class_sig_fields=
    /* empty */                                 /*{ [] }*/
    {: return new Def(); :}
  | class_sig_fields.s INHERIT class_signature.a    /*{ Pctf_inher $3 :: $1 }*/
  	{: return Def.root(s,a); :}
  | class_sig_fields.s VAL.v value_type.a             /*{ Pctf_val   $3 :: $1 }*/
  	{:
  		Def da = (Def)a;
  		da.defPosStart = v.getStart();
  		return Def.root(s,a);
  	:}
  | class_sig_fields.s virtual_method_type.a        /*{ Pctf_virt  $2 :: $1 }*/
  	{: return Def.root(s,a); :}
  | class_sig_fields.s method_type.a                /*{ Pctf_meth  $2 :: $1 }*/
  	{: return Def.root(s,a); :}
  | class_sig_fields.s CONSTRAINT constrain.a       /*{ Pctf_cstr  $3 :: $1 }*/
  	{: return Def.root(s,a); :}
;
value_type=
    VIRTUAL mutable_flag.m label.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
	    def.add(a);
	    def.collapse();
    	def.bAlt = ((Def)m).bAlt;
	    backup(def);
	    return def;
    :}
  | MUTABLE virtual_flag label.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
	    def.add(a);
	    def.collapse();
    	def.bAlt = true;
	    backup(def);
	    return def;
    :}
  | label.id COLON core_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Val, ident.posStart, ident.posEnd);
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
;
method_type=
    METHOD.m private_flag label.id COLON poly_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
;
virtual_method_type=
    METHOD.m PRIVATE VIRTUAL label.id COLON poly_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
  | METHOD.m VIRTUAL private_flag label.id COLON poly_type.a
    {:
    	Def ident = (Def)id;
    	assert (ident.type == Def.Type.Identifier);
    	Def def = new Def(ident.name, Def.Type.Method, ident.posStart, ident.posEnd);
    	def.defPosStart = m.getStart();
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
;
constrain=
	core_type.a EQUAL core_type.b
	{: return Def.root(a,b); :}
;
class_descriptions=
    class_descriptions.a AND.n class_description.b
  	{:
  		Def db = (Def)b;
  		db.defPosStart = n.getStart();
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
  | class_description.a
  	{: return a; :}
;
class_description=
    virtual_flag class_type_parameters LIDENT.id COLON class_type.a
    {:
    	Def def = new Def((String)id.value, Def.Type.Class, id.getStart(), id.getEnd());
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
;
class_type_declarations=
    class_type_declarations.a AND.n class_type_declaration.b
  	{:
  		Def db = (Def)b;
  		db.defPosStart = n.getStart();
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
  | class_type_declaration.a
  	{: return a; :}
;
class_type_declaration=
    virtual_flag class_type_parameters LIDENT.id EQUAL class_signature.a
    {:
    	Def def = new Def((String)id.value, Def.Type.ClassType, id.getStart(), id.getEnd());
	    def.add(a);
	    def.collapse();
	    backup(def);
	    return def;
    :}
;

/* Core expressions */

seq_expr=
   expr.a       @ below_SEMI
   {: return a; :}
  | expr.a SEMI
   {: return a; :}
  | expr.a SEMI seq_expr.b
   {: return Def.root(a,b); :}

;
labeled_simple_pattern=
    QUESTION LPAREN label_let_pattern.a opt_default.b RPAREN
    {: return Def.root(a,b); :}
  | QUESTION label_var.a
    {: return a; :}
  | OPTLABEL.id LPAREN let_pattern.a opt_default.b RPAREN
    {:
    	Def ident = new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    	return Def.root(ident,a,b);
    :}
  | OPTLABEL.id pattern_var.a
    {:
    	Def ident = new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    	return Def.root(ident,a);
    :}
  | TILDE LPAREN label_let_pattern.a RPAREN
  	{: return a; :}
  | TILDE label_var.a
    {: return a; :}
  | LABEL.id simple_pattern.a
    {:
    	Def ident = new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    	return Def.root(ident,a);
    :}

  | simple_pattern.a
  	{: return a; :}
;
pattern_var=
    LIDENT.id            /*{ mkpat(Ppat_var $1) }*/
    {:
    	return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    :}
  | UNDERSCORE.id        /*{ mkpat Ppat_any }*/
    {:
    	return new Def("_", Def.Type.Identifier, id.getStart(), id.getEnd());
    :}
;
opt_default=
    /* empty */                         /*{ None }*/
    {: return new Def(); :}
  | EQUAL seq_expr.a                      /*{ Some $2 }*/
  	{: return a; :}
;
label_let_pattern=
    label_var.a
    {: return a; :}
  | label_var.a COLON core_type.b
	{: return Def.root(a,b); :}
;
label_var=
    LIDENT.id
    {:
    	return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    :}

;
let_pattern=
    pattern.a
    {: return a; :}
  | pattern.a COLON core_type.b
  	{: return Def.root(a,b); :}
;
expr=
    simple_expr.a  @ below_SHARP
    {: return a; :}
  | simple_expr.a simple_labeled_expr_list.b
  	{: return Def.root(a,b); :}
  | LET rec_flag.r let_bindings.a IN seq_expr.b
  	{:
  		Def da = (Def)a;
  		Def rec = (Def)r;

  		// transform the "lets" into "let ins" and add the "rec" flag
  		ArrayList<Def> lets = new ArrayList<Def>();
  		da.findLets(lets);
  		for(Def let: lets){
  			let.type = Def.Type.LetIn;
  			let.bRec = rec.bRec;
  		}

  		if(lets.size() > 0){
  			Def last = lets.get(lets.size() - 1);
  			Def in = new Def("<in>", Def.Type.In, 0, 0);

  			in.add(b);
  			in.collapse();
  			last.children.add(in);
  			last.collapse();
  			backup(last);
  			return a;
  		}

  		return Def.root(a,b);
  	:}
  | LET MODULE UIDENT.id module_binding.a IN seq_expr.b
    {:
    	Def def = new Def((String)id.value, Def.Type.Module, id.getStart(), id.getEnd());
    	def.add(a);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | LET OPEN mod_longident.id IN seq_expr.a
    {:
    	Def ident = (Def)id;
    	Def def = new Def(ident.name, Def.Type.Open, ident.posStart, ident.posEnd);
		Def in = new Def("<in>", Def.Type.In, 0, 0);
		in.add(a);
		in.collapse();
		def.children.add(in);
		def.collapse();
    	backup(def);
    	return def;
    :}
  | FUNCTION opt_bar.a match_cases.b
  	{: return Def.root(a,b); :}
  | FUN labeled_simple_pattern.a fun_def.b
  	{:
  		// find the identifiers defined in this pattern, and
  		// transform them into "parameter" nodes
  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)a;
    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	boolean bFirst = true;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);
    		if(isParameter(ident.name)) {
	    		Def def = new Def(ident.name, Def.Type.Parameter, ident.posStart, ident.posEnd);
	    		if(!bFirst)
	    			def.bAnd = true;
	    		bFirst = false;
	    		root.add(def);
	    		last = def;
    		} else {
	    		Def def = new Def(ident.name, Def.Type.Identifier, ident.posStart, ident.posEnd);
	    		root.add(def);
    		}
    	}

    	if(last != null){
    		last.add(b);
    		last.collapse();
    		return root;
    	} else {
    		return Def.root(a, b);
    	}
  	:}
  | FUN LPAREN TYPE LIDENT.id RPAREN fun_def.a
  	{:
    	Def root = new Def();
    	Def def = new Def((String)id.value, Def.Type.Parameter, id.getStart(), id.getEnd());
   		root.add(def);
   		def.add(a);
   		def.collapse();
   		return root;
  	:}
  | MATCH seq_expr.a WITH opt_bar.b match_cases.c
    {: return Def.root(a,b,c); :}
  | TRY seq_expr.a WITH opt_bar.b match_cases.c
    {: return Def.root(a,b,c); :}
  | TRY seq_expr WITH error
    {: return new Def(); :}
  | expr_comma_list.a @ below_COMMA
    {: return a; :}
  | constr_longident.a simple_expr.b @ below_SHARP
    {: return Def.root(a,b); :}
  | name_tag.a simple_expr.b @ below_SHARP
    {: return Def.root(a,b); :}
  | IF seq_expr.a THEN expr.b ELSE expr.c
    {: return Def.root(a,b,c); :}
  | IF seq_expr.a THEN expr.b
    {: return Def.root(a,b); :}
  | WHILE seq_expr.a DO seq_expr.b DONE
    {: return Def.root(a,b); :}
  | FOR val_ident.ident EQUAL seq_expr.b direction_flag seq_expr.c DO seq_expr.d DONE
    {:
	    Def i = (Def)ident;
	    Def def = new Def(i.name, Def.Type.Parameter, i.posStart, i.posEnd);
	    def.add(b);
	    def.add(c);
	    def.add(d);
	    def.collapse();
    	return def;
    :}
  | expr.a COLONCOLON expr.b
    {: return Def.root(a,b); :}
  | LPAREN COLONCOLON RPAREN LPAREN expr.a COMMA expr.b RPAREN
    {: return Def.root(a,b); :}
  | expr.a INFIXOP0 expr.b
    {: return Def.root(a,b); :}
  | expr.a INFIXOP1 expr.b
    {: return Def.root(a,b); :}
  | expr.a INFIXOP2 expr.b
    {: return Def.root(a,b); :}
  | expr.a INFIXOP3 expr.b
    {: return Def.root(a,b); :}
  | expr.a INFIXOP4 expr.b
    {: return Def.root(a,b); :}
  | expr.a PLUS expr.b
    {: return Def.root(a,b); :}
  | expr.a PLUSDOT expr.b
    {: return Def.root(a,b); :}
  | expr.a MINUS expr.b
    {: return Def.root(a,b); :}
  | expr.a MINUSDOT expr.b
    {: return Def.root(a,b); :}
  | expr.a STAR expr.b
    {: return Def.root(a,b); :}
  | expr.a EQUAL expr.b
    {: return Def.root(a,b); :}
  | expr.a LESS expr.b
    {: return Def.root(a,b); :}
  | expr.a GREATER expr.b
    {: return Def.root(a,b); :}
  | expr.a OR expr.b
    {: return Def.root(a,b); :}
  | expr.a BARBAR expr.b
    {: return Def.root(a,b); :}
  | expr.a AMPERSAND expr.b
    {: return Def.root(a,b); :}
  | expr.a AMPERAMPER expr.b
    {: return Def.root(a,b); :}
  | expr.a COLONEQUAL expr.b
    {: return Def.root(a,b); :}
  | subtractive expr.a @ prec_unary_minus
    {: return a; :}
  | additive expr.a @ prec_unary_plus
    {: return a; :}
  | simple_expr.a DOT label_longident.b LESSMINUS expr.c
    {: return Def.root(a,b,c); :}
  | simple_expr.a DOT LPAREN seq_expr.b RPAREN LESSMINUS expr.c
    {: return Def.root(a,b,c); :}
  | simple_expr.a DOT LBRACKET seq_expr.b RBRACKET LESSMINUS expr.c
  	{: return Def.root(a,b,c); :}
  | simple_expr.a DOT LBRACE expr.b RBRACE LESSMINUS expr.c
	{: return Def.root(a,b,c); :}
  | label.a LESSMINUS expr.b
    {: return Def.root(a,b); :}
  | ASSERT simple_expr.a @ below_SHARP
    {: return a; :}
  | LAZY simple_expr.a @ below_SHARP
    {: return a; :}
  | OBJECT.o class_structure.a END
    {:
    	Def def = new Def("<object>", Def.Type.Object, o.getStart(), o.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | OBJECT class_structure error
    {: return new Def(); :}
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
  | LPAREN seq_expr.a RPAREN
    {: return a; :}
  | LPAREN seq_expr.a error
    {: return a; :}
  | BEGIN seq_expr.a END
    {: return a; :}
  | BEGIN END
    {: return new Def(); :}
  | BEGIN seq_expr.a error
    {: return a; :}
  | LPAREN seq_expr.a type_constraint.b RPAREN
    {: return Def.root(a,b); :}
  | simple_expr.a DOT label_longident.b
    {: return Def.root(a,b); :}
  | mod_longident.a DOT LPAREN seq_expr.b RPAREN
    {: return Def.root(a,b); :}
  | mod_longident.a DOT LPAREN seq_expr.b error
    {: return Def.root(a,b); :}
  | simple_expr.a DOT LPAREN seq_expr.b RPAREN
  	{: return Def.root(a,b); :}
  | simple_expr.a DOT LPAREN seq_expr.b error
    {: return Def.root(a,b); :}
  | simple_expr.a DOT LBRACKET seq_expr.b RBRACKET
    {: return Def.root(a,b); :}
  | simple_expr.a DOT LBRACKET seq_expr.b error
    {: return Def.root(a,b); :}
  | simple_expr.a DOT LBRACE expr.b RBRACE
    {: return Def.root(a,b); :}
  | simple_expr.a DOT LBRACE expr_comma_list.b error
    {: return Def.root(a,b); :}
  | LBRACE record_expr.a RBRACE
    {: return a; :}
  | LBRACE record_expr.a error
    {: return a; :}
  | LBRACKETBAR expr_semi_list.a opt_semi.b BARRBRACKET
    {: return Def.root(a,b); :}
  | LBRACKETBAR expr_semi_list.a opt_semi.b error
    {: return Def.root(a,b); :}
  | LBRACKETBAR BARRBRACKET
    {: return new Def(); :}
  | LBRACKET expr_semi_list.a opt_semi.b RBRACKET
    {: return Def.root(a,b); :}
  | LBRACKET expr_semi_list.a opt_semi.b error
    {: return Def.root(a,b); :}
  | PREFIXOP simple_expr.a
    {: return a; :}
  | BANG simple_expr.a
    {: return a; :}
  | NEW class_longident.a
    {: return a; :}
  | LBRACELESS field_expr_list.a opt_semi.b GREATERRBRACE
    {: return Def.root(a,b); :}
  | LBRACELESS field_expr_list.a opt_semi.b error
    {: return Def.root(a,b); :}
  | LBRACELESS GREATERRBRACE
    {: return new Def(); :}
  | simple_expr.a SHARP label.b
    {: return Def.root(a,b); :}
  | LPAREN MODULE module_expr.a COLON package_type.b RPAREN
    {: return Def.root(a,b); :}
  | LPAREN MODULE module_expr.a COLON error
    {: return a; :}
;
simple_labeled_expr_list=
    labeled_simple_expr.a
    {: return a; :}
  | simple_labeled_expr_list.a labeled_simple_expr.b
    {: return Def.root(a,b); :}
;
labeled_simple_expr=
    simple_expr.a @ below_SHARP
    {: return a; :}
  | label_expr.a
    {: return a; :}
;
label_expr=
    LABEL simple_expr.a @ below_SHARP
    {: return a; :}
  | TILDE label_ident.a
  	{: return a; :}
  | QUESTION label_ident.a
 	{: return a; :}
  | OPTLABEL simple_expr.a @ below_SHARP
    {: return a; :}
;
label_ident=
    LIDENT.id
    {:
    	return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd());
    :}
;
let_bindings=
    let_binding.b
    {: return b; :}
  | let_bindings.a AND let_binding.b
  	{:
  		Def db = (Def)b;
		db.bAnd = true;
  		return Def.root(a,b);
  	:}
;
let_binding=
    val_ident.i fun_binding.f
    {:
    	Def ident = (Def)i;
    	Def def = new Def(ident.name, Def.Type.Let, ident.posStart, ident.posEnd);
    	def.add(f);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | val_ident.i COLON typevar_list DOT core_type EQUAL seq_expr.b
    {:
    	Def ident = (Def)i;
    	Def def = new Def(ident.name, Def.Type.Let, ident.posStart, ident.posEnd);
    	def.add(b);
    	def.collapse();
    	backup(def);
    	return def;
    :}
  | pattern.p EQUAL seq_expr.b
  	{:
  		// find the identifiers defined in this pattern, and
  		// create and return a new node for them

  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)p;

    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);

    		Def def = new Def(ident.name, Def.Type.Let, ident.posStart, ident.posEnd);
    		if(i != 0)
    			def.bAnd = true;

    		root.add(def);
    		last = def;
    	}

    	if(last != null){
    		last.add(b);
    		last.collapse();
    		backup(root);
    		return root;
    	}

    	return Def.root(p, b);
  	:}
;
fun_binding=
    strict_binding.a
    {: return a; :}
  | type_constraint.a EQUAL seq_expr.b
    {: return Def.root(a,b); :}
;
strict_binding=
    EQUAL seq_expr.a
    {: return a; :}
  | labeled_simple_pattern.p fun_binding.b
  	{:
  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)p;

    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	boolean bFirst = true;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);
    		if(isParameter(ident.name)) {
	    		Def def = new Def(ident.name, Def.Type.Parameter, ident.posStart, ident.posEnd);
	    		if(!bFirst)
	    			def.bAnd = true;
	    		bFirst = false;
	    		root.add(def);
	    		last = def;
    		} else {
	    		Def def = new Def(ident.name, Def.Type.Identifier, ident.posStart, ident.posEnd);
	    		root.add(def);
    		}
    	}

    	if(last != null){
    		last.add(b);
    		last.collapse();
    		backup(root);
    		return root;
    	}

    	return Def.root(p, b);
  	:}
  | LPAREN TYPE LIDENT.id RPAREN fun_binding.a
  	{:
    	Def root = new Def();
    	Def def = new Def((String)id.value, Def.Type.Parameter, id.getStart(), id.getEnd());
   		root.add(def);
   		def.add(a);
   		def.collapse();
   		return root;
  	:}
;
match_cases=
    pattern.p match_action.b
  	{:
  		// find the identifiers defined in this pattern, and
  		// create and return a new node for them

  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)p;

    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	boolean bFirst = true;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);
    		if(isParameter(ident.name)) {
	    		Def def = new Def(ident.name, Def.Type.Parameter, ident.posStart, ident.posEnd);
	    		if(!bFirst)
	    			def.bAnd = true;
	    		bFirst = false;
	    		root.add(def);
	    		last = def;
    		} else {
	    		Def def = new Def(ident.name, Def.Type.Identifier, ident.posStart, ident.posEnd);
	    		root.add(def);
    		}
    	}

    	if(last != null){
    		last.add(b);
    		last.collapse();
    		return root;
    	}

    	return Def.root(p, b);
  	:}
  | match_cases.a BAR pattern.b match_action.c
  	{:
  		// find the identifiers defined in this pattern, and
  		// create and return a new node for them

  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)b;

    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	boolean bFirst = true;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);
    		if(isParameter(ident.name)) {
	    		Def def = new Def(ident.name, Def.Type.Parameter, ident.posStart, ident.posEnd);
	    		if(!bFirst)
	    			def.bAnd = true;
	    		bFirst = false;
	    		root.add(def);
	    		last = def;
    		} else {
	    		Def def = new Def(ident.name, Def.Type.Identifier, ident.posStart, ident.posEnd);
	    		root.add(def);
    		}
    	}

    	if(last != null){
    		last.add(c);
    		last.collapse();
    		return Def.root(a, root);
    	}

    	return Def.root(a, b, c);
  	:}
;
fun_def=
    match_action.a
    {: return a; :}
  | labeled_simple_pattern.p fun_def.b
  	{:
  		// find the identifiers defined in this pattern, and
  		// create and return a new node for them

  		ArrayList<Def> idents = new ArrayList<Def>();
    	Def pat = (Def)p;

    	pat.findIdents(idents);

    	Def root = new Def();

    	Def last = null;
    	boolean bFirst = true;
    	for(int i = 0; i < idents.size(); i++){
    		Def ident = idents.get(i);
    		if(isParameter(ident.name)) {
	    		Def def = new Def(ident.name, Def.Type.Parameter, ident.posStart, ident.posEnd);
	    		if(!bFirst)
	    			def.bAnd = true;
	    		bFirst = false;
	    		root.add(def);
	    		last = def;
    		} else {
	    		Def def = new Def(ident.name, Def.Type.Identifier, ident.posStart, ident.posEnd);
	    		root.add(def);
    		}
    	}

    	if(last != null){
    		last.add(b);
    		last.collapse();
    		return root;
    	}

    	return Def.root(root, b);
  	:}
  | LPAREN TYPE LIDENT.id RPAREN fun_def.a
  	{:
    	Def root = new Def();
    	Def def = new Def((String)id.value, Def.Type.Parameter, id.getStart(), id.getEnd());
   		root.add(def);
   		def.add(a);
   		def.collapse();
   		return root;
  	:}
;
match_action=
    MINUSGREATER seq_expr.a
    {: return a; :}
  | WHEN seq_expr.a MINUSGREATER seq_expr.b
  	{: return Def.root(a,b); :}
;
expr_comma_list=
    expr_comma_list.a COMMA expr.b
    {: return Def.root(a,b); :}
  | expr.a COMMA expr.b
  	{: return Def.root(a,b); :}
;
record_expr=
    simple_expr.a WITH lbl_expr_list.b opt_semi.c
    {: return Def.root(a,b,c); :}
  | lbl_expr_list.a opt_semi.b
  	{: return Def.root(a,b); :}
;
lbl_expr_list=
    label_longident.a EQUAL expr.b
    {: return Def.root(a,b); :}
  | label_longident.a
    {: return a; :}
  | lbl_expr_list.a SEMI label_longident.b EQUAL expr.c
    {: return Def.root(a,b,c); :}
  | lbl_expr_list.a SEMI label_longident.b
    {: return Def.root(a,b); :}
;
field_expr_list=
    label.a EQUAL expr.b
    {: return Def.root(a,b); :}
  | field_expr_list.a SEMI label.b EQUAL expr.c
    {: return Def.root(a,b,c); :}
;
expr_semi_list=
    expr.a
    {: return a; :}
  | expr_semi_list.a SEMI expr.b
  	{: return Def.root(a,b); :}
;
type_constraint=
    COLON core_type.a
    {: return a; :}
  | COLON core_type.a COLONGREATER core_type.b
  	{: return Def.root(a,b); :}
  | COLONGREATER core_type.a
  	{: return a; :}
  | COLON error
  	{: return new Def(); :}
  | COLONGREATER error
  	{: return new Def(); :}
;

/* Patterns */

pattern=
    simple_pattern.a
    {: return a; :}
  | pattern.a AS val_ident.b
  	{: return Def.root(a,b); :}
  | pattern_comma_list.a  @ below_COMMA
  	{: return a; :}
  | constr_longident.a pattern.b @ prec_constr_appl
    {: return Def.root(a,b); :}
  | name_tag.a pattern.b @ prec_constr_appl
    {: return Def.root(a,b); :}
  | pattern.a COLONCOLON pattern.b
  	{: return Def.root(a,b); :}
  | LPAREN COLONCOLON RPAREN LPAREN pattern.a COMMA pattern.b RPAREN
  	{: return Def.root(a,b); :}
  | pattern.a BAR pattern.b
	{: return Def.root(a,b); :}
  | LAZY simple_pattern.a
  	{: return a; :}
;
simple_pattern=
    val_ident.a @ below_EQUAL
    {: return a; :}
  | UNDERSCORE.id
  	{: return new Def("_", Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | signed_constant.a
    {: return a; :}
  | CHAR DOTDOT CHAR
  	{: return new Def(); :}
  | constr_longident.a
  	{: return a; :}
  | name_tag.a
    {: return a; :}
  | SHARP type_longident.a
    {: return a; :}
  | LBRACE lbl_pattern_list.a record_pattern_end RBRACE
    {: return a; :}
  | LBRACE lbl_pattern_list.a opt_semi error
    {: return a; :}
  | LBRACKET pattern_semi_list.a opt_semi RBRACKET
    {: return a; :}
  | LBRACKET pattern_semi_list.a opt_semi error
    {: return a; :}
  | LBRACKETBAR pattern_semi_list.a opt_semi BARRBRACKET
    {: return a; :}
  | LBRACKETBAR BARRBRACKET
    {: return new Def(); :}
  | LBRACKETBAR pattern_semi_list.a opt_semi error
    {: return a; :}
  | LPAREN pattern.a RPAREN
    {: return a; :}
  | LPAREN pattern.a error
    {: return a; :}
  | LPAREN pattern.a COLON core_type.b RPAREN
    {: return Def.root(a,b); :}
  | LPAREN pattern.a COLON core_type.b error
    {: return Def.root(a,b); :}
;

pattern_comma_list=
    pattern_comma_list.a COMMA pattern.b
    {: return Def.root(a,b); :}
  | pattern.a COMMA pattern.b
    {: return Def.root(a,b); :}
;
pattern_semi_list=
    pattern.a
  	{: return a; :}
  | pattern_semi_list.a SEMI pattern.b
    {: return Def.root(a,b); :}
;
lbl_pattern_list=
    label_longident.a EQUAL pattern.b
    {: return Def.root(a,b); :}
  | label_longident.a
  	{: return a; :}
  | lbl_pattern_list.a SEMI label_longident.b EQUAL pattern.c
    {: return Def.root(a,b,c); :}
  | lbl_pattern_list.a SEMI label_longident.b
    {: return Def.root(a,b); :}
;
record_pattern_end=
    opt_semi.a
  	{: return a; :}
  | SEMI UNDERSCORE opt_semi.a
  	{: return a; :}
;

/* Primitive declarations */

primitive_declaration=
    STRING
    {: return new Def(); :}
  | STRING primitive_declaration.a
   	{: return a; :}
;

/* Type declarations */

type_declarations=
    type_declaration.a                            /*{ [$1] }*/
    {: return a; :}
  | type_declarations.a AND.n type_declaration.b      /*{ $3 :: $1 }*/
  	{:
  		Def db = (Def)b;
		db.bAnd = true;
		db.defPosStart = n.getStart();
  		return Def.root(a,b);
  	:}
;

type_declaration=
    type_parameters LIDENT.id type_kind.a constraints
    {:
    	Def def = new Def((String)id.value, Def.Type.Type, id.getStart(), id.getEnd());
    	def.add(a);
    	def.collapse();
    	backup(def);
    	return def;
    :}
;
constraints=
    constraints CONSTRAINT constrain        /*{ $3 :: $1 }*/
	{: return new Def(); :}
  | /* empty */                             /*{ [] }*/
    {: return new Def(); :}
;
type_kind=
    /*empty*/
    {: return new Def(); :}
  | EQUAL core_type
    {: return new Def(); :}
  | EQUAL PRIVATE core_type
    {: return new Def(); :}
  | EQUAL constructor_declarations.a
    {: return a; :}
  | EQUAL PRIVATE constructor_declarations.a
    {: return a; :}
  | EQUAL private_flag BAR constructor_declarations.a
    {: return a; :}
  | EQUAL private_flag LBRACE label_declarations.a opt_semi RBRACE
    {: return a; :}
  | EQUAL core_type EQUAL private_flag opt_bar constructor_declarations.a
    {: return a; :}
  | EQUAL core_type EQUAL private_flag LBRACE label_declarations.a opt_semi RBRACE
    {: return a; :}
;
type_parameters=
    /*empty*/                                   /*{ [] }*/
    {: return new Def(); :}
  | type_parameter                              /*{ [$1] }*/
    {: return new Def(); :}
  | LPAREN type_parameter_list RPAREN           /*{ List.rev $2 }*/
    {: return new Def(); :}
;
type_parameter=
    type_variance QUOTE ident                   /*{ $3, $1 }*/
    {: return new Def(); :}
;
type_variance=
    /* empty */                                 /*{ false, false }*/
    {: return new Def(); :}
  | PLUS                                        /*{ true, false }*/
    {: return new Def(); :}
  | MINUS                                       /*{ false, true }*/
    {: return new Def(); :}
;
type_parameter_list=
    type_parameter                              /*{ [$1] }*/
    {: return new Def(); :}
  | type_parameter_list COMMA type_parameter    /*{ $3 :: $1 }*/
    {: return new Def(); :}
;

/* variant type constructors */
constructor_declarations=
    constructor_declaration.a
    {: return a; :}
  | constructor_declarations.a BAR constructor_declaration.b
  	{: return Def.root(a,b); :}
;
constructor_declaration=
    constr_ident.a constructor_arguments
    {: return a; :}
;
constructor_arguments=
    /*empty*/                                   /*{ [] }*/
	{: return new Def(); :}
  | OF core_type_list                           /*{ List.rev $2 }*/
	{: return new Def(); :}
;

/* record type constructors */
label_declarations=
    label_declaration.a
	{: return a; :}
  | label_declarations.a SEMI label_declaration.b
	{: return Def.root(a,b); :}
;
label_declaration=
    mutable_flag.m label.a COLON poly_type
	{:
		// transform the generic identifier into a type constructor
		Def def = (Def) a;
		Def da = new Def(def.name, Def.Type.RecordTypeConstructor, def.posStart, def.posEnd);
    	int pos = ((Def)m).posStart;
    	da.defPosStart = (pos != 0 ? pos : def.posStart);
		return da;
	:}

;

/* "with" constraints (additional type equations over signature components) */

with_constraints=
    with_constraint                             /*{ [$1] }*/
	{: return new Def(); :}
  | with_constraints AND with_constraint        /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
with_constraint=
    TYPE type_parameters label_longident with_type_binder core_type constraints
	{: return new Def(); :}
  | TYPE type_parameters label_longident COLONEQUAL core_type
	{: return new Def(); :}
  | MODULE mod_longident EQUAL mod_ext_longident
	{: return new Def(); :}
  | MODULE mod_longident COLONEQUAL mod_ext_longident
	{: return new Def(); :}
;
with_type_binder=
    EQUAL
	{: return new Def(); :}
  | EQUAL PRIVATE
	{: return new Def(); :}
;

/* Polymorphic types */

typevar_list=
        QUOTE ident                             /*{ [$2] }*/
	{: return new Def(); :}
      | typevar_list QUOTE ident                /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
poly_type=
        core_type
	{: return new Def(); :}
      | typevar_list DOT core_type
	{: return new Def(); :}
;

/* Core types */

core_type=
    core_type2
	{: return new Def(); :}
  | core_type2 AS QUOTE ident
	{: return new Def(); :}
;
core_type2=
    simple_core_type_or_tuple
	{: return new Def(); :}
  | QUESTION LIDENT COLON core_type2 MINUSGREATER core_type2
	{: return new Def(); :}
  | OPTLABEL core_type2 MINUSGREATER core_type2
	{: return new Def(); :}
  | LIDENT COLON core_type2 MINUSGREATER core_type2
	{: return new Def(); :}
  | core_type2 MINUSGREATER core_type2
	{: return new Def(); :}
;

simple_core_type=
    simple_core_type2  @ below_SHARP
	{: return new Def(); :}
  | LPAREN core_type_comma_list RPAREN @ below_SHARP
	{: return new Def(); :}
;
simple_core_type2=
    QUOTE ident
	{: return new Def(); :}
  | UNDERSCORE
	{: return new Def(); :}
  | type_longident
	{: return new Def(); :}
  | simple_core_type2 type_longident
	{: return new Def(); :}
  | LPAREN core_type_comma_list RPAREN type_longident
	{: return new Def(); :}
  | LESS meth_list GREATER
	{: return new Def(); :}
  | LESS GREATER
	{: return new Def(); :}
  | SHARP class_longident opt_present
	{: return new Def(); :}
  | simple_core_type2 SHARP class_longident opt_present
	{: return new Def(); :}
  | LPAREN core_type_comma_list RPAREN SHARP class_longident opt_present
	{: return new Def(); :}
  | LBRACKET tag_field RBRACKET
	{: return new Def(); :}
  | LBRACKET BAR row_field_list RBRACKET
	{: return new Def(); :}
  | LBRACKET row_field BAR row_field_list RBRACKET
	{: return new Def(); :}
  | LBRACKETGREATER opt_bar row_field_list RBRACKET
	{: return new Def(); :}
  | LBRACKETGREATER RBRACKET
	{: return new Def(); :}
  | LBRACKETLESS opt_bar row_field_list RBRACKET
	{: return new Def(); :}
  | LBRACKETLESS opt_bar row_field_list GREATER name_tag_list RBRACKET
	{: return new Def(); :}
  | LPAREN MODULE package_type RPAREN
	{: return new Def(); :}
;
package_type=
    mty_longident
	{: return new Def(); :}
  | mty_longident WITH package_type_cstrs
	{: return new Def(); :}
;
package_type_cstr=
    TYPE LIDENT EQUAL core_type
	{: return new Def(); :}
;
package_type_cstrs=
    package_type_cstr
	{: return new Def(); :}
  | package_type_cstr AND package_type_cstrs
	{: return new Def(); :}
;
row_field_list=
    row_field                                   /*{ [$1] }*/
	{: return new Def(); :}
  | row_field_list BAR row_field                /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
row_field=
    tag_field                                   /*{ $1 }*/
	{: return new Def(); :}
  | simple_core_type2                           /*{ Rinherit $1 }*/
	{: return new Def(); :}
;
tag_field=
    name_tag OF opt_ampersand amper_type_list
	{: return new Def(); :}
  | name_tag
	{: return new Def(); :}
;
opt_ampersand=
    AMPERSAND                                   /*{ true }*/
	{: return new Def(); :}
  | /* empty */                                 /*{ false }*/
	{: return new Def(); :}
;
amper_type_list=
    core_type                                   /*{ [$1] }*/
	{: return new Def(); :}
  | amper_type_list AMPERSAND core_type         /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
opt_present=
    LBRACKETGREATER name_tag_list RBRACKET      /*{ List.rev $2 }*/
	{: return new Def(); :}
  | /* empty */                                 /*{ [] }*/
	{: return new Def(); :}
;
name_tag_list=
    name_tag                                    /*{ [$1] }*/
	{: return new Def(); :}
  | name_tag_list name_tag                      /*{ $2 :: $1 }*/
	{: return new Def(); :}
;
simple_core_type_or_tuple=
    simple_core_type                            /*{ $1 }*/
	{: return new Def(); :}
  | simple_core_type STAR core_type_list
	{: return new Def(); :}
      /*{ mktyp(Ptyp_tuple($1 :: List.rev $3)) }*/
;
core_type_comma_list=
    core_type                                   /*{ [$1] }*/
	{: return new Def(); :}
  | core_type_comma_list COMMA core_type        /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
core_type_list=
    simple_core_type                            /*{ [$1] }*/
	{: return new Def(); :}
  | core_type_list STAR simple_core_type        /*{ $3 :: $1 }*/
	{: return new Def(); :}
;
meth_list=
    field SEMI meth_list                        /*{ $1 :: $3 }*/
	{: return new Def(); :}
  | field opt_semi                              /*{ [$1] }*/
	{: return new Def(); :}
  | DOTDOT                                      /*{ [mkfield Pfield_var] }*/
	{: return new Def(); :}
;
field=
    label COLON poly_type                       /*{ mkfield(Pfield($1, $3)) }*/
	{: return new Def(); :}
;
label=
    LIDENT.id                                      /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
;

/* Constants */

constant=
    INT                                         /*{ Const_int $1 }*/
	{: return new Def(); :}
  | CHAR                                        /*{ Const_char $1 }*/
	{: return new Def(); :}
  | STRING                                      /*{ Const_string $1 }*/
	{: return new Def(); :}
  | FLOAT                                       /*{ Const_float $1 }*/
	{: return new Def(); :}
  | INT32                                       /*{ Const_int32 $1 }*/
	{: return new Def(); :}
  | INT64                                       /*{ Const_int64 $1 }*/
	{: return new Def(); :}
  | NATIVEINT                                   /*{ Const_nativeint $1 }*/
	{: return new Def(); :}
;
signed_constant=
    constant                                    /*{ $1 }*/
	{: return new Def(); :}
  | MINUS INT                                   /*{ Const_int(- $2) }*/
	{: return new Def(); :}
  | MINUS FLOAT                                 /*{ Const_float("-" ^ $2) }*/
	{: return new Def(); :}
  | MINUS INT32                                 /*{ Const_int32(Int32.neg $2) }*/
	{: return new Def(); :}
  | MINUS INT64                                 /*{ Const_int64(Int64.neg $2) }*/
	{: return new Def(); :}
  | MINUS NATIVEINT                             /*{ Const_nativeint(Nativeint.neg $2) }*/
	{: return new Def(); :}
  | PLUS INT                                    /*{ Const_int $2 }*/
	{: return new Def(); :}
  | PLUS FLOAT                                  /*{ Const_float $2 }*/
	{: return new Def(); :}
  | PLUS INT32                                  /*{ Const_int32 $2 }*/
	{: return new Def(); :}
  | PLUS INT64                                  /*{ Const_int64 $2 }*/
	{: return new Def(); :}
  | PLUS NATIVEINT                              /*{ Const_nativeint $2 }*/
	{: return new Def(); :}
;
/* Identifiers and long identifiers */

ident=
    UIDENT.id                                      /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | LIDENT.id                                      /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
;
val_ident=
    LIDENT.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | LPAREN operator.o RPAREN                      /*{ $2 }*/
    {:
    	Def op = (Def)o;
    	return new Def(op.name, Def.Type.Identifier, op.posStart, op.posEnd);
    :}
;
operator=
    PREFIXOP.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | INFIXOP0.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | INFIXOP1.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | INFIXOP2.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | INFIXOP3.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | INFIXOP4.id                                    /*{ $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | BANG.id                                        /*{ "!" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | PLUS.id                                        /*{ "+" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | PLUSDOT.id                                     /*{ "+." }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | MINUS.id                                       /*{ "-" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | MINUSDOT.id                                    /*{ "-." }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | STAR.id                                        /*{ "*" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | EQUAL.id                                       /*{ "=" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | LESS.id                                        /*{ "<" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | GREATER.id                                     /*{ ">" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | OR.id                                          /*{ "or" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | BARBAR.id                                      /*{ "||" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | AMPERSAND.id                                   /*{ "&" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | AMPERAMPER.id                                  /*{ "&&" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | COLONEQUAL.id                                  /*{ ":=" }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
;
constr_ident=
    UIDENT.id                                      /*{ $1 }*/
    {:
    	Def def = new Def((String)id.value, Def.Type.TypeConstructor, id.getStart(), id.getEnd());
    	def.defPosStart = id.getStart();
    	return def;
    :}
  | LPAREN.a RPAREN.b                               /*{ "()" }*/
    {: return new Def("()", Def.Type.TypeConstructor, a.getStart(), b.getEnd()); :}
  | COLONCOLON.a                                  /*{ "::" }*/
    {: return new Def("::", Def.Type.TypeConstructor, a.getStart(), a.getEnd()); :}
  | FALSE.id                                       /*{ "false" }*/
    {: return new Def("false", Def.Type.TypeConstructor, id.getStart(), id.getEnd()); :}
  | TRUE.id                                        /*{ "true" }*/
    {: return new Def("true", Def.Type.TypeConstructor, id.getStart(), id.getEnd()); :}
;

val_longident=
    val_ident.a
    {: return a; :}
  | mod_longident.a DOT val_ident.b
  	{:
  		Def da = (Def)a;
  		Def db = (Def)b;
  		return new Def(da.name + "." + db.name, Def.Type.Identifier, da.posStart, db.posEnd);
  	:}
;
constr_longident=
    mod_longident.a       @ below_DOT             /*{ $1 }*/
    {: return a; :}
  | LBRACKET RBRACKET                           /*{ Lident "[]" }*/
  	{: return new Def(); :}
  	//{: return new Def("[]", Def.Type.Identifier, a.getStart(), b.getEnd()); :}
  | LPAREN.a RPAREN.b                               /*{ Lident "()" }*/
  	//{: return new Def(); :}
  	{: return new Def("()", Def.Type.Identifier, a.getStart(), b.getEnd()); :}
  | FALSE                                       /*{ Lident "false" }*/
  	{: return new Def(); :}
  	//{: return new Def("false", Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | TRUE                                        /*{ Lident "true" }*/
  	{: return new Def(); :}
  	//{: return new Def("true", Def.Type.Identifier, id.getStart(), id.getEnd()); :}
;
label_longident=
    LIDENT.id                                      /*{ Lident $1 }*/
	{: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_longident.a DOT LIDENT.id                    /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
;
type_longident=
    LIDENT.id                                      /*{ Lident $1 }*/
	{: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_ext_longident.a DOT LIDENT.id                /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
;
mod_longident=
    UIDENT.id                                      /*{ Lident $1 }*/
	{: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_longident.a DOT UIDENT.id                    /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
;
mod_ext_longident=
    UIDENT.id                                      /*{ Lident $1 }*/
	{: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_ext_longident.a DOT UIDENT.id                /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
  | mod_ext_longident.a LPAREN mod_ext_longident.b RPAREN.par /*{ Lapply($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		Def db = (Def)b;
  		return new Def(da.name + "(" + db.name + ")", Def.Type.Identifier, da.posStart, par.getEnd());
  	:}
;
mty_longident=
    ident.a                                       /*{ Lident $1 }*/
    {: return a; :}
  | mod_ext_longident.a DOT ident.b                 /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		Def db = (Def)b;
  		return new Def(da.name + "." + db.name, Def.Type.Identifier, da.posStart, db.posEnd);
  	:}
;
clty_longident=
    LIDENT.id                                      /*{ Lident $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_ext_longident.a DOT LIDENT.id                /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
;
class_longident=
    LIDENT.id                                      /*{ Lident $1 }*/
    {: return new Def((String)id.value, Def.Type.Identifier, id.getStart(), id.getEnd()); :}
  | mod_longident.a DOT LIDENT.id                    /*{ Ldot($1, $3) }*/
  	{:
  		Def da = (Def)a;
  		return new Def(da.name + "." + (String)id.value, Def.Type.Identifier, da.posStart, id.getEnd());
  	:}
;

/* Toplevel directives */

toplevel_directive=
    SHARP ident                 /*{ Ptop_dir($2, Pdir_none) }*/
    {: return new Def(); :}
  | SHARP ident STRING          /*{ Ptop_dir($2, Pdir_string $3) }*/
    {: return new Def(); :}
  | SHARP ident INT             /*{ Ptop_dir($2, Pdir_int $3) }*/
    {: return new Def(); :}
  | SHARP ident val_longident   /*{ Ptop_dir($2, Pdir_ident $3) }*/
    {: return new Def(); :}
  | SHARP ident FALSE           /*{ Ptop_dir($2, Pdir_bool false) }*/
    {: return new Def(); :}
  | SHARP ident TRUE            /*{ Ptop_dir($2, Pdir_bool true) }*/
    {: return new Def(); :}
;

/* Miscellaneous */

name_tag=
    BACKQUOTE ident.a                             /*{ $2 }*/
    {: return a; :}
;
rec_flag=
    /* empty */                                 /*{ Nonrecursive }*/
    {: return new Def(); :}
  | REC                                         /*{ Recursive }*/
    {: Def def = new Def(); def.bRec = true; return def; :}
;
direction_flag=
    TO                                          /*{ Upto }*/
    {: return new Def(); :}
  | DOWNTO                                      /*{ Downto }*/
    {: return new Def(); :}
;
private_flag=
    /* empty */                                 /*{ Public }*/
    {: return new Def(); :}
  | PRIVATE.p                                     /*{ Private }*/
    {: Def def = new Def(); def.bAlt=true; def.posStart = p.getStart(); return def; :}
;
mutable_flag=
    /* empty */                                 /*{ Immutable }*/
    {: return new Def(); :}
  | MUTABLE.m                                     /*{ Mutable }*/
    {: Def def = new Def(); def.bAlt=true; def.posStart = m.getStart(); return def; :}
;
virtual_flag=
    /* empty */                                 /*{ Concrete }*/
    {: return new Def(); :}
  | VIRTUAL.v                                   /*{ Virtual }*/
    {: Def def = new Def(); def.bAlt=true; def.posStart = v.getStart(); return def; :}
;
override_flag=
    /* empty */                                 /*{ Fresh }*/
    {: return new Def(); :}
  | BANG.b                                      /*{ Override }*/
    {: Def def = new Def(); def.bAlt=true; def.posStart = b.getStart(); return def; :}
;
opt_bar=
    /* empty */                                 /*{ () }*/
    {: return new Def(); :}
  | BAR                                         /*{ () }*/
    {: return new Def(); :}
;
opt_semi=
   /* empty */                                 /*{ () }*/
    {: return new Def(); :}
  | SEMI                                        /*{ () }*/
    {: return new Def(); :}
;
subtractive=
    MINUS                                       /*{ "-" }*/
    {: return new Def(); :}
  | MINUSDOT                                    /*{ "-." }*/
    {: return new Def(); :}
;
additive=
    PLUS                                        /*{ "+" }*/
    {: return new Def(); :}
  | PLUSDOT                                     /*{ "+." }*/
    {: return new Def(); :}
;
