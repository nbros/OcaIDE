package ocaml.editor.completion;

import java.util.ArrayList;
import java.util.TreeSet;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
import ocaml.parser.Def;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

/**
 * This class is responsible for managing completion in the O'Caml editor.
 * <p>
 * Find the expression before the cursor in the O'Caml editor, find completion proposals by going
 * through the definitions tree, find context informations that will appear after a completion
 * proposal is selected...
 */
public class OcamlCompletionProcessor implements IContentAssistProcessor {

	//private final OcamlEditor ocamlEditor;

	private final IProject project;

	/** The partition type in which completion was triggered. */
	private final String partitionType;

	public OcamlCompletionProcessor(OcamlEditor edit, String regionType) {
		//this.ocamlEditor = edit;
		this.partitionType = regionType;
		this.project = edit.getProject();
	}

	public OcamlCompletionProcessor(OcamllexEditor edit, String regionType) {
		//this.ocamlEditor = null;
		this.partitionType = regionType;
		this.project = edit.getProject();
	}

	public OcamlCompletionProcessor(OcamlyaccEditor edit, String regionType) {
		//this.ocamlEditor = null;
		this.partitionType = regionType;
		this.project = edit.getProject();
	}

	/**
	 * To know whether the user is precising a completion or starting a new one, so as to dismiss
	 * the completion box when he types a space
	 */
	private int lastOffset = -1;

	/**
	 * Compute and return completion proposals available at the offset <code>documentOffset</code>.
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {

		// the word right before the caret in the editor
		String lastWord = this.getLastWord(viewer, documentOffset - 1);
		if (lastWord == " " && lastOffset == documentOffset - 1) {
			lastOffset = documentOffset;
			return new OcamlCompletionProposal[0];
		}

		lastOffset = documentOffset;

		// System.err.println("\"" + lastWord + "\"");

		// the word right before the caret in an ocamldoc comment
		String lastWordDoc = this.getLastWordDoc(viewer, documentOffset - 1);

		if (partitionType.equals(OcamlPartitionScanner.OCAML_DOCUMENTATION_COMMENT)) {

			// the documentation annotations that can appear in ocamldoc comments
			String[] docCompletions = { "@author ", "@deprecated ", "@param ", "@raise ",
					"@return ", "@see ", "@since ", "@version " };

			ArrayList<ICompletionProposal> docProposals = new ArrayList<ICompletionProposal>(8);
			for (String s : docCompletions)
				if (s.startsWith(lastWordDoc))
					docProposals.add(new SimpleCompletionProposal(s, documentOffset
							- lastWordDoc.length(), lastWordDoc.length(), s.length()));

			return docProposals.toArray(new ICompletionProposal[0]);
		}

		// "@": only for documentation comments
		if (lastWordDoc.startsWith("@"))
			return new OcamlCompletionProposal[0];

		String completion = completionExpression(viewer, documentOffset);

		OcamlCompletionProposal[] proposals;
		/*
		 * If the interfaces parsing Job isn't done, we return an empty list to avoid blocking the
		 * graphical interface by doing a potentially long search (should be quick normally, but not
		 * on a network file system for example).
		 */
		if (CompletionJob.isParsingFinished()) {
			Def definitionsRoot = CompletionJob.buildDefinitionsTree(this.project, true);
			proposals = findCompletionProposals(completion, definitionsRoot, documentOffset);
		} else {
			proposals = new OcamlCompletionProposal[0];
			OcamlPlugin.logInfo("Completion proposals skipped (background job not done yet)");
		}

		ICompletionProposal[] templateCompletionProposals;

		OcamlTemplateCompletionProcessor tcp = new OcamlTemplateCompletionProcessor();
		templateCompletionProposals = tcp.computeCompletionProposals(viewer, documentOffset);

		ArrayList<ICompletionProposal> allProposals = new ArrayList<ICompletionProposal>();

		// create a list with both types of proposals
		for (int j = 0; j < templateCompletionProposals.length; j++) {
			if (templateCompletionProposals[j].getDisplayString().startsWith(completion)) {
				allProposals.add(templateCompletionProposals[j]);
			}

		}

		for (int j = 0; j < proposals.length; j++)
			allProposals.add(proposals[j]);

		// get the definitions from the file being edited
		/*if (!completion.contains(".") && this.ocamlEditor != null) {
			Def def = ocamlEditor.getOutlineDefinitionsTree();
			if (def != null) {
				ICompletionProposal[] moduleCompletionProposals = findModuleCompletionProposals(
						def, documentOffset, lastWord.length(), lastWord);

				for (int j = 0; j < moduleCompletionProposals.length; j++)
					allProposals.add(moduleCompletionProposals[j]);
			} else
				OcamlPlugin
						.logError("OcamlCompletionProcessor:computeCompletionProposals : module definitions=null");
		}*/

		/*
		 * If a definition appears twice, remove the second one so that the user needn't press enter
		 * to enter it
		 */
		/*if (allProposals.size() == 2) {
			ICompletionProposal prop1 = allProposals.get(0);
			ICompletionProposal prop2 = allProposals.get(1);

			if (prop1 instanceof OcamlCompletionProposal
					&& prop2 instanceof SimpleCompletionProposal
					&& prop1.getDisplayString().equals(prop2.getDisplayString()))
				allProposals.remove(1);
		}*/

		return allProposals.toArray(new ICompletionProposal[] {});
	}

	/** Return the completions from the module */
	private ICompletionProposal[] findModuleCompletionProposals(Def root, int offset, int length,
			String completion) {
		TreeSet<SimpleCompletionProposal> proposals = new TreeSet<SimpleCompletionProposal>();

		findModuleCompletionProposalsAux(root, offset, length, completion, proposals);

		return proposals.toArray(new ICompletionProposal[0]);
	}

	/** Fill the list "<code>proposals</code>" with the completions found recursively */
	private void findModuleCompletionProposalsAux(Def def, int offset, int length,
			String completion, TreeSet<SimpleCompletionProposal> proposals) {
		String name = def.name.trim();
		Def.Type type = def.type;

		if (name.startsWith(completion) && !name.equals("") && !name.equals("()")
				&& !name.equals("_") && !type.equals(Def.Type.Open)
				&& !type.equals(Def.Type.Include) && !type.equals(Def.Type.Root)
				&& !type.equals(Def.Type.LetIn))
			proposals.add(new SimpleCompletionProposal(def, name, offset - length, length, name
					.length()));

		for (Def child : def.children)
			findModuleCompletionProposalsAux(child, offset, length, completion, proposals);
	}

	/**
	 * Find the completions matching the argument <code>completion</code> from all the definitions
	 * found in the definitionsRoot tree.
	 * 
	 * @return the completions found
	 */
	private OcamlCompletionProposal[] findCompletionProposals(String completion,
			Def definitionsRoot, int offset) {

		ArrayList<Def> definitions = definitionsRoot.children;

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		// look in the module before the dot what's after the dot
		if (completion.contains(".")) {
			int index = completion.indexOf('.');
			String prefix = completion.substring(0, index);
			String suffix = completion.substring(index + 1);

			for (Def def : definitions) {
				if (def.name.equals(prefix))
					return findCompletionProposals(suffix, def, offset);
			}
		}
		// find elements starting by <completion> in the list of elements
		else {

			for (Def def : definitions) {
				if (def.name.startsWith(completion) && isCompletionDef(def))
					proposals.add(new OcamlCompletionProposal(def, offset, completion.length()));

			}
		}

		return proposals.toArray(new OcamlCompletionProposal[0]);
	}

	private boolean isCompletionDef(Def def) {
		switch (def.type) {
		case Parameter:
		case Object:
		case LetIn:
		case Open:
		case Include:
		case In:
		case Identifier:
		case Dummy:
		case Root:
		case Sig:
		case Struct:
			return false;
		default:
			return true;
		}
	}

	/** Return the last pointed expression before the caret (at documentOffset). */
	private String completionExpression(ITextViewer viewer, int documentOffset) {
		String doc = "";
		try {
			doc = viewer.getDocument().get(0, documentOffset);
		} catch (BadLocationException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return "";
		}

		final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'.";

		int startIndex = documentOffset;
		for (int i = documentOffset - 1; i >= -1; i--) {
			if (i == -1) {
				startIndex = 0;
				break;
			}

			if (!chars.contains("" + doc.charAt(i))) {
				startIndex = i + 1;
				break;
			}
		}

		if (startIndex > documentOffset)
			return "";

		return doc.substring(startIndex, documentOffset);
	}

	/**
	 * Return the last word before the cursor (index) in an ocamldoc comment
	 */
	private String getLastWordDoc(ITextViewer viewer, int index) {
		String lastWord = "";
		try {
			char c = viewer.getDocument().getChar(index);
			if (c == ' ')
				return " ";
			while (c != ' ' && "abcdefghijklmnopqrstuvwxyz@".contains("" + c)) {
				lastWord = String.valueOf(c) + lastWord;
				index--;
				c = viewer.getDocument().getChar(index);
			}
		} catch (BadLocationException e) {
			return lastWord;
		}
		return lastWord;
	}

	/**
	 * Return the last word entered
	 */
	private String getLastWord(ITextViewer viewer, int index) {
		String lastWord = "";
		try {
			char c = viewer.getDocument().getChar(index);
			if (c == ' ')
				return " ";
			while (c != ' '
					&& "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_'".contains("" + c)) {
				lastWord = String.valueOf(c) + lastWord;
				index--;
				c = viewer.getDocument().getChar(index);
			}
		} catch (BadLocationException e) {
			return lastWord;
		}
		return lastWord;
	}

	/**
	 * Return the list of characters that activate completion when they are typed in the editor.
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.', '@' };
	}

	/**
	 * Return the list of characters that activate context informations when they are typed in the
	 * editor.
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * Provides a context information validator that will say whether the context information is
	 * still valid at a given position. This is used to make the information box disappear when it
	 * is no longer needed (it can also be discarded by the ESCAPE key). It is also used to format
	 * the text in this box with colors (but it cannot be modified there).
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return new OcamlContextInformation();
	}

	/** Return the expression under the caret (at documentOffset) in the viewer */
	private String expressionAtOffset(ITextViewer viewer, int documentOffset) {
		String doc = viewer.getDocument().get();

		int endOffset = doc.length();

		final String charsAfter = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'";

		for (int i = documentOffset; i < doc.length(); i++) {
			if (!charsAfter.contains("" + doc.charAt(i))) {
				endOffset = i;
				break;
			}
		}

		final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_1234567890'.";

		int startIndex = documentOffset;
		for (int i = endOffset - 1; i >= -1; i--) {
			if (i == -1) {
				startIndex = 0;
				break;
			}

			if (!chars.contains("" + doc.charAt(i))) {
				startIndex = i + 1;
				break;
			}
		}

		if (startIndex > endOffset)
			return "";

		return doc.substring(startIndex, endOffset);
	}

	/** Return context informations available at a given position in the editor (at documentOffset) */
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {

		IContextInformation[] infos;
		if (CompletionJob.isParsingFinished()) {
			Def definitionsRoot = CompletionJob.buildDefinitionsTree(this.project, true);

			String expression = expressionAtOffset(viewer, documentOffset);

			infos = findContextInformation(expression, definitionsRoot);

			return infos;
		}

		return null;
	}

	/**
	 * Find the element "expression" in the tree rooted in "definitionsRoot", and return the
	 * corresponding context informations.
	 */
	private IContextInformation[] findContextInformation(String expression, Def definitionsRoot) {

		ArrayList<Def> definitions = definitionsRoot.children;

		ArrayList<IContextInformation> infos = new ArrayList<IContextInformation>();

		// search in the module before the dot the element after the dot
		if (expression.contains(".")) {
			int index = expression.indexOf('.');
			String prefix = expression.substring(0, index);
			String suffix = expression.substring(index + 1);

			for (Def def : definitions) {
				if (def.name.equals(prefix)) {
					IContextInformation[] informations = findContextInformation(suffix, def);
					for (IContextInformation i : informations)
						infos.add(i);
				}
			}
			return infos.toArray(new IContextInformation[0]);
		}

		// search in the list of non-doted names
		else {

			for (Def def : definitions) {
				/*
				 * trick: the character '\u00A0' is a non-breakable space. It is used as a delimiter
				 * between parts.
				 */

				if (def.name.equals(expression)) {
					String body = def.body;
					// if (!def.getParentName().equals(""))
					// body = body + " (constructor of type " + def.getParentName() + ")";

					String message = body + "\u00A0";
					String comment = def.comment;
					if (!comment.equals(""))
						message = "\n" + message + "\n" + comment;
					String section = def.sectionComment;
					if (!section.equals(""))
						message = message + "\n\u00A0\nSection:\n" + section;
					else
						message = message + "\u00A0";

					String filename = def.filename;
					message = message + "\u00A0\n\n" + filename;

					message = message.trim();
					if (!message.equals("")) {
						String context = def.filename + " : " + def.body;
						infos.add(new ContextInformation(context, message));
					}
				}

			}

			return infos.toArray(new IContextInformation[0]);
		}
	}

	public String getErrorMessage() {
		return null;
	}
}
