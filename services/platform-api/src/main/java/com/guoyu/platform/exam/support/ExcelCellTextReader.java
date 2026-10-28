package com.guoyu.platform.exam.support;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.util.StringUtils;

/**
 * XLSX 单元格统一读取器（方案 §14.4.2）。
 *
 * <p>核心约束：普通数值禁止 {@code (int)} 强转，必须按 plain decimal 输出；日期/百分比/自定义格式
 * 用 DataFormatter 保留显示语义；公式按缓存结果类型分派，缓存缺失/错误一律 hard error。
 *
 * <p>实例与单个 Workbook 绑定（DataFormatter 有格式缓存），不可跨工作簿复用。
 */
public final class ExcelCellTextReader {

  static final String RULE_CELL_NON_FINITE = "EXCEL_CELL_NON_FINITE";
  static final String RULE_FORMULA_CACHE_MISSING = "EXCEL_FORMULA_CACHE_MISSING";
  static final String RULE_CELL_ERROR_LITERAL = "EXCEL_CELL_ERROR_LITERAL";
  static final String RULE_NUMERIC_PRECISION_WARNING = "EXCEL_NUMERIC_PRECISION_WARNING";

  private static final int MAX_SAFE_SIGNIFICANT_DIGITS = 15;

  private final DataFormatter formatter = new DataFormatter();
  private final boolean use1904Windowing;

  public ExcelCellTextReader(Workbook workbook) {
    // DataFormatter 使用工作簿日期系统与单元格格式；不启用 emulateCSV，避免数值被格式化
    this.use1904Windowing = workbook instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook xssf
        && xssf.isDate1904();
  }

  public ExcelCellValue read(Cell cell, String sheet, int row, int column, String field) {
    if (cell == null) {
      return new ExcelCellValue("", "", "", List.of());
    }
    String numberFormat = safeFormat(cell);
    String rawDisplay = safeDisplay(cell);
    List<QuestionImportIssue> issues = new ArrayList<>();

    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      return readFormula(cell, sheet, row, column, field, numberFormat, rawDisplay, issues);
    }
    if (type == CellType.ERROR) {
      String literal = errorLiteral(cell.getErrorCellValue());
      issues.add(errorIssue(sheet, row, column, field, rawDisplay, literal));
      return new ExcelCellValue("", rawDisplay, numberFormat, List.copyOf(issues));
    }
    if (type == CellType.NUMERIC) {
      String text = readNumeric(cell.getNumericCellValue(), numberFormat, rawDisplay, sheet, row,
          column, field, issues);
      return new ExcelCellValue(text, rawDisplay, numberFormat, List.copyOf(issues));
    }
    if (type == CellType.STRING) {
      String text = cell.getStringCellValue();
      text = text == null ? "" : text.trim();
      return new ExcelCellValue(text, rawDisplay, numberFormat, List.copyOf(issues));
    }
    if (type == CellType.BOOLEAN) {
      return new ExcelCellValue(Boolean.toString(cell.getBooleanCellValue()), rawDisplay,
          numberFormat, List.copyOf(issues));
    }
    // BLANK / _NONE
    return new ExcelCellValue("", rawDisplay, numberFormat, List.copyOf(issues));
  }

  private ExcelCellValue readFormula(Cell cell, String sheet, int row, int column, String field,
      String numberFormat, String rawDisplay, List<QuestionImportIssue> issues) {
    if (cell instanceof XSSFCell xssf && !xssf.getCTCell().isSetV()) {
      issues.add(new QuestionImportIssue(QuestionImportIssue.SEVERITY_ERROR,
          RULE_FORMULA_CACHE_MISSING, sheet, row, column, field, rawDisplay, rawDisplay,
          "公式单元格没有缓存计算结果，请先在 Excel 中重算并保存后再导入"));
      return new ExcelCellValue("", rawDisplay, numberFormat, List.copyOf(issues));
    }
    CellType cached = cell.getCachedFormulaResultType();
    if (cached == CellType.ERROR) {
      String literal = errorLiteral(cell.getErrorCellValue());
      issues.add(errorIssue(sheet, row, column, field, rawDisplay, literal));
      return new ExcelCellValue("", rawDisplay, numberFormat, List.copyOf(issues));
    }
    if (cached == CellType.NUMERIC) {
      String text = readNumeric(cell.getNumericCellValue(), numberFormat, rawDisplay, sheet, row,
          column, field, issues);
      return new ExcelCellValue(text, rawDisplay, numberFormat, List.copyOf(issues));
    }
    if (cached == CellType.STRING) {
      String text = cell.getStringCellValue();
      return new ExcelCellValue(text == null ? "" : text.trim(), rawDisplay, numberFormat,
          List.copyOf(issues));
    }
    if (cached == CellType.BOOLEAN) {
      return new ExcelCellValue(Boolean.toString(cell.getBooleanCellValue()), rawDisplay,
          numberFormat, List.copyOf(issues));
    }
    issues.add(new QuestionImportIssue(QuestionImportIssue.SEVERITY_ERROR,
        RULE_FORMULA_CACHE_MISSING, sheet, row, column, field, rawDisplay, rawDisplay,
        "公式单元格没有可读取的结果，请先在 Excel 中重算并保存后再导入"));
    return new ExcelCellValue("", rawDisplay, numberFormat, List.copyOf(issues));
  }

  private String readNumeric(double value, String numberFormat, String rawDisplay, String sheet,
      int row, int column, String field, List<QuestionImportIssue> issues) {
    if (!Double.isFinite(value)) {
      issues.add(new QuestionImportIssue(QuestionImportIssue.SEVERITY_ERROR, RULE_CELL_NON_FINITE,
          sheet, row, column, field, rawDisplay, rawDisplay,
          "单元格数值无效（NaN 或无穷大），请修正源文件"));
      return "";
    }
    boolean dateFormatted = false;
    boolean percentFormatted = numberFormat != null && numberFormat.contains("%");
    boolean generalOrScientific = numberFormat == null || numberFormat.isEmpty()
        || "General".equalsIgnoreCase(numberFormat) || numberFormat.toUpperCase().contains("E+")
        || numberFormat.toUpperCase().contains("E-");
    try {
      dateFormatted = DateUtil.isADateFormat(0, numberFormat);
    } catch (RuntimeException ignore) {
      dateFormatted = false;
    }
    if (dateFormatted) {
      // 日期/时间：交给 DataFormatter，禁止输出 Excel 序列数
      return safeCellDisplay(value, numberFormat);
    }
    if (percentFormatted) {
      return safeCellDisplay(value, numberFormat);
    }
    if (!generalOrScientific) {
      // 自定义小数/千分位：保留业务展示含义（0.50、12,345）
      return safeCellDisplay(value, numberFormat);
    }
    BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
    if (decimal.scale() < 0) {
      decimal = decimal.setScale(0);
    }
    String plain = decimal.toPlainString();
    if (plain.replace("-", "").replace(".", "").length() > MAX_SAFE_SIGNIFICANT_DIGITS) {
      issues.add(new QuestionImportIssue(QuestionImportIssue.SEVERITY_WARNING,
          RULE_NUMERIC_PRECISION_WARNING, sheet, row, column, field, rawDisplay, plain,
          "数值有效位数超过 15 位，Excel 双精度可能已丢失精度；如为编码/标识请改为文本格式"));
    }
    return plain;
  }

  /**
   * 用 DataFormatter 按单元格自身格式输出显示文本。此方法不依赖 cell 引用，构造一个临时格式
   * 计算：直接复用 formatter 的 formatRawCellContents（与单元格格式一致）。
   */
  private String safeCellDisplay(double value, String numberFormat) {
    try {
      return formatter.formatRawCellContents(value, 0, numberFormat == null ? "General" : numberFormat,
          use1904Windowing);
    } catch (RuntimeException e) {
      return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
  }

  private String safeDisplay(Cell cell) {
    try {
      return formatter.formatCellValue(cell);
    } catch (RuntimeException e) {
      return "";
    }
  }

  private String safeFormat(Cell cell) {
    try {
      return cell.getCellStyle() == null ? "" : cell.getCellStyle().getDataFormatString();
    } catch (RuntimeException e) {
      return "";
    }
  }

  private QuestionImportIssue errorIssue(String sheet, int row, int column, String field,
      String rawDisplay, String literal) {
    return new QuestionImportIssue(QuestionImportIssue.SEVERITY_ERROR, RULE_CELL_ERROR_LITERAL,
        sheet, row, column, field, rawDisplay, literal,
        "该单元格包含表格错误值 " + literal + "，请修正后重新导入");
  }

  private String errorLiteral(byte errorCode) {
    try {
      return FormulaError.forInt(errorCode).getString();
    } catch (RuntimeException e) {
      return "#ERROR";
    }
  }

  /** 供导入侧判断：文本是否只有空白。 */
  public static boolean isBlank(String text) {
    return !StringUtils.hasText(text);
  }
}
