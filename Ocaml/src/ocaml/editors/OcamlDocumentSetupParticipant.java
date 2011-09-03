package ocaml.editors;

import ocaml.OcamlPlugin;
import ocaml.editor.syntaxcoloring.OcamlPartitionScanner;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/** Configures the OCaml editor with the partitioning rules */
public class OcamlDocumentSetupParticipant implements IDocumentSetupParticipant {

	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner =
					new FastPartitioner(OcamlPlugin.getInstance().getOcamlPartitionScanner(),
						OcamlPartitionScanner.OCAML_PARTITION_TYPES);
			extension3
				.setDocumentPartitioner(OcamlPartitionScanner.OCAML_PARTITIONING, partitioner);
			partitioner.connect(document);
		}

	}

}
