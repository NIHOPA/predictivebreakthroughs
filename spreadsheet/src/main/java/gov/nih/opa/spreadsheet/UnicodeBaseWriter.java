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
import com.univocity.parsers.common.CommonWriterSettings;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public abstract class UnicodeBaseWriter<S extends CommonWriterSettings<?>> implements SpreadsheetWriter {
	private final AbstractWriter<S> baseWriter;

	public UnicodeBaseWriter(String path, boolean append) throws IOException {
		baseWriter = createWriter(path, append);
	}

	public UnicodeBaseWriter(OutputStream outputStream) {
		baseWriter = createWriter(outputStream);
	}

	protected abstract AbstractWriter<S> createWriter(String path, boolean append) throws IOException;

	protected abstract AbstractWriter<S> createWriter(OutputStream outputStream);

	@Override
	public void write(Object obj) {
		if (obj == null) {
			baseWriter.addValue(null);
			return;
		}
		if (obj instanceof Collection<?> c) {
			String s = c.stream().map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(";"));
			baseWriter.addValue(s);
		}
		else {
			baseWriter.addValue(obj.toString());
		}
	}

	@Override
	public void writeLink(String text, String href) {
		baseWriter.addValue(href);
	}

	@Override
	public void endRecord() {
		baseWriter.writeValuesToRow();
	}

	@Override
	public void writeRecord(List<?> record) {
		if (record != null) {
			for (Object o : record) {
				write(o);
			}
		}
		endRecord();
	}

	@Override
	public void close() {
		baseWriter.close();
	}

	@Override
	public void flush() {
		baseWriter.flush();
	}

	@Override
	public void writeRecord(Object... cells) {
		if (cells != null) {
			writeRecord(Arrays.asList(cells));
		}
		else {
			endRecord();
		}
	}
}