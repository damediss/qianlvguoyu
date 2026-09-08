package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintNodeEntity;
import com.guoyu.platform.exam.theorygen.entity.TheoryGenDraftEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/** 试题表导出（方案 §8 样式：Sheet1 试题表 + Sheet2 批次报告由前端汇总展示，此处导出 Sheet1） */
@Service
public class TheoryGenExportService {

  private static final String[] HEADERS = {
      "序号", "一级代码名称", "二级代码名称", "三级代码名称", "考评点名称", "重要程度", "题型", "题干",
      "（A）", "（B）", "（C）", "（D）", "（E）", "答案", "难度", "一致性",
      "解析", "参考答案（主观）", "评分要点（主观）", "评分标准（主观）", "建议分值",
      "审核状态"
  };

  /** 难度等级中文（指引：易、较易、中、较难、难 ↔ 1~5） */
  private static final String[] DIFFICULTY_TEXT = {"易", "较易", "中", "较难", "难"};
  /** 题目-目标一致性中文（指引：差、较差、中、较好、好 ↔ 1~5） */
  private static final String[] CONSISTENCY_TEXT = {"差", "较差", "中", "较好", "好"};

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
    // 全量节点一次载入（此前逐行 selectById 曾致公网导出超前端 25s 超时）：
    // 行级渲染需沿父链拼考评点代码（指引口径 A-A-A-001），并取名称/重要程度
    Map<Long, TheoryBlueprintNodeEntity> nodeMap = new java.util.HashMap<>();
    if (!drafts.isEmpty()) {
      for (TheoryBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId,
          drafts.get(0).getBlueprintId())) {
        nodeMap.put(n.getId(), n);
      }
    }
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
        var node = nodeMap.get(d.getNodeId());
        String[] levelCells = levelCells(nodeMap, node);
        int c = 0;
        row.createCell(c++).setCellValue(r - 1);
        row.createCell(c++).setCellValue(nvl(levelCells[0]));
        row.createCell(c++).setCellValue(nvl(levelCells[1]));
        row.createCell(c++).setCellValue(nvl(levelCells[2]));
        row.createCell(c++).setCellValue(node == null ? "" : nvl(node.getName()));
        row.createCell(c++).setCellValue(node == null || node.getImportance() == null
            ? "" : node.getImportance());
        row.createCell(c++).setCellValue(typeName(d.getQuestionType()));
        row.createCell(c++).setCellValue(nvl(d.getContent()));
        // 选项 A~E
        java.util.List<java.util.Map<String, String>> options = parseOptions(d.getOptions());
        for (int i = 0; i < 5; i++) {
          row.createCell(c++).setCellValue(i < options.size()
              ? nvl(options.get(i).get("content")) : "");
        }
        row.createCell(c++).setCellValue(displayAnswer(d));
        row.createCell(c++).setCellValue(levelText(DIFFICULTY_TEXT, d.getDifficulty(), 3));
        row.createCell(c++).setCellValue(levelText(CONSISTENCY_TEXT, d.getConsistency(), 5));
        row.createCell(c++).setCellValue(nvl(d.getAnalysis()));
        row.createCell(c++).setCellValue(nvl(d.getReferenceAnswer()));
        row.createCell(c++).setCellValue(nvl(d.getScorePoints()));
        row.createCell(c++).setCellValue(nvl(d.getScoringStandard()));
        row.createCell(c++).setCellValue(d.getScore() == null ? "" : d.getScore().toPlainString());
        row.createCell(c++).setCellValue(reviewName(d.getReviewStatus()));
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

  /** 数值等级 → 中文；空值用默认档，越界夹取 */
  private String levelText(String[] texts, Integer level, int defaultLevel) {
    int idx = level == null ? defaultLevel : level;
    idx = Math.max(1, Math.min(texts.length, idx));
    return texts[idx - 1];
  }

  /** 判断题答案用「对/错」不用符号（√/×、true/false 均转换），其余题型原样 */
  private String displayAnswer(TheoryGenDraftEntity d) {
    String a = nvl(d.getAnswer());
    if (!"judge".equals(d.getQuestionType())) {
      return a;
    }
    return switch (a) {
      case "√", "true", "T", "t", "Y", "y" -> "对";
      case "×", "false", "F", "f", "N", "n" -> "错";
      default -> a;
    };
  }

  /** 沿父链取一/二/三级考评范围的「代码 名称」（index 0=一级，1=二级，2=三级） */
  private String[] levelCells(Map<Long, TheoryBlueprintNodeEntity> nodes,
      TheoryBlueprintNodeEntity leaf) {
    String[] out = new String[3];
    TheoryBlueprintNodeEntity cur = leaf;
    int guard = 0;
    while (cur != null && guard++ < 8) { // 树深 4；防御脏数据成环
      int lvl = cur.getNodeLevel() == null ? 0 : cur.getNodeLevel();
      if (lvl >= 1 && lvl <= 3) {
        out[lvl - 1] = nvl(cur.getCode()) + " " + nvl(cur.getName());
      }
      Long pid = cur.getParentId();
      cur = pid == null || pid == 0L ? null : nodes.get(pid);
    }
    return out;
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
