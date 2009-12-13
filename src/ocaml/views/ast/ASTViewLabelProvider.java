package ocaml.views.ast;

import ocaml.OcamlPlugin;

import org.eclipse.jface.viewers.LabelProvider;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class ASTViewLabelProvider extends LabelProvider {
	@Override
	public String getText(Object element) {
		if (element instanceof ASTNode) {
			ASTNode node = (ASTNode) element;
			final String prefix = node.getLinkName() != null ? node.getLinkName() + " = " : "";
			Node domNode = node.getNode();
			if (domNode instanceof Element) {
				Element domElement = (Element) domNode;
				return prefix + domElement.getNodeName();
			}
			if (domNode instanceof Text) {
				Text domText = (Text) domNode;
				return prefix + domText.getWholeText();
			} else {
				OcamlPlugin.logWarning("DOM element type not handled: "
						+ element.getClass().getSimpleName());
			}
			// else if (domNode instanceof Element) {
			// Element domElement = (Element) domNode;
			// return prefix + domElement.getNodeName();
			// }
		}

		return super.getText(element);
	}
}
