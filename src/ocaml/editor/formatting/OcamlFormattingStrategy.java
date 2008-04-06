package ocaml.editor.formatting;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.formatter.IFormattingStrategy;
import org.eclipse.jface.text.formatter.IFormattingStrategyExtension;

/**
 * This class gets the text to format from the source code editor and passes it on to the formatter.
 */
public class OcamlFormattingStrategy implements IFormattingStrategy, IFormattingStrategyExtension {

	private IDocument document;

	private int start, length;

	/** This method is called by the editor to give us the formatting context */
	public void formatterStarts(IFormattingContext context) {
		try {

			// get the information we need for the formatting

			// the document
			this.document = (IDocument) context.getProperty(FormattingContextProperties.CONTEXT_MEDIUM);

			// the region to format
			IRegion region = (IRegion) context.getProperty(FormattingContextProperties.CONTEXT_REGION);

			// do we format the whole document or just the selection?
			boolean bFormatWholeDocument = (Boolean) context
					.getProperty(FormattingContextProperties.CONTEXT_DOCUMENT);

			if (bFormatWholeDocument)
				region = new Region(0, document.getLength());

			/*
			 * Create a formatting region that starts at the beginning of the line and ends at the end of the
			 * last line of the selection.
			 */
			this.start = document.getLineInformationOfOffset(region.getOffset()).getOffset();

			IRegion endLine = document.getLineInformationOfOffset(region.getOffset() + region.getLength());
			int end = endLine.getOffset() + endLine.getLength();

			this.length = end - this.start;
		}

		catch (Throwable e) {
			ocaml.OcamlPlugin.logError("Error in OcamlFormattingStrategy:formatterStarts", e);
		}
	}

	/** This method is called by the editor to format the document */
	public void format() {
		try {
			String text = document.get(start, length);
			OcamlFormatter formatter = new OcamlFormatter();
			String formattedText = formatter.format(text);
			
			int firstChar = 0;
			int textLength = text.length();
			int lastChar = textLength - 1;
			
			// avoid formatting leading and trailing newlines
			while(firstChar  < textLength - 1){
				char c = text.charAt(firstChar);
				if(c == '\n' || c == '\r')
					firstChar ++;
				else
					break;
			}

			while(lastChar > 0){
				char c = text.charAt(lastChar);
				if(c == '\n' || c == '\r')
					lastChar --;
				else
					break;
			}
			
			if(firstChar >= lastChar)
				return;

			int resultFirstChar = 0;
			int resultLastChar = formattedText.length() - 1;

			int formattedTextLength = formattedText.length();
			while(resultFirstChar  < formattedTextLength - 1){
				char c = formattedText.charAt(resultFirstChar);
				if(c == '\n' || c == '\r')
					resultFirstChar ++;
				else
					break;
			}

			while(resultLastChar > 0){
				char c = formattedText.charAt(resultLastChar);
				if(c == '\n' || c == '\r')
					resultLastChar --;
				else
					break;
			}
			
			if(resultFirstChar >= resultLastChar)
				return;
			
			text = text.substring(firstChar, lastChar + 1);
			formattedText = formattedText.substring(resultFirstChar, resultLastChar + 1);
			
			if(!text.equals(formattedText))
				document.replace(start + firstChar, lastChar - firstChar + 1, formattedText);
		} catch (Throwable e) {
			ocaml.OcamlPlugin.logError("Error in OcamlFormattingStrategy:format", e);
		}
	}

	/** This method is required by the interface, but we don't use it */
	public String format(String content, boolean isLineStart, String indentation, int[] positions) {
		return "";
	}

	/** This method is required by the interface, but we don't use it */
	public void formatterStarts(String initialIndentation) {
	}

	/** This method is required by the interface, but we don't use it */
	public void formatterStops() {
	}
}
