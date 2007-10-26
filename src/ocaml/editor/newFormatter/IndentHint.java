package ocaml.editor.newFormatter;

import java.util.Comparator;

public class IndentHint {
	public enum Type {
		BEGIN, /* begin - end */
		STRUCT,  /* struct - end */
		SIG,  /* sig - end */
		IN,  /* indentation after 'in' */
		DEF, /* indentation after '=' */
		FOR,  /* indentation between 'do' and 'done' */
		THEN, 
		ELSE,
		WHILE,
		MATCH_ACTION,
		FIRST_MATCH_ACTION,
		FUNCTOR,
		TRY,
		WITH,
		OBJECT,
		APP, /* function application */
		RECORD, /* {a=.., b=..} */
		FIRST_CONTRUCTOR,
		PAREN,
		FIRSTCATCH, /* first case in a try with */
		FUNARGS,
		MODULECONSTRAINT,
	};
	
	/** indent or dedent? */
	public boolean indent;

	private Type type;
	private int pos;
	
	/** the dedent (resp. indent) hint for an indent (resp. dedent) hint */
	public IndentHint counterpart;

	public IndentHint(Type type, boolean indent, int pos) {
		this.type = type;
		this.pos = pos;
		this.indent = indent;
	}

	public Type getType() {
		return type;
	}

	public int getLine() {
		return Pos.getLine(pos);
	}

	public int getColumn() {
		return Pos.getColumn(pos);
	}

	@Override
	public String toString() {
		return (indent?"Indent":"Dedent") + "Hint(" + type.name() + ":" + (getLine() + 1) + "," + (getColumn()+1)
				+ ")";
	}

	public static class HintComparator implements Comparator<IndentHint> {
		public int compare(IndentHint o1, IndentHint o2) {
			IndentHint a = (IndentHint) o1;
			IndentHint b = (IndentHint) o2;

			if (a.getLine() < b.getLine())
				return -1;
			else if (a.getLine() > b.getLine())
				return +1;
			else {
				if (a.getColumn() < b.getColumn())
					return -1;
				else if (a.getColumn() > b.getColumn())
					return +1;
				else
					return 0;
			}
		}

	}
	
	public boolean isIndent(){
		return indent;
	}
	
	public IndentHint getCounterpart() {
		return counterpart;
	}
	
	public int getIndent(){
		if(type == Type.FUNARGS)
			return 2;
		
		
		return 1;
	}
}
