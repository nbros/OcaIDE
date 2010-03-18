package ocaml.views.ast;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlEditor.ICursorPositionListener;
import ocaml.views.outline.SynchronizeOutlineJob;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jdom.Attribute;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class OcamlASTPage extends Page implements ICursorPositionListener,
		ISelectionChangedListener, IDocumentListener {

	private TreeViewer treeViewer;
	private final OcamlEditor editor;
	private Job synchronizeASTViewJob = null;
	private Job parseJob = null;
	private boolean ignoreSelectionEvent;
	
	private OccurrencesJob occurrencesJob = null;

	public OcamlASTPage(OcamlEditor editor) {
		this.editor = editor;
		editor.addCursorPositionListener(OcamlASTPage.this);
		document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		document.addDocumentListener(this);		
	}

	@Override
	public void createControl(Composite parent) {
		// parent.setLayout(new FillLayout());
		this.treeViewer = new TreeViewer(parent, SWT.SINGLE);
		this.treeViewer.setContentProvider(new ASTViewContentProvider());
		this.treeViewer.setLabelProvider(new ASTViewLabelProvider());
		this.treeViewer.addSelectionChangedListener(this);
		update();
	}

	private void update() {
		// get the text of the document currently opened in the associated
		// editor
		String doc = document.get();
		
		Indexer.getInstance().getXMLAstFromInput(doc, new CallBackAdapter() {
			@Override
			public void receiveXMLFromInput(final String xml) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						treeViewer.setInput(parseXML(xml));
						treeViewer.expandToLevel(2);
					}
				});
			}
		});
		
		/* Récupération du chemin du fichier : OK  tant que pas de modif en cours sur le fichier (sinon chemin dans temp !)... 
		 * Possibilité de forcer la sauvegarde mais pas top (avec editor.doSave(monitor); ?) */
		IEditorInput editorInput = null;
		editorInput = editor.getEditorInput();
		System.out.println(editorInput);
		((IFileEditorInput) editorInput).getFile();
		String path = ((IFileEditorInput) editorInput).getFile().getLocation().toOSString();
		System.out.println(path);
		/* ou */
		//String path2 = editor.getPathOfFileBeingEdited().toString();
		/**/
						
		Indexer.getInstance().getAstOccVar("ab", 47, 49, /*path*//*path2*/"C:\\Users\\Pauline\\Documents\\Cours\\PSTL\\workspace_ocaide\\Test\\test.ml",new CallBackAdapter() {
			@Override
			public void receiveAstOccVar(final String xml) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						
						System.out.println("receiveAstOccVar");

						try {
							SAXBuilder builder = new SAXBuilder(); 
							org.jdom.Document doc = builder.build(new InputSource(new StringReader(xml)));
							org.jdom.Element root = doc.getRootElement();
							System.out.println(root.getName());
							List children = root.getChildren(); 
							Iterator it = children.iterator();
							HashMap<Integer, Integer> locMap = new HashMap<Integer, Integer>();
							
							for(int i=0;i<children.size();i++)
							{
								org.jdom.Element child = (org.jdom.Element)it.next();
								Attribute att = child.getAttribute("loc");
								Loc loc = getLoc(att.getValue());
								System.out.println(loc.startOffset+","+loc.endOffset);
								int length = loc.endOffset - loc.startOffset;
								//int offset = translateLocToDocumentOffset(document, loc.startOffset); //donne offset-2... donc on garde startOffset de loc comme offset ds doc ?
								
								locMap.put(loc.startOffset, length);
							}
							
						} catch (JDOMException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
						 
						
						
						
						//recup des loc
						// loc to offsets
						//startoffset -> translateLocTooffest...
						//lenght = Loc.end - Loc.start
						
						//tout ça dans une meth de classse occurrencesJob qui ressemble à outlinejob ???
						
						//markers
					}
				});
		
		
			}
		});
	}

	@Override
	public Control getControl() {
		return this.treeViewer.getControl();
	}

	private Object parseXML(String xml) {
		System.out.println(xml);

		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
			// doc.getDocumentElement().normalize();
			return doc;
		} catch (SAXParseException e) {
			return null;
		} catch (Exception e) {
			OcamlPlugin.logError(e);
			return null;
		}
	}
	

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof ASTNode) {
				ASTNode astNode = (ASTNode) firstElement;
				handleSelectedNodeChanged(astNode);
			}
		}
	}

	private void handleSelectedNodeChanged(ASTNode astNode) {
		if (this.ignoreSelectionEvent)
			return;

		Node node = astNode.getNode();
		if (node instanceof Element) {
			Element element = (Element) node;

			Loc loc = getLoc(element);

			// get a loc from the direct child in the case of a link
			if (loc == null) {
				String nodeName = element.getNodeName();
				if (nodeName.length() > 0 && Character.isLowerCase(nodeName.charAt(0))) {
					NodeList childNodes = element.getChildNodes();
					if (childNodes.getLength() == 1) {
						Node child = childNodes.item(0);
						if (child instanceof Element) {
							Element childElement = (Element) child;
							loc = getLoc(childElement);
						}
					}
				}
			}

			if (loc != null) {
				ISelection curSelection = editor.getSelectionProvider().getSelection();
				if (curSelection instanceof ITextSelection) {
					// ITextSelection textSelection = (ITextSelection)
					// curSelection;
					int length = loc.endOffset - loc.startOffset;
					// int curOffset = textSelection.getOffset();
					// if (curOffset < loc.startOffset || curOffset >
					// loc.endOffset
					// || (textSelection.getLength() > 0 &&
					// textSelection.getLength() != length)) {
					try {
						IDocument document = editor.getDocumentProvider().getDocument(
								editor.getEditorInput());
						int offset = translateLocToDocumentOffset(document, loc.startOffset);

						this.ignoreSelectionEvent = true;
						editor.selectAndReveal(offset, length);
					} finally {
						this.ignoreSelectionEvent = false;
					}
					// }
				}
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
	
	//Moi
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
		ASTNode node;
		Element element;
		int length;
	}

	private static long count = 0;
	private IDocument document;

	/** Synchronize the AST view with the cursor offset in the editor */
	public void cursorPositionChanged(ITextEditor textEditor, Point selectedRange) {
		if (this.ignoreSelectionEvent)
			return;

		//System.out.println(selectedRange.x);
		
		System.out.println("OffSet = "+selectedRange.x);

		if (synchronizeASTViewJob != null) {
			if (synchronizeASTViewJob.getState() == SynchronizeOutlineJob.RUNNING) {
				// only one job at a time
				return;
			} else {
				synchronizeASTViewJob.cancel();
			}
		}

		final Object input = treeViewer.getInput();
		final IDocument document = editor.getDocumentProvider()
				.getDocument(editor.getEditorInput());
		final int offset = translateDocumentOffsetToLoc(document, selectedRange.x);
		synchronizeASTViewJob = new Job("Synchronizing AST view with editor") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				selectElementAtOffset(offset, input);
				return Status.OK_STATUS;
			}
		};

		synchronizeASTViewJob.setPriority(CompletionJob.DECORATE);
		synchronizeASTViewJob.schedule(100);
		
		
		// ME
		if (this.occurrencesJob == null)
			occurencesJob = new OccurrencesJob("Occurrences Job");
	}

	/** find the element with the smallest range around the cursor */
	private void selectElementAtOffset(final int offset, Object input) {
		if (input == null)
			return;

		final List<Match> matches = new ArrayList<Match>();
		count = 0;
		ITreeContentProvider contentProvider = (ITreeContentProvider) this.treeViewer
				.getContentProvider();
		Object[] elements = contentProvider.getElements(input);
		for (Object element : elements) {
			fillMatches(offset, element, matches, contentProvider);
		}
		// System.out.println(count);
		Collections.sort(matches, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.length - m2.length;
			}
		});

		if (matches.size() > 0) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					try {
						ignoreSelectionEvent = true;
						treeViewer.setSelection(new StructuredSelection(matches.get(0).node), true);
					} finally {
						ignoreSelectionEvent = false;
					}
				}
			});
		}
	}

	private static void fillMatches(int offset, Object element, List<Match> matches,
			ITreeContentProvider contentProvider) {
		count++;
		if (element instanceof ASTNode) {
			ASTNode astNode = (ASTNode) element;
			Node node = astNode.getNode();
			if (node instanceof Element) {
				Element domElement = (Element) node;
				Loc loc = getLoc(domElement);
				if (loc != null) {
					if (offset >= loc.startOffset && offset <= loc.endOffset) {
						Match match = new Match();
						match.node = astNode;
						match.element = domElement;
						match.length = loc.endOffset - loc.startOffset;
						matches.add(match);
					}
				}
			}
		}

		Object[] children = contentProvider.getChildren(element);
		if (children != null) {
			for (Object child : children) {
				fillMatches(offset, child, matches, contentProvider);
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
						// resultOffset++;
					}
				} catch (BadLocationException e) {
					OcamlPlugin.logError(e);
				}
			}
			resultOffset += remaining;
			return resultOffset;
		} else {
			return startOffset;
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
			return startOffset;
		}
	}

	@Override
	public void setFocus() {

	}

	@Override
	public void dispose() {
		super.dispose();
		editor.removeCursorPositionListener(OcamlASTPage.this);
		document.removeDocumentListener(this);
	}

	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	public void documentChanged(DocumentEvent event) {

		if (parseJob != null) {
			if (parseJob.getState() == SynchronizeOutlineJob.RUNNING) {
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

		parseJob.setPriority(CompletionJob.DECORATE);
		parseJob.schedule(100);

	}

}               