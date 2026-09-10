package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;

/**
 * Excel 模板变量替换：扫描全部单元格中的 ${key} / $key$，替换为标量值。
 * 逐行学员花名册请用 {@link SubsidyXlsxRosterFiller}。
 */
public final class TrainingXlsxVariableReplacer {

  private static final Pattern BRACE = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
  private static final Pattern DOLLAR = Pattern.compile("\\$([a-zA-Z0-9_]+)\\$");
  private static final DataFormatter FORMATTER = new DataFormatter();

  private TrainingXlsxVariableReplacer() {}

  public static byte[] replaceAll(byte[] xlsxBytes, Map<String, String> vars) throws IOException {
    if (xlsxBytes == null || xlsxBytes.length == 0) {
      throw new IOException("空 Excel 模板");
    }
    List<Map.Entry<String, String>> ordered = new ArrayList<>(vars.entrySet());
    ordered.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
            replaceCell(cell, ordered);
          }
        }
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  public static Set<String> extractKeys(byte[] xlsxBytes) throws IOException {
    Set<String> keys = new LinkedHashSet<>();
    if (xlsxBytes == null || xlsxBytes.length == 0) {
      return keys;
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
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
            String text = cellText(cell);
            if (!StringUtils.hasText(text)) {
              continue;
            }
            Matcher m1 = BRACE.matcher(text);
            while (m1.find()) {
              keys.add(m1.group(1));
            }
            Matcher m2 = DOLLAR.matcher(text);
            while (m2.find()) {
              keys.add(m2.group(1));
            }
          }
        }
      }
    }
    return keys;
  }

  static void replaceCell(Cell cell, List<Map.Entry<String, String>> ordered) {
    if (cell == null) {
      return;
    }
    String text = cellText(cell);
    if (!StringUtils.hasText(text) || (!text.contains("${") && !text.contains("$"))) {
      return;
    }
    String replaced = text;
    for (Map.Entry<String, String> var : ordered) {
      String value = var.getValue() == null ? "" : var.getValue();
      replaced = replaced.replace("${" + var.getKey() + "}", value);
      replaced = replaced.replace("$" + var.getKey() + "$", value);
    }
    if (!replaced.equals(text)) {
      cell.setCellValue(replaced);
    }
  }

  static String cellText(Cell cell) {
    if (cell == null) {
      return "";
    }
    if (cell.getCellType() == CellType.STRING) {
      return cell.getStringCellValue();
    }
    if (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.STRING) {
      return cell.getStringCellValue();
    }
    String formatted = FORMATTER.formatCellValue(cell);
    return formatted == null ? "" : formatted;
  }

  static boolean containsAnyPlaceholder(String text, Set<String> keys) {
    if (!StringUtils.hasText(text) || keys == null || keys.isEmpty()) {
      return false;
    }
    for (String key : keys) {
      if (text.contains("${" + key + "}") || text.contains("$" + key + "$")) {
        return true;
      }
    }
    return false;
  }
}
