package ocaml.debugging;

/**
 * Represents the current debug position. 
 * 
 * @see DebugMarkers
 */
public class DebugCurrentPosition {

	private final String filename;
	private final int offset;

	public DebugCurrentPosition(String filename, int offset) {
		this.filename = filename;
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}
	
	public String getFilename() {
		return filename;
	}
}
