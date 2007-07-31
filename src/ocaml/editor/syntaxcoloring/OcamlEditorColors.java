package ocaml.editor.syntaxcoloring;
import ocaml.OcamlPlugin;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/** Store the O'Caml editor colors, so as to minimize memory use */
public class OcamlEditorColors {
	
	private static Color COMMENT_COLOR;
	private static Color DOC_COMMENT_COLOR;
	private static Color DOC_ANNOTATION_COLOR;
	private static Color KEYWORD_COLOR;
	private static Color LETIN_COLOR;
	private static Color FUN_COLOR;
	private static Color CONSTANT_COLOR;
	private static Color STRING_COLOR;
	private static Color INTEGER_COLOR;
	private static Color DECIMAL_COLOR;
	private static Color CHARACTER_COLOR;
	private static Color YACC_DEFINITION_COLOR;
	private static Color PUNCTUATION_COLOR;
	
	private static boolean initialized = false;

	private static void init(){
		Display display = Display.getCurrent();
		
		COMMENT_COLOR = new Color(display, OcamlPlugin.getCommentColor());
		DOC_COMMENT_COLOR = new Color(display, OcamlPlugin.getDocCommentColor());
		DOC_ANNOTATION_COLOR = new Color(display, OcamlPlugin.getDocAnnotationColor());
		KEYWORD_COLOR = new Color(display, OcamlPlugin.getKeywordColor());
		CONSTANT_COLOR = new Color(display, OcamlPlugin.getConstantColor());
		STRING_COLOR = new Color(display, OcamlPlugin.getStringColor());
		INTEGER_COLOR = new Color(display, OcamlPlugin.getIntegerColor());
		DECIMAL_COLOR = new Color(display, OcamlPlugin.getDecimalColor());
		CHARACTER_COLOR = new Color(display, OcamlPlugin.getCharacterColor());
		YACC_DEFINITION_COLOR = new Color(display, OcamlPlugin.getYaccDefinitionColor());
		LETIN_COLOR = new Color(display, OcamlPlugin.getLetInColor());
		FUN_COLOR = new Color(display, OcamlPlugin.getFunColor());
		PUNCTUATION_COLOR = new Color(display, OcamlPlugin.getPunctuationColor());
		
		initialized = true;
	}
	
	/**
	 * Reinitialize the colors. Useful when the user changes the colors for the syntax coloring.
	 */
	public static void reset(){
		init();
	}
	
	public static Color getCommentColor(){
		if(!initialized)
			init();
		return COMMENT_COLOR;
	}
	public static Color getDocCommentColor(){
		if(!initialized)
			init();
		return DOC_COMMENT_COLOR;
	}
	
	public static Color getDocAnnotationColor(){
		if(!initialized)
			init();
		return DOC_ANNOTATION_COLOR;
	}
	
	public static Color getKeywordColor(){
		if(!initialized)
			init();
		return KEYWORD_COLOR;
	}
	
	public static Color getConstantColor(){
		if(!initialized)
			init();
		return CONSTANT_COLOR;
	}
	
	public static Color getStringColor(){
		if(!initialized)
			init();
		return STRING_COLOR;
	}
	
	public static Color getIntegerColor(){
		if(!initialized)
			init();
		return INTEGER_COLOR;
	}
	
	public static Color getDecimalColor(){
		if(!initialized)
			init();
		return DECIMAL_COLOR;
	}
	
	public static Color getYaccDefinitionColor(){
		if(!initialized)
			init();
		return YACC_DEFINITION_COLOR;
	}

	public static Color getCharacterColor(){
		if(!initialized)
			init();
		return CHARACTER_COLOR;
	}

	public static Color getLetInColor() {
		if(!initialized)
			init();
		return LETIN_COLOR;
	}

	public static Color getFunColor() {
		if(!initialized)
			init();
		return FUN_COLOR;
	}

	public static Color getPunctuationColor() {
		if(!initialized)
			init();
		return PUNCTUATION_COLOR;
	}
}