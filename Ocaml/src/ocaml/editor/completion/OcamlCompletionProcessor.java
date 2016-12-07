package ocaml.editor.completion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.editors.OcamlEditor;
import ocaml.editors.lex.OcamllexEditor;
import ocaml.editors.yacc.OcamlyaccEditor;
import ocaml.parser.Def;
import ocaml.util.Misc;
import ocaml.typeHovers.OcamlAnnotParser;
import ocaml.typeHovers.TypeAnnotation;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.ui.dialogs.NewFolderDialog;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * This class is responsible for managing completion in the OCaml editor.
 * <p>
 * Find the expression before the cursor in the OCaml editor, find completion proposals by going through the
 * definitions tree, find context informations that will appear after a completion proposal is selected...
 */
public class OcamlCompletionProcessor implements IContentAssistProcessor {

	private final TextEditor editor;

	private final IProject project;

	// cache last parsed annotation file for speed up
	private TypeAnnotation[] lastUsedAnnotations = new TypeAnnotation[0];
	private String lastParsedFileName = "";
	private long lastParsedTime = 0;
	private static int cacheTime = 2000;

	/** The partition type in which completion was triggered. */
	private final String partitionType;

	public OcamlCompletionProcessor(OcamlEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.project = edit.getProject();
		this.partitionType = regionType;
	}

	public OcamlCompletionProcessor(OcamllexEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.project = edit.getProject();
		this.partitionType = regionType;
	}

	public OcamlCompletionProcessor(OcamlyaccEditor edit, String regionType) {
		this.editor = (TextEditor)edit;
		this.project = edit.getProject();
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

		IDocument document = viewer.getDocument();

		// must wait parsing job finish first.
		if (CompletionJob.isParsingFinished()) {
			Def interfacesDefinitionsRoot =	null;
			if (project != null)
				interfacesDefinitionsRoot = CompletionJob.buildDefinitionsTree(project, false);

			proposals = findCompletionProposals(completion, interfacesDefinitionsRoot, document, documentOffset);
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
			Def interfacesDefsRoot,
			IDocument doc,
			int offset) {

		ArrayList<OcamlCompletionProposal> proposals;

		String moduleName = editor.getEditorInput().getName();
		if (moduleName.endsWith(".ml"))
			moduleName = moduleName.substring(0, moduleName.length() - 3);
		else if (moduleName.endsWith(".mli"))
			moduleName = moduleName.substring(0, moduleName.length() - 4);
		if (moduleName.length() > 0)
			moduleName = Character.toUpperCase(moduleName.charAt(0))
					+ moduleName.substring(1);

		if (completion.contains("."))
			proposals = processDottedCompletion(completion, interfacesDefsRoot,
					moduleName, doc, offset, completion.length());
		else
			proposals = processNondottedCompletion(completion, interfacesDefsRoot,
					moduleName, doc, offset, completion.length());

		proposals = removeDuplicatedCompletionProposal(proposals);

		return proposals.toArray(new OcamlCompletionProposal[0]);
	}

	// completion string must contained dots
	private ArrayList<OcamlCompletionProposal> processDottedCompletion(String completion,
			Def interfacesDefsRoot,
			String moduleName,
			IDocument document,
			int offset,
			int length) {

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		if (interfacesDefsRoot == null)
			return proposals;

		// look in the module before the dot what's after the dot
		// The module can be like "A.B." or "c.D.E."
		if (completion.contains(".")) {
			// find the first module in completion string
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

			boolean isLowerCasePrefix= false;
			if (prefix.length() > 0 && Character.isLowerCase(prefix.charAt(0)))
				isLowerCasePrefix = true;

			Def currentDef = null;
			for (Def def: interfacesDefsRoot.children) {
				if (def.name.equals(moduleName)) {
					currentDef = def;
					break;
				}
			}

			/*
			 * prefix is a lower-case identifier, hence look for suffix completion
			 * in the current module, sub-modules, included modules or opened modules
			 */
			if (isLowerCasePrefix) {

				// find in current def
				for (Def def : currentDef.children) {
					// look for completion of suffix in the current module
					if (checkCompletion(def, suffix) && isCompletionDef(def)) {
						Def proposedDef = createProposalDef(project, def);
						proposals.add(new OcamlCompletionProposal(proposedDef,
								offset, suffix.length()));
					}

					// look for completion in opened or included module of current module
					// by attach the involved module name to suffix and find new completion
					if (def.type == Def.Type.Open || def.type == Def.Type.Include) {
						String newCompletion = def.name + "." + suffix;
						proposals.addAll(processDottedCompletion(newCompletion,
								interfacesDefsRoot, moduleName, document,
								offset, suffix.length()));
					}
				}

				// completion maybe modules's name
				for (Def def : interfacesDefsRoot.children) {
					if (checkCompletion(def, suffix) && isCompletionDef(def)) {
						Def proposedDef = createProposalDef(project, def);
						proposals.add(new OcamlCompletionProposal(proposedDef,
								offset, suffix.length()));
					}
				}
			}
			/*
			 * prefix is upper-case identifier, hence look for a module which has
			 * name is or aliased by 'prefix'
			 */
			else {
				// bottom-up search to look for prefix in sub-module
				// or aliased module of current modules
				final Def nearestDef = findSmallestDefAtOffset(currentDef, offset, document);
				String newPrefix = bottomUpFindAliasedModule(prefix, "", nearestDef);
				String newSuffix = suffix;

				// compute prefix, suffix
				if (newPrefix.contains(".")) {
					index = newPrefix.indexOf('.');
					newSuffix = newPrefix.substring(index+1);
					newSuffix = combineModuleNameParts(newSuffix, suffix);
					newPrefix = newPrefix.substring(0,index);
				}

				// find in other modules
				for (Def def: interfacesDefsRoot.children) {
					if (def.name.equals(newPrefix))
						proposals.addAll(lookupProposalsCompletionInDef(
								newSuffix, def, interfacesDefsRoot, document,
								offset, length));
				}
			}

		}
		// find elements starting by <completion> in the list of elements
		else {
			for (Def def : interfacesDefsRoot.children) {
				if (checkCompletion(def, completion) && isCompletionDef(def)) {
					Def proposedDef = createProposalDef(project, def);
					proposals.add(new OcamlCompletionProposal(proposedDef, offset, completion.length()));
				}
			}
		}

		return proposals;
	}


	private ArrayList<OcamlCompletionProposal> processNondottedCompletion(String completion,
			Def interfacesDefsRoot,
			String moduleName,
			IDocument document,
			int offset,
			int length) {

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		if (interfacesDefsRoot == null)
			return proposals;

		/*
		 *  look in def root
		 */
		for (Def def: interfacesDefsRoot.children) {
			if (checkCompletion(def, completion)) {
				Def proposedDef = createProposalDef(project, def);
				proposals.add(new OcamlCompletionProposal(proposedDef, offset, length));
			}
		}

		/*
		 *  looked in current module
		 */
		Def currentDef = null;
		for (Def def: interfacesDefsRoot.children) {
			if (def.name.equals(moduleName)) {
				currentDef = def;
				break;
			}
		}

		if (currentDef != null) {
			// search defns from current def to its parents (bottom-up search)
			final Def nearestDef = findSmallestDefAtOffset(currentDef, offset, document);
			proposals.addAll(bottomUpFindProposals(completion, nearestDef, offset));

			// looking in children of current module
			for (Def def: currentDef.children) {
				if (checkCompletion(def, completion)) {
					Def proposedDef = createProposalDef(project, def);
					proposals.add(new OcamlCompletionProposal(proposedDef, offset, length));
				}
			}

			/*
			 * look in opened module of current module
			 */
			for (Def def : currentDef.children) {
				if (def.type != Def.Type.Open && def.type != Def.Type.Include)
					continue;

				String newCompletion = def.name + "." + completion;
				proposals.addAll(processDottedCompletion(newCompletion,
						interfacesDefsRoot, moduleName, document, offset, length));
			}
		}

		/*
		 * look in Pervasives module, which is always opended
		 */
		for (Def def: interfacesDefsRoot.children) {
			if (!def.name.equals("Pervasives"))
				continue;

			for (Def d: def.children) {
				if (d == null || d.name == null)
					break;

				if (checkCompletion(d, completion) && isCompletionDef(d)) {
					Def proposedDef = createProposalDef(project, d);
					proposals.add(new OcamlCompletionProposal(proposedDef, offset, completion.length()));
				}
			}
		}

		return proposals;
	}

	private ArrayList<OcamlCompletionProposal> lookupProposalsCompletionInDef(String completion,
			Def defsRoot,
			Def interfacesDefRoot,
			IDocument document,
			int offset,
			int length) {

		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		if (defsRoot == null)
			return proposals;

		if (completion.contains(".")) {
			// find the first module in completion string
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

			// look inside def roots first
			for (Def def: defsRoot.children) {
				if (def.name.equals(prefix))
					proposals.addAll(lookupProposalsCompletionInDef(suffix, def,
							interfacesDefRoot, document, offset, length));
			}

			// look inside included module
			for (Def def1: defsRoot.children) {
				if (def1.type == Def.Type.Include) {
					Def includedDef = def1;
					// look for included def in current defs
					for (Def def2: defsRoot.children) {
						if (def2.name.equals(includedDef.name)) {
							Def includedDefRoot = def2;
							proposals.addAll(lookupProposalsCompletionInDef(
									completion,includedDefRoot, interfacesDefRoot,
									document, offset, length));
						}

					}
					// look for included def in interfaces root defs
					for (Def def2: interfacesDefRoot.children) {
						if (def2.name.equals(includedDef.name)) {
							Def includedDefRoot = def2;
							proposals.addAll(lookupProposalsCompletionInDef(
									completion, includedDefRoot, interfacesDefRoot,
									document, offset, length));
						}

					}
				}
			}
		}
		// find elements starting by <completion> in the list of elements
		else {
			// look inside def roots first
			for (Def def : defsRoot.children) {
				if (checkCompletion(def, completion) && isCompletionDef(def)) {
					Def proposedDef = createProposalDef(project, def);
					proposals.add(new OcamlCompletionProposal(proposedDef,
							offset, completion.length()));
				}
			}
			// look inside included module
			for (Def def1: defsRoot.children) {
				if (def1.type == Def.Type.Include) {
					Def includedDef = def1;
					// look for included def in current defs
					for (Def def2: defsRoot.children) {
						if (def2.name.equals(includedDef.name)) {
							Def includedDefRoot = def2;
							proposals.addAll(lookupProposalsCompletionInDef(
									completion, includedDefRoot, interfacesDefRoot,
									document, offset, length));
						}

					}
					// look for included def in interfaces root defs
					for (Def def2: interfacesDefRoot.children) {
						if (def2.name.equals(includedDef.name)) {
							Def includedDefRoot = def2;
							proposals.addAll(lookupProposalsCompletionInDef(
									completion, includedDefRoot, interfacesDefRoot,
									document, offset, length));
						}

					}
				}
			}

		}

		return proposals;
	}

	private ArrayList<OcamlCompletionProposal> bottomUpFindProposals(String completion, Def node, int offset) {
		ArrayList<OcamlCompletionProposal> proposals = new ArrayList<OcamlCompletionProposal>();

		if (node == null)
			return proposals;

		Def travelNode = node.parent;
		while (true) {
			if (travelNode == null || travelNode.name == null)
				break;

			if (checkCompletion(travelNode, completion) && isCompletionDef(travelNode)) {
				Def proposedDef = createProposalDef(project, travelNode);
				proposals.add(new OcamlCompletionProposal(proposedDef, offset, completion.length()));
			}

			for (Def def : travelNode.children) {
				if (def == null || def.name == null)
					continue;
				if (checkCompletion(def, completion)) {
//						&& (def.type == Def.Type.Let
//								|| def.type == Def.Type.LetIn
//								|| def.type == Def.Type.Parameter)) {
					Def proposedDef = createProposalDef(project, def);
					proposals.add(new OcamlCompletionProposal(proposedDef, offset, completion.length()));
				}
			}

			if (travelNode.type == Def.Type.Root)
				break;

			travelNode = travelNode.parent;
		}

		return proposals;
	}

	private String combineModuleNameParts(String prefix, String suffix) {
		if (suffix.isEmpty())
			return prefix;
		else
			return prefix + "." + suffix;
	}

	private String bottomUpFindAliasedModule(String prefixAlias, String suffixAlias, Def node) {
		if (node == null)
			return combineModuleNameParts(prefixAlias, suffixAlias);

		Def travelNode = node.parent;
		String newPrefixAlias = prefixAlias;
		while (true) {
			if (travelNode == null || travelNode.name == null)
				break;

			if (travelNode.type == Def.Type.ModuleAlias
					&& (travelNode.name.compareTo(newPrefixAlias) == 0)) {
				if (travelNode.children.size() > 0) {
					newPrefixAlias = travelNode.children.get(0).name;
					if (newPrefixAlias.contains(".")) // stop when name has "."
						break;
				}
			}

			if (travelNode.type == Def.Type.Root)
				break;

			travelNode = travelNode.parent;
		}
		// if aliased module is a sub-module (containing "."), then find
		// the first part. Otherwise, continue to search aliased module
		if (newPrefixAlias.contains(".")) {
			String[] parts = newPrefixAlias.split("\\.");
			String newSuffixAlias = "";
			for (int i = parts.length-1; i > 0; i--)
				newSuffixAlias = parts[i] + "." + newSuffixAlias ;
			if (newSuffixAlias.length() > 0) {
				newSuffixAlias = newSuffixAlias + suffixAlias;
			} else
				newSuffixAlias = suffixAlias;
			newPrefixAlias = parts[0];
			return bottomUpFindAliasedModule(newPrefixAlias, newSuffixAlias, travelNode);
		}
		else
			return combineModuleNameParts(newPrefixAlias, suffixAlias);
	}

	private ArrayList<OcamlCompletionProposal> removeDuplicatedCompletionProposal(ArrayList<OcamlCompletionProposal> proposals) {

		ArrayList<OcamlCompletionProposal> newProposals = new ArrayList<OcamlCompletionProposal>();
		HashSet<String> proposalHashSet = new HashSet<>();
		for (OcamlCompletionProposal p: proposals) {
			String s = p.getAdditionalProposalInfo(null);
			if (!proposalHashSet.contains(s)) {
				newProposals.add(p);
				proposalHashSet.add(s);
			}
		}

		return newProposals;
	}


	/** Find an identifier (or an open directive) at a position in the document */
	private Def findSmallestDefAtOffset(Def def, int offset, IDocument doc) {

		if (def == null || doc == null)
			return null;

		if (def.children.size() == 0)
			return def;

		Def firstChild = def.children.get(0);
		IRegion region = firstChild.getNameRegion(doc);
		if (region != null) {
			if (region.getOffset() > offset)
				return def;
		}
		else return null;


		Def nearestChild = null;
		for (Def d : def.children) {
			region = d.getNameRegion(doc);
			if (region != null) {
				if (region.getOffset() < offset)
					nearestChild = d;
			}
		}

		return findSmallestDefAtOffset(nearestChild, offset, doc);
	}

	private boolean checkCompletion(Def def, String completion) {
		if (def == null)
			return false;

		if (def.name == null)
			return false;

		if (def.name.startsWith(completion)){
//			if (def.type != Def.Type.Identifier)
//				return true;
//			else if (def.name.length() > completion.length())
//				return true;
			return true;
		}

		return false;
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
					String body = def.getBody();
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

					String filename = def.getFileName();
					message = message + "\u00A0\n\n" + filename;

					message = message.trim();
					if (!message.equals("")) {
						String context = def.getFileName() + " : " + body;
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

	private Def createProposalDef(IProject project, Def def) {
		Def newDef = new Def(def);
		if (newDef.type == Def.Type.Let
				|| newDef.type == Def.Type.LetIn
				|| newDef.type == Def.Type.External) {

			String typeInfo = "";

			// look for type infor in body first
			String body = newDef.getBody();
			int index = body.indexOf(newDef.name);
			if (index >= 0 && body.length() > newDef.name.length()) {
				typeInfo = body.substring(index);
				newDef.setOcamlType(typeInfo);
			}

			// not found type in body
			if (typeInfo.isEmpty()) {
				String filename = newDef.getFileName();

				// store last used annotation for caching
				TypeAnnotation[] annotations;
				long currentTime = System.currentTimeMillis();
				if (filename.equals(lastParsedFileName)
						&& (currentTime - lastParsedTime < cacheTime)) {
					annotations = lastUsedAnnotations;
				}
				else {
					annotations = parseModuleAnnotation(project, filename);
					lastUsedAnnotations = annotations;
					lastParsedFileName = filename;
					lastParsedTime = System.currentTimeMillis();
				}

				IDocument document = getDocument(project, filename);
				typeInfo = computeTypeInfo(newDef, annotations, document);
				if (!typeInfo.isEmpty()) {
					newDef.setOcamlType(typeInfo);
					newDef.setBody("val " + typeInfo);
				}
			}
		}
		else if (def.type == Def.Type.Type) {
			String typeInfo = newDef.name + " 't";
			newDef.setOcamlType(typeInfo);
		}

		return newDef;
	}

	private IDocument getDocument(IProject project, String filename) {
		if (project == null)
			return null;

		if (filename.isEmpty())
			return null;

		try {
			IFile[] files = project.getWorkspace().getRoot().findFilesForLocationURI(URIUtil.toURI(filename));
			if (files.length == 0)
				return null;

			IFile file = files[0];
			IDocumentProvider provider = new TextFileDocumentProvider();
			provider.connect(file);
			IDocument document = provider.getDocument(file);

			return document;
		} catch (Exception e) {
			return null;
		}
	}


	private TypeAnnotation[] parseModuleAnnotation(IProject project, String filename) {
		if (project == null)
			return new TypeAnnotation[0];

		if (filename.isEmpty())
			return new TypeAnnotation[0];

		try {
			IFile[] files = project.getWorkspace().getRoot().findFilesForLocationURI(URIUtil.toURI(filename));
			if (files.length == 0)
				return new TypeAnnotation[0];

			IFile file = files[0];
			IPath relativeProjectPath = file.getFullPath();
			IDocumentProvider provider = new TextFileDocumentProvider();
			provider.connect(file);
			IDocument document = provider.getDocument(file);

			File annotFile = null;

			annotFile = Misc.getOtherFileFor(file.getProject(), relativeProjectPath, ".annot");

			if (annotFile != null && annotFile.exists()) {
				TypeAnnotation[] annotations = OcamlAnnotParser.parseFile(annotFile, document);

				return annotations;
			}
		} catch (Exception e) {
//			e.printStackTrace();
			return new TypeAnnotation[0];
		}

		return new TypeAnnotation[0];

	}

	private String computeTypeInfo(Def def, TypeAnnotation[] annotations, IDocument document) {
		try {
			String typeInfo = "";
			IRegion region = def.getNameRegion(document);
			int offset = region.getOffset();

			ArrayList<TypeAnnotation> found = new ArrayList<TypeAnnotation>();

			if (annotations != null) {
				for (TypeAnnotation annot : annotations)
					if (annot.getBegin() <= offset && offset < annot.getEnd())
						found.add(annot);

				/*
				 * Search for the smallest hovered type annotation
				 */
				TypeAnnotation annot = null;
				int minSize = Integer.MAX_VALUE;

				for (TypeAnnotation a : found) {
					int size = a.getEnd() - a.getBegin();
					if (size < minSize) {
						annot = a;
						minSize = size;
					}
				}

				String docContent = document.get();
				if (annot != null) {
					String expr = docContent.substring(annot.getBegin(), annot.getEnd());
					String[] lines = expr.split("\\n");
					if (expr.length() < 50 && lines.length <= 6)
						typeInfo = expr + ": " + annot.getType();
					else if (lines.length > 6) {
						int l = lines.length;
						typeInfo = lines[0] + "\n" + lines[1] + "\n" + lines[2]
								+ "\n" + "..." + (l - 6) + " more lines...\n" + lines[l - 3]
								+ "\n" + lines[l - 2] + "\n" + lines[l - 1] + "\n:" + annot
								.getType();
					} else
						typeInfo = expr + "\n:" + annot.getType();
				}
			}
			return typeInfo.trim();
		} catch (Exception e) {
			return "";
		}

	}

	ArrayList<Integer> lineOffsets = null;

	private void computeLinesStartOffset(String doc) {
		lineOffsets = new ArrayList<Integer>();
		lineOffsets.add(0);
		for (int i = 0; i < doc.length(); i++) {
			/*
			 * if(doc.charAt(i) == '\r') System.err.print("<R>\n");
			 * if(doc.charAt(i) == '\n') System.err.print("<N>\n"); else
			 * System.err.print(doc.charAt(i));
			 */

			if (doc.charAt(i) == '\n') {
				lineOffsets.add(i + 1);
				// System.err.println(i);
			}

		}
	}


}
