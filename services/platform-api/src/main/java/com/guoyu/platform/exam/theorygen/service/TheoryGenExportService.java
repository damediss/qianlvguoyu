package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.entity.TheoryBlueprintEntity;
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

/** 试题表导出：无主观题时不含主观 4 列；文件名 = 蓝图标题前段 + 理论试题_日期_批次N.xlsx */
@Service
public class TheoryGenExportService {

  private static final String[] HEADERS_BASE = {
      "考评范围一级", "考评范围二级", "考评范围三级", "考评点名称", "等级", "题型", "题干",
      "（A）", "（B）", "（C）", "（D）", "（E）", "答案", "难度"
  };
  private static final String[] HEADERS_SUBJECTIVE = {
      "参考答案（主观）", "评分要点（主观）", "评分标准（主观）", "建议分值"
  };
  private static final String BLUEPRINT_TITLE_SUFFIX = "理论知识考评要素细目表";

  /** 难度等级中文（指引：易、较易、中、较难、难 ↔ 1~5） */
  private static final String[] DIFFICULTY_TEXT = {"易", "较易", "中", "较难", "难"};

  private final TheoryGenDraftService draftService;
  private final TheoryBlueprintApplicationService blueprintService;

  public TheoryGenExportService(TheoryGenDraftService draftService,
      TheoryBlueprintApplicationService blueprintService) {
    this.draftService = draftService;
    this.blueprintService = blueprintService;
  }

  /** 导出文件：xlsx 字节流 + 中文文件名（蓝图标题前段 + 理论试题_日期_批次N.xlsx） */
  public record ExportFile(byte[] content, String filename) {
  }

  /** 导出批次全部草稿为 xlsx；无主观题时不含主观 4 列 */
  public ExportFile exportBatch(Long tenantId, Long batchId) throws IOException {
    List<TheoryGenDraftEntity> drafts = draftService.listAll(tenantId, batchId);
    var blueprint = drafts.isEmpty() ? null
        : blueprintService.findBlueprint(tenantId, drafts.get(0).getBlueprintId());
    // 全量节点一次载入（此前逐行 selectById 曾致公网导出超前端 25s 超时）：行级渲染沿父链取考评范围名称
    Map<Long, TheoryBlueprintNodeEntity> nodeMap = new java.util.HashMap<>();
    if (blueprint != null) {
      for (TheoryBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId,
          blueprint.getId())) {
        nodeMap.put(n.getId(), n);
      }
    }
    boolean hasSubjective = drafts.stream().anyMatch(d -> isSubjective(d.getQuestionType()));
    List<String> headers = new java.util.ArrayList<>(List.of(HEADERS_BASE));
    if (hasSubjective) {
      headers.addAll(List.of(HEADERS_SUBJECTIVE));
    }
    headers.add("审核状态");
    String levelDigit = blueprint == null || blueprint.getLevel() == null
        ? "" : String.valueOf(blueprint.getLevel());

    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = wb.createSheet("试题表");
      Row head = sheet.createRow(0);
      for (int i = 0; i < headers.size(); i++) {
        head.createCell(i).setCellValue(headers.get(i));
      }
      int r = 1;
      for (TheoryGenDraftEntity d : drafts) {
        Row row = sheet.createRow(r++);
        var node = nodeMap.get(d.getNodeId());
        String[] levelNames = levelNames(nodeMap, node);
        int c = 0;
        row.createCell(c++).setCellValue(nvl(levelNames[0]));
        row.createCell(c++).setCellValue(nvl(levelNames[1]));
        row.createCell(c++).setCellValue(nvl(levelNames[2]));
        row.createCell(c++).setCellValue(node == null ? "" : nvl(node.getName()));
        row.createCell(c++).setCellValue(levelDigit);
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
        if (hasSubjective) {
          row.createCell(c++).setCellValue(nvl(d.getReferenceAnswer()));
          row.createCell(c++).setCellValue(nvl(d.getScorePoints()));
          row.createCell(c++).setCellValue(nvl(d.getScoringStandard()));
          row.createCell(c++).setCellValue(d.getScore() == null ? "" : d.getScore().toPlainString());
        }
        row.createCell(c++).setCellValue(reviewName(d.getReviewStatus()));
      }
      wb.write(out);
      return new ExportFile(out.toByteArray(), buildFilename(blueprint, batchId));
    }
  }

  /** 文件名：蓝图标题去掉“理论知识考评要素细目表”后缀 + “理论试题_yyyyMMdd_批次N.xlsx” */
  private String buildFilename(TheoryBlueprintEntity blueprint, Long batchId) {
    String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    String base = "理论知识试题";
    if (blueprint != null && blueprint.getTitle() != null
        && blueprint.getTitle().endsWith(BLUEPRINT_TITLE_SUFFIX)) {
      base = blueprint.getTitle().substring(0,
          blueprint.getTitle().length() - BLUEPRINT_TITLE_SUFFIX.length()) + "理论试题";
    }
    return base + "_" + date + "_批次" + batchId + ".xlsx";
  }

  private boolean isSubjective(String questionType) {
    return "essay".equals(questionType) || "cal".equals(questionType)
        || "draw".equals(questionType) || "discuss".equals(questionType)
        || "case_analysis".equals(questionType);
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

  /** 沿父链取一/二/三级考评范围名称（index 0=一级，1=二级，2=三级；按需求不含代码） */
  private String[] levelNames(Map<Long, TheoryBlueprintNodeEntity> nodes,
      TheoryBlueprintNodeEntity leaf) {
    String[] out = new String[3];
    TheoryBlueprintNodeEntity cur = leaf;
    int guard = 0;
    while (cur != null && guard++ < 8) { // 树深 4；防御脏数据成环
      int lvl = cur.getNodeLevel() == null ? 0 : cur.getNodeLevel();
      if (lvl >= 1 && lvl <= 3) {
        out[lvl - 1] = nvl(cur.getName());
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
