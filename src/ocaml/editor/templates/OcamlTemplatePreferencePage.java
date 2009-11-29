package ocaml.editor.templates;

import ocaml.OcamlPlugin;

import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

/**
 * @see org.eclipse.jface.preference.PreferencePage
 */
public class OcamlTemplatePreferencePage extends TemplatePreferencePage {

    public OcamlTemplatePreferencePage() {
        setPreferenceStore(OcamlPlugin.getInstance().getPreferenceStore());
        setTemplateStore(OcamlTemplateAccess.getDefault().getTemplateStore());
        setContextTypeRegistry(OcamlTemplateAccess.getDefault().getContextTypeRegistry());
    }

//    /* (non-Javadoc)
//     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
//     */
//    public boolean performOk() {
//    	  boolean ok = super.performOk();
//    	  AntUIPlugin.getDefault().savePluginPreferences();
//    	  return ok;
//    }

//    /*
//     * (non-Javadoc)
//     * 
//     * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#createViewer(org.eclipse.swt.widgets.Composite)
//     */
//    protected SourceViewer createViewer(Composite parent) {
//    	SourceViewer viewer = new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
//          
//		SourceViewerConfiguration configuration = new AntTemplateViewerConfiguration();        
//		IDocument document = new Document();       
//		new AntDocumentSetupParticipant().setup(document);
//		viewer.configure(configuration);
//		viewer.setDocument(document);
//		viewer.setEditable(false);	
//		Font font= JFaceResources.getFont(JFaceResources.TEXT_FONT);
//		viewer.getTextWidget().setFont(font);    
//		        
//		return viewer;
//    }
//
//    /* (non-Javadoc)
//     * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#getFormatterPreferenceKey()
//     */
//    protected String getFormatterPreferenceKey() {
//		return AntEditorPreferenceConstants.TEMPLATES_USE_CODEFORMATTER;
//	}
//	
//	/*
//	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#updateViewerInput()
//	 */
//	protected void updateViewerInput() {
//		IStructuredSelection selection= (IStructuredSelection) getTableViewer().getSelection();
//		SourceViewer viewer= getViewer();
//		
//		if (selection.size() == 1 && selection.getFirstElement() instanceof TemplatePersistenceData) {
//			TemplatePersistenceData data= (TemplatePersistenceData) selection.getFirstElement();
//			Template template= data.getTemplate();
//			if (AntUIPlugin.getDefault().getPreferenceStore().getBoolean(getFormatterPreferenceKey())) {
//				String formatted= XmlFormatter.format(template.getPattern(), fFormattingPreferences);
//				viewer.getDocument().set(formatted);
//			} else {
//				viewer.getDocument().set(template.getPattern());
//			}
//		} else {
//			viewer.getDocument().set(""); //$NON-NLS-1$
//		}		
//	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#isShowFormatterSetting()
	 */
	protected boolean isShowFormatterSetting() {
		return false;
	}
}