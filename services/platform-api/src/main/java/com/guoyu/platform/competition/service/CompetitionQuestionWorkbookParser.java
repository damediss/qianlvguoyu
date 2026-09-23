package com.guoyu.platform.competition.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 竞赛题库导入解析器。解析结果只存在内存中，持久化由事务服务一次性完成。
 * 理论模板是一行一题；实操模板支持一行一题/评分项以及认定模块常用的评分记录表。
 */
public class CompetitionQuestionWorkbookParser {

  private static final Pattern SCORE_PATTERN = Pattern.compile("[（(]\\s*(\\d+(?:\\.\\d+)?)\\s*分[）)]");
  private static final String[] OPTION_KEYS = {"A", "B", "C", "D", "E", "F", "G", "H"};
  private final DataFormatter formatter = new DataFormatter();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public ParsedWorkbook parse(Path file, String contentType) throws IOException {
    return parse(file, contentType, null);
  }

  public ParsedWorkbook parse(Path file, String contentType, String selectedSheet) throws IOException {
    if (file == null || contentType == null) {
      throw new IllegalArgumentException("导入文件和题目类型不能为空");
    }
    try (Workbook workbook = WorkbookFactory.create(file.toFile(), null, true)) {
      if ("theory".equalsIgnoreCase(contentType)) {
        Sheet sheet = workbook.getSheetAt(0);
        return new ParsedWorkbook(parseTheory(sheet), sheet.getSheetName());
      }
      if ("practical".equalsIgnoreCase(contentType)) {
        PracticalParseResult practical = parsePractical(workbook, selectedSheet);
        return new ParsedWorkbook(practical.questions(), practical.selectedSheet());
      }
      throw new IllegalArgumentException("不支持的题目类型: " + contentType);
    }
  }

  private List<ParsedQuestion> parseTheory(Sheet sheet) {
    Header header = findHeader(sheet, true);
    Map<String, String> merged = collectMergedValues(sheet);
    List<ParsedQuestion> result = new ArrayList<>();
    int sequenceIndex = header.index("序号", "题号", "题目序号");
    int pointCodeIndex = header.index("考评点完整编码", "考评点代码", "鉴定点代码", "考评点编号");
    int pointNameIndex = header.index("考评点名称", "鉴定点", "考评点");
    int pathIndex = header.index("层级路径", "考评点路径", "考核范围路径");
    int implicitSequence = 0;
    for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlank(row)) {
        continue;
      }
      String sequenceText = sequenceIndex < 0 ? "" : value(sheet, row, sequenceIndex, merged);
      int sequence = sequenceText.isBlank() ? ++implicitSequence : parseInt(sequenceText, rowIndex, "序号");
      implicitSequence = Math.max(implicitSequence, sequence);
      String type = normalizeTheoryType(value(sheet, row, header.index("题型", "试题类型", "类型"), merged));
      if (type == null) {
        throw rowError(rowIndex, "题型必须是单选、多选、判断或填空");
      }
      String stem = value(sheet, row, header.index("题干", "题目", "题目内容", "试题内容"), merged);
      if (stem.isBlank()) {
        throw rowError(rowIndex, "题干不能为空");
      }
      String referenceAnswer = value(sheet, row, header.index("参考答案", "答案", "正确答案"), merged);
      String fillAnswer = value(sheet, row, header.index("回答", "填空答案", "填空回答"), merged);
      Map<String, String> options = new LinkedHashMap<>();
      for (String option : OPTION_KEYS) {
        String text = value(sheet, row, header.index("选项" + option, option, "选项 " + option), merged);
        if (!text.isBlank()) {
          options.put(option, text);
        }
      }
      validateAnswer(type, referenceAnswer, fillAnswer, options, rowIndex);
      Integer difficulty = parseOptionalInt(value(sheet, row, header.index("难度", "难易度"), merged), rowIndex, "难度");
      String pointCode = value(sheet, row, pointCodeIndex, merged);
      String pointName = value(sheet, row, pointNameIndex, merged);
      String pointPath = value(sheet, row, pathIndex, merged);
      if (pointPath.isBlank()) pointPath = joinPath(sheet, row, 0, 5, merged);
      result.add(new ParsedQuestion(
          sequence, type, stem, referenceAnswer.isBlank() ? null : referenceAnswer,
          fillAnswer.isBlank() ? null : fillAnswer, optionsJson(options), difficulty,
          null, null, null, List.of(),
          null, null, null, null, null, null, null, null, null, List.of(),
          false, false, pointCode, pointPath, pointName));
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("理论模板没有可导入的题目");
    }
    return result;
  }

  private PracticalParseResult parsePractical(Workbook workbook, String selectedSheet) {
    List<SheetCandidate> candidates = new ArrayList<>();
    for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
      Sheet sheet = workbook.getSheetAt(sheetIndex);
      Header header = findPracticalTableHeader(sheet);
      if (header != null) {
        // A number of historical workbooks contain a preparation-only sheet before
        // the actual scoring-record sheet.  Parse every recognized table and keep
        // only sheets that contain complete scoring rows instead of returning the
        // first (empty) table.
        List<ParsedQuestion> parsed = parsePracticalTable(sheet, header);
        if (!parsed.isEmpty()) candidates.add(new SheetCandidate(sheet.getSheetName(), parsed));
      }
    }

    if (candidates.isEmpty()) {
      for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
        ParsedQuestion question = parseScoreRecordSheet(workbook.getSheetAt(sheetIndex), 1);
        if (question != null) candidates.add(new SheetCandidate(workbook.getSheetName(sheetIndex), List.of(question)));
      }
    }
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("实操模板没有可导入的题目或评分项");
    }
    if (selectedSheet != null && !selectedSheet.isBlank()) {
      SheetCandidate selected = candidates.stream().filter(candidate -> selectedSheet.equals(candidate.sheetName())).findFirst().orElse(null);
      if (selected == null) throw new IllegalArgumentException("指定工作表不存在或没有有效题目: " + selectedSheet);
      return new PracticalParseResult(selected.questions(), selected.sheetName());
    }
    candidates.sort((left, right) -> {
      // Coverage is the primary safety criterion: a sheet with more rows but
      // only one assessment point must not win over a complete sheet merely
      // because it contains duplicated/irrelevant questions.  Question count
      // breaks a coverage tie; a full tie is deliberately surfaced to the UI.
      int byCoverage = Integer.compare(right.coverage(), left.coverage());
      if (byCoverage != 0) return byCoverage;
      return Integer.compare(right.questions().size(), left.questions().size());
    });
    SheetCandidate best = candidates.get(0);
    if (candidates.size() > 1 && best.coverage() == candidates.get(1).coverage()) {
      throw new IllegalArgumentException("实操题库存在考评点覆盖率并列的工作表，请明确选择："
          + best.sheetName() + "、" + candidates.get(1).sheetName());
    }
    return new PracticalParseResult(best.questions(), best.sheetName());
  }

  private List<ParsedQuestion> parsePracticalTable(Sheet sheet, Header header) {
    Map<String, String> merged = collectMergedValues(sheet);
    Map<Integer, PracticalBuilder> builders = new LinkedHashMap<>();
    boolean legacyOriginalLayout = header.index("考评点") >= 0
        && header.index("实操题目", "实操题") >= 0
        && header.index("评分要素", "评分项名称", "评分项", "要素") >= 0
        && header.index("配分", "最大分值", "评分项分值", "项目分值") >= 0
        && header.index("评分标准", "标准", "评分要求") >= 0;
    boolean structuredTemplate = header.index("考试时间（分钟）", "考试时间", "时长") >= 0
        && header.index("评分差值") >= 0
        && header.index("试题总分", "总分") >= 0
        && header.index("评分方式") >= 0;
    int sequenceIndex = header.index("序号", "题号", "题目序号");
    int stemIndex = header.index("题干", "题目", "题目内容", "试题内容", "实操题目", "实操题");
    int allocationIndex = header.index("是否配分", "配分开关");
    int maxScoreIndex = header.index("配分", "最大分值", "评分项分值", "项目分值", "最大扣分");
    int contentOrderIndex = header.index("考核内容序号", "内容序号");
    int contentHtmlIndex = header.index("考核内容", "考核内容名称");
    // The new competition template calls this column “考评点名称”, while the
    // historical score-record workbook uses the shorter “考评点”.  Resolve
    // both forms so point-name matching is not silently lost on import.
    int assessmentPointIndex = header.index("考评点名称", "考评点", "鉴定点", "鉴定点名称");
    int assessmentPointCodeIndex = header.index("考评点完整编码", "考评点代码", "考评点编号");
    int assessmentPointPathIndex = header.index("层级路径", "考评点路径", "考核范围路径");
    int implicitSequence = 0;
    Integer currentSequence = null;
    for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlank(row)) continue;

      String sequenceText = sequenceIndex < 0 ? "" : value(sheet, row, sequenceIndex, merged);
      int sequence;
      if (!sequenceText.isBlank()) {
        sequence = parseInt(sequenceText, rowIndex, "序号");
        currentSequence = sequence;
        implicitSequence = Math.max(implicitSequence, sequence);
      } else if (sequenceIndex >= 0 && currentSequence != null) {
        // Merged cells in both the old score-record workbook and hand-edited
        // versions of the new template are blank below the first row.
        sequence = currentSequence;
      } else {
        String stemCandidate = sequenceIndex < 0 ? value(row, stemIndex) : value(sheet, row, stemIndex, merged);
        if (!stemCandidate.isBlank()) {
          sequence = ++implicitSequence;
          currentSequence = sequence;
        } else if (currentSequence != null) {
          sequence = currentSequence;
        } else {
          throw rowError(rowIndex, "序号不能为空（旧版评分记录表请在每道实操题首行填写实操题目）");
        }
      }

      PracticalBuilder builder = builders.computeIfAbsent(sequence,
          key -> new PracticalBuilder(key, structuredTemplate, legacyOriginalLayout));
      String stem = sequenceIndex < 0 ? value(row, stemIndex) : value(sheet, row, stemIndex, merged);
      if (!stem.isBlank()) builder.stem = stem;
      String operation = value(row, header.index("操作内容", "实操内容", "操作要求"));
      if (!operation.isBlank()) builder.operationContent = operation;
      String student = value(row, header.index("考生准备", "学生准备"));
      if (!student.isBlank()) builder.studentPrepare = student;
      String equipment = value(row, header.index("设备准备", "设备器材", "设备准备清单"));
      if (!equipment.isBlank()) builder.equipmentPrepare = equipment;
      String assessmentPoint = value(sheet, row, assessmentPointIndex, merged);
      if (!assessmentPoint.isBlank()) builder.assessmentPoint = assessmentPoint;
      String assessmentPointCode = value(sheet, row, assessmentPointCodeIndex, merged);
      if (!assessmentPointCode.isBlank()) builder.assessmentPointCode = assessmentPointCode;
      String assessmentPointPath = value(sheet, row, assessmentPointPathIndex, merged);
      if (!assessmentPointPath.isBlank()) builder.assessmentPointPath = assessmentPointPath;
      builder.durationMinutes = firstInt(builder.durationMinutes,
          parseOptionalInt(value(row, header.index("考试时间（分钟）", "考试时间", "时长")), rowIndex, "考试时间"));
      builder.scoreDifference = firstDecimal(builder.scoreDifference,
          parseOptionalDecimal(value(row, header.index("评分差值")), rowIndex, "评分差值"));
      builder.totalScore = firstDecimal(builder.totalScore,
          parseOptionalDecimal(value(row, header.index("试题总分", "总分")), rowIndex, "试题总分"));
      String scoringMethod = value(row, header.index("评分方式"));
      if (!scoringMethod.isBlank()) builder.scoringMethod = scoringMethod;
      String requirements = value(row, header.index("考核要求"));
      if (!requirements.isBlank()) builder.operationContent = requirements;
      String room = value(row, header.index("考场准备"));
      if (!room.isBlank()) builder.equipmentPrepare = room;
      String negative = value(row, header.index("否定项"));
      if (!negative.isBlank()) builder.negativeItemsHtml = negative;
      String materials = value(row, header.index("试题材料"));
      if (!materials.isBlank()) builder.questionMaterialsHtml = materials;
      String itemsJson = value(row, header.index("评分项JSON", "评分项", "评分标准JSON"));
      if (!itemsJson.isBlank() && itemsJson.trim().startsWith("[")) {
        builder.items.addAll(parseScoreItemsJson(itemsJson, rowIndex));
      } else {
        String itemName = value(row, header.index("评分要素", "评分项名称", "评分项", "要素"));
        if (!itemName.isBlank()) {
          String maxScoreText = value(row, maxScoreIndex);
          // The original nine-column score-record layout has no allocation
          // switch.  A blank 配分 means “不配分”; a number means allocated.
          boolean allocated = allocationIndex >= 0
              ? parseBoolean(value(row, allocationIndex), !maxScoreText.isBlank())
              : !maxScoreText.isBlank();
          BigDecimal maxScore = allocated
              ? parseDecimal(maxScoreText, rowIndex, "配分")
              : null;
          BigDecimal maxDeduction = parseOptionalDecimal(value(row, header.index("最大扣分", "扣分上限")), rowIndex, "最大扣分");
          BigDecimal step = parseOptionalDecimal(value(row, header.index("扣分步长", "步长")), rowIndex, "扣分步长");
          String criterion = value(row, header.index("评分标准", "标准", "评分要求"));
          Integer parsedItemNo = parseOptionalInt(value(row, header.index("评分项序号", "项目序号", "项号")), rowIndex, "评分项序号");
          int itemNo = parsedItemNo == null ? builder.items.size() + 1 : parsedItemNo;
          builder.items.add(new ParsedScoreItem(itemNo, itemName, criterion, maxScore, maxDeduction, step,
              itemName, allocated));
          Integer contentOrder = parseOptionalInt(value(row, contentOrderIndex), rowIndex, "考核内容序号");
          String contentHtml = value(row, contentHtmlIndex);
          if (legacyOriginalLayout && contentOrder == null && contentHtml.isBlank()) {
            // “考评点” is the only question-level grouping field in the
            // original workbook.  Preserve it as both the requirements text
            // and the default assessment-content group.
            contentHtml = builder.assessmentPoint;
          }
          if (contentOrder == null && contentHtml.isBlank() && builder.currentContentOrder != null) {
            // Merged cells in a structured workbook expose the content number
            // and HTML only on the first element row.  Carry that group over
            // so every following element remains in the same assessment
            // content instead of becoming an orphaned flat score item.
            contentOrder = builder.currentContentOrder;
            contentHtml = builder.currentContentHtml;
          }
          if (contentOrder != null || !contentHtml.isBlank()) {
            builder.addContent(contentOrder == null ? 1 : contentOrder,
                contentHtml.isBlank() ? "<p>默认考核内容</p>" : contentHtml,
                builder.items.get(builder.items.size() - 1));
          }
        }
      }
    }
    List<ParsedQuestion> result = new ArrayList<>();
    for (PracticalBuilder builder : builders.values()) {
      // Preparation-only sheets in historical files have question rows but no
      // scoring elements.  They are intentionally ignored; the scoring sheet
      // that follows carries the authoritative question records.
      if (builder.items.isEmpty()) continue;
      result.add(builder.build());
    }
    return result;
  }

  private ParsedQuestion parseScoreRecordSheet(Sheet sheet, int defaultSequence) {
    int titleRow = -1;
    String title = null;
    BigDecimal legacyFallbackDeduction = null;
    for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 20); i++) {
      String text = rowText(sheet.getRow(i));
      Matcher matcher = SCORE_PATTERN.matcher(text);
      if (matcher.find() && (text.contains("评分") || text.contains("题") || text.contains("实操"))) {
        titleRow = i;
        title = text.trim();
        legacyFallbackDeduction = new BigDecimal(matcher.group(1));
        break;
      }
    }
    if (titleRow < 0) return null;
    int sequence = defaultSequence;
    Matcher sequenceMatcher = Pattern.compile("(?:第|序号[：:]?)(\\d+)").matcher(title);
    if (sequenceMatcher.find()) sequence = Integer.parseInt(sequenceMatcher.group(1));
    String content = firstNonBlankRowText(sheet, titleRow + 1);
    List<ParsedScoreItem> items = new ArrayList<>();
    int itemNo = 1;
    for (int i = titleRow + 1; i <= sheet.getLastRowNum(); i++) {
      Row row = sheet.getRow(i);
      if (isBlank(row)) continue;
      String rowText = rowText(row);
      if (!rowText.contains("扣分") && !rowText.contains("评分项") && !rowText.contains("评分要素")) continue;
      String itemName = value(row, 1);
      if (itemName.isBlank()) itemName = value(row, 0);
      if (itemName.isBlank() || itemName.contains("评分项")) continue;
      String criterion = value(row, 2);
      BigDecimal maxDeduction = parseFirstDecimal(row, 3).orElse(legacyFallbackDeduction);
      BigDecimal step = parseFirstDecimal(row, 4).orElse(BigDecimal.ONE);
      items.add(new ParsedScoreItem(itemNo++, itemName, criterion, maxDeduction, maxDeduction, step));
    }
    if (items.isEmpty()) {
      throw new IllegalArgumentException("评分记录表第" + (titleRow + 1) + "行未找到评分项");
    }
     return new ParsedQuestion(sequence, "practical", title, null, null, null, null,
         content, null, null, items,
         null, null, null, null, null, null, null, null, null, List.of(),
         false, true, null, null, null);
  }

  private Header findHeader(Sheet sheet, boolean theory) {
    for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 8); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;
      Header header = Header.from(row, formatter);
      if ((header.index("序号", "题号", "题目序号") >= 0 ||
              (theory && header.index("鉴定点", "考评点", "行为领域") >= 0))
          && header.index("题型", "试题类型", "类型") >= 0
          && header.index("题干", "题目", "题目内容", "试题内容") >= 0) {
        return header.withRowIndex(i);
      }
    }
    throw new IllegalArgumentException((theory ? "理论" : "实操") + "模板缺少表头（至少需要序号、题型、题干）");
  }

  private Header findPracticalTableHeader(Sheet sheet) {
    for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 12); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;
      Header header = Header.from(row, formatter);
      boolean hasSequenceAndStem = header.index("序号", "题号", "题目序号") >= 0
          && header.index("题干", "题目", "题目内容", "试题内容") >= 0;
      boolean hasOriginalScoreRecordColumns = header.index("考评点") >= 0
          && header.index("实操题目", "实操题") >= 0
          && header.index("评分要素", "评分项名称", "评分项", "要素") >= 0
          && header.index("配分", "最大分值", "评分项分值", "项目分值") >= 0
          && header.index("评分标准", "标准", "评分要求") >= 0;
      if ((hasSequenceAndStem
              && (header.index("评分要素", "评分项名称", "评分项", "要素", "评分项JSON") >= 0
                  || header.index("操作内容", "实操内容", "操作要求") >= 0))
          || hasOriginalScoreRecordColumns) {
        return header.withRowIndex(i);
      }
    }
    return null;
  }

  private void validateAnswer(String type, String answer, String fillAnswer,
      Map<String, String> options, int rowIndex) {
    if ("fill".equals(type)) {
      if (fillAnswer.isBlank() && answer.isBlank()) throw rowError(rowIndex, "填空题必须填写回答或参考答案");
      return;
    }
    if (answer.isBlank()) throw rowError(rowIndex, "选择/判断题必须填写参考答案");
    if ("judge".equals(type)) {
      String normalized = answer.trim().toUpperCase(Locale.ROOT);
      if (!(normalized.equals("T") || normalized.equals("F"))) {
        throw rowError(rowIndex, "判断题答案只能是 T 或 F");
      }
      return;
    }
    String letters = answer.toUpperCase(Locale.ROOT).replaceAll("[^A-H]", "");
    if (letters.isBlank() || ("single".equals(type) && letters.length() != 1)
        || ("multi".equals(type) && letters.length() < 2)) {
      throw rowError(rowIndex, "答案必须是有效的A-H选项");
    }
    for (char letter : letters.toCharArray()) {
      if (!options.containsKey(String.valueOf(letter))) {
        throw rowError(rowIndex, "答案选项 " + letter + " 没有对应选项内容");
      }
    }
  }

  private String normalizeTheoryType(String type) {
    if (type == null) return null;
    String value = type.trim().toLowerCase(Locale.ROOT);
    return switch (value) {
      case "single", "单选", "单选题" -> "single";
      case "multi", "multiple", "多选", "多选题" -> "multi";
      case "judge", "判断", "判断题" -> "judge";
      case "fill", "填空", "填空题" -> "fill";
      default -> null;
    };
  }

  private String optionsJson(Map<String, String> options) {
    if (options.isEmpty()) return null;
    StringBuilder builder = new StringBuilder("{");
    int index = 0;
    for (Map.Entry<String, String> entry : options.entrySet()) {
      if (index++ > 0) builder.append(',');
      builder.append('"').append(jsonEscape(entry.getKey())).append("\":\"")
          .append(jsonEscape(entry.getValue())).append('"');
    }
    return builder.append('}').toString();
  }

  private List<ParsedScoreItem> parseScoreItemsJson(String json, int rowIndex) {
    try {
      JsonNode root = objectMapper.readTree(json);
      if (!root.isArray()) throw new IllegalArgumentException("评分项必须是数组");
      List<ParsedScoreItem> result = new ArrayList<>();
      int fallbackNo = 1;
      for (JsonNode node : root) {
        int itemNo = node.path("itemNo").asInt(fallbackNo++);
        String itemName = node.path("itemName").asText("").trim();
        if (itemName.isBlank()) throw new IllegalArgumentException("评分项名称不能为空");
        BigDecimal maxScore = decimal(node, "maxScore", "maxDeduction");
        BigDecimal maxDeduction = decimal(node, "maxDeduction", "maxScore");
        BigDecimal step = decimal(node, "deductionStep", null);
        result.add(new ParsedScoreItem(itemNo, itemName, node.path("criterion").asText(null),
            maxScore, maxDeduction, step));
      }
      return result;
    } catch (Exception ex) {
      throw rowError(rowIndex, "评分项JSON格式错误: " + ex.getMessage());
    }
  }

  private BigDecimal decimal(JsonNode node, String primary, String fallback) {
    String value = node.path(primary).asText("").trim();
    if (value.isBlank() && fallback != null) value = node.path(fallback).asText("").trim();
    if (value.isBlank()) throw new IllegalArgumentException(primary + "不能为空");
    return new BigDecimal(value);
  }

  private Optional<BigDecimal> parseFirstDecimal(Row row, int index) {
    String value = value(row, index);
    if (value.isBlank()) return Optional.empty();
    Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(value);
    return matcher.find() ? Optional.of(new BigDecimal(matcher.group())) : Optional.empty();
  }

  private int parseInt(String value, int rowIndex, String name) {
    try { return new BigDecimal(value.trim()).intValueExact(); }
    catch (Exception ex) { throw rowError(rowIndex, name + "必须是整数"); }
  }

  private BigDecimal parseDecimal(String value, int rowIndex, String name) {
    try {
      BigDecimal number = new BigDecimal(value.trim());
      if (number.signum() <= 0) throw new NumberFormatException();
      return number;
    } catch (Exception ex) { throw rowError(rowIndex, name + "必须是大于0的数字"); }
  }

  private BigDecimal parseOptionalDecimal(String value, int rowIndex, String name) {
    if (value == null || value.isBlank()) return null;
    try {
      BigDecimal number = new BigDecimal(value.trim());
      if (number.signum() < 0) throw new NumberFormatException();
      return number;
    } catch (Exception ex) {
      throw rowError(rowIndex, name + "必须是大于等于0的数字");
    }
  }

  private Integer firstInt(Integer current, Integer candidate) {
    return current == null ? candidate : current;
  }

  private BigDecimal firstDecimal(BigDecimal current, BigDecimal candidate) {
    return current == null ? candidate : current;
  }

  private boolean parseBoolean(String value, boolean defaultValue) {
    if (value == null || value.isBlank()) return defaultValue;
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "1", "true", "yes", "y", "是", "配分" -> true;
      case "0", "false", "no", "n", "否", "不配分" -> false;
      default -> defaultValue;
    };
  }

  private Integer parseOptionalInt(String value, int rowIndex, String name) {
    if (value == null || value.isBlank()) return null;
    return parseInt(value, rowIndex, name);
  }

  private String value(Row row, int index) {
    if (row == null || index < 0) return "";
    Cell cell = row.getCell(index);
    return cell == null ? "" : formatter.formatCellValue(cell).trim();
  }

  private String rowText(Row row) {
    if (row == null) return "";
    StringBuilder text = new StringBuilder();
    for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
      String value = value(row, i);
      if (!value.isBlank()) text.append(value).append(' ');
    }
    return text.toString();
  }

  private String joinPath(Row row, int start, int endExclusive) {
    if (row == null) return "";
    StringBuilder result = new StringBuilder();
    for (int i = start; i < endExclusive; i++) {
      String item = value(row, i);
      if (!item.isBlank() && !item.equalsIgnoreCase("null")) {
        if (result.length() > 0) result.append("/");
        result.append(item);
      }
    }
    return result.toString();
  }

  private String joinPath(Sheet sheet, Row row, int start, int endExclusive, Map<String, String> merged) {
    if (row == null) return "";
    StringBuilder result = new StringBuilder();
    for (int i = start; i < endExclusive; i++) {
      String item = value(sheet, row, i, merged);
      if (!item.isBlank() && !item.equalsIgnoreCase("null")) {
        if (result.length() > 0) result.append("/");
        result.append(item);
      }
    }
    return result.toString();
  }

  private String value(Sheet sheet, Row row, int index, Map<String, String> merged) {
    if (row == null || index < 0) return "";
    String mergedValue = merged == null ? null : merged.get(row.getRowNum() + ":" + index);
    return mergedValue == null ? value(row, index) : mergedValue.trim();
  }

  private Map<String, String> collectMergedValues(Sheet sheet) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
      CellRangeAddress region = sheet.getMergedRegion(i);
      String value = value(sheet.getRow(region.getFirstRow()), region.getFirstColumn());
      for (int row = region.getFirstRow(); row <= region.getLastRow(); row++) {
        for (int col = region.getFirstColumn(); col <= region.getLastColumn(); col++) {
          values.put(row + ":" + col, value);
        }
      }
    }
    return values;
  }

  private String firstNonBlankRowText(Sheet sheet, int start) {
    for (int i = start; i <= sheet.getLastRowNum(); i++) {
      String text = rowText(sheet.getRow(i));
      if (!text.isBlank()) return text.trim();
    }
    return "";
  }

  private boolean isBlank(Row row) { return row == null || rowText(row).isBlank(); }

  private String jsonEscape(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\r", "\\r").replace("\n", "\\n");
  }

  private IllegalArgumentException rowError(int rowIndex, String message) {
    return new IllegalArgumentException("第" + (rowIndex + 1) + "行: " + message);
  }

  public record ParsedWorkbook(List<ParsedQuestion> questions, String selectedSheet) {
    public ParsedWorkbook {
      questions = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(questions)));
    }
    public ParsedWorkbook(List<ParsedQuestion> questions) { this(questions, null); }
  }

  private record SheetCandidate(String sheetName, List<ParsedQuestion> questions) {
    int coverage() {
      // Prefer the complete assessment-point code when the workbook provides
      // it.  Point names are only a fallback: two leaf points may legitimately
      // share a display name under different paths, and counting names alone
      // would under-report coverage and could select the wrong practical sheet.
      return (int) questions.stream()
          .map(question -> {
            String code = question.blueprintFullCode();
            return code == null || code.isBlank() ? question.blueprintPointName() : code;
          })
          .filter(value -> value != null && !value.isBlank())
          .map(value -> java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
              .replaceAll("\\s+", ""))
          .distinct().count();
    }
  }

  private record PracticalParseResult(List<ParsedQuestion> questions, String selectedSheet) {}

  public record ParsedQuestion(
      Integer sortOrder,
      String questionType,
      String stem,
      String referenceAnswer,
      String fillAnswer,
      String optionsJson,
      Integer difficulty,
      String operationContent,
      String studentPrepare,
      String equipmentPrepare,
      List<ParsedScoreItem> scoreItems,
      Integer durationMinutes,
      BigDecimal scoreDifference,
      BigDecimal totalScore,
      String scoringMethod,
      String assessmentRequirementsHtml,
      String examRoomPrepareHtml,
      String candidatePrepareHtml,
      String negativeItemsHtml,
      String questionMaterialsHtml,
      List<ParsedAssessmentContent> assessmentContents,
      boolean structuredTemplate,
      boolean legacyPracticalLayout,
      String blueprintFullCode,
      String blueprintPath,
      String blueprintPointName) {
    public ParsedQuestion(
        Integer sortOrder,
        String questionType,
        String stem,
        String referenceAnswer,
        String fillAnswer,
        String optionsJson,
        Integer difficulty,
        String operationContent,
        String studentPrepare,
        String equipmentPrepare,
        List<ParsedScoreItem> scoreItems) {
      this(sortOrder, questionType, stem, referenceAnswer, fillAnswer, optionsJson,
          difficulty, operationContent, studentPrepare, equipmentPrepare, scoreItems,
          null, null, null, null, null, null, null, null, null, List.of(), false, false,
          null, null, null);
    }

    public ParsedQuestion {
      scoreItems = scoreItems == null ? List.of() : List.copyOf(scoreItems);
      assessmentContents = assessmentContents == null ? List.of() : List.copyOf(assessmentContents);
    }
  }

  public record ParsedScoreItem(
      Integer itemNo,
      String itemName,
      String criterion,
      BigDecimal maxScore,
      BigDecimal maxDeduction,
      BigDecimal deductionStep,
      String elementHtml,
      boolean allocationEnabled) {
    public ParsedScoreItem(
        Integer itemNo,
        String itemName,
        String criterion,
        BigDecimal maxScore,
        BigDecimal maxDeduction,
        BigDecimal deductionStep) {
      this(itemNo, itemName, criterion, maxScore, maxDeduction, deductionStep, itemName, true);
    }
  }

  public record ParsedAssessmentContent(
      Integer sortOrder,
      String contentHtml,
      List<ParsedScoreItem> elements) {
    public ParsedAssessmentContent {
      elements = elements == null ? List.of() : List.copyOf(elements);
    }
  }

  private static final class PracticalBuilder {
    private final int sequence;
    private final boolean structuredTemplate;
    private final boolean legacyOriginalLayout;
    private String stem;
    private String operationContent;
    private String assessmentPoint;
    private String assessmentPointCode;
    private String assessmentPointPath;
    private String studentPrepare;
    private String equipmentPrepare;
    private Integer durationMinutes;
    private BigDecimal scoreDifference;
    private BigDecimal totalScore;
    private String scoringMethod;
    private String negativeItemsHtml;
    private String questionMaterialsHtml;
    private final List<ParsedScoreItem> items = new ArrayList<>();
    private final Map<Integer, ContentBuilder> contentBuilders = new LinkedHashMap<>();
    private Integer currentContentOrder;
    private String currentContentHtml;

    private PracticalBuilder(int sequence, boolean structuredTemplate, boolean legacyOriginalLayout) {
      this.sequence = sequence;
      this.structuredTemplate = structuredTemplate;
      this.legacyOriginalLayout = legacyOriginalLayout;
    }

    private void addContent(int sortOrder, String contentHtml, ParsedScoreItem item) {
      currentContentOrder = sortOrder;
      currentContentHtml = contentHtml;
      ContentBuilder existing = contentBuilders.get(sortOrder);
      if (existing != null && existing.contentHtml != null && !existing.contentHtml.isBlank()
          && contentHtml != null && !contentHtml.isBlank()
          && !existing.contentHtml.trim().equals(contentHtml.trim())) {
        throw new IllegalArgumentException("考核内容序号重复且内容不一致: " + sortOrder);
      }
      ContentBuilder builder = contentBuilders.computeIfAbsent(sortOrder, key -> new ContentBuilder(key, contentHtml));
      if (builder.contentHtml == null || builder.contentHtml.isBlank()) builder.contentHtml = contentHtml;
      builder.elements.add(item);
    }

    private ParsedQuestion build() {
      if (stem == null || stem.isBlank()) throw new IllegalArgumentException("实操题序号" + sequence + "题干不能为空");
      if (items.isEmpty()) throw new IllegalArgumentException("实操题序号" + sequence + "至少需要一个评分项");
      List<ParsedAssessmentContent> contents = new ArrayList<>();
      for (ContentBuilder content : contentBuilders.values()) {
        contents.add(new ParsedAssessmentContent(content.sortOrder, content.contentHtml, content.elements));
      }
      if (contents.isEmpty()) {
        contents.add(new ParsedAssessmentContent(1,
            operationContent == null || operationContent.isBlank() ? "<p>默认考核内容</p>" : operationContent,
            items));
      }
      BigDecimal computedTotal = items.stream().filter(ParsedScoreItem::allocationEnabled)
          .map(ParsedScoreItem::maxScore).filter(Objects::nonNull)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal effectiveTotal = totalScore == null ? computedTotal : totalScore;
      Integer effectiveDuration = durationMinutes;
      BigDecimal effectiveDifference = scoreDifference;
      String effectiveMethod = scoringMethod;
      if (legacyOriginalLayout) {
        // The historical score-record workbook predates the question-level
        // metadata columns.  Its score table is still unambiguous, so map it
        // into the current model with the documented compatibility values for
        // the missing metadata.  A supplemented workbook keeps its explicit
        // values because firstNonBlank/COALESCE semantics are used here.
        effectiveDuration = effectiveDuration == null ? 60 : effectiveDuration;
        effectiveDifference = effectiveDifference == null
            ? (effectiveTotal == null ? null : effectiveTotal.min(new BigDecimal("10")))
            : effectiveDifference;
        if (effectiveMethod == null || effectiveMethod.isBlank()) {
          boolean hasDeductionRule = items.stream().anyMatch(item ->
              item.maxDeduction() != null || item.deductionStep() != null);
          effectiveMethod = hasDeductionRule ? "deductive" : "additive";
        }
      }
      String requirements = operationContent == null || operationContent.isBlank()
          ? assessmentPoint : operationContent;
      String defaultContent = requirements == null || requirements.isBlank()
          ? "<p>默认考核内容</p>" : requirements;
      return new ParsedQuestion(sequence, "practical", stem, null, null, null, null,
          requirements, studentPrepare, equipmentPrepare, items, effectiveDuration,
          effectiveDifference, effectiveTotal, effectiveMethod,
          requirements, equipmentPrepare, studentPrepare, negativeItemsHtml,
          questionMaterialsHtml, contents.isEmpty()
              ? List.of(new ParsedAssessmentContent(1, defaultContent, items)) : contents,
          structuredTemplate, legacyOriginalLayout, assessmentPointCode,
          assessmentPointPath, assessmentPoint);
    }

    private static final class ContentBuilder {
      private final int sortOrder;
      private String contentHtml;
      private final List<ParsedScoreItem> elements = new ArrayList<>();
      private ContentBuilder(int sortOrder, String contentHtml) {
        this.sortOrder = sortOrder;
        this.contentHtml = contentHtml;
      }
    }
  }

  private record Header(Map<String, Integer> indices, int rowIndex) {
    private static Header from(Row row, DataFormatter formatter) {
      Map<String, Integer> map = new LinkedHashMap<>();
      if (row != null) {
        for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
          Cell cell = row.getCell(i);
          if (cell != null) map.put(normalize(formatter.formatCellValue(cell)), i);
        }
      }
      return new Header(map, -1);
    }

    private Header withRowIndex(int value) { return new Header(indices, value); }

    private int index(String... names) {
      for (String name : names) {
        Integer index = indices.get(normalize(name));
        if (index != null) return index;
      }
      return -1;
    }

    private static String normalize(String value) {
      return value == null ? "" : value.trim().replace(" ", "").replace("　", "").toLowerCase(Locale.ROOT);
    }
  }
}
