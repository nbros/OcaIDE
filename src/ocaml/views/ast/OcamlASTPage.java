package ocaml.views.ast;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ocaml.OcamlPlugin;
import ocaml.editor.completion.CompletionJob;
import ocaml.editors.OcamlEditor;
import ocaml.editors.OcamlEditor.ICursorPositionListener;
import ocaml.views.outline.SynchronizeOutlineJob;

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
import org.eclipse.ui.part.Page;
import org.eclipse.ui.texteditor.ITextEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
	}

	@Override
	public Control getControl() {
		return this.treeViewer.getControl();
	}

	private Object parseXML(String xml) {
		// System.out.println(xml);

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

		System.out.println(selectedRange.x);

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
}
