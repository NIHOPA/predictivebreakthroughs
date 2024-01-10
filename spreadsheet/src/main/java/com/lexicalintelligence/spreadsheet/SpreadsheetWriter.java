package com.lexicalintelligence.spreadsheet;

import java.io.IOException;
import java.util.List;

/**
 * @author Kirk Baker
 *
 */
public interface SpreadsheetWriter extends AutoCloseable {
	void write(Object cell) throws Exception;

	void writeLink(String text, String href);

	void writeRecord(List<?> row) throws Exception;

	void writeRecord(Object... cell) throws Exception;

	void endRecord() throws IOException;

	void flush() throws Exception;
}
