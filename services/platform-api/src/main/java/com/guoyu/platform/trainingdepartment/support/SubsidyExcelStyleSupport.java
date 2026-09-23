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

  /**
   * 按内容识别：docx / xlsx / xls；无法识别返回空串。
   * <p>注意：docx 与 xlsx 都是 ZIP（PK），不能只看魔数，须看包内 {@code word/} vs {@code xl/}。
   */
  public static String sniffFormat(byte[] bytes) {
    return TrainingOfficeFileKind.sniffFormat(bytes);
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
