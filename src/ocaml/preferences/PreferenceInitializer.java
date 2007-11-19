package ocaml.preferences;

import java.io.File;

import ocaml.OcamlPlugin;
import ocaml.exec.CommandRunner;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/** Initializes preferences with default values */
	@Override
	public void initializeDefaultPreferences() {
		try {

			IPreferenceStore store = OcamlPlugin.getInstance().getPreferenceStore();

			// set the default syntax coloring colors in the preference register
			store.setDefault(PreferenceConstants.P_COMMENT_COLOR, "63,127,95");
			store.setDefault(PreferenceConstants.P_STRING_COLOR, "42,0,255");
			store.setDefault(PreferenceConstants.P_KEYWORD_COLOR, "127,0,85");
			store.setDefault(PreferenceConstants.P_LETIN_COLOR, "127,0,85");
			store.setDefault(PreferenceConstants.P_FUN_COLOR, "127,0,85");
			store.setDefault(PreferenceConstants.P_CONSTANT_COLOR, "150,0,50");
			store.setDefault(PreferenceConstants.P_INTEGER_COLOR, "0,128,128");
			store.setDefault(PreferenceConstants.P_DECIMAL_COLOR, "192,64,0");
			store.setDefault(PreferenceConstants.P_CHARACTER_COLOR, "255,128,0");
			store.setDefault(PreferenceConstants.P_DOC_COMMENT_COLOR, "63,95,191");
			store.setDefault(PreferenceConstants.P_DOC_ANNOTATION_COLOR, "127,159,191");
			store.setDefault(PreferenceConstants.P_YACC_DEFINITION_COLOR, "191,63,63");
			store.setDefault(PreferenceConstants.P_PUNCTUATION_COLOR, "128,0,0");

			// set the default syntax coloring bold attributes
			store.setDefault(PreferenceConstants.P_BOLD_CHARACTERS, false);
			store.setDefault(PreferenceConstants.P_BOLD_COMMENTS, false);
			store.setDefault(PreferenceConstants.P_BOLD_CONSTANTS, false);
			store.setDefault(PreferenceConstants.P_BOLD_KEYWORDS, true);
			store.setDefault(PreferenceConstants.P_BOLD_NUMBERS, false);
			store.setDefault(PreferenceConstants.P_BOLD_STRINGS, false);

			// set the defaults for the editor
			store.setDefault(PreferenceConstants.P_EDITOR_AUTOCOMPLETION, true);
			store.setDefault(PreferenceConstants.P_EDITOR_DISABLE_AUTOFORMAT, false);
			store.setDefault(PreferenceConstants.P_EDITOR_TABS, 2);
			store.setDefault(PreferenceConstants.P_EDITOR_CONTINUE_COMMENTS, true);
			store.setDefault(PreferenceConstants.P_EDITOR_PIPE_AFTER_TYPE, true);
			store.setDefault(PreferenceConstants.P_EDITOR_PIPE_AFTER_WITH, true);
			store.setDefault(PreferenceConstants.P_EDITOR_PIPE_AFTER_FUN, true);
			store.setDefault(PreferenceConstants.P_EDITOR_AUTO_INDENT_CONT, true);
			store.setDefault(PreferenceConstants.P_EDITOR_KEEP_INDENT, true);
			store.setDefault(PreferenceConstants.P_EDITOR_REMOVE_PIPE, true);
			store.setDefault(PreferenceConstants.P_EDITOR_CONTINUE_PIPES, true);
			store.setDefault(PreferenceConstants.P_EDITOR_INDENT_IN, false);
			store.setDefault(PreferenceConstants.P_EDITOR_DEDENT_SEMI_SEMI, true);
			store.setDefault(PreferenceConstants.P_EDITOR_INTELLIGENT_INDENT_START, true);
			store.setDefault(PreferenceConstants.P_EDITOR_COLON_COLON_TAB, true);
			store.setDefault(PreferenceConstants.P_EDITOR_FN_TAB, true);
			store.setDefault(PreferenceConstants.P_EDITOR_TAB_ARROW, true);
			store.setDefault(PreferenceConstants.P_EDITOR_DOUBLEQUOTES, true);

			String os = Platform.getOS();
			boolean windows = os.equals(Platform.OS_WIN32);
			store.setDefault(PreferenceConstants.P_DISABLE_UNICODE_CHARS, windows);

			store.setDefault(PreferenceConstants.P_SHOW_TYPES_IN_OUTLINE, true);
			store.setDefault(PreferenceConstants.P_SHOW_TYPES_IN_POPUPS, true);
			store.setDefault(PreferenceConstants.P_SHOW_TYPES_IN_STATUS_BAR, true);

			// set the defaults for the formatter
			store.setDefault(PreferenceConstants.P_FORMATTER_INDENT_IN, false);
			store.setDefault(PreferenceConstants.P_FORMATTER_INDENT_IN_LETS, false);
			store.setDefault(PreferenceConstants.P_FORMATTER_COMMENT_WIDTH, 78);
			store.setDefault(PreferenceConstants.P_FORMATTER_MAX_BLANK_LINES, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FORMAT_COMMENTS, true);
			
			store.setDefault(PreferenceConstants.P_FORMATTER_INDENT_LET_IN, false);
			store.setDefault(PreferenceConstants.P_FORMATTER_APPLICATION, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_BEGIN, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_DEF, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_ELSE, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FIRST_CATCH, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FIRST_CONSTRUCTOR, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FIRST_MATCH_CASE, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FOR, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_FUN_ARGS, 2);
			store.setDefault(PreferenceConstants.P_FORMATTER_FUNCTOR, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_IN, 0);
			store.setDefault(PreferenceConstants.P_FORMATTER_MATCH_ACTION, 2);
			store.setDefault(PreferenceConstants.P_FORMATTER_MODULE_CONSTRAINT, 2);
			store.setDefault(PreferenceConstants.P_FORMATTER_OBJECT, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_PAREN, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_RECORD, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_SIG, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_STRUCT, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_THEN, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_TRY, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_WHILE, 1);
			store.setDefault(PreferenceConstants.P_FORMATTER_WITH, 0);

			// set the default for the outline
			store.setDefault(PreferenceConstants.P_OUTLINE_LET_MINIMUM_CHARS, 0);
			store.setDefault(PreferenceConstants.P_OUTLINE_LET_IN_MINIMUM_CHARS, 0);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_CLASS, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_CLASSTYPE, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_EXCEPTION, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_EXTERNAL, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_INCLUDE, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_INITIALIZER, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_LET, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_LET_IN, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_METHOD, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_MODULE, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_MODULE_TYPE, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_OPEN, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_RECORD_CONS, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_TYPE, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_VAL, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_SHOW_VARIANT_CONS, true);
			
			store.setDefault(PreferenceConstants.P_OUTLINE_EXPAND_ALL, false);
			store.setDefault(PreferenceConstants.P_OUTLINE_EXPAND_MODULES, false);
			store.setDefault(PreferenceConstants.P_OUTLINE_EXPAND_CLASSES, false);
			
			store.setDefault(PreferenceConstants.P_OUTLINE_UNNEST_IN, true);
			store.setDefault(PreferenceConstants.P_OUTLINE_AND_BLUE, true);

			
			// set the defaults for the debugger
			store.setDefault(PreferenceConstants.P_DEBUGGER_CHECKPOINTS, true);
			store.setDefault(PreferenceConstants.P_DEBUGGER_SMALL_STEP, 1000);
			store.setDefault(PreferenceConstants.P_DEBUGGER_BIG_STEP, 10000);
			store.setDefault(PreferenceConstants.P_DEBUGGER_PROCESS_COUNT, 15);

			String ocamlLibPath = "";
			String ocaml = "";
			String ocamlc = "";
			String ocamlopt = "";
			String ocamldep = "";
			String ocamllex = "";
			String ocamlyacc = "";
			String ocamldoc = "";
			String ocamldebug = "";
			String camlp4 = "";
			String ocamlbuild = "";
			String make = "";

			String which = "";

			/*
			 * If the operating system is Linux, then we search in the most common install
			 * directories. Then, if we couldn't find the executables this way, we use the "which"
			 * command (if it is available)
			 */
			if (OcamlPlugin.runningOnLinuxCompatibleSystem()) {

				final String[] commonPrefixes = new String[] { "/bin", "/usr/bin", "/usr/local/bin" };

				// search in the most common install directories
				File file;
				for (String prefix : commonPrefixes) {
					if (ocaml.equals("")) {
						file = new File(prefix + "/ocaml");
						if (file.exists() && file.isFile())
							ocaml = file.getPath();
					}

					if (ocamlc.equals("")) {
						file = new File(prefix + "/ocamlc.opt");
						if (file.exists() && file.isFile())
							ocamlc = file.getPath();
					}

					if (ocamlc.equals("")) {
						file = new File(prefix + "/ocamlc");
						if (file.exists() && file.isFile())
							ocamlc = file.getPath();
					}

					if (ocamlopt.equals("")) {
						file = new File(prefix + "/ocamlopt.opt");
						if (file.exists() && file.isFile())
							ocamlopt = file.getPath();
					}

					if (ocamlopt.equals("")) {
						file = new File(prefix + "/ocamlopt");
						if (file.exists() && file.isFile())
							ocamlopt = file.getPath();
					}

					if (ocamldep.equals("")) {
						file = new File(prefix + "/ocamldep.opt");
						if (file.exists() && file.isFile())
							ocamldep = file.getPath();
					}

					if (ocamldep.equals("")) {
						file = new File(prefix + "/ocamldep");
						if (file.exists() && file.isFile())
							ocamldep = file.getPath();
					}

					if (ocamllex.equals("")) {
						file = new File(prefix + "/ocamllex.opt");
						if (file.exists() && file.isFile())
							ocamllex = file.getPath();
					}

					if (ocamllex.equals("")) {
						file = new File(prefix + "/ocamllex");
						if (file.exists() && file.isFile())
							ocamllex = file.getPath();
					}

					if (ocamlyacc.equals("")) {
						file = new File(prefix + "/ocamlyacc");
						if (file.exists() && file.isFile())
							ocamlyacc = file.getPath();
					}

					if (ocamldoc.equals("")) {
						file = new File(prefix + "/ocamldoc.opt");
						if (file.exists() && file.isFile())
							ocamldoc = file.getPath();
					}

					if (ocamldoc.equals("")) {
						file = new File(prefix + "/ocamldoc");
						if (file.exists() && file.isFile())
							ocamldoc = file.getPath();
					}

					if (ocamldebug.equals("")) {
						file = new File(prefix + "/ocamldebug");
						if (file.exists() && file.isFile())
							ocamldebug = file.getPath();
					}

					if (camlp4.equals("")) {
						file = new File(prefix + "/camlp4");
						if (file.exists() && file.isFile())
							camlp4 = file.getPath();
					}

					if (ocamlbuild.equals("")) {
						file = new File(prefix + "/ocamlbuild");
						if (file.exists() && file.isFile())
							ocamlbuild = file.getPath();
					}

					if (make.equals("")) {
						file = new File(prefix + "/make");
						if (file.exists() && file.isFile())
							make = file.getPath();
					}

					if (which.equals("")) {
						file = new File(prefix + "/which");
						if (file.exists() && file.isFile())
							which = file.getPath();
					}
				}

				/*
				 * java.io.File file = new java.io.File("/usr/lib/ocaml"); if (!file.exists() ||
				 * !file.isDirectory()) { file = new java.io.File("/lib/ocaml"); if (!file.exists() ||
				 * !file.isDirectory()) file = new java.io.File("/usr/local/lib/ocaml"); }
				 * 
				 * if (file.exists() && file.isDirectory()) { for (String s : file.list()) {
				 * java.io.File dir = new java.io.File(file.getAbsolutePath() + "/" + s); if
				 * (dir.isDirectory()) { for (String s2 : dir.list()) { java.io.File f = new
				 * java.io.File(s2); if (f.getName().equals("pervasives.mli")) { try { ocamlLibPath =
				 * dir.getCanonicalPath(); } catch (IOException e) { ocamlLibPath = ""; } } } } } }
				 */

				CommandRunner commandRunner;

				if (!which.equals("")) {

					if (ocamlc.equals("")) {
						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamlc.opt" },
									"/");
							ocamlc = commandRunner.getStdout().trim();
							if (ocamlc.equals("")) {
								commandRunner = new CommandRunner(new String[] { which, "ocamlc" },
										"/");
								ocamlc = commandRunner.getStdout().trim();
							}
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamlopt.equals("")) {
						try {
							commandRunner = new CommandRunner(
									new String[] { which, "ocamlopt.opt" }, "/");
							ocamlopt = commandRunner.getStdout().trim();
							if (ocamlopt.equals("")) {
								commandRunner = new CommandRunner(
										new String[] { which, "ocamlopt" }, "/");
								ocamlopt = commandRunner.getStdout().trim();
							}
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocaml.equals("")) {
						try {
							commandRunner = new CommandRunner(new String[] { which, "ocaml" }, "/");
							ocaml = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamldep.equals("")) {
						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamldep" },
									"/");
							ocamldep = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamllex.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamllex" },
									"/");
							ocamllex = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamlyacc.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamlyacc" },
									"/");
							ocamlyacc = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamldoc.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamldoc" },
									"/");
							ocamldoc = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamldebug.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamldebug" },
									"/");
							ocamldebug = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (ocamlbuild.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "ocamlbuild" },
									"/");
							ocamlbuild = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (camlp4.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "camlp4" }, "/");
							camlp4 = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}
					if (make.equals("")) {

						try {
							commandRunner = new CommandRunner(new String[] { which, "make" }, "/");
							make = commandRunner.getStdout().trim();
						} catch (Exception e) {
							OcamlPlugin.logError("ocaml plugin error", e);
						}
					}

				}

				if (!ocamlc.equals("")) {
					try {
						commandRunner = new CommandRunner(new String[] { ocamlc, "-where" }, "/");
						ocamlLibPath = commandRunner.getStdout().trim();
					} catch (Throwable e) {
						OcamlPlugin.logError("ocaml plugin error", e);
					}
				}
			}
			// on Windows:
			else if (Platform.getOS().equals(Platform.OS_WIN32)) {
				/*
				 * Since we can't access the register, we take the O'Caml installer's default
				 * install directory
				 */

				String basepath = "C:\\Program Files\\Objective Caml";

				File file = new File(basepath);
				if (file.exists() && file.isDirectory()) {
					ocamlLibPath = basepath + "\\lib";
					ocaml = basepath + "\\bin\\ocaml.exe";
					ocamlc = basepath + "\\bin\\ocamlc.exe";
					ocamlopt = basepath + "\\bin\\ocamlopt.exe";
					ocamldep = basepath + "\\bin\\ocamldep.exe";
					ocamllex = basepath + "\\bin\\ocamllex.exe";
					ocamlyacc = basepath + "\\bin\\ocamlyacc.exe";
					ocamldoc = basepath + "\\bin\\ocamldoc.exe";
					camlp4 = basepath + "\\bin\\camlp4.exe";
					ocamlbuild = basepath + "\\bin\\ocamlbuild.exe";
					// configure ocamldebug manually under Windows (with cygwin)
				}
			}

			// Save all the preferences in the preferences register
			store.setDefault(PreferenceConstants.P_LIB_PATH, ocamlLibPath);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAML, ocaml);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLC, ocamlc);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLOPT, ocamlopt);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLDEP, ocamldep);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLLEX, ocamllex);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLYACC, ocamlyacc);
			store.setDefault(PreferenceConstants.P_PATH_CAMLP4, camlp4);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLDOC, ocamldoc);
			store.setDefault(PreferenceConstants.P_COMPIL_PATH_OCAMLDEBUG, ocamldebug);
			store.setDefault(PreferenceConstants.P_PATH_OCAMLBUILD, ocamlbuild);
			store.setDefault(PreferenceConstants.P_MAKE_PATH, make);

		} catch (Throwable e) {
			OcamlPlugin.logError("error in preference initializer", e);
		}
	}
}
