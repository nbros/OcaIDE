package ocaml.editors.yacc.outline;

import ocaml.util.ImageRepository;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/** Provide the text and icon of each element in the outline. */
public class OcamlYaccOutlineLabelProvider extends LabelProvider /* implements IStructureItem */{

	@Override
	public Image getImage(Object element) {
		return ImageRepository.getImage(ImageRepository.ICON_VALUE);
		// return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof YaccDef) {
			YaccDef def = (YaccDef) element;
			return def.name;
		}
		return "<error>";
	}

}
