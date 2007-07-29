package ocaml.parser;

import java.util.ArrayList;

import ocaml.OcamlPlugin;
import ocaml.util.Misc;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import beaver.Symbol;

/** A definition in the definitions tree */
public class Def extends beaver.Symbol {

	public enum Type {
		/** a dummy node: only used to build the AST, but discarded afterwards */
		Dummy,
		/** The 'in' part of a 'let in' definition: appears as the first child of a 'let in' */
		In,
		/** the root of the definitions tree (module implementation) */
		Root,
		/** a variable name, with its position */
		Identifier, Let, LetIn, Type, Module, ModuleType, Exception, External, Class, Sig,

		Open, Object, Method, Struct, Functor, Include, Val, Constraint, Initializer,

		ClassType, TypeConstructor, RecordTypeConstructor, Parameter,

		/** This type signals a parser error */
		ParserError

	};

	public ArrayList<Def> children;

	/**
	 * The (encoded) position of the beginning of the name which is used for this definition in the
	 * source code
	 */
	public int posStart;

	/**
	 * The (encoded) position of the end of the name which is used for this definition in the source
	 * code
	 */
	public int posEnd;

	/**
	 * The (encoded) position of the beginning of the first token which is used for this definition
	 * in the source code (ex: in "let a = 2", posStart is before 'a' and defPosStart is before
	 * 'let')
	 */
	public int defPosStart;
	/**
	 * The (encoded) position of the end of the last token which is used for this definition in the
	 * source code (ex: in "let a = 2", posEnd is after 'a' and defPosEnd is after '2')
	 */
	// public int defPosEnd;
	/** The offsets in the document of the definition */
	public int defOffsetStart;
	public int defOffsetEnd;

	public String name;

	public Type type;

	/**
	 * The type inferred by the OCaml compiler for this definition (this is retrieved and displayed
	 * in the outline when a ".annot" file is present and up-to-date)
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
	 * This is used by the error recovery mechanism. If true, then this node will appear at the root
	 * of the outline. If false, it is a child of an element whose bTop=true and so it will not
	 * appear itself at the root of the outline
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
		this.posStart = 0;
		this.posEnd = 0;
		this.defPosStart = 0;
		// this.defPosEnd = 0;
	}

	/** Create a definition node: let, type,... */
	public Def(String name, Type type, int start, int end) {
		super();
		children = new ArrayList<Def>();
		this.name = name;
		this.type = type;
		this.posStart = start;
		this.posEnd = end;
		this.defPosStart = 0;
		// this.defPosEnd = 0;
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
	 * Collapse all dummy nodes up to the next real nodes, so that <code>realNode</code> will
	 * become the root of all the immediately following real nodes.
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

	/** Find the first element of this type in the tree rooted at this definition */
	Def findFirstOfType(Type type) {
		if (this.type == type) {
			return this;
		}

		for (Def child : children) {
			Def result = child.findFirstOfType(type);
			if (result != null)
				return result;
		}

		return null;
	}

	public void print(int nestingLevel) {
		for (int i = 0; i < nestingLevel; i++)
			System.out.print("    ");

		System.out.println(this.type + " : " + this.name + " " + this.comment);

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
			def.bInIn = d.bInIn;
			def.defPosStart = d.defPosStart;
			def.defOffsetStart = d.defOffsetStart;
			def.defOffsetEnd = d.defOffsetEnd;
			def.ocamlType = d.ocamlType;
			def.comment = d.comment;
			def.sectionComment = d.sectionComment;
			def.filename = d.filename;
			def.body = d.body;

			newNode.add(def);
		}

		for (int i = 0; i < realNodes.size(); i++)
			cleanCopyAux(realNodes.get(i), newNode.children.get(i));
	}

	private void findRealChildren(Def node, ArrayList<Def> nodes, boolean root) {
		if (node.type == Type.Dummy || node.type == Type.Identifier || node.type == Type.Parameter
				|| node.type == Type.Functor || node.type == Type.Sig || node.type == Type.Object
				|| node.type == Type.Struct || node.type == Type.In || root
				|| "_".equals(node.name) || "()".equals(node.name)) {
			for (Def d : node.children)
				findRealChildren(d, nodes, false);
		} else {
			nodes.add(node);
		}
	}

	/** completely unnest the 'in' definitions */
	/*
	 * public void completelyUnnestIn(Def parent, int index) {
	 * 
	 * for(int i = 0; i < children.size(); i++){ Def child = children.get(i);
	 * child.completelyUnnestIn(this, i); }
	 * 
	 * ArrayList<Def> newChildren = new ArrayList<Def>();
	 * 
	 * int j = 1; for(int i = 0; i < children.size(); i++){ Def child = children.get(i);
	 * 
	 * if(type == Type.LetIn && child.type == Type.LetIn){ parent.children.add(index + j++, child);
	 * }else newChildren.add(child); }
	 * 
	 * children = newChildren; }
	 */

	/** Whether this definition is in the 'in' branch of its parent */
	boolean bInIn = false;

	/** Apply the 'in in' attribute (before the 'in' nodes are lost in the outline) */
	public void setInInAttribute() {
		for (Def child : children) {
			child.bInIn = (type == Def.Type.In);
			child.setInInAttribute();
		}
	}

	// /** Call this function on a 'Let' node to get a list of all the 'in' nodes in it to collapse
	// */
	// private void retrieveInInBranch(ArrayList<Def> inNodes, ArrayList<Def> otherNodes, Def def,
	// boolean root) {
	//
	// ArrayList<Def> newChildren = new ArrayList<Def>();
	//
	// for (Def child : def.children) {
	// // if(parent.type == Type.LetIn){
	// if (child.bInIn && def.type == Type.LetIn) {
	// child.bInIn = false;
	// if(inNodes != null)
	// inNodes.add(child);
	// retrieveInInBranch(inNodes, null, child);
	// /*}
	// else if (def.parent != null && (def.parent.type == Type.LetIn || def.parent.type ==
	// Type.Let)) {
	// newChildren.add(child);
	// retrieveInInBranch(inNodes, null, child);
	// */
	// } else {
	// if(otherNodes != null)
	// otherNodes.add(child);
	// newChildren.add(child);
	// }
	//
	// }
	//
	// def.children = newChildren;
	// }

	// /** Unnest the 'in' definitions */
	// public void unnestIn(Def def) {
	// if (false)
	// return;
	//
	// ArrayList<Def> inNodes = new ArrayList<Def>();
	// ArrayList<Def> otherNodes = new ArrayList<Def>();
	//
	// retrieveInInBranch(inNodes, otherNodes, def);
	//
	// // ArrayList<Def> newChildren = new ArrayList<Def>();
	//
	// /*
	// * for (int i = 0; i < otherNodes.size(); i++) { Def child = otherNodes.get(i);
	// * newChildren.add(child); }
	// */
	//
	// for (int i = 0; i < inNodes.size(); i++) {
	// Def child = inNodes.get(i);
	// def.children.add(child);
	// }
	//
	// for (Def child : otherNodes)
	// unnestIn(child);
	//
	// // children = newChildren;
	// }

	/** Unnest the 'in' definitions */
	public void unnestIn(Def parent, int index) {
		if (false)
			return;
		
		ArrayList<Def> newChildren = new ArrayList<Def>();
		ArrayList<Def> recChildren = new ArrayList<Def>();

		int j = 1;
		for (int i = 0; i < children.size(); i++) {
			Def child = children.get(i);

			if (type == Type.LetIn && child.type == Type.LetIn && child.bInIn) {
				parent.children.add(index + j, child);
				child.siblingsOffset = index + j;
				j++;
				child.parent = parent;
			} else{
				newChildren.add(child);
				child.siblingsOffset = newChildren.size() - 1;
				child.parent = this;
			}
			
			child.bInIn = false;
			recChildren.add(child);
		}

		children = newChildren;

		//ArrayList<Def> children2 = new ArrayList<Def>();
		//children2.addAll(this.children);
		for (int i = 0; i < recChildren.size(); i++) {
			Def child = recChildren.get(i);
			child.unnestIn(child.parent, child.siblingsOffset);
		}

		/*for (int i = 0; i < newChildren.size(); i++) {
			Def child = newChildren.get(i);
			child.unnestIn(child.parent, child.siblingsOffset);
		}*/

	}

	/** Unnest the 'in' definitions */
	// public void unnestIn(Def parent, int index) {
	// if (false)
	// return;
	//
	// ArrayList<Def> children2 = new ArrayList<Def>();
	// children2.addAll(this.children);
	// for (int i = 0; i < children2.size(); i++) {
	// Def child = children2.get(i);
	// child.unnestIn(this, i);
	// }
	//
	// ArrayList<Def> newChildren = new ArrayList<Def>();
	//
	// int j = 1;
	// for (int i = 0; i < children.size(); i++) {
	// Def child = children.get(i);
	//
	// if (type == Type.LetIn && child.type == Type.LetIn && child.bInIn) {
	// parent.children.add(index + j++, child);
	// } else
	// newChildren.add(child);
	//
	// child.bInIn = false;
	// }
	//
	// children = newChildren;
	// }

	/** Unnest the constructors from the types */
	public void unnestTypes(Def parent, int index) {
		for (int i = 0; i < children.size(); i++) {
			Def child = children.get(i);
			child.unnestTypes(this, i);
		}

		if (type != Type.Type)
			return;

		int j = 1;
		for (int i = 0; i < children.size(); i++) {
			Def child = children.get(i);
			parent.children.add(index + j++, child);
		}

		children = new ArrayList<Def>();
	}

	/** Returns the region in the document covered by the name of the definition */
	public IRegion getRegion(IDocument doc) {
		int lineOffset = 0;
		try {
			lineOffset = doc.getLineOffset(getLine(posStart));
		} catch (BadLocationException e) {
			OcamlPlugin.logError("offset error", e);
			return null;
		}

		int startOffset = lineOffset + getColumn(posStart);
		int endOffset = lineOffset + getColumn(posEnd);

		return new Region(startOffset, endOffset - startOffset + 1);

	}

	/** The ocamldoc comment associated with this definition */
	public String comment = "";

	public void appendToComment(String comment) {
		// the end-of-documentation delimiter (we just ignore it)
		if (comment.equals("/*"))
			return;

		if (this.comment.equals(""))
			this.comment = comment;
		else
			this.comment = this.comment + "\n________________________________________\n\n"
					+ comment;

		this.comment = clean(this.comment);
	}

	public String sectionComment = "";

	public void setSectionComment(String text) {
		this.sectionComment = clean(text);
	}

	public void setComment(String text) {
		if (text.equals("/*"))
			return;

		this.comment = clean(text);
	}

	public void setBody(String text) {
		this.body = Misc.beautify(clean(text));
	}

	private static String clean(String str) {
		if (str == null)
			return "";
		// remove all redundant spaces
		str = str.trim().replaceAll("( |\\t)( |\\t)+", " ");
		// remove the heading space
		str = str.replaceAll("\\n ", "\n");
		// remove empty lines
		str = str.replaceAll("\\n\\s*\\n", "\n");
		return str;
	}

	public String body = "";

	public String filename = "";

	public String parentName = "";

	/** Returns the region in the document covered by the entire definition */
	/*
	 * public IRegion getFullRegion(IDocument doc) { int firstLineOffset = 0; int lastLineOffset =
	 * 0; try { firstLineOffset = doc.getLineOffset(getLine(defPosStart)); lastLineOffset =
	 * doc.getLineOffset(getLine(defPosEnd)); } catch (BadLocationException e) {
	 * OcamlPlugin.logError("offset error", e); return null; }
	 * 
	 * int startOffset = firstLineOffset + getColumn(defPosStart); int endOffset = lastLineOffset +
	 * getColumn(defPosEnd);
	 * 
	 * return new Region(startOffset, endOffset - startOffset + 1); }
	 */
}
