package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
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
 * 企业《新型学徒制培训学员花名册》解析。
 * 不与报名扁平模板、分班名单、人社成绩花名册混用。
 */
@Service
public class ClassPrepRosterParseService {

  public static final int MAX_ROWS = 500;

  private static final Set<String> NAME_SKIP = Set.of("合计", "小计", "序号", "姓名", "示例");
  private static final DataFormatter FORMATTER = new DataFormatter(Locale.CHINA);

  public ParsedRoster parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "花名册文件为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      if (workbook.getNumberOfSheets() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "花名册无有效工作表");
      }
      HeaderHit hit = findBestHeader(workbook);
      if (hit == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "未识别到学徒制花名册表头。请上传《新型学徒制培训学员花名册》（含姓名、身份证号码，以及社保号/劳动合同/人员类别）。");
      }
      Sheet sheet = workbook.getSheetAt(hit.sheetIndex);
      Meta meta = extractMeta(sheet, hit.headerRowIdx);

      List<ParsedRow> rows = new ArrayList<>();
      int last = Math.max(sheet.getLastRowNum(), 0);
      for (int r = hit.headerRowIdx + 1; r <= last; r++) {
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
        throw new BusinessException(ErrorCode.BAD_REQUEST, "识别到表头，但未解析到有效人员行");
      }
      return new ParsedRoster(
          meta.title, meta.company, meta.trainingOrg, meta.classPeriod, meta.classPeriodNo, rows);
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel解析失败");
    }
  }

  private HeaderHit findBestHeader(Workbook workbook) {
    HeaderHit best = null;
    int sheets = workbook.getNumberOfSheets();
    for (int s = 0; s < sheets; s++) {
      Sheet sheet = workbook.getSheetAt(s);
      int last = Math.min(Math.max(sheet.getLastRowNum(), 0), 40);
      for (int i = 0; i <= last; i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          continue;
        }
        Map<String, Integer> colMap = mapColumns(sheet, row);
        if (!colMap.containsKey("name") || !colMap.containsKey("idCard")) {
          continue;
        }
        if (!colMap.containsKey("socialSecurity")
            && !colMap.containsKey("contract")
            && !colMap.containsKey("personCategory")) {
          continue;
        }
        int score = colMap.size();
        if (best == null || score > best.score) {
          best = new HeaderHit(s, i, score, colMap);
        }
        if (score >= 8) {
          return best;
        }
      }
    }
    return best;
  }

  private Map<String, Integer> mapColumns(Sheet sheet, Row header) {
    Map<String, Integer> map = new HashMap<>();
    int last = Math.max(header.getLastCellNum(), 0);
    for (int c = 0; c < last; c++) {
      String raw = cell(sheet, header, c);
      if (!StringUtils.hasText(raw)) {
        continue;
      }
      String h = compactHeader(raw);
      if (isNameHeader(h) && !map.containsKey("name")) {
        map.put("name", c);
      } else if (isIdCardHeader(h) && !map.containsKey("idCard")) {
        map.put("idCard", c);
      } else if ((h.equals("性别") || h.contains("性别")) && !map.containsKey("gender")) {
        map.put("gender", c);
      } else if (h.contains("民族") && !map.containsKey("nation")) {
        map.put("nation", c);
      } else if (h.contains("学历") && !map.containsKey("education")) {
        map.put("education", c);
      } else if (h.contains("政治面貌") && !map.containsKey("political")) {
        map.put("political", c);
      } else if ((h.contains("家庭住址") || h.equals("住址") || h.contains("联系地址"))
          && !map.containsKey("address")) {
        map.put("address", c);
      } else if (isPhoneHeader(h) && !map.containsKey("phone")) {
        map.put("phone", c);
      } else if (isSocialHeader(h) && !map.containsKey("socialSecurity")) {
        map.put("socialSecurity", c);
      } else if (isContractHeader(h) && !map.containsKey("contract")) {
        map.put("contract", c);
      } else if (isPersonCategoryHeader(h) && !map.containsKey("personCategory")) {
        map.put("personCategory", c);
      } else if (h.equals("备注") && !map.containsKey("remark")) {
        map.put("remark", c);
      }
    }
    return map;
  }

  private static String compactHeader(String raw) {
    return raw.trim()
        .replace("\n", "")
        .replace("\r", "")
        .replace(" ", "")
        .replace("（", "(")
        .replace("）", ")");
  }

  private static boolean isNameHeader(String h) {
    return h.equals("姓名") || h.equals("学员姓名") || h.equals("员工姓名")
        || (h.contains("姓名") && h.length() <= 6);
  }

  private static boolean isIdCardHeader(String h) {
    return h.contains("身份证") || h.contains("证件号码") || h.equals("证件号") || h.equals("证号");
  }

  private static boolean isPhoneHeader(String h) {
    return h.contains("联系电话") || h.contains("手机") || h.contains("联系方式") || h.equals("电话");
  }

  private static boolean isSocialHeader(String h) {
    return h.contains("社保");
  }

  private static boolean isContractHeader(String h) {
    return h.contains("劳动合同") || (h.contains("合同") && (h.contains("签订") || h.contains("起") || h.contains("止")));
  }

  private static boolean isPersonCategoryHeader(String h) {
    return h.contains("培训人员类别") || h.contains("人员类别") || h.contains("人员类型");
  }

  private Meta extractMeta(Sheet sheet, int headerRowIdx) {
    Meta meta = new Meta();
    int last = Math.min(headerRowIdx, Math.max(sheet.getLastRowNum(), 0));
    for (int r = 0; r <= last; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      int cols = Math.max(row.getLastCellNum(), 0);
      for (int c = 0; c < cols; c++) {
        String raw = cell(sheet, row, c);
        if (!StringUtils.hasText(raw)) {
          continue;
        }
        String t = raw.replace("：", ":").replace("\n", "").trim();
        if (!StringUtils.hasText(meta.title) && t.contains("花名册") && t.length() <= 40) {
          meta.title = t;
        }
        if (!StringUtils.hasText(meta.company) && t.contains("企业")) {
          meta.company = valueAfterLabel(t, "企业", sheet, row, c);
        }
        if (!StringUtils.hasText(meta.trainingOrg) && t.contains("合作培训机构")) {
          meta.trainingOrg = valueAfterLabel(t, "合作培训机构", sheet, row, c);
        }
        if (!StringUtils.hasText(meta.classPeriod) && t.contains("班期") && !t.contains("编号")) {
          meta.classPeriod = valueAfterLabel(t, "班期", sheet, row, c);
        }
        if (!StringUtils.hasText(meta.classPeriodNo) && (t.contains("班期编号") || t.contains("班期号"))) {
          String key = t.contains("班期编号") ? "班期编号" : "班期号";
          meta.classPeriodNo = valueAfterLabel(t, key, sheet, row, c);
        }
      }
    }
    return meta;
  }

  private String valueAfterLabel(String text, String key, Sheet sheet, Row row, int col) {
    int idx = text.indexOf(key);
    if (idx < 0) {
      return null;
    }
    String rest = text.substring(idx + key.length()).replaceFirst("^[:：\\s]+", "").trim();
    if (StringUtils.hasText(rest) && rest.length() < 200 && !isBareLabel(rest)) {
      return rest;
    }
    int last = Math.max(row.getLastCellNum(), col + 6);
    for (int c = col + 1; c < last && c <= col + 6; c++) {
      String next = cell(sheet, row, c);
      if (StringUtils.hasText(next) && !isBareLabel(next)) {
        return next.trim();
      }
    }
    return null;
  }

  private static boolean isBareLabel(String raw) {
    String t = raw.replace("：", ":").replace(" ", "").trim();
    if (t.endsWith(":")) {
      t = t.substring(0, t.length() - 1);
    }
    return t.equals("企业") || t.equals("合作培训机构") || t.equals("班期")
        || t.equals("班期编号") || t.equals("班期号") || t.equals("编号");
  }

  private ParsedRow readDataRow(Sheet sheet, Row row, Map<String, Integer> colMap, int rowNo) {
    String name = trimToNull(cell(sheet, row, colMap.get("name")));
    if (!StringUtils.hasText(name) || isSkipName(name)) {
      return null;
    }
    String seq = trimToNull(cell(sheet, row, 0));
    if (looksLikeSummary(name, seq)) {
      return null;
    }
    String idRaw = trimToNull(cell(sheet, row, colMap.get("idCard")));
    String phoneRaw = trimToNull(cell(sheet, row, colMap.get("phone")));
    String contractRaw = trimToNull(cell(sheet, row, colMap.get("contract")));
    ContractDates contract = parseContract(contractRaw);
    return new ParsedRow(
        rowNo,
        name.trim(),
        trimToNull(cell(sheet, row, colMap.get("gender"))),
        trimToNull(cell(sheet, row, colMap.get("nation"))),
        trimToNull(cell(sheet, row, colMap.get("education"))),
        trimToNull(cell(sheet, row, colMap.get("political"))),
        normalizeIdCard(idRaw),
        idRaw,
        trimToNull(cell(sheet, row, colMap.get("address"))),
        normalizePhone(phoneRaw),
        phoneRaw,
        trimToNull(cell(sheet, row, colMap.get("socialSecurity"))),
        contractRaw,
        contract.start(),
        contract.end(),
        contract.workTimeFallback(),
        trimToNull(cell(sheet, row, colMap.get("personCategory"))),
        trimToNull(cell(sheet, row, colMap.get("remark"))));
  }

  static boolean isSkipName(String name) {
    String t = name.trim();
    return NAME_SKIP.contains(t) || t.contains("姓名");
  }

  static boolean looksLikeSummary(String name, String seq) {
    String t = name == null ? "" : name.trim();
    if (t.contains("占总人数") || t.contains("合计") || t.contains("小计")) {
      return true;
    }
    if (t.startsWith("女性") || t.startsWith("男性")) {
      return true;
    }
    String s = seq == null ? "" : seq.trim();
    return s.contains("占总人数") || s.startsWith("女性") || s.startsWith("男性");
  }

  public static ContractDates parseContract(String raw) {
    if (!StringUtils.hasText(raw)) {
      return ContractDates.empty();
    }
    String t = raw.trim()
        .replace("～", "至")
        .replace("~", "至")
        .replace("—", "至")
        .replace("–", "至")
        .replace("到", "至");
    int idx = t.indexOf("至");
    if (idx > 0) {
      String start = trimToNull(t.substring(0, idx));
      String endRaw = t.substring(idx + 1).trim();
      String end = !StringUtils.hasText(endRaw) || endRaw.contains("无固定") ? null : trimToNull(endRaw);
      return new ContractDates(start, end, null);
    }
    if (looksLikeDate(t)) {
      return new ContractDates(t, null, null);
    }
    return new ContractDates(null, null, t);
  }

  private static boolean looksLikeDate(String t) {
    return t.matches("\\d{4}[-/.年]\\d{1,2}[-/.月]\\d{1,2}日?");
  }

  public static String normalizeIdCard(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().replace(" ", "").replace("\u00a0", "").toUpperCase(Locale.ROOT);
    if (t.matches("\\d+\\.0")) {
      t = t.substring(0, t.indexOf('.'));
    }
    return t.isEmpty() ? null : t;
  }

  public static String normalizePhone(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().replace(" ", "").replace("-", "");
    if (t.endsWith(".0") && t.substring(0, t.length() - 2).matches("\\d+")) {
      t = t.substring(0, t.length() - 2);
    }
    return t.isEmpty() ? null : t;
  }

  private String cell(Sheet sheet, Row row, Integer col) {
    if (col == null || row == null) {
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
      if (Math.abs(n - Math.rint(n)) < 1e-9 && Math.abs(n) >= 1e10) {
        return BigDecimal.valueOf(n).toPlainString().split("\\.")[0];
      }
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
      String title,
      String company,
      String trainingOrg,
      String classPeriod,
      String classPeriodNo,
      List<ParsedRow> rows) {}

  public record ParsedRow(
      int rowNo,
      String name,
      String gender,
      String nation,
      String education,
      String political,
      String idCard,
      String idCardRaw,
      String address,
      String phone,
      String phoneRaw,
      String socialSecurity,
      String contractRaw,
      String workDateStart,
      String workDateEnd,
      String workTime,
      String personCategory,
      String remark) {}

  public record ContractDates(String start, String end, String workTimeFallback) {
    static ContractDates empty() {
      return new ContractDates(null, null, null);
    }
  }

  private static final class Meta {
    String title;
    String company;
    String trainingOrg;
    String classPeriod;
    String classPeriodNo;
  }

  private static final class HeaderHit {
    final int sheetIndex;
    final int headerRowIdx;
    final int score;
    final Map<String, Integer> colMap;

    HeaderHit(int sheetIndex, int headerRowIdx, int score, Map<String, Integer> colMap) {
      this.sheetIndex = sheetIndex;
      this.headerRowIdx = headerRowIdx;
      this.score = score;
      this.colMap = colMap;
    }
  }
}
