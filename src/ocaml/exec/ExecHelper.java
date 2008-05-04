package ocaml.exec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;

/**
 * Execute an external process, and permits interactive communication with the process. To receive
 * the output from the process, we use the IExecEvents call-back interface.
 */
public class ExecHelper implements Runnable {
	// Allocate 1K buffers for Input and Error Streams.
	private byte[] inBuffer = new byte[1024];

	private byte[] errBuffer = new byte[1024];

	// Declare internal variables we will need.
	private Process process;

	private InputStream pErrorStream;

	private InputStream pInputStream;

	private OutputStream pOutputStream;

	private PrintWriter outputWriter;

	private Thread inReadThread;

	private Thread errReadThread;

	private IExecEvents handler;

	/** Do not use this constructor directly unless necessary. Use the static "exec" methods instead */
	public ExecHelper(IExecEvents ep, Process p) {
		// Save variables.
		handler = ep;
		process = p;
		// Get the streams.
		pErrorStream = process.getErrorStream();
		pInputStream = process.getInputStream();
		pOutputStream = process.getOutputStream();
		// Create a PrintWriter on top of the output stream.
		outputWriter = new PrintWriter(pOutputStream, true);
		// Create the threads and start them.
		inReadThread = new Thread(this);
		errReadThread = new Thread(this);
		new Thread() {
			public void run() {
				try {
					// This Thread just waits for the process to end and notifies the handler.
					int returnValue = -1;
					returnValue = process.waitFor();

					inReadThread.join();
					errReadThread.join();

					processEnded(returnValue);
				} catch (InterruptedException ex) {
					OcamlPlugin.logError("ocaml plugin error", ex);
				} finally {
					// means that the process ended
					process = null;
				}
			}
		}.start();
		inReadThread.start();
		errReadThread.start();
	}

	private void processEnded(int exitValue) {
		// Handle process end.
		handler.processEnded(exitValue);
	}

	private void processNewInput(String input) {
		// Handle process new input.
		handler.processNewInput(input);
	}

	private void processNewError(String error) {
		// Handle process new error.
		handler.processNewError(error);
	}

	/** Run the command and return the ExecHelper wrapper object. */
	public static ExecHelper exec(IExecEvents handler, String command) throws IOException {
		return new ExecHelper(handler, Runtime.getRuntime().exec(command));
	}

	/** Create a new instance of ExecHelper while giving an already created process */
	public static ExecHelper exec(IExecEvents handler, Process process) throws IOException {
		return new ExecHelper(handler, process);
	}

	/** Run the command and return the ExecHelper wrapper object. */
	public static ExecHelper exec(IExecEvents handler, String command[], String[] envp, File dir)
			throws IOException {
		return new ExecHelper(handler, Runtime.getRuntime().exec(command, envp, dir));
	}

	/**
	 * Start a process and merge its error and output streams
	 * 
	 * @see ExecHelper#exec()
	 */
	public static ExecHelper execMerge(IExecEvents handler, String command[]) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true);

		return new ExecHelper(handler, processBuilder.start());
	}

	/**
	 * Start a process Start a process and merge its error and output streams
	 * 
	 * @see ExecHelper#exec()
	 */
	public static ExecHelper execMerge(IExecEvents handler, String command[],
			Map<String, String> envp, File dir) throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(dir);
		processBuilder.redirectErrorStream(true);
		if (envp != null)
			processBuilder.environment().putAll(envp);

		return new ExecHelper(handler, processBuilder.start());
	}

	/** Send the output string through the print writer. */
	public void sendLine(String output) throws IOException {
		outputWriter.println(output);
	}

	/** Send a single byte to the output stream. */
	public void sendByte(byte b) throws IOException {
		pOutputStream.write(b);
	}

	/** Send a single character through the print writer. */
	public void sendChar(char c) throws IOException {
		outputWriter.print(c);
	}

	public void run() {
		// Are we on the InputRead Thread?
		if (inReadThread == Thread.currentThread()) {
			try {
				// Read the InputStream in a loop until we find no more bytes to read.
				for (int i = 0; i > -1; i = pInputStream.read(inBuffer)) {
					// We have a new segment of input, so process it as a String.
					processNewInput(Misc.CRLFtoLF(new String(inBuffer, 0, i)));
				}
			} catch (IOException ex) {
			}
			// Are we on the ErrorRead Thread?
		} else if (errReadThread == Thread.currentThread()) {
			try {
				// Read the ErrorStream in a loop until we find no more bytes to read.
				for (int i = 0; i > -1; i = pErrorStream.read(errBuffer)) {
					// We have a new segment of error, so process it as a String.
					processNewError(Misc.CRLFtoLF(new String(errBuffer, 0, i)));
				}
			} catch (IOException ex) {
			}
		}
	}

	/** Kill the process */
	public void kill() {
		if (process != null)
			process.destroy();
		process = null;
	}

	/** Is the process still running? */
	public boolean isRunning() {
		return process != null;
	}

	/** Wait for the process to end */
	public void join() {
		try {
			if (process != null)
				process.waitFor();
			process = null;
		} catch (InterruptedException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}

	}

}
