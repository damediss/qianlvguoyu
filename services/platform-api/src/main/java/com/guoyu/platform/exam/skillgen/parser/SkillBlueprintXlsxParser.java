package com.guoyu.platform.exam.skillgen.parser;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult;
import com.guoyu.platform.exam.skillgen.dto.SkillBlueprintParseResult.NodeRow;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 《操作技能考评要素细目表》xlsx 解析器（POI）。
 *
 * <p>样例画像（验收基准，方案 §2.5）：1 sheet，28 行 × 13 列，73 处合并单元格：
 * R1 标题、R2 职业/级别、R3 块表头（考评范围一级|二级|考评点）、R4 子列表头、数据自 R5 起、页脚"填表人/审表人"截断。
 * 列 A=一级代码⏎（X:Y:Z）| B=一级名称 | C=一级考评比重 | D=一级选考方式 |
 * E=二级代码⏎（X:Y:Z）| F=二级名称 | G=技能要求 | H=二级考评比重 | I=二级选考方式 |
 * J=考评点代码 | K=考评点名称 | L=重要程度 | M=试题量。
 *
 * <p>合并单元格值向下填充；表头按"考评范围一级"关键字定位（不硬编码行号，容忍前导行差异）。
 */
@Component
public class SkillBlueprintXlsxParser {

  private static final int COL_L1_CODE_XYZ = 0;
  private static final int COL_L1_NAME = 1;
  private static final int COL_L1_WEIGHT = 2;
  private static final int COL_L1_SELECT = 3;
  private static final int COL_L2_CODE_XYZ = 4;
  private static final int COL_L2_NAME = 5;
  private static final int COL_L2_SKILL = 6;
  private static final int COL_L2_WEIGHT = 7;
  private static final int COL_L2_SELECT = 8;
  private static final int COL_POINT_CODE = 9;
  private static final int COL_POINT_NAME = 10;
  private static final int COL_POINT_IMPORTANCE = 11;
  private static final int COL_POINT_QUOTA = 12;

  /** 表头定位扫描上限（前 15 行内必须出现"考评范围一级"） */
  private static final int HEADER_SCAN_ROWS = 15;

  public SkillBlueprintParseResult parse(InputStream in, String fileName) {
    SkillBlueprintParseResult result = new SkillBlueprintParseResult();
    result.setFileName(fileName);
    List<NodeRow> rows = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    try (Workbook wb = new XSSFWorkbook(in)) {
      if (wb.getNumberOfSheets() < 1) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel 文件无工作表");
      }
      Sheet sheet = wb.getSheetAt(0);
      Map<String, String> merged = collectMergedValues(sheet);

      result.setWorkTypeTitle(firstNonBlank(cellTextAt(sheet, 0, 0, merged), cellTextAt(sheet, 1, 0, merged)));

      int headerBlockRow = locateBlockHeaderRow(sheet, merged);
      if (headerBlockRow < 0) {
        throw new BusinessException(ErrorCode.BAD_REQUEST,
            "未找到表头（缺少含「考评范围一级」的表头行），请确认上传的是操作技能考评要素细目表");
      }
      int dataStart = headerBlockRow + 2;

      int lastRow = sheet.getLastRowNum();
      String l1Raw = null;
      String l2Raw = null;
      int l1Sort = 0;
      int l2Sort = 0;
      int pointSort = 0;
      int l1Idx = -1;
      int l2Idx = -1;
      boolean sawPoint = false;

      for (int r = dataStart; r <= lastRow; r++) {
        Row row = sheet.getRow(r);
        String c1 = cellAt(row, sheet, COL_L1_CODE_XYZ, merged);
        String c2 = cellAt(row, sheet, COL_L2_CODE_XYZ, merged);
        String pCode = cellAt(row, sheet, COL_POINT_CODE, merged);
        String pName = cellAt(row, sheet, COL_POINT_NAME, merged);
        // 页脚截断：填表人/审表人 行不进入数据区
        if (isFooter(c1) || isFooter(c2) || isFooter(pCode) || isFooter(pName)) {
          break;
        }
        if (isBlankRow(c1, c2, pCode, pName, cellAt(row, sheet, COL_L1_NAME, merged),
            cellAt(row, sheet, COL_L2_NAME, merged))) {
          continue;
        }
        // 空值继承：合并/ragged 文件中空单元格视为延续上一块，不产生空节点
        if (c1 != null && !c1.isBlank() && !eqText(c1, l1Raw)) {
          l1Raw = c1;
          l1Sort++;
          l2Sort = 0;
          l2Raw = null;
          sawPoint = false;
          NodeRow n = levelRow(1, c1, cellAt(row, sheet, COL_L1_NAME, merged),
              cellAt(row, sheet, COL_L1_WEIGHT, merged), cellAt(row, sheet, COL_L1_SELECT, merged),
              null, null, l1Sort);
          n.setParentIndex(-1);
          n.setRowIndex(rows.size());
          l1Idx = rows.size();
          rows.add(n);
        }
        if (c2 != null && !c2.isBlank() && !eqText(c2, l2Raw)) {
          l2Raw = c2;
          l2Sort++;
          sawPoint = false;
          NodeRow n = levelRow(2, c2, cellAt(row, sheet, COL_L2_NAME, merged),
              cellAt(row, sheet, COL_L2_WEIGHT, merged), cellAt(row, sheet, COL_L2_SELECT, merged),
              SkillCellTextParser.normalizeMultiline(cellAt(row, sheet, COL_L2_SKILL, merged)),
              null, l2Sort);
          n.setParentIndex(l1Idx);
          n.setRowIndex(rows.size());
          l2Idx = rows.size();
          rows.add(n);
        }
        if ((pCode != null && !pCode.isBlank()) || (pName != null && !pName.isBlank())) {
          if (l2Idx < 0) {
            warnings.add("第" + (r + 1) + "行考评点缺少所属二级考评范围，已跳过");
            continue;
          }
          pointSort = sawPoint ? pointSort + 1 : 1;
          sawPoint = true;
          String impRaw = cellAt(row, sheet, COL_POINT_IMPORTANCE, merged);
          String imp = SkillCellTextParser.parseImportance(impRaw);
          if (imp == null && impRaw != null && !impRaw.isBlank()) {
            warnings.add("第" + (r + 1) + "行考评点重要程度无法识别: " + impRaw);
          }
          Integer quota = null;
          String quotaRaw = cellAt(row, sheet, COL_POINT_QUOTA, merged);
          try {
            quota = SkillCellTextParser.parseQuota(quotaRaw);
          } catch (IllegalArgumentException e) {
            warnings.add("第" + (r + 1) + "行试题量列异常（已忽略）: " + e.getMessage());
          }
          NodeRow n = levelRow(3, pCode, pName, null, null, null, imp, pointSort);
          n.setQuestionQuota(quota);
          n.setParentIndex(l2Idx);
          n.setRowIndex(rows.size());
          rows.add(n);
        }
      }
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel 文件读取失败: " + e.getMessage());
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel 解析失败: " + e.getMessage());
    }
    result.setRows(rows);
    result.setWarnings(warnings);
    return result;
  }

  private NodeRow levelRow(int level, String codeRaw, String nameRaw, String weightRaw,
      String selectRaw, String skillRequirement, String importance, int sort) {
    NodeRow n = new NodeRow();
    n.setNodeLevel(level);
    n.setSortOrder(sort);
    if (level == 3) {
      // 考评点：代码/名称/重要程度按列直读
      n.setCode(SkillCellTextParser.parsePointCode(codeRaw));
      n.setName(nameRaw == null ? "" : nameRaw.trim());
      n.setImportance(importance);
    } else {
      SkillCellTextParser.CodeParts parts = SkillCellTextParser.parseCodeXyz(codeRaw);
      n.setCode(parts.code() != null && !parts.code().isBlank()
          ? parts.code() : fallbackCode(level, sort));
      n.setName(nameRaw == null ? "" : nameRaw.trim());
      n.setXyzStat(parts.xyzStat());
      n.setWeight(SkillCellTextParser.parseWeight(weightRaw));
      n.setSelectMode(selectRaw == null ? "" : SkillCellTextParser.normalizeMultiline(selectRaw));
      if (level == 2) {
        n.setSkillRequirement(skillRequirement);
      }
    }
    return n;
  }

  private String fallbackCode(int level, int sort) {
    if (level == 1) {
      return sort == 1 ? "A" : "B";
    }
    return String.valueOf((char) ('A' + sort - 1));
  }

  /** 表头定位：前 15 行内找含"考评范围一级"的行（块表头行） */
  private int locateBlockHeaderRow(Sheet sheet, Map<String, String> merged) {
    int limit = Math.min(HEADER_SCAN_ROWS, sheet.getLastRowNum());
    for (int r = 0; r <= limit; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      for (int c = 0; c <= COL_L1_WEIGHT; c++) {
        String v = cellAt(row, sheet, c, merged);
        if (v != null && v.replace(" ", "").contains("考评范围一级")) {
          return r;
        }
      }
    }
    return -1;
  }

  private boolean isFooter(String v) {
    return v != null && (v.contains("填表人") || v.contains("审表人") || v.contains("制表人"));
  }

  /** 合并区域首格值 → 区域内所有坐标可取 */
  private Map<String, String> collectMergedValues(Sheet sheet) {
    Map<String, String> map = new HashMap<>();
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      Row firstRow = sheet.getRow(region.getFirstRow());
      if (firstRow == null) {
        continue;
      }
      Cell cell = firstRow.getCell(region.getFirstColumn());
      String v = cellText(cell);
      for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
        for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
          map.put(r + ":" + c, v);
        }
      }
    }
    return map;
  }

  private String cellTextAt(Sheet sheet, int rowIdx, int col, Map<String, String> merged) {
    return cellAt(sheet.getRow(rowIdx), sheet, col, merged);
  }

  private String cellAt(Row row, Sheet sheet, int col, Map<String, String> merged) {
    int r = row == null ? -1 : row.getRowNum();
    if (r >= 0 && merged.containsKey(r + ":" + col)) {
      return merged.get(r + ":" + col);
    }
    if (row == null) {
      return null;
    }
    return cellText(row.getCell(col));
  }

  private String cellText(Cell cell) {
    if (cell == null) {
      return null;
    }
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case NUMERIC -> {
        double d = cell.getNumericCellValue();
        if (d == Math.floor(d)) {
          yield String.valueOf((long) d);
        }
        yield String.valueOf(d);
      }
      case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
      case FORMULA -> {
        try {
          yield cell.getStringCellValue();
        } catch (IllegalStateException | NumberFormatException e) {
          yield String.valueOf(cell.getNumericCellValue());
        }
      }
      default -> null;
    };
  }

  private boolean eqText(String a, String b) {
    String ta = a == null ? "" : a.trim();
    String tb = b == null ? "" : b.trim();
    return ta.equals(tb) && !ta.isEmpty();
  }

  private boolean isBlankRow(String... cells) {
    for (String c : cells) {
      if (c != null && !c.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v.trim();
      }
    }
    return null;
  }
}
