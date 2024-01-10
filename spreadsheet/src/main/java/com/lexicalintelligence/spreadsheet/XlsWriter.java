package com.lexicalintelligence.spreadsheet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @author Kirk Baker
 */
public class XlsWriter extends WorkbookWriter {

	XlsWriter(String file) throws FileNotFoundException {
		this(new FileOutputStream(file));
	}

	XlsWriter(OutputStream outputStream) {
		writer = new HSSFWorkbook();
		sheet = writer.createSheet();
		os = outputStream;
	}
}
