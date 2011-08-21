package ocaml.editor.completion;

import ocaml.OcamlPlugin;
import ocaml.editor.templates.OcamlFileContextType;
import ocaml.editor.templates.OcamlTemplateAccess;
import ocaml.editors.OcamlEditor;
import ocaml.util.ImageRepository;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

/** Process the completions for O'Caml language constructs */
public class OcamlTemplateCompletionProcessor extends TemplateCompletionProcessor {

	private int currentIndent = 0;

	@Override
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		String doc = viewer.getDocument().get();
		int offset = viewer.getSelectedRange().x;

		IRegion lineRegion = null;
		try {
			lineRegion = viewer.getDocument().getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return new OcamlFileContextType();
		}

		currentIndent = 0;
		if (lineRegion != null) {
			int tabSize = OcamlEditor.getTabSize();

			int max = lineRegion.getOffset() + lineRegion.getLength();
			for (int i = lineRegion.getOffset(); i < max; i++) {
				if (doc.charAt(i) == '\t')
					currentIndent += tabSize;
				else if (doc.charAt(i) == ' ')
					currentIndent++;
				else
					break;
			}

			currentIndent /= tabSize;
		}

		return new OcamlFileContextType();
	}

	@Override
	protected Image getImage(Template template) {
		return ImageRepository.getImage(ImageRepository.ICON_TEMPLATES);
	}

	private String tab;

	@Override
	protected Template[] getTemplates(String contextTypeId) {

		this.tab = OcamlEditor.getTab();

		Template[] templates = OcamlTemplateAccess.getDefault().getTemplateStore().getTemplates(
				contextTypeId);
		Template[] indentedTemplates = new Template[templates.length];
		for (int i = 0; i < templates.length; i++) {
			Template template = templates[i];
			indentedTemplates[i] = new Template(template.getName(), template.getDescription(),
					template.getContextTypeId(), indentTemplatePattern(template.getPattern()),
					template.isAutoInsertable());
		}

		return indentedTemplates;
	}

	private String indentTemplatePattern(String pattern) {
		String[] lines = pattern.split("\\r?\\n");
		StringBuilder result = new StringBuilder();

		for (int i = 0; i < lines.length; i++) {
			String initialIndent = (i == 0 ? "" : indent(currentIndent));
			result.append(initialIndent + lines[i] + OcamlPlugin.newline);
		}

		return result.toString();
	}

	/** return a string containing <code>count</code> tabulations */
	String indent(int count) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < count; i++)
			stringBuilder.append(tab);
		return stringBuilder.toString();
	}

}
