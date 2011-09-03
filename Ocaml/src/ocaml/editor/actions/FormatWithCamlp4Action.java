package ocaml.editor.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
import ocaml.exec.CommandRunner;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/** This action formats the contents of the OCaml editor using camlp4. */
public class FormatWithCamlp4Action implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;

	public void run(IAction action) {
		IWorkbenchPage page = window.getActivePage();
		if (page != null) {
			IEditorPart editorPart = page.getActiveEditor();
			if (editorPart != null) {
				if (editorPart instanceof OcamlEditor) {
					OcamlEditor editor = (OcamlEditor) editorPart;
					
					String text = editor.getTextViewer().getDocument().get();
					if(text.trim().equals(""))
						return;
					
					IFile ifile = editor.getFileBeingEdited();
					
					if(ifile == null)
						return;

					/* Create a temporary file so that we don't have to save the editor */
					File tempFile = null;
					try {
						tempFile = File.createTempFile(ifile.getName(), "." + ifile.getLocation().getFileExtension());
						FileWriter writer = new FileWriter(tempFile);
						writer.append(text);
						writer.flush();
						writer.close();
					} catch (IOException e) {
						OcamlPlugin.logError("couldn't create temporary file for formatting with camlp4", e);
					}
					
					
					if (tempFile != null) {
						String[] command = new String[6];
						command[0] = OcamlPlugin.getCamlp4FullPath();
						command[1] = "-parser";
						command[2] = "OCaml";
						command[3] = "-printer";
						command[4] = "OCaml";
						command[5] = tempFile.getPath();

						CommandRunner cmd = new CommandRunner(command, null);
						String result = cmd.getStdout();
						String errors = cmd.getStderr();
						
						/* If the result is an empty string, then camlp4 couldn't format correctly */
						if(result.trim().equals("")){
							MessageDialog.openInformation(null, "Ocaml Plugin", "Couldn't format because of syntax errors\n" + errors);
							return;
						}

						result = blankLines(result);

						Point sel = editor.getTextViewer().getSelectedRange();
						IDocument doc = editor.getTextViewer().getDocument();
						try {
							doc.replace(0, doc.getLength(), result);
						} catch (BadLocationException e1) {
							OcamlPlugin.logError("bad location while formatting with camlp4", e1);
							return;
						}

						// editor.getTextViewer().setSelectedRange(sel.x, sel.y);
						editor.selectAndReveal(sel.x, sel.y);
						// editor.highlightLineAtOffset(sel.x);
					}

				} else if (editorPart instanceof OcamllexEditor) {
					//OcamllexEditor editor = (OcamllexEditor) editorPart;

				} else if (editorPart instanceof OcamlyaccEditor) {
					//OcamlyaccEditor editor = (OcamlyaccEditor) editorPart;

				} else
					OcamlPlugin.logError("FormatWithCamlp4Action: not an Ocaml editor");

			} else
				OcamlPlugin.logError("FormatWithCamlp4Action: editorPart is null");
		} else
			OcamlPlugin.logError("FormatWithCamlp4Action: page is null");

	}

	Pattern patternDefinition = Pattern
			.compile("^\\s*(?:let|module|exception|type|and|method|external|class|\\(\\*)\\W");

	private final Pattern lineContinuatorsPattern = Pattern
			.compile("(\\Winitializer$)|(\\Wthen$)|(\\Welse$)|(\\Wdo$)|(\\Wbegin$)|(\\Wmodule$)|"
					+ "(\\Wstruct$)|(\\Wtry$)|(\\Wsig$)|(\\Wobject$)|(\\Wof$)|(\\Wwhen$)|(\\Wwith$)|(=$)|(->$)|(\\{$)|(\\($)|(\\[$)|(\\Win$)|(\\*\\)$)|([^;];$)");

	private String blankLines(String result) {
		StringBuilder stringBuilder = new StringBuilder();

		String[] lines = result.split("\\r?\\n");

		String previousLine = "";
		for (int lineNum = 0; lineNum < lines.length; lineNum++) {
			String line = lines[lineNum];
			String nextLine = lineNum + 1 < lines.length ? lines[lineNum + 1] : "";

			Matcher matcher = patternDefinition.matcher(line);
			if (matcher.find()) {
				/* if the previous line and next line is a definition of the same kind, then don't skip a line */
				int start = matcher.start();
				int end = matcher.end();
				boolean noNewline = false;
				if (previousLine.length() > end && nextLine.length() > end) {
					String currDef = matcher.group();
					
					
					String prevDef = previousLine.substring(start, end);
					String nextDef = nextLine.substring(start, end);
					if(prevDef.equals(currDef) && nextDef.equals(currDef))
						noNewline = true;
				}
				
				if(previousLine.trim().equals(""))
					noNewline = true;

				if (!noNewline) {
					Matcher matcher2 = lineContinuatorsPattern.matcher(previousLine);
					if (!matcher2.find())
						stringBuilder.append(OcamlPlugin.newline);
				}
			}

			stringBuilder.append(line + OcamlPlugin.newline);
			previousLine = line;
		}

		return stringBuilder.toString();
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
