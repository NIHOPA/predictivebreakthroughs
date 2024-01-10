package gov.nih.opa.spreadsheet;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class WorkbookWriter implements SpreadsheetWriter {
	protected OutputStream os;
	protected Sheet sheet;
	protected Workbook writer;
	protected int i = 0, j = 0;
	protected Row row = null;
	protected XSSFHelper xssfHelper;

	@Override
	public void close() throws Exception {
		writer.write(os);
		os.close();
	}

	@Override
	public void write(Object value) {
		if (row == null) {
			row = sheet.createRow(i);
		}
		setCellValue(row.createCell(j++), value);
	}

	@Override
	public void writeLink(String text, String href) {
		if (row == null) {
			row = sheet.createRow(i);
		}
		Cell cell = row.createCell(j++);
		XSSFHyperlink link = (XSSFHyperlink) writer.getCreationHelper().createHyperlink(HyperlinkType.URL);
		link.setAddress(href);
		if (xssfHelper != null) {
			cell.setCellStyle(xssfHelper.getHyperLinkStyle());
		}
		cell.setHyperlink(link);
		cell.setCellValue(text);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setCellValue(Cell cell, Object obj) {
		if (obj == null) {
			cell.setBlank();
		}
		else if (obj instanceof String) {
			String s = truncateForExcel((String) obj);
			if (s == null || s.isBlank()) {
				cell.setBlank();
			}
			else {
				try {
					cell.setCellValue(Double.valueOf(s));
				}
				catch (Exception e) {
					cell.setCellValue(s);
				}
			}
		}
		else if (obj instanceof Number) {
			cell.setCellValue(((Number) obj).doubleValue());
		}
		else if (obj instanceof Date) {
			cell.setCellStyle(xssfHelper.getDateStyle());
			cell.setCellValue((Date) obj);
		}
		else if (obj instanceof Boolean) {
			cell.setCellValue(Boolean.valueOf((Boolean) obj));
		}
		else if (obj instanceof Collection) {
			Collection collection = (Collection) obj;
			if (collection.size() == 1) {
				setCellValue(cell, collection.iterator().next());
			}
			else {
				String s = collection.stream().map(o -> o == null ? "" : o.toString()).collect(Collectors.joining(";")).toString();
				cell.setCellValue(truncateForExcel(s));
			}
		}
		else {
			cell.setCellValue(truncateForExcel(obj.toString()));
		}
		if (i == 0 && xssfHelper != null) {
			cell.setCellStyle(xssfHelper.getHeaderBoldCellStyle());
		}
	}

	@Override
	public void writeRecord(List<?> record) {
		if (this.row == null) {
			this.row = sheet.createRow(i);
		}
		if (record == null) {
			endRecord();
			return;
		}
		record.forEach(obj -> {
			setCellValue(this.row.createCell(j++), obj);
		});
		endRecord();
	}

	@Override
	public void endRecord() {
		i++;
		j = 0;
		row = null;
	}

	@Override
	public void flush() throws Exception {
		os.flush();
	}

	@Override
	public void writeRecord(Object... cells) {
		if (this.row == null) {
			this.row = sheet.createRow(i);
		}
		if (cells == null) {
			endRecord();
		}
		else {
			writeRecord(Arrays.asList(cells));
		}
	}

	private String truncateForExcel(String cell) {
		if (cell == null) {
			return cell;
		}
		cell = cell.trim();
		if (cell.isBlank()) {
			return cell;
		}
		if (cell.length() > 32000) {
			return cell.substring(0, 32000) + "...[TRUNCATED]";
		}
		return cell;
	}
}