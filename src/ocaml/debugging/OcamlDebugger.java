package ocaml.debugging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.debugging.views.OcamlBreakpointsView;
import ocaml.debugging.views.OcamlCallStackView;
import ocaml.debugging.views.OcamlWatchView;
import ocaml.editors.OcamlEditor;
import ocaml.exec.ExecHelper;
import ocaml.exec.IExecEvents;
import ocaml.perspectives.OcamlDebugPerspective;
import ocaml.perspectives.OcamlPerspective;
import ocaml.preferences.PreferenceConstants;
import ocaml.util.OcamlPaths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Main class of the debugger. Manages the debugger state in a finite state automaton. Communicates with the
 * O'Caml debugger through its standard input and output.
 */
public class OcamlDebugger implements IExecEvents {

	/** The possible states of the debugger */
	public enum State {
		NotStarted, Starting1, Starting1a, Starting1b, Starting1c, Starting1d, Starting2, Starting3, Starting4, Idle, Running, RunningBackwards, Stepping, BackStepping, SteppingOver, BackSteppingOver, Frame, Quitting, PuttingBreakpoint, StepReturn, BackstepReturn, Displaying, BackTrace, RemovingBreakpoint, RemovingBreakpoints, RemovedBreakpoints, Restarting, DisplayingWatchVars, DisplayWatchVars
	};

	/** The current state of the debugger: idle, not started, stepping... */
	private State state;

	/** The project containing the debugged executable */
	private IProject project;

	/** The debugger process, to which we send messages through its command line interface */
	private ExecHelper debuggerProcess;

	/** The debugged process */
	private Process process;

	/** The singleton instance of the ocaml debugger */
	private static OcamlDebugger instance;

	/** The debugger output since the last action */
	private StringBuilder debuggerOutput;

	/**
	 * A list of missing source files, so as to display an error message only once per missing file
	 */
	private ArrayList<String> missingSourceFiles = new ArrayList<String>();

	/** To display the error message only once */
	private boolean bDebuggingInfoMessage = true;

	private ILaunch launch;

	private File exeFile;

	private String[] args;

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
	 * @param exeFile
	 *            the executable to start under ocamldebug
	 * @param project
	 *            the project in which the executable is started
	 * 
	 * @param launch
	 *            the launch object, to which we add the started process
	 */
	public synchronized void start(File exeFile, IProject project, ILaunch launch, String[] args) {

		this.exeFile = exeFile;
		this.args = args;
		this.project = project;
		this.launch = launch;

		bDebuggingInfoMessage = true;

		if (!state.equals(State.NotStarted)) {
			message("The debugger is already started.");
			return;
		}

		debuggerOutput = new StringBuilder();

		if (!exeFile.exists() || !exeFile.isFile()) {
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

			String ocamldebug = OcamlPlugin.getOcamldebugFullPath();

			ArrayList<String> commandLine = new ArrayList<String>();
			commandLine.add(ocamldebug);
			commandLine.add(exeFile.getPath());

			OcamlPaths ocamlPaths = new OcamlPaths(project);
			String[] paths = ocamlPaths.getPaths();
			for (String path : paths) {
				path = path.trim();
				if (!".".equals(path)) {
					commandLine.add("-I");
					commandLine.add(path);
				}
			}
			
			// TODO add "_build" directory to paths (only in ocamlbuild projects)

			String[] strCommandLine = commandLine.toArray(new String[commandLine.size()]);
			Process process = DebugPlugin.exec(strCommandLine, exeFile.getParentFile());
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

		state = State.NotStarted;
	}

	public synchronized void run() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
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
			state = State.BackStepping;
			send("backstep");
		}
	}

	public synchronized void stepOver() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
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
			state = State.BackSteppingOver;
			send("previous");
		}
	}

	public synchronized void stepReturn() {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
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
			state = State.BackstepReturn;
			send("start");
		}
	}

	public synchronized void putBreakpointAt(String moduleName, int offset) {
		if (!checkStarted())
			return;

		if (state.equals(State.Idle)) {
			state = State.PuttingBreakpoint;
			send("break @ " + moduleName + " # " + offset);
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

		if (state == State.Quitting) {
			// the user clicked twice on the "quit" button
			kill();
		} else {
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
	 * Ask the debugger to analyze the expression and return its value. This function is synchronous, so it is
	 * blocking until the debugger answers back, or 2000ms ellapsed.
	 */
	public synchronized String display(String expression) {
		if (!checkStarted())
			return "";

		if (state.equals(State.Idle)) {
			state = State.Displaying;
			displayExpression = "";
			send("print " + expression);

			try {
				/*
				 * The debugger thread will put the value in 'displayExpression' and call "notify()" as soon
				 * as it receives the value. Timeout after 2000ms (it means the debugger is stuck or busy)
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

	Pattern patternLostConnection = Pattern.compile("Lost connection with process \\d+");

	public synchronized void processNewError(String error) {
		// System.err.print(error);

		if (bDebuggingInfoMessage) {
			if (error.endsWith("has no debugging info.\n")) {
				message("This executable has no debugging information, so it cannot be debugged. "
						+ "To add debugging information, compile with the '-g' switch, "
						+ "or use a .d.byte target with ocamlbuild.");
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

		/* If the debugger lost the connection with its process, then we stop the debugger */
		Matcher matcher = patternLostConnection.matcher(error);
		if (matcher.find()) {
			message("Lost connection with the debugged process.");
			if (isStarted())
				kill();
		}
	}

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
				state = State.Starting4;
				if (!loadProgram(matcher.group(1)))
					state = State.NotStarted;
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
				processMessage(output);
				debuggerOutput.setLength(0);
				state = State.Frame;
				send("frame");
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
			} else if (state.equals(State.Starting4)) {
				debuggerOutput.setLength(0);
				showPerspective(OcamlDebugPerspective.ID);
				state = State.Idle;
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
			 * This case happens only if there is no breakpoint to delete (and so the confirmation message
			 * isn't displayed)
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
			} else if (state.equals(State.Quitting)) {
				// do nothing
			} else {
				OcamlPlugin.logError("debugger: incoherent state (" + state + ")");
				// System.err.println("###other###");
				message("debugger internal error (incoherent state)");
			}

		}

	}

	private void processWatchVar(String output, int currentWatchVariable) {
		// remove "(ocd) " from the end
		String result = output.substring(0, output.length() - 6);
		watchVariablesResult.set(currentWatchVariable, result);
	}

	private boolean loadProgram(String socket) {
		String[] envp = new String[] { "CAML_DEBUG_SOCKET=" + socket };

		String[] commandLine = new String[args.length + 1];
		commandLine[0] = exeFile.getPath();
		System.arraycopy(args, 0, commandLine, 1, args.length);

		File workingDir = exeFile.getParentFile(); // project.getLocation().toFile();
		try {
			process = DebugPlugin.exec(commandLine, workingDir, envp);
			IProcess iProcess = DebugPlugin.newProcess(launch, process, exeFile.getName());
			launch.addProcess(iProcess);
		} catch (Exception e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			state = State.NotStarted;
			message("couldn't start " + exeFile.getName());
			return false;
		}

		return true;
	}

	private void emptyBreakpointsView() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
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

					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
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

	Pattern patternBeginning = Pattern.compile("Time : 0\\nBeginning of program.\\n\\(ocd\\) ");

	Pattern patternEnd = Pattern.compile("Time : \\d+\\nProgram exit.\\n\\(ocd\\) ");

	private void processMessage(String output) {

		if (patternEnd.matcher(output).find()) {
			message("Program reached end");
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		} else if (patternBeginning.matcher(output).find()) {
			message("Program reached beginning");
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		}
	}

	Pattern patternBreakpoint = Pattern
			.compile("Breakpoint (\\d+) at (\\d+) : file (.*?), line (\\d+), characters (\\d+)-(\\d+)");

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
							breakpointsview
									.addBreakpoint(number, address, filename, line, charBegin, charEnd);

					} catch (Throwable e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}
				}
			});

		} else
			OcamlPlugin.logError("ocamldebugger: couldn't parse breakpoint information:\n" + output);
	}

	Pattern patternFrame = Pattern.compile("\\A#\\d+  Pc : \\d+  (\\w+) char (\\d+)");

	private void processFrame(String output) {
		Matcher matcher = patternFrame.matcher(output);
		if (matcher.find()) {
			String module = matcher.group(1);
			int offset = Integer.parseInt(matcher.group(2));

			String file = Character.toLowerCase(module.charAt(0)) + module.substring(1) + ".ml";
			highlight(file, offset);

			// System.out.println("module: " + module + " offset: " + offset);

		} else if (output.equals("(ocd) ")) {
		} else
			OcamlPlugin.logError("ocamldebugger: couldn't parse frame");
	}

	private void processCallStack(final String output) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {

					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					OcamlCallStackView stackview = (OcamlCallStackView) activePage
							.findView(OcamlCallStackView.ID);
					if (stackview != null) {

						String[] elements = output.split("\\n");
						/*
						 * Remove the first and last line. The first is "Backtrace:" and the last is "(ocd)".
						 */
						if (elements.length >= 2) {
							String[] backtrace = new String[elements.length - 2];
							System.arraycopy(elements, 1, backtrace, 0, elements.length - 2);

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

	private void highlight(final String filename, final int offset) {
		OcamlPaths opaths = new OcamlPaths(project);
		String[] projectPaths = opaths.getPaths();
		String[] paths = new String[projectPaths.length + 1];

		// Add the "external sources" folder in the lookup paths
		System.arraycopy(projectPaths, 0, paths, 0, projectPaths.length);
		paths[projectPaths.length] = OcamlPaths.EXTERNAL_SOURCES;

		IFile file = null;

		// for each path in this project
		for (String path : paths) {
			if (path.equals(".")) {
				IResource resource = project.findMember(filename);
				if (resource != null && resource.getType() == IResource.FILE) {
					file = (IFile) resource;
				}
			} else {
				IResource resource = project.findMember(path);
				if (resource != null && resource.getType() == IResource.FOLDER) {
					IFolder folder = (IFolder) resource;
					IResource resource2 = folder.findMember(filename);
					if (resource2 != null && resource2.getType() == IResource.FILE) {
						file = (IFile) resource2;
					}
				}
			}

			if (file != null && file.exists()) {
				final IFile fFile = file;

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {

						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window != null) {
							IWorkbenchPage page = window.getActivePage();
							if (page != null) {
								try {
									IEditorPart part = page.openEditor(new FileEditorInput(fFile),
											OcamlEditor.ML_EDITOR_ID, true);

									if (part instanceof OcamlEditor) {
										OcamlEditor editor = (OcamlEditor) part;

										DebugMarkers.getInstance().setCurrentPosition(filename, offset);
										editor.redraw();
										editor.highlightLineAtOffset(offset);
									}
								} catch (PartInitException e) {
									OcamlPlugin.logError("ocaml plugin error", e);
								}

							}
						}
					}
				});

				break;
			}
		}

		if (file == null) {
			if (!missingSourceFiles.contains(filename)) {
				missingSourceFiles.add(filename);
				message("Source file " + filename + " not found. \n"
						+ "You will not be able to see the current instruction pointer\n"
						+ "but you can still use step, backstep, etc.");
			}
			DebugMarkers.getInstance().clearCurrentPosition();
			refreshEditor();
		}

		/*
		 * try { IEditorDescriptor descriptor = IDE.getEditorDescriptor(file); // descriptor.
		 * System.err.println(descriptor); } catch (PartInitException e) { OcamlPlugin.logError("ocaml plugin
		 * error", e); }
		 */

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

	private void message(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openInformation(null, "Ocaml Debugger", message);
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
			// System.out.println("[" + command + "]");
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

					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					OcamlWatchView watchview = (OcamlWatchView) activePage.findView(OcamlWatchView.ID);

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

					final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
							.getActivePage();
					OcamlWatchView watchview = (OcamlWatchView) activePage.findView(OcamlWatchView.ID);

					if (watchview != null)
						watchview.clearVariables();

				} catch (Throwable e) {
					OcamlPlugin.logError("ocaml plugin error", e);
				}
			}
		});
	}
}
