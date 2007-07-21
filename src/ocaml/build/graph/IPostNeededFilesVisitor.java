package ocaml.build.graph;

/**
 * This interface represents visitors that go through required files recursively.
 * 
 * @see Vertex#accept(IPostNeededFilesVisitor)
 * 
 */
public interface IPostNeededFilesVisitor {

	public boolean visit(final Vertex v);

	public void popVertex(final Vertex v);

	public void pushVertex(final Vertex v);

}
