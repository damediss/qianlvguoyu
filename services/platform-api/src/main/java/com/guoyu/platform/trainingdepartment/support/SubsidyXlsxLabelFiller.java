package com.guoyu.platform.trainingdepartment.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.util.StringUtils;

/**
 * 政务空白 Excel 申请表：按中文标签自动匹配并填入右侧/下方空格。
 *
 * <p>与 {@link TrainingXlsxVariableReplacer} 互补——无 {@code ${key}} 时仍可填数。
 */
public final class SubsidyXlsxLabelFiller {

  public record FillResult(byte[] bytes, int matchedCount, List<String> matchedLabels) {}

  public record LabelHit(String label, String varKey, String sheetName, int row, int col) {}

  private record LabelRule(String label, String varKey) {}

  /** 长标签优先，避免「联系电话」抢先匹配「企业…联系电话」 */
  private static final List<LabelRule> RULES;

  static {
    List<LabelRule> rules = new ArrayList<>();
    rules.add(new LabelRule("企业组织培训企业名称", "company"));
    rules.add(new LabelRule("企业组织培训企业联系电话", "company_phone"));
    rules.add(new LabelRule("培训机构名称", "pxdw"));
    rules.add(new LabelRule("培训单位名称", "pxdw"));
    rules.add(new LabelRule("培训学校名称", "pxdw"));
    rules.add(new LabelRule("经办人姓名", "fzr"));
    rules.add(new LabelRule("经办人", "fzr"));
    rules.add(new LabelRule("联系电话", "fzrtel"));
    rules.add(new LabelRule("联系人电话", "fzrtel"));
    rules.add(new LabelRule("本次培训班次", "class_title"));
    rules.add(new LabelRule("培训班次", "class_title"));
    rules.add(new LabelRule("班期编号", "class_title"));
    rules.add(new LabelRule("班期名称", "class_title"));
    rules.add(new LabelRule("培训地址", "pxaddr"));
    rules.add(new LabelRule("培训地点", "pxaddr"));
    rules.add(new LabelRule("培训专业类别", "worktype"));
    rules.add(new LabelRule("培训工种", "worktype"));
    rules.add(new LabelRule("工种", "worktype"));
    rules.add(new LabelRule("培训级别", "level"));
    // 人员分类：企业职工填总数，失业/农村转移默认 0（避免总数误写入「城镇登记失业人员」）
    rules.add(new LabelRule("城镇登记失业人员", "count_urban_unemployed"));
    rules.add(new LabelRule("农村转移就业劳动者", "count_rural_transfer"));
    rules.add(new LabelRule("农村转移就业劳动", "count_rural_transfer"));
    rules.add(new LabelRule("毕业学年高校毕业生", "count_graduate"));
    rules.add(new LabelRule("高校毕业生", "count_graduate"));
    rules.add(new LabelRule("企业在职员工", "count_enterprise_employee"));
    rules.add(new LabelRule("企业职工", "count_enterprise_employee"));
    rules.add(new LabelRule("在职职工", "count_enterprise_employee"));
    rules.add(new LabelRule("补贴总人数", "nums"));
    rules.add(new LabelRule("补贴总金额", "amount"));
    rules.add(new LabelRule("申请补贴金额", "amount"));
    rules.add(new LabelRule("补贴金额", "amount"));
    rules.add(new LabelRule("企业名称", "company"));
    rules.add(new LabelRule("申报日期", "today"));
    rules.add(new LabelRule("申请日期", "today"));
    rules.add(new LabelRule("性质", "org_nature"));
    // 已删除短规则「补贴人数」：会误匹配「各类人员享受培训补贴人数」并把总数写到城镇失业格
    rules.sort(Comparator.comparingInt((LabelRule r) -> r.label().length()).reversed());
    RULES = List.copyOf(rules);
  }

  private static final Pattern INLINE_DATE =
      Pattern.compile("(申报日期|申请日期)[^\\d年月日]*(\\d{4})?\\s*年\\s*月\\s*日");

  private SubsidyXlsxLabelFiller() {}

  /** 扫描模板中可匹配的中文标签，返回对应变量 key（用于上传解析展示） */
  public static Set<String> detectMatchedKeys(byte[] xlsxBytes) throws IOException {
    Set<String> keys = new LinkedHashSet<>();
    for (LabelHit hit : scanHits(xlsxBytes)) {
      keys.add(hit.varKey());
      keys.add("label:" + hit.label());
    }
    return keys;
  }

  public static List<LabelHit> scanHits(byte[] xlsxBytes) throws IOException {
    List<LabelHit> hits = new ArrayList<>();
    if (xlsxBytes == null || xlsxBytes.length == 0) {
      return hits;
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes))) {
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        Set<String> usedLabels = new LinkedHashSet<>();
        for (Row row : sheet) {
          if (row == null) {
            continue;
          }
          for (Cell cell : row) {
            if (cell == null) {
              continue;
            }
            String text = TrainingXlsxVariableReplacer.cellText(cell);
            if (!StringUtils.hasText(text)) {
              continue;
            }
            LabelRule rule = matchRule(text, usedLabels);
            if (rule == null) {
              continue;
            }
            usedLabels.add(rule.label());
            hits.add(
                new LabelHit(
                    rule.label(),
                    rule.varKey(),
                    sheet.getSheetName(),
                    cell.getRowIndex(),
                    cell.getColumnIndex()));
          }
        }
      }
    }
    return hits;
  }

  public static FillResult fill(byte[] xlsxBytes, Map<String, String> vars) throws IOException {
    if (xlsxBytes == null || xlsxBytes.length == 0) {
      throw new IOException("空 Excel 模板");
    }
    Map<String, String> safeVars = vars != null ? vars : Map.of();
    int matched = 0;
    List<String> matchedLabels = new ArrayList<>();

    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(xlsxBytes));
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
        Sheet sheet = workbook.getSheetAt(s);
        if (sheet == null) {
          continue;
        }
        int formLastCol = formLastColumn(sheet);
        // 清掉表单右缘外的脏数据（历史误写入 N 列等），保持与模板一致
        clearOutsideForm(sheet, formLastCol);
        Set<String> usedLabels = new LinkedHashSet<>();
        for (Row row : sheet) {
          if (row == null) {
            continue;
          }
          // 先快照本行已有单元格：填数可能 createCell，直接 for-each row 会 CME
          List<Cell> labelCells = snapshotCells(row);
          for (Cell cell : labelCells) {
            String text = TrainingXlsxVariableReplacer.cellText(cell);
            if (!StringUtils.hasText(text)) {
              continue;
            }

            // 同行内联日期：「申报日期：2025年 月 日」——只占这一格，不再写右侧
            if (isFilledInlineDateLabel(text)) {
              usedLabels.add("申报日期");
              usedLabels.add("申请日期");
              continue;
            }
            String inline = tryFillInlineDate(text, safeVars);
            if (inline != null && !inline.equals(text)) {
              cell.setCellValue(inline);
              matched++;
              matchedLabels.add("申报日期(内联)");
              usedLabels.add("申报日期");
              usedLabels.add("申请日期");
              continue;
            }

            LabelRule rule = matchRule(text, usedLabels);
            if (rule == null) {
              continue;
            }
            // 内联日期已写过则跳过同义标签
            if ("today".equals(rule.varKey())
                && (usedLabels.contains("申报日期") || usedLabels.contains("申请日期"))) {
              continue;
            }
            String value = resolveValue(rule.varKey(), safeVars);
            if (!StringUtils.hasText(value)) {
              continue;
            }

            // 标签与值同格：「培训机构名称：」或「培训机构名称xxx」
            String afterColon = fillSameCellAfterColon(text, rule.label(), value);
            if (afterColon != null) {
              cell.setCellValue(afterColon);
              usedLabels.add(rule.label());
              matched++;
              matchedLabels.add(rule.label());
              continue;
            }

            Cell target = findValueCell(sheet, cell.getRowIndex(), cell.getColumnIndex(), formLastCol);
            if (target == null) {
              continue;
            }
            target.setCellValue(value);
            usedLabels.add(rule.label());
            matched++;
            matchedLabels.add(rule.label());
          }
        }
      }
      workbook.write(out);
      return new FillResult(out.toByteArray(), matched, matchedLabels);
    }
  }

  private static LabelRule matchRule(String cellText, Set<String> usedLabels) {
    String norm = normalize(cellText);
    if (!StringUtils.hasText(norm)) {
      return null;
    }
    // 分组标题，本身不是填数格
    if (norm.contains("各类人员享受培训补贴") || norm.equals("申请补贴情况")) {
      return null;
    }
    if (norm.length() > 40 && !norm.contains("申报日期") && !norm.contains("申请日期")) {
      return null;
    }

    LabelRule best = null;
    int bestScore = -1;
    for (LabelRule rule : RULES) {
      if (usedLabels.contains(rule.label())) {
        continue;
      }
      String ln = normalize(rule.label());
      if (!StringUtils.hasText(ln)) {
        continue;
      }
      int score = -1;
      if (norm.equals(ln) || norm.equals(ln + "人数") || norm.equals(ln + "数")) {
        score = 300 + ln.length();
      } else if (norm.startsWith(ln) || (ln.startsWith(norm) && norm.length() >= 6)) {
        // 单元格被换行截断时：农村转移就业劳动 ≈ 农村转移就业劳动者
        score = 200 + Math.min(norm.length(), ln.length());
      } else if (norm.contains(ln)) {
        // 短标签禁止「包含」匹配，避免「补贴人数」命中「各类人员…补贴人数」
        if (ln.length() < 4) {
          continue;
        }
        // 人员分类：必须标签本身命中，不能靠父标题 contains
        if (isPersonCategoryLabel(ln) && !isPersonCategoryLabel(norm) && !norm.startsWith(ln)) {
          continue;
        }
        score = 100 + ln.length();
      }
      if (score > bestScore) {
        bestScore = score;
        best = rule;
      }
    }
    return best;
  }

  private static boolean isPersonCategoryLabel(String norm) {
    return norm.contains("城镇登记")
        || norm.contains("农村转移")
        || norm.contains("高校毕业")
        || norm.contains("企业在职")
        || norm.contains("企业职工")
        || norm.contains("在职职工");
  }

  private static String normalize(String s) {
    if (s == null) {
      return "";
    }
    return s.replaceAll("[\\s　\\u00A0:：．.、|/\\\\（）()【】\\[\\]《》<>]", "").trim();
  }

  private static String resolveValue(String varKey, Map<String, String> vars) {
    if (!StringUtils.hasText(varKey)) {
      return "";
    }
    String v = vars.get(varKey);
    if (StringUtils.hasText(v)) {
      return v;
    }
    // 别名兜底
    return switch (varKey) {
      case "pxdw" -> first(vars, "train_school", "trainSchool", "school");
      case "company" -> first(vars, "company_name", "com_name");
      case "class_title" -> first(vars, "banqiname", "banqi", "classTitle", "class_term");
      case "worktype" -> first(vars, "work_type", "wktype");
      case "fzrtel" -> first(vars, "company_phone", "teatel1");
      case "company_phone" -> first(vars, "fzrtel", "teatel1");
      case "amount" -> first(vars, "subsidy_amount", "subsidyAmount");
      case "nums" -> first(vars, "studentCount");
      case "count_enterprise_employee" -> first(vars, "nums", "studentCount");
      case "count_urban_unemployed", "count_rural_transfer", "count_graduate" ->
          first(vars, varKey + "_explicit", "zero");
      case "pxaddr" -> first(vars, "company", "pxdw");
      case "today" -> first(vars, "create_time");
      default -> "";
    };
  }

  private static String first(Map<String, String> vars, String... keys) {
    for (String k : keys) {
      String v = vars.get(k);
      if (StringUtils.hasText(v)) {
        return v;
      }
    }
    return "";
  }

  private static boolean isFilledInlineDateLabel(String text) {
    if (text == null) {
      return false;
    }
    if (!text.contains("申报日期") && !text.contains("申请日期")) {
      return false;
    }
    return text.matches(".*\\d{4}\\s*年\\s*\\d{1,2}\\s*月\\s*\\d{1,2}\\s*日.*");
  }

  private static String tryFillInlineDate(String text, Map<String, String> vars) {
    if (!text.contains("年") || !text.contains("月") || !text.contains("日")) {
      return null;
    }
    if (!text.contains("申报日期") && !text.contains("申请日期") && !text.matches(".*\\d{4}\\s*年\\s*月\\s*日.*")) {
      // 仅处理带「年 月 日」空月日的申报行
      if (!(text.contains("年") && text.contains(" 月") && text.contains(" 日"))) {
        return null;
      }
    }
    String today = resolveValue("today", vars);
    if (!StringUtils.hasText(today)) {
      return null;
    }
    Matcher m = INLINE_DATE.matcher(text);
    if (m.find()) {
      return m.replaceFirst(m.group(1) + "：" + today);
    }
    // 「2025年 月 日」→ 完整 today
    if (text.matches(".*\\d{4}\\s*年\\s*月\\s*日.*")) {
      return text.replaceAll("\\d{4}\\s*年\\s*月\\s*日", today);
    }
    if (text.contains("年 月 日") || text.contains("年　月　日")) {
      return text.replace("年 月 日", "年" + extractMd(today)).replace("年　月　日", "年" + extractMd(today));
    }
    return null;
  }

  /** 从 yyyy年MM月dd日 取出 M月d日 */
  private static String extractMd(String today) {
    Matcher m = Pattern.compile("(\\d{4})年(\\d{1,2})月(\\d{1,2})日").matcher(today);
    if (m.find()) {
      return m.group(2) + "月" + m.group(3) + "日";
    }
    return today;
  }

  /**
   * 同格「标签：」后为空时写入。纯标签（无冒号）留给右侧/下方空格。
   *
   * @return 新文本；不适用返回 null
   */
  private static String fillSameCellAfterColon(String text, String label, String value) {
    String trimmed = text.trim();
    if (!trimmed.contains(":") && !trimmed.contains("：")) {
      return null;
    }
    Pattern p =
        Pattern.compile(
            "^\\s*" + Pattern.quote(label) + "\\s*[:：]\\s*$",
            Pattern.CASE_INSENSITIVE);
    if (p.matcher(trimmed).matches()) {
      return label + "：" + value;
    }
    return null;
  }

  private static boolean isBlankPlaceholder(String text) {
    String t = text.trim();
    return t.isEmpty()
        || t.matches("[_＿\\-—/\\\\.\\s　]+")
        || "年 月 日".equals(t)
        || t.matches("\\d{4}年\\s*月\\s*日");
  }

  /**
   * 找标签右侧（其次下方）的值格。优先写入模板已有合并区，不跨出表单列范围；
   * 首个候选值格无论空否都返回（覆盖旧值），避免已填时再溢出到更右的窄列。
   */
  private static Cell findValueCell(Sheet sheet, int rowIdx, int colIdx, int formLastCol) {
    CellRangeAddress labelMerge = findMerge(sheet, rowIdx, colIdx);
    int startCol = labelMerge != null ? labelMerge.getLastColumn() + 1 : colIdx + 1;
    int endCol = Math.min(Math.min(startCol + 8, colIdx + 12), formLastCol);

    Cell right = findValueInRow(sheet, rowIdx, startCol, endCol, formLastCol);
    if (right != null) {
      return right;
    }

    int startRow = labelMerge != null ? labelMerge.getLastRow() + 1 : rowIdx + 1;
    for (int r = startRow; r <= startRow + 2; r++) {
      Cell below = findValueInRow(sheet, r, startCol, endCol, formLastCol);
      if (below != null) {
        return below;
      }
      if (colIdx <= formLastCol) {
        Cell sameCol = getExistingOrCreateWithinForm(sheet, r, colIdx, formLastCol);
        if (sameCol != null && isWritableValueCell(sheet, sameCol, formLastCol)) {
          String t = TrainingXlsxVariableReplacer.cellText(sameCol);
          if (!StringUtils.hasText(t) || isBlankPlaceholder(t) || matchRule(t, Set.of()) == null) {
            return sameCol;
          }
        }
      }
    }
    return null;
  }

  private static Cell findValueInRow(
      Sheet sheet, int rowIdx, int startCol, int endCol, int formLastCol) {
    if (startCol > endCol || startCol > formLastCol) {
      return null;
    }
    for (int c = startCol; c <= endCol; c++) {
      if (isInsideOtherMergeAsNonOrigin(sheet, rowIdx, c)) {
        continue;
      }
      Cell cell = getExistingOrCreateWithinForm(sheet, rowIdx, c, formLastCol);
      if (cell == null || !isWritableValueCell(sheet, cell, formLastCol)) {
        continue;
      }
      String t = TrainingXlsxVariableReplacer.cellText(cell);
      if (StringUtils.hasText(t) && matchRule(t, Set.of()) != null) {
        // 碰到下一个标签，停止向右找
        break;
      }
      // 空或已有内容：都作为本标签值格（覆盖），不再往更右溢
      return cell;
    }
    return null;
  }

  /** 可写入的值格：允许 2～8 列合并值区；拒绝通栏标题/表外列 */
  private static boolean isWritableValueCell(Sheet sheet, Cell cell, int formLastCol) {
    if (cell == null) {
      return false;
    }
    int row = cell.getRowIndex();
    int col = cell.getColumnIndex();
    if (col > formLastCol) {
      return false;
    }
    CellRangeAddress merge = findMerge(sheet, row, col);
    if (merge != null) {
      int width = merge.getLastColumn() - merge.getFirstColumn() + 1;
      int height = merge.getLastRow() - merge.getFirstRow() + 1;
      // 通栏标题/分区空行（接近整表宽）
      if (width >= 10 || (merge.getFirstColumn() == 0 && width >= 8)) {
        return false;
      }
      if (merge.getFirstRow() != row || merge.getFirstColumn() != col) {
        return false;
      }
      if (height > 3) {
        return false;
      }
    }
    String t = TrainingXlsxVariableReplacer.cellText(cell);
    return matchRule(t, Set.of()) == null;
  }

  /** 表单右缘：有合并时以合并最大列为准；无合并的简单模板允许向右扩展写值 */
  private static int formLastColumn(Sheet sheet) {
    int mergeMax = -1;
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      mergeMax = Math.max(mergeMax, region.getLastColumn());
    }
    if (mergeMax >= 0) {
      return mergeMax;
    }
    int cellMax = 0;
    for (Row row : sheet) {
      if (row != null && row.getLastCellNum() > 0) {
        cellMax = Math.max(cellMax, row.getLastCellNum() - 1);
      }
    }
    return Math.max(cellMax + 8, 8);
  }

  private static void clearOutsideForm(Sheet sheet, int formLastCol) {
    for (Row row : sheet) {
      if (row == null) {
        continue;
      }
      short last = row.getLastCellNum();
      if (last < 0) {
        continue;
      }
      for (int c = formLastCol + 1; c < last; c++) {
        Cell cell = row.getCell(c);
        if (cell != null) {
          cell.setBlank();
        }
      }
    }
  }

  private static Cell getExistingOrCreateWithinForm(
      Sheet sheet, int rowIdx, int colIdx, int formLastCol) {
    if (colIdx > formLastCol) {
      return null;
    }
    Row row = sheet.getRow(rowIdx);
    if (row == null) {
      row = sheet.createRow(rowIdx);
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
      cell = row.createCell(colIdx);
    }
    return cell;
  }

  private static CellRangeAddress findMerge(Sheet sheet, int row, int col) {
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      if (region.isInRange(row, col)) {
        return region;
      }
    }
    return null;
  }

  private static boolean isInsideOtherMergeAsNonOrigin(Sheet sheet, int row, int col) {
    CellRangeAddress m = findMerge(sheet, row, col);
    return m != null && (m.getFirstRow() != row || m.getFirstColumn() != col);
  }

  /** 避免遍历 Row 时 createCell 触发 ConcurrentModificationException */
  private static List<Cell> snapshotCells(Row row) {
    List<Cell> cells = new ArrayList<>();
    short last = row.getLastCellNum();
    if (last < 0) {
      return cells;
    }
    for (int c = 0; c < last; c++) {
      Cell cell = row.getCell(c);
      if (cell != null) {
        cells.add(cell);
      }
    }
    return cells;
  }

  /** 解析结果摘要（上传后展示） */
  public static Map<String, String> describeMatches(byte[] xlsxBytes) throws IOException {
    Map<String, String> map = new LinkedHashMap<>();
    for (LabelHit hit : scanHits(xlsxBytes)) {
      map.put(hit.label(), hit.varKey());
    }
    return map;
  }
}
