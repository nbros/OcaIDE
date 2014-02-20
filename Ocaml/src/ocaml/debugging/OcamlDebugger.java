package ocaml.debugging;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.debugging.views.OcamlBreakpointsView;
import ocaml.debugging.views.OcamlCallStackView;
import ocaml.debugging.views.OcamlWatchView;
import ocaml.editors.OcamlEditor;
import ocaml.exec.ExecHelper;
import ocaml.exec.IExecEvents;
import ocaml.parser.Def;
import ocaml.parsers.OcamlNewInterfaceParser;
import ocaml.perspectives.OcamlDebugPerspective;
import ocaml.perspectives.OcamlPerspective;
import ocaml.preferences.PreferenceConstants;
import ocaml.util.FileUtil;
import ocaml.util.Misc;
import ocaml.util.OcamlPaths;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import beaver.Symbol;

/**
 * Main class of the debugger. Manages the debugger state in a finite state automaton. Communicates
 * with the OCaml debugger through its standard input and output.
 */
public class OcamlDebugger implements IExecEvents {

	/** The possible states of the debugger */
	public enum State {
		NotStarted, Starting1, Starting1a, Starting1b, Starting1c, Starting1d, Starting2, Starting3, Starting3a, Starting4, Idle, Running, RunningBackwards, Stepping, BackStepping, SteppingOver, BackSteppingOver, Frame, SettingFrame, Quitting, PuttingBreakpoint, StepReturn, BackstepReturn, Displaying, BackTrace, RemovingBreakpoint, RemovingBreakpoints, RemovedBreakpoints, Restarting, DisplayingWatchVars, DisplayWatchVars
	};

	/** The current state of the debugger: idle, not started, stepping... */
	private State state;

	/** The project containing the debugged executable */
	private IProject project;
	
	/** list of paths of project */
	private String[] projectPaths;

	/** The debugger process, to which we send messages through its command line interface */
	private ExecHelper debuggerProcess;

	/** The debugged process */
	private Process process;

	/** The singleton instance of the ocaml debugger */
	private static OcamlDebugger instance;

	/** The debugger output since the last action */
	private StringBuilder debuggerOutput;

	/** process executable console */
	private IOConsoleOutputStream console;

	/** debugger error output console */
	private IOConsoleOutputStream errorConsole;

	/**
	 * A list of missing source files, so as to display an error message only once per missing file
	 */
	private ArrayList<String> missingSourceFiles = new ArrayList<String>();

	/** To display the error message only once */
	private boolean bDebuggingInfoMessage = true;

	private ILaunch launch;

	private File byteFile;

	private File runFile;

	private String[] args;

	/** Indicates whether or not remote debugging is enabled. */
	private boolean remoteDebugEnable;

	/** The port on which to listen when remote debugging is enabled. */
	private int remoteDebugPort;

	private String scriptFile;

	/** The list of variables to display in the "variables watch" after each step */
	private ArrayList<String> watchVariables;

	/** The answer of the debugger after sending "print var" for each variable */
	private ArrayList<String> watchVariablesResult;

	/** The index in <code>watchVariables</code> of the next variable to display */
	private int iCurrentWatchVariable;

	/** whether checkpoints are activated */
	private boolean checkpoints;

	/**
	 * private constructor (so that the user cannot create an instance of the debugger)
	 *
	 * @see #getInstance
	 */
	private OcamlDebugger() {
		state = State.NotStarted;
		watchVariables = new ArrayList<String>();
		watchVariablesResult = new ArrayList<String>();
	}

	/** singleton design pattern */
	public synchronized static OcamlDebugger getInstance() {
		if (instance == null)
			instance = new OcamlDebugger();
		return instance;
	}

	/**
	 * Start the debugger
	 *
	 * @param ocamldebug
	 *            the full path of the ocamldebug executable
     * @param runFile
     *            the executable to run
     * @param byteFile
     *            the bytecode file to debug
	 * @param project
	 *            the project in which the executable is started
	 *
	 * @param launch
	 *            the launch object, to which we add the started process
	 *
	 * @param remoteDebugEnable
	 *            whether or not remote debugging should be enabled
	 *
	 * @param remoteDebugPort
	 *            the port on which to listen if remote debugging is enabled
	 *
	 * @param scriptFile
	 *            the script file to execute in debugger
	 *
	 * @param debuggerRootProject
	 *            whether the debugger should be started from the project root instead of the executable folder
	 */
	public synchronized void start(
			String ocamldebug, File runFile, File byteFile, IProject project, ILaunch launch,
			String[] args, boolean remoteDebugEnable, int remoteDebugPort, String scriptFile, boolean debuggerRootProject)
	{
		this.runFile = runFile;
        this.byteFile = byteFile;
		this.args = args;
		this.project = project;
		OcamlPaths opaths = new OcamlPaths(project);
		this.projectPaths = opaths.getPaths();
		this.launch = launch;
		this.remoteDebugEnable = remoteDebugEnable;
		this.remoteDebugPort = remoteDebugPort;
		this.scriptFile = scriptFile;

		bDebuggingInfoMessage = true;

		if (!state.equals(State.NotStarted)) {
			message("The debugger is already started.");
			return;
		}

		debuggerOutput = new StringBuilder();

		if (!byteFile.exists() || !byteFile.isFile()) {
			OcamlPlugin.logError("OcamlDebugger:start : not a file");
			return;
		}

		try {
			// System.err.println("starting debugger on " + exeFile.getAbsolutePath() + " in project
			// " + project.getName());
			state = State.Starting1;

			missingSourceFiles.clear();

			emptyBreakpointsView();
			emptyCallStackView();
			resetWatchVariables();


			/*
			 * FIXME launch shortcuts on exe symbolic links don't work (debugger can't find other
			 * modules)
			 */

			// Build the command line arguments for ocamldebug
			List<String> commandLineArgs = new ArrayList<String>();
			commandLineArgs.add(ocamldebug);
			if (remoteDebugEnable) {
				commandLineArgs.add("-s");
				commandLineArgs.add("0.0.0.0:" + remoteDebugPort);
			}

			OcamlPaths ocamlPaths = new OcamlPaths(project);
			String[] paths = ocamlPaths.getPaths();
			IPath projectLocation = project.getLocation();
			for (String pathStr : paths) {
				pathStr = pathStr.trim();
				if (!".".equals(pathStr)) {
					commandLineArgs.add("-I");
					
					//These paths are either absolute or relative to the project
					//directory. We must convert them to absolute paths in case
					//we are running a bytecode within a nested directory.
					Path path = Paths.get(pathStr);
					if (!path.isAbsolute()) {
						path = Paths.get(projectLocation.append(pathStr).toOSString());
					}
					commandLineArgs.add(path.toString());	
				}
			}

			// add the _build folder (for the cases making project by using Ocamlbuild)
			String buildpath = projectLocation.append("_build").toOSString();
			ArrayList<String> buildFolders = FileUtil.findSubdirectories(buildpath);
			for (String path: buildFolders) {
				commandLineArgs.add("-I");
				commandLineArgs.add(path);
			}

			// add module Camlp4
			commandLineArgs.add("-I");
			commandLineArgs.add("+camlp4");

			// add the root of the project
			commandLineArgs.add("-I");
			commandLineArgs.add(projectLocation.toOSString());

			commandLineArgs.add(byteFile.getPath());
			String[] commandLine = commandLineArgs.toArray(new String[commandLineArgs.size()]);

			File debuggerRoot;
			if(debuggerRootProject)
				debuggerRoot = project.getLocation().toFile();
			else
				debuggerRoot = byteFile.getParentFile();



			Process process = DebugPlugin.exec(commandLine, debuggerRoot);
			debuggerProcess = ExecHelper.exec(this, process);

		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			state = State.NotStarted;
			return;
		}
	}

	public synchronized void kill() {
		if (debuggerProcess != null) {
			debuggerProcess.kill();
			debuggerProcess = null;
		}

		if (process != null) {
			process.destroy();
			process = null;
		}

		remoteConnectionRequestDialog.close();

		state = State.NotStarted;
	}

	public synchronized void run() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			indicateRunningState("Running...");
			state = State.Running;
			send("run");
		}
	}

	public synchronized void reverse() {
		if (!checkStarted())
			return;

		if (!checkpoints) {
			message("You cannot go back when checkpoints are disabled");
			return;
		}

		if (state.equals(State.Idle)) {
			indicateRunningState("Running backwards...");
			state = State.RunningBackwards;
			send("reverse");
		}
	}

	public synchronized void restart() {
		if (!checkStarted())
			return;

		if (!checkpoints) {
			message("You cannot go back when checkpoints are disabled");
			return;
		}

		if (state.equals(State.Idle)) {
			emptyCallStackView();
			resetWatchVariables();
			state = State.Restarting;
			send("goto 0");
		}
	}

	public synchronized void step() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			indicateRunningState("Stepping...");
			state = State.Stepping;
			send("step");
		}
	}

	public synchronized void backstep() {
		if (!checkStarted())
			return;

		if (!checkpoints) {
			message("You cannot go back when checkpoints are disabled");
			return;
		}

		if (state.equals(State.Idle)) {
			indicateRunningState("Backstepping...");
			state = State.BackStepping;
			send("backstep");
		}
	}

	public synchronized void stepOver() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			indicateRunningState("Stepping over...");
			state = State.SteppingOver;
			send("next");
		}
	}

	public synchronized void backstepOver() {
		if (!checkStarted())
			return;

		if (!checkpoints) {
			message("You cannot go back when checkpoints are disabled");
			return;
		}

		if (state.equals(State.Idle)) {
			indicateRunningState("Backstepping over...");
			state = State.BackSteppingOver;
			send("previous");
		}
	}

	public synchronized void stepReturn() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			indicateRunningState("Step-returning...");
			state = State.StepReturn;
			send("finish");
		}
	}

	public synchronized void backstepReturn() {
		if (!checkStarted())
			return;

		if (!checkpoints) {
			message("You cannot go back when checkpoints are disabled");
			return;
		}

		if (state.equals(State.Idle)) {
			indicateRunningState("Backstep-returning...");
			state = State.BackstepReturn;
			send("start");
		}
	}
	
	public synchronized void setFrame(int frame) {
		if (!checkStarted())
			return;

		send("frame " + frame);
		state = State.SettingFrame;
	}

	public synchronized void putBreakpointAt(String moduleName, int line, int column) {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			state = State.PuttingBreakpoint;
			send("break @ " + moduleName + " " + (line + 1) + " " + (column + 1));
		}
	}

	public void removeBreakpoint(int number) {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			state = State.RemovingBreakpoint;
			send("delete " + number);

			DebugMarkers.getInstance().removeBreakpoint(number);
			refreshEditor();
		}
	}

	public synchronized void removeBreakpoints() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			state = State.RemovingBreakpoints;
			send("delete");
		}
	}

	public synchronized void quit() {
		if (!checkStarted())
			return;

		if(state == State.Quitting){
			// the user clicked twice on the "quit" button
			kill();
		}else{
			state = State.Quitting;
			send("quit");
		}
	}

	public synchronized void displayWatchVars() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle) || state.equals(State.DisplayWatchVars)) {

			// obtainWatchVariables();

			if (watchVariables.size() == 0)
				return;

			iCurrentWatchVariable = 0;
			state = State.DisplayingWatchVars;
			send("print " + watchVariables.get(iCurrentWatchVariable));
		}
	}

	/** The result of evaluating a variable by sending "print" or "display" to the debugger */
	private String displayExpression = "";

	/**
	 * Ask the debugger to analyze the expression and return its value. This function is
	 * synchronous, so it is blocking until the debugger answers back, or 2000ms ellapsed.
	 */
	public synchronized String display(String expression) {

		if (!checkStarted())
			return "";

		if(!Misc.isValidOcamlIdentifier(expression))
			return "";

		if (state.equals(State.Idle)) {
			state = State.Displaying;
			displayExpression = "";
			send("print " + expression);

			try {
				/*
				 * The debugger thread will put the value in 'displayExpression' and call "notify()"
				 * as soon as it receives the value. Timeout after 2000ms (it means the debugger is
				 * stuck or busy)
				 */
				wait(2000);
			} catch (InterruptedException e) {
				OcamlPlugin.logError("unexpected thread interruption", e);
			}

			return displayExpression;
		}

		return "<debugger is busy>";
	}

	public synchronized boolean isStarted() {
		return !(state.equals(State.NotStarted));
	}

	public synchronized boolean isReady() {
		return state.equals(State.Idle);
	}

	public synchronized IProject getProject() {
		return project;
	}

	public synchronized void processEnded(int exitValue) {
		// System.err.println("ended with exit value " + exitValue);
		state = State.NotStarted;
		DebugMarkers.getInstance().clearCurrentPosition();
		DebugMarkers.getInstance().removeBreakpoints();
		emptyBreakpointsView();
		emptyCallStackView();
		resetWatchVariables();
		refreshEditor();
		showPerspective(OcamlPerspective.ID);
	}

	static final Pattern patternBindFailed = Pattern.compile("^Unix error : 'bind' failed :");
	static final Pattern patternLostConnection = Pattern.compile("Lost connection with process \\d+");

	public synchronized void processNewError(String error) {

		if(!error.equals("done.\n"))
			errorMessage(error);

		if (bDebuggingInfoMessage) {
			if (error.endsWith("has no debugging info.\n")) {
				message("This executable has no debugging information, so it cannot be debugged. "
						+ "To add debugging information, compile with the '-g' switch, " +
								"or use a .d.byte target with ocamlbuild.");
				bDebuggingInfoMessage = false;
				state = State.Quitting;
				send("quit");
				return;
			}

			if (error.endsWith("is not a bytecode file.\n")) {
				message("This executable is not a bytecode file so it cannot be debugged.");
				bDebuggingInfoMessage = false;
				state = State.Quitting;
				send("quit");
				return;
			}
		}

		Matcher matcherBindFailed = patternBindFailed.matcher(error);
		if (matcherBindFailed.find()) {
			message("Unable to start a remote debugging session on port " +
					remoteDebugPort + ", since that port is already in use. " +
					"Please choose a different port in the launch configuration " +
					"dialog and try again.");
			bDebuggingInfoMessage = false;
			state = State.Quitting;
			send("quit");
			return;
		}

		/* If the debugger lost the connection with its process, then we stop the debugger */
		Matcher matcherLostConnection = patternLostConnection.matcher(error);
		if (matcherLostConnection.find()) {
			message("Lost connection with the debugged process.");
			if (isStarted())
				kill();
		}
	}

	/**
	 * A message dialog that instructs the user to start the remote
	 * process they wish to debug.
	 *
	 * Invoking the {@link #open()} method will cause the dialog to
	 * be displayed. (Only one dialog can be displayed at any given
	 * time.)
	 *
	 * When the user starts the remote process, invoking the {@link
	 * #signalRemoteProcessConnected()} method will cause the dialog
	 * previously opened with {@link #open()} to disappear.
	 *
	 * However, if the user instead selects the cancel button, the
	 * thread originally spawned by {@link #open()} will invoke the
	 * {@link OcamlDebugger#kill()} method itself, thus immediately
	 * terminating the debugger session.
	 */
	private class RemoteConnectionRequestDialog {

		/** Singleton reference to the message dialog box. */
		private volatile MessageDialog dialog = null;

		/** Whether or not a remote connection has been established. */
		private volatile boolean remoteProcessConnected = false;

		/** Opens the message dialog and waits for it to be closed. */
		synchronized void open () {
			// Do nothing if there is already an open message dialog.
			if (dialog != null) {
				OcamlPlugin.logError(
					"Unexpected request to open the remote connection request dialog.");
				return;
			}
			Display.getDefault().asyncExec(new Runnable () {
				public void run() {
					dialog = new MessageDialog(
						null, "OCaml Debugger",
						null, "Waiting for connection on port " + remoteDebugPort + "...\n",
						MessageDialog.INFORMATION,
						new String[] { IDialogConstants.CANCEL_LABEL }, 0
					);
					// Assume that no remote process will connect.
					remoteProcessConnected = false;
					// Wait for the dialog to be closed.
					dialog.setBlockOnOpen(true);
					dialog.open();
					if (! remoteProcessConnected) {
						// No remote process connected: the user selected cancel.
						kill ();
					}
				}
			});
		}

		/**
		 * Safely closes the message dialog if it is open.
		 */
		synchronized void close () {
			Display.getDefault().asyncExec(new Runnable () {
				public void run() {
					if (dialog != null) {
						dialog.close();
						dialog = null;
					}
				}
			});
		}

		/**
		 * Signals that a remote process has successfully connected,
		 * and closes the message dialog automatically.
		 */
		synchronized void signalRemoteProcessConnected () {
			remoteProcessConnected = true;
			close ();
		}
	}

	/* Singleton reference to remote connection request dialog. */
	private RemoteConnectionRequestDialog remoteConnectionRequestDialog =
		new RemoteConnectionRequestDialog ();

	public synchronized void processNewInput(String input) {

		// System.out.print(input);

		debuggerOutput.append(input);

		String output = debuggerOutput.toString();

		if (state.equals(State.Quitting)) {
			if (output.endsWith("The program is running. Quit anyway ? (y or n) "))
				send("y");
			return;
		}

		else if (state.equals(State.RemovingBreakpoints)) {

			if (output.endsWith("Delete all breakpoints ? (y or n) ")) {
				state = State.RemovedBreakpoints;
				// answer "yes" to the confirmation prompt
				send("y");
				debuggerOutput.setLength(0);
			}
			return;

		} else if (state.equals(State.Starting3)) {
			Pattern patternSocket = Pattern.compile("Loading program... \\n"
					+ "Waiting for connection...\\(the socket is (.*?)\\)\\n");
			Matcher matcher = patternSocket.matcher(output);
			if (matcher.find()) {
				if (scriptFile.length() > 0) {
					state = State.Starting3a;
				}
				else {
					state = State.Starting4;
				}
				if (remoteDebugEnable) {
					remoteConnectionRequestDialog.open();
					console = null;
					errorConsole = null;
				} else {
				    IProcess runprocess = loadProgram(matcher.group(1));
				    if (runprocess != null) {

				    	IOConsole ioConsole = ((IOConsole) DebugUITools.getConsole(runprocess));

			            console = ioConsole.newOutputStream();
			            errorConsole = ioConsole.newOutputStream();

			            /* set the color and font; must be on the UI thread */
			            /* TODO: make this a preference */
			            PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			                public void run() {
		                        console.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_MAGENTA));
		                        console.setFontStyle(SWT.ITALIC);

		                        errorConsole.setColor(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_RED));
			                }
			            });
				    } else {
						state = State.NotStarted;
				    }
				}
				debuggerOutput.setLength(0);
			}
			return;
		}

		if (output.endsWith("(ocd) ")) {
			// System.out.println("prompt");
			if (state.equals(State.Stepping) || state.equals(State.BackStepping)
					|| state.equals(State.SteppingOver) || state.equals(State.BackSteppingOver)
					|| state.equals(State.Running) || state.equals(State.RunningBackwards)
					|| state.equals(State.StepReturn) || state.equals(State.BackstepReturn)) {
				if (!processMessage(output)) {
					getFrame();
				}
				debuggerOutput.setLength(0);
			} else if (state.equals(State.SettingFrame)) {
				debuggerOutput.setLength(0);
				getFrame();
			} else if (state.equals(State.Starting1)) {
				debuggerOutput.setLength(0);
				state = State.Starting1a;
				send("set loadingmode manual");
			} else if (state.equals(State.Starting1a)) {
				debuggerOutput.setLength(0);
				state = State.Starting1b;
				checkpoints = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
						PreferenceConstants.P_DEBUGGER_CHECKPOINTS);
				if (checkpoints)
					send("set checkpoints on");
				else
					send("set checkpoints off");
			} else if (state.equals(State.Starting1b)) {
				debuggerOutput.setLength(0);
				state = State.Starting1c;
				int smallstep = OcamlPlugin.getInstance().getPreferenceStore().getInt(
						PreferenceConstants.P_DEBUGGER_SMALL_STEP);
				send("set smallstep " + smallstep);
			} else if (state.equals(State.Starting1c)) {
				debuggerOutput.setLength(0);
				state = State.Starting1d;
				int bigstep = OcamlPlugin.getInstance().getPreferenceStore().getInt(
						PreferenceConstants.P_DEBUGGER_BIG_STEP);
				send("set bigstep " + bigstep);
			} else if (state.equals(State.Starting1d)) {
				debuggerOutput.setLength(0);
				state = State.Starting2;
				int processcount = OcamlPlugin.getInstance().getPreferenceStore().getInt(
						PreferenceConstants.P_DEBUGGER_PROCESS_COUNT);
				send("set processcount " + processcount);
			} else if (state.equals(State.Starting2)) {
				debuggerOutput.setLength(0);
				state = State.Starting3;
				send("goto 0");
			} else if (state.equals(State.Starting3a)) {
				debuggerOutput.setLength(0);
				send("source "+scriptFile);
				state = State.Starting4;
			} else if (state.equals(State.Starting4)) {
				if (scriptFile.length() > 0) {
					String strippedOutput = output;
					int lastEolIndex = output.lastIndexOf("\n");
					if (lastEolIndex >= 0)
						strippedOutput = output.substring(0, lastEolIndex);
					message(strippedOutput);
				}
				processMessage(output);
				debuggerOutput.setLength(0);
				if (remoteDebugEnable)
					remoteConnectionRequestDialog.signalRemoteProcessConnected();
				showPerspective(OcamlDebugPerspective.ID);
			} else if (state.equals(State.Restarting)) {
				debuggerOutput.setLength(0);
				DebugMarkers.getInstance().clearCurrentPosition();
				refreshEditor();
				state = State.Idle;
			}

			else if (state.equals(State.Frame)) {
				processFrame(output);
				debuggerOutput.setLength(0);
				// state = State.Idle;
				state = State.BackTrace;
				send("bt");
			} else if (state.equals(State.BackTrace)) {
				processCallStack(output);
				debuggerOutput.setLength(0);
				state = State.DisplayWatchVars;
				displayWatchVars();
				if (!state.equals(State.DisplayingWatchVars))
					state = State.Idle;
			} else if (state.equals(State.DisplayingWatchVars)) {
				processWatchVar(output, iCurrentWatchVariable);
				debuggerOutput.setLength(0);

				iCurrentWatchVariable++;
				if (iCurrentWatchVariable >= watchVariables.size()) {
					// send the variables to the view
					putWatchVariables();
					state = State.Idle;
				} else {
					state = State.DisplayingWatchVars;
					send("print " + watchVariables.get(iCurrentWatchVariable));
				}
			} else if (state.equals(State.PuttingBreakpoint)) {
				processBreakpoint(output);
				debuggerOutput.setLength(0);
				state = State.Idle;
			} else if (state.equals(State.RemovingBreakpoint)) {
				debuggerOutput.setLength(0);
				state = State.Idle;
			} else if (state.equals(State.RemovedBreakpoints)) {
				debuggerOutput.setLength(0);

				DebugMarkers.getInstance().removeBreakpoints();
				refreshEditor();

				emptyBreakpointsView();

				state = State.Idle;
			}
			/*
			 * This case happens only if there is no breakpoint to delete (and so the confirmation
			 * message isn't displayed)
			 */
			else if (state.equals(State.RemovingBreakpoints)) {
				debuggerOutput.setLength(0);
				state = State.Idle;
			}

			else if (state.equals(State.Displaying)) {
				displayExpression = output.substring(0, output.length() - 6);
				// Wake up the thread which asked for the expression to be displayed
				notify();
				debuggerOutput.setLength(0);
				state = State.Idle;
			}
			else if (state.equals(State.Quitting)) {
				// do nothing
			}
			else {
				OcamlPlugin.logError("debugger: incoherent state (" + state + ")");
				// System.err.println("###other###");
				message("debugger internal error (incoherent state)");
			}

		}

	}
	
	private void getFrame() {
		state = State.Frame;
		send("frame");
	}

	private void processWatchVar(String output, int currentWatchVariable) {
		// remove "(ocd) " from the end
		String result = output.substring(0, output.length() - 6);
		watchVariablesResult.set(currentWatchVariable, result);
	}

	private IProcess loadProgram(String socket) {

		ArrayList<String> commandLine = new ArrayList<String>();
		commandLine.add(runFile.getPath());
		for(String arg : args)
			commandLine.add(arg);

		try {
			ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
			processBuilder.directory(runFile.getParentFile());
			// add the CAML_DEBUG_SOCKET variable to the current environment
			processBuilder.environment().put("CAML_DEBUG_SOCKET", socket);
//			processBuilder.environment().put("OCAMLLIB", OcamlPlugin.getLibFullPath());

			process = processBuilder.start();
			return DebugPlugin.newProcess(launch, process, byteFile.getName());
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			state = State.NotStarted;
			message("couldn't start " + runFile.getName());
			return null;
		}
	}

	private void emptyBreakpointsView() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlBreakpointsView breakpointsview = (OcamlBreakpointsView) activePage
							.findView(OcamlBreakpointsView.ID);

					if (breakpointsview != null)
						breakpointsview.removeAllBreakpoints();

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}

	private void emptyCallStackView() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlCallStackView callstackview = (OcamlCallStackView) activePage
							.findView(OcamlCallStackView.ID);

					if (callstackview != null)
						callstackview.empty();

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}

	private void showPerspective(final String id) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				try {
					workbench.showPerspective(id, workbench.getActiveWorkbenchWindow());
				} catch (WorkbenchException e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}

	static final Pattern patternBeginning = Pattern.compile("Time\\s*:\\s+0\\nBeginning of program.\\n\\(ocd\\) ");

	static final Pattern patternEnd = Pattern.compile("Time\\s*:\\s+\\d+\\nProgram (exit|end).\\n\\(ocd\\) ");

	static final Pattern patternException = Pattern.compile("Time\\s*:\\s+\\d+\\nProgram end.\\nUncaught exception: (.*\\n)\\(ocd\\) ");

	private boolean processMessage(String output) {

		if (patternEnd.matcher(output).find()) {
			message("Program reached end");
            state = State.Idle;
		} else if (patternBeginning.matcher(output).find()) {
			message("Program reached beginning");
			state = State.Idle;
		} else {
		    Matcher matcher = patternException.matcher(output);
		    if (matcher.find()) {
		        message("Uncaught exception: " + matcher.group(1));
	            state = State.BackStepping;
	            send("backstep");
		    } else {
		        return false;
		    }
		}
        DebugMarkers.getInstance().clearCurrentPosition();
        refreshEditor();
        return true;
	}

	Pattern patternBreakpoint = Pattern
			.compile("Breakpoint\\s+(\\d+)\\s+at\\s+(\\d+):\\s+file\\s+(.*?),\\s+line\\s+(\\d+),\\s+characters\\s+(\\d+)-(\\d+)");

	private void processBreakpoint(final String output) {
		Matcher matcher = patternBreakpoint.matcher(output);
		if (matcher.find()) {
			final int number = Integer.parseInt(matcher.group(1));
			final int address = Integer.parseInt(matcher.group(2));
			final String fullpath = matcher.group(3);
			final int line = Integer.parseInt(matcher.group(4));
			final int charBegin = Integer.parseInt(matcher.group(5));
			final int charEnd = Integer.parseInt(matcher.group(6));

			final String filename;

			int idx = fullpath.lastIndexOf(File.separator);
			if (idx != -1)
				filename = fullpath.substring(idx + 1);
			else
				filename = fullpath;

			final String filepath = getFilePath(filename);
			final String functionName = findFunctionContainingLine(filepath, line);

			DebugMarkers.getInstance().addBreakpoint(number, line - 1, charEnd - 1, filename);

			refreshEditor();

			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {

						final IWorkbenchPage activePage = PlatformUI.getWorkbench()
								.getActiveWorkbenchWindow().getActivePage();
						OcamlBreakpointsView breakpointsview = (OcamlBreakpointsView) activePage
								.findView(OcamlBreakpointsView.ID);

						if (breakpointsview != null)
							breakpointsview.addBreakpoint(number, address, filename, functionName, line,
									charBegin, charEnd);

					} catch (Throwable e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}
				}
			});

		} else
			OcamlPlugin
					.logError("ocamldebugger: couldn't parse breakpoint information:\n" + output);
	}

	static final Pattern patternFrame = Pattern.compile("\\A#\\d+\\s+Pc\\s*:\\s+\\d+\\s+(\\w+)\\s+char\\s+(\\d+)");

	private void processFrame(String output) {
		Matcher matcher = patternFrame.matcher(output);
		if (matcher.find()) {
			String module = matcher.group(1);
			int offset = Integer.parseInt(matcher.group(2));

			String filename = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
			highlight(filename, offset);

		} else if (output.equals("(ocd) ")) {
		} else
			OcamlPlugin.logError("ocamldebugger: couldn't parse frame");
	}

    public static final Pattern patternCallstack = Pattern
            .compile("\\A#(\\d+)\\s+Pc\\s*:\\s+(\\d+)\\s+(\\w+)\\s+char\\s+(\\d+)");
    
	private void processCallStack(final String output) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlCallStackView stackview = (OcamlCallStackView) activePage
							.findView(OcamlCallStackView.ID);
					if (stackview != null) {

						String[] elements = output.split("\\n");
						/*
						 * Remove the first and last line. The first is "Backtrace:" and the last is
						 * "(ocd)".
						 */
						if (elements.length >= 2) {
							String[] backtrace = new String[elements.length - 2];
							System.arraycopy(elements, 1, backtrace, 0, elements.length - 2);
							for (int i = 0; i < backtrace.length; i++) {
								String output = backtrace[i];
								Matcher matcher = patternCallstack.matcher(output);
								if (matcher.find()) {
									String s1 = matcher.group(1);
									String module = matcher.group(3); // module name
									int offset = Integer.parseInt(matcher.group(4));
									String filename = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
									String functionName = null;
									int line = -1;
									int column = -1;
									try {
										String filepath = getFilePath(filename);
										List<Integer> position = FileUtil.findLineColumnOfOffset(filepath, offset);
										line = position.get(0);
										column = position.get(1);
										functionName = findFunctionContainingLine(filepath, line);
									} catch (Exception e) {
										// TODO: handle exception
										e.printStackTrace();
									}

									// Make sure we have a function name; a trailing period is weird.
									if (functionName == null || functionName.isEmpty())
										functionName = "_";

									// prettier stackview
									String newOutput = "#" + s1 + "  -  " + module + "." + functionName
														+ "  -  (" + line + ": " + column + ")"; 
									backtrace[i] = newOutput;
								} else {
									OcamlPlugin.logError("ocamldebugger: couldn't parse call stack");
								}
							}
							stackview.setCallStack(backtrace);
						} else
							stackview.setCallStack(new String[0]);

					}

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}
	
	// get function that contains line
	private synchronized String findFunctionContainingLine(String filepath, int line) {
		String functionName = "";
		OcamlNewInterfaceParser parser = OcamlNewInterfaceParser.getInstance();
		File file = new File(filepath);
		Def root = parser.parseFile(file, true);
		List<Def> childs = root.children;
		int i = 0;
		for (i = 0; i < childs.size(); i++) {
			Def def = childs.get(i);
			int l = Symbol.getLine(def.posStart); 
			if (l >= line ) 
				break;
		}
		if (i > 0) {
			Def child = childs.get(i-1);
			if (child.type == Def.Type.Let)
				functionName = child.name;
			else if (child.type == Def.Type.Module) { 
				List<Def> grandChilds = child.children;
				int j = 0;
				for (j = 0; j < grandChilds.size(); j++) {
					Def def = grandChilds.get(j);
					int l = Symbol.getLine(def.posStart); 
					if (l > line)
						break;
				}
				if (j > 0) {
					Def grandChild = grandChilds.get(j-1);
					functionName = child.name + "." + grandChild.name;
				}
			}
		}
		return functionName;
	}
	
	private void indicateRunningState(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlCallStackView stackview = (OcamlCallStackView) activePage
							.findView(OcamlCallStackView.ID);
					if (stackview != null) {
						stackview.setCallStack(new String[] { message });
					}

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
		DebugMarkers.getInstance().clearCurrentPosition();
		refreshEditor();
	}
	
	private synchronized IEditorInput getEditorInput(final String filename)
	{
		String ufilename = filename.substring(0, 1).toUpperCase() + filename.substring(1);

		// for each path in this project
		for (String path : projectPaths) {
			IFile file = null;

			if (path.equals(".")) {
				IResource resource = project.findMember(filename);
				if (resource != null && resource.getType() == IResource.FILE) {
					file = (IFile) resource;
				} else {
					resource = project.findMember(ufilename);
					if (resource != null && resource.getType() == IResource.FILE) {
						file = (IFile) resource;
					}
				}
			} else {
				IResource resource = project.findMember(path);
				if (resource != null && resource.getType() == IResource.FOLDER) {
					IFolder folder = (IFolder) resource;
					IResource resource2 = folder.findMember(filename);
					if (resource2 != null && resource2.getType() == IResource.FILE) {
						file = (IFile) resource2;
					} else {
						resource2 = folder.findMember(ufilename);
						if (resource2 != null && resource2.getType() == IResource.FILE) {
							file = (IFile) resource2;
						}
					}
				}
			}

			if (file != null)
				return new FileEditorInput(file);

			try {
				File file2 = new File(path, filename);
				if (file2.isFile()) {
					URI uri = new File(path, filename).toURI();
					final IFileStore fileStore = EFS.getStore(uri);
					return new FileStoreEditorInput(fileStore);
				}
			} catch (CoreException e) {
				OcamlPlugin.logError("OcamlDebugger.highlight()", e);
				return null;
			}
		}
		return null;
	}

	// get absolute path of a file from file name
	private synchronized String getFilePath(final String filename)
	{
		IEditorInput editorInput = getEditorInput(filename);
		if (editorInput instanceof IURIEditorInput) {
			IURIEditorInput uriEditorInput = (IURIEditorInput) editorInput;
			return uriEditorInput.getURI().getPath();
		}
		return null;
	}

	public void highlight(final String filename, final int offset) {
		final IEditorInput editorInput = getEditorInput(filename);
		if (editorInput != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							try {
								IEditorDescriptor editorDescriptor = IDE.getEditorDescriptor(editorInput.getName());
								IEditorPart part = IDE.openEditor(page, editorInput, editorDescriptor.getId(), false);
								if (part instanceof OcamlEditor) {
									OcamlEditor editor = (OcamlEditor) part;
									DebugMarkers.getInstance().setCurrentPosition(filename,	offset);
									editor.redraw();
									editor.highlight(offset);
								}
							} catch (PartInitException e) {
								OcamlPlugin.logError("ocaml plugin error", e);
							}
						}
					}
				}
			});
		}
		else {
			if (!missingSourceFiles.contains(filename)) {
				missingSourceFiles.add(filename);
				message("highlight: Source file " + filename + " not found. \n"
						+ "You will not be able to see the current instruction pointer\n"
						+ "but you can still use step, backstep, etc.");
			}
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		}
	}

	// opens file in editor and jumps to specific offset
	public void highlight(final String filename, final int line, final int column1, final int column2) {
		final IEditorInput editorInput = getEditorInput(filename);
		if (editorInput != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							try {
								IEditorDescriptor editorDescriptor = IDE.getEditorDescriptor(editorInput.getName());
								IEditorPart part = IDE.openEditor(page, editorInput, editorDescriptor.getId(), false);
								if (part instanceof OcamlEditor) {
									OcamlEditor editor = (OcamlEditor) part;
									editor.redraw();
									editor.highlight(line, column1, column2);
								}
							} catch (PartInitException e) {
								OcamlPlugin.logError("ocaml plugin error", e);
							}
						}
					}
				}
			});
		}
		else {
			if (!missingSourceFiles.contains(filename)) {
				missingSourceFiles.add(filename);
				message("highlightBreakpoint: Source file " + filename + " not found. \n"
						+ "You will not be able to see the current instruction pointer\n"
						+ "but you can still use step, backstep, etc.");
			}
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		}
	}

	// opens file in editor and jumps to specific breakpoint location
	public void highlight(final String filename, final int line, final int column) {
		final IEditorInput editorInput = getEditorInput(filename);
		if (editorInput != null) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if (window != null) {
						IWorkbenchPage page = window.getActivePage();
						if (page != null) {
							try {
								IEditorDescriptor editorDescriptor = IDE.getEditorDescriptor(editorInput.getName());
								IEditorPart part = IDE.openEditor(page, editorInput, editorDescriptor.getId(), false);
								if (part instanceof OcamlEditor) {
									OcamlEditor editor = (OcamlEditor) part;
									editor.redraw();
									editor.highlight(line, column);
								}
							} catch (PartInitException e) {
								OcamlPlugin.logError("ocaml plugin error", e);
							}
						}
					}
				}
			});
		}
		else {
			if (!missingSourceFiles.contains(filename)) {
				missingSourceFiles.add(filename);
				message("highlightCallpoint: Source file " + filename + " not found. \n"
						+ "You will not be able to see the current instruction pointer\n"
						+ "but you can still use step, backstep, etc.");
			}
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		}
	}

	private void refreshEditor() {
		// refresh the editor
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window != null) {
					IWorkbenchPage page = window.getActivePage();
					if (page != null) {
						IEditorPart part = page.getActiveEditor();
						if (part instanceof OcamlEditor) {
							OcamlEditor editor = (OcamlEditor) part;
							editor.redraw();
						}
					}
				}
			}
		});
	}

	public void message(final String message) {
		message(message, false);
	}

	public void errorMessage(final String message) {
		message(message, true);
	}

	private void message(final String message, final boolean bError) {
		final IOConsoleOutputStream console = bError ? this.errorConsole : this.console;

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				/* delay just a little to let the executable's output finish */
				Display.getDefault().timerExec(100, new Runnable() {
					public void run() {
						if (console != null) {
							try {
								console.write(message);
								console.write("\n");
								return;
							} catch (IOException e) {
								if(bError)
									OcamlDebugger.this.errorConsole = null;
								else
									OcamlDebugger.this.console = null;
							}
						}
						if(!bError)
							MessageDialog.openInformation(null, "Ocaml Debugger", message);
					}
				});
			}
		});
	}
	
	public void printMessage(final String message) {
		final IOConsoleOutputStream console = this.console;

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				/* delay just a little to let the executable's output finish */
				Display.getDefault().timerExec(100, new Runnable() {
					public void run() {
						if (console != null) {
							try {
								console.write(message);
								return;
							} catch (IOException e) {
								OcamlDebugger.this.console = null;
							}
						}
					}
				});
			}
		});
	}

	private boolean checkStarted() {
		if (isStarted())
			return true;
		else {
			message("Please start the debugger first.");
			return false;
		}

	}

	private synchronized void send(String command) {
		try {
			//System.out.println("[" + command + "]");
			debuggerProcess.sendLine(command);
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public synchronized void addWatchVariable(String var) {
		watchVariables.add(var);
		watchVariablesResult.add(var + " : ");
	}

	public synchronized void removeWatchVariables(int[] indices) {
		for (int index : indices) {
			if (index < watchVariables.size()) {
				watchVariables.remove(index);
				watchVariablesResult.remove(index);
			} else
				OcamlPlugin.logError("OcamlDebugger:removeWatchVariable : index out of bounds");
		}
	}

	public synchronized void removeAllWatchVariables() {
		watchVariables.clear();
		watchVariablesResult.clear();
	}

	/** Send the variables to the "Watch" view */
	private void putWatchVariables() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlWatchView watchview = (OcamlWatchView) activePage
							.findView(OcamlWatchView.ID);

					if (watchview != null)
						watchview.setVariables(watchVariablesResult.toArray(new String[0]));

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}

	/**
	 * Remove the value associated to the variables in the "Watch" view
	 */
	private void resetWatchVariables() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					OcamlWatchView watchview = (OcamlWatchView) activePage
							.findView(OcamlWatchView.ID);

					if (watchview != null)
						watchview.clearVariables();

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}
}
