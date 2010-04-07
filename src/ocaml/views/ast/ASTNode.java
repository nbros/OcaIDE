package ocaml.views.ast;

import org.w3c.dom.Node;

/** A node in the AST tree view */
public class ASTNode {
	private final Node node;
	/**
	 * If the parent node has only one child (this one), the link doesn't appear
	 * as an element in the tree. This is the name of the link that will appear
	 * before the node's name.
	 */
	private String linkName;
	private final Object parent;

	public ASTNode(Node node, String linkName, Object parent) {
		this.node = node;
		this.linkName = linkName;
		this.parent = parent;
	}

	public ASTNode(Node node, Object parent) {
		this.node = node;
		this.parent = parent;
	}

	public String getLinkName() {
		return linkName;
	}

	public Node getNode() {
		return node;
	}

	public Object getParent() {
		return parent;
	}
}
