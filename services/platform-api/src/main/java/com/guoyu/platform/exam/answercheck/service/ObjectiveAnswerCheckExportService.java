package com.guoyu.platform.exam.answercheck.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckBatchEntity;
import com.guoyu.platform.exam.answercheck.entity.ObjectiveAnswerCheckItemEntity;
import com.guoyu.platform.exam.answercheck.vo.ObjectiveAnswerItemDetailVO;
import com.guoyu.platform.exam.support.QuestionContentQualityWarning;
import com.guoyu.platform.exam.vo.ExportResultDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * 导出（方案 §14.1）：基于批次快照生成，不重新组题、不改写 AI 结果。
 * 置信度为数值 + 百分比格式，不写成带 % 的字符串。
 */
@Slf4j
@Service
public class ObjectiveAnswerCheckExportService {

  private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final ObjectMapper objectMapper;

  public ObjectiveAnswerCheckExportService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ExportResultDTO export(ObjectiveAnswerCheckBatchEntity batch,
      List<ObjectiveAnswerCheckItemEntity> items) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      CellStyle percent = workbook.createCellStyle();
      percent.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));

      writeBatchSheet(workbook, batch, items);
      writeResultSheet(workbook, batch, items, percent);

      workbook.write(out);
      String suffix = String.valueOf(batch.getId());
      suffix = suffix.length() > 8 ? suffix.substring(suffix.length() - 8) : suffix;
      String filename = "客观题答案AI检查_" + suffix + "_"
          + LocalDateTime.now().format(FILE_TIME) + ".xlsx";
      return new ExportResultDTO(out.toByteArray(), filename);
    } catch (IOException e) {
      throw new IllegalStateException("导出失败: " + e.getMessage(), e);
    }
  }

  private void writeBatchSheet(XSSFWorkbook workbook, ObjectiveAnswerCheckBatchEntity batch,
      List<ObjectiveAnswerCheckItemEntity> items) {
    Sheet sheet = workbook.createSheet("批次信息");
    int rowIndex = 0;
    rowIndex = kv(sheet, rowIndex, "批次 ID", String.valueOf(batch.getId()));
    rowIndex = kv(sheet, rowIndex, "筛选快照", filterSummary(batch.getFilterJson()));
    rowIndex = kv(sheet, rowIndex, "模型", batch.getModelId());
    rowIndex = kv(sheet, rowIndex, "JEV Base URL", batch.getJevBaseUrl());
    rowIndex = kv(sheet, rowIndex, "JEV 派发并发", batch.getJevConcurrency());
    rowIndex = kv(sheet, rowIndex, "JEV 读超时（秒）", batch.getJevReadTimeoutSeconds());
    rowIndex = kv(sheet, rowIndex, "提示词版本", batch.getPromptVersion());
    rowIndex = kv(sheet, rowIndex, "高置信门槛(AI置信度)", batch.getConfidenceThreshold());
    rowIndex = kv(sheet, rowIndex, "高置信门槛(概率差)", batch.getMarginThreshold());
    rowIndex = kv(sheet, rowIndex, "总数", batch.getTotal());
    rowIndex = kv(sheet, rowIndex, "已完成", batch.getDone());
    rowIndex = kv(sheet, rowIndex, "高置信一致", batch.getConsistentHigh());
    rowIndex = kv(sheet, rowIndex, "高置信不一致", batch.getMismatchHigh());
    rowIndex = kv(sheet, rowIndex, "低置信复核", batch.getLowConfidence());
    rowIndex = kv(sheet, rowIndex, "答案结构异常", batch.getInvalid());
    rowIndex = kv(sheet, rowIndex, "检查失败", batch.getFailed());
    rowIndex = kv(sheet, rowIndex, "内容格式疑似", batch.getContentSuspect());
    rowIndex = kv(sheet, rowIndex, "内容格式确定异常", batch.getContentInvalid());
    rowIndex = kv(sheet, rowIndex, "预计逻辑调用", batch.getExpectedCalls());
    rowIndex = kv(sheet, rowIndex, "实际逻辑调用", batch.getCallCount());
    rowIndex = kv(sheet, rowIndex, "精确输入 token", batch.getInputTokensExact());
    rowIndex = kv(sheet, rowIndex, "估算输入 token", batch.getInputTokensEstimated());
    rowIndex = kv(sheet, rowIndex, "含估算调用数", batch.getUsageEstimatedCalls());
    rowIndex = kv(sheet, rowIndex, "输出 token", batch.getOutputTokens());
    rowIndex = kv(sheet, rowIndex, "创建人", batch.getCreatedBy());
    rowIndex = kv(sheet, rowIndex, "开始时间", format(batch.getStartedAt()));
    rowIndex = kv(sheet, rowIndex, "结束时间", format(batch.getFinishedAt()));
    kv(sheet, rowIndex, "导出记录数", items.size());
    sheet.setColumnWidth(0, 26 * 256);
    sheet.setColumnWidth(1, 60 * 256);
  }

  private int kv(Sheet sheet, int rowIndex, String key, Object value) {
    Row row = sheet.createRow(rowIndex);
    row.createCell(0).setCellValue(key);
    Cell cell = row.createCell(1);
    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
    } else {
      cell.setCellValue(value == null ? "" : String.valueOf(value));
    }
    return rowIndex + 1;
  }

  private void writeResultSheet(XSSFWorkbook workbook, ObjectiveAnswerCheckBatchEntity batch,
      List<ObjectiveAnswerCheckItemEntity> items, CellStyle percent) {
    Sheet sheet = workbook.createSheet("检查结果");
    String[] headers = {"批次ID", "试题ID", "工种", "等级", "课程", "知识点路径", "来源", "题型",
        "题干", "选项明细", "原解析", "原答案", "AI答案", "原答案支持度", "AI置信度", "最小概率差",
        "一致性", "检查结果", "内容质量", "格式规则", "格式位置", "匹配文本", "转换候选",
        "异常/失败原因", "需人工复核", "人工复核", "复核备注", "模型", "检查时间", "逻辑调用ID"};
    Row header = sheet.createRow(0);
    for (int i = 0; i < headers.length; i++) {
      header.createCell(i).setCellValue(headers[i]);
      sheet.setColumnWidth(i, (i == 8 || i == 9 || i == 10 ? 50 : 18) * 256);
    }

    int rowIndex = 1;
    for (ObjectiveAnswerCheckItemEntity item : items) {
      Row row = sheet.createRow(rowIndex++);
      int col = 0;
      row.createCell(col++).setCellValue(String.valueOf(batch.getId()));
      row.createCell(col++).setCellValue(item.getDetailId() == null ? ""
          : String.valueOf(item.getDetailId()));
      row.createCell(col++).setCellValue(nvl(item.getWorkTypeTitle()));
      row.createCell(col++).setCellValue(item.getLevelCode() == null ? ""
          : String.valueOf(item.getLevelCode()));
      row.createCell(col++).setCellValue(nvl(item.getCourseTitle()));
      row.createCell(col++).setCellValue(nvl(item.getKnowledgePath()));
      row.createCell(col++).setCellValue(ObjectiveAnswerCheckApplicationService.sourceLabel(item.getSource()));
      row.createCell(col++).setCellValue(ObjectiveAnswerCheckApplicationService.typeLabel(item.getQuestionType()));
      row.createCell(col++).setCellValue(nvl(item.getStemText()));
      row.createCell(col++).setCellValue(optionSummary(item));
      row.createCell(col++).setCellValue(nvl(item.getResolveSnapshot()));
      row.createCell(col++).setCellValue(answerText(item, item.getOriginalAnswerKeys()));
      row.createCell(col++).setCellValue(answerText(item, item.getAiAnswerKeys()));
      cellPercent(row.createCell(col++), item.getReferenceConfidence(), percent);
      cellPercent(row.createCell(col++), item.getAiConfidence(), percent);
      cellPercent(row.createCell(col++), item.getMinMargin(), percent);
      row.createCell(col++).setCellValue(item.getConsistent() == null ? ""
          : item.getConsistent() == 1 ? "一致" : "不一致");
      row.createCell(col++).setCellValue(resultLabel(item.getResultCode()));
      row.createCell(col++).setCellValue(contentQualityLabel(item.getContentQuality()));
      List<QuestionContentQualityWarning> warnings = warnings(item);
      row.createCell(col++).setCellValue(warnings.stream()
          .map(QuestionContentQualityWarning::ruleCode).distinct().collect(Collectors.joining(";")));
      row.createCell(col++).setCellValue(warnings.stream()
          .map(w -> w.location() + (w.optionId() == null ? "" : ":" + w.optionId())).distinct()
          .collect(Collectors.joining(";")));
      row.createCell(col++).setCellValue(warnings.stream()
          .map(QuestionContentQualityWarning::matchedText).filter(java.util.Objects::nonNull)
          .distinct().collect(Collectors.joining(";")));
      row.createCell(col++).setCellValue(warnings.stream()
          .map(QuestionContentQualityWarning::suggestedText).filter(java.util.Objects::nonNull)
          .distinct().collect(Collectors.joining(";")));
      row.createCell(col++).setCellValue(nvl(item.getErrorMessage()));
      row.createCell(col++).setCellValue(needsReview(item) ? "是" : "否");
      row.createCell(col++).setCellValue(reviewLabel(item.getReviewStatus()));
      row.createCell(col++).setCellValue(nvl(item.getReviewNote()));
      row.createCell(col++).setCellValue(nvl(batch.getModelId()));
      row.createCell(col++).setCellValue(format(item.getCheckedAt()));
      row.createCell(col++).setCellValue(callId(item));
    }
  }

  private void cellPercent(Cell cell, BigDecimal value, CellStyle style) {
    if (value == null) {
      cell.setCellValue("");
      return;
    }
    cell.setCellValue(value.doubleValue());
    cell.setCellStyle(style);
  }

  private List<QuestionContentQualityWarning> warnings(ObjectiveAnswerCheckItemEntity item) {
    if (item.getContentWarningsJson() == null || item.getContentWarningsJson().isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(item.getContentWarningsJson(),
          new TypeReference<List<QuestionContentQualityWarning>>() {
          });
    } catch (Exception e) {
      return List.of();
    }
  }

  private String optionSummary(ObjectiveAnswerCheckItemEntity item) {
    List<Map<String, Object>> options = options(item);
    List<String> parts = new ArrayList<>();
    for (Map<String, Object> option : options) {
      parts.add(option.get("label") + ". " + option.get("text"));
    }
    return String.join("\n", parts);
  }

  private String answerText(ObjectiveAnswerCheckItemEntity item, String keys) {
    if (keys == null || keys.isBlank()) {
      return "";
    }
    Set<String> keySet = new LinkedHashSet<>(List.of(keys.split(",")));
    List<Map<String, Object>> options = options(item);
    List<String> parts = new ArrayList<>();
    for (String key : keySet) {
      for (Map<String, Object> option : options) {
        if (key.equals(String.valueOf(option.get("optionId")))) {
          parts.add("judge".equals(item.getQuestionType()) ? String.valueOf(option.get("text"))
              : option.get("label") + ". " + option.get("text"));
          break;
        }
      }
    }
    return String.join("；", parts);
  }

  private List<Map<String, Object>> options(ObjectiveAnswerCheckItemEntity item) {
    try {
      return objectMapper.readValue(item.getOptionSnapshotJson(), new TypeReference<>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

  private boolean needsReview(ObjectiveAnswerCheckItemEntity item) {
    if (item.getResultCode() == null || !"unreviewed".equals(item.getReviewStatus())) {
      return false;
    }
    return Set.of("mismatch_high", "low_confidence", "invalid", "failed")
        .contains(item.getResultCode()) || !"clean".equals(item.getContentQuality());
  }

  private String callId(ObjectiveAnswerCheckItemEntity item) {
    return item.getId() == null ? "" : String.valueOf(item.getId());
  }

  private String resultLabel(String code) {
    if (code == null) {
      return "";
    }
    return switch (code) {
      case "consistent_high" -> "高置信一致";
      case "mismatch_high" -> "高置信不一致";
      case "low_confidence" -> "低置信复核";
      case "invalid" -> "答案结构异常";
      case "failed" -> "检查失败";
      default -> code;
    };
  }

  private String contentQualityLabel(String quality) {
    if (quality == null) {
      return "";
    }
    return switch (quality) {
      case "clean" -> "正常";
      case "suspect" -> "格式疑似异常";
      case "invalid" -> "格式确定异常";
      default -> quality;
    };
  }

  private String reviewLabel(String status) {
    if (status == null) {
      return "";
    }
    return switch (status) {
      case "unreviewed" -> "未复核";
      case "confirmed_correct" -> "确认原答案无误";
      case "needs_edit" -> "需要修改";
      case "deferred" -> "暂缓处理";
      default -> status;
    };
  }

  private String filterSummary(String filterJson) {
    return filterJson == null ? "" : filterJson;
  }

  private String format(LocalDateTime time) {
    return time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
  }

  private String nvl(String text) {
    return text == null ? "" : text;
  }
}
