package ocaml.views.outline;

import java.util.Arrays;
import java.util.Comparator;

import ocaml.OcamlPlugin;
import ocaml.parser.Def;
import ocaml.preferences.PreferenceConstants;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/** This class allows the JFace tree viewer to discover our definitions tree */
public class OcamlOutlineContentProvider implements ITreeContentProvider {

	public Object[] getChildren(Object parentElement) {

		if (parentElement instanceof Def) {
			Def def = (Def) parentElement;

			boolean bSort = OcamlPlugin.getInstance().getPreferenceStore().getBoolean(
					PreferenceConstants.P_OUTLINE_SORT);

			Def[] children = def.children.toArray(new Def[def.children.size()]);

			if (bSort) {
				Arrays.sort(children, new Comparator<Def>() {
					public int compare(Def d1, Def d2) {
						return d1.name == null ? -1 : d1.name.compareToIgnoreCase(d2.name);
					}
				});
			}
			return children;
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
