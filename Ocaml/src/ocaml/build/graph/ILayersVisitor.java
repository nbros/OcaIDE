package ocaml.build.graph;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This interface is used to define visitors that visit successively each vertex in a graph layer, from layer
 * 0 to the last layer (which can be empty).
 * 
 */
public interface ILayersVisitor {

	public boolean visit(final Vertex vertex, final IProgressMonitor monitor);

}
