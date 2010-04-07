package ocaml.editors.yacc.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** This class allows the JFace tree viewer to discover our definitions tree */
public class OcamlYaccOutlineContentProvider implements ITreeContentProvider{

	public Object[] getChildren(Object parentElement) {
		
		if (parentElement instanceof YaccDef) {
			YaccDef def = (YaccDef) parentElement;
			return def.children.toArray(new YaccDef[def.children.size()]);
		}
		
		return new YaccDef[0];
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof YaccDef) {
			YaccDef def = (YaccDef) element;
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
