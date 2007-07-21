package ocaml.views.outline;

import ocaml.parser.Def;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** This class allows the JFace tree viewer to discover our definitions tree */
public class OcamlOutlineContentProvider implements ITreeContentProvider{

	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof Def) {
			Def def = (Def) parentElement;
			return def.children.toArray(new Def[def.children.size()]);
		}
		
		return new Def[0];
	}

	public Object getParent(Object element) {
		if (element instanceof Def) {
			Def def = (Def) element;
			return def.parent;
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof Def) {
			Def def = (Def) element;
			return def.children.size() > 0;
		}

		return false;
	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
