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
import com.univocity.parsers.tsv.TsvWriter;
import com.univocity.parsers.tsv.TsvWriterSettings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UnicodeTsvWriter extends UnicodeBaseWriter<TsvWriterSettings> {

	public UnicodeTsvWriter(String path, boolean append) throws IOException {
		super(path, append);
	}

	public UnicodeTsvWriter(OutputStream outputStream) {
		super(outputStream);
	}

	@Override
	protected AbstractWriter<TsvWriterSettings> createWriter(String path, boolean append) throws IOException {
		TsvWriterSettings settings = new TsvWriterSettings();
		FileWriter writer = new FileWriter(new File(path), StandardCharsets.UTF_8, append);
		return new TsvWriter(writer, settings);
	}

	@Override
	protected AbstractWriter<TsvWriterSettings> createWriter(OutputStream outputStream) {
		TsvWriterSettings settings = new TsvWriterSettings();
		return new TsvWriter(outputStream, StandardCharsets.UTF_8, settings);
	}

}