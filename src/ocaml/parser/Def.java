package ocaml.parser;

import java.util.ArrayList;

import beaver.Symbol;

/** A definition in the definitions tree */
public class Def extends beaver.Symbol {

	public enum Type {
		/** a dummy node: only used to build the AST, but discarded afterwards */
		Dummy,
		/** the root of the definitions tree (module implementation) */
		Root,
		/** a variable name, with its position */
		Identifier, Let, LetIn, Type, Module, ModuleType, Exception, External, Class, Sig,

		Open, Object, Method, Struct, Functor, Include, Val, Constraint, Initializer,

		ClassType, TypeConstructor, RecordTypeConstructor, Parameter

	};

	public ArrayList<Def> children;

	public final int posStart;

	public final int posEnd;

	public String name;

	public Type type;

	/**
	 * The type inferred by the OCaml compiler for this definition (this is retrieved and displayed in the
	 * outline when a ".annot" file is present and up-to-date)
	 */
	public String ocamlType;

	/** The parent of this node. This is required by the outline's ContentProvider */
	public Def parent;

	/** The offset of this node among its siblings (in the children list of its parent) */
	private int siblingsOffset;

	public int getSiblingsOffset() {
		return siblingsOffset;
	}

	/** does this definition appear after another definition using an "and" */
	public boolean bAnd = false;

	/**
	 * This is used by the error recovery mechanism. If true, then this node will appear at the root of the
	 * outline. If false, it is a child of an element whose bTop=true and so it will not appear itself at the
	 * root of the outline
	 */
	public boolean bTop = true;

	/** Use alternative icon (indicating a special property, such as private, mutable, ...) */
	public boolean bAlt = false;
	
	
	/** Is this definition recursive? */
	public boolean bRec = false;

	/**
	 * Create a dummy node (which is used to build the AST, but will be discarded afterwards)
	 */
	public Def() {
		super();
		children = new ArrayList<Def>();
		this.name = null;
		this.type = Type.Dummy;
		this.posStart = -1;
		this.posEnd = -1;
	}

	/** Create a definition node: let, type,... */
	public Def(String name, Type type, int start, int end) {
		super();
		children = new ArrayList<Def>();
		this.name = name;
		this.type = type;
		this.posStart = start;
		this.posEnd = end;
	}

	void add(Symbol s) {
		assert s instanceof Def;
		children.add((Def) s);
	}

	/** Creates a new dummy node as root of a and b, and return this node */
	public static Def root(Symbol a) {
		assert a instanceof Def;
		Def def = new Def();
		Def defa = (Def) a;
		def.children.add(defa);
		return def;
	}

	/** Creates a new dummy node as root of a and b, and return this node */
	public static Def root(Symbol a, Symbol b) {
		assert a instanceof Def;
		assert b instanceof Def;

		Def def = new Def();

		Def defa = (Def) a;
		Def defb = (Def) b;

		def.children.add(defa);
		def.children.add(defb);

		return def;
	}

	/** Creates a new dummy node as root of a, b and c, and return this node */
	public static Def root(Symbol a, Symbol b, Symbol c) {
		assert a instanceof Def;
		assert b instanceof Def;
		assert c instanceof Def;

		Def def = new Def();

		Def defa = (Def) a;
		Def defb = (Def) b;
		Def defc = (Def) c;

		def.children.add(defa);
		def.children.add(defb);
		def.children.add(defc);

		return def;
	}

	/** Creates a new dummy node as root of a, b, c and d, and return this node */
	public static Def root(Symbol a, Symbol b, Symbol c, Symbol d) {
		assert a instanceof Def;
		assert b instanceof Def;
		assert c instanceof Def;
		assert d instanceof Def;

		Def def = new Def();

		Def defa = (Def) a;
		Def defb = (Def) b;
		Def defc = (Def) c;
		Def defd = (Def) d;

		def.children.add(defa);
		def.children.add(defb);
		def.children.add(defc);
		def.children.add(defd);

		return def;
	}

	/** Creates a new Def from an ident (as a terminal symbol) */
	public static Def ident(Symbol i) {
		Def def = new Def((String) i.value, Type.Identifier, i.getStart(), i.getEnd());
		return def;
	}

	/**
	 * Collapse all dummy nodes up to the next real nodes, so that <code>realNode</code> will become the
	 * root of all the immediately following real nodes.
	 */
	public void collapse() {

		ArrayList<Def> realNodes = new ArrayList<Def>();

		for (Def d : this.children)
			collapseAux(d, realNodes, false);

		this.children = realNodes;
	}

	private void collapseAux(Def node, ArrayList<Def> nodes, boolean bClean) {
		if (node.type == Type.Dummy || bClean && node.type == Type.Identifier) {
			for (Def d : node.children)
				collapseAux(d, nodes, bClean);
		} else {
			nodes.add(node);
			if (bClean)
				node.clean();
		}
	}

	/**
	 * Clean the tree descending from this node: remove all the Dummy nodes and the Identifiers
	 */
	public void clean() {
		ArrayList<Def> realNodes = new ArrayList<Def>();

		for (Def d : this.children)
			collapseAux(d, realNodes, true);

		this.children = realNodes;
	}

	/** Find all the identifiers in the tree rooted at this definition */
	void findIdents(ArrayList<Def> idents) {
		if (this.type == Type.Identifier)
			idents.add(this);
		for (Def child : children)
			child.findIdents(idents);
	}

	/** Find all the "lets" in the tree rooted at this definition (and do not go further down) */
	void findLets(ArrayList<Def> idents) {
		if (this.type == Type.Let) {
			idents.add(this);
			return;
		}
		for (Def child : children)
			child.findLets(idents);
	}

	public void print(int nestingLevel) {
		for (int i = 0; i < nestingLevel; i++)
			System.out.print("    ");

		System.out.println(this.type + " : " + this.name + (this.bAnd ? " (blue)" : "") + "  "
				+ getLine(posStart) + ":" + getColumn(posStart));

		for (Def child : children)
			child.print(nestingLevel + 1);
	}

	public void buildParents() {
		for (Def child : children) {
			child.parent = this;
			child.buildParents();
		}
	}

	public void buildSiblingOffsets() {
		for (int i = 0; i < children.size(); i++) {
			Def child = children.get(i);
			child.siblingsOffset = i;
			child.buildSiblingOffsets();
		}
	}

	public Def cleanCopy() {
		Def def = new Def("<root>", Def.Type.Root, 0, 0);
		cleanCopyAux(this, def);
		def.buildParents();
		return def;
	}

	private void cleanCopyAux(Def node, Def newNode) {
		ArrayList<Def> realNodes = new ArrayList<Def>();
		findRealChildren(node, realNodes, true);

		for (Def d : realNodes) {
			Def def = new Def(d.name, d.type, d.posStart, d.posEnd);
			def.bAlt = d.bAlt;
			def.bAnd = d.bAnd;
			def.bRec = d.bRec;
			def.ocamlType = d.ocamlType;
			newNode.add(def);
		}

		for (int i = 0; i < realNodes.size(); i++)
			cleanCopyAux(realNodes.get(i), newNode.children.get(i));
	}

	private void findRealChildren(Def node, ArrayList<Def> nodes, boolean root) {
		if (node.type == Type.Dummy || node.type == Type.Identifier || node.type == Type.Parameter
				|| node.type == Type.Sig || node.type == Type.Object || node.type == Type.Struct || root) {
			for (Def d : node.children)
				findRealChildren(d, nodes, false);
		} else {
			nodes.add(node);
		}
	}

}
