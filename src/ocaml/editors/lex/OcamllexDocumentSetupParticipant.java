package ocaml.editors.lex;

import ocaml.OcamlPlugin;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/** Configures the partitioning of the ocamllex editor (.mll files) */
public class OcamllexDocumentSetupParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner =
					new FastPartitioner(OcamlPlugin.getInstance().getOcamllexPartitionScanner(),
						OcamllexPartitionScanner.OCAMLLEX_PARTITION_TYPES);
			extension3
				.setDocumentPartitioner(OcamllexPartitionScanner.OCAMLLEX_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}

}
