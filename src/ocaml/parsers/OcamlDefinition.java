package ocaml.parsers;

import java.util.ArrayList;

import ocaml.util.Misc;

/** Represents a definition extracted from an O'Caml interface file (.mli) */
public class OcamlDefinition {
	
	/** The kind of definition (value, exception, class...) */
	public enum Type {
		DefVal, DefType, DefModule, DefException, DefExternal, DefConstructor, DefClass, DefSig
	};
	
	
	/** The kind of definition */
	private Type type;
	/** The name of the variable assigned to this new definition */
	private String name = "";
	/** The signature of the definition */
	private String body = "";
	/** The ocamldoc comment associated with this definition */
	private String comment = "";
	/** The last section comment before this definition (they look like '{6 description}' ) */
	private String sectionComment = "";
	/** the type name for constructors... */
	private String parentName = "";
	
	/** the name of the file in which this definition appears */
	private String filename = "";
	
	/** The list of definitions contained in this definition */
	private ArrayList<OcamlDefinition> children = new ArrayList<OcamlDefinition>();

	public OcamlDefinition(Type type) {
		this.type = type;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = Misc.beautify(clean(body));
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = clean(comment);
	}

	public void appendToComment(String comment) {
		// the end-of-documentation delimiter (we just ignore it)
		if(comment.equals("/*"))
			return;
		
		if (this.comment.equals(""))
			this.comment = comment;
		else 
			this.comment = this.comment + "\n________________________________________\n\n" + comment;

		this.comment = clean(this.comment);
	}

	public Type getType() {
		return type;
	}

	public void setSectionComment(String sectionComment) {
		this.sectionComment = clean(sectionComment);
	}

	public String getSectionComment() {
		return sectionComment;
	}

	public void addChild(OcamlDefinition child) {
		children.add(child);
	}

	public OcamlDefinition[] getChildren() {
		return children.toArray(new OcamlDefinition[0]);
	}

	public void debugPrint(int level) {
		if (type == Type.DefVal)
			System.err.println(level + " val:");
		if (type == Type.DefException)
			System.err.println(level + " exception:");
		if (type == Type.DefExternal)
			System.err.println(level + " external:");
		if (type == Type.DefModule)
			System.err.println(level + " module:");
		if (type == Type.DefType)
			System.err.println(level + " type:");
		if (type == Type.DefConstructor)
			System.err.println(level + " type:");

		System.err.println("name='" + name + "'");
		System.err.println("parentname='" + parentName + "'");
		System.err.println("section='" + sectionComment + "'");
		System.err.println("body='" + body + "'");
		System.err.println("comment='" + comment + "'\n");

		for (OcamlDefinition def : children)
			def.debugPrint(level + 1);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = clean(name);
	}

	public String getParentName() {
		return parentName;
	}

	public void setParentName(String parentName) {
		this.parentName = clean(parentName);
	}
	
	public void setFilename(String file) {
		this.filename = file;
	}
	
	public String getFilename() {
		return filename;
	}
	
	private static String clean(String str) {
		if(str == null)
			return "";
		// remove all redundant spaces
		str =  str.trim().replaceAll("( |\\t)( |\\t)+", " ");
		// remove the heading space
		str = str.replaceAll("\\n ", "\n");
		if(str.endsWith(";;"))
			str = str.substring(0, str.length() - 2);
		
		if(str.endsWith(":") || str.endsWith("="))
			str = str.substring(0, str.length() - 1);

		return str;
	}
}
