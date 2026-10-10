package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 人社《职业技能等级认定成绩花名册》及扁平成绩模板解析。
 * 不与分班 {@link TrainPlanRosterParseService} 混用。
 */
@Service
public class RensheScoreRosterParseService {

  public static final String LAYOUT_RENSHE = "RENSHE_ROSTER";
  public static final String LAYOUT_FLAT = "FLAT_TEMPLATE";
  public static final int MAX_ROWS = 5000;

  private static final Set<String> EMPTY_SCORE_TOKENS = Set.of(
      "—", "–", "-", "/", "／", "无", "空", "n/a", "na", "\\");
  private static final Set<String> NAME_ALIASES = Set.of("姓名", "学员姓名", "考生姓名");
  private static final DataFormatter FORMATTER = new DataFormatter(Locale.CHINA);

  public ParsedRoster parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩单文件为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      if (workbook.getNumberOfSheets() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "成绩单无有效工作表");
      }
      HeaderHit hit = findBestHeader(workbook);
      if (hit == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "未识别到含「姓名」和「证件号码/身份证」的表头。请上传人社成绩花名册或标准模板。");
      }
      Sheet sheet = workbook.getSheetAt(hit.sheetIndex);
      Meta meta = extractMeta(sheet);
      String layout = resolveLayout(hit, meta);

      List<ParsedRow> rows = new ArrayList<>();
      int last = Math.max(sheet.getLastRowNum(), 0);
      for (int r = hit.dataStartRow; r <= last; r++) {
        if (rows.size() >= MAX_ROWS) {
          break;
        }
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        ParsedRow parsed = readDataRow(sheet, row, hit.colMap, r + 1);
        if (parsed == null) {
          continue;
        }
        rows.add(parsed);
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "识别到表头，但未解析到有效人员行");
      }
      return new ParsedRoster(layout, meta.planName, meta.planCode, meta.examTimeText, rows);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析成绩单失败：" + ex.getMessage());
    }
  }

  private static String resolveLayout(HeaderHit hit, Meta meta) {
    if (StringUtils.hasText(meta.planCode) || StringUtils.hasText(meta.planName) || hit.twoRowHeader) {
      return LAYOUT_RENSHE;
    }
    return LAYOUT_FLAT;
  }

  private HeaderHit findBestHeader(Workbook workbook) {
    HeaderHit best = null;
    for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
      Sheet sheet = workbook.getSheetAt(s);
      if (sheet == null) {
        continue;
      }
      int maxScan = Math.min(Math.max(sheet.getLastRowNum(), 0), 40);
      for (int i = 0; i <= maxScan; i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        Map<String, Integer> colMap = mapColumns(sheet, row);
        Row next = i + 1 <= maxScan ? sheet.getRow(i + 1) : null;
        Map<String, Integer> subMap = mapColumns(sheet, next);
        boolean subheader = !subMap.containsKey("name")
            && (subMap.containsKey("theory") || subMap.containsKey("skill")
            || subMap.containsKey("comprehensive") || subMap.containsKey("performance"));
        Map<String, Integer> merged = new HashMap<>(colMap);
        if (subheader) {
          subMap.forEach(merged::putIfAbsent);
        }
        if (!merged.containsKey("name") || !merged.containsKey("idCard")) {
          continue;
        }
        int score = 20;
        if (merged.containsKey("comprehensive")) {
          score += 20;
        }
        if (merged.containsKey("theory")) {
          score += 8;
        }
        if (merged.containsKey("skill")) {
          score += 8;
        }
        if (merged.containsKey("ticket")) {
          score += 6;
        }
        if (subheader) {
          score += 15;
        }
        String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName();
        if (sheetName.contains("成绩") || sheetName.contains("花名册") || sheetName.contains("认定")) {
          score += 10;
        }
        int dataStart = subheader ? i + 2 : i + 1;
        if (best == null || score > best.score) {
          best = new HeaderHit(s, i, dataStart, merged, score, subheader);
        }
        if (score >= 55) {
          return best;
        }
      }
    }
    return best;
  }

  private Map<String, Integer> mapColumns(Sheet sheet, Row header) {
    Map<String, Integer> map = new HashMap<>();
    if (header == null) {
      return map;
    }
    int last = Math.max(header.getLastCellNum(), 0);
    for (int c = 0; c < last; c++) {
      String raw = cell(sheet, header, c);
      if (!StringUtils.hasText(raw)) {
        continue;
      }
      String h = raw.trim().replace("\n", "").replace("\r", "").replace(" ", "");
      String lower = h.toLowerCase(Locale.ROOT);
      if (isNameHeader(h) && !map.containsKey("name")) {
        map.put("name", c);
      } else if (isIdCardHeader(h) && !map.containsKey("idCard")) {
        map.put("idCard", c);
      } else if ((h.contains("准考证")) && !map.containsKey("ticket")) {
        map.put("ticket", c);
      } else if ((h.contains("职业工种") || (h.contains("工种") && !h.contains("等级")))
          && !map.containsKey("job")) {
        map.put("job", c);
      } else if ((h.contains("技能等级") || h.equals("等级") || h.contains("级别"))
          && !map.containsKey("level")) {
        map.put("level", c);
      } else if (isComprehensiveHeader(h) && !map.containsKey("comprehensive")) {
        map.put("comprehensive", c);
      } else if (isTheoryHeader(h) && !map.containsKey("theory")) {
        map.put("theory", c);
      } else if (isSkillHeader(h) && !map.containsKey("skill")) {
        map.put("skill", c);
      } else if ((h.contains("工作业绩") || h.equals("业绩")) && !map.containsKey("performance")) {
        map.put("performance", c);
      } else if (lower.contains("idcard") && !map.containsKey("idCard")) {
        map.put("idCard", c);
      }
    }
    return map;
  }

  private static boolean isNameHeader(String h) {
    if (!StringUtils.hasText(h)) {
      return false;
    }
    if (NAME_ALIASES.contains(h)) {
      return true;
    }
    return h.equals("姓名") || (h.contains("姓名") && h.length() <= 6);
  }

  private static boolean isIdCardHeader(String h) {
    return h.equals("证件号码") || h.equals("证件号") || h.equals("证号")
        || h.equals("身份证") || h.equals("身份证号") || h.equals("身份证号码")
        || h.contains("证件号码");
  }

  private static boolean isComprehensiveHeader(String h) {
    if (h.contains("考核成绩")) {
      return false;
    }
    return h.equals("综合") || h.contains("综合成绩") || h.equals("总评") || h.contains("认定成绩");
  }

  private static boolean isTheoryHeader(String h) {
    return h.equals("理论") || h.contains("理论成绩") || h.equals("笔试");
  }

  private static boolean isSkillHeader(String h) {
    if (h.contains("技能等级")) {
      return false;
    }
    return h.equals("技能") || h.contains("技能成绩") || h.equals("实操") || h.equals("操作");
  }

  private Meta extractMeta(Sheet sheet) {
    Meta meta = new Meta();
    int maxScan = Math.min(Math.max(sheet.getLastRowNum(), 0), 30);
    for (int r = 0; r <= maxScan; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      int last = Math.max(row.getLastCellNum(), 0);
      for (int c = 0; c < last; c++) {
        String raw = cell(sheet, row, c);
        if (!StringUtils.hasText(raw)) {
          continue;
        }
        harvestMeta(meta, raw, sheet, row, c);
      }
    }
    return meta;
  }

  private void harvestMeta(Meta meta, String raw, Sheet sheet, Row row, int col) {
    String t = raw.replace("：", ":").replace("\n", "").trim();
    if (!StringUtils.hasText(meta.planName) && t.contains("计划名称")) {
      meta.planName = valueAfterLabel(t, "计划名称", sheet, row, col);
    }
    if (!StringUtils.hasText(meta.planCode) && t.contains("计划编码")) {
      meta.planCode = valueAfterLabel(t, "计划编码", sheet, row, col);
    }
    if (!StringUtils.hasText(meta.examTimeText) && (t.contains("考试时间") || t.contains("考核时间"))) {
      String key = t.contains("考试时间") ? "考试时间" : "考核时间";
      meta.examTimeText = valueAfterLabel(t, key, sheet, row, col);
    }
  }

  private String valueAfterLabel(String text, String key, Sheet sheet, Row row, int col) {
    int idx = text.indexOf(key);
    if (idx < 0) {
      return null;
    }
    String rest = text.substring(idx + key.length()).replaceFirst("^[:：\\s]+", "").trim();
    if (StringUtils.hasText(rest) && rest.length() < 200) {
      return rest;
    }
    String next = cell(sheet, row, col + 1);
    return StringUtils.hasText(next) ? next.trim() : null;
  }

  private ParsedRow readDataRow(Sheet sheet, Row row, Map<String, Integer> colMap, int rowNo) {
    String name = trimToNull(cell(sheet, row, colMap.get("name")));
    String idRaw = trimToNull(cell(sheet, row, colMap.get("idCard")));
    String ticket = trimToNull(cell(sheet, row, colMap.get("ticket")));
    if (!StringUtils.hasText(name) && !StringUtils.hasText(idRaw) && !StringUtils.hasText(ticket)) {
      return null;
    }
    if (name != null && (isNameHeader(name) || "合计".equals(name) || "小计".equals(name) || "序号".equals(name))) {
      return null;
    }
    String idCard = normalizeIdCard(idRaw);
    return new ParsedRow(
        rowNo,
        name,
        idCard,
        idRaw,
        ticket,
        trimToNull(cell(sheet, row, colMap.get("job"))),
        trimToNull(cell(sheet, row, colMap.get("level"))),
        parseScore(cell(sheet, row, colMap.get("theory"))),
        parseScore(cell(sheet, row, colMap.get("skill"))),
        parseScore(cell(sheet, row, colMap.get("comprehensive"))),
        parseScore(cell(sheet, row, colMap.get("performance"))),
        null,
        null,
        null,
        0,
        null,
        false);
  }

  static String normalizeIdCard(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().replace(" ", "").replace("\u3000", "").toUpperCase(Locale.ROOT);
    if (t.matches("\\d{17}[0-9X]")) {
      return t;
    }
    return null;
  }

  static BigDecimal parseScore(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().replace(" ", "").replace("\u3000", "");
    if (EMPTY_SCORE_TOKENS.contains(t) || EMPTY_SCORE_TOKENS.contains(t.toLowerCase(Locale.ROOT))) {
      return null;
    }
    t = t.replace(",", "");
    try {
      return new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String cell(Sheet sheet, Row row, Integer col) {
    if (col == null || row == null || sheet == null) {
      return null;
    }
    return cell(sheet, row, col.intValue());
  }

  private String cell(Sheet sheet, Row row, int col) {
    if (row == null) {
      return null;
    }
    Cell cell = row.getCell(col);
    String direct = formatCell(cell);
    if (StringUtils.hasText(direct)) {
      return direct;
    }
    int merged = sheet.getNumMergedRegions();
    for (int i = 0; i < merged; i++) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      if (region.isInRange(row.getRowNum(), col)) {
        Row first = sheet.getRow(region.getFirstRow());
        if (first == null) {
          return null;
        }
        return formatCell(first.getCell(region.getFirstColumn()));
      }
    }
    return null;
  }

  private String formatCell(Cell cell) {
    if (cell == null) {
      return null;
    }
    CellType type = cell.getCellType();
    if (type == CellType.FORMULA) {
      type = cell.getCachedFormulaResultType();
    }
    if (type == CellType.NUMERIC) {
      double n = cell.getNumericCellValue();
      if (Math.abs(n - Math.rint(n)) < 1e-9 && Math.abs(n) >= 1e14) {
        return BigDecimal.valueOf(n).toPlainString().split("\\.")[0];
      }
      return FORMATTER.formatCellValue(cell).trim();
    }
    String v = FORMATTER.formatCellValue(cell);
    return v == null ? null : v.trim();
  }

  private static String trimToNull(String v) {
    if (!StringUtils.hasText(v)) {
      return null;
    }
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  public record ParsedRoster(
      String layout,
      String planName,
      String planCode,
      String examTimeText,
      List<ParsedRow> rows) {}

  public record ParsedRow(
      int rowNo,
      String name,
      String idCard,
      String idCardRaw,
      String ticketNo,
      String jobName,
      String levelName,
      BigDecimal theory,
      BigDecimal skill,
      BigDecimal comprehensive,
      BigDecimal performance,
      /** 页头考试时间原文 */
      String examTimeText,
      String planCode,
      String planName,
      /** 多文件上传时的文件序号（0-based） */
      int fileIndex,
      /** 合并后说明，如「合并2场」 */
      String mergeNote,
      boolean examTimeMissing) {

    /** 解析后尚未打上场次元数据时的便捷构造 */
    public static ParsedRow ofScores(
        int rowNo,
        String name,
        String idCard,
        String idCardRaw,
        String ticketNo,
        String jobName,
        String levelName,
        BigDecimal theory,
        BigDecimal skill,
        BigDecimal comprehensive,
        BigDecimal performance) {
      return new ParsedRow(
          rowNo, name, idCard, idCardRaw, ticketNo, jobName, levelName,
          theory, skill, comprehensive, performance,
          null, null, null, 0, null, false);
    }

    public ParsedRow withSessionMeta(
        String examTimeText, String planCode, String planName, int fileIndex, int rowNo) {
      boolean missing = !StringUtils.hasText(examTimeText);
      return new ParsedRow(
          rowNo,
          name,
          idCard,
          idCardRaw,
          ticketNo,
          jobName,
          levelName,
          theory,
          skill,
          comprehensive,
          performance,
          examTimeText,
          planCode,
          planName,
          fileIndex,
          null,
          missing);
    }

    public ParsedRow withMergeResult(String mergeNote, boolean examTimeMissing) {
      return new ParsedRow(
          rowNo,
          name,
          idCard,
          idCardRaw,
          ticketNo,
          jobName,
          levelName,
          theory,
          skill,
          comprehensive,
          performance,
          examTimeText,
          planCode,
          planName,
          fileIndex,
          mergeNote,
          examTimeMissing);
    }
  }

  private static final class Meta {
    String planName;
    String planCode;
    String examTimeText;
  }

  private static final class HeaderHit {
    final int sheetIndex;
    final int headerRowIdx;
    final int dataStartRow;
    final Map<String, Integer> colMap;
    final int score;
    final boolean twoRowHeader;

    HeaderHit(int sheetIndex, int headerRowIdx, int dataStartRow, Map<String, Integer> colMap,
        int score, boolean twoRowHeader) {
      this.sheetIndex = sheetIndex;
      this.headerRowIdx = headerRowIdx;
      this.dataStartRow = dataStartRow;
      this.colMap = colMap;
      this.score = score;
      this.twoRowHeader = twoRowHeader;
    }
  }
}
