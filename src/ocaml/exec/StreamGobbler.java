package ocaml.exec;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class, used by CommandRunner to absorb the output of a process quickly, so as to avoid
 * saturating the system buffers (which makes the whole application hang...).
 */
public class StreamGobbler extends Thread {

	private InputStream inputStream = null;
	private StringBuffer result = new StringBuffer();

	StreamGobbler(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	@Override
	public void run() {
		this.fillReturnBuffer();
	}

	protected synchronized void fillReturnBuffer() {
		try {
			InputStreamReader inputStreamReader = new InputStreamReader(this.inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					this.result.append(line + "\n");
			}
		} catch (Exception e) {
			ocaml.OcamlPlugin.logError("error in StreamGobbler:fillReturnBuffer", e);
		}
	}

	public synchronized String waitAndGetResult() {
		try {
			this.join();
		} catch (InterruptedException e) {
			ocaml.OcamlPlugin.logError("StreamGobbler:waitAndGetResult interrupted", e);
		}
		
		return this.result.toString();
	}
}