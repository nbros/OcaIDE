package ocaml.editor.indexer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlEditor.ICursorPositionListener;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jdom.Attribute;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;



public class MarkOccurrences implements ICursorPositionListener, IDocumentListener {

	private final OcamlEditor editor;
	private boolean ignoreSelectionEvent;
	private IDocument document;
	private Document AST;
	private Job parseJob;
	public static boolean toggleOccurrences;
	
	public MarkOccurrences(OcamlEditor editor) {
		this.editor = editor;
		editor.addCursorPositionListener(MarkOccurrences.this);
		document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		document.addDocumentListener(this);
		
		try {
			editor.getFileBeingEdited().deleteMarkers("Ocaml.ocamlOccurrencesMarker", false, IResource.DEPTH_ZERO);
			editor.getFileBeingEdited().deleteMarkers("Ocaml.ocamlWriteOccurrencesMarker", false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			OcamlPlugin.logError(e);
		}
				
		AST = null;
		parseJob = null;
		toggleOccurrences = true;
		
		update();
	}

	
	public void update() {
		String doc = document.get();
		Indexer.getInstance().getXMLAstFromInput(doc, new CallBackAdapter() {
			@Override
			public void receiveXMLFromInput(final String xml) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						AST = parseXML(xml);
					}
				});
			}
		});
	}
	

	private Document parseXML(String xml) {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
			return doc;
		} catch (SAXParseException e) {
			OcamlPlugin.logError("parseXML - SAXParseException "+e);
			return null;
		} catch (Exception e) {
			OcamlPlugin.logError(e);
			return null;
		}
	}
	

	public void cursorPositionChanged(ITextEditor textEditor, Point selectedRange) {
		if (this.ignoreSelectionEvent)
			return;
		
		try {
			editor.getFileBeingEdited().deleteMarkers("Ocaml.ocamlOccurrencesMarker", false, IResource.DEPTH_ZERO);
			editor.getFileBeingEdited().deleteMarkers("Ocaml.ocamlWriteOccurrencesMarker", false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			OcamlPlugin.logError(e);
		}

		if (toggleOccurrences) {		

			final IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
			final int offset = translateDocumentOffsetToLoc(document, selectedRange.x);

			Match match = getElementAtOffset(offset, AST); 

			if(match != null) {
				Loc loc2 = new Loc(match.loc.startOffset,match.loc.endOffset);
				String name = match.name;

				String doc = document.get();

				Indexer.getInstance().getAstOccVarFromInput(doc, name, loc2.startOffset, loc2.endOffset ,new CallBackAdapter() {
					@Override
					public void receiveAstOccVar(final String xml) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {

								try {
									SAXBuilder builder = new SAXBuilder(); 
									org.jdom.Document doc = builder.build(new InputSource(new StringReader(xml)));
									org.jdom.Element root = doc.getRootElement();
									List<?> children = root.getChildren(); 
									Iterator<?> it = children.iterator();
	
									for(int i=0;i<children.size();i++)
									{
										org.jdom.Element child = (org.jdom.Element)it.next();
										Attribute attLoc = child.getAttribute("loc");
										Loc loc = getLoc(attLoc.getValue());
										Loc loc2 = new Loc(translateLocToDocumentOffset(document,loc.startOffset),translateLocToDocumentOffset(document,loc.endOffset));

										Attribute attWrite = child.getAttribute("write");

										try {
											IMarker marker;
											if (attWrite.getValue().equals("true"))
												marker = editor.getFileBeingEdited().createMarker("Ocaml.ocamlWriteOccurrencesMarker");
											else
												marker = editor.getFileBeingEdited().createMarker("Ocaml.ocamlOccurrencesMarker");

											marker.setAttribute(IMarker.MESSAGE, "");
											marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
											marker.setAttribute(IMarker.CHAR_START, loc2.startOffset);
											marker.setAttribute(IMarker.CHAR_END, loc2.endOffset);
										} catch (CoreException e) {
											OcamlPlugin.logError(e);
										}
									}
								} catch (JDOMException e) {
									OcamlPlugin.logError("getAstOccVarFromInput - JDOMException "+e);
								} catch (IOException e) {
									OcamlPlugin.logError(e);
								} catch (Exception e) {
									OcamlPlugin.logError(e);
								}

							}

						});
					}
				});
			}
		}
	}

	
	private static class Loc {
		int startOffset;
		int endOffset;

		public Loc(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
	}
	
	
	private static Loc getLoc(Element element) {
		String loc = element.getAttribute("loc");
		return getLoc(loc);
	}
	
	
	private static Loc getLoc(String loc) {
		String[] parts = loc.split(",");
		if (parts.length == 2) {
			try {
				int startOffset = Integer.parseInt(parts[0]);
				int endOffset = Integer.parseInt(parts[1]);
				return new Loc(startOffset, endOffset);
			} catch (NumberFormatException e) {
				OcamlPlugin.logError(e);
			}
		}
		return null;
	}

	
	private static class Match {
		Node node;
		int length;
		Loc loc;
		String name;
	}
	
	
	private Match getElementAtOffset(final int offset, Document input) {
		if(input == null)
			return null;
		
		final List<Match> matches = new ArrayList<Match>();
		
		NodeList children = input.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			fillMatches(offset, children.item(i), matches);
		}
			

		Collections.sort(matches, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.length - m2.length;
			}
		});
		
		if (matches.size() > 0)
		{
			for(int i =0; i<matches.size();i++) {
				if(matches.get(i).node.getNodeName().equals("IdLid"))
					return matches.get(i);
			}
			return matches.get(1);
		}
					
		
		return null;
	}	
	
	
	private static void fillMatches(int offset, Node node, List<Match> matches) {

		if (node instanceof Element) {
			Element element = (Element) node;
			Loc loc = getLoc(element);
			if (loc != null) {
				if (offset >= loc.startOffset && offset <= loc.endOffset) {
					Match match = new Match();
					match.node = node;
					match.length = loc.endOffset - loc.startOffset;
					match.loc = loc;					
					match.name = "";
					if(node.getNodeName().equals("IdLid")) {
						match.name = node.getFirstChild().getFirstChild().getNodeValue();
					}						
					matches.add(match);
				}
			}
		}

		NodeList children = node.getChildNodes();
		if (children != null) {
			for(int i=0; i<children.getLength(); i++) {
				fillMatches(offset, children.item(i), matches);
			}
		}
	}	 
	
	
	/**
	 * The Camlp4 locations seem to consider that each line ends with '\r\n' on
	 * Windows, even when it does not. So, increment the offset by one on each
	 * line missing a '\r' (only on Windows)
	 */
	private int translateLocToDocumentOffset(IDocument document, int startOffset) {
		int resultOffset = 0;
		int remaining = startOffset;
		if (!OcamlPlugin.runningOnLinuxCompatibleSystem()) {
			int numberOfLines = document.getNumberOfLines();
			for (int i = 0; i < numberOfLines; i++) {
				try {
					String lineDelimiter = document.getLineDelimiter(i);
					int lineLength = document.getLineLength(i)
							- (lineDelimiter != null ? lineDelimiter.length() : 0);
					if (remaining - (lineLength + 1) <= 0)
						break;

					remaining -= lineLength + 1;
					resultOffset += lineLength + 1;

					if ("\n".equals(lineDelimiter)) {
						remaining--;
					}
				} catch (BadLocationException e) {
					OcamlPlugin.logError(e);
				}
			}
			resultOffset += remaining;
			return resultOffset;
		} else {
			int numberOfLines = document.getNumberOfLines();
			for (int i = 0; i < numberOfLines; i++) {
				try {
					String lineDelimiter = document.getLineDelimiter(i);
					int lineLength = document.getLineLength(i)
							- (lineDelimiter != null ? lineDelimiter.length() : 0);
					if (remaining - (lineLength + 1) <= 0)
						break;

					remaining -= lineLength + 1;
					resultOffset += lineLength + 1;

					if (!"\n".equals(lineDelimiter)) {
						resultOffset++;
					}
				} catch (BadLocationException e) {
					OcamlPlugin.logError(e);
				}
			}
			resultOffset += remaining;
			return resultOffset;
		}
	}

	
	/**
	 * Reverse operation from
	 * {@link #translateLocToDocumentOffset(IDocument, int)}
	 */
	private int translateDocumentOffsetToLoc(IDocument document, int startOffset) {
		int resultOffset = 0;
		int remaining = startOffset;
		if (!OcamlPlugin.runningOnLinuxCompatibleSystem()) {
			int numberOfLines = document.getNumberOfLines();
			for (int i = 0; i < numberOfLines; i++) {
				try {
					String lineDelimiter = document.getLineDelimiter(i);
					int lineLength = document.getLineLength(i)
							- (lineDelimiter != null ? lineDelimiter.length() : 0);
					if (remaining - (lineLength + 1) <= 0)
						break;

					remaining -= lineLength + 1;
					resultOffset += lineLength + 1;

					if ("\n".equals(lineDelimiter)) {
						resultOffset++;
					}
				} catch (BadLocationException e) {
					OcamlPlugin.logError(e);
				}
			}
			resultOffset += remaining;
			return resultOffset;
		} else {
			try {
				int numberOfLInes = document.getLineOfOffset(startOffset);
				int offset = 0;
				for (int i=0; i<numberOfLInes; i++) {
					String lineDelimiter = document.getLineDelimiter(i);
					if(!lineDelimiter.equals("\n")) {
						offset--;
					}
				}
				return startOffset + offset;
			} catch (BadLocationException e) {
				OcamlPlugin.logError(e);
			}
		}
		return startOffset;
	}
	

	public void documentAboutToBeChanged(DocumentEvent arg0) {
		
	}

	
	public void documentChanged(DocumentEvent arg0) {
		if (parseJob != null) {
			if (parseJob.getState() == Job.RUNNING) {
				// only one job at a time
				return;
			} else {
				parseJob.cancel();
			}
		}

		parseJob = new Job("Parsing editor contents") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				update();
				return Status.OK_STATUS;
			}
		};

		parseJob.setPriority(Job.DECORATE);
		parseJob.schedule(100);
	}
	
}
