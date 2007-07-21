package ocaml.debugging;

import ocaml.OcamlPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/** This class is responsibe for displaying the debug elements overlayed on top of the ocaml source editor */
public class DebugVisuals implements PaintListener {

	private final StyledText textWidget;

	private String filename;

	public DebugVisuals(StyledText textWidget) {
		this.textWidget = textWidget;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	// int lastPos = 0;

	public void paintControl(PaintEvent e) {

		try {

			Breakpoint[] breakpoints = DebugMarkers.getInstance().getBreakpointsInFile(filename);
			for (Breakpoint breakpoint : breakpoints) {
				int line = breakpoint.getLine();
				int offset = breakpoint.getOffset();

				offset = textWidget.getOffsetAtLine(line) + offset;

				Rectangle bounds = textWidget.getTextBounds(offset, offset);

				boolean bEndOfLine = false;
				if (offset > 0 && textWidget.getTextBounds(offset - 1, offset - 1).x == bounds.x)
					bEndOfLine = true;

				e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLUE));

				if (bEndOfLine)
					drawEndOfLineMarker(e, bounds.x, bounds.y, bounds.width, bounds.height);
				else {
					drawIBeam(e, bounds.x, bounds.y, bounds.height);
				}
			}

			int position = DebugMarkers.getInstance().getPositionInFile(filename);
			if (position != -1) {

				if (position < 0)
					return;

				Rectangle bounds = textWidget.getTextBounds(position, position);
				int x = bounds.x;
				int y = bounds.y;
				int w = bounds.width;
				int h = bounds.height;

				boolean bEndOfLine = false;
				/*
				 * Special case: the last two characters of a line return the same position (I don't know why...)
				 */
				if (position > 0 && textWidget.getTextBounds(position - 1, position - 1).x == x)
					bEndOfLine = true;

				e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));

				if (bEndOfLine) {
					drawEndOfLineMarker(e, x, y, w, h);

				} else {
					drawIBeam(e, x, y, h);
				}
			}

		} catch (Exception exc) {
			OcamlPlugin.logError("ocaml plugin error (debug caret)", exc);
			return;
		}
	}

	/** Draw an I-beam */
	private void drawIBeam(PaintEvent e, int x, int y, int h) {
		// barre du milieu
		e.gc.drawLine(x, y, x, y + h - 1);
		e.gc.drawLine(x + 1, y, x + 1, y + h - 1);
		e.gc.drawLine(x - 1, y, x - 1, y + h - 1);

		// barre du haut
		e.gc.drawLine(x - 3, y, x + 3, y);
		e.gc.drawLine(x - 3, y + 1, x + 3, y + 1);

		// barre du bas
		e.gc.drawLine(x - 3, y + h - 1, x + 3, y + h - 1);
		e.gc.drawLine(x - 3, y + h - 2, x + 3, y + h - 2);
	}

	/** Draw a double I-beam */
	/*private void drawIBeam2(PaintEvent e, int x, int y, int h) {
		// barre du milieu
		e.gc.drawLine(x, y, x, y + h - 1);
		e.gc.drawLine(x + 3, y, x + 3, y + h - 1);
		e.gc.drawLine(x + 2, y, x + 2, y + h - 1);
		e.gc.drawLine(x - 3, y, x - 3, y + h - 1);
		e.gc.drawLine(x - 2, y, x - 2, y + h - 1);

		// barre du haut
		e.gc.drawLine(x - 3, y, x + 3, y);
		e.gc.drawLine(x - 3, y + 1, x + 3, y + 1);

		// barre du bas
		e.gc.drawLine(x - 3, y + h - 1, x + 3, y + h - 1);
		e.gc.drawLine(x - 3, y + h - 2, x + 3, y + h - 2);
	}*/

	/** Draw a "]"-shaped beam */
	private void drawEndOfLineMarker(PaintEvent e, int x, int y, int w, int h) {
		// barre du milieu
		e.gc.drawLine(x + w + 3, y, x + w + 3, y + h - 1);
		e.gc.drawLine(x + w + 2, y, x + w + 2, y + h - 1);
		e.gc.drawLine(x + w + 1, y, x + w + 1, y + h - 1);
		// e.gc.drawLine(x + w - 3, y, x + w - 3, y + h - 1);

		// barre du haut
		e.gc.drawLine(x + w - 8, y, x + w, y);
		e.gc.drawLine(x + w - 8, y + 1, x + w, y + 1);

		// barre du bas
		e.gc.drawLine(x + w - 8, y + h - 1, x + w, y + h - 1);
		e.gc.drawLine(x + w - 8, y + h - 2, x + w, y + h - 2);
	}

	/** Draw a double "]"-shaped beam */
	/*private void drawEndOfLineMarker2(PaintEvent e, int x, int y, int w, int h) {
		// barre du milieu
		e.gc.drawLine(x + w + 4, y, x + w + 4, y + h - 1);
		e.gc.drawLine(x + w + 2, y, x + w + 2, y + h - 1);
		e.gc.drawLine(x + w + 0, y, x + w + 0, y + h - 1);
		// e.gc.drawLine(x + w - 3, y, x + w - 3, y + h - 1);

		// barre du haut
		e.gc.drawLine(x + w - 8, y, x + w, y);
		e.gc.drawLine(x + w - 8, y + 1, x + w, y + 1);

		// barre du bas
		e.gc.drawLine(x + w - 8, y + h - 1, x + w, y + h - 1);
		e.gc.drawLine(x + w - 8, y + h - 2, x + w, y + h - 2);
	}*/
}
