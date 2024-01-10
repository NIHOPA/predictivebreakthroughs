package com.lexicalintelligence.spreadsheet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpreadsheetRow {

	private List<String> row;
	private Map<String, Integer> headerToIndexMap;

	public SpreadsheetRow(List<String> row, Map<String, Integer> headerToIndexMap) {
		this.row = row;
		this.headerToIndexMap = headerToIndexMap;
	}

	public List<String> getRow() {
		return row;
	}

	public boolean hasHeader(String col) {
		if (headerToIndexMap.isEmpty()) {
			throw new RuntimeException(ErrorMessages.READ_HEADERS);
		}
		return headerToIndexMap.containsKey(col);
	}

	public String get(String col) {
		if (hasHeader(col)) {
			return row.get(headerToIndexMap.get(col));
		}
		else {
			throw new IllegalArgumentException("Header <" + col + "> does not exist");
		}

	}

	public String get(int index) {
		if (index < 0 || index >= row.size()) {
			throw new RuntimeException("Invalid column " + index + " for spreadsheet with " + row.size() + " columns");
		}
		return row.get(index);
	}

	public List<String> getAsList(String col) {
		return getAsList(col, ";");
	}

	public List<String> getAsList(String col, String delim) {
		String val = get(col);
		if (val == null) {
			return Collections.emptyList();
		}
		return Arrays.asList(val.split(delim));
	}

	public List<String> getAsList(int col) {
		return getAsList(col, ";");
	}

	public List<String> getAsList(int col, String delim) {
		String val = get(col);
		return val == null ? Collections.emptyList() : Arrays.asList(val.split(delim));
	}

	public boolean getBoolean(String col) {
		return Boolean.parseBoolean(get(col));
	}

	public boolean getBoolean(int index) {
		return Boolean.parseBoolean(get(index));
	}

	public int getInteger(String col) throws NumberFormatException {
		return Integer.parseInt(get(col));
	}

	public int getInteger(int index) throws NumberFormatException {
		return Integer.parseInt(get(index));
	}

	public long getLong(String col) throws NumberFormatException {
		return Long.parseLong(get(col));
	}

	public long getLong(int index) throws NumberFormatException {
		return Long.parseLong(get(index));
	}

	public float getFloat(String col) throws NumberFormatException {
		return Float.parseFloat(get(col));
	}

	public float getFloat(int index) throws NumberFormatException {
		return Float.parseFloat(get(index));
	}

	public double getDouble(String col) throws NumberFormatException {
		return Double.parseDouble(get(col));
	}

	public double getDouble(int index) throws NumberFormatException {
		return Double.parseDouble(get(index));
	}

}
