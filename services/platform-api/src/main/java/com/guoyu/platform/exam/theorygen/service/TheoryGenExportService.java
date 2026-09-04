package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** 试题表导出（方案 §8 样式：Sheet1 试题表 + Sheet2 批次报告由前端汇总展示，此处导出 Sheet1） */
@Service
public class TheoryGenExportService {

  private static final String[] HEADERS = {
      "序号", "考评点代码", "考评点名称", "重要程度", "题型代码", "题型", "题干",
      "（A）", "（B）", "（C）", "（D）", "（E）", "答案", "难度(1-5)", "一致性",
      "解析", "参考答案（主观）", "评分要点（主观）", "评分标准（主观）", "建议分值",
      "来源", "审核状态"
  };

  private final TheoryGenDraftService draftService;
  private final TheoryBlueprintApplicationService blueprintService;

  public TheoryGenExportService(TheoryGenDraftService draftService,
      TheoryBlueprintApplicationService blueprintService) {
    this.draftService = draftService;
    this.blueprintService = blueprintService;
  }

  /** 导出批次全部草稿为 xlsx 字节流 */
  public byte[] exportBatch(Long tenantId, Long batchId) throws IOException {
    List<TheoryGenDraftEntity> drafts = draftService.listAll(tenantId, batchId);
    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("试题表");
      Row head = sheet.createRow(0);
      for (int i = 0; i < HEADERS.length; i++) {
        head.createCell(i).setCellValue(HEADERS[i]);
      }
      int r = 1;
      for (TheoryGenDraftEntity d : drafts) {
        Row row = sheet.createRow(r++);
        int c = 0;
        row.createCell(c++).setCellValue(r - 1);
        row.createCell(c++).setCellValue(pointFullCode(tenantId, d));
        row.createCell(c++).setCellValue(pointName(tenantId, d));
        row.createCell(c++).setCellValue(pointImportance(tenantId, d));
        row.createCell(c++).setCellValue(d.getTypeCode() == null ? "" : d.getTypeCode());
        row.createCell(c++).setCellValue(typeName(d.getQuestionType()));
        row.createCell(c++).setCellValue(nvl(d.getContent()));
        // 选项 A~E
        java.util.List<java.util.Map<String, String>> options = parseOptions(d.getOptions());
        for (int i = 0; i < 5; i++) {
          row.createCell(c++).setCellValue(i < options.size()
              ? nvl(options.get(i).get("content")) : "");
        }
        row.createCell(c++).setCellValue(nvl(d.getAnswer()));
        row.createCell(c++).setCellValue(d.getDifficulty() == null ? 3 : d.getDifficulty());
        row.createCell(c++).setCellValue(d.getConsistency() == null ? 5 : d.getConsistency());
        row.createCell(c++).setCellValue(nvl(d.getAnalysis()));
        row.createCell(c++).setCellValue(nvl(d.getReferenceAnswer()));
        row.createCell(c++).setCellValue(nvl(d.getScorePoints()));
        row.createCell(c++).setCellValue(nvl(d.getScoringStandard()));
        row.createCell(c++).setCellValue(d.getScore() == null ? "" : d.getScore().toPlainString());
        row.createCell(c++).setCellValue("AI:" + nvl(d.getAiModel()));
        row.createCell(c).setCellValue(reviewName(d.getReviewStatus()));
      }
      wb.write(out);
      return out.toByteArray();
    }
  }

  private java.util.List<java.util.Map<String, String>> parseOptions(String json) {
    if (json == null || json.isBlank()) {
      return java.util.Collections.emptyList();
    }
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json,
          new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String, String>>>() { });
    } catch (Exception e) {
      return java.util.Collections.emptyList();
    }
  }

  private String pointFullCode(Long tenantId, TheoryGenDraftEntity d) {
    var n = blueprintService.findNode(tenantId, d.getNodeId());
    return n == null ? "" : nvl(n.getFullCode());
  }

  private String pointName(Long tenantId, TheoryGenDraftEntity d) {
    var n = blueprintService.findNode(tenantId, d.getNodeId());
    return n == null ? "" : nvl(n.getName());
  }

  private String pointImportance(Long tenantId, TheoryGenDraftEntity d) {
    var n = blueprintService.findNode(tenantId, d.getNodeId());
    return n == null || n.getImportance() == null ? "" : n.getImportance();
  }

  private String typeName(String t) {
    return switch (t == null ? "" : t) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      case "essay" -> "简答题";
      case "cal" -> "计算题";
      case "draw" -> "绘图题";
      case "discuss" -> "论述题";
      case "case_analysis" -> "案例分析题";
      default -> t == null ? "" : t;
    };
  }

  private String reviewName(String s) {
    return switch (s == null ? "" : s) {
      case "approved" -> "已通过";
      case "rejected" -> "已驳回";
      default -> "待审";
    };
  }

  private String nvl(String s) {
    return s == null ? "" : s;
  }
}
