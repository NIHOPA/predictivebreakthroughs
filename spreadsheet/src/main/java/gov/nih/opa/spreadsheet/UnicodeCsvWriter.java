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
package gov.nih.opa.spreadsheet;

import com.univocity.parsers.common.AbstractWriter;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UnicodeCsvWriter extends UnicodeBaseWriter<CsvWriterSettings> {

	public UnicodeCsvWriter(String path, boolean append) throws IOException {
		super(path, append);
	}

	public UnicodeCsvWriter(OutputStream outputStream) {
		super(outputStream);
	}

	@Override
	protected AbstractWriter<CsvWriterSettings> createWriter(String path, boolean append) throws IOException {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setMaxColumns(2048);
		FileWriter writer = new FileWriter(new File(path), StandardCharsets.UTF_8, append);
		return new CsvWriter(writer, settings);
	}

	@Override
	protected AbstractWriter<CsvWriterSettings> createWriter(OutputStream outputStream) {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setMaxColumns(2048);
		return new CsvWriter(outputStream, StandardCharsets.UTF_8, settings);
	}

}