package com.lexicalintelligence.spreadsheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Kirk Baker
 *
 */
public abstract class SpreadsheetReader implements AutoCloseable, Iterable<SpreadsheetRow> {

	private static final Logger LOG = LoggerFactory.getLogger(SpreadsheetReader.class);

	private SpreadsheetRow currentRow = new SpreadsheetRow(Collections.emptyList(), Collections.emptyMap());
	private List<String> headers = new ArrayList<>();
	private Map<String, Integer> headerToIndexMap = new HashMap<>();

	public SpreadsheetReader() {

	}

	protected void readHeaders() throws IOException {
		if (hasAnotherRow()) {
			readRow();
			handleRawHeader(currentRow.getRow());
		}
		else {
			throw new RuntimeException(ErrorMessages.NO_HEADER);
		}
	}

	protected void handleRawHeader(List<String> rawHeaders) {
		if (rawHeaders == null || rawHeaders.isEmpty()) {
			throw new RuntimeException(ErrorMessages.NO_HEADER);
		}
		else {

			for (int i = 0; i < rawHeaders.size(); i++) {
				String header = rawHeaders.get(i);
				if (header == null) {
					header = "";
				}
				header = header.trim();

				if (header.isEmpty()) {
					LOG.warn(ErrorMessages.MISSING_CELLS_IN_HEADER + " in column " + i);
				}
				else {
					headers.add(header);
					if (!headerToIndexMap.containsKey(header)) {
						headerToIndexMap.put(header, i);
					}
					else {
						LOG.warn(ErrorMessages.DUPLICATE_HEADER + " Header: " + header);
					}
				}
			}
		}

	}

	public Iterator<SpreadsheetRow> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return hasAnotherRow();
			}

			@Override
			public SpreadsheetRow next() {
				try {
					readRow();
					return currentRow;
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}

			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("No changes allowed");
			}
		};
	}

	protected Map<String, Integer> getHeaderToIndexMap() {
		return headerToIndexMap;
	}

	public List<String> getHeaders() {
		return headers;
	}

	public boolean hasHeader(String col) {
		return headerToIndexMap.containsKey(col);
	}

	public SpreadsheetRow getCurrentRow() {
		return currentRow;
	}

	protected void setCurrentRow(SpreadsheetRow currentRow) {
		this.currentRow = currentRow;
	}

	public String get(String col) {
		return currentRow.get(col);
	}

	public String get(int index) {
		return currentRow.get(index);
	}

	public List<String> getRow() {
		return currentRow.getRow();
	}

	public List<String> getAsList(String col) {
		return currentRow.getAsList(col);
	}

	public List<String> getAsList(String col, String delim) {
		return currentRow.getAsList(col, delim);
	}

	public List<String> getAsList(int col) {
		return currentRow.getAsList(col);
	}

	public List<String> getAsList(int col, String delim) {
		return currentRow.getAsList(col, delim);
	}

	public int getInteger(String col) throws NumberFormatException {
		return currentRow.getInteger(col);
	}

	public int getInteger(int index) throws NumberFormatException {
		return currentRow.getInteger(index);
	}

	public double getDouble(String col) throws NumberFormatException {
		return currentRow.getDouble(col);
	}

	public double getDouble(int index) throws NumberFormatException {
		return currentRow.getDouble(index);
	}

	public abstract int getTotalNumberOfRecords() throws IOException;

	public abstract boolean hasAnotherRow();

	public abstract boolean readRow() throws IOException;

}
