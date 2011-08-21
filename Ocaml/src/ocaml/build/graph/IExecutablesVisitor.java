package ocaml.build.graph;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * The interface for visitors of the graph's executables.
 *
 */
public interface IExecutablesVisitor {
	public boolean visit(final Vertex vertex,final IProgressMonitor monitor);
}
