/*
Copyright Nicolas Bros, Rafael Cerioli, Guillaume Curat, 
Leonard Dallot, Alexandre Deckner, Nicolas Deckner, 
Sylvain Le Ligné, Alexandre Serra, Guillaume Viel

This software is a computer program whose purpose is to provide an 
integrated development environment for the OCaml language.

This software is governed by the CeCILL-B license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-B
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-B license and that you accept its terms.
 */

package ocaml;

import java.io.File;
import java.net.URL;

import ocaml.debugging.OcamlDebugger;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;
import ocaml.editors.lex.OcamllexPartitionScanner;
import ocaml.editors.yacc.OcamlyaccPartitionScanner;
import ocaml.preferences.PreferenceConstants;
import ocaml.util.GeneratedResourcesHandler;
import ocaml.views.outline.OutlineBuildListener;
import ocaml.views.toplevel.OcamlToplevelView;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.DataFormatException;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plug-in activator, the singleton instance of the O'Caml plug-in. This class is loaded only once by Eclipse
 * when one of the features of the plug-in is used.
 */
public class OcamlPlugin extends AbstractUIPlugin {

	/** newline */
	public static String newline = System.getProperty("line.separator");

	/** The singleton instance of the plug-in. */
	private static OcamlPlugin instance = null;

	/** The directory in which the plug-in was started. */
	private final String pluginDirectory;

	// public static final String PLUGIN_ID = "ocaml";

	/**
	 * We use this "Qualifier" to create "QualifiedName"s for this plug-in. (value="ocaml")
	 */
	public final static String QUALIFIER = "ocaml";

	/**
	 * This constructor is called only once by Eclipse when one of the features of the plug-in is used for the
	 * first time.
	 */
	public OcamlPlugin() {
		super();
		if (instance != null)
			logError("OcamlPlugin:OcamlPlugin() : The plug-in is already loaded!");
		instance = this;

		File f = new File("");
		this.pluginDirectory = f.getAbsolutePath();
	}

	private OcamlPartitionScanner partitionScanner = null;

	public OcamlPartitionScanner getOcamlPartitionScanner() {
		if (partitionScanner == null)
			partitionScanner = new OcamlPartitionScanner();
		return partitionScanner;
	}

	private OcamllexPartitionScanner lexPartitionScanner = null;

	public OcamllexPartitionScanner getOcamllexPartitionScanner() {
		if (lexPartitionScanner == null)
			lexPartitionScanner = new OcamllexPartitionScanner();
		return lexPartitionScanner;
	}

	private OcamlyaccPartitionScanner yaccPartitionScanner = null;

	public OcamlyaccPartitionScanner getOcamlyaccPartitionScanner() {
		if (yaccPartitionScanner == null)
			yaccPartitionScanner = new OcamlyaccPartitionScanner();
		return yaccPartitionScanner;
	}

	/** To display the error message only once if the paths are not correctly set. */
	private boolean bShowMessage = true;

	/**
	 * Check that the paths are correctly set, and if this is not the case, then display an error message
	 * asking the user to configure them.
	 */
	public void checkPaths() {

		if (!bShowMessage)
			return;

		String missing = "";

		File file = new File(getOcamlcFullPath());
		if (!file.exists())
			missing = missing + "ocamlc (ocaml compiler)" + newline;

		file = new File(getLibFullPath());
		if (!file.exists())
			missing = missing + "ocaml library directory" + newline;

		file = new File(getOcamlFullPath());
		if (!file.exists())
			missing = missing + "ocaml (toplevel)" + newline;

		file = new File(getOcamldepFullPath());
		if (!file.exists())
			missing = missing + "ocamldep (ocaml dependencies generator)" + newline;

		file = new File(getOcamloptFullPath());
		if (!file.exists())
			missing = missing + "ocamlopt (ocaml native code compiler)" + newline;

		file = new File(getOcamllexFullPath());
		if (!file.exists())
			missing = missing + "ocamllex (ocaml lexer generator)" + newline;

		file = new File(getOcamlyaccFullPath());
		if (!file.exists())
			missing = missing + "ocamlyacc (ocaml parser generator)" + newline;

		file = new File(getOcamldocFullPath());
		if (!file.exists())
			missing = missing + "ocamldoc (ocaml documentation generator)" + newline;

		file = new File(getCamlp4FullPath());
		if (!file.exists())
			missing = missing + "camlp4 (ocaml preprocessor-pretty printer)" + newline;

		if (!missing.equals("")) {

			String message = "The following paths are not correctly set:" + newline + missing + newline
					+ "Please set them in Window>Preferences>Ocaml>Paths.";

			boolean bDoNotShowMessage = getPreferenceStore().getBoolean(
					PreferenceConstants.P_DONT_SHOW_MISSING_PATHS_WARNING);
			if (!bDoNotShowMessage) {
				MessageDialog.openInformation(null, "Ocaml Plugin", message);
				getPreferenceStore().setValue(PreferenceConstants.P_DONT_SHOW_MISSING_PATHS_WARNING, true);
			}

			OcamlPlugin.logWarning(message);

			bShowMessage = false;
		}
	}

	/** Log an informative message (in Eclipse log) */
	public static void logInfo(String msg) {
		instance.getLog().log(new Status(IStatus.INFO, "ocaml", 0, msg, null));
	}

	/** Log a warning message (in Eclipse log) */
	public static void logWarning(String msg) {
		instance.getLog().log(new Status(IStatus.WARNING, "ocaml", 0, msg + getPosition(), null));
	}

	/** Log an error message (in Eclipse log) */
	public static void logError(String msg) {
		instance.getLog().log(new Status(IStatus.ERROR, "ocaml", 0, msg + getPosition(), null));
	}

	/**
	 * Log an error message in Eclipse log.
	 * 
	 * @param exception
	 *            The exception that triggered this error message.
	 */
	public static void logError(String msg, Throwable exception) {
		instance.getLog().log(new Status(IStatus.ERROR, "ocaml", 0, msg, exception));
	}

	/**
	 * @param exception
	 *            The exception to log.
	 */
	public static void logError(Throwable e) {
		instance.getLog().log(new Status(IStatus.ERROR, "ocaml", 0, e.getMessage(), e));
	}
	
	private static String getPosition() {
		try {
			// get the stack element corresponding to the caller of the log method
			StackTraceElement element = new Exception().getStackTrace()[2];
			return " \n[" + element.getClassName() + "#" + element.getMethodName() + " : "
					+ element.getLineNumber() + "]";
		} catch (Throwable e) {
			return "";
		}
	}


	/**
	 * Returns the plug-in instance.
	 */
	public static OcamlPlugin getInstance() {
		return instance;
	}

	/** Returns the O'Caml plug-in install directory URL */
	public static URL getInstallURL() {
		return instance.getBundle().getEntry("/");
	}

	/*
	 * / ** Retourne le chemin absolu du répertoire du plugin ocamlCompiler * / public static String
	 * getCompilerPath() { return getPluginDirectory(); }
	 */

	/** Returns ocamllex absolute path */
	public static String getOcamllexFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLLEX).trim();
	}

	/** Returns ocamlyacc absolute path */
	public static String getOcamlyaccFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLYACC).trim();
	}

	/** Returns ocamlopt absolute path */
	public static String getOcamloptFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLOPT).trim();
	}

	/** Returns ocaml toplevel absolute path */
	public static String getOcamlFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAML).trim();
	}

	/** Returns ocamlc absolute path */
	public static String getOcamlcFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLC).trim();
	}

	/** Returns ocamldep absolute path */
	public static String getOcamldepFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLDEP).trim();
	}

	/** Returns ocamldoc absolute path */
	public static String getOcamldocFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLDOC).trim();
	}

	/** Returns ocamldebug absolute path */
	public static String getOcamldebugFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_COMPIL_PATH_OCAMLDEBUG).trim();
	}

	/** Returns ocamlbuild absolute path */
	public static String getOcamlbuildFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_PATH_OCAMLBUILD).trim();

	}

	/** Returns camlp4 absolute path */
	public static String getCamlp4FullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_PATH_CAMLP4).trim();
	}

	/** Returns the make command absolute path */
	public static String getMakeFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_MAKE_PATH).trim();
	}

	/** Returns the omake command absolute path */
	public static String getOMakeFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_OMAKE_PATH).trim();
	}

	/** Returns the ocaml library absolute path */
	public static String getLibFullPath() {
		return instance.getPreferenceStore().getString(PreferenceConstants.P_LIB_PATH).trim();
	}

	/** Returns the comments color from the preferences */
	public static RGB getCommentColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_COMMENT_COLOR));
	}

	/** Returns the documentation comments color from the preferences */
	public static RGB getDocCommentColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_DOC_COMMENT_COLOR));
	}

	/** Returns the color of the annotations in ocamldoc comments from the preferences */
	public static RGB getDocAnnotationColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_DOC_ANNOTATION_COLOR));
	}

	/** Returns the constants color from the preferences */
	public static RGB getConstantColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_CONSTANT_COLOR));
	}

	/** Returns the keywords color from the preferences */
	public static RGB getKeywordColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_KEYWORD_COLOR));
	}

	/** Returns the 'let' and 'in' keywords color from the preferences */
	public static RGB getLetInColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_LETIN_COLOR));
	}

	/** Returns the 'fun' and 'function' keywords color from the preferences */
	public static RGB getFunColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_FUN_COLOR));
	}

	/** Returns the strings color from the preferences */
	public static RGB getStringColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_STRING_COLOR));
	}

	/** Returns the integers color from the preferences */
	public static RGB getIntegerColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_INTEGER_COLOR));
	}

	/** Returns the floats color from the preferences */
	public static RGB getDecimalColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_DECIMAL_COLOR));
	}

	/** Returns the characters color from the preferences */
	public static RGB getCharacterColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_CHARACTER_COLOR));
	}

	/** Returns the yacc definitions color from the preferences */
	public static RGB getYaccDefinitionColor() {
		return string2RGB(instance.getPreferenceStore()
				.getString(PreferenceConstants.P_YACC_DEFINITION_COLOR));
	}

	public static RGB getPunctuationColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_PUNCTUATION_COLOR));
	}

	public static RGB getUppercaseColor() {
		return string2RGB(instance.getPreferenceStore().getString(PreferenceConstants.P_UPPERCASE_COLOR));
	}

	public static RGB getPointedUppercaseColor() {
		return string2RGB(instance.getPreferenceStore().getString(
				PreferenceConstants.P_POINTED_UPPERCASE_COLOR));
	}

	/** Returns whether comments should appear in bold (from the user preferences) */
	public static boolean getCommentIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_COMMENTS);
	}

	/** Returns whether constants should appear in bold (from the user preferences) */
	public static boolean getConstantIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_CONSTANTS);
	}

	/** Returns whether keywords should appear in bold (from the user preferences) */
	public static boolean getKeywordIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_KEYWORDS);
	}

	/** Returns whether strings should appear in bold (from the user preferences) */
	public static boolean getStringIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_STRINGS);
	}

	/** Returns whether numbers should appear in bold (from the user preferences) */
	public static boolean getNumberIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_NUMBERS);
	}

	/** Returns whether characters should appear in bold (from the user preferences) */
	public static boolean getCharacterIsBold() {
		return instance.getPreferenceStore().getBoolean(PreferenceConstants.P_BOLD_CHARACTERS);
	}

	/**
	 * Converts a string from "r,g,b" format to a RGB instance
	 */
	private static RGB string2RGB(String rgb) {
		RGB color = null;
		try {
			color = StringConverter.asRGB(rgb);
		} catch (DataFormatException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return new RGB(255, 0, 0);
		}

		return color;
	}

	/** Returns the plug-in starting directory */
	public static String getPluginDirectory() {
		return instance.pluginDirectory;
	}

	/** @return true if the system is compatible with linux (that is: Linux or Mac Os X) */
	public static boolean runningOnLinuxCompatibleSystem() {
		String os = Platform.getOS();
		return !(os.equals(Platform.OS_WIN32));
	}

	/** Instance of the last focused top-level view. This is used to evaluate expressions in the top-level. */
	private static OcamlToplevelView lastFocusedToplevelInstance = null;

	public static OcamlToplevelView getLastFocusedToplevelInstance() {
		return lastFocusedToplevelInstance;
	}

	public static void setLastFocusedToplevelInstance(OcamlToplevelView lastFocusedToplevelInstance) {
		OcamlPlugin.lastFocusedToplevelInstance = lastFocusedToplevelInstance;
	}

	/**
	 * Called when the plug-in is first used.<br>
	 * Register a listener to manage automatically generated files.<br>
	 * This listener must add the IS_GEN property to any file generated during compilation, and must remove it
	 * from any file manually modified by the user.
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();

		// add a listener to be notified of resource changes
		this.registeredListener = new GeneratedResourcesHandler();
		workspace.addResourceChangeListener(registeredListener, IResourceChangeEvent.PRE_BUILD
				| IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.POST_BUILD
				| IResourceChangeEvent.PRE_CLOSE);

		// add a second listener to know when we can rebuild the outline after a project build
		this.outlineBuildListener = new OutlineBuildListener();
		workspace.addResourceChangeListener(outlineBuildListener, IResourceChangeEvent.POST_BUILD);
		
		this.folderChangeListener = new FolderChangeListener();
		   workspace.addResourceChangeListener(this.folderChangeListener,
		      IResourceChangeEvent.POST_CHANGE);

	}

	/** The registered listeners. Used to remove it afterwards. */
	private IResourceChangeListener registeredListener;

	private IResourceChangeListener outlineBuildListener;
	
	private IResourceChangeListener folderChangeListener;

	/**
	 * Called when the plug-in is stopping.<br>
	 * Remove the listener added in the <code>start</code> method.
	 */
	@Override
	public void stop(BundleContext context) throws Exception {

		super.stop(context);
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		// Remove the previously listener
		workspace.removeResourceChangeListener(registeredListener);
		workspace.removeResourceChangeListener(outlineBuildListener);
		workspace.removeResourceChangeListener(folderChangeListener);

		// stop the ocamldebug process if it is started
		OcamlDebugger debugger = OcamlDebugger.getInstance();
		if (debugger.isStarted()) {
			debugger.quit();
			int timeout = 50;
			while (debugger.isStarted() && timeout > 0) {
				Thread.sleep(100);
				timeout--;
			}
		}
		// if it won't stop after a delay, then kill it
		if (debugger.isStarted())
			debugger.kill();

	}

	/*private void hideCustomToplevelViews() {
		try {
			final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
			IViewReference[] viewReferences = activePage.getViewReferences();
			for (IViewReference viewReference : viewReferences) {
				if (OcamlCustomToplevelView.ID.equals(viewReference.getId()))
					activePage.hideView(viewReference);
			}
		} catch (Throwable e) {
			OcamlPlugin.logError("Error while closing custom toplevel views", e);
		}
	}*/

}
