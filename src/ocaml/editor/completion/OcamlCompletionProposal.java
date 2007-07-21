package ocaml.editor.completion;

import ocaml.OcamlPlugin;
import ocaml.parsers.OcamlDefinition;
import ocaml.util.Misc;

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

	private final OcamlDefinition definition;

	private int typedLength;

	private static Image libraryModuleIcon = null;

	private static Image valueIcon = null;

	private static Image constructorIcon = null;

	private static Image typeIcon = null;

	private static Image exceptionIcon = null;

	private static Image externalIcon = null;

	private static Image classIcon = null;

	/**
	 * @param definition
	 *            the module, function, value...
	 * @param replacementOffset
	 *            where to replace
	 * @param typedWordLength
	 *            the typed word length before the completion was triggered
	 */
	public OcamlCompletionProposal(OcamlDefinition definition, int replacementOffset, int typedWordLength) {
		if (replacementOffset < 0)
			replacementOffset = 0;

		this.replacementOffset = replacementOffset;
		this.definition = definition;
		this.typedLength = typedWordLength;

		if (libraryModuleIcon == null)
			libraryModuleIcon = Misc.createIcon("var.gif");
		if (valueIcon == null)
			valueIcon = Misc.createIcon("value.gif");
		if (constructorIcon == null)
			constructorIcon = Misc.createIcon("constructor.gif");
		if (typeIcon == null)
			typeIcon = Misc.createIcon("type.gif");
		if (exceptionIcon == null)
			exceptionIcon = Misc.createIcon("exception.gif");
		if (externalIcon == null)
			externalIcon = Misc.createIcon("external.gif");
		if (classIcon == null)
			classIcon = Misc.createIcon("class.gif");
	}

	public void apply(IDocument document) {
		String name = this.definition.getName();

		try {
			document.replace(this.replacementOffset - typedLength, typedLength, name);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
		}
	}

	public Point getSelection(IDocument document) {
		return new Point(this.replacementOffset + this.definition.getName().length() - typedLength, 0);
	}

	public IContextInformation getContextInformation() {
		OcamlDefinition.Type type = definition.getType();
		/*
		 * We display context information only for functions (to help the user with the types of the expected
		 * arguments), an exception with arguments, or a constructor with arguments.
		 */
		boolean bArrow = definition.getBody().contains("->") || definition.getBody().contains("\u2192");
		boolean bFun = type.equals(OcamlDefinition.Type.DefVal) && bArrow;
		boolean bExtFun = type.equals(OcamlDefinition.Type.DefExternal) && bArrow;
		boolean bExceptionArgs = type.equals(OcamlDefinition.Type.DefException)
				&& definition.getBody().contains(" of ");
		boolean bConstructorArgs = type.equals(OcamlDefinition.Type.DefConstructor)
				&& definition.getBody().contains(" of ");
		if (!(bFun || bExtFun || bExceptionArgs || bConstructorArgs))
			return null;

		final String body = definition.getBody();
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
		if (definition.getType() == OcamlDefinition.Type.DefModule)
			return libraryModuleIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefConstructor)
			return constructorIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefException)
			return exceptionIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefType)
			return typeIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefVal)
			return valueIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefExternal)
			return externalIcon;
		else if (definition.getType() == OcamlDefinition.Type.DefClass)
			return classIcon;
		else
			return null;
	}

	public String getDisplayString() {
		return definition.getName();
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
		return definition.getParentName() + " $@| " + definition.getBody() + " $@| "
				+ definition.getSectionComment() + " $@| " + definition.getComment() + " $@| "
				+ definition.getFilename();
	}

}