package ocaml.views.ast;

import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.IOException;

import ocaml.OcamlPlugin;
import ocaml.exec.ExecHelper;
import ocaml.exec.IExecEvents;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

public class Indexer implements IExecEvents {

	/** End Of Text character; used to signal the end of the output of a command */
	private static final char EOT = '\003';

	/** The possible states of the debugger */
	private enum State {
		NotStarted, Starting, Idle, ParseAstToXml
	};

	/** The current state of the debugger: idle, not started, stepping... */
	private State state;

	private static Indexer instance;
	private ExecHelper execHelper;
	private StringBuilder indexerOutput;

	private ICallBack callBack;

	private Indexer() {
		state = State.NotStarted;
		indexerOutput = new StringBuilder();
		try {
			start();
		} catch (IOException e) {
			OcamlPlugin.logError(e);
		}
	}

	public static Indexer getInstance() {
		if (instance == null)
			instance = new Indexer();
		return instance;
	}

	public synchronized void getXMLAstFromInput(final String input, final ICallBack callBack) {
		Job job = new Job("Parse Ocaml AST and get XML") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				waitReady();
				if (state == State.Idle) {
					state = State.ParseAstToXml;
					Indexer.this.callBack = callBack;
					send("astFromInput");
					String[] lines = input.split("\\r?\\n");
					send("" + lines.length);
					for (String line : lines) {
						send(line);
					}
				} else {
					OcamlPlugin.logError("Indexer busy (" + state + ")");
				}
				return OK_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}
		
	
	private void waitReady() {
		int timeout = 30;
		synchronized (this) {
			if (state == State.NotStarted) {
				try {
					start();
				} catch (IOException e) {
					OcamlPlugin.logError(e);
					return;
				}
			}
		}
		while (timeout > 0) {
			synchronized (this) {
				if (state == State.Idle)
					return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				OcamlPlugin.logError(e);
			}
			timeout--;
		}
	}

	private synchronized void start() throws IOException {
		state = State.Starting;
		// TODO: use ocamlrun path from preferences
		// "-b" : print stack traces
		execHelper = ExecHelper.exec(this, new String[] { "ocamlrun", "-b",
				/*"C:\\Users\\Pauline\\Documents\\Cours\\PSTL\\workspace\\OcamlPDB\\_build\\main.byte"*/"/home/user/workspace/OcamlPDB/_build/main.byte" }, null, null);
	}

	private synchronized void send(String command) {
		try {
			execHelper.sendLine(command);
		} catch (IOException e) {
			OcamlPlugin.logError(e);
		}
	}

	public synchronized void processEnded(int exitValue) {
		state = State.NotStarted;
		OcamlPlugin.logError("Indexer quit with exit code: " + exitValue);
	}

	public synchronized void processNewError(String error) {
		if (error.length() > 0) {
			OcamlPlugin.logError("Indexer error: " + error);
		}
	}

	public synchronized void processNewInput(final String input) {
		
		//System.out.println("processNewInput state : "+state);
		
		indexerOutput.append(input);
		final String output = indexerOutput.toString();

		final int length = output.length();
		
		//System.out.println("lenght ="+length+"   "+input);
		
		if (length > 0 && output.charAt(length - 1) == EOT) {
			final String result = output.substring(0, length - 1);
			switch (state) {
			case Starting:
				if (result.equals("ok\n")) {
					state = State.Idle;
				} else {
					OcamlPlugin.logError("Error starting indexer. Expected 'ok', received:'"
							+ result + "'");
				}
				break;
			case ParseAstToXml:
				if (callBack != null) {
					callBack.receiveXMLFromInput(result);
				}
				state = State.Idle;
				break;
			case Idle:
				OcamlPlugin.logError("Received unexpected result from indexer: '" + result + "'");
				break;
			case NotStarted:
				OcamlPlugin.logError("Indexer internal state error: received input "
						+ "while not yet started '" + result + "'");
				break;
			}
			indexerOutput.setLength(0);
		}
	}
}
