package ocaml.views.outline;

import ocaml.parser.Def;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class OcamlOutlineDecoratingLabelProvider extends DecoratingLabelProvider {

	// private final Font recFont;

	public OcamlOutlineDecoratingLabelProvider(ILabelProvider provider, ILabelDecorator decorator) {
		super(provider, decorator);
		
		/*Font defaultFont = org.eclipse.jface.resource.JFaceResources.getDefaultFont();
		FontData[] fontDatas = defaultFont.getFontData();
		if(fontDatas.length > 0)
			recFont = new Font(Display.getDefault(), fontDatas[0].getName(), fontDatas[0].getHeight() - 1, SWT.BOLD);
		else
			recFont = new Font(Display.getDefault(), "", 10, SWT.BOLD);
		*/
	}

	Color blue = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);

	// Color red = Display.getCurrent().getSystemColor(SWT.COLOR_RED);

	// Color magenta = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_MAGENTA);
	
	Color green = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN);

	@Override
	public Color getForeground(Object element) {
		if (element instanceof Def) {
			Def def = (Def) element;

			//if (def.bRec)
			//	return green;

			/*
			 
			 if (def.type == Def.Type.Parameter && def.bAnd)
				return magenta;
			if (def.type == Def.Type.Parameter)
				return red;
			*/

			
			if (def.bAnd)
				return blue;
		}

		return super.getForeground(element);

	}
	
	
	/*@Override
	public Font getFont(Object element) {
		Font font = super.getFont(element);
		
		if (element instanceof Def) {
			Def def = (Def) element;

			if (def.bRec){
				return recFont;
			}
		}

		return font;
	}*/

}
