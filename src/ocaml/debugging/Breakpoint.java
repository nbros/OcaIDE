package ocaml.debugging;

/** A breakpoint in a ".ml" source file */
public class Breakpoint {

	private final int number;
	private final int line;
	private final int offset;
	private final String filename;

	public Breakpoint(int number, int line, int offset, String filename) {
		this.number = number;
		this.line = line;
		this.offset = offset;
		this.filename = filename;
	}

	public int getNumber() {
		return number;
	}

	public int getLine() {
		return line;
	}

	public int getOffset() {
		return offset;
	}

	public String getFilename() {
		return filename;
	}
}