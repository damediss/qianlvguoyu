package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;

/**
 * 在已有 Excel 上按单元格地址改值，尽量保留边框/字体/合并等样式（不整表重建）。
 */
public final class SubsidyXlsxCellPatcher {

  private static final Pattern ADDR = Pattern.compile("^([A-Za-z]+)(\\d+)$");

  private SubsidyXlsxCellPatcher() {}

  public static byte[] patch(byte[] sourceBytes, Map<String, String> cells) throws IOException {
    if (sourceBytes == null || sourceBytes.length == 0) {
      throw new IOException("空 Excel 文件");
    }
    if (cells == null || cells.isEmpty()) {
      return sourceBytes;
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(sourceBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
      if (sheet == null) {
        throw new IOException("工作表为空");
      }
      for (Map.Entry<String, String> e : cells.entrySet()) {
        if (!StringUtils.hasText(e.getKey())) {
          continue;
        }
        int[] rc = parseAddress(e.getKey().trim());
        if (rc == null) {
          continue;
        }
        applyValue(sheet, rc[0], rc[1], e.getValue());
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private static void applyValue(Sheet sheet, int rowIdx, int colIdx, String raw) {
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
      row = sheet.createRow(rowIdx);
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
      cell = row.createCell(colIdx);
    }
    String text = raw == null ? "" : raw;
    if (!StringUtils.hasText(text)) {
      // 清空内容，保留 CellStyle（边框等）
      cell.setBlank();
      return;
    }
    if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
      String cleaned = text.replace(",", "").trim();
      if (cleaned.matches("^-?\\d+(\\.\\d+)?$")) {
        cell.setCellValue(Double.parseDouble(cleaned));
        return;
      }
    }
    cell.setCellValue(text);
  }

  /** @return int[]{rowIndex0, colIndex0} */
  static int[] parseAddress(String addr) {
    Matcher m = ADDR.matcher(addr.trim().toUpperCase(Locale.ROOT));
    if (!m.matches()) {
      return null;
    }
    String colLetters = m.group(1);
    int row = Integer.parseInt(m.group(2));
    if (row < 1) {
      return null;
    }
    int col = 0;
    for (int i = 0; i < colLetters.length(); i++) {
      col = col * 26 + (colLetters.charAt(i) - 'A' + 1);
    }
    return new int[] {row - 1, col - 1};
  }
}
