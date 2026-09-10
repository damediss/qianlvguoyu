package com.guoyu.platform.exam.standard.export;

import com.guoyu.platform.exam.standard.entity.ExamStandardNodeEntity;
import com.guoyu.platform.exam.standard.export.StandardExportSupport.ExportTree;
import com.guoyu.platform.exam.standard.parser.model.DocxTextUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
 * 理论知识考评要素细目表 Excel 导出（方案 §2.3/§9.2，版式对齐样例 11_矿山救护工 Sheet1，10 列）。
 */
public class MubiaoExcelExporter {

  private static final int COLS = 10;

  /**
   * @param tree      单 等级×方向 导出树
   * @param sheetName 如 "Sheet1"
   * @param titleLine 如 "矿山救护工(五级)理论知识考评要素细目表"
   */
  public byte[] export(ExportTree tree, String sheetName, String titleLine) throws IOException {
    // SXSSF 流式写出（window=-1 保留全部行以支持合并区域随机访问；细目表规模仅数百行）
    try (Workbook workbook = new SXSSFWorkbook(new XSSFWorkbook(), -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(sheetName);
      Styles styles = new Styles(workbook);

      // 表头 4 行：先把每行所有格子创建并套上样式，再做合并——
      // Excel 合并区的边线由区域内所有底层格子的边框叠加，非锚点格无样式会缺线
      // 第1行：标题（合并整行）
      Row r0 = sheet.createRow(0);
      for (int c = 0; c < COLS; c++) {
        r0.createCell(c).setCellStyle(styles.title);
      }
      r0.getCell(0).setCellValue(titleLine);
      sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COLS - 1));

      // 第2行：考评范围 / 考评点
      Row r1 = sheet.createRow(1);
      for (int c = 0; c < COLS; c++) {
        r1.createCell(c).setCellStyle(styles.header);
      }
      r1.getCell(0).setCellValue("考评范围");
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));
      r1.getCell(7).setCellValue("考评点");
      sheet.addMergedRegion(new CellRangeAddress(1, 1, 7, 9));

      // 第3行：一级/二级/三级 + 代码/名称/重要程度
      Row r2 = sheet.createRow(2);
      for (int c = 0; c < COLS; c++) {
        r2.createCell(c).setCellStyle(styles.header);
      }
      r2.getCell(0).setCellValue("一级");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 1));
      r2.getCell(2).setCellValue("二级");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 2, 3));
      r2.getCell(4).setCellValue("三级");
      sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, 6));
      r2.getCell(7).setCellValue("代码");
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 7, 7));
      r2.getCell(8).setCellValue("名称");
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 8, 8));
      r2.getCell(9).setCellValue("重要程度");
      sheet.addMergedRegion(new CellRangeAddress(2, 3, 9, 9));

      // 第4行：「代码名称」列带换行的「（重要程度比例）」副标题
      Row r3 = sheet.createRow(3);
      for (int c = 0; c < COLS; c++) {
        r3.createCell(c).setCellStyle(styles.header);
      }
      r3.getCell(0).setCellValue("代码名称\n（重要程度比例）");
      r3.getCell(1).setCellValue("权重");
      r3.getCell(2).setCellValue("代码名称\n（重要程度比例）");
      r3.getCell(3).setCellValue("权重");
      r3.getCell(4).setCellValue("代码名称\n（重要程度比例）");
      r3.getCell(5).setCellValue("考评权重");
      r3.getCell(6).setCellValue("相关知识");
      // 两行表头需要显式行高（POI 写出的文件 Excel 打开时不会自动撑高）
      r3.setHeightInPoints(2 * sheet.getDefaultRowHeightInPoints());

      int rowIdx = 4;
      if (tree.rootA != null) {
        rowIdx = writeL0Branch(sheet, tree, tree.rootA, rowIdx, styles);
      }
      if (tree.rootB != null) {
        rowIdx = writeL0Branch(sheet, tree, tree.rootB, rowIdx, styles);
      }

      // 列宽（上限 60 字符宽）
      int[] widths = {24, 8, 24, 8, 24, 8, 40, 8, 40, 10};
      for (int c = 0; c < COLS; c++) {
        sheet.setColumnWidth(c, widths[c] * 256);
      }
      workbook.write(out);
      out.flush();
      return out.toByteArray();
    }
  }

  /** 写一个 L0 分支（A=基本要求 / B=工作要求），返回下一个可用行号 */
  private int writeL0Branch(Sheet sheet, ExportTree tree, ExamStandardNodeEntity l0,
      int startRow, Styles styles) {
    List<ExamStandardNodeEntity> l1List = tree.children(l0.getId());
    int totalRows = 0;
    for (ExamStandardNodeEntity l1 : l1List) {
      totalRows += branchRows(tree, l1);
    }
    if (totalRows == 0) {
      totalRows = 1;
    }
    int endRow = startRow + totalRows - 1;

    // L0 合并单元格：名称+代码+（X:Y:Z）、权重
    int[] xyz = StandardExportSupport.countXyz(tree, l0.getId(), "theory");
    Row first = sheet.createRow(startRow);
    String l0Text = stripKeySuffix(l0.getTitle()) + "\n" + StandardExportSupport.lastSegment(l0.getNodeKey())
        + "\n" + StandardExportSupport.xyzText(xyz);
    setCell(first, 0, l0Text, styles.center);
    setWeight(first, 1, l0.getWeight(), styles);
    if (endRow > startRow) {
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 0, 0));
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 1, 1));
    }

    int rowIdx = startRow;
    for (ExamStandardNodeEntity l1 : l1List) {
      rowIdx = writeL1(sheet, tree, l1, rowIdx, styles);
    }
    applyBorders(sheet, startRow, endRow, styles);
    return endRow + 1;
  }

  private int writeL1(Sheet sheet, ExportTree tree, ExamStandardNodeEntity l1, int startRow,
      Styles styles) {
    List<ExamStandardNodeEntity> l2List = tree.children(l1.getId());
    int totalRows = 0;
    for (ExamStandardNodeEntity l2 : l2List) {
      totalRows += branchRows(tree, l2);
    }
    if (totalRows == 0) {
      totalRows = 1;
    }
    int endRow = startRow + totalRows - 1;

    int[] xyz = StandardExportSupport.countXyz(tree, l1.getId(), "theory");
    Row first = sheet.getRow(startRow) != null ? sheet.getRow(startRow) : sheet.createRow(startRow);
    String l1Text = stripKeySuffix(l1.getTitle()) + "\n" + StandardExportSupport.lastSegment(l1.getNodeKey())
        + "\n" + StandardExportSupport.xyzText(xyz);
    setCell(first, 2, l1Text, styles.center);
    setWeight(first, 3, l1.getWeight(), styles);
    if (endRow > startRow) {
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 2, 2));
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 3, 3));
    }

    // 本 L1 下所有 L2 权重一次性分配（最大余数法），保证合计严格等于 L1 权重
    Map<Long, BigDecimal> l2Weights = StandardExportSupport.allocateL2Weights(tree, l1);
    int rowIdx = startRow;
    for (ExamStandardNodeEntity l2 : l2List) {
      rowIdx = writeL2(sheet, tree, l1, l2, rowIdx, styles, l2Weights.get(l2.getId()));
    }
    return endRow + 1;
  }

  private int writeL2(Sheet sheet, ExportTree tree, ExamStandardNodeEntity l1,
      ExamStandardNodeEntity l2, int startRow, Styles styles, BigDecimal l2Weight) {
    List<ExamStandardNodeEntity> leaves = theoryLeaves(tree, l2);
    int rows = Math.max(leaves.size(), 1);
    int endRow = startRow + rows - 1;

    int[] xyz = StandardExportSupport.countXyz(tree, l2.getId(), "theory");
    Row first = sheet.getRow(startRow) != null ? sheet.getRow(startRow) : sheet.createRow(startRow);
    String l2Text = stripKeySuffix(l2.getTitle()) + "\n" + StandardExportSupport.lastSegment(l2.getNodeKey())
        + "\n" + StandardExportSupport.xyzText(xyz);
    setCell(first, 4, l2Text, styles.center);
    setWeight(first, 5, l2Weight, styles);
    // 旧任务库存的是单行串联文本，导出时按编号重拆逐行显示（新数据已是逐行，relineItems 原样返回）
    setCell(first, 6, l2.getRelatedKnowledge() != null
        ? DocxTextUtil.relineItems(l2.getRelatedKnowledge()) : "", styles.left);
    if (endRow > startRow) {
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 4, 4));
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 5, 5));
      sheet.addMergedRegion(new CellRangeAddress(startRow, endRow, 6, 6));
    }

    for (int i = 0; i < rows; i++) {
      Row row = sheet.getRow(startRow + i) != null ? sheet.getRow(startRow + i)
          : sheet.createRow(startRow + i);
      if (i < leaves.size()) {
        ExamStandardNodeEntity leaf = leaves.get(i);
        setCell(row, 7, StandardExportSupport.lastSegment(leaf.getNodeKey()), styles.center);
        setCell(row, 8, leaf.getTitle(), styles.left);
        setCell(row, 9, leaf.getImportant() != null ? leaf.getImportant() : "", styles.center);
      } else {
        setCell(row, 7, "", styles.center);
        setCell(row, 8, "", styles.left);
        setCell(row, 9, "", styles.center);
      }
    }
    return endRow + 1;
  }

  private List<ExamStandardNodeEntity> theoryLeaves(ExportTree tree, ExamStandardNodeEntity l2) {
    return tree.children(l2.getId()).stream()
        .filter(n -> n.getNodeLevel() == 3 && "theory".equals(n.getDimension()))
        .toList();
  }

  /** 分支占用行数 = 理论叶数（至少 1 行） */
  private int branchRows(ExportTree tree, ExamStandardNodeEntity node) {
    if (node.getNodeLevel() == 2) {
      return Math.max((int) tree.children(node.getId()).stream()
          .filter(n -> n.getNodeLevel() == 3 && "theory".equals(n.getDimension())).count(), 1);
    }
    int rows = 0;
    for (ExamStandardNodeEntity child : tree.children(node.getId())) {
      rows += branchRows(tree, child);
    }
    return Math.max(rows, 1);
  }

  /** 标题去掉内嵌编码后缀（标题=名称+编码，样例单元格内名称与代码分行显示） */
  private String stripKeySuffix(String title) {
    return title == null ? "" : title.replaceAll("[A-Z](-[A-Z]){0,2}(-\\d{3})?$", "");
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

  private void applyBorders(Sheet sheet, int startRow, int endRow, Styles styles) {
    // 数据区边框（首行样式已带边框，此处补齐合并区续行）
    for (int r = startRow; r <= endRow; r++) {
      Row row = sheet.getRow(r);
      if (row == null) {
        row = sheet.createRow(r);
      }
      for (int c = 0; c < COLS; c++) {
        Cell cell = row.getCell(c);
        if (cell == null) {
          cell = row.createCell(c);
          cell.setCellStyle(styles.center);
        }
      }
    }
  }

  /** 样式集合 */
  private static final class Styles {
    final CellStyle title;
    final CellStyle header;
    final CellStyle center;
    final CellStyle left;
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
      // 「代码名称\n（重要程度比例）」两行表头需要自动换行才能显示换行符
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

      weight = workbook.createCellStyle();
      weight.setAlignment(HorizontalAlignment.CENTER);
      weight.setVerticalAlignment(VerticalAlignment.CENTER);
      // 权重按百分数显示（0.05 → 5%），对齐 OSTA 纸质样表
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
