package ocaml.preferences;

/** Constants used by the preference saving system */
public class PreferenceConstants
{
	public static final String P_LIB_PATH = "compilPathLibPreference";
	public static final String P_COMPIL_PATH_OCAML = "ocamlPathPreference";
	public static final String P_COMPIL_PATH_OCAMLC = "ocamlcPathPreference";
	public static final String P_COMPIL_PATH_OCAMLOPT = "ocamloptPathPreference";
	public static final String P_COMPIL_PATH_OCAMLDEP = "ocamldepPathPreference";
	public static final String P_COMPIL_PATH_OCAMLLEX = "ocamllexPathPreference";
	public static final String P_COMPIL_PATH_OCAMLYACC = "ocamlyaccPathPreference";
	public static final String P_COMPIL_PATH_OCAMLDOC = "ocamldocPathPreference";
	public static final String P_COMPIL_PATH_OCAMLDEBUG = "ocamldebugPathPreference";
	public static final String P_MAKE_PATH = "makePathPreference";
	public static final String P_OMAKE_PATH = "omakePathPreference";
	public static final String P_PATH_CAMLP4 = "camlp4PathPreference";
	public static final String P_PATH_OCAMLBUILD = "ocamlbuildPathPreference";
	
	
	public static final String P_COMMENT_COLOR = "commentColor";
	public static final String P_DOC_COMMENT_COLOR = "DocCommentColor";
	public static final String P_DOC_ANNOTATION_COLOR = "DocAnnotationColor";
	public static final String P_STRING_COLOR = "stringColor";
	public static final String P_KEYWORD_COLOR = "keywordColor";
	public static final String P_LETIN_COLOR = "letinColor";
	public static final String P_FUN_COLOR = "funColor";
	public static final String P_CONSTANT_COLOR = "constantColor";
	public static final String P_INTEGER_COLOR = "integerColor";
	public static final String P_DECIMAL_COLOR = "decimalColor";
	public static final String P_CHARACTER_COLOR = "characterColor";
	public static final String P_YACC_DEFINITION_COLOR = "yaccDefinitionColor";
	public static final String P_PUNCTUATION_COLOR = "PunctuationColor";
	public static final String P_UPPERCASE_COLOR = "UppercaseColor";
	public static final String P_POINTED_UPPERCASE_COLOR = "PointedUppercaseColor";
	
	public static final String P_BOLD_KEYWORDS = "boldKeywords";
	public static final String P_BOLD_COMMENTS = "boldComments";
	public static final String P_BOLD_DOCS_COMMENTS = "boldDocumentationComments";
	public static final String P_BOLD_CONSTANTS = "boldConstants";
	public static final String P_BOLD_STRINGS = "boldStrings";
	public static final String P_BOLD_NUMBERS = "boldNumbers";
	public static final String P_BOLD_CHARACTERS = "boldCharacters";
	public static final String P_DONT_SHOW_MISSING_PATHS_WARNING = "DontShowMissingPathsWarning";
	
	public static final String P_EDITOR_DISABLE_AUTOFORMAT = "DisableAutoformat";
	public static final String P_EDITOR_TABS = "TabsWidth";
	public static final String P_EDITOR_SPACES_FOR_TABS = "SpacesForTabs";
	public static final String P_EDITOR_CONTINUE_COMMENTS = "ContinueComments";
	public static final String P_EDITOR_PIPE_AFTER_TYPE = "PipeAfterType";
	public static final String P_EDITOR_PIPE_AFTER_WITH = "PipeAfterWith";
	public static final String P_EDITOR_PIPE_AFTER_FUN = "PipeAfterFun";
	public static final String P_EDITOR_AUTO_INDENT_CONT = "AutoIndentCont";
	public static final String P_EDITOR_KEEP_INDENT = "KeepIndent";
	public static final String P_EDITOR_REMOVE_PIPE = "RemovePipe";
	public static final String P_EDITOR_CONTINUE_PIPES = "ContinuePipes";
	public static final String P_EDITOR_INDENT_IN = "IndentIn";
	public static final String P_EDITOR_INDENT_WITH = "IndentWith";
	public static final String P_EDITOR_DEDENT_SEMI_SEMI = "DedentSemiSemi";
	public static final String P_EDITOR_INTELLIGENT_INDENT_START = "IntelligentIndentStart";
	public static final String P_EDITOR_COLON_COLON_TAB = "ColonColonTab";
	public static final String P_EDITOR_FN_TAB = "FnTab";
	public static final String P_EDITOR_TAB_ARROW = "TabArrow";
	public static final String P_EDITOR_DOUBLEQUOTES = "DoubleQuotes";
	
	public static final String P_DISABLE_UNICODE_CHARS = "DisableUnicodeChars";
	public static final String P_SHOW_TYPES_IN_OUTLINE = "ShowTypesInOutline";
	public static final String P_SHOW_TYPES_IN_STATUS_BAR = "ShowTypesInStatusBar";
	public static final String P_SHOW_MARKERS_IN_STATUS_BAR = "ShowMarkersInStatusBar";
	public static final String P_SHOW_TYPES_IN_POPUPS = "ShowTypesInPopups";
	
	public static final String P_FORMATTER_INDENT_IN = "FormatterIndentIn";
	public static final String P_FORMATTER_COMMENT_WIDTH = "FormatterCommentWidth";
	public static final String P_FORMATTER_MAX_BLANK_LINES = "FormatterMaxBlankLines";
	public static final String P_FORMATTER_FORMAT_COMMENTS = "FormatterFormatComments";
	public static final String P_FORMATTER_INDENT_IN_LETS = "FormatterIndentInLets";
	
	public static final String P_FORMATTER_BEGIN = "FormatterBegin";
	public static final String P_FORMATTER_STRUCT = "FormatterStruct";
	public static final String P_FORMATTER_SIG = "FormatterSig";
	public static final String P_FORMATTER_IN = "FormatterIn";
	public static final String P_FORMATTER_DEF =  "FormatterDef";
	public static final String P_FORMATTER_FOR =  "FormatterFor";
	public static final String P_FORMATTER_THEN =  "FormatterThen";
	public static final String P_FORMATTER_ELSE =  "FormatterElse";
	public static final String P_FORMATTER_WHILE =  "FormatterWhile";
	public static final String P_FORMATTER_MATCH_ACTION =  "FormatterMatchAction";
	public static final String P_FORMATTER_FIRST_MATCH_CASE =  "FormatterFirstMatchCase";
	public static final String P_FORMATTER_FUNCTOR =  "FormatterFunctor";
	public static final String P_FORMATTER_TRY =  "FormatterTry";
	public static final String P_FORMATTER_WITH =  "FormatterWith";
	public static final String P_FORMATTER_OBJECT =  "FormatterObject";
	public static final String P_FORMATTER_APPLICATION =  "FormatterApplication";
	public static final String P_FORMATTER_RECORD =  "FormatterRecord";
	public static final String P_FORMATTER_FIRST_CONSTRUCTOR =  "FormatterFirstConstructor";
	public static final String P_FORMATTER_PAREN =  "FormatterParen";
	public static final String P_FORMATTER_FIRST_CATCH =  "FormatterFirstCatch";
	public static final String P_FORMATTER_FUN_ARGS =  "FormatterFunArgs";
	public static final String P_FORMATTER_MODULE_CONSTRAINT =  "FormatterModuleConstraint";

	public static final String P_FORMATTER_INDENT_LET_IN = "FormatterIndentLetIn";
	
	
	public static final String P_OUTLINE_SHOW_LET = "OutlineShowLet";
	public static final String P_OUTLINE_SHOW_LET_IN = "OutlineShowLetIn";
	public static final String P_OUTLINE_SHOW_TYPE = "OutlineShowType";
	public static final String P_OUTLINE_SHOW_MODULE = "OutlineShowModule";
	public static final String P_OUTLINE_SHOW_MODULE_TYPE = "OutlineShowModuleType";
	public static final String P_OUTLINE_SHOW_EXCEPTION = "OutlineShowException";
	public static final String P_OUTLINE_SHOW_EXTERNAL = "OutlineShowExternal";
	public static final String P_OUTLINE_SHOW_CLASS = "OutlineShowClass";
	public static final String P_OUTLINE_SHOW_OPEN = "OutlineShowOpen";
	public static final String P_OUTLINE_SHOW_METHOD = "OutlineShowMethod";
	public static final String P_OUTLINE_SHOW_INCLUDE = "OutlineShowInclude";
	public static final String P_OUTLINE_SHOW_VAL = "OutlineShowVal";
	public static final String P_OUTLINE_SHOW_INITIALIZER = "OutlineShowInitializer";
	public static final String P_OUTLINE_SHOW_CLASSTYPE = "OutlineShowClassType";
	public static final String P_OUTLINE_SHOW_VARIANT_CONS = "OutlineShowVariantCons";
	public static final String P_OUTLINE_SHOW_RECORD_CONS = "OutlineShowRecordCons";

	/** whether the outline must be sorted alphabetically */
	public static final String P_OUTLINE_SORT = "OutlineSort";
	
	public static final String P_OUTLINE_LET_MINIMUM_CHARS = "OutlineLetMinChars";
	public static final String P_OUTLINE_LET_IN_MINIMUM_CHARS = "OutlineLetInMinChars";
	
	public static final String P_OUTLINE_EXPAND_MODULES = "OutlineExpandModules";
	public static final String P_OUTLINE_EXPAND_CLASSES = "OutlineExpandClasses";
	public static final String P_OUTLINE_EXPAND_ALL = "OutlineExpandAll";
	
	public static final String P_OUTLINE_UNNEST_IN = "OutlineUnnestIn";
	public static final String P_OUTLINE_AND_BLUE = "OutlineAndBlue";

	public static final String P_DEBUGGER_CHECKPOINTS = "Checkpoints";
	public static final String P_DEBUGGER_SMALL_STEP = "SmallStep";
	public static final String P_DEBUGGER_BIG_STEP = "BigStep";
	public static final String P_DEBUGGER_PROCESS_COUNT = "ProcessCount";
	
	/** debug mode for the outline */
	public static final String P_OUTLINE_DEBUG_MODE = "OutlineDebugMode";
	
	/** The paths encoded as a list of strings separated by newlines */
	public static final String P_BROWSER_PATHS = "BrowserPaths";
	
	/** Activate automatic completion after '.' or '@'?*/
	public static final String P_EDITOR_AUTOCOMPLETION = "AutoCompletion";
	
	/** Spell check comments? */
	public static final String P_EDITOR_SPELL_CHECKING = "SpellChecking";
	
	/** Override default ocamlbuild paths by user paths */
	public static final String P_OCAMLBUILD_COMPIL_PATHS_OVERRIDE = "OcamlbuildPathsOverride";
	
	
}
