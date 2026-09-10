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
    if (file == null || contentType == null) {
      throw new IllegalArgumentException("导入文件和题目类型不能为空");
    }
    try (Workbook workbook = WorkbookFactory.create(file.toFile())) {
      if ("theory".equalsIgnoreCase(contentType)) {
        return new ParsedWorkbook(parseTheory(workbook.getSheetAt(0)));
      }
      if ("practical".equalsIgnoreCase(contentType)) {
        return new ParsedWorkbook(parsePractical(workbook));
      }
      throw new IllegalArgumentException("不支持的题目类型: " + contentType);
    }
  }

  private List<ParsedQuestion> parseTheory(Sheet sheet) {
    Header header = findHeader(sheet, true);
    List<ParsedQuestion> result = new ArrayList<>();
    for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlank(row)) {
        continue;
      }
      String sequenceText = value(row, header.index("序号", "题号", "题目序号"));
      if (sequenceText.isBlank()) {
        throw rowError(rowIndex, "序号不能为空");
      }
      int sequence = parseInt(sequenceText, rowIndex, "序号");
      String type = normalizeTheoryType(value(row, header.index("题型", "试题类型", "类型")));
      if (type == null) {
        throw rowError(rowIndex, "题型必须是单选、多选、判断或填空");
      }
      String stem = value(row, header.index("题干", "题目", "题目内容", "试题内容"));
      if (stem.isBlank()) {
        throw rowError(rowIndex, "题干不能为空");
      }
      String referenceAnswer = value(row, header.index("参考答案", "答案", "正确答案"));
      String fillAnswer = value(row, header.index("回答", "填空答案", "填空回答"));
      Map<String, String> options = new LinkedHashMap<>();
      for (String option : OPTION_KEYS) {
        String text = value(row, header.index("选项" + option, option, "选项 " + option));
        if (!text.isBlank()) {
          options.put(option, text);
        }
      }
      validateAnswer(type, referenceAnswer, fillAnswer, options, rowIndex);
      Integer difficulty = parseOptionalInt(value(row, header.index("难度", "难易度")), rowIndex, "难度");
      result.add(new ParsedQuestion(
          sequence, type, stem, referenceAnswer.isBlank() ? null : referenceAnswer,
          fillAnswer.isBlank() ? null : fillAnswer, optionsJson(options), difficulty,
          null, null, null, List.of()));
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("理论模板没有可导入的题目");
    }
    return result;
  }

  private List<ParsedQuestion> parsePractical(Workbook workbook) {
    for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
      Sheet sheet = workbook.getSheetAt(sheetIndex);
      Header header = findPracticalTableHeader(sheet);
      if (header != null) {
        return parsePracticalTable(sheet, header);
      }
    }

    List<ParsedQuestion> result = new ArrayList<>();
    for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
      ParsedQuestion question = parseScoreRecordSheet(workbook.getSheetAt(sheetIndex), result.size() + 1);
      if (question != null) {
        result.add(question);
      }
    }
    if (result.isEmpty()) {
      throw new IllegalArgumentException("实操模板没有可导入的题目或评分项");
    }
    return result;
  }

  private List<ParsedQuestion> parsePracticalTable(Sheet sheet, Header header) {
    Map<Integer, PracticalBuilder> builders = new LinkedHashMap<>();
    for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (isBlank(row)) continue;
      int sequence = parseInt(value(row, header.index("序号", "题号", "题目序号")), rowIndex, "序号");
      PracticalBuilder builder = builders.computeIfAbsent(sequence, PracticalBuilder::new);
      String stem = value(row, header.index("题干", "题目", "题目内容", "试题内容"));
      if (!stem.isBlank()) builder.stem = stem;
      String operation = value(row, header.index("操作内容", "实操内容", "操作要求"));
      if (!operation.isBlank()) builder.operationContent = operation;
      String student = value(row, header.index("考生准备", "学生准备"));
      if (!student.isBlank()) builder.studentPrepare = student;
      String equipment = value(row, header.index("设备准备", "设备器材", "设备准备清单"));
      if (!equipment.isBlank()) builder.equipmentPrepare = equipment;
      String itemsJson = value(row, header.index("评分项JSON", "评分项", "评分标准JSON"));
      if (!itemsJson.isBlank() && itemsJson.trim().startsWith("[")) {
        builder.items.addAll(parseScoreItemsJson(itemsJson, rowIndex));
      } else {
        String itemName = value(row, header.index("评分要素", "评分项名称", "评分项", "要素"));
        if (!itemName.isBlank()) {
          BigDecimal maxScore = parseDecimal(value(row, header.index("最大分值", "评分项分值", "项目分值", "最大扣分")), rowIndex, "最大分值");
          BigDecimal maxDeduction = parseDecimal(value(row, header.index("最大扣分", "扣分上限")), rowIndex, "最大扣分");
          BigDecimal step = parseDecimal(value(row, header.index("扣分步长", "步长")), rowIndex, "扣分步长");
          String criterion = value(row, header.index("评分标准", "标准", "评分要求"));
          Integer parsedItemNo = parseOptionalInt(value(row, header.index("评分项序号", "项目序号", "项号")), rowIndex, "评分项序号");
          int itemNo = parsedItemNo == null ? builder.items.size() + 1 : parsedItemNo;
          builder.items.add(new ParsedScoreItem(itemNo, itemName, criterion, maxScore, maxDeduction, step));
        }
      }
    }
    List<ParsedQuestion> result = new ArrayList<>();
    for (PracticalBuilder builder : builders.values()) {
      result.add(builder.build());
    }
    if (result.isEmpty()) throw new IllegalArgumentException("实操模板没有可导入的题目");
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
         content, null, null, items);
  }

  private Header findHeader(Sheet sheet, boolean theory) {
    for (int i = 0; i <= Math.min(sheet.getLastRowNum(), 8); i++) {
      Row row = sheet.getRow(i);
      if (row == null) continue;
      Header header = Header.from(row, formatter);
      if (header.index("序号", "题号", "题目序号") >= 0
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
      if (header.index("序号", "题号", "题目序号") >= 0
          && header.index("题干", "题目", "题目内容", "试题内容") >= 0
          && (header.index("评分要素", "评分项名称", "评分项", "要素", "评分项JSON") >= 0
              || header.index("操作内容", "实操内容", "操作要求") >= 0)) {
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

  public record ParsedWorkbook(List<ParsedQuestion> questions) {
    public ParsedWorkbook {
      questions = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(questions)));
    }
  }

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
      List<ParsedScoreItem> scoreItems) {
    public ParsedQuestion {
      scoreItems = scoreItems == null ? List.of() : List.copyOf(scoreItems);
    }
  }

  public record ParsedScoreItem(
      Integer itemNo,
      String itemName,
      String criterion,
      BigDecimal maxScore,
      BigDecimal maxDeduction,
      BigDecimal deductionStep) {
  }

  private static final class PracticalBuilder {
    private final int sequence;
    private String stem;
    private String operationContent;
    private String studentPrepare;
    private String equipmentPrepare;
    private final List<ParsedScoreItem> items = new ArrayList<>();

    private PracticalBuilder(int sequence) { this.sequence = sequence; }

    private ParsedQuestion build() {
      if (stem == null || stem.isBlank()) throw new IllegalArgumentException("实操题序号" + sequence + "题干不能为空");
      if (items.isEmpty()) throw new IllegalArgumentException("实操题序号" + sequence + "至少需要一个评分项");
      return new ParsedQuestion(sequence, "practical", stem, null, null, null, null,
          operationContent, studentPrepare, equipmentPrepare, items);
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
