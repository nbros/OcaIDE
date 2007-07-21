package ocaml.build.graph;

/**
 * This interface represents visitors to go through affected files recursively.
 * 
 * Visit the file first, and then visit all its affected files.
 * 
 * @see Vertex#accept(IPostAffectedFilesVisitor)
 * 
 */
public interface IPostAffectedFilesVisitor {

	public boolean visit(final Vertex ver);

	public void popVertex(final Vertex v);

	public void pushVertex(final Vertex v);

}
