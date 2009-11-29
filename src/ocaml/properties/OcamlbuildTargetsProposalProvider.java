package ocaml.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

/**
 * Completion for the ocamlbuild targets field: proposes a list of targets that
 * can be built.
 */
public class OcamlbuildTargetsProposalProvider implements IContentProposalProvider {

	private final IProject project;

	public OcamlbuildTargetsProposalProvider(IProject project) {
		this.project = project;
	}

	/**
	 * Return an array of content proposals for the field.
	 * 
	 * @param contents
	 *            the current contents of the field
	 * @param position
	 *            the current cursor position within the field
	 * @return the array of valid proposals for the field given its current
	 *         contents and cursor position.
	 */
	public IContentProposal[] getProposals(String contents, int position) {

		final List<String> proposals = computeAllProposals();
		// what the user typed before the cursor position
		final String prefix = computePrefix(contents, position);

		// filter proposals
		ListIterator<String> listIterator = proposals.listIterator();
		while (listIterator.hasNext()) {
			String proposal = listIterator.next();
			if (!proposal.startsWith(prefix))
				listIterator.remove();
		}

		IContentProposal[] contentProposals;
		contentProposals = new IContentProposal[proposals.size()];
		for (int i = 0; i < proposals.size(); i++) {
			String content = proposals.get(i).substring(prefix.length());
			String description = proposals.get(i);
			int cursorPos = position + content.length();
			contentProposals[i] = makeContentProposal(content, description, cursorPos);
		}
		return contentProposals;
	}

	/**
	 * Find what the user typed before the cursor position. Since commas
	 * separate entries, take the word before the last comma before the cursor
	 */
	private String computePrefix(String contents, int position) {
		int start;
		for (start = position; start > 0; start--) {
			char ch = contents.charAt(start - 1);
			if (ch == ',')
				break;
		}

		// int end;
		// for (end = start; end < contents.length(); end++) {
		// char ch = contents.charAt(end);
		// if (ch == ',')
		// break;
		// }

		return contents.substring(start, position).trim();
	}

	private List<String> computeAllProposals() {
		final List<String> mlFiles = new ArrayList<String>();
		final List<String> mliFiles = new ArrayList<String>();
		try {
			project.accept(new IResourceVisitor() {
				public boolean visit(IResource resource) throws CoreException {
					if (resource instanceof IFile) {
						IFile file = (IFile) resource;
						if (file.getName().endsWith(".ml")) {
							mlFiles.add(file.getName().substring(0, file.getName().length() - 3));
						}
						if (file.getName().endsWith(".mli")) {
							mliFiles.add(file.getName().substring(0, file.getName().length() - 4));
						}
					}
					if (resource instanceof IFolder) {
						IFolder folder = (IFolder) resource;
						// ignore source files in build folder
						if ("_build".equals(folder.getName()))
							return false;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			OcamlPlugin.logError(e);
		}

		Collections.sort(mlFiles);
		Collections.sort(mliFiles);

		final List<String> proposals = new ArrayList<String>();

		for (String mlFile : mlFiles) {
			proposals.add(mlFile + ".byte");
			proposals.add(mlFile + ".d.byte");
			proposals.add(mlFile + ".native");
			// proposals.add(mlFile + ".cmo");
			// proposals.add(mlFile + ".d.cmo");
			// proposals.add(mlFile + ".cmx");
			// proposals.add(mlFile + ".cma");
			// proposals.add(mlFile + ".d.cma");
			// proposals.add(mlFile + ".cmxa");
		}

		for (String mliFile : mliFiles) {
			proposals.add(mliFile + ".cmi");
			// proposals.add(mliFile + ".cmo");
			// proposals.add(mliFile + ".d.cmo");
		}

		return proposals;
	}

	/**
	 * Make an IContentProposal
	 * 
	 * @param content
	 *            the inserted content
	 * @param description
	 *            the string shown in the pop-up
	 * @param the
	 *            new cursor position after inserting
	 */
	private IContentProposal makeContentProposal(final String content, final String description,
			final int cursorPos) {
		return new IContentProposal() {
			public String getContent() {
				return content;
			}

			public String getDescription() {
				return null;
			}

			public String getLabel() {
				return description;
			}

			public int getCursorPosition() {
				return cursorPos;
			}
		};
	}
}