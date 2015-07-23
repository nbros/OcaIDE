package ocaml.editor.formatting;

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
		posEnd = end + 1;
	}

	/** Creates a position from the positions of the first and last symbols */
	public Pos(Symbol first, Symbol last) {
		super();
		posStart = first.getStart();
		posEnd = last.getEnd() + 1;
	}

	/** Creates a position for a non-terminal consisting of a single terminal */
	public Pos(Symbol s) {
		super();
		posStart = s.getStart();
		posEnd = s.getEnd() + 1;
	}

	/*public Pos(Symbol first, Pos last) {
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
	}*/
	
	public int getStartColumn(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");

		return getColumn(posStart);
	}
	
	public int getEndColumn(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");
		return getColumn(posEnd);
	}

	public int getStartLine(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");
		return getLine(posStart);
	}
	
	public int getEndLine(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");
		return getLine(posEnd);
	}
	
	@Override
	public int getStart(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");
		return posStart; 
	}

	@Override
	public int getEnd(){
		if(this == Pos.NONE)
			System.err.println("Formatter parser: Pos.NONE encountered");
		return posEnd; 
	}
}
