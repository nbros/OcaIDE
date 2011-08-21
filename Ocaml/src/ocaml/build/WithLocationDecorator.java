package ocaml.build;

import ocaml.util.ImageRepository;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

/** This decorator is used to display the full path beside the name of files in the "External Files" folder. */
public class WithLocationDecorator implements ILightweightLabelDecorator {

	public void decorate(Object element, IDecoration decoration) {
		final IFile file = (IFile) element;
		final String filepath = file.getLocation().toOSString();
		decoration.addSuffix(" - " + filepath);

		decoration.addOverlay(ImageRepository.getImageDescriptor("java_lib_obj.gif"));

	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}

}
