package ocaml.debugging;

import java.util.ArrayList;

import ocaml.OcamlPlugin;

/**
 * Represents the visual indicators overlayed on top of the ocaml text editor:
 * <ul>
 * <li> the current position of the debugger
 * <li> the positions of all the breakpoints defined by the user
 * </ul>
 * 
 * This class uses the singleton pattern because there can only be one ocaml debugger at a time.
 * 
 * @see DebugCurrentPosition
 */
public class DebugMarkers {

	/** the singleton instance */
	private static DebugMarkers instance;

	/** the breakpoints markers list */
	private ArrayList<Breakpoint> breakpoints;

	/** The events markers list */
	// private ArrayList<DebugMarker> eventMarkers;
	/** current position in the debugger (there is only one) */
	private DebugCurrentPosition currentPosition;

	public DebugMarkers() {
		breakpoints = new ArrayList<Breakpoint>();
		currentPosition = null;
	}

	public static synchronized DebugMarkers getInstance() {
		if (instance == null)
			instance = new DebugMarkers();
		return instance;
	}

	public synchronized void setCurrentPosition(String filename, int offset) {
		currentPosition = new DebugCurrentPosition(filename, offset);
	}

	/**
	 * Returns the current debug position in file <code>filename</code> or -1 if the current position
	 * is not inside this file.
	 */
	public synchronized int getPositionInFile(String filename) {
		if (currentPosition != null && currentPosition.getFilename().equals(filename))
			return currentPosition.getOffset();
		else
			return -1;
	}
	
	public synchronized void addBreakpoint(int number, int line, int offset, String filename){
		breakpoints.add(new Breakpoint(number, line, offset, filename));
	}
	
	public synchronized Breakpoint[] getBreakpointsInFile(String filename){
		ArrayList<Breakpoint> result = new ArrayList<Breakpoint>();
		for(Breakpoint breakpoint : breakpoints)
			if(breakpoint.getFilename().equals(filename))
				result.add(breakpoint);
		return result.toArray(new Breakpoint[0]);
	}

	public void clearCurrentPosition() {
		currentPosition = null;		
	}

	public void removeBreakpoints() {
		breakpoints.clear();
	}

	public void removeBreakpoint(int number) {
		Breakpoint toRemove = null;
		for(Breakpoint b : breakpoints)
			if(b.getNumber() == number)
				toRemove = b;
				
		if(toRemove != null)
			breakpoints.remove(toRemove);
		else
			OcamlPlugin.logError("DebugMarkers:removeBreakpoint not found");
	}
}
