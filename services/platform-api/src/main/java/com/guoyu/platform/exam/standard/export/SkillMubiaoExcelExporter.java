package com.guoyu.platform.exam.standard.export;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 操作技能考评要素细目表 Excel 导出（方案 §2.4/§9.3，版式对齐样例 16_矿山救护工，13 列）。
 *
 * <ul>
 *   <li>仅工作要求 B 支（dimension=skill 叶）；基本要求不出现；</li>
 *   <li>一级：代码+(X:Y:Z)、名称、考评比重（技能权重表）、选考方式=必考；</li>
 *   <li>二级：代码+(计数)、名称、技能要求原文、考评比重（仅首个二级标出）、选考方式=任选一项
 *       （组内仅 1 个考评点时为必考，2026-09-16 拍板）；</li>
 *   <li>试题量列程序留空，人工审核后填写（拍板⑤）。</li>
 * </ul>
 */
public class SkillMubiaoExcelExporter {

  private static final int COLS = 13;

  /**
   * @param tree        单 等级×方向 导出树（只读 B 支）
   * @param titleLine   如 "矿山救护工（五级）操作技能考评要素细目表"
   * @param careerLine  如 "职业： 矿山救护工                级别： 五"
   * @param skillWeight 一级考评比重解析器：入参 L1 节点，返回比重（可能 null）
   */
  public byte[] export(ExportTree tree, String titleLine, String careerLine,
      SkillWeightResolver skillWeight) throws IOException {
    try (Workbook workbook = new SXSSFWorkbook(new XSSFWorkbook(), -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Sheet1");
      Styles styles = new Styles(workbook);

      // 表头各行：先把所有格子创建并套样式，再做合并——
      // Excel 合并区的边线由区域内所有底层格子的边框叠加，非锚点格无样式会缺线
      Row r0 = sheet.createRow(0);
      for (int c = 0; c < COLS; c++) {
        r0.createCell(c).setCellStyle(styles.title);
      }
      r0.getCell(0).setCellValue(titleLine);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COLS - 1));

      Row r1 = sheet.createRow(1);
      for (int c = 0; c < COLS; c++) {
        r1.createCell(c).setCellStyle(styles.leftNoBorder);
      }
      r1.getCell(0).setCellValue(careerLine);
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, COLS - 1));

      Row r2 = sheet.createRow(2);
      for (int c = 0; c < COLS; c++) {
        r2.createCell(c).setCellStyle(styles.header);
      }
      r2.getCell(0).setCellValue("考评范围一级");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));
      r2.getCell(4).setCellValue("考评范围二级");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 8));
      r2.getCell(9).setCellValue("考评点");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 9, 12));

      Row r3 = sheet.createRow(3);
      String[] headers = {"代码\n（重要程度比例）", "名称", "考评比重", "选考方式",
          "代码\n（重要程度比例）", "名称", "技能要求", "考评比重", "选考方式",
          "代码", "名称", "重要程度", "试题量"};
      for (int c = 0; c < COLS; c++) {
        setCell(r3, c, headers[c], styles.header);
      }
      // 两行表头需要显式行高（POI 写出的文件 Excel 打开时不会自动撑高）
      r3.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());

      int rowIdx = 4;
      if (tree.rootB != null) {
        for (ExamStandardNodeEntity l1 : tree.children(tree.rootB.getId())) {
          rowIdx = writeL1(sheet, tree, l1, rowIdx, styles, skillWeight);
        }
      }

      // 尾行：填表人/填表日期/审表人/审表日期（对齐样例）
      Row footer = sheet.createRow(rowIdx);
      setCell(footer, 0, "填表人：", styles.leftNoBorder);
      setCell(footer, 2, "填表日期：20  年  月  日", styles.leftNoBorder);
      sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 2, 4));
      setCell(footer, 8, "审表人：", styles.leftNoBorder);
      setCell(footer, 10, "审表日期：20  年  月  日", styles.leftNoBorder);
      sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 10, 12));

      int[] widths = {14, 14, 8, 8, 14, 14, 40, 8, 10, 8, 40, 8, 8};
      for (int c = 0; c < COLS; c++) {
        sheet.setColumnWidth(c, widths[c] * 256);
      }
      workbook.write(out);
      out.flush();
      return out.toByteArray();
    }
  }

  private int writeL1(Sheet sheet, ExportTree tree, ExamStandardNodeEntity l1, int startRow,
      Styles styles, SkillWeightResolver skillWeight) {
    List<ExamStandardNodeEntity> l2List = tree.children(l1.getId());
    int totalRows = 0;
    for (ExamStandardNodeEntity l2 : l2List) {
      totalRows += l2Rows(tree, l2);
    }
    if (totalRows == 0) {
      totalRows = 1;
    }
    int endRow = startRow + totalRows - 1;

    int[] xyz = StandardExportSupport.countXyz(tree, l1.getId(), "skill");
    Row first = sheet.getRow(startRow) != null ? sheet.getRow(startRow) : sheet.createRow(startRow);
    setCell(first, 0, StandardExportSupport.lastSegment(l1.getNodeKey())
        + "\n" + StandardExportSupport.xyzText(xyz), styles.center);
    setCell(first, 1, displayName(l1.getTitle()), styles.center);
    setWeight(first, 2, skillWeight.weightOf(l1), styles);
    setCell(first, 3, "必考", styles.center);
    if (endRow > startRow) {
      for (int c = 0; c <= 3; c++) {
        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, c, c));
      }
    }

    int rowIdx = startRow;
    boolean firstL2 = true;
    for (ExamStandardNodeEntity l2 : l2List) {
      rowIdx = writeL2(sheet, tree, l1, l2, rowIdx, styles, firstL2, skillWeight);
      firstL2 = false;
    }
    fillBorders(sheet, startRow, endRow, styles);
    return endRow + 1;
  }

  private int writeL2(Sheet sheet, ExportTree tree, ExamStandardNodeEntity l1,
      ExamStandardNodeEntity l2, int startRow, Styles styles, boolean firstL2,
      SkillWeightResolver skillWeight) {
    List<ExamStandardNodeEntity> leaves = skillLeaves(tree, l2);
    int rows = Math.max(leaves.size(), 1);
    int endRow = startRow + rows - 1;

    int[] xyz = StandardExportSupport.countXyz(tree, l2.getId(), "skill");
    Row first = sheet.getRow(startRow) != null ? sheet.getRow(startRow) : sheet.createRow(startRow);
    setCell(first, 4, StandardExportSupport.lastSegment(l2.getNodeKey())
        + "\n" + StandardExportSupport.xyzText(xyz), styles.center);
    setCell(first, 5, displayName(l2.getTitle()), styles.center);
    // 旧任务库存的是单行串联文本，导出时按编号重拆逐行显示（新数据已是逐行，relineItems 原样返回）
    setCell(first, 6, l2.getSkillRequirement() != null
        ? DocxTextUtil.relineItems(l2.getSkillRequirement()) : "", styles.left);
    // 选考方式=任选一项 → 仅每个一级的首个二级行标出比重数值，其余留空（§8.2.3）
    if (firstL2) {
      setWeight(first, 7, skillWeight.weightOf(l1), styles);
    } else {
      setCell(first, 7, "", styles.center);
    }
    // 选考方式（2026-09-16 拍板）：组内仅 1 个考评点时为"必考"，多选一为"任选一项"；空组保持"任选一项"
    setCell(first, 8, leaves.size() == 1 ? "必考" : "任选一项", styles.center);
    if (endRow > startRow) {
      for (int c = 4; c <= 8; c++) {
        sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, c, c));
      }
    }

    for (int i = 0; i < rows; i++) {
      Row row = sheet.getRow(startRow + i) != null ? sheet.getRow(startRow + i)
          : sheet.createRow(startRow + i);
      if (i < leaves.size()) {
        ExamStandardNodeEntity leaf = leaves.get(i);
        setCell(row, 9, StandardExportSupport.lastSegment(leaf.getNodeKey()), styles.center);
        setCell(row, 10, leaf.getTitle(), styles.left);
        setCell(row, 11, leaf.getImportant() != null ? leaf.getImportant() : "", styles.center);
        // 试题量列留空（人工填写，拍板⑤）
        setCell(row, 12, "", styles.center);
      } else {
        for (int c = 9; c <= 12; c++) {
          setCell(row, c, "", styles.center);
        }
      }
    }
    return endRow + 1;
  }

  private List<ExamStandardNodeEntity> skillLeaves(ExportTree tree, ExamStandardNodeEntity l2) {
    List<ExamStandardNodeEntity> leaves = new ArrayList<>();
    for (ExamStandardNodeEntity n : tree.children(l2.getId())) {
      if (n.getNodeLevel() == 3 && "skill".equals(n.getDimension())) {
        leaves.add(n);
      }
    }
    return leaves;
  }

  private int l2Rows(ExportTree tree, ExamStandardNodeEntity l2) {
    return Math.max(skillLeaves(tree, l2).size(), 1);
  }

  private String stripKeySuffix(String title) {
    return title == null ? "" : title.replaceAll("[A-Z](-[A-Z]){0,2}(-\\d{3})?$", "");
  }

  /** 名称列文本：去编码后缀 + 去枚举序号（存量任务标题入库时带序号，导出侧再剥一次） */
  private String displayName(String title) {
    return DocxTextUtil.stripEnumerationPrefix(stripKeySuffix(title));
  }

  private void setCell(Row row, int col, String text, CellStyle style) {
    Cell cell = row.createCell(col);
    cell.setCellValue(text);
    cell.setCellStyle(style);
  }

  private void setWeight(Row row, int col, BigDecimal weight, Styles styles) {
    Cell cell = row.createCell(col);
    if (weight != null) {
      cell.setCellValue(weight.doubleValue());
      cell.setCellStyle(styles.weight);
    } else {
      cell.setCellValue("");
      cell.setCellStyle(styles.center);
    }
  }

  private void fillBorders(Sheet sheet, int startRow, int endRow, Styles styles) {
    for (int r = startRow; r <= endRow; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        row = sheet.createRow(r);
      }
      for (int c = 0; c < COLS; c++) {
        if (row.getCell(c) == null) {
          row.createCell(c).setCellStyle(styles.center);
        }
      }
    }
  }

  /** 一级考评比重解析（技能权重表） */
  public interface SkillWeightResolver {
    BigDecimal weightOf(ExamStandardNodeEntity l1);
  }

  private static final class Styles {
    final CellStyle title;
    final CellStyle header;
    final CellStyle center;
    final CellStyle left;
    final CellStyle leftNoBorder;
    final CellStyle weight;

    Styles(Workbook workbook) {
      Font titleFont = workbook.createFont();
      titleFont.setBold(true);
      titleFont.setFontHeightInPoints((short) 14);
      title = workbook.createCellStyle();
      title.setFont(titleFont);
      title.setAlignment(HorizontalAlignment.CENTER);
      title.setVerticalAlignment(VerticalAlignment.CENTER);
      border(title);

      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      header = workbook.createCellStyle();
      header.setFont(headerFont);
      header.setAlignment(HorizontalAlignment.CENTER);
      header.setVerticalAlignment(VerticalAlignment.CENTER);
      // 「代码\n（重要程度比例）」两行表头需要自动换行才能显示换行符
      header.setWrapText(true);
      header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      border(header);

      center = workbook.createCellStyle();
      center.setAlignment(HorizontalAlignment.CENTER);
      center.setVerticalAlignment(VerticalAlignment.CENTER);
      center.setWrapText(true);
      border(center);

      left = workbook.createCellStyle();
      left.setAlignment(HorizontalAlignment.LEFT);
      left.setVerticalAlignment(VerticalAlignment.CENTER);
      left.setWrapText(true);
      border(left);

      leftNoBorder = workbook.createCellStyle();
      leftNoBorder.setAlignment(HorizontalAlignment.LEFT);
      leftNoBorder.setVerticalAlignment(VerticalAlignment.CENTER);

      weight = workbook.createCellStyle();
      weight.setAlignment(HorizontalAlignment.CENTER);
      weight.setVerticalAlignment(VerticalAlignment.CENTER);
      // 考评比重按百分数显示（0.15 → 15%），对齐 OSTA 纸质样表
      weight.setDataFormat(workbook.createDataFormat().getFormat("0%"));
      border(weight);
    }

    private void border(CellStyle style) {
      style.setBorderTop(BorderStyle.THIN);
      style.setBorderBottom(BorderStyle.THIN);
      style.setBorderLeft(BorderStyle.THIN);
      style.setBorderRight(BorderStyle.THIN);
    }
  }
}
