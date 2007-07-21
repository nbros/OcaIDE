package ocaml.build.graph;

/**
 * This interface represents visitors that go through required files recursively.
 * 
 * @see Vertex#accept(IPreNeededFilesVisitor)
 * @see IPostNeededFilesVisitor pour les méthodes popVertex et pushVertex
 * 
 */
public interface IPreNeededFilesVisitor {

	public boolean visit(final Vertex n);

	public void popVertex(final Vertex v);

	public void pushVertex(final Vertex v);

}
