package com.guoyu.platform.exam.theorygen.parser;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult;
import com.guoyu.platform.exam.theorygen.dto.BlueprintParseResult.NodeRow;
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
 * 《理论知识考评要素细目表》xlsx 解析器（POI）。
 *
 * <p>样例画像（验收基准，方案 §2.5）：
 * 表头 2~4 行，数据自第 5 行起；列 A~J：
 * A 一级 | B 一级权重 | C 二级 | D 二级权重 | E 三级 | F 三级权重 | G 相关知识 | H 考评点代码 | I 考评点名称 | J 重要程度。
 * 合并单元格值向下填充；多行单格兼容「名称⏎代码⏎（X:Y:Z）」与「代码⏎名称⏎（X:Y:Z）」。
 */
@Component
public class TheoryBlueprintXlsxParser {

  private static final int HEADER_ROWS = 4;
  private static final int COL_L1 = 0;
  private static final int COL_L1_WEIGHT = 1;
  private static final int COL_L2 = 2;
  private static final int COL_L2_WEIGHT = 3;
  private static final int COL_L3 = 4;
  private static final int COL_L3_WEIGHT = 5;
  private static final int COL_KNOWLEDGE = 6;
  private static final int COL_POINT_CODE = 7;
  private static final int COL_POINT_NAME = 8;
  private static final int COL_IMPORTANCE = 9;

  public BlueprintParseResult parse(InputStream in, String fileName) {
    BlueprintParseResult result = new BlueprintParseResult();
    result.setFileName(fileName);
    List<NodeRow> rows = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    try (Workbook wb = new XSSFWorkbook(in)) {
      if (wb.getNumberOfSheets() < 1) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "Excel 文件无工作表");
      }
      Sheet sheet = wb.getSheetAt(0);
      Map<String, String> merged = collectMergedValues(sheet);

      int lastRow = sheet.getLastRowNum();
      String l1Raw = null;
      String l2Raw = null;
      String l3Raw = null;
      String l1W = null;
      String l2W = null;
      String l3W = null;
      String l1Key = "";
      String l2Key = "";
      String l3Key = "";
      int l1Sort = 0;
      int l2Sort = 0;
      int l3Sort = 0;
      int pointSort = 0;
      boolean sawPoint = false;
      // 父行索引追踪（-1=根）
      int l1Idx = -1;
      int l2Idx = -1;
      int l3Idx = -1;

      for (int r = HEADER_ROWS; r <= lastRow; r++) {
        Row row = sheet.getRow(r);
        String c1 = cellAt(row, sheet, COL_L1, merged);
        String c2 = cellAt(row, sheet, COL_L2, merged);
        String c3 = cellAt(row, sheet, COL_L3, merged);
        if (isBlankRow(c1, c2, c3, cellAt(row, sheet, COL_POINT_CODE, merged),
            cellAt(row, sheet, COL_POINT_NAME, merged))) {
          continue;
        }
        if (!eqText(c1, l1Raw)) {
          l1Raw = c1;
          l1W = cellAt(row, sheet, COL_L1_WEIGHT, merged);
          l1Key = l1Raw == null ? "" : l1Raw.trim();
          l1Sort++;
          l2Sort = 0;
          l3Sort = 0;
          l2Raw = null;
          l3Raw = null;
          NodeRow n = levelRow(1, l1Key, l1Raw, l1W, null, "", l1Sort);
          n.setParentIndex(-1);
          l1Idx = rows.size();
          n.setRowIndex(l1Idx);
          rows.add(n);
          sawPoint = false;
        }
        if (!eqText(c2, l2Raw)) {
          l2Raw = c2;
          l2W = cellAt(row, sheet, COL_L2_WEIGHT, merged);
          l2Key = l2Raw == null ? "" : l2Raw.trim();
          l2Sort++;
          l3Sort = 0;
          l3Raw = null;
          NodeRow n = levelRow(2, l2Key, l2Raw, l2W, null, l1Key, l2Sort);
          n.setParentIndex(l1Idx);
          l2Idx = rows.size();
          n.setRowIndex(l2Idx);
          rows.add(n);
          sawPoint = false;
        }
        if (!c3Blank(c3, l3Raw) && !eqText(c3, l3Raw)) {
          l3Raw = c3;
          l3W = cellAt(row, sheet, COL_L3_WEIGHT, merged);
          l3Key = l3Raw == null ? "" : l3Raw.trim();
          l3Sort++;
          String knowledge = BlueprintCellTextParser.normalizeKnowledge(
              cellAt(row, sheet, COL_KNOWLEDGE, merged));
          NodeRow n = levelRow(3, l3Key, l3Raw, l3W, knowledge, l1Key + "|" + l2Key, l3Sort);
          n.setParentIndex(l2Idx);
          l3Idx = rows.size();
          n.setRowIndex(l3Idx);
          rows.add(n);
          sawPoint = false;
        }
        // 考评点行：H/I 任一非空即视为考评点
        String pCode = cellAt(row, sheet, COL_POINT_CODE, merged);
        String pName = cellAt(row, sheet, COL_POINT_NAME, merged);
        String imp = cellAt(row, sheet, COL_IMPORTANCE, merged);
        if ((pCode != null && !pCode.isBlank()) || (pName != null && !pName.isBlank())) {
          pointSort = sawPoint ? pointSort + 1 : 1;
          sawPoint = true;
          NodeRow n = levelRow(4, (pCode == null ? "" : pCode.trim()),
              pName, null, null, l1Key + "|" + l2Key + "|" + l3Key, pointSort);
          n.setParentIndex(l3Idx);
          n.setRowIndex(rows.size());
          // 考评点重要程度
          if (imp != null && !imp.isBlank()) {
            String t = imp.trim().toUpperCase();
            if (t.startsWith("X") || t.startsWith("Y") || t.startsWith("Z")) {
              n.setImportance(t.substring(0, 1));
            } else {
              warnings.add("第" + (r + 1) + "行考评点重要程度无法识别: " + imp);
            }
          }
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

  private NodeRow levelRow(int level, String key, String raw, String weightRaw,
      String knowledge, String parentKey, int sort) {
    NodeRow n = new NodeRow();
    n.setNodeLevel(level);
    n.setSortOrder(sort);
    n.setParentKey(parentKey);
    BlueprintCellTextParser.CellParts parts = BlueprintCellTextParser.parse(raw);
    if (level == 4) {
      // 考评点：H 列代码、I 列名称
      n.setCode(key);
      n.setName(raw == null ? "" : raw.trim());
    } else {
      n.setCode(parts.code() != null && !parts.code().isBlank() ? parts.code() : fallbackCode(level, sort));
      n.setName(parts.name());
      n.setXyzStat(parts.xyzStat());
      n.setWeight(BlueprintCellTextParser.parseWeight(weightRaw));
    }
    if (level == 3 && knowledge != null && !knowledge.isBlank()) {
      n.setRelatedKnowledge(knowledge);
    }
    return n;
  }

  private String fallbackCode(int level, int sort) {
    if (level == 1) {
      return sort == 1 ? "A" : "B";
    }
    return String.valueOf((char) ('A' + sort - 1));
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

  /** 三级换新判定：新值非空且与旧值不同（考评点-only 行 E 列为空不代表换三级） */
  private boolean c3Blank(String c3, String l3Raw) {
    return c3 == null || c3.isBlank();
  }

  private boolean isBlankRow(String... cells) {
    for (String c : cells) {
      if (c != null && !c.isBlank()) {
        return false;
      }
    }
    return true;
  }
}
