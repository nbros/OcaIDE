package ocaml.editor.newFormatter;

import ocaml.OcamlPlugin;
import ocaml.editor.newFormatter.IndentHint.Type;
import ocaml.preferences.PreferenceConstants;

public class IndentingPreferences {

	int indentBegin;
	int indentStruct;
	int indentSig;
	int indentIn;
	int indentDef;
	int indentFor;
	int indentThen;
	int indentElse;
	int indentWhile;
	int indentMatchAction;
	int indentFirstMatchAction;
	int indentFunctor;
	int indentTry;
	int indentWith;
	int indentObject;
	int indentApplication;
	int indentRecord;
	int indentFirstConstructor;
	int indentParen;
	int indentFirstCatch;
	int indentFunArgs;
	int indentModuleConstraint;

	public void readPreferences() {
		indentBegin = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_BEGIN);
		indentStruct = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_STRUCT);
		indentSig = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_SIG);
		indentIn = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_IN);
		indentDef = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_DEF);
		indentFor = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_FOR);
		indentThen = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_THEN);
		indentElse = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_ELSE);
		indentWhile = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_WHILE);
		indentMatchAction = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_MATCH_ACTION);
		indentFirstMatchAction = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_FIRST_MATCH_CASE);
		indentFunctor = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_FUNCTOR);
		indentTry = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_TRY);
		indentWith = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_WITH);
		indentObject = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_OBJECT);
		indentApplication = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_APPLICATION);
		indentRecord = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_RECORD);
		indentFirstConstructor = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_FIRST_CONSTRUCTOR);
		indentParen = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_PAREN);
		indentFirstCatch = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_FIRST_CATCH);
		indentFunArgs = OcamlPlugin.getInstance().getPreferenceStore().getInt(
				PreferenceConstants.P_FORMATTER_FUN_ARGS);
		indentModuleConstraint = OcamlPlugin.getInstance().getPreferenceStore()
				.getInt(PreferenceConstants.P_FORMATTER_MODULE_CONSTRAINT);
	}

	public int getIndent(Type type) {
		switch (type) {
		case APP:
			return indentApplication;
		case BEGIN:
			return indentBegin;
		case DEF:
			return indentDef;
		case ELSE:
			return indentElse;
		case FIRST_CONTRUCTOR:
			return indentFirstConstructor;
		case FIRST_MATCH_CASE:
			return indentFirstMatchAction;
		case FIRSTCATCH:
			return indentFirstCatch;
		case FOR:
			return indentFor;
		case FUNARGS:
			return indentFunArgs;
		case FUNCTOR:
			return indentFunctor;
		case IN:
			return indentIn;
		case MATCH_ACTION:
			return indentMatchAction;
		case MODULECONSTRAINT:
			return indentModuleConstraint;
		case OBJECT:
			return indentObject;
		case PAREN:
			return indentParen;
		case RECORD:
			return indentRecord;
		case SIG:
			return indentSig;
		case STRUCT:
			return indentStruct;
		case THEN:
			return indentThen;
		case TRY:
			return indentTry;
		case WHILE:
			return indentWhile;
		case WITH:
			return indentWith;
		default:
			OcamlPlugin
					.logError("IndentHint not handled in IndentingPreferences: "
							+ type.name());
		}

		return 1;
	}
}
