package gov.nih.opa.mcl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

/***
 * Simple datasource when input in tab separated three columns file: nodeA nodeB weight
 */
public class ABCMCLDataSource extends FileDataSource {

	public ABCMCLDataSource(String fileName) {
		super(fileName);
	}

	public void open() throws IOException {

		try (Stream<String> stream = Files.lines(Paths.get(getFileName()))) {
			stream.forEach(s -> {

				if (!s.isEmpty()) {

					int firstTab = s.indexOf("\t");
					int lastTab = s.lastIndexOf("\t");

					if (firstTab == -1 || lastTab == -1) {
						throw new RuntimeException("Expected three columns separated by tabs.  Found: " + s);
					}

					String nodeA = s.substring(0, firstTab).trim();
					String nodeB = s.substring(firstTab + 1, lastTab).trim();
					if (nodeB.indexOf('\t') != -1) {
						throw new RuntimeException("Expected three columns separated by tabs.  Found: " + s);
					}

					//skip self loops
					if (!nodeA.equals(nodeB)) {

						String value = s.substring(lastTab + 1);

						handleInput(nodeA, nodeB, value);
					}
				}
			});
		}
	}

}
