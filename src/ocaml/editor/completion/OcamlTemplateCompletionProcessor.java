package ocaml.editor.completion;

import ocaml.OcamlPlugin;
import ocaml.editors.OcamlEditor;
import ocaml.util.ImageRepository;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;

// TODO: templates editor (in preferences)

/** Process the completions for O'Caml language constructs */
public class OcamlTemplateCompletionProcessor extends TemplateCompletionProcessor {

	private int indent = 0;

	@Override
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		String doc = viewer.getDocument().get();
		int offset = viewer.getSelectedRange().x;

		IRegion lineRegion = null;
		try {
			lineRegion = viewer.getDocument().getLineInformationOfOffset(offset);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return new TemplateContextType("ocamltemplatecontext");
		}

		indent = 0;
		if (lineRegion != null) {
			int tabSize = OcamlEditor.getTabSize();

			int max = lineRegion.getOffset() + lineRegion.getLength();
			for (int i = lineRegion.getOffset(); i < max; i++) {
				if (doc.charAt(i) == '\t')
					indent += tabSize;
				else if (doc.charAt(i) == ' ')
					indent++;
				else
					break;
			}

			indent /= tabSize;
		}

		return new TemplateContextType("ocamltemplatecontext");
	}

	@Override
	protected Image getImage(Template template) {
		return ImageRepository.getImage(ImageRepository.ICON_TEMPLATES);
	}

	
	private String tab;
	
	@Override
	protected Template[] getTemplates(String contextTypeId) {
		
		this.tab = OcamlEditor.getTab();

		Template t1 =
				new Template("if", "condition", "ocamltemplatecontext", "if ${condition} then" + OcamlPlugin.newline
						+ indent(indent + 1) + "${consequence}" + OcamlPlugin.newline + indent(indent) + "else\n"
						+ indent(indent + 1) + "${alternative}" + OcamlPlugin.newline+indent(indent), true);
		Template t2 =
				new Template("for", "loop", "ocamltemplatecontext", "for ${i} = ${0} to ${10} do" + OcamlPlugin.newline
						+ indent(indent + 1) + "${instruction}" + OcamlPlugin.newline + indent(indent) + "done" + OcamlPlugin.newline +indent(indent), true);
		Template t3 =
				new Template("let+match", "let+match", "ocamltemplatecontext",
					"let rec ${function name} ${parameter} = match ${parameter} with", true);
		Template t4 =
				new Template("type1", "variant type", "ocamltemplatecontext",
					"type ${name} = ${Cons1} of ${type1} | ${Cons2} of ${type2}", true);
		Template t5 =
				new Template("type2", "record type", "ocamltemplatecontext",
					"type ${name} = {${Cons1}: ${type1}; ${Cons2}: ${type2}}", true);
		Template t6 =
				new Template("exception", "exception", "ocamltemplatecontext",
					"exception ${exc};;", true);
		Template t7 =
				new Template("try", "try with", "ocamltemplatecontext",
					"try ${expr} with ${Exception} -> ${expr2}", true);
		Template t8 =
				new Template("module", "module", "ocamltemplatecontext", "module ${Name} =" + OcamlPlugin.newline
						+ indent(indent) + "struct" + OcamlPlugin.newline + indent(indent + 1) + "${body}" + OcamlPlugin.newline
						+ indent(indent) + "end;;" + OcamlPlugin.newline + indent(indent), true);
		Template t9 =
				new Template("module type", "module signature", "ocamltemplatecontext",
					"module type ${Name} =" + OcamlPlugin.newline+indent(indent) + "sig" + OcamlPlugin.newline +indent(indent+1) + "${body}" + OcamlPlugin.newline+indent(indent) + "end;;" + OcamlPlugin.newline+indent(indent), true);
		Template t10 =
				new Template("class", "class definition", "ocamltemplatecontext",
					"class ${name} =" + OcamlPlugin.newline + indent(indent) + "object (self)" + OcamlPlugin.newline + indent(indent + 1)
							+ "${body}" + OcamlPlugin.newline + indent(indent) + "end;;" + OcamlPlugin.newline+indent(indent), true);
		Template t11 =
			new Template("match", "match with", "ocamltemplatecontext",
				"match ${name} with" + OcamlPlugin.newline + indent(indent+1) + "| ", true);

		Template t12 =
			new Template("while", "while", "ocamltemplatecontext",
				"while ${condition} do" + OcamlPlugin.newline + indent(indent+1) + "${expression}" + "" + OcamlPlugin.newline + indent(indent) + "done" + OcamlPlugin.newline, true);

		Template t13 =
			new Template("begin end", "begin end block", "ocamltemplatecontext",
				"begin" + OcamlPlugin.newline + indent(indent+1) + "${}" + OcamlPlugin.newline + indent(indent) + "end" + OcamlPlugin.newline, true);

		return new Template[] { t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13};
	}

	/** return a string containing <code>count</code> tabulations */
	String indent(int count) {
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < count; i++)
			stringBuilder.append(tab);
		return stringBuilder.toString();
	}

}
