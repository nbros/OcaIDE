package ocaml.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

	// TRUNG: finds a new offset in file by avoiding the space, tabs, newline character
	//        and comment blocks before this offset.
	public static int refineOffset (String filepath, int offset) {
		String logMsg = "@FileUtil.refineOffset\n";
		logMsg = logMsg + "filepath: " + filepath + "\n";
		logMsg = logMsg + "offset: " + String.valueOf(offset) + "\n";
		int newOffset;
		int pos = offset;
		int commentBlocks = 0;
		try {
			File f = new File (filepath);
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			while (pos > 0) {
				raf.seek(pos - 1);
				int ch = raf.read();
				//LogUtil.printInfo("@FileUtil.refineOffset: ch = " + String.valueOf(ch));
				if ((ch == ' ') || (ch == '\t') || (ch == '\n') || (ch == '\r')) {
					// meets separation character
					pos--;
					continue;
				} else if (ch == ')') {
					raf.seek(pos - 2);
					int ch2 = raf.read();
					if (ch2 == '*') {
						// meets end comment block symbols
						commentBlocks++;
						pos = pos - 2;
						continue;
					} else if (commentBlocks > 0)
						pos--;
					else
						break;
				} else if (ch == '*') {
					raf.seek(pos-2);
					int ch2 = raf.read();
					if (ch2 == '(') {
						// meets begin comments block symbols
						commentBlocks--;
						pos = pos - 2;
						continue;
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

	// TRUNG: finds the line and column of current position from the offset in file
	public static List<Integer> findLineColumnOfOffset(String filepath, int offset) {
		List<Integer> position = new ArrayList<Integer>();
		String logMsg = "@FileUtil.findLineColumnOfOffset\n";
		logMsg = logMsg + "filepath: " + filepath + "\n";
		logMsg = logMsg + "offset: " + String.valueOf(offset) + "\n";
		try {
			int count = 0;
			int line = 1;
			int newlineOffset = 0;
			File f = new File (filepath);
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

}
