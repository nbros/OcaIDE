package ocaml.natures;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;

/** The O'Caml Makefile projects nature */
public class OcamlNatureMakefile implements IProjectNature {
	private IProject project;

	/**
	 * nature unique identifier
	 */
	public static final String ID = "ocaml.ocamlnatureMakefile";
	
	
	public void configure() throws CoreException {
		final String OCAML_BUILDER_ID = "Ocaml.ocamlMakefileBuilder";
		IProjectDescription desc = project.getDescription();
		ICommand[] cmds = desc.getBuildSpec();
		ICommand[] newCmds = new ICommand[cmds.length + 1];

		System.arraycopy(cmds, 0, newCmds, 1, cmds.length);
		ICommand cmdBuilder = desc.newCommand();
		//cmdBuilder.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, false);
		
		cmdBuilder.setBuilderName(OCAML_BUILDER_ID);
		newCmds[0] = cmdBuilder;
		desc.setBuildSpec(newCmds);
		project.setDescription(desc, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return this.project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}

}
