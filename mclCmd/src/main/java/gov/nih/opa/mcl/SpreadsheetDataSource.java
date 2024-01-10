package gov.nih.opa.mcl;

import com.lexicalintelligence.spreadsheet.SpreadsheetFactory;
import com.lexicalintelligence.spreadsheet.SpreadsheetReader;
import com.lexicalintelligence.spreadsheet.SpreadsheetRow;

public class SpreadsheetDataSource extends FileDataSource {

	public SpreadsheetDataSource(String fileName) {
		super(fileName);
	}

	public void open() throws Exception {

		try (SpreadsheetReader reader = SpreadsheetFactory.reader(getFileName(), true)) {

			for (SpreadsheetRow row : reader) {

				if (row.getRow().size() != 3) {
					throw new RuntimeException("Expected three column input (Node A, Node B, Weight).  Found <" + row.getRow() + ">");
				}

				String nodeA = row.get(0);
				String nodeB = row.get(1);
				String value = row.get(2);

				handleInput(nodeA, nodeB, value);
			}
		}

	}

}
