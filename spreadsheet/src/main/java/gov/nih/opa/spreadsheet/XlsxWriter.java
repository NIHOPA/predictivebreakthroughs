package gov.nih.opa.spreadsheet;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @author Kirk Baker
 *
 */
public class XlsxWriter extends WorkbookWriter {

	XlsxWriter(String file) throws FileNotFoundException {
		this(new FileOutputStream(file));
	}

	XlsxWriter(OutputStream outputStream) {
		writer = new SXSSFWorkbook();
		sheet = writer.createSheet();
		os = outputStream;
		xssfHelper = new XSSFHelper((SXSSFWorkbook) writer);
	}

}
