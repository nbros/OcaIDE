package ocaml.editor.completion;

import ocaml.OcamlPlugin;
import ocaml.parser.Def;
import ocaml.views.outline.OcamlOutlineLabelProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * A completion proposal, with its type, its text, its image, its future location in the source code, and its
 * associated context informations
 */
public class OcamlCompletionProposal implements ICompletionProposal, ICompletionProposalExtension5 {

	private final int replacementOffset;

	private final Def definition;

	private int typedLength;

	/**
	 * @param definition
	 *            the module, function, value...
	 * @param replacementOffset
	 *            where to replace
	 * @param typedWordLength
	 *            the typed word length before the completion was triggered
	 */
	public OcamlCompletionProposal(Def definition, int replacementOffset, int typedWordLength) {
		if (replacementOffset < 0)
			replacementOffset = 0;

		this.replacementOffset = replacementOffset;
		this.definition = definition;
		this.typedLength = typedWordLength;

	}
	
	public void apply(IDocument document) {
		String name = this.definition.name;

		try {
			document.replace(this.replacementOffset - typedLength, typedLength, name);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public Point getSelection(IDocument document) {
		return new Point(this.replacementOffset + this.definition.name.length() - typedLength, 0);
	}

	public IContextInformation getContextInformation() {
		Def.Type type = definition.type;
		/*
		 * We display context information only for functions (to help the user with the types of the expected
		 * arguments), an exception with arguments, or a constructor with arguments.
		 */
		final String body = definition.getBody();
		boolean bArrow = body.contains("->") || body.contains("\u2192");
		boolean bFun = type.equals(Def.Type.Let) && bArrow;
		boolean bExtFun = type.equals(Def.Type.External) && bArrow;
		boolean bExceptionArgs = type.equals(Def.Type.Exception)
				&& body.contains(" of ");
		boolean bConstructorArgs = type.equals(Def.Type.TypeConstructor)
				&& body.contains(" of ");
		if (!(bFun || bExtFun || bExceptionArgs || bConstructorArgs))
			return null;

		if (body.trim().equals(""))
			return null;

		class ContextInfo implements IContextInformation, IContextInformationExtension {
			public String getInformationDisplayString() {
				return body;
			}

			public Image getImage() {
				return null;
			}

			public String getContextDisplayString() {
				return body;
			}

			public int getContextInformationPosition() {
				return -1;
			}
		}

		return new ContextInfo();

	}

	public Image getImage() {
		
		// use the same image as in the outline
		return OcamlOutlineLabelProvider.retrieveImage(definition);
	}

	public String getDisplayString() {
		return definition.name;
	}

	/** @deprecated replaced by the same name function in ICompletionProposalExtension5 */
	public String getAdditionalProposalInfo() {
		return getAdditionalProposalInfo(null);
	}

	public String getAdditionalProposalInfo(IProgressMonitor monitor) {

		/*
		 * encodes as a string the informations that will be read back by OcamlInformationPresenter to format
		 * them
		 */
		return definition.parentName + " $@| " + definition.getBody() + " $@| "
				+ definition.sectionComment + " $@| " + definition.comment + " $@| "
				+ definition.getFileName();
	}

}