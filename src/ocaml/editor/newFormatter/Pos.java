package ocaml.editor.newFormatter;

import beaver.Symbol;

/** Represents a start position and end position */
public class Pos extends beaver.Symbol {
	
	/** Indicates an invalid position (corresponding to an empty terminal) */
	public static final Pos NONE = new Pos();
	

	/** Encoded start position */
	public int posStart;
	/** Encoded end position */
	public int posEnd;

	public Pos() {
		super();
		posStart = -1;
		posEnd = -1;
	}

	public Pos(int start, int end) {
		super();
		posStart = start;
		posEnd = end;
	}

	/** Creates a position from the positions of the first and last symbols */
	public Pos(Symbol first, Symbol last) {
		posStart = first.getStart();
		posEnd = last.getEnd();
	}

	/** Creates a position for a non-terminal consisting of a single terminal */
	public Pos(Symbol s) {
		posStart = s.getStart();
		posEnd = s.getEnd();
	}

	public Pos(Symbol first, Pos last) {
		posStart = first.getStart();
		posEnd = last.posEnd;
	}

	public Pos(Pos first, Symbol last) {
		posStart = first.posStart;
		posEnd = last.getEnd();
	}

	public Pos(Pos first, Pos last) {
		posStart = first.posStart;
		posEnd = last.posEnd;
	}
	
	public int getStartColumn(){
		return getColumn(posStart);
	}
	
	public int getEndColumn(){
		return getColumn(posEnd);
	}

	public int getStartLine(){
		return getLine(posStart);
	}
	
	public int getEndLine(){
		return getLine(posEnd);
	}
	
	@Override
	public int getStart(){
		return posStart; 
	}

	@Override
	public int getEnd(){
		return posEnd; 
	}
}
