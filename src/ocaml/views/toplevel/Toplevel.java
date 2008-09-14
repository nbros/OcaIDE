package ocaml.views.toplevel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.exec.CommandRunner;
import ocaml.exec.ExecHelper;
import ocaml.exec.IExecEvents;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

/**
 * Implements the interaction between the top-level view and the real top-level (the underlying process)
 */
public class Toplevel {

	private final OcamlToplevelView view;

	private final StyledText userText;

	private final StyledText resultText;

	private ExecHelper exec = null;

	/** End of line delimiter (OS dependent) */
	private static final String newline;

	/** End of line delimiter for regular expressions */
	private static final String nl = "\\r?\\n";
	static {
		newline = System.getProperty("line.separator");
	}

	/** The help message that is displayed when the user types "help" in the top-level view */
	private final String helpMessage = "\nType an ocaml expression followed by ';;' and type <ENTER> to evaluate it.\n"
			+ "Type <Ctrl+Enter> to evaluate the expression without checking that it is terminated by ';;'\n"
			+ "Use the <UP> and <DOWN> arrow keys to recall history, or <F3> and <F4> in multiline expressions\n"
			+ "Press <Ctrl+C> to abort the current evaluation (only on linux compatible systems)\n"
			+ "You can also type the following commands:\n"
			+ "kill    kill the ocaml interpreter\n"
			+ "reset   restart the interpreter\n"
			+ "clear   clear the console\n"
			+ "break   abort current evaluation (only on linux compatible systems)\n"
			+ "help    show this help message\n" + "title <new title> change the view title\n";

	/** Command history, so as to be able to recall previous commands (UP arrow key) */
	ArrayList<String> history = new ArrayList<String>();

	/**
	 * The currently edited expression (we save it if the user started typing something then decided to recall
	 * history)
	 */
	String currentLine = "";

	/**
	 * Up to which level did we go back in the history? ( in the interval [-1; history.size()], -1 meaning the
	 * line currently being edited, and not yet in history.
	 */
	private int iHistory = -1;

	/** The process pid (used to send it the interrupt (Ctrl+C) signal) */
	private int ocamlPid = -1;

	private final Color colorUserText;

	private final Color colorErrorText;

	private final Color colorMessage;

	private final Color colorWhite;

	private final Color colorRed;

	public Toplevel(final OcamlToplevelView view, final StyledText userText, final StyledText resultText) {

		Display display = Display.getDefault();
		colorUserText = new Color(display, 64, 64, 255);
		colorMessage = new Color(display, 191, 95, 0);

		colorErrorText = new Color(display, 128, 128, 0);

		colorWhite = new Color(display, 255, 255, 255);
		colorRed = new Color(display, 255, 0, 0);

		int defaultSize = 12;
		Font defaultFont = org.eclipse.jface.resource.JFaceResources.getDefaultFont();
		FontData[] fontDatas = defaultFont.getFontData();
		if (fontDatas.length > 0)
			defaultSize = fontDatas[0].getHeight();

		Font font = null;
		if (OcamlPlugin.runningOnLinuxCompatibleSystem())
			font = new Font(userText.getDisplay(), "monospace", defaultSize, SWT.NONE);
		else
			font = new Font(userText.getDisplay(), "Courier New", defaultSize, SWT.NONE);

		resultText.setFont(font);
		userText.setFont(font);

		this.view = view;
		this.userText = userText;
		this.userText.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {

				if (e.character == '\r' && ((e.stateMask & SWT.CTRL) > 0)) { // <Ctrl> + <return>
					eval(userText.getText());
					e.doit = false;
				}

				else if (e.character == '\r') { // <return>
					sendText();
					e.doit = false;
				} else if (e.keyCode == SWT.ARROW_UP) {
					historyPrev(true);
					e.doit = false;
				}

				else if (e.keyCode == SWT.ARROW_DOWN) {
					historyNext(true);
					e.doit = false;
				} else if (e.keyCode == SWT.F3) {
					historyPrev(false);
					e.doit = false;
				} else if (e.keyCode == SWT.F4) {
					historyNext(false);
					e.doit = false;
				} else if (e.character == 3) // Ctrl+C
					interrupt();
				else {
					saveCurrentLine();
					iHistory = -1;
				}
			}
		});

		this.resultText = resultText;
		this.resultText.setEditable(false);
	}

	protected void saveCurrentLine() {
		currentLine = userText.getText();

	}

	/**
	 * Go back in the history of entered commands.
	 * 
	 * @param bOnlySingleLine
	 *            whether we must go back in history only if the currently edited expression is on a single
	 *            line (this allows us to continue using arrow keys in multi-line commands)
	 */
	protected void historyPrev(final boolean bOnlySingleLine) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				synchronized (userText) {
					if (userText.getLineCount() > 1 && bOnlySingleLine)
						return;

					// if we are on the first line
					if (iHistory < history.size() - 1) {
						iHistory++;
						userText.setText(history.get(history.size() - iHistory - 1));
					}
				}
			}
		});
	}

	/**
	 * Go forward in history.
	 * 
	 * @param bOnlySingleLine
	 *            whether we must go back in history only if the currently edited expression is on a single
	 *            line (this allows us to continue using arrow keys in multi-line commands)
	 */
	protected void historyNext(final boolean bOnlySingleLine) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				synchronized (userText) {
					if (userText.getLineCount() > 1 && bOnlySingleLine)
						return;

					// if we are on the first line
					if (iHistory >= 0) {
						iHistory--;

						if (iHistory == -1)
							userText.setText(currentLine);
						else
							userText.setText(history.get(history.size() - iHistory - 1));
					}
				}
			}
		});
	}

	protected void scroll() {
		synchronized (resultText) {
			resultText.setTopIndex(resultText.getLineCount() - 1);
		}
	}

	protected synchronized void sendText() {
		synchronized (resultText) {
			String text = userText.getText();
			// System.err.println("<< '" + text + "'");
			if (text.equals("kill" + newline)) {
				kill();
				userText.setText("");
				message("\nProcess killed." + newline);
				scroll();
				return;
			} else if (text.equals("reset" + newline)) {
				reset();
				return;
			} else if (text.equals("clear" + newline)) {
				userText.setText("");
				clear();
				return;
			} else if (text.equals("break" + newline)) {
				interrupt();
				userText.setText("");
				return;
			} else if (text.equals("help" + newline)) {
				help();
				return;
			} else if (text.startsWith("title ") && !text.endsWith(";;" + newline)) {
				String title = text.substring(6).trim();
				if (title.equals(""))
					this.view.setTabTitle("Ocaml Toplevel");
				else
					this.view.setTabTitle(text.substring(6).trim());
				userText.setText("");
				return;
			}

			if (text.endsWith(";;" + newline) && userText.getCaretOffset() == text.length()) {
				eval(text);
			}
		}
	}

	public void eval(String text) {
		text = text.trim();
		history.add(text);
		iHistory = -1;

		resultText.append(text + "\n");
		scroll();

		try {
			exec.sendLine(text);
		} catch (IOException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return;
		}
		userText.setText("");

		int docLength = resultText.getText().length();
		int length = text.length();

		resultText.setStyleRange(new StyleRange(docLength - length - 1, length, colorUserText, null));
	}

	public void help() {
		message(helpMessage);
		userText.setText("");
	}

	public void kill() {
		if (exec != null && exec.isRunning())
			exec.kill();
	}
	
	/** The path of the toplevel to start. <code>null</code> means default toplevel, as defined in 
	 * user preferences */
	private String toplevelPath = null;
	
	/** Change the path of the toplevel (used to start a custom toplevel, by default the toplevel path 
	 * is the one defined in the user preferences )*/
	public void setToplevelPath(String path){
		toplevelPath = path;
	}

	public void start() {
		IExecEvents execEvents = new IExecEvents() {

			public void processNewInput(String input) {
				receiveOutput(input);
			}

			public void processNewError(String error) {
				receiveError(error);
			}

			public void processEnded(int exitValue) {
				processEndend(exitValue);
			}
		};

		try {
			String path = toplevelPath;
			if(path == null)
				path = OcamlPlugin.getOcamlFullPath();

			if (OcamlPlugin.runningOnLinuxCompatibleSystem()) {
				/*
				 * Here, we try to get the pid of our process by comparing the list of pids before and after
				 * starting it.
				 */

				// get the pid of the processes before running our process
				CommandRunner cr = new CommandRunner(new String[] { "ps", "-A", "-o", "pid,ucomm" },
						OcamlPlugin.getPluginDirectory());
				String result = cr.getStdout();
				Integer[] pidsBefore = getOcamlPids(result);

				// start the top-level process
				try {
					Map<String, String> environment = new HashMap<String, String>();
					environment.putAll(System.getenv());
					/*
					 * The O'Caml toplevel prints special formatting characters when it detects a compatible
					 * terminal. So, we set the TERM variable to a kind of terminal it doesn't know (so that
					 * it will print the standard "^^^" characters to show errors.
					 */
					environment.put("TERM", "eclipse");

					exec = ExecHelper.execMerge(execEvents, new String[] { path },
							environment, null);
					
				} catch (Throwable e) {
					if (exec == null)
						resultText.append("Error: couldn't start toplevel.\n"
								+ "Please check its path in the preferences.");
					OcamlPlugin.logError("couldn't start toplevel", e);
				}

				// (execEvents, OcamlPlugin.getOcamlFullPath());

				ocamlPid = -1;
				int retries = 0;
				while (ocamlPid == -1) {
					retries++;

					// get the pid of the processes after running our process
					cr = new CommandRunner(new String[] { "ps", "-A", "-o", "pid,ucomm" }, OcamlPlugin
							.getPluginDirectory());
					result = cr.getStdout();

					Integer[] pidsAfter = getOcamlPids(result);

					// compare the two lists to find the pid of the process we just launched
					ocamlPid = findNewOcamlPid(pidsBefore, pidsAfter);

					if (ocamlPid != -1 || retries > 30)
						break;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
			} else {
				// XXX Why is the starting code different for Linux and Windows?
				
				String[] command = { path };
				
				File file = new File(path);
				
				if (!file.exists() || !file.isFile()) {
					resultText.append("Error: couldn't start toplevel.\n" + path + " doesn't exist or is not a file."
							+ "Please check toplevel path in preferences.");
					return;
				}

				
				File dir = file.getParentFile();

				ProcessBuilder processBuilder = new ProcessBuilder(command);
				processBuilder.directory(dir);
				processBuilder.environment().put("OCAMLLIB", OcamlPlugin.getLibFullPath());
				processBuilder.environment().put("TERM", "eclipse");

				Process process = processBuilder.start();
				exec = new ExecHelper(execEvents, process);
			}
		} catch (IOException e) {
			OcamlPlugin.logError("OCaml plugin IO error while trying to start ocaml toplevel", e);
			return;
		}
	}

	/** Look for all the pids of processes whose name is "ocaml" in the result of a "ps" command. */
	private Integer[] getOcamlPids(String result) {
		String[] lines = result.split("\\n");
		ArrayList<Integer> pids = new ArrayList<Integer>();

		for (int i = 1; i < lines.length; i++) {
			if (lines[i].contains("ocaml")) {
				String[] words = lines[i].trim().split(" ");
				if (words.length > 0) {
					try {
						int pid = Integer.parseInt(words[0]);
						pids.add(pid);
					} catch (NumberFormatException e) {
					}

				}
			}
		}
		return pids.toArray(new Integer[0]);
	}

	private int findNewOcamlPid(Integer[] pidsBefore, Integer[] pidsAfter) {

		ArrayList<Integer> newPids = new ArrayList<Integer>();

		for (int i = 0; i < pidsAfter.length; i++) {
			boolean bExisted = false;
			for (int j = 0; j < pidsBefore.length; j++)
				if (pidsAfter[i].equals(pidsBefore[j])) {

					bExisted = true;
					break;
				}
			if (!bExisted)
				newPids.add(pidsAfter[i]);
		}

		// if we found exactly one pid, we return it
		if (newPids.size() == 1)
			return newPids.get(0);
		/* otherwise, we don't take risks, and return -1 (don't try to kill a -1 process...) */
		else
			return -1;
	}

	protected void processEndend(final int exitValue) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				message("Process ended with exit value " + exitValue + "\n");
			}
		});
	}

	StringBuffer errorsBuffer = new StringBuffer();

	protected synchronized void receiveError(final String error) {

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				if (resultText.isDisposed())
					return;

				synchronized (resultText) {
					resultText.append(error);
					scroll();

					int length = resultText.getText().length();

					resultText.setStyleRange(new StyleRange(length - error.length(), error.length(),
							colorErrorText, null));

				}
			}
		});
	}

	Pattern patternErrors = Pattern.compile("Characters (?:\\d+)-(?:\\d+):" + nl + "(.*)" + nl + "(\\s*\\^*)"
			+ nl);

	protected synchronized void receiveOutput(final String output) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				if (resultText.isDisposed())
					return;

				synchronized (resultText) {
					if ("".equals(output.trim()))
						return;
					// System.err.println(">> '" + output + "'");
					resultText.append(output);

					String doc = resultText.getText();

					if (doc.endsWith("# ")) {

						Matcher matcher = patternErrors.matcher(doc);
						while (matcher.find()) {
							String line = matcher.group(1);
							String markers = matcher.group(2);

							Point range = findMarkersRange(markers);

							int start = matcher.start();
							int end = matcher.end();
							int length = end - start;

							int rangeStart = start + range.x;
							int rangeEnd = start + range.y;

							if (start < 0 || start > doc.length() - 1 || end < 0 || end > doc.length() - 1
									|| end <= start || rangeStart < 0 || rangeEnd < 0
									|| rangeStart > doc.length() - 1 || rangeEnd > doc.length() - 1
									|| rangeEnd <= rangeStart) {

								OcamlPlugin
										.logError("error : index out of bounds while trying to highlight errors");
								break;
							}

							resultText.replaceTextRange(start, length, line + "\n");

							resultText.setStyleRange(new StyleRange(rangeStart, rangeEnd - rangeStart,
									colorWhite, colorRed));

							doc = resultText.getText();
							matcher = patternErrors.matcher(doc);
						}
					}

					scroll();

				}
			}
		});
	}

	/** Compute the starting and ending offsets of the "^^^" in a top-level error message */
	protected Point findMarkersRange(String markers) {
		int i;
		int begin = 0;
		for (i = 0; i < markers.length(); i++)
			if (markers.charAt(i) == '^') {
				begin = i;
				break;
			}
		return new Point(begin, markers.length());
	}

	public void interrupt() {
		if (OcamlPlugin.runningOnLinuxCompatibleSystem()) {
			if (ocamlPid != -1)
				new CommandRunner(new String[] { "kill", "-INT", "" + ocamlPid }, OcamlPlugin
						.getPluginDirectory());
			else
				OcamlPlugin.logError("Ocamlplugin : not a valid pid (-1)");
		}
	}

	public void clear() {
		resultText.setText("");
	}

	public void reset() {
		kill();

		userText.setText("");
		message("\nRestarting Ocaml\n");
		scroll();
		start();
	}

	protected void message(String message) {
		if (resultText.isDisposed())
			return;

		synchronized (resultText) {
			resultText.append(message);
			int length = resultText.getText().length();

			resultText.setStyleRange(new StyleRange(length - message.length(), message.length(),
					colorMessage, null));

			scroll();
		}
	}

}
