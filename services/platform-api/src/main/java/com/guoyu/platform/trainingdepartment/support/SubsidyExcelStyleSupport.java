package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/** Excel 格式嗅探与边框密度（用于挑选带格式底稿） */
public final class SubsidyExcelStyleSupport {

  private SubsidyExcelStyleSupport() {}

  /** xlsx=OOXML(ZIP)，xls=OLE2；无法识别返回空串 */
  public static String sniffFormat(byte[] bytes) {
    if (bytes == null || bytes.length < 4) {
      return "";
    }
    // PK\003\004
    if (bytes[0] == 0x50 && bytes[1] == 0x4B) {
      return "xlsx";
    }
    // D0 CF 11 E0
    if ((bytes[0] & 0xFF) == 0xD0 && (bytes[1] & 0xFF) == 0xCF) {
      return "xls";
    }
    return "";
  }

  public static int countBorderedCells(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return 0;
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      int n = 0;
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        for (Row row : sheet) {
          if (row == null) {
            continue;
          }
          for (Cell cell : row) {
            if (cell == null) {
              continue;
            }
            CellStyle st = cell.getCellStyle();
            if (st == null) {
              continue;
            }
            if (st.getBorderTop() != BorderStyle.NONE
                || st.getBorderBottom() != BorderStyle.NONE
                || st.getBorderLeft() != BorderStyle.NONE
                || st.getBorderRight() != BorderStyle.NONE) {
              n++;
            }
          }
        }
      }
      return n;
    } catch (Exception ex) {
      return 0;
    }
  }

  public static boolean hasUsefulBorders(byte[] bytes) {
    return countBorderedCells(bytes) >= 20;
  }
}
