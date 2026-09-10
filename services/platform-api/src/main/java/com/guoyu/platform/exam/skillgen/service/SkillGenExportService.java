package com.guoyu.platform.exam.skillgen.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillBlueprintNodeEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenBatchEntity;
import com.guoyu.platform.exam.skillgen.entity.SkillGenDraftEntity;
import com.guoyu.platform.exam.skillgen.mapper.SkillGenBatchMapper;
import com.guoyu.platform.security.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 操作技能试题表导出（方案 §8，决策 C8）：三 Sheet——
 * 《试题表》/《评分记录表》（一行一个评分要素）/《批次报告》（含 X:Y:Z 题量占比与总量下限达成）。
 */
@Service
public class SkillGenExportService {

  private static final String[] SHEET1_HEADERS = {
      "序号", "考评点代码", "考评点名称", "重要程度", "所属一级", "所属二级", "二级选考方式",
      "试题名称", "本题分值", "权重（%）", "考核时间（min）", "考核形式",
      "考场准备", "考生准备", "具体考核要求", "否定项", "审核状态"
  };
  private static final String[] SHEET2_HEADERS = {
      "试题名称", "考评点", "实操题目", "考场准备", "考生准备",
      "评分要素", "配分", "评分标准", "最大扣分", "步长"
  };

  private final SkillGenDraftService draftService;
  private final SkillGenBatchMapper batchMapper;
  private final SkillBlueprintApplicationService blueprintService;
  private final com.guoyu.platform.exam.mapper.ExamWorkTypeMapper workTypeMapper;
  private final ObjectMapper objectMapper;

  public SkillGenExportService(SkillGenDraftService draftService, SkillGenBatchMapper batchMapper,
      SkillBlueprintApplicationService blueprintService,
      com.guoyu.platform.exam.mapper.ExamWorkTypeMapper workTypeMapper, ObjectMapper objectMapper) {
    this.draftService = draftService;
    this.batchMapper = batchMapper;
    this.blueprintService = blueprintService;
    this.workTypeMapper = workTypeMapper;
    this.objectMapper = objectMapper;
  }

  /** 导出文件：xlsx 字节流 + 中文文件名 */
  public record ExportFile(byte[] content, String filename) {
  }

  public ExportFile exportBatch(Long tenantId, Long batchId) throws IOException {
    SkillGenBatchEntity batch = batchMapper.selectById(batchId);
    if (batch == null || !batch.getTenantId().equals(tenantId)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "生成批次不存在");
    }
    SkillBlueprintEntity bp = blueprintService.findBlueprint(tenantId, batch.getBlueprintId());
    List<SkillGenDraftEntity> drafts = draftService.listAll(tenantId, batchId);
    Map<Long, SkillBlueprintNodeEntity> nodeMap = new LinkedHashMap<>();
    if (bp != null) {
      for (SkillBlueprintNodeEntity n : blueprintService.findBlueprintNodes(tenantId, bp.getId())) {
        nodeMap.put(n.getId(), n);
      }
    }

    try (Workbook wb = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      writeSheet1(wb, drafts, nodeMap);
      writeSheet2(wb, drafts, nodeMap);
      writeSheet3(wb, tenantId, batch, bp, drafts, nodeMap);
      wb.write(out);
      return new ExportFile(out.toByteArray(), buildFilename(workTypeTitle(bp), bp == null ? null : bp.getLevel(), batchId));
    }
  }

  /** 工种名（exam_work_type.title）；查不到时回退蓝图标题前缀 */
  private String workTypeTitle(SkillBlueprintEntity bp) {
    if (bp == null) {
      return "";
    }
    com.guoyu.platform.exam.entity.ExamWorkTypeEntity wt =
        workTypeMapper.selectById(Long.valueOf(bp.getWorkTypeId()));
    if (wt != null && wt.getTitle() != null && !wt.getTitle().isBlank()) {
      return wt.getTitle();
    }
    return "";
  }

  /** 文件名规则（用户 2026-09-10 确认）：工种(等级)_操作技能试题_批次{短号}.xlsx，等级形如"二级-技师" */
  static String buildFilename(String workTypeTitle, Integer level, Long batchId) {
    String wt = workTypeTitle == null || workTypeTitle.isBlank() ? "未指定工种" : workTypeTitle.trim();
    String levelText = levelText(level);
    String shortId = String.valueOf(batchId);
    if (shortId.length() > 8) {
      shortId = shortId.substring(shortId.length() - 8);
    }
    return String.format("%s(%s)_操作技能试题_批次%s.xlsx", wt, levelText, shortId);
  }

  /** 等级中文写法："二级-技师"（1~5 → 一/二/三/四/五级-等级名）；level 为空时退化为"未知等级" */
  static String levelText(Integer level) {
    if (level == null || level < 1 || level > 5) {
      return "未知等级";
    }
    String[] digits = {"一", "二", "三", "四", "五"};
    String name = SkillBlueprintApplicationService.LEVEL_NAMES[level];
    return digits[level - 1] + "级-" + name;
  }

  /** Sheet1 试题表：一行一题，三件套完整展开 */
  private void writeSheet1(Workbook wb, List<SkillGenDraftEntity> drafts,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    Sheet sheet = wb.createSheet("试题表");
    Row head = sheet.createRow(0);
    for (int i = 0; i < SHEET1_HEADERS.length; i++) {
      head.createCell(i).setCellValue(SHEET1_HEADERS[i]);
    }
    int r = 1;
    int idx = 1;
    for (SkillGenDraftEntity d : drafts) {
      SkillBlueprintNodeEntity point = nodeMap.get(d.getNodeId());
      SkillBlueprintNodeEntity l2 = parentOf(point, nodeMap);
      SkillBlueprintNodeEntity l1 = parentOf(l2, nodeMap);
      Row row = sheet.createRow(r++);
      int c = 0;
      row.createCell(c++).setCellValue(idx++);
      row.createCell(c++).setCellValue(nvl(point == null ? "" : point.getFullCode()));
      row.createCell(c++).setCellValue(nvl(point == null ? "" : point.getName()));
      row.createCell(c++).setCellValue(nvl(point == null ? "" : point.getImportance()));
      row.createCell(c++).setCellValue(nvl(l1 == null ? "" : l1.getName()));
      row.createCell(c++).setCellValue(nvl(l2 == null ? "" : l2.getName()));
      row.createCell(c++).setCellValue(nvl(l2 == null ? "" : l2.getSelectMode()));
      row.createCell(c++).setCellValue(nvl(d.getQuestionName()));
      row.createCell(c++).setCellValue(d.getScore() == null ? "100" : d.getScore().toPlainString());
      row.createCell(c++).setCellValue(d.getWeight() == null ? "" : d.getWeight().toPlainString());
      row.createCell(c++).setCellValue(d.getDurationMinutes() == null ? ""
          : String.valueOf(d.getDurationMinutes()));
      row.createCell(c++).setCellValue(nvl(d.getExamForm()));
      row.createCell(c++).setCellValue(draftService.renderKaochangForExport(d));
      row.createCell(c++).setCellValue(draftService.renderKaoshengForExport(d));
      row.createCell(c++).setCellValue(nvl(d.getRequirements()));
      row.createCell(c++).setCellValue(nvl(d.getNegatives()));
      row.createCell(c).setCellValue(reviewName(d.getReviewStatus()));
    }
  }

  /** Sheet2 评分记录表：一行一个评分要素（试题级字段按题重复填充） */
  private void writeSheet2(Workbook wb, List<SkillGenDraftEntity> drafts,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    Sheet sheet = wb.createSheet("评分记录表");
    Row head = sheet.createRow(0);
    for (int i = 0; i < SHEET2_HEADERS.length; i++) {
      head.createCell(i).setCellValue(SHEET2_HEADERS[i]);
    }
    int r = 1;
    for (SkillGenDraftEntity d : drafts) {
      SkillBlueprintNodeEntity point = nodeMap.get(d.getNodeId());
      List<Map<String, String>> rows = parseScoring(d.getScoringItems());
      int no = 1;
      for (Map<String, String> item : rows) {
        Row row = sheet.createRow(r++);
        int c = 0;
        row.createCell(c++).setCellValue(nvl(d.getQuestionName()));
        row.createCell(c++).setCellValue(nvl(point == null ? "" : point.getFullCode()));
        row.createCell(c++).setCellValue(nvl(d.getRequirements()));
        row.createCell(c++).setCellValue(draftService.renderKaochangForExport(d));
        row.createCell(c++).setCellValue(draftService.renderKaoshengForExport(d));
        row.createCell(c++).setCellValue(no++ + ". " + nvl(item.get("item")));
        row.createCell(c++).setCellValue(nvl(item.get("score")));
        row.createCell(c++).setCellValue(nvl(item.get("scoreDesc")));
        row.createCell(c++).setCellValue(nvl(item.get("maxDeduct")));
        row.createCell(c).setCellValue(nvl(item.get("step")));
      }
    }
  }

  /** Sheet3 批次报告：模型快照/各二级块分布/XYZ 题量占比/总量下限达成 */
  private void writeSheet3(Workbook wb, Long tenantId, SkillGenBatchEntity batch,
      SkillBlueprintEntity bp, List<SkillGenDraftEntity> drafts,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    Sheet sheet = wb.createSheet("批次报告");
    int r = 0;
    r = kv(sheet, r, "工种", bp == null ? "" : String.valueOf(bp.getWorkTypeId()));
    r = kv(sheet, r, "等级", bp == null ? "" :
        (bp.getLevel() + "级/" + SkillBlueprintApplicationService.LEVEL_NAMES[bp.getLevel()]));
    r = kv(sheet, r, "细目表批次", batch.getBlueprintId() == null ? ""
        : String.valueOf(batch.getBlueprintId()));
    r = kv(sheet, r, "生成批次", String.valueOf(batch.getId()));
    r = kv(sheet, r, "模型与思考模式", nvl(batch.getModelId())
        + "（thinking=" + nvl(batch.getThinking()) + "，effort=" + nvl(batch.getReasoningEffort()) + "）");
    r = kv(sheet, r, "每考评点题量", String.valueOf(SkillBlueprintApplicationService.PER_POINT_QUOTA));
    r = kv(sheet, r, "已生成试题数", drafts.size() + "（完成 " + nvl(batch.getDonePoints()) + "/"
        + nvl(batch.getTotalPoints()) + " 个考评点）");

    // 各二级块题量分布与选考方式
    r = kv(sheet, r, "", "");
    r = kv(sheet, r, "各二级块题量分布", "题量 | 一级 | 二级 | 二级选考方式");
    Map<String, Integer> byL2 = new LinkedHashMap<>();
    Map<String, String> l2Meta = new LinkedHashMap<>();
    Map<String, Integer> byImportance = new LinkedHashMap<>();
    for (SkillGenDraftEntity d : drafts) {
      SkillBlueprintNodeEntity point = nodeMap.get(d.getNodeId());
      SkillBlueprintNodeEntity l2 = parentOf(point, nodeMap);
      SkillBlueprintNodeEntity l1 = parentOf(l2, nodeMap);
      String key = nvl(l1 == null ? "" : l1.getName()) + " | " + nvl(l2 == null ? "" : l2.getName());
      byL2.merge(key, 1, Integer::sum);
      if (l2 != null) {
        l2Meta.putIfAbsent(key, nvl(l2.getSelectMode()));
      }
      String imp = point == null || point.getImportance() == null ? "X" : point.getImportance();
      byImportance.merge(imp, 1, Integer::sum);
    }
    for (Map.Entry<String, Integer> e : byL2.entrySet()) {
      r = kv(sheet, r, e.getValue() + " 题", e.getKey() + " | " + l2Meta.getOrDefault(e.getKey(), ""));
    }

    // X/Y/Z 题量占比（附件 3 第 22 条核对信息，仅展示不拦截）
    r = kv(sheet, r, "", "");
    int total = Math.max(1, drafts.size());
    r = kv(sheet, r, "X/Y/Z 题量占比", String.format("X %d（%.1f%%）| Y %d（%.1f%%）| Z %d（%.1f%%）",
        byImportance.getOrDefault("X", 0), byImportance.getOrDefault("X", 0) * 100.0 / total,
        byImportance.getOrDefault("Y", 0), byImportance.getOrDefault("Y", 0) * 100.0 / total,
        byImportance.getOrDefault("Z", 0), byImportance.getOrDefault("Z", 0) * 100.0 / total));

    // 总量下限达成（累计口径，S10）
    if (bp != null) {
      int floor = SkillBlueprintApplicationService.MIN_QUESTIONS.getOrDefault(bp.getLevel(), 50);
      SkillBlueprintApplicationService.CumulativeStat stat =
          blueprintService.cumulativeStat(tenantId, bp.getWorkTypeId(), bp.getLevel());
      r = kv(sheet, r, "题库总量下限", String.valueOf(floor));
      r = kv(sheet, r, "累计已入库（全部来源）", stat.total() + "（其中 AI 生成 " + stat.aiTotal() + "）");
      r = kv(sheet, r, "下限达成", stat.total() >= floor ? "已达成" : "未达成（还差 " + (floor - stat.total()) + " 道）");
    }
    r = kv(sheet, r, "", "");
    r = kv(sheet, r, "导出人", SecurityContext.currentUser().getUserId() == null ? ""
        : String.valueOf(SecurityContext.currentUser().getUserId()));
    kv(sheet, r, "导出时间", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
  }

  private int kv(Sheet sheet, int r, String key, String value) {
    Row row = sheet.createRow(r);
    row.createCell(0).setCellValue(key);
    row.createCell(1).setCellValue(value);
    return r + 1;
  }

  private SkillBlueprintNodeEntity parentOf(SkillBlueprintNodeEntity node,
      Map<Long, SkillBlueprintNodeEntity> nodeMap) {
    if (node == null) {
      return null;
    }
    Long pid = node.getParentId();
    return pid == null || pid == 0L ? null : nodeMap.get(pid);
  }

  private List<Map<String, String>> parseScoring(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() { });
    } catch (Exception e) {
      return List.of();
    }
  }

  private String reviewName(String s) {
    return switch (s == null ? "" : s) {
      case "approved" -> "已通过";
      case "rejected" -> "已驳回";
      default -> "待审";
    };
  }

  private String nvl(Object o) {
    return o == null ? "" : String.valueOf(o);
  }
}
