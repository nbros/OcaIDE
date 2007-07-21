package ocaml.exec;

/**
 * This interface is used to provide a call-back mechanism, so that we can get the output from the process as
 * they happen
 */
public interface IExecEvents {
	// This method gets called when the process sent us a new input String.
	public void processNewInput(String input);

	// This method gets called when the process sent us a new error String.
	public void processNewError(String error);

	// This method gets called when the process has ended.
	public void processEnded(int exitValue);
}
