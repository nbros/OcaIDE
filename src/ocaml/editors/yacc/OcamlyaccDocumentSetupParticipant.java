package ocaml.editors.yacc;

import ocaml.OcamlPlugin;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/** Configures the partitioning of the O'Caml Yacc editor */
public class OcamlyaccDocumentSetupParticipant implements IDocumentSetupParticipant {
	
	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner =
					new FastPartitioner(OcamlPlugin.getInstance().getOcamlyaccPartitionScanner(),
						OcamlyaccPartitionScanner.OCAMLYACC_PARTITION_TYPES);
			extension3
				.setDocumentPartitioner(OcamlyaccPartitionScanner.OCAMLYACC_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}

}
