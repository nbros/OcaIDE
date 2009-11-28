package ocaml.editor.completion;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/** Create a custom control for the completion window */
public class OcamlInformationControlCreator implements IInformationControlCreator{

	public IInformationControl createInformationControl(Shell parent) {
		DefaultInformationControl.IInformationPresenter presenter = new OcamlInformationPresenter();
		IInformationControl informationControl = new DefaultInformationControl(parent, SWT.V_SCROLL|SWT.H_SCROLL, presenter);
		//informationControl.setSizeConstraints(800, 600);
		return informationControl;
	}

}
