package com.guoyu.platform.textbook.exporter;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.textbook.entity.TextbookOutlineNodeEntity;
import com.guoyu.platform.textbook.entity.TextbookProjectEntity;
import com.guoyu.platform.textbook.vo.OutlineSummaryVO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * 教材目录导出（设计 §2.4-⑦）：Markdown / Excel（POI），附配比依据列；
 * 合计不平衡不阻塞导出，但在文件内标注。文件名：教材目录_{工种}_{方向}_{等级}_{yyyyMMdd}.{ext}。
 */
public class TextbookOutlineExporter {

  /** blob 下载载体（照 StandardParseController.writeFile 响应口径） */
  public record ExportFile(String fileName, String contentType, byte[] content) {
  }

  private static final String[] LEVEL_LABELS = {
      "五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"
  };
  private static final String[] LEVEL_NAMES = {"篇", "章", "节", "点"};

  public ExportFile export(TextbookProjectEntity project, List<TextbookOutlineNodeEntity> nodes,
      OutlineSummaryVO summary, String format) {
    String fileName = "教材目录_" + project.getWorkTypeTitle() + "_" + project.getDirection()
        + "_" + levelLabel(project.getLevelCode()).replace("/", "-") + "_"
        + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    if ("xlsx".equalsIgnoreCase(format)) {
      return new ExportFile(fileName + ".xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          exportExcel(project, nodes, summary));
    }
    if ("md".equalsIgnoreCase(format)) {
      return new ExportFile(fileName + ".md", "text/markdown;charset=UTF-8",
          exportMarkdown(project, nodes, summary).getBytes(StandardCharsets.UTF_8));
    }
    throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持导出格式：" + format + "（仅 md/xlsx）");
  }

  // ============================================================
  // Markdown
  // ============================================================

  private String exportMarkdown(TextbookProjectEntity project,
      List<TextbookOutlineNodeEntity> nodes, OutlineSummaryVO summary) {
    Map<Long, List<TextbookOutlineNodeEntity>> byParent = groupByParent(nodes);
    StringBuilder md = new StringBuilder();
    md.append("# ").append(project.getWorkTypeTitle()).append('-').append(project.getDirection())
        .append('（').append(levelLabel(project.getLevelCode())).append("）\n\n");

    List<TextbookOutlineNodeEntity> parts = byParent.getOrDefault(0L, List.of());
    int partSeq = 0;
    for (TextbookOutlineNodeEntity part : parts) {
      partSeq++;
      md.append("**第").append(cnNumber(partSeq)).append("篇 ")
          .append(part.getTitle()).append("**\n\n");
      for (TextbookOutlineNodeEntity chapter : byParent.getOrDefault(part.getId(), List.of())) {
        md.append("## ").append(chapter.getTitle());
        appendChapterMeta(md, chapter);
        md.append("\n\n");
        for (TextbookOutlineNodeEntity section : byParent.getOrDefault(chapter.getId(), List.of())) {
          md.append("### ").append(section.getTitle());
          if (section.getTargetWords() != null) {
            md.append("（目标 ").append(section.getTargetWords()).append(" 字）");
          }
          md.append("\n\n");
          for (TextbookOutlineNodeEntity point : byParent.getOrDefault(section.getId(), List.of())) {
            md.append("- [").append("skill".equals(point.getDimension()) ? "技能" : "理论")
                .append("] ").append(point.getTitle()).append('\n');
          }
          md.append('\n');
        }
      }
    }

    md.append("## 配比依据\n\n");
    md.append("| 章 | 权重来源 | 是否人工调整 |\n");
    md.append("| --- | --- | --- |\n");
    for (TextbookOutlineNodeEntity part : parts) {
      for (TextbookOutlineNodeEntity chapter : byParent.getOrDefault(part.getId(), List.of())) {
        md.append("| ").append(chapter.getTitle()).append(" | ")
            .append(weightSource(chapter)).append(" | ")
            .append(Integer.valueOf(1).equals(chapter.getWordsManual()) ? "是" : "否")
            .append(" |\n");
      }
    }
    md.append('\n');
    appendImbalanceNote(md, summary);
    return md.toString();
  }

  // ============================================================
  // Excel（表头加粗 + 冻结首行）
  // ============================================================

  private byte[] exportExcel(TextbookProjectEntity project,
      List<TextbookOutlineNodeEntity> nodes, OutlineSummaryVO summary) {
    try (Workbook workbook = new SXSSFWorkbook(new XSSFWorkbook(), -1);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("教材目录");
      CellStyle headerStyle = workbook.createCellStyle();
      Font bold = workbook.createFont();
      bold.setBold(true);
      headerStyle.setFont(bold);

      String[] headers = {"层级", "编号", "标题", "维度", "权重%", "目标字数", "配比依据"};
      Row header = sheet.createRow(0);
      for (int c = 0; c < headers.length; c++) {
        header.createCell(c).setCellValue(headers[c]);
        header.getCell(c).setCellStyle(headerStyle);
      }

      Map<Long, List<TextbookOutlineNodeEntity>> byParent = groupByParent(nodes);
      Map<Long, Integer> pointCountBySection = countPoints(byParent);
      int rowIdx = 1;
      for (TextbookOutlineNodeEntity part : byParent.getOrDefault(0L, List.of())) {
        rowIdx = writeNode(sheet, rowIdx, part, byParent, pointCountBySection);
      }
      if (!summary.balanced()) {
        Row note = sheet.createRow(rowIdx + 1);
        note.createCell(0).setCellValue("⚠ 合计不平衡：章合计 " + summary.chapterSum()
            + " ≠ 总字数 " + summary.totalWords()
            + (summary.unbalancedChapters().isEmpty() ? "" : "（章节：" + String.join("、", summary.unbalancedChapters()) + "）"));
      }
      for (int c = 0; c < headers.length; c++) {
        sheet.setColumnWidth(c, c == 2 ? 8000 : 3000);
      }
      sheet.createFreezePane(0, 1);
      workbook.write(out);
      out.flush();
      return out.toByteArray();
    } catch (IOException e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导出 Excel 失败：" + e.getMessage());
    }
  }

  private int writeNode(Sheet sheet, int rowIdx, TextbookOutlineNodeEntity node,
      Map<Long, List<TextbookOutlineNodeEntity>> byParent,
      Map<Long, Integer> pointCountBySection) {
    Row row = sheet.createRow(rowIdx++);
    int level = node.getNodeLevel() == null ? 0 : node.getNodeLevel();
    row.createCell(0).setCellValue(level >= 1 && level <= 4 ? LEVEL_NAMES[level - 1] : "");
    row.createCell(1).setCellValue(node.getSourceNo() == null ? "" : node.getSourceNo());
    row.createCell(2).setCellValue(node.getTitle() == null ? "" : node.getTitle());
    row.createCell(3).setCellValue(pointDimensionLabel(node));
    row.createCell(4).setCellValue(weightText(node.getWeight()));
    if (node.getTargetWords() != null) {
      row.createCell(5).setCellValue(node.getTargetWords());
    } else {
      row.createCell(5).setCellValue("");
    }
    row.createCell(6).setCellValue(basisText(node, pointCountBySection));
    // 篇/章/节向下递归；点无子级
    if (level >= 1 && level <= 3) {
      for (TextbookOutlineNodeEntity child : byParent.getOrDefault(node.getId(), List.of())) {
        rowIdx = writeNode(sheet, rowIdx, child, byParent, pointCountBySection);
      }
    }
    return rowIdx;
  }

  // ============================================================
  // 共享工具
  // ============================================================

  private Map<Long, List<TextbookOutlineNodeEntity>> groupByParent(
      List<TextbookOutlineNodeEntity> nodes) {
    Map<Long, List<TextbookOutlineNodeEntity>> byParent = new HashMap<>();
    for (TextbookOutlineNodeEntity node : nodes) {
      byParent.computeIfAbsent(node.getParentId() == null ? 0L : node.getParentId(),
          k -> new ArrayList<>()).add(node);
    }
    return byParent;
  }

  private Map<Long, Integer> countPoints(Map<Long, List<TextbookOutlineNodeEntity>> byParent) {
    Map<Long, Integer> counts = new HashMap<>();
    for (List<TextbookOutlineNodeEntity> siblings : byParent.values()) {
      for (TextbookOutlineNodeEntity node : siblings) {
        if (node.getNodeLevel() != null && node.getNodeLevel() == 4) {
          Long parentId = node.getParentId() == null ? 0L : node.getParentId();
          counts.merge(parentId, 1, Integer::sum);
        }
      }
    }
    return counts;
  }

  private void appendChapterMeta(StringBuilder md, TextbookOutlineNodeEntity chapter) {
    boolean hasMeta = chapter.getWeight() != null || chapter.getTargetWords() != null;
    if (!hasMeta) {
      return;
    }
    md.append("（");
    if (chapter.getWeight() != null) {
      md.append("权重 ").append(weightText(chapter.getWeight())).append('%');
    }
    if (chapter.getTargetWords() != null) {
      if (chapter.getWeight() != null) {
        md.append("，");
      }
      md.append("目标 ").append(chapter.getTargetWords()).append(" 字");
    }
    md.append("）");
  }

  private void appendImbalanceNote(StringBuilder md, OutlineSummaryVO summary) {
    if (summary.balanced()) {
      return;
    }
    md.append("> ⚠ 合计不平衡：章合计 ").append(summary.chapterSum())
        .append(" ≠ 总字数 ").append(summary.totalWords());
    if (!summary.unbalancedChapters().isEmpty()) {
      md.append("（章节：").append(String.join("、", summary.unbalancedChapters())).append("）");
    }
    md.append('\n');
  }

  /** 章配比依据：权重表行 / 人工调整标记；节配比依据：条数占比 / 人工调整 */
  private String basisText(TextbookOutlineNodeEntity node,
      Map<Long, Integer> pointCountBySection) {
    boolean manual = Integer.valueOf(1).equals(node.getWordsManual());
    String basis;
    if (node.getNodeLevel() != null && node.getNodeLevel() == 2) {
      basis = node.getWeight() != null
          ? "理论知识权重表 " + weightText(node.getWeight()) + "%" : "无权重（人工兜底）";
    } else if (node.getNodeLevel() != null && node.getNodeLevel() == 3) {
      int count = pointCountBySection.getOrDefault(node.getId(), 0);
      basis = "知识点+技能点 " + count + " 条占比";
    } else {
      return "";
    }
    return manual ? basis + "；人工调整" : basis;
  }

  private String weightSource(TextbookOutlineNodeEntity chapter) {
    return chapter.getWeight() != null
        ? "理论知识权重表 " + weightText(chapter.getWeight()) + "%" : "无权重（人工兜底）";
  }

  private String pointDimensionLabel(TextbookOutlineNodeEntity node) {
    if (node.getNodeLevel() == null || node.getNodeLevel() != 4) {
      return "";
    }
    return "skill".equals(node.getDimension()) ? "技能" : "理论";
  }

  private static String weightText(BigDecimal weight) {
    if (weight == null) {
      return "";
    }
    return weight.stripTrailingZeros().toPlainString();
  }

  static String levelLabel(Integer levelCode) {
    if (levelCode == null || levelCode < 1 || levelCode > 5) {
      return String.valueOf(levelCode);
    }
    return LEVEL_LABELS[5 - levelCode];
  }

  private static String cnNumber(int n) {
    return switch (n) {
      case 1 -> "一";
      case 2 -> "二";
      case 3 -> "三";
      case 4 -> "四";
      default -> String.valueOf(n);
    };
  }
}
