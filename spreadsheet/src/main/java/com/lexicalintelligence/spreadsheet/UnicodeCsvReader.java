/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package com.lexicalintelligence.spreadsheet;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

public class UnicodeCsvReader extends SpreadsheetReader {

	private CsvParser csv;
	private Reader reader;
	private String[] next;

	private CsvParser createParser(char delimiter) {
		CsvParserSettings parserSettings = new CsvParserSettings();
		parserSettings.setLineSeparatorDetectionEnabled(true);
		CsvFormat csvFormat = new CsvFormat();
		csvFormat.setDelimiter(delimiter);
		parserSettings.setFormat(csvFormat);
		parserSettings.setMaxCharsPerColumn(128 * 1024 * 1024);
		parserSettings.setMaxColumns(2048);
		return new CsvParser(parserSettings);
	}

	public UnicodeCsvReader(String path, char delimiter, boolean hasHeaders) throws Exception {
		openReader(path, delimiter, hasHeaders);

	}

	public UnicodeCsvReader(InputStream inputStream, char delimiter, boolean hasHeaders) throws Exception {
		openReader(inputStream, delimiter, hasHeaders);
	}

	@Override
	public void close() throws Exception {
		reader.close();
		csv.stopParsing();
	}

	@Override
	public boolean hasAnotherRow() {
		return (next != null);
	}

	@Override
	public boolean readRow() {
		if (hasAnotherRow()) {
			String[] row = next;
			setCurrentRow(new SpreadsheetRow(Arrays.asList(row), getHeaderToIndexMap()));
			next = csv.parseNext();
			return true;
		}
		else {
			return false;
		}

	}

	@Override
	public int getTotalNumberOfRecords() {
		throw new UnsupportedOperationException("CSV/TSV does not support this");
	}

	private void openReader(String path, char delimiter, boolean hasHeaders) throws IOException {
		csv = createParser(delimiter);
		if (path.toLowerCase().endsWith(".gz")) {
			reader = new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(path))), StandardCharsets.UTF_8);
		}
		else {
			reader = new InputStreamReader(new FileInputStream(new File(path)), StandardCharsets.UTF_8);
		}
		csv.beginParsing(reader);
		next = csv.parseNext();
		if (hasHeaders) {
			readHeaders();
		}

	}

	private void openReader(InputStream inputStream, char delimiter, boolean hasHeaders) throws IOException {
		this.csv = createParser(delimiter);
		this.reader = new InputStreamReader(inputStream);
		this.csv.beginParsing(reader);
		next = csv.parseNext();
		if (hasHeaders) {
			readHeaders();
		}

	}

	protected static byte[] readStreamToBytes(InputStream inputStream, boolean close) throws IOException {

		byte[] data;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			inputStream.transferTo(baos);
			data = baos.toByteArray();
		}
		finally {
			if (close) {
				inputStream.close();
			}
		}
		return data;
	}

}