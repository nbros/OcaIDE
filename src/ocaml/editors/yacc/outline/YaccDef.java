package ocaml.editors.yacc.outline;

import java.util.ArrayList;

public class YaccDef {
	
	public String name;
	
	public int start, end;
	
	public ArrayList<YaccDef>children;
	
	YaccDef(String name, int start, int end){
		this.name = name;
		this.start = start;
		this.end = end;
		
		children = new ArrayList<YaccDef>();
	}
	
	public void addChild(YaccDef def){
		children.add(def);
	}

}
