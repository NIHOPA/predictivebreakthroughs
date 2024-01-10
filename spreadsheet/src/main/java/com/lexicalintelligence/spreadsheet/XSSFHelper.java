package com.lexicalintelligence.spreadsheet;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Payam Meyer on 5/20/16.
 * @author pmeyer
 */
public class XSSFHelper {

	private final SXSSFWorkbook workbook;
	private Map<String, CellStyle> cache = new HashMap<>();

	public XSSFHelper(SXSSFWorkbook workbook) {
		this.workbook = workbook;
	}

	public CellStyle getBoldCellStyle() {
		return cache.computeIfAbsent("boldCellStyle", key -> {
			CellStyle cellStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short) 10);
			cellStyle.setFont(font);
			return cellStyle;
		});
	}

	public CellStyle getHeaderBoldCellStyle() {
		return cache.computeIfAbsent("headerBoldCellStyle", key -> {
			CellStyle cellStyle = workbook.createCellStyle();
			Font font = workbook.createFont();
			font.setBold(true);
			font.setFontHeightInPoints((short) 10);
			cellStyle.setAlignment(HorizontalAlignment.CENTER);
			cellStyle.setFont(font);
			return cellStyle;
		});
	}

	public CellStyle getBorderCellStyle() {
		return cache.computeIfAbsent("borderCellStyle", key -> {
			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setBorderBottom(BorderStyle.MEDIUM);
			cellStyle.setBorderTop(BorderStyle.MEDIUM);
			cellStyle.setBorderLeft(BorderStyle.MEDIUM);
			cellStyle.setBorderRight(BorderStyle.MEDIUM);
			return cellStyle;
		});
	}

	public CellStyle getHyperLinkStyle() {
		return cache.computeIfAbsent("hyperLinkStyle", key -> {
			CellStyle cellStyle = workbook.createCellStyle();
			Font hlinkfont = workbook.createFont();
			hlinkfont.setUnderline(XSSFFont.U_SINGLE);
			hlinkfont.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
			cellStyle.setFont(hlinkfont);
			return cellStyle;
		});
	}

	public CellStyle getDateStyle() {
		return cache.computeIfAbsent("dateStyle", key -> {
			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setDataFormat((short) BuiltinFormats.getBuiltinFormat("m/d/yy"));
			return cellStyle;
		});
	}

}