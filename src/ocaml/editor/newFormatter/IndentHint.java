package ocaml.editor.newFormatter;

import java.util.Comparator;

public class IndentHint {
	public enum Type {
		INDENT_BEGIN, DEDENT_BEGIN, /* begin - end */
		INDENT_STRUCT, DEDENT_STRUCT, /* struct - end */
		INDENT_IN, DEDENT_IN, /* indentation after 'in' */
		INDENT_DEF, DEDENT_DEF, /* indentation after '=' */
		INDENT_FOR, DEDENT_FOR, /* indentation between 'do' and 'done' */
		INDENT_THEN, DEDENT_THEN,
		INDENT_ELSE, DEDENT_ELSE,
		INDENT_WHILE, DEDENT_WHILE,
		INDENT_MATCH_ACTION, DEDENT_MATCH_ACTION,
		INDENT_FUNCTOR, DEDENT_FUNCTOR,
	};

	private Type type;
	private int pos;

	public IndentHint(Type type, int pos) {
		this.type = type;
		this.pos = pos;
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
		return "Hint(" + type.name() + ":" + getLine() + "," + getColumn()
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
}
