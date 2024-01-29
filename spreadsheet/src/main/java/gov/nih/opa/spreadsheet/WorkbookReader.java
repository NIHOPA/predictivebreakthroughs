package gov.nih.opa.spreadsheet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.util.LocaleUtil;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * @author Kirk Baker
 */
public class WorkbookReader extends SpreadsheetReader {

	private Workbook reader;
	private Sheet sheet;

	private int numberOfRows;
	private int currentRow;
	private int numberOfCells;

	private final boolean hasHeader;

	public WorkbookReader(String file, boolean hasHeader) throws Exception {
		this(new FileInputStream(file), hasHeader);
	}

	public WorkbookReader(InputStream inputStream, boolean hasHeader) throws Exception {
		LocaleUtil.setUserTimeZone(LocaleUtil.TIMEZONE_UTC);
		reader = WorkbookFactory.create(inputStream);
		sheet = reader.getSheetAt(0);
		numberOfRows = sheet.getLastRowNum() + 1;
		numberOfCells = getCellCount(hasHeader);

		this.hasHeader = hasHeader;
		if (hasHeader) {
			readHeaders();
		}

	}

	@Override
	public void close() throws Exception {
		reader.close();
	}

	@Override
	public boolean hasAnotherRow() {
		return (currentRow < numberOfRows);
	}

	@Override
	public boolean readRow() {
		if (hasAnotherRow()) {
			ArrayList<String> values = new ArrayList<>();
			Row next = sheet.getRow(currentRow);
			if (next != null) {
				for (int i = 0; i < numberOfCells; i++) {
					Cell cell = next.getCell(i);
					String cellStringValue = getCellStringValue(cell);
					if (cellStringValue != null && cellStringValue.isEmpty()) {
						cellStringValue = null;
					}
					values.add(cellStringValue);
				}
			}
			setCurrentRow(new SpreadsheetRow(values, getHeaderToIndexMap()));
			currentRow++;
			return true;
		}
		return false;
	}

	private String getCellStringValue(Cell cell) {
		if (cell != null) {
			switch (cell.getCellType()) {
				case BLANK, ERROR -> {
					// do nothing?
				}
				case BOOLEAN -> {
					return String.valueOf(cell.getBooleanCellValue());
				}
				case STRING -> {
					return cell.getStringCellValue();
				}
				case NUMERIC -> {
					if (DateUtil.isCellDateFormatted(cell)) {
						return DateTimeFormatter.ISO_INSTANT.format(cell.getDateCellValue().toInstant());
					}

					Number numericCellValue = cell.getNumericCellValue();
					if ((numericCellValue.doubleValue() == Math.floor(numericCellValue.doubleValue())) && !Double.isInfinite(numericCellValue.doubleValue())) {
						return String.valueOf(numericCellValue.intValue());
					}
					else {
						return String.valueOf(numericCellValue.doubleValue());
					}
				}
				case FORMULA -> {
					if (cell.getCachedFormulaResultType().equals(CellType.NUMERIC)) {
						Number numericCellValue = cell.getNumericCellValue();
						if ((numericCellValue.doubleValue() == Math.floor(numericCellValue.doubleValue())) && !Double.isInfinite(
								numericCellValue.doubleValue())) {
							return String.valueOf(numericCellValue.intValue());
						}
						else {
							return String.valueOf(numericCellValue.doubleValue());
						}
					}
					else if (cell.getCachedFormulaResultType().equals(CellType.STRING)) {
						return cell.getRichStringCellValue().getString();
					}
				}
			}
		}
		return "";
	}

	@Override
	public int getTotalNumberOfRecords() {
		if (hasHeader) {
			return sheet.getLastRowNum();
		}
		else {
			return sheet.getLastRowNum() + 1;
		}
	}

	private int getCellCount(boolean hasHeader) throws Exception {
		Row row = sheet.getRow(0);
		if (row == null) {
			if (!hasHeader) {
				throw new Exception(ErrorMessages.NO_FIRST_ROW);
			}
			else {
				throw new Exception(ErrorMessages.NO_HEADER);
			}
		}
		else {
			return row.getLastCellNum();
		}
	}
}

