package com.guoyu.platform.trainingdepartment.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainPlanRosterEntity;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 企业筛选名单 Excel → 待分班池行。
 * 支持：多工作表扫描、标题行偏移、姓名/证件号等常见表头别名。
 */
@Service
public class TrainPlanRosterParseService {

  private static final Set<String> NAME_ALIASES = Set.of(
      "姓名", "员工姓名", "学员姓名", "人员姓名", "名字", "职工姓名", "工人姓名");

  private final DataFormatter formatter = new DataFormatter();

  public List<TrainPlanRosterEntity> parseEnterpriseRoster(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选名单文件为空");
    }
    try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
      if (workbook.getNumberOfSheets() <= 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选名单无有效工作表");
      }

      HeaderHit hit = findBestHeader(workbook);
      if (hit == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "未识别到含「姓名」的表头行。请确认上传的是人员名单（表头含姓名/员工姓名等），"
                + "而非纯排班表；若名单在其他工作表也会自动扫描。");
      }

      Sheet sheet = workbook.getSheetAt(hit.sheetIndex);
      Map<String, Integer> colMap = hit.colMap;
      Integer nameCol = colMap.get("name");
      if (nameCol == null) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "表头缺少姓名列");
      }
      Integer sheetYear = extractYearFromText(sheet.getSheetName());

      List<TrainPlanRosterEntity> rows = new ArrayList<>();
      Set<String> seenKeys = new java.util.HashSet<>();
      for (int r = hit.headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) continue;
        String name = cell(row, nameCol);
        if (!StringUtils.hasText(name)) continue;
        name = name.trim();
        if (isNameHeaderAlias(name) || "示例".equals(name) || "合计".equals(name) || "小计".equals(name)) {
          continue;
        }
        // 跳过分段标题行（如「对外鉴定培训人员：133人」）
        if (looksLikeSectionTitle(name, row, colMap)) {
          continue;
        }

        TrainPlanRosterEntity entity = new TrainPlanRosterEntity();
        entity.setSource("enterprise_roster");
        entity.setPersonName(name);
        entity.setIdCard(trimToNull(cell(row, colMap.get("idCard"))));
        entity.setPhone(trimToNull(cell(row, colMap.get("phone"))));
        entity.setJobName(trimToNull(cell(row, colMap.get("job"))));
        entity.setLevelName(trimToNull(cell(row, colMap.get("level"))));
        entity.setWorkshop(trimToNull(cell(row, colMap.get("workshop"))));
        entity.setShiftTeam(trimToNull(cell(row, colMap.get("shiftTeam"))));
        Integer rowYear = parseYearCell(cell(row, colMap.get("year")));
        entity.setPlanYear(rowYear != null ? rowYear : sheetYear);
        entity.setAssignStatus(0);
        entity.setDeleted(0);

        String dedupeKey = StringUtils.hasText(entity.getIdCard())
            ? "ID:" + entity.getIdCard().trim().toUpperCase(Locale.ROOT)
            : "N:" + name;
        if (!seenKeys.add(dedupeKey)) {
          continue;
        }
        rows.add(entity);
      }
      if (rows.isEmpty()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "在工作表「" + sheet.getSheetName() + "」识别到表头，但未解析到有效人员行");
      }
      return rows;
    } catch (BusinessException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "解析筛选名单失败：" + ex.getMessage());
    }
  }

  private HeaderHit findBestHeader(Workbook workbook) {
    HeaderHit best = null;
    for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
      Sheet sheet = workbook.getSheetAt(s);
      if (sheet == null) continue;
      String sheetName = sheet.getSheetName() == null ? "" : sheet.getSheetName();
      // 排班类工作表通常无人员名单，降低优先级但仍扫描（防止误判）
      boolean scheduleLike = sheetName.contains("排班") || sheetName.contains("倒班")
          || sheetName.matches(".*\\d+月.*");

      int maxScan = Math.min(Math.max(sheet.getLastRowNum(), 0), 40);
      for (int i = 0; i <= maxScan; i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;
        Map<String, Integer> colMap = mapColumns(row);
        if (!colMap.containsKey("name")) continue;
        int score = scoreHeader(colMap, scheduleLike, sheetName);
        if (best == null || score > best.score) {
          best = new HeaderHit(s, i, colMap, score);
        }
        // 已有姓名+证件号的高质量表头，可提前采用
        if (score >= 50) {
          return best;
        }
      }
    }
    return best;
  }

  private static int scoreHeader(Map<String, Integer> colMap, boolean scheduleLike, String sheetName) {
    int score = 10;
    if (colMap.containsKey("idCard")) score += 30;
    if (colMap.containsKey("job")) score += 8;
    if (colMap.containsKey("level")) score += 6;
    if (colMap.containsKey("workshop")) score += 6;
    if (colMap.containsKey("phone")) score += 4;
    if (colMap.containsKey("shiftTeam")) score += 2;
    if (sheetName.contains("名单") || sheetName.contains("明细") || sheetName.contains("人员")
        || sheetName.contains("报名")) {
      score += 15;
    }
    if (scheduleLike) score -= 20;
    return score;
  }

  private Map<String, Integer> mapColumns(Row header) {
    Map<String, Integer> map = new HashMap<>();
    if (header == null) return map;
    short last = header.getLastCellNum();
    if (last < 0) return map;
    for (int c = 0; c < last; c++) {
      String raw = formatter.formatCellValue(header.getCell(c));
      if (!StringUtils.hasText(raw)) continue;
      // 合并单元格/换行表头
      String h = raw.trim().replace("\n", "").replace("\r", "").toLowerCase(Locale.ROOT);
      if (isNameHeaderAlias(h) && !map.containsKey("name")) {
        map.put("name", c);
      } else if ((h.contains("身份证") || h.contains("证件号") || h.contains("证件号码") || h.equals("证号"))
          && !map.containsKey("idCard")) {
        map.put("idCard", c);
      } else if ((h.contains("手机") || h.contains("电话") || h.contains("联系方式") || h.contains("联系电话"))
          && !map.containsKey("phone")) {
        map.put("phone", c);
      } else if ((h.contains("工种") || h.contains("职业") || h.contains("岗位")) && !map.containsKey("job")) {
        map.put("job", c);
      } else if ((h.contains("等级") || h.contains("级别") || h.contains("技能等级")) && !map.containsKey("level")) {
        map.put("level", c);
      } else if ((h.contains("车间") || h.contains("工段") || h.contains("部门") || h.contains("单位")
          || h.contains("所在单位") || h.contains("工作单位"))
          && !map.containsKey("workshop")) {
        map.put("workshop", c);
      } else if ((h.contains("班组") || h.equals("班别") || h.contains("倒班班"))
          && !map.containsKey("shiftTeam")) {
        map.put("shiftTeam", c);
      } else if ((h.contains("规划年") || h.contains("培训年") || h.equals("年份") || h.equals("年度")
          || h.contains("计划年"))
          && !map.containsKey("year")) {
        map.put("year", c);
      }
    }
    // 备注列常放生产班组（来铝样例）；仅当尚无班组列时启用
    if (!map.containsKey("shiftTeam")) {
      for (int c = 0; c < last; c++) {
        String raw = formatter.formatCellValue(header.getCell(c));
        if (raw != null && raw.trim().replace("\n", "").contains("备注")) {
          map.put("shiftTeam", c);
          break;
        }
      }
    }
    return map;
  }

  private boolean looksLikeSectionTitle(String name, Row row, Map<String, Integer> colMap) {
    if (name.contains("人员") && (name.contains("：") || name.contains(":"))) return true;
    if (name.endsWith("人") && name.length() > 8) return true;
    // 姓名列有值但身份证列像标题（空且同行几乎无其他字段）
    Integer idCol = colMap.get("idCard");
    if (idCol != null) {
      String id = cell(row, idCol);
      if (!StringUtils.hasText(id) && name.length() > 12) return true;
    }
    return false;
  }

  private static boolean isNameHeaderAlias(String raw) {
    if (!StringUtils.hasText(raw)) return false;
    String t = raw.trim().replace("\n", "").replace("\r", "");
    if (NAME_ALIASES.contains(t)) return true;
    // 「姓名」夹在其他字中间，避免「姓名所在单位」误判；优先精确再包含
    return t.equals("姓名") || t.endsWith("姓名") || t.startsWith("姓名")
        || (t.contains("姓名") && t.length() <= 6);
  }

  private String cell(Row row, Integer col) {
    if (col == null || row == null) return null;
    Cell cell = row.getCell(col);
    if (cell == null) return null;
    return formatter.formatCellValue(cell);
  }

  private String trimToNull(String v) {
    if (!StringUtils.hasText(v)) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  /** 从工作表名等文本提取 20xx 年份（如「明细_2026」）。 */
  static Integer extractYearFromText(String text) {
    if (!StringUtils.hasText(text)) return null;
    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})").matcher(text);
    if (m.find()) {
      try {
        return Integer.parseInt(m.group(1));
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private Integer parseYearCell(String raw) {
    if (!StringUtils.hasText(raw)) return null;
    String t = raw.trim();
    Integer fromText = extractYearFromText(t);
    if (fromText != null) return fromText;
    try {
      double d = Double.parseDouble(t);
      int y = (int) d;
      if (y >= 2000 && y <= 2100) return y;
    } catch (NumberFormatException ignored) {
      // ignore
    }
    return null;
  }

  private static final class HeaderHit {
    final int sheetIndex;
    final int headerRowIdx;
    final Map<String, Integer> colMap;
    final int score;

    HeaderHit(int sheetIndex, int headerRowIdx, Map<String, Integer> colMap, int score) {
      this.sheetIndex = sheetIndex;
      this.headerRowIdx = headerRowIdx;
      this.colMap = colMap;
      this.score = score;
    }
  }
}
