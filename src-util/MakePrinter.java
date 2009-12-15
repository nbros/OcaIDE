import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MakePrinter {

	static enum Kind {
		AstType, Str, Todo
	};

	static class Constr {
		Kind kind;
		String origName;
		String name;
	}

	private static Pattern pattern = Pattern
			.compile("(\\s*)\\|\\s*(\\w+)\\s*\\((.*?)\\) -> pp f \"<\\w+ loc='%s'></\\w+>.*");

	public static void main(String[] args0) throws Exception {
		List<String> types = new ArrayList<String>();
		types.add("ident");
		types.add("ctyp");
		types.add("patt");
		types.add("expr");
		types.add("module_type");
		types.add("sig_item");
		types.add("with_constr");
		types.add("binding");
		types.add("rec_binding");
		types.add("module_binding");
		types.add("match_case");
		types.add("module_expr");
		types.add("str_item");
		types.add("class_type");
		types.add("class_sig_item");
		types.add("class_expr");
		types.add("class_str_item");
		types.add("meta_bool");

		String input = readFileAsString("C:\\Users\\Nicolas\\workspaceOcaIDE\\OcamlPDB\\input.ml");
		String[] lines = input.split("\\r?\\n");
		for (String line : lines) {
			List<String> names = new ArrayList<String>();
			List<Constr> constrs = new ArrayList<Constr>();

			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				String indent = matcher.group(1);
				String name = matcher.group(2);
				String strArgs = matcher.group(3);
				// System.out.print(name + " : " + strArgs + "\t");
				System.out.print(indent + "| " + name + "(");
				String[] args = strArgs.split(",");
				for (String arg : args) {
					arg = arg.trim();

					if ("loc".equals(arg)) {
						System.out.print("loc, ");
					} else if ("string".equals(arg)) {
						// find a fresh name
						int suffix = 1;
						String newArg = "name";
						while (names.contains(newArg)) {
							newArg = "name" + suffix;
							suffix++;
						}
						names.add(newArg);
						System.out.print(newArg + ", ");

						Constr constr = new Constr();
						constr.kind = Kind.Str;
						constr.name = newArg;
						constrs.add(constr);
					}

					else if (types.contains(arg)) {
						// find a fresh name
						int suffix = 1;
						String newArg = arg + suffix;
						while (names.contains(newArg)) {
							newArg = arg + suffix;
							suffix++;
						}
						names.add(newArg);
						System.out.print(newArg + ", ");
						Constr constr = new Constr();
						constr.kind = Kind.AstType;
						constr.origName = arg;
						constr.name = newArg;
						constrs.add(constr);
					} else {
						System.out.print("(*TODO*) " + arg + ", ");
						Constr constr = new Constr();
						constr.kind = Kind.Todo;
						constr.name = arg;
						constrs.add(constr);
					}
				}
				System.out.print(") -> pp f \"<" + name + " loc='%s'>");
				for (Constr constr : constrs) {
					switch(constr.kind) {
					case AstType:
						System.out.print("<" + constr.name + ">%a</" + constr.name + ">");
						break;
					case Str:
						System.out.print("<" + constr.name + ">%s</" + constr.name + ">");
						break;
					case Todo:
						System.out.print("<" + constr.name + ">%a</" + constr.name + ">");
						break;
					}
				}
				System.out.print("</" + name + ">\" (string_of_loc loc) ");
				for (Constr constr : constrs) {
					switch(constr.kind) {
					case AstType:
						System.out.print("print_" + constr.origName + " " + constr.name + " ");
						break;
					case Str:
						System.out.print("(escape_name " + constr.name + ") ");
						break;
					case Todo:
						System.out.print("(*TODO:" + constr.name + "*) ");
						break;
					}
				}
				
				
				
				System.out.println();

				// System.out.println(line);
			} else {
				System.out.println(line);
			}
		}
	}

	private static String readFileAsString(String filePath) throws java.io.IOException {
		byte[] buffer = new byte[(int) new File(filePath).length()];
		FileInputStream f = new FileInputStream(filePath);
		f.read(buffer);
		return new String(buffer);
	}

}
