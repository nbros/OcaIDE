package ocaml.build.util;

/** This exception is generated when we detect a cycle in the dependencies graph. */
public class CycleException extends Exception {
	private static final long serialVersionUID = 1L;

	public CycleException() {
	}
}