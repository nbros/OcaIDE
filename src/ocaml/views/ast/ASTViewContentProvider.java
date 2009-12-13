package ocaml.views.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ASTViewContentProvider implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ASTNode) {
			List<Object> children = new ArrayList<Object>();
			ASTNode astNode = (ASTNode) parentElement;
			Node node = astNode.getNode();
			if (node instanceof Element) {
				Element element = (Element) node;
				NodeList childNodes = element.getChildNodes();

				for (int i = 0; i < childNodes.getLength(); i++) {
					Node childNode = childNodes.item(i);
					if (childNode instanceof Element) {
						Element childElement = (Element) childNode;
						String name = childElement.getNodeName();
						NodeList subChildNodes = childElement.getChildNodes();
						int length = subChildNodes.getLength();
						// collapse links with a single child element
						if (length == 1 && name.length() > 0
								&& Character.isLowerCase(name.charAt(0))) {
							Node subChildNode = subChildNodes.item(0);
							ASTNode subChildASTNode;
							subChildASTNode = (ASTNode) subChildNode.getUserData("ast");
							if (subChildASTNode == null) {
								subChildASTNode = new ASTNode(subChildNode, name, parentElement);
								// memorize the AST node so as not to have
								// to
								// re-create it, so that the JFace viewer's
								// selection can be restored using the
								// physical
								// identity of the Object
								subChildNode.setUserData("ast", subChildASTNode, null);
							}
							children.add(subChildASTNode);
						} else {
							ASTNode childASTNode;
							childASTNode = (ASTNode) childElement.getUserData("ast");
							if (childASTNode == null) {
								childASTNode = new ASTNode(childElement, parentElement);
								childElement.setUserData("ast", childASTNode, null);
							}
							children.add(childASTNode);
						}
					} else {
						children.add(childNode);
					}
				}
			}
			return children.toArray();
		}
		return null;
	}

	public Object getParent(Object element) {
		if (element instanceof ASTNode) {
			ASTNode astNode = (ASTNode) element;
			return astNode.getParent();
		} else if (element instanceof Node) {
			Node domNode = (Node) element;
			return domNode.getParentNode();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return true;
	}

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof Document) {
			Document doc = (Document) inputElement;
			return new Object[] { new ASTNode(doc.getDocumentElement(), null) };
		}
		return null;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
