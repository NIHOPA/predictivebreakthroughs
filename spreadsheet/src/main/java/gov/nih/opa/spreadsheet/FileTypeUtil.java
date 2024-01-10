package gov.nih.opa.spreadsheet;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackageRelationshipTypes;
import org.apache.poi.poifs.filesystem.FileMagic;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

public class FileTypeUtil {

	private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String PPTX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
	private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
	private static final String DOC_CONTENT_TYPE = "application/vnd.msword";
	private static final String PPT_CONTENT_TYPE = "application/vnd.ms-powerpoint";
	private static final String CSV_CONTENT_TYPE = "text/csv";
	private static final String TSV_CONTENT_TYPE = "tab-separated-values";
	private static final String TXT_CONTENT_TYPE = "text/plain";
	private static final String PDF_CONTENT_TYPE = "application/pdf";
	private static final String PNG_CONTENT_TYPE = "image/png";
	private static final String JPEG_CONTENT_TYPE = "image/jpeg";
	private static final String GIF_CONTENT_TYPE = "image/gif";
	private static final String XML_CONTENT_TYPE = "application/xml";
	private static final String HTML_CONTENT_TYPE = "text/html";
	private static final String JSON_CONTENT_TYPE = "application/json";
	private static final String ZIP_CONTENT_TYPE = "application/zip";

	private static final String HTML_EXTENSION = "html";
	private static final String XML_EXTENSION = "xml";
	private static final String JSON_EXTENSION = "json";
	private static final String TXT_EXTENSION = "txt";
	private static final String CSV_EXTENSION = "csv";
	private static final String TSV_EXTENSION = "tsv";

	private static final List<String> DOCX_EXTENSIONS = Arrays.asList("docx", "dotx", "docm");
	private static final List<String> XLSX_EXTENSIONS = Arrays.asList("xlsx", "xltx", "xlsm", "xltm", "xlam", "xlsb");
	private static final List<String> PPTX_EXTENSIONS = Arrays.asList("pptx", "potx", "ppsx", "ppam", "pptm", "potm", "ppsm");

	private static final List<String> DOC_EXTENSIONS = Arrays.asList("doc", "dot");
	private static final List<String> XLS_EXTENSIONS = Arrays.asList("xls", "xlt", "xla");
	private static final List<String> PPT_EXTENSIONS = Arrays.asList("ppt", "pot", "pps", "ppa");

	private static final String MICROSOFT_HEX = "D0CF11E0A1B11AE";
	private static final String MICROSOFT_X_HEX = "504B03041400080";
	private static final String MICROSOFT_X_HEX2 = "504B03041400060";
	private static final String ZIP_HEX = "504B03041403000";
	private static final String PDF_HEX = "255044462D312E3";
	private static final String PNG_HEX = "89504E470D0A1A0";
	private static final String JPEG_HEX = "FFD8FFE000104A4";
	private static final String GIF_HEX = "4749463839611E0";

	private static final char[] hexCode = "0123456789ABCDEF".toCharArray();

	public static String getContentType(byte[] byteArray, String filename) {

		StringBuilder r = new StringBuilder(byteArray.length * 2);
		byte[] arr = byteArray;
		int length = byteArray.length;

		for (int idx = 0; idx < length; ++idx) {
			byte b = arr[idx];
			r.append(hexCode[b >> 4 & 15]);
			r.append(hexCode[b & 15]);
		}

		String hex = r.toString().substring(0, 15);
		String extension = filename.substring(filename.indexOf(".") + 1);

		if (hex.equals(MICROSOFT_HEX)) {
			try {
				if (FileMagic.valueOf(new ByteArrayInputStream(byteArray)) == FileMagic.OLE2) {
					// need to rely on the extension but accept only one of doc, xls or ppt
					if (DOC_EXTENSIONS.contains(extension)) {
						return DOC_CONTENT_TYPE;
					}
					else if (XLS_EXTENSIONS.contains(extension)) {
						return XLS_CONTENT_TYPE;
					}
					else if (PPT_EXTENSIONS.contains(extension)) {
						return PPT_CONTENT_TYPE;
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}
		else if (hex.equals(MICROSOFT_X_HEX) || hex.equals(MICROSOFT_X_HEX2)) {
			try {
				OPCPackage opcPackage = OPCPackage.open(new ByteArrayInputStream(byteArray));

				PackageRelationshipCollection core = opcPackage.getRelationshipsByType(PackageRelationshipTypes.CORE_DOCUMENT);
				PackagePart corePart = opcPackage.getPart(core.getRelationship(0));

				return corePart.getContentType().substring(0, corePart.getContentType().lastIndexOf("."));

			}
			catch (Exception e) {
				// need to rely on the extension but accept only one of docx, xlsx or pptx
				if (DOCX_EXTENSIONS.contains(extension)) {
					return DOCX_CONTENT_TYPE;
				}
				else if (XLSX_EXTENSIONS.contains(extension)) {
					return XLSX_CONTENT_TYPE;
				}
				else if (PPTX_EXTENSIONS.contains(extension)) {
					return PPTX_CONTENT_TYPE;
				}
			}
		}
		else if (hex.equals(ZIP_HEX)) {
			return ZIP_CONTENT_TYPE;
		}
		else if (hex.equals(PDF_HEX)) {
			return PDF_CONTENT_TYPE;
		}
		else if (hex.equals(PNG_HEX)) {
			return PNG_CONTENT_TYPE;
		}
		else if (hex.equals(JPEG_HEX)) {
			return JPEG_CONTENT_TYPE;
		}
		else if (hex.equals(GIF_HEX)) {
			return GIF_CONTENT_TYPE;
		}
		else {
			// rely on extension txt, csv, tsv, json, xml, html
			if (extension.equals(TXT_EXTENSION)) {
				return TXT_CONTENT_TYPE;
			}
			else if (extension.equals(CSV_EXTENSION)) {
				return CSV_CONTENT_TYPE;
			}
			else if (extension.equals(TSV_EXTENSION)) {
				return TSV_CONTENT_TYPE;
			}
			else if (extension.equals(JSON_EXTENSION)) {
				return JSON_CONTENT_TYPE;
			}
			else if (extension.equals(XML_EXTENSION)) {
				return XML_CONTENT_TYPE;
			}
			else if (extension.equals(HTML_EXTENSION)) {
				return HTML_CONTENT_TYPE;
			}
		}

		return null;
	}

}
