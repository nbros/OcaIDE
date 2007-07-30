package ocaml.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ocaml.OcamlPlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * This class implements a cache system for icons, so as to minimize memory use.
 */
public final class ImageRepository {

	private static Map<String, Image> cache = new HashMap<String, Image>();

	public static String ICON_BINARY = "binaryfile.gif";

	public static String ICON_BUILDALL = "buildAll.gif";

	public static String ICON_BYTECODE = "byteCode.gif";

	public static String ICON_CAML16 = "caml16x16.gif";

	public static String ICON_CAML32 = "caml32.gif";

	public static String ICON_NATURE = "camlNature.gif";

	public static String ICON_PURPLECAML16 = "camlPurple16x16.gif";

	public static String ICON_YACC = "yaccFile.gif";

	public static String ICON_LEX = "lexFile.gif";

	// public static String ICON_LIBRARY = "library.gif";
	public static String ICON_LIBRARY = "var.gif";

	public static String ICON_MODULE = "implFile.gif";

	public static String ICON_INTERFACE = "interfFile.gif";

	public static String ICON_F = "final_co.gif";

	public static String ICON_C = "constructor.gif";

	public static String ICON_LIB = "java_lib_obj.gif";

	public static String ICON_VALUE = "value.gif";

	public static String ICON_OPEN = "open.gif";

	public static String ICON_INCLUDE = "openGroup.gif";

	public static String ICON_EXCEPTION = "exception.gif";

	public static String ICON_TYPE = "type.gif";

	public static String ICON_TEMPLATES = "templates.gif";

	public static String ICON_INTERRUPT = "term_sbook.gif";

	public static String ICON_CLEAR = "clear_co.gif";

	public static String ICON_RESET = "refresh_nav.gif";

	public static String ICON_CLASS = "class.gif";

	public static String ICON_CLASS_TYPE = "class_default_obj.gif";

	public static String ICON_EXTERNAL = "external.gif";

	public static String ICON_OCAML_MODULE = "M.gif";

	public static String ICON_OCAML_MODULE_TYPE = "T.gif";

	public static String ICON_METHOD = "public.gif";

	public static String ICON_METHOD_PRIVATE = "private.gif";

	public static String ICON_OBJECT = "object.gif";

	public static String ICON_ADD = "add.gif";

	public static String ICON_HELP = "help.gif";

	public static String ICON_DELETE = "delete.gif";

	public static String ICON_DELETEALL = "removeAllRed.gif";

	public static String ICON_VAL_MUTABLE = "external.gif"; // "breakpoint.gif";

	public static String ICON_FUNCTOR = "typevariable_obj.gif";

	public static String ICON_RECORD_TYPE_CONSTRUCTOR = "att_URI_obj.gif";

	public static String ICON_INITIALIZER = "info_st_obj.gif";

	public static String ICON_LETIN = "letin.gif";

	public static final String ICON_EXPAND_ALL = "expandall.gif";

	public static final String ICON_MODULE_PARSER_ERROR = "moduleParserError.gif";
	
	public static final String ICON_BROWSE = "browse.gif";

	public static String[] all = { ICON_BINARY, ICON_BUILDALL, ICON_BYTECODE, ICON_BYTECODE,
			ICON_CAML16, ICON_CAML32, ICON_NATURE, ICON_PURPLECAML16, ICON_YACC, ICON_LEX,
			ICON_LIBRARY, ICON_MODULE, ICON_INTERFACE, ICON_F, ICON_C, ICON_LIB, ICON_VALUE,
			ICON_OPEN, ICON_EXCEPTION, ICON_TYPE, ICON_TEMPLATES, ICON_CLASS, ICON_EXTERNAL,
			ICON_OCAML_MODULE, ICON_METHOD, ICON_OBJECT, ICON_ADD, ICON_HELP,
			ICON_OCAML_MODULE_TYPE, ICON_METHOD_PRIVATE, ICON_INCLUDE, ICON_CLASS_TYPE,
			ICON_VAL_MUTABLE, ICON_FUNCTOR, ICON_RECORD_TYPE_CONSTRUCTOR, ICON_INITIALIZER,
			ICON_LETIN, ICON_MODULE_PARSER_ERROR, ICON_BROWSE };

	static {
		init();
	}

	private static void init() {
		int i = 0;
		for (int size = all.length; i < size; i++) {
			ImageDescriptor descriptor = getImageDescriptor(all[i]);
			Image img = descriptor.createImage();
			cache.put(all[i], img);
		}
	}

	public static ImageDescriptor getImageDescriptor(String name) {
		String iconPath = "icons/";
		try {
			URL installURL = OcamlPlugin.getInstallURL();
			URL url = new URL(installURL, iconPath + name);
			return ImageDescriptor.createFromURL(url);
		} catch (MalformedURLException e) {
			OcamlPlugin.logError("ocaml plugin error", e);
			return ImageDescriptor.getMissingImageDescriptor();
		}
	}

	public static Image getImage(String imageName) {
		return cache.get(imageName);
	}

}
