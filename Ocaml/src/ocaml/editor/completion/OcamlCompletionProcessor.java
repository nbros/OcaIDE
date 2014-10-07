package ocaml.editor.completion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
import ocaml.parser.Def;
import ocaml.parser.Def.Type;
import ocaml.parsers.OcamlNewInterfaceParser;
import ocaml.util.Misc;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.editors.text.TextEditor;

/**
 * This class is responsible for managing completion in the OCaml editor.
 * <p>
 * Find the expression before the cursor in the OCaml editor, find completion proposals by going through the
 * definitions tree, find context informations that will appear after a completion proposal is selected...
 */
public class OcamlCompletionProcessor implements IContentAssistProcessor {

	 private final TextEditor editor;

	/** The partition type in which completion was triggered. */
	private final String partitionType;

	public OcamlCompletionProcessor(OcamlEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.partitionType = regionType;
	}

	public OcamlCompletionProcessor(OcamllexEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.partitionType = regionType;
	}

	public OcamlCompletionProcessor(OcamlyaccEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.partitionType = regionType;
	}

	/**
	 * To know whether the user is precising a completion or starting a new one, so as to dismiss the
	 * completion box when he types a space
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
			String[] docCompletions = { "@author ", "@deprecated ", "@param ", "@raise ", "@return ",
					"@see ", "@since ", "@version " };

			ArrayList<ICompletionProposal> docProposals = new ArrayList<ICompletionProposal>(8);
			for (String s : docCompletions)
				if (s.startsWith(lastWordDoc))
					docProposals.add(new SimpleCompletionProposal(s, documentOffset - lastWordDoc.length(),
							lastWordDoc.length(), s.length()));

			return docProposals.toArray(new ICompletionProposal[0]);
		}

		// "@": only for documentation comments
		if (lastWordDoc.startsWith("@"))
			return new OcamlCompletionProposal[0];

		String completion = completionExpression(viewer, documentOffset);

		OcamlCompletionProposal[] proposals = new OcamlCompletionProposal[0];

		IProject project = null;
		
		IDocument document = viewer.getDocument();

		// must wait parsing job finish first.
		if (CompletionJob.isParsingFinished()) {
			// lookup in module definition provided in editor first
			Def outlineDefinitionsRoot = null;
			if (editor instanceof OcamlEditor) {
				OcamlEditor ocamlEditor = (OcamlEditor) editor;
				project = ocamlEditor.getProject();
				outlineDefinitionsRoot = ocamlEditor.getOutlineDefinitionsTree();
			}
			else if (editor instanceof OcamllexEditor) {
				OcamllexEditor ocamllexEditor = (OcamllexEditor) editor;
				project = ocamllexEditor.getProject();
				outlineDefinitionsRoot = null;
			}
			else if (editor instanceof OcamlyaccEditor) {
				OcamlyaccEditor ocamlyaccEditor = (OcamlyaccEditor) editor;
				project = ocamlyaccEditor.getProject();
				outlineDefinitionsRoot = null;
			}
			else {
				project = null;
				outlineDefinitionsRoot = null;
			}
			
			Def interfacesDefinitionsRoot =	null;
			if (project != null)
				interfacesDefinitionsRoot = CompletionJob.buildDefinitionsTree(project, false);
			
			proposals = findCompletionProposals(completion, outlineDefinitionsRoot, interfacesDefinitionsRoot, document, documentOffset);
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
		/*
		 * if (!completion.contains(".") && this.ocamlEditor != null) { Def def =
		 * ocamlEditor.getOutlineDefinitionsTree(); if (def != null) { ICompletionProposal[]
		 * moduleCompletionProposals = findModuleCompletionProposals( def, documentOffset, lastWord.length(),
		 * lastWord);
		 * 
		 * for (int j = 0; j < moduleCompletionProposals.length; j++)
		 * allProposals.add(moduleCompletionProposals[j]); } else OcamlPlugin
		 * .logError("OcamlCompletionProcessor:computeCompletionProposals : module definitions=null"); }
		 */

		/*
		 * If a definition appears twice, remove the second one so that the user needn't press enter to enter
		 * it
		 */
		/*
		 * if (allProposals.size() == 2) { ICompletionProposal prop1 = allProposals.get(0);
		 * ICompletionProposal prop2 = allProposals.get(1);
		 * 
		 * if (prop1 instanceof OcamlCompletionProposal && prop2 instanceof SimpleCompletionProposal &&
		 * prop1.getDisplayString().equals(prop2.getDisplayString())) allProposals.remove(1); }
		 */

		return allProposals.toArray(new ICompletionProposal[] {});
	}

//	/** Return the completions from the module */
//	private ICompletionProposal[] findModuleCompletionProposals(Def root, int offset, int length,
//			String completion) {
//		TreeSet<SimpleCompletionProposal> proposals = new TreeSet<SimpleCompletionProposal>();
//
//		findModuleCompletionProposalsAux(root, offset, length, completion, proposals);
//
//		return proposals.toArray(new ICompletionProposal[0]);
//	}

//	/** Fill the list "<code>proposals</code>" with the completions found recursively */
//	private void findModuleCompletionProposalsAux(Def def, int offset, int length, String completion,
//			TreeSet<SimpleCompletionProposal> proposals) {
//		String name = def.name.trim();
//		Def.Type type = def.type;
//
//		if (name.startsWith(completion) && !name.equals("") && !name.equals("()") && !name.equals("_")
//				&& !type.equals(Def.Type.Open) && !type.equals(Def.Type.Include)
//				&& !type.equals(Def.Type.Root) && !type.equals(Def.Type.LetIn))
//			proposals.add(new SimpleCompletionProposal(def, name, offset - length, length, name.length()));
//
//		for (Def child : def.children)
//			findModuleCompletionProposalsAux(child, offset, length, completion, proposals);
//	}

	/**
	 * Find the completions matching the argument <code>completion</code> from all the definitions found in
	 * the definitionsRoot tree.
	 * 
	 * @return the completions found
	 */
	private OcamlCompletionProposal[] findCompletionProposals(String completion,
			Def outlineDefinitionsRoot,
			Def definitionsRoot,
			IDocument document,
			int offset) {

		// look in the module before the dot what's after the dot
		// The module can be like "A.B." or "c.D.E."
		if (completion.contains(".")) 
			return findDottedCompletionProposals(completion, outlineDefinitionsRoot, definitionsRoot, document, offset);
		// find elements starting by <completion> in the list of elements
		else
			return findNondottedCompletionProposals(completion, outlineDefinitionsRoot, definitionsRoot, document, offset);
	}
	
	// completion string must contained dots
	private OcamlCompletionProposal[] findDottedCompletionProposals(String completion,
			Def moduleDefinitionsRoot,
			Def interfacesDefinitionsRoot,
			IDocument document,
			int offset) {

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		// look in the module before the dot what's after the dot
		// The module can be like "A.B." or "c.D.E."
		if (completion.contains(".")) {
			//find the name of root module
			int index = completion.lastIndexOf('.');
			String[] parts = completion.substring(0,index).split("\\.");
			String prefix = parts[parts.length - 1];
			String suffix = completion.substring(index+1);
			int i = parts.length - 2;
			while (i >= 0) {
				if (parts[i].isEmpty())
					break;
				if (!Character.isUpperCase(parts[i].charAt(0)))
					break;
				suffix = prefix + "." + suffix; 
				prefix = parts[i];
				i--;
			}
			
			String moduleName = prefix;
			
			// look into current module first
			boolean stop = false;
			while (!stop) {
				stop = true;
				if (moduleDefinitionsRoot == null)
					break;
				
				for (Def def : moduleDefinitionsRoot.children) {
					if (def == null || def.name == null)
						break;
					
					if (def.name.equals(moduleName)) {
						if (def.type == Def.Type.Module) { 
							return findDottedCompletionProposals(suffix, moduleDefinitionsRoot, def, document, offset);
						}
						else if (def.type == Def.Type.ModuleAlias)  {
							String aliasedName = def.children.get(0).name;
							if (moduleName.equals(aliasedName)) 
								stop = true;
							else {
								moduleName = aliasedName;
								stop = false;
							}
							break;
						}
					}
				}
			}
			
			// then look into interface definitions
			if (interfacesDefinitionsRoot == null)
				return proposals.toArray(new OcamlCompletionProposal[0]);
			for (Def def: interfacesDefinitionsRoot.children) {
				if (def == null || def.name == null)
					continue;
				
				if (def.name.equals(moduleName) && (def.type == Def.Type.Module))
					return findDottedCompletionProposals(suffix, def, interfacesDefinitionsRoot, document, offset);
			}

		}
		// find elements starting by <completion> in the list of elements
		else {
			for (Def def : moduleDefinitionsRoot.children) {
				if (def.name.startsWith(completion) && isCompletionDef(def))
					proposals.add(new OcamlCompletionProposal(def, offset, completion.length()));

			}
		}

		return proposals.toArray(new OcamlCompletionProposal[0]);
	}
	
	private OcamlCompletionProposal[] findNondottedCompletionProposals(String completion,
			Def outlineDefinitionsRoot,
			Def interfacesDefinitionsRoot,
			IDocument document,
			int offset) {

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		// look up for definition
		final Def nearestDef = findNearestDefAt(outlineDefinitionsRoot, offset, document);
		proposals.addAll(lookCompletionProposalsUp(completion, nearestDef, offset));
		
		proposals = removeDuplicatedCompletionProposal(proposals);
		// look in current module
		if (outlineDefinitionsRoot == null)
			return proposals.toArray(new OcamlCompletionProposal[0]);
		
		for (Def def : outlineDefinitionsRoot.children) {
			if (def.name.startsWith(completion) && isCompletionDef(def))
				proposals.add(new OcamlCompletionProposal(def, offset, completion.length()));
		}
		
		proposals = removeDuplicatedCompletionProposal(proposals);
				// look in all opened or included modules
		if (interfacesDefinitionsRoot == null)
			return proposals.toArray(new OcamlCompletionProposal[0]);
		
		for (Def def : outlineDefinitionsRoot.children) {
			if (def == null || def.name == null)
				break;
			
			if (def.type == Def.Type.Open) {
				for (Def idef: interfacesDefinitionsRoot.children) {
					if (idef == null || idef.name == null)
						break;
					
					if (idef.name.equals(def.name)) {
						for (Def d: idef.children) {
							if (d == null || d.name == null)
								break;
							
							if (d.name.startsWith(completion) && isCompletionDef(d))
								proposals.add(new OcamlCompletionProposal(d, offset, completion.length()));
						}
					}
				}
			}
		}
		
		proposals = removeDuplicatedCompletionProposal(proposals);
		return proposals.toArray(new OcamlCompletionProposal[0]);
	}
	
	

	private ArrayList<OcamlCompletionProposal> lookCompletionProposalsUp(String completion, Def node, int offset) {
		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();
		
		if (node == null)
			return proposals;

		Def travelNode = node.parent;
		while (true) {
			if (travelNode == null || travelNode.name == null)
				break;

			if (travelNode.name.startsWith(completion)
					&& (travelNode.name.length() > completion.length())
					&& isCompletionDef(travelNode))
				proposals.add(new OcamlCompletionProposal(travelNode, offset, completion.length()));

			for (Def def : travelNode.children) {
				if (def == null || def.name == null)
					continue;
				if (def.name.startsWith(completion)
						&& (def.name.length() > completion.length())
						&& isCompletionDef(def))
					proposals.add(new OcamlCompletionProposal(def, offset, completion.length()));
			}

			if (travelNode.type == Def.Type.Root)
				break;
			
			travelNode = travelNode.parent;
		}
		
		return proposals;
	}
	
	private ArrayList<OcamlCompletionProposal> removeDuplicatedCompletionProposal(ArrayList<OcamlCompletionProposal> proposals) {

		ArrayList<OcamlCompletionProposal> newProposals = new ArrayList<OcamlCompletionProposal>();
		HashSet<String> names = new HashSet<>();
		for (OcamlCompletionProposal p: proposals) {
			String s = p.getDisplayString();
			if (!names.contains(s)) {
				newProposals.add(p);
				names.add(s);
			}
		}
		
		return newProposals;
	}
	
	
	/** Find an identifier (or an open directive) at a position in the document */
	private Def findNearestDefAt(Def def, int offset, IDocument doc) {

		if (def == null || doc == null)
			return null;
		
		if (def.children.size() == 0)
			return def;
		
		Def firstChild = def.children.get(0);
		IRegion region = firstChild.getRegion(doc);
		if (region != null) {
			if (region.getOffset() > offset)
				return def;
		}
		else return null;
		

		Def nearestChild = null;
		for (Def d : def.children) {
			region = d.getRegion(doc);
			if (region != null) {
				if (region.getOffset() < offset)
					nearestChild = d;
			}
		}
		
		return findNearestDefAt(nearestChild, offset, doc);
	}

	private boolean isCompletionDef(Def def) {
		switch (def.type) {
//		case Parameter:
//		case Object:
//		case LetIn:
		case Open:
		case Include:
		case In:
//		case Identifier:
		case Dummy:
		case Root:
//		case Sig:
//		case Struct:
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

		int startIndex = documentOffset;
		for (int i = documentOffset - 1; i >= -1; i--) {
			if (i == -1) {
				startIndex = 0;
				break;
			}

			char c = doc.charAt(i);
			if (!(Misc.isOcamlIdentifierChar(c) || c == '.')) {
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
			while (c != ' ' && (c >= 'a' && c <= 'z' || c == '@')) {
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
		IDocument document = viewer.getDocument();
		try {
			char c = document.getChar(index);
			if (c == ' ')
				return " ";
			while (c != ' ' && Misc.isOcamlIdentifierChar(c)) {
				lastWord = String.valueOf(c) + lastWord;
				index--;
				c = document.getChar(index);
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
	 * Return the list of characters that activate context informations when they are typed in the editor.
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * Provides a context information validator that will say whether the context information is still valid
	 * at a given position. This is used to make the information box disappear when it is no longer needed (it
	 * can also be discarded by the ESCAPE key). It is also used to format the text in this box with colors
	 * (but it cannot be modified there).
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return new OcamlContextInformation();
	}

	/** Return the expression under the caret (at documentOffset) in the viewer */
	private String expressionAtOffset(ITextViewer viewer, int documentOffset) {
		String doc = viewer.getDocument().get();

		int endOffset = doc.length();

		for (int i = documentOffset; i < doc.length(); i++) {
			if (!Misc.isOcamlIdentifierChar(doc.charAt(i))) {
				endOffset = i;
				break;
			}
		}

		int startIndex = documentOffset;
		for (int i = endOffset - 1; i >= -1; i--) {

			if (i == -1) {
				startIndex = 0;
				break;
			}

			char c = doc.charAt(i);
			if (!(Misc.isOcamlIdentifierChar(c) || c == '.')) {
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
		IProject project = null;
		if (editor instanceof OcamlEditor) {
			project = ((OcamlEditor) editor).getProject();
		}

		IContextInformation[] infos;
		if (CompletionJob.isParsingFinished()) {
			Def definitionsRoot = CompletionJob.buildDefinitionsTree(project, true);

			String expression = expressionAtOffset(viewer, documentOffset);

			infos = findContextInformation(expression, definitionsRoot);

			return infos;
		}

		return null;
	}

	/**
	 * Find the element "expression" in the tree rooted in "definitionsRoot", and return the corresponding
	 * context informations.
	 */
	private IContextInformation[] findContextInformation(String expression, Def definitionsRoot) {

		ArrayList<Def> definitions = definitionsRoot.children;

		ArrayList<IContextInformation> infos = new ArrayList<IContextInformation>();

		// search in the module before the dot the element after the dot
		if (expression.contains(".")) {
			int index = expression.lastIndexOf('.');
			String[] parts = expression.substring(0,index).split("\\.");
			String prefix = parts[parts.length - 1];
			String suffix = expression.substring(index+1);
			int i = parts.length - 2;
			while (i >= 0) {
				if (parts[i].isEmpty())
					break;
				if (!Character.isUpperCase(parts[i].charAt(0)))
					break;
				suffix = prefix + "." + suffix; 
				prefix = parts[i];
				i--;
			}

			String moduleName = prefix;
			boolean stop = false;
			while (!stop) {
				stop = true;
				for (Def def : definitions) {
					if (def.name.equals(moduleName)) {
						if (def.type == Def.Type.Module) {
							IContextInformation[] informations = findContextInformation(suffix, def);
							for (IContextInformation d : informations)
								infos.add(d);
							stop = true;
							break;
						}
						else if (def.type == Def.Type.ModuleAlias)  {
							String aliasedName = def.children.get(0).name;
							if (moduleName.equals(aliasedName)) 
								stop = true;
							else {
								moduleName = aliasedName;
								stop = false;
							}
							break;
						}
					}
				}
			}

			return infos.toArray(new IContextInformation[0]);
		}

		// search in the list of non-doted names
		else {

			for (Def def : definitions) {
				/*
				 * trick: the character '\u00A0' is a non-breakable space. It is used as a delimiter between
				 * parts.
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
