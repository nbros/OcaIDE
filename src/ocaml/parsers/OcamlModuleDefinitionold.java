//package ocaml.parsers;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//
///**
// * A definition: variable, function, type, module,...<br>
// * This is used (among other things) to build the outline
// */
//public class OcamlModuleDefinitionold {
//
//	public OcamlModuleDefinitionold() {
//		indent = -1;
//		line = -1;
//		name = "";
//		type = Type.DefModule;
//		children = new ArrayList<OcamlModuleDefinition>();
//		parent = null;
//	}
//
//	public enum Type {
//		DefLet, DefType, DefModule, DefException, DefExternal, DefClass, DefSig, Open, DefObject,
//		DefMethod
//	};
//
//	/** Indentation in number of spaces */
//	private int indent;
//	/** The line number on which this definition was found */
//	private int line;
//	/** The name of the variable defined */
//	private String name;
//	/** The kind of definition */
//	private Type type;
//
//	/** The list of definitions contained in this definition */
//	private ArrayList<OcamlModuleDefinition> children;
//
//	private OcamlModuleDefinition parent;
//
//	public OcamlModuleDefinition getParent() {
//		return parent;
//	}
//
//	private void setParent(OcamlModuleDefinition parent) {
//		this.parent = parent;
//	}
//
//	public int getIndent() {
//		return indent;
//	}
//
//	public void setIndent(int indent) {
//		this.indent = indent;
//	}
//
//	public int getLine() {
//		return line;
//	}
//
//	public void setLine(int line) {
//		this.line = line;
//	}
//
//	public String getName() {
//		return name;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}
//
//	public Type getType() {
//		return type;
//	}
//
//	public void setType(Type type) {
//		this.type = type;
//	}
//
//	public void addChild(OcamlModuleDefinition child) {
//		children.add(child);
//		child.setParent(this);
//	}
//
//	public OcamlModuleDefinition[] getChildren() {
//		return children.toArray(new OcamlModuleDefinition[0]);
//	}
//
//	public void debugPrint(int level) {
//		if (type == Type.DefLet)
//			System.err.println(level + " let:");
//		if (type == Type.DefType)
//			System.err.println(level + " type:");
//		if (type == Type.DefModule)
//			System.err.println(level + " module:");
//		if (type == Type.DefException)
//			System.err.println(level + " exception:");
//		if (type == Type.DefExternal)
//			System.err.println(level + " external:");
//		if (type == Type.DefClass)
//			System.err.println(level + " class:");
//		if (type == Type.DefSig)
//			System.err.println(level + " sig:");
//
//		System.err.println("name='" + name + "'");
//		System.err.println("indent=" + indent);
//		System.err.println("line=" + line + "\n");
//
//		for (OcamlModuleDefinition def : children)
//			def.debugPrint(level + 1);
//	}
//
//	/**
//	 * Look for a definition named <code>name</code> in the tree whose root is this node. 
//	 * <p>
//	 * Do a breadth-first search, so as to get the upper-level definitions as possible
//
//	 * @return the last definition before <code>startLine</code>, or <code>null</code> if none 
//	 */
//	public OcamlModuleDefinition find(String name, int startLine) {
//		LinkedList<OcamlModuleDefinition> list = new LinkedList<OcamlModuleDefinition>();
//		ArrayList<OcamlModuleDefinition> found = new ArrayList<OcamlModuleDefinition>();
//		
//		list.addLast(this);
//
//		while (list.size() > 0) {
//			OcamlModuleDefinition def = list.removeFirst();
//
//			if (def.name.equals(name) && def.line < startLine)
//				found.add(def);
//
//			for (OcamlModuleDefinition child : def.children)
//				list.addLast(child);
//		}
//		
//		OcamlModuleDefinition last = null;
//		for(OcamlModuleDefinition def : found)
//		{
//			if(last == null)
//				last = def;
//			else if(def.indent > last.indent)
//				break;
//			else if(def.line > last.line)
//				last = def;
//		}
//
//		return last;
//	}
//
//}
