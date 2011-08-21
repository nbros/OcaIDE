package ocaml.views.ast;

import ocaml.editors.OcamlEditor;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPage;
import org.eclipse.ui.part.MessagePage;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.PageBookView;

public class OcamlASTView extends PageBookView {

	public OcamlASTView() {
	}

	@Override
	protected IPage createDefaultPage(PageBook book) {
		MessagePage messagePage = new MessagePage();
		initPage(messagePage);
		messagePage.setMessage("AST is not available");
		messagePage.createControl(book);
		return messagePage;
	}

	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		if (part instanceof OcamlEditor) {
			OcamlEditor editor = (OcamlEditor) part;
			OcamlASTPage page = new OcamlASTPage(editor);
			initPage(page);
			page.createControl(getPageBook());
			return new PageRec(part, page);
		}
		return null;
	}

	@Override
	protected void doDestroyPage(IWorkbenchPart part, PageRec pageRecord) {
		pageRecord.page.dispose();
		pageRecord.dispose();
	}

	@Override
	protected IWorkbenchPart getBootstrapPart() {
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			IWorkbenchPart activePart = page.getActivePart();
			return isImportant(activePart) ? activePart : null;
		}
		return null;
	}

	@Override
	protected boolean isImportant(IWorkbenchPart part) {
		return part instanceof OcamlEditor;
	}
}
