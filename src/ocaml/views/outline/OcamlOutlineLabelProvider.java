package ocaml.views.outline;

import ocaml.parser.Def;
import ocaml.util.ImageRepository;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/** Provide the text and icon of each element in the outline. */
public class OcamlOutlineLabelProvider extends LabelProvider /* implements IStructureItem */{

	public static Image retrieveImage(Object element) {
		if (element instanceof Def) {
			Def def = (Def) element;

			Def.Type type = def.type;

			if (type.equals(Def.Type.Let))
				return ImageRepository.getImage(ImageRepository.ICON_VALUE);
			if (type.equals(Def.Type.Val))
				if (def.bAlt)
					return ImageRepository.getImage(ImageRepository.ICON_VAL_MUTABLE);
				else
					return ImageRepository.getImage(ImageRepository.ICON_VALUE);
			if (type.equals(Def.Type.LetIn)) {
				if (def.parent != null
						&& (def.parent.type == Def.Type.Let || def.parent.type == Def.Type.LetIn))
					return ImageRepository.getImage(ImageRepository.ICON_VALUE);
				else
					return ImageRepository.getImage(ImageRepository.ICON_LETIN);
			}
			if (type.equals(Def.Type.Type))
				return ImageRepository.getImage(ImageRepository.ICON_TYPE);
			if (type.equals(Def.Type.Class))
				return ImageRepository.getImage(ImageRepository.ICON_CLASS);
			if (type.equals(Def.Type.ClassType))
				return ImageRepository.getImage(ImageRepository.ICON_CLASS_TYPE);
			if (type.equals(Def.Type.Exception))
				return ImageRepository.getImage(ImageRepository.ICON_EXCEPTION);
			if (type.equals(Def.Type.External))
				return ImageRepository.getImage(ImageRepository.ICON_EXTERNAL);
			if (type.equals(Def.Type.Module))
				return ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE);
			if (type.equals(Def.Type.ModuleType))
				return ImageRepository.getImage(ImageRepository.ICON_OCAML_MODULE_TYPE);
			if (type.equals(Def.Type.Open))
				return ImageRepository.getImage(ImageRepository.ICON_OPEN);
			if (type.equals(Def.Type.Include))
				return ImageRepository.getImage(ImageRepository.ICON_INCLUDE);
			if (type.equals(Def.Type.Object))
				return ImageRepository.getImage(ImageRepository.ICON_OBJECT);
			if (type.equals(Def.Type.Method))
				if (def.bAlt)
					return ImageRepository.getImage(ImageRepository.ICON_METHOD_PRIVATE);
				else
					return ImageRepository.getImage(ImageRepository.ICON_METHOD);
			if (type.equals(Def.Type.TypeConstructor))
				return ImageRepository.getImage(ImageRepository.ICON_C);
			if (type.equals(Def.Type.RecordTypeConstructor))
				return ImageRepository.getImage(ImageRepository.ICON_RECORD_TYPE_CONSTRUCTOR);
			if (type.equals(Def.Type.Functor))
				return ImageRepository.getImage(ImageRepository.ICON_FUNCTOR);
			if (type.equals(Def.Type.Initializer))
				return ImageRepository.getImage(ImageRepository.ICON_INITIALIZER);
			if (type.equals(Def.Type.ParserError))
				return ImageRepository.getImage(ImageRepository.ICON_MODULE_PARSER_ERROR);

			if (type.equals(Def.Type.Identifier))
				return ImageRepository.getImage(ImageRepository.ICON_CIRCLE);
			if (type.equals(Def.Type.Parameter))
				return ImageRepository.getImage(ImageRepository.ICON_CIRCLE);

		}

		return null;

	}

	@Override
	public Image getImage(Object element) {
		return retrieveImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Def) {
			Def def = (Def) element;

			if (OcamlOutlineControl.bOutlineDebugButton && OcamlOutlineControl.bDebug)
				return def.name + " (" + def.type + ")";

			if (def.ocamlType != null && !"".equals(def.ocamlType))
				return def.name + " : " + def.ocamlType;
			else
				return def.name;
		}

		return "<error>";
	}

}
