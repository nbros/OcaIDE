package ocaml.build.ocamlbuild;

import java.util.ArrayList;

import ocaml.OcamlPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.QualifiedName;

public class OcamlbuildFlags {

	public static final String TARGETS = "ocamlbuildProjectTargets";
	public static final String LIBS = "ocamlbuildProjectLibs";
	public static final String CFLAGS = "ocamlbuildProjectCompilerFlags";
	public static final String LFLAGS = "ocamlbuildProjectLinkerFlags";

	ArrayList<String> targets;
	ArrayList<String> libs;
	ArrayList<String> cflags;
	ArrayList<String> lflags;

	private final IProject project;

	public OcamlbuildFlags(IProject project) {
		this.project = project;
	}

	public void load() {

		targets = new ArrayList<String>();
		libs = new ArrayList<String>();
		cflags = new ArrayList<String>();
		lflags = new ArrayList<String>();

		try {
			// load targets
			String strTargets = project.getPersistentProperty(new QualifiedName(
					OcamlPlugin.QUALIFIER, TARGETS));
			if(strTargets == null)
				strTargets = "";
			setTargets(strTargets);

			// load libs
			String strLibs = project.getPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER,
					LIBS));
			if(strLibs == null)
				strLibs = "";
			setLibs(strLibs);

			// load cflags
			String strCFlags = project.getPersistentProperty(new QualifiedName(
					OcamlPlugin.QUALIFIER, CFLAGS));
			if(strCFlags == null)
				strCFlags = "";
			setCFlags(strCFlags);

			// load lflags
			String strLFlags = project.getPersistentProperty(new QualifiedName(
					OcamlPlugin.QUALIFIER, LFLAGS));
			if(strLFlags == null)
				strLFlags = "";
			setLFlags(strLFlags);
		} catch (Throwable e) {
			OcamlPlugin.logError("problem loading ocamlbuild project properties", e);
		}

	}

	public void save() {
		try {
			// save targets
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, TARGETS),
					getTargets());

			// save libs
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, LIBS), getLibs());

			// save cflags
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, CFLAGS),
					getCFlags());

			// save lflags
			project.setPersistentProperty(new QualifiedName(OcamlPlugin.QUALIFIER, LFLAGS),
					getLFlags());

		} catch (Throwable e) {
			OcamlPlugin.logError("problem saving ocamlbuild project properties", e);
		}
	}
	

	public void setTargets(String strTargets) {
		targets = new ArrayList<String>();
		String targets[] = strTargets.split(",");

		for (String target : targets)
			if (!"".equals(target.trim()))
				this.targets.add(target.trim());
	}

	public void setLibs(String strLibs) {
		libs = new ArrayList<String>();
		String libs[] = strLibs.split(",");

		for (String lib : libs)
			if (!"".equals(lib.trim()))
				this.libs.add(lib.trim());
	}

	public void setCFlags(String strCFlags) {
		cflags = new ArrayList<String>();
		String cflags[] = strCFlags.split(",");

		for (String cflag : cflags)
			if (!"".equals(cflag.trim()))
				this.cflags.add(cflag.trim());
	}

	public void setLFlags(String strLFlags) {
		lflags = new ArrayList<String>();
		String lflags[] = strLFlags.split(",");

		for (String lflag : lflags)
			if (!"".equals(lflag.trim()))
				this.lflags.add(lflag.trim());
	}

	public String getTargets() {
		boolean bFirst = true;

		StringBuilder strTargets = new StringBuilder();
		bFirst = true;
		for (String target : this.targets) {
			if (bFirst)
				bFirst = false;
			else
				strTargets.append(",");
			strTargets.append(target.trim());
		}
		
		return strTargets.toString();
	}

	public String[] getTargetsAsList() {
		return this.targets.toArray(new String[this.targets.size()]);
	}

	public String getLibs() {
		StringBuilder strLibs = new StringBuilder();
		boolean bFirst = true;
		for (String lib : this.libs) {
			if (bFirst)
				bFirst = false;
			else
				strLibs.append(",");
			strLibs.append(lib.trim());
		}
		
		return strLibs.toString();
	}

	public String getCFlags() {
		StringBuilder strcflags = new StringBuilder();
		boolean bFirst = true;
		for (String cflag : this.cflags) {
			if (bFirst)
				bFirst = false;
			else
				strcflags.append(",");
			strcflags.append(cflag.trim());
		}
		
		return strcflags.toString();
	}

	public String getLFlags() {
		StringBuilder strlflags = new StringBuilder();
		boolean bFirst = true;
		for (String lflag : this.lflags) {
			if (bFirst)
				bFirst = false;
			else
				strlflags.append(",");
			strlflags.append(lflag.trim());
		}
		
		return strlflags.toString();
	}

}
