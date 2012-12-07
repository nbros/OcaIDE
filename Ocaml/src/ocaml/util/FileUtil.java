package ocaml.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class FileUtil {

	/**
	 * finds a new offset in file by avoiding the space, tabs, newline character
	 * and comment blocks before this offset.
	 * 
	 * @author TRUNG
	 */
	public static int refineOffset(String filepath, int offset) {
		String logMsg = "@FileUtil.refineOffset\n";
		logMsg = logMsg + "filepath: " + filepath + "\n";
		logMsg = logMsg + "offset: " + String.valueOf(offset) + "\n";
		int newOffset;
		int pos = offset;
		int commentBlocks = 0;
		try {
			File f = new File(filepath);
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			while (pos > 0) {
				raf.seek(pos - 1);
				int ch = raf.read();
				// LogUtil.printInfo("@FileUtil.refineOffset: ch = " +
				// String.valueOf(ch));
				if ((ch == ' ') || (ch == '\t') || (ch == '\n') || (ch == '\r')) {
					// meets separation character
					pos--;
				} else if (ch == ')') {
					raf.seek(pos - 2);
					int ch2 = raf.read();
					if (ch2 == '*') {
						// meets end comment block symbols
						commentBlocks++;
						pos = pos - 2;
					} else if (commentBlocks > 0)
						pos--;
					else
						break;
				} else if (ch == '*') {
					raf.seek(pos - 2);
					int ch2 = raf.read();
					if (ch2 == '(') {
						// meets begin comments block symbols
						commentBlocks--;
						pos = pos - 2;
					} else if (commentBlocks > 0)
						pos--;
					else
						break;
				} else if (commentBlocks > 0)
					// current position is still in the comment blocks
					pos--;
				else
					break;
			}
			newOffset = pos;
			raf.close();
			logMsg = logMsg + "newOffset: " + String.valueOf(newOffset) + "\n";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			newOffset = -1;
			return newOffset;
		}
		return newOffset;
	}

	/**
	 * finds the line and column of current position from the offset in file
	 * 
	 * @author TRUNG
	 */
	public static List<Integer> findLineColumnOfOffset(String filepath, int offset) {
		List<Integer> position = new ArrayList<Integer>();
		String logMsg = "@FileUtil.findLineColumnOfOffset\n";
		logMsg = logMsg + "filepath: " + filepath + "\n";
		logMsg = logMsg + "offset: " + String.valueOf(offset) + "\n";
		try {
			int count = 0;
			int line = 1;
			int newlineOffset = 0;
			File f = new File(filepath);
			BufferedReader bfr = new BufferedReader(new FileReader(f));
			while (count < offset) {
				if (!bfr.ready())
					break;
				int ch = bfr.read();
				count++;
				if (ch == 10) {
					line++;
					newlineOffset = count - 1;
				}
			}
			bfr.close();
			position.add(line);
			int column = offset - newlineOffset;
			position.add(column);
			logMsg = logMsg + "line: " + String.valueOf(line) + "\n";
			logMsg = logMsg + "column: " + String.valueOf(column) + "\n";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return null;
		}
		return position;
	}
	
	/**
	 * Find all the subdirectories of a directory
	 */
	public static ArrayList<String> findSubdirectories (String path)
	{
		ArrayList<String> dirs = new ArrayList<String>();
		File root = new File(path);
		if (root.isDirectory()) {
			File[] list = root.listFiles();
			for (File f: list){
				if (f.isDirectory()){
					String fpath = f.getAbsolutePath();
					dirs.add(fpath);
					ArrayList<String> subdirs = findSubdirectories(fpath);
					dirs.addAll(subdirs);
				}
			}
		}
		return dirs;
	}

	/**
	 * Delete a file
	 * 
	 * @return true if the file was really deleted
	 */
	public static boolean deleteFile(final IFile file) {
		if (file != null && file.exists()) {
			int retries = 5;

			while (file.exists() && retries > 0) {
				try {
					retries--;
					file.refreshLocal(IFile.DEPTH_ZERO, new NullProgressMonitor());
					file.delete(true, new NullProgressMonitor());
					return true;
				} catch (CoreException e) {
					OcamlPlugin.logError("error deleting file : " + file.getName()
							+ ((retries > 0) ? " : retrying" : " : stop retrying"), e);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
				}

			}

		}
		return false;
	}

	public static void closeEditorsOpenedOn(final IFile file) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				List<IEditorReference> openedEditors = findEditorsOpenedOn(file);
				for (IEditorReference editorReference : openedEditors) {
					editorReference.getPage().closeEditor(editorReference.getEditor(false), false);
				}
			}
		});
	}

	public static List<IEditorReference> findEditorsOpenedOn(IFile file) {
		List<IEditorReference> editors = new ArrayList<IEditorReference>();
		try {
			for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				for (IWorkbenchPage page : window.getPages()) {
					for (IEditorReference editor : page.getEditorReferences()) {
						IEditorInput editorInput = editor.getEditorInput();
						if (editorInput instanceof IFileEditorInput) {
							IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
							if (file.equals(fileEditorInput.getFile())) {
								editors.add(editor);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			OcamlPlugin.logError("Error finding editors opened on: " + file.getName(), e);
		}
		return editors;
	}

	public static void closeResource(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				OcamlPlugin.logError("Error closing resource", e);
			}
		}
	}

}
