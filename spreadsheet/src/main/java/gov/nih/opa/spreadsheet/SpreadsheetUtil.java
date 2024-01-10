package gov.nih.opa.spreadsheet;

import java.io.IOException;
import java.io.InputStream;

public class SpreadsheetUtil {

	public static int getRowCount(String file, boolean hasHeader) throws Exception {
		try (SpreadsheetReader reader = SpreadsheetFactory.reader(file, hasHeader)) {
			return internalCount(reader);
		}
	}

	public static int getRowCount(String file, char delimiter, boolean hasHeader) throws Exception {
		try (SpreadsheetReader reader = SpreadsheetFactory.reader(file, delimiter, hasHeader)) {
			return internalCount(reader);
		}
	}

	public static int getRowCount(InputStream inputStream, String contentType, boolean hasHeader) throws Exception {
		try (SpreadsheetReader reader = SpreadsheetFactory.reader(inputStream, contentType, hasHeader)) {
			return internalCount(reader);
		}
	}

	public static int getRowCount(InputStream inputStream, String contentType, boolean hasHeader, char delimiter) throws Exception {
		try (SpreadsheetReader reader = SpreadsheetFactory.reader(inputStream, contentType, hasHeader, delimiter)) {
			return internalCount(reader);
		}
	}

	private static int internalCount(SpreadsheetReader reader) throws IOException {
		try {
			return reader.getTotalNumberOfRecords();
		}
		catch (UnsupportedOperationException e) {
			int count = 0;
			for (SpreadsheetRow ignored : reader) {
				count++;
			}
			return count;
		}
	}
}
