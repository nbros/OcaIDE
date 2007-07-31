package ocaml.build.ocamlbuild;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class OcamlbuildBuilder extends IncrementalProjectBuilder {

	public static final String ID = "Ocaml.ocamlbuildBuilder";
	
	public OcamlbuildBuilder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		if (kind == IncrementalProjectBuilder.FULL_BUILD)
			System.err.println("full build");
		else if (kind == IncrementalProjectBuilder.AUTO_BUILD)
			System.err.println("auto build");
		else if (kind == IncrementalProjectBuilder.CLEAN_BUILD)
			System.err.println("clean build");
		else if (kind == IncrementalProjectBuilder.INCREMENTAL_BUILD)
			System.err.println("incremental build");
		else
			System.err.println("unknown kind of build!");
		
		/*if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}*/
		return null;
	}
	
	

	@Override
	protected void startupOnInitialize() {
	}

	@Override
	protected void clean(IProgressMonitor monitor) {
		System.err.println("clean");
	}

}
