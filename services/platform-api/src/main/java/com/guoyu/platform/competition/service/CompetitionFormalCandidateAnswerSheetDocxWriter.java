package com.guoyu.platform.competition.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompetitionFormalCandidateAnswerSheetDocxWriter {

  public static final String DOCX_MEDIA_TYPE =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  public static final String ZIP_MEDIA_TYPE = "application/zip";
  public static final String ZIP_FILE_NAME = "理论考试答题卡.zip";
  public static final int MAX_EXPORT_COUNT = 200;
  private static final String TEMPLATE = "/competition/formal-answer-sheet-template.docx";
  private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

  /**
   * 按官方答题卡模板分区填充：
   * <ol>
   *   <li>左侧密封区文本框：姓名 / 单位名称 / 身份证号码（模板含正反两套，同步填写）</li>
   *   <li>正文标题：赛事名称；页脚：工种 +「理论笔答」</li>
   *   <li>得分汇总表（表0）：一~四列对应单选/多选/判断/填空，末列总分</li>
   *   <li>各大题「得分」小表与「(  分)」题头满分</li>
    *   <li>各大题下方写入题干、选项、标准答案和考生答案；填空题追加「四、填空题」</li>
   * </ol>
   */
  public byte[] write(AnswerSheetDocument document) {
    if (document == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "答题卡内容不能为空");
    }
    try (XWPFDocument docx = openTemplate();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      List<AnswerSheetDocument.Question> questions = sortedQuestions(document.questions());
      List<AnswerSheetDocument.Question> single = ofType(questions, "single");
      List<AnswerSheetDocument.Question> multi = ofType(questions, "multi");
      List<AnswerSheetDocument.Question> judge = ofType(questions, "judge");
      List<AnswerSheetDocument.Question> fill = ofType(questions, "fill");
      fillSeal(docx, document);
      fillTitle(docx, document.eventName());
      fillFooter(docx, document.workTypeName());
      fillScoreTables(docx, single, multi, judge, fill, document.totalScore());
      fillQuestionSections(docx, single, multi, judge, fill);
      docx.write(output);
      return output.toByteArray();
    } catch (IOException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成答题卡文档失败");
    }
  }

  public static String docxFileName(String candidateName, String candidateNo) {
    return uniqueDocxFileName(candidateName, candidateNo, new LinkedHashSet<>());
  }

  public static String uniqueDocxFileName(
      String candidateName, String candidateNo, Set<String> usedNames) {
    String name = sanitizeStem(candidateName);
    String candidateNumber = sanitizeStem(candidateNo);
    String stem = !name.isEmpty() ? name : (!candidateNumber.isEmpty() ? candidateNumber : "答题卡");
    String candidate = stem + ".docx";
    if (reserve(usedNames, candidate)) {
      return candidate;
    }
    if (!name.isEmpty() && !candidateNumber.isEmpty()) {
      candidate = name + "-" + candidateNumber + ".docx";
      if (reserve(usedNames, candidate)) {
        return candidate;
      }
    }
    int index = 2;
    while (true) {
      candidate = stem + "-" + index + ".docx";
      if (reserve(usedNames, candidate)) {
        return candidate;
      }
      index++;
    }
  }

  private static XWPFDocument openTemplate() throws IOException {
    InputStream input = CompetitionFormalCandidateAnswerSheetDocxWriter.class.getResourceAsStream(TEMPLATE);
    if (input == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答题卡模板不存在");
    }
    try (input) {
      return new XWPFDocument(input);
    }
  }

  private static void fillSeal(XWPFDocument document, AnswerSheetDocument sheet) {
    String name = plainText(sheet.candidateName());
    String org = plainText(sheet.orgName());
    String idCard = printableId(sheet.idCardNo());
    XmlCursor cursor = document.getDocument().newCursor();
    try {
      cursor.selectPath(
          "declare namespace w='" + W_NS + "' .//w:txbxContent");
      while (cursor.toNextSelection()) {
        XmlObject box = cursor.getObject();
        String compact = compact(textboxLabel(box));
        if ("姓名".equals(compact) && StringUtils.hasText(name)) {
          rewriteTextbox(box, "姓     名  " + name);
        } else if ("单位名称".equals(compact) && StringUtils.hasText(org)) {
          rewriteTextbox(box, "单 位 名 称  " + org);
        } else if ("身份证号码".equals(compact) && StringUtils.hasText(idCard)) {
          rewriteTextbox(box, "身 份 证 号 码  " + idCard);
        }
      }
    } finally {
      cursor.dispose();
    }
  }

  private static String textboxLabel(XmlObject box) {
    XmlCursor cursor = box.newCursor();
    try {
      cursor.selectPath("declare namespace w='" + W_NS + "' .//w:t");
      StringBuilder text = new StringBuilder();
      while (cursor.toNextSelection()) {
        String value = cursor.getTextValue();
        if (value != null) {
          text.append(value);
        }
      }
      return text.toString();
    } finally {
      cursor.dispose();
    }
  }

  private static void rewriteTextbox(XmlObject box, String text) {
    XmlCursor cursor = box.newCursor();
    try {
      cursor.selectPath("declare namespace w='" + W_NS + "' .//w:t");
      boolean written = false;
      while (cursor.toNextSelection()) {
        if (!written) {
          cursor.setTextValue(text == null ? "" : text);
          written = true;
        } else {
          cursor.setTextValue("");
        }
      }
    } finally {
      cursor.dispose();
    }
  }

  private static void fillTitle(XWPFDocument document, String eventName) {
    if (!StringUtils.hasText(eventName)) {
      return;
    }
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      for (XWPFRun run : paragraph.getRuns()) {
        String value = run.getText(0);
        if (value != null && value.contains("职业技能竞赛")) {
          run.setText(eventName.trim(), 0);
          return;
        }
      }
    }
  }

  private static void fillFooter(XWPFDocument document, String workTypeName) {
    String label = StringUtils.hasText(workTypeName)
        ? workTypeName.trim() + "理论笔答"
        : "理论笔答";
    for (XWPFFooter footer : document.getFooterList()) {
      for (XWPFParagraph paragraph : footer.getParagraphs()) {
        replaceFooterLabel(paragraph, label);
      }
    }
  }

  private static void replaceFooterLabel(XWPFParagraph paragraph, String label) {
    List<XWPFRun> runs = paragraph.getRuns();
    List<Integer> titleRuns = new ArrayList<>();
    StringBuilder combined = new StringBuilder();
    for (int index = 0; index < runs.size(); index++) {
      String value = runs.get(index).getText(0);
      if (value == null) {
        value = "";
      }
      if (titleRuns.isEmpty() && !value.contains("笔答") && !value.contains("金属")) {
        continue;
      }
      if (!titleRuns.isEmpty() && (value.contains("第") || value.contains("页"))) {
        break;
      }
      titleRuns.add(index);
      combined.append(value);
      if (combined.toString().contains("）")) {
        break;
      }
    }
    if (titleRuns.isEmpty()) {
      return;
    }
    runs.get(titleRuns.get(0)).setText(label, 0);
    for (int index = 1; index < titleRuns.size(); index++) {
      runs.get(titleRuns.get(index)).setText("", 0);
    }
  }

  private static void fillScoreTables(
      XWPFDocument document,
      List<AnswerSheetDocument.Question> single,
      List<AnswerSheetDocument.Question> multi,
      List<AnswerSheetDocument.Question> judge,
      List<AnswerSheetDocument.Question> fill,
      BigDecimal totalScore) {
    List<XWPFTable> tables = document.getTables();
    if (!tables.isEmpty()) {
      XWPFTable summary = tables.get(0);
      if (summary.getNumberOfRows() > 1) {
        List<XWPFTableCell> cells = summary.getRow(1).getTableCells();
        setScoreCell(cells, 1, sectionAwarded(single));
        setScoreCell(cells, 2, sectionAwarded(multi));
        setScoreCell(cells, 3, sectionAwarded(judge));
        setScoreCell(cells, 4, sectionAwarded(fill));
        BigDecimal total = totalScore != null ? totalScore : sum(List.of(
            sectionAwarded(single), sectionAwarded(multi), sectionAwarded(judge), sectionAwarded(fill)));
        setScoreCell(cells, cells.size() - 1, total);
      }
    }
    List<List<AnswerSheetDocument.Question>> sections = List.of(single, multi, judge);
    for (int index = 0; index < sections.size() && index + 1 < tables.size(); index++) {
      XWPFTable table = tables.get(index + 1);
      if (table.getNumberOfRows() == 0) {
        continue;
      }
      List<XWPFTableCell> cells = table.getRow(0).getTableCells();
      setScoreCell(cells, cells.size() - 1, sectionAwarded(sections.get(index)));
    }
  }

  private static void fillQuestionSections(
      XWPFDocument document,
      List<AnswerSheetDocument.Question> single,
      List<AnswerSheetDocument.Question> multi,
      List<AnswerSheetDocument.Question> judge,
      List<AnswerSheetDocument.Question> fill) {
    fillQuestionSection(document, "一、单选题", single, "每题选择一个正确答案");
    fillQuestionSection(document, "二、多选题", multi, "每题可选择多个正确答案");
    fillQuestionSection(document, "三、判断题", judge, "每题判断正误");
    if (!fill.isEmpty()) {
      appendQuestionSection(document, "四、填空题", fill, "在横线处填写答案");
    }
    appendCandidateAnswerSummary(document, single, multi, judge, fill);
  }

  private static void appendCandidateAnswerSummary(
      XWPFDocument document,
      List<AnswerSheetDocument.Question> single,
      List<AnswerSheetDocument.Question> multi,
      List<AnswerSheetDocument.Question> judge,
      List<AnswerSheetDocument.Question> fill) {
    List<AnswerSheetDocument.Question> questions = new ArrayList<>();
    questions.addAll(single);
    questions.addAll(multi);
    questions.addAll(judge);
    questions.addAll(fill);
    questions.sort(Comparator.comparing(
        AnswerSheetDocument.Question::questionOrder, Comparator.nullsLast(Integer::compareTo)));
    if (questions.isEmpty()) {
      return;
    }
    List<String> tokens = new ArrayList<>();
    for (int index = 0; index < questions.size(); index++) {
      AnswerSheetDocument.Question question = questions.get(index);
      int order = question.questionOrder() == null ? index + 1 : question.questionOrder();
      String answer = answerDisplay(question, question.candidateAnswer(), normalizeType(question.questionType()), false);
      tokens.add(order + "." + answer);
    }
    XWPFParagraph summary = document.createParagraph();
    setParagraphText(summary, "考生答案汇总：" + String.join("    ", tokens));
  }

  private static void fillQuestionSection(
      XWPFDocument document,
      String headingText,
      List<AnswerSheetDocument.Question> questions,
      String instruction) {
    XWPFParagraph heading = findHeading(document, headingText);
    if (heading == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "答题卡模板缺少题型区域：" + headingText);
    }
    setParagraphText(heading, sectionHeading(headingText, questions, instruction));

    List<XWPFParagraph> paragraphs = document.getParagraphs();
    int headingIndex = paragraphs.indexOf(heading);
    int nextHeadingIndex = findNextHeading(paragraphs, headingIndex + 1);
    int sectionEnd = nextHeadingIndex < 0 ? paragraphs.size() : nextHeadingIndex;
    List<XWPFParagraph> slots = new ArrayList<>();
    for (int index = headingIndex + 1; index < sectionEnd; index++) {
      XWPFParagraph paragraph = paragraphs.get(index);
      if (!StringUtils.hasText(paragraph.getText())) {
        slots.add(paragraph);
      }
    }

    int used = Math.min(slots.size(), questions.size());
    for (int index = 0; index < used; index++) {
      writeQuestion(slots.get(index), questions.get(index));
    }
    XWPFParagraph anchor = used > 0 ? slots.get(used - 1) : heading;
    for (int index = used; index < questions.size(); index++) {
      anchor = insertParagraphAfter(document, anchor);
      writeQuestion(anchor, questions.get(index));
    }
    for (int index = slots.size() - 1; index >= used; index--) {
      removeParagraph(document, slots.get(index));
    }
  }

  private static void appendQuestionSection(
      XWPFDocument document,
      String headingText,
      List<AnswerSheetDocument.Question> questions,
      String instruction) {
    XWPFParagraph heading = document.createParagraph();
    setParagraphText(heading, sectionHeading(headingText, questions, instruction));
    for (AnswerSheetDocument.Question question : questions) {
      XWPFParagraph paragraph = document.createParagraph();
      writeQuestion(paragraph, question);
    }
  }

  private static String sectionHeading(
      String headingText, List<AnswerSheetDocument.Question> questions, String instruction) {
    BigDecimal max = sectionMax(questions);
    String score = max == null ? "未设置" : plainNumber(max);
    return headingText + "(" + score + "分)（共" + questions.size() + "题；" + instruction + "）";
  }

  private static XWPFParagraph findHeading(XWPFDocument document, String heading) {
    for (XWPFParagraph paragraph : document.getParagraphs()) {
      String text = paragraph.getText();
      if (text != null && text.contains(heading)) {
        return paragraph;
      }
    }
    return null;
  }

  private static int findNextHeading(List<XWPFParagraph> paragraphs, int start) {
    for (int index = start; index < paragraphs.size(); index++) {
      String text = paragraphs.get(index).getText();
      if (text != null && (text.contains("二、多选题") || text.contains("三、判断题"))) {
        return index;
      }
    }
    return -1;
  }

  private static XWPFParagraph insertParagraphAfter(XWPFDocument document, XWPFParagraph anchor) {
    XmlCursor cursor = anchor.getCTP().newCursor();
    try {
      cursor.toEndToken();
      return document.insertNewParagraph(cursor);
    } finally {
      cursor.dispose();
    }
  }

  private static void removeParagraph(XWPFDocument document, XWPFParagraph paragraph) {
    int index = document.getBodyElements().indexOf(paragraph);
    if (index >= 0) {
      document.removeBodyElement(index);
    }
  }

  private static void writeQuestion(XWPFParagraph paragraph, AnswerSheetDocument.Question question) {
    String type = normalizeType(question.questionType());
    int order = question.questionOrder() == null ? 1 : question.questionOrder();
    List<String> lines = new ArrayList<>();
    String stem = plainText(question.stem());
    lines.add(order + ". " + (StringUtils.hasText(stem) ? stem : "（题干未提供）"));
    if ("judge".equals(type)) {
      lines.add("判断：□ 正确（√）　□ 错误（×）");
    } else if (!"fill".equals(type)) {
      for (AnswerSheetDocument.Option option : safeOptions(question.options())) {
        String label = StringUtils.hasText(option.label()) ? option.label().trim() : "?";
        String content = plainText(option.content());
        lines.add(label + "、" + (StringUtils.hasText(content) ? content : "（选项内容未提供）"));
      }
    }
    String correct = answerDisplay(question, question.correctAnswer(), type, true);
    String candidate = answerDisplay(question, question.candidateAnswer(), type, false);
    lines.add("正确答案：" + correct + "    考生答案：" + candidate);
    setParagraphLines(paragraph, lines);
  }

  private static String answerDisplay(
      AnswerSheetDocument.Question question, JsonNode answer, String type, boolean correct) {
    List<String> answeredBlanks = blanks(answer);
    if (!answeredBlanks.isEmpty()) {
      List<String> filled = answeredBlanks.stream()
          .map(CompetitionFormalCandidateAnswerSheetDocxWriter::plainText)
          .filter(StringUtils::hasText)
          .toList();
      return filled.isEmpty() ? (correct ? "未提供" : "未作答") : String.join("；", filled);
    }
    Set<String> selected = selectedIds(answer);
    if (selected.isEmpty()) {
      return correct ? "未提供" : "未作答";
    }
    List<String> labels = new ArrayList<>();
    for (AnswerSheetDocument.Option option : safeOptions(question.options())) {
      if (!selected.contains(option.optionId())) {
        continue;
      }
      if ("judge".equals(type)) {
        labels.add(judgmentLabel(option.content(), option.label()));
      } else {
        labels.add(StringUtils.hasText(option.label()) ? option.label().trim() : "?");
      }
    }
    if (labels.isEmpty()) {
      return correct ? "未提供" : "未作答";
    }
    return String.join("multi".equals(type) ? "、" : "", labels);
  }

  private static String judgmentLabel(String rawContent, String rawLabel) {
    String content = plainText(rawContent).trim();
    String normalized = content.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    if (List.of("正确", "对", "是", "√", "✓", "true", "t", "1").contains(normalized)) {
      return "正确（√）";
    }
    if (List.of("错误", "错", "否", "×", "✗", "false", "f", "0").contains(normalized)) {
      return "错误（×）";
    }
    return StringUtils.hasText(content) ? content : (StringUtils.hasText(rawLabel) ? rawLabel.trim() : "未识别");
  }

  private static String normalizeType(String type) {
    String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    return "multiple".equals(normalized) ? "multi" : normalized;
  }

  private static List<AnswerSheetDocument.Option> safeOptions(List<AnswerSheetDocument.Option> options) {
    return options == null ? List.of() : options.stream().filter(option -> option != null).toList();
  }

  private static void setParagraphLines(XWPFParagraph paragraph, List<String> lines) {
    List<XWPFRun> runs = paragraph.getRuns();
    for (int index = runs.size() - 1; index >= 0; index--) {
      paragraph.removeRun(index);
    }
    XWPFRun run = paragraph.createRun();
    run.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
    run.setFontSize(10);
    for (int index = 0; index < lines.size(); index++) {
      if (index > 0) {
        run.addBreak();
      }
      run.setText(lines.get(index));
    }
    paragraph.setSpacingAfter(40);
  }

  private static void setScoreCell(List<XWPFTableCell> cells, int index, BigDecimal score) {
    if (score == null || index < 0 || index >= cells.size()) {
      return;
    }
    XWPFTableCell cell = cells.get(index);
    if (cell.getParagraphs().isEmpty()) {
      return;
    }
    setParagraphText(cell.getParagraphs().get(0), plainNumber(score));
  }

  private static void setParagraphText(XWPFParagraph paragraph, String text) {
    List<XWPFRun> runs = paragraph.getRuns();
    if (runs == null || runs.isEmpty()) {
      XWPFRun run = paragraph.createRun();
      run.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
      run.setFontSize(10);
      run.setText(text == null ? "" : text);
      return;
    }
    runs.get(0).setText(text == null ? "" : text, 0);
    for (int index = runs.size() - 1; index > 0; index--) {
      paragraph.removeRun(index);
    }
  }

  private static List<AnswerSheetDocument.Question> ofType(
      List<AnswerSheetDocument.Question> questions, String type) {
    return questions.stream()
        .filter(question -> type.equals(normalizeType(question.questionType())))
        .toList();
  }

  private static BigDecimal sectionMax(List<AnswerSheetDocument.Question> questions) {
    if (questions.isEmpty()) {
      return null;
    }
    BigDecimal total = questions.stream()
        .map(AnswerSheetDocument.Question::maxScore)
        .filter(score -> score != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return questions.stream().allMatch(question -> question.maxScore() == null) ? null : total;
  }

  private static BigDecimal sectionAwarded(List<AnswerSheetDocument.Question> questions) {
    if (questions.isEmpty()) {
      return null;
    }
    return questions.stream()
        .map(question -> question.awardedScore() == null ? BigDecimal.ZERO : question.awardedScore())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal sum(List<BigDecimal> values) {
    return values.stream().filter(value -> value != null).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static List<AnswerSheetDocument.Question> sortedQuestions(
      List<AnswerSheetDocument.Question> questions) {
    List<AnswerSheetDocument.Question> sorted =
        new ArrayList<>(questions == null ? List.of() : questions);
    sorted.sort(Comparator.comparing(
        AnswerSheetDocument.Question::questionOrder, Comparator.nullsLast(Integer::compareTo)));
    return sorted;
  }

  private static Set<String> selectedIds(JsonNode answer) {
    LinkedHashSet<String> selected = new LinkedHashSet<>();
    JsonNode ids = answer == null ? null : answer.get("selectedOptionIds");
    if (ids == null || !ids.isArray()) {
      return selected;
    }
    for (JsonNode idNode : ids) {
      if (idNode == null || idNode.isNull()) {
        continue;
      }
      String optionId = idNode.asText("").trim();
      if (StringUtils.hasText(optionId)) {
        selected.add(optionId);
      }
    }
    return selected;
  }

  private static List<String> blanks(JsonNode answer) {
    JsonNode node = answer == null ? null : answer.get("blanks");
    if (node == null || !node.isArray() || node.isEmpty()) {
      return List.of();
    }
    List<String> blanks = new ArrayList<>();
    for (JsonNode blank : node) {
      blanks.add(blank == null || blank.isNull() ? "" : blank.asText(""));
    }
    boolean answered = blanks.stream().anyMatch(StringUtils::hasText);
    return answered ? blanks : List.of();
  }

  static String plainText(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String text = raw
        .replaceAll("(?i)<br\\s*/?>", "\n")
        .replaceAll("(?i)</p>", "\n")
        .replaceAll("<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&amp;", "&");
    return text.replaceAll("[\\t\\x0B\\f\\r]+", "")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  static String plainNumber(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private static String printableId(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String value = raw.trim();
    return value.matches("[0-9Xx]{15,18}") ? value : "";
  }

  private static String compact(String value) {
    return value == null ? "" : value.replace(" ", "").replace("\u3000", "");
  }

  static String sanitizeStem(String raw) {
    if (raw == null) {
      return "";
    }
    String cleaned = raw.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").trim();
    cleaned = cleaned.replaceAll("[. ]+$", "");
    if (cleaned.length() > 80) {
      cleaned = cleaned.substring(0, 80).replaceAll("[. ]+$", "");
    }
    if (cleaned.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
      cleaned = cleaned + "_";
    }
    return cleaned;
  }

  private static boolean reserve(Set<String> usedNames, String fileName) {
    return usedNames.add(fileName.toLowerCase(Locale.ROOT));
  }

  public record AnswerSheetDocument(
      String candidateName,
      String candidateNo,
      BigDecimal totalScore,
      BigDecimal fullScore,
      List<Question> questions,
      String eventName,
      String workTypeName,
      String orgName,
      String idCardNo) {

    public record Question(
        Integer questionOrder,
        String questionType,
        String stem,
        Boolean answered,
        Boolean correct,
        BigDecimal awardedScore,
        BigDecimal maxScore,
        JsonNode candidateAnswer,
        JsonNode correctAnswer,
        List<Option> options) {

      public Question(
          Integer questionOrder,
          String questionType,
          String stem,
          Boolean answered,
          Boolean correct,
          BigDecimal awardedScore,
          BigDecimal maxScore,
          JsonNode candidateAnswer,
          List<Option> options) {
        this(
            questionOrder,
            questionType,
            stem,
            answered,
            correct,
            awardedScore,
            maxScore,
            candidateAnswer,
            null,
            options);
      }
    }

    public record Option(String optionId, String label, String content) {
    }
  }
}
