package com.lexicalintelligence.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author Kirk Baker
 *
 */
public class SpreadsheetFactory {

	public static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
	public static final String CSV_CONTENT_TYPE = "text/csv";
	public static final String TSV_CONTENT_TYPE = "text/tab-separated-values";

	public static SpreadsheetWriter writer(String file) throws IOException {
		if (file.toLowerCase().endsWith(".csv")) {
			return new UnicodeCsvWriter(file, false);
		}
		else if (file.toLowerCase().endsWith(".tsv")) {
			return new UnicodeTsvWriter(file, false);
		}
		else if (file.toLowerCase().endsWith(".xls")) {
			return new XlsWriter(file);
		}
		else if (file.toLowerCase().endsWith(".xlsx")) {
			return new XlsxWriter(file);
		}
		throw new IllegalArgumentException("File format is not supported.");
	}

	public static SpreadsheetWriter writer(String file, boolean append) throws IOException {
		if (file.toLowerCase().endsWith(".csv")) {
			return new UnicodeCsvWriter(file, append);
		}
		else if (file.toLowerCase().endsWith(".tsv")) {
			return new UnicodeTsvWriter(file, append);
		}
		throw new IllegalArgumentException("File format is not supported for append, use tsv or csv");
	}

	public static SpreadsheetWriter writer(OutputStream outputStream, String extension) throws Exception {
		if (extension.toLowerCase().endsWith(".csv")) {
			return new UnicodeCsvWriter(outputStream);
		}
		else if (extension.toLowerCase().endsWith(".tsv")) {
			return new UnicodeTsvWriter(outputStream);
		}
		else if (extension.toLowerCase().endsWith(".xls")) {
			return new XlsWriter(outputStream);
		}
		else if (extension.toLowerCase().endsWith(".xlsx")) {
			return new XlsxWriter(outputStream);
		}
		//		throw new UnsupportedOperationException("Not supported: " + contentType);
		throw new IllegalArgumentException("File format is not supported.");
	}

	public static SpreadsheetReader reader(String file, boolean hasHeader) throws Exception {
		if (file.toLowerCase().endsWith(".csv") || file.toLowerCase().endsWith(".csv.gz") || file.toLowerCase().endsWith(".tsv") || file.toLowerCase()
				.endsWith(".tsv.gz")) {
			if (file.toLowerCase().endsWith(".csv") || file.toLowerCase().endsWith(".csv.gz")) {
				return new UnicodeCsvReader(file, ',', hasHeader);
			}
			else {
				return new UnicodeCsvReader(file, '\t', hasHeader);
			}
		}
		else if (file.toLowerCase().endsWith(".xls")) {
			return new WorkbookReader(file, hasHeader);
		}
		else if (file.toLowerCase().endsWith(".xlsx")) {
			return new WorkbookReader(file, hasHeader);
		}
		//		throw new UnsupportedOperationException("Not supported: " + contentType);
		throw new IllegalArgumentException("File format is not supported.");
	}

	public static SpreadsheetReader reader(String file, char delimiter, boolean hasHeader) throws Exception {
		if (file.toLowerCase().endsWith(".csv") || file.toLowerCase().endsWith(".csv.gz") || file.toLowerCase().endsWith(".tsv") || file.toLowerCase()
				.endsWith(".tsv.gz")) {
			return new UnicodeCsvReader(file, delimiter, hasHeader);
		}
		else if (file.toLowerCase().endsWith(".xls")) {
			return new WorkbookReader(file, hasHeader);
		}
		else if (file.toLowerCase().endsWith(".xlsx")) {
			return new WorkbookReader(file, hasHeader);
		}
		//		throw new UnsupportedOperationException("Not supported: " + contentType);
		throw new IllegalArgumentException("File format is not supported.");
	}

	public static SpreadsheetReader reader(InputStream inputStream, String contentType, boolean hasHeader) throws Exception {

		if (contentType.contains("csv") || contentType.contains("tab-separated-values")) {
			if (contentType.contains("csv")) {
				if (contentType.endsWith(".gz")) {
					return new UnicodeCsvReader(new GZIPInputStream(inputStream), ',', hasHeader);
				}
				else {
					return new UnicodeCsvReader(inputStream, ',', hasHeader);
				}
			}
			else {
				return new UnicodeCsvReader(inputStream, '\t', hasHeader);
			}
		}
		else if (contentType.contains(".xls") || contentType.contains("application/vnd.ms-excel")) {
			return new WorkbookReader(inputStream, hasHeader);
		}
		else if (contentType.contains(".xlsx") || contentType.contains("spreadsheetml")) {
			return new WorkbookReader(inputStream, hasHeader);
		}

		//		throw new UnsupportedOperationException("Not supported: " + contentType);
		throw new IllegalArgumentException("File format " + contentType + " is not supported.");
	}

	public static SpreadsheetReader reader(InputStream inputStream, String contentType, boolean hasHeader, char delimiter) throws Exception {
		if (contentType.contains("csv") || contentType.contains("tab-separated-values")) {
			if (contentType.contains("csv")) {
				if (contentType.endsWith(".gz")) {
					return new UnicodeCsvReader(new GZIPInputStream(inputStream), delimiter, hasHeader);
				}
				else {
					return new UnicodeCsvReader(inputStream, delimiter, hasHeader);
				}
			}
			else {
				return new UnicodeCsvReader(inputStream, '\t', hasHeader);
			}
		}
		else if (contentType.contains(".xlsx") || contentType.contains("spreadsheetml")) {
			return new WorkbookReader(inputStream, hasHeader);
		}
		else if (contentType.contains(".xls") || contentType.contains("application/vnd.ms-excel")) {
			return new WorkbookReader(inputStream, hasHeader);
		}

		//		throw new UnsupportedOperationException("Not supported: " + contentType);
		throw new IllegalArgumentException("File format " + contentType + " is not supported.");
	}

	public static String getContentType(String fileExtension) {

		if (fileExtension.equals(".xls")) {
			return XLS_CONTENT_TYPE;
		}
		else if (fileExtension.equals(".xlsx")) {
			return XLSX_CONTENT_TYPE;
		}
		else if (fileExtension.equals(".csv")) {
			return CSV_CONTENT_TYPE;
		}
		else if (fileExtension.equals(".tsv")) {
			return TSV_CONTENT_TYPE;
		}

		return null;
	}

}
