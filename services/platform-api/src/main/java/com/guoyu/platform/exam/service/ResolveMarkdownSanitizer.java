package com.guoyu.platform.exam.service;

import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.exam.service.QuestionResolveInputFactory.QuestionPromptInput;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析生成质量门（《试题解析Markdown表格化_一期实施方案》§5.7 S1~S14）。
 *
 * <p>LLM 输出 → 清洗（剥围栏/表格校验与超限整表丢弃/markdown 标记/长度按完整行截断）
 * → 硬校验（必需小节顺序、答案一致性、禁止内容、内容下限）。任何拒绝都返回
 * {@code rejectedReason != null} 且 text=null，由编排方写 rejected draft + 冷却；
 * 表格超限只整表丢弃（{@code tableDropped=true}），不算失败。
 *
 * <p>S13 安全/法规白名单在调用 LLM 前由编排方用 {@link #isOfficialOnlyKnowledge(String)}
 * 判定（命中返回 empty/official_only，不写 rejected、不进冷却）。
 */
public final class ResolveMarkdownSanitizer {

  private ResolveMarkdownSanitizer() {
  }

  /** 清洗结果；rejectedReason != null 时 text=null；tableDropped 表示有表格被整表丢弃。 */
  public record SanitizeResult(String text, String rejectedReason, boolean tableDropped) {
    static SanitizeResult ok(String text, boolean tableDropped) {
      return new SanitizeResult(text, null, tableDropped);
    }

    static SanitizeResult reject(String reason) {
      return new SanitizeResult(null, reason, false);
    }
  }

  /** S13 白名单（默认 安全规程,法律法规）：知识点名命中即不调用 LLM。 */
  private static final String[] OFFICIAL_ONLY_KEYWORDS = {"安全规程", "法律法规"};

  private static final Pattern TABLE_CELL_SPLIT = Pattern.compile("\\|");
  private static final Pattern NUMBER_TOKEN =
      Pattern.compile("\\d+(?:\\.\\d+)?%?");
  private static final Pattern HTML_TAG = Pattern.compile("<\\s*/?\\s*[a-zA-Z][^>]*>");
  private static final Pattern LIST_MARKER = Pattern.compile("^(#{1,6}|[*+-])\\s+");

  private static final String[] SECTION_OBJECTIVE = {"【考点】", "【正确】", "【干扰】", "【记法】"};

  public static boolean isOfficialOnlyKnowledge(String knowledgeTitle) {
    return isOfficialOnlyKnowledge(knowledgeTitle, null);
  }

  /**
   * S13 白名单判定：keywords 为空时用默认常量；sys_dict
   * {@code question_resolve_official_only_keywords}（逗号分隔，§5.7）可按环境扩展。
   */
  public static boolean isOfficialOnlyKnowledge(String knowledgeTitle, java.util.List<String> keywords) {
    if (knowledgeTitle == null || knowledgeTitle.isBlank()) {
      return false;
    }
    java.util.List<String> effective = (keywords == null || keywords.isEmpty())
        ? java.util.List.of(OFFICIAL_ONLY_KEYWORDS)
        : keywords;
    for (String keyword : effective) {
      if (keyword != null && !keyword.isBlank() && knowledgeTitle.contains(keyword.trim())) {
        return true;
      }
    }
    return false;
  }

  /** 完整清洗 + 硬校验管线；input 提供题型（小节集合）、题库答案（S9）、题干与选项（S10/S12）。 */
  public static SanitizeResult sanitize(String raw, QuestionPromptInput input) {
    if (raw == null || raw.isBlank()) {
      return SanitizeResult.reject("empty_output");
    }
    String text = raw.trim();

    // S1 首尾代码围栏剥除
    text = stripCodeFences(text);

    // S14 思考链/JSON 外壳/prompt 复述
    SanitizeResult shellCheck = rejectAbnormalShells(text);
    if (shellCheck != null) {
      return shellCheck;
    }

    // S11 禁止内容
    String forbidden = forbiddenReason(text);
    if (forbidden != null) {
      return SanitizeResult.reject(forbidden);
    }

    // S2~S5 + S10 表格识别、结构与数字白名单；超限/残行/编造数字 → 整表丢弃
    TableExtraction tables = extractTables(text);
    boolean tableDropped = dropIllegalTables(tables, input);
    // 丢弃后按行重建文本，保证后续小节/截断基于实际内容
    text = String.join("\n", tables.lines);

    // S6 其他 markdown 标记（仅 "# "/"* "/"- " 带空格形态，不误伤负数）
    text = stripListMarkers(text);

    // S8 必需小节与顺序
    List<Section> sections = parseSections(text);
    String sectionError = checkSections(sections, input.normalizedType());
    if (sectionError != null) {
      return SanitizeResult.reject(sectionError);
    }

    // S9 答案硬结构与题库一致
    String answerError = checkAnswerPrefix(sections, input);
    if (answerError != null) {
      return SanitizeResult.reject(answerError);
    }

    // S7 纯文本超 800 字按完整行截断（不截表格），截后重验小节
    String truncated = truncatePlainText(text, tables);
    if (truncated == null) {
      return SanitizeResult.reject("exceeds_length");
    }
    if (!truncated.equals(text)) {
      text = truncated;
      sections = parseSections(text);
      sectionError = checkSections(sections, input.normalizedType());
      if (sectionError != null) {
        return SanitizeResult.reject(sectionError);
      }
      answerError = checkAnswerPrefix(sections, input);
      if (answerError != null) {
        return SanitizeResult.reject(answerError);
      }
    }

    // S12 内容下限与"等同于题干"拒绝
    String floorError = checkContentFloor(sections, input);
    if (floorError != null) {
      return SanitizeResult.reject(floorError);
    }

    return SanitizeResult.ok(text, tableDropped);
  }

  // ──────────────────────── S1 / S14 ────────────────────────

  /** S1：剥除首尾 ``` / ~~~ 围栏（含语言标注行），至多两轮。 */
  static String stripCodeFences(String text) {
    String t = text;
    for (int i = 0; i < 2; i++) {
      String next = stripOneFence(t);
      if (next.equals(t)) {
        break;
      }
      t = next;
    }
    return t;
  }

  private static String stripOneFence(String text) {
    String t = text.trim();
    if (t.startsWith("```") || t.startsWith("~~~")) {
      int newline = t.indexOf('\n');
      if (newline > 0) {
        t = t.substring(newline + 1).trim();
      }
    }
    if (t.endsWith("```")) {
      int idx = t.lastIndexOf("```");
      if (idx > 0 && t.charAt(idx - 1) == '\n') {
        t = t.substring(0, idx).trim();
      }
    } else if (t.endsWith("~~~")) {
      int idx = t.lastIndexOf("~~~");
      if (idx > 0 && t.charAt(idx - 1) == '\n') {
        t = t.substring(0, idx).trim();
      }
    }
    return t;
  }

  /** S14：&lt;think&gt; 标签、JSON 外壳、明显 prompt 复述 → 拒绝。 */
  private static SanitizeResult rejectAbnormalShells(String text) {
    String lower = text.toLowerCase();
    if (lower.contains("<think")) {
      return SanitizeResult.reject("think_tag");
    }
    String t = text.trim();
    boolean jsonShell = (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    if (jsonShell) {
      return SanitizeResult.reject("json_shell");
    }
    if (text.contains("你是职业技能等级认定考试的辅导老师")
        || text.contains("每行以【】小节名开头")) {
      return SanitizeResult.reject("prompt_echo");
    }
    return null;
  }

  // ──────────────────────── S11 ────────────────────────

  /** S11 附加：模型名/内部表名字段名泄露一律拒绝（大小写不敏感）。 */
  private static final String[] INTERNAL_TERMS = {
      "deepseek", "glm", "openai", "max_tokens", "sys_dict",
      "exam_exam_detail", "ai_generation_draft", "question_resolve"};

  /** S11：保过/必过/稳过、学员作答表述、HTML、链接、模型/内部术语 → 拒绝。 */
  static String forbiddenReason(String text) {
    if (text.contains("保过") || text.contains("必过") || text.contains("稳过")) {
      return "forbidden_promise";
    }
    if (text.contains("你选了") || text.contains("你答了")) {
      return "forbidden_student_answer";
    }
    if (HTML_TAG.matcher(text).find()) {
      return "forbidden_html";
    }
    String lower = text.toLowerCase();
    if (lower.contains("http://") || lower.contains("https://")) {
      return "forbidden_link";
    }
    for (String term : INTERNAL_TERMS) {
      if (lower.contains(term)) {
        return "forbidden_internal_term";
      }
    }
    return null;
  }

  // ──────────────────────── S2~S5 / S10 ────────────────────────

  /** 表格块：连续 ≥3 行均以 | 开头且第 2 行仅含 | - : 空白。 */
  static final class TableExtraction {
    final List<String> lines;
    /** 每个表格块：[起始行, 结束行)（行下标），升序不重叠。 */
    final List<int[]> blocks = new ArrayList<>();

    TableExtraction(List<String> lines) {
      this.lines = lines;
    }
  }

  static TableExtraction extractTables(String text) {
    String[] rawLines = text.split("\n", -1);
    List<String> lines = new ArrayList<>(List.of(rawLines));
    TableExtraction extraction = new TableExtraction(lines);
    int i = 0;
    while (i + 2 < lines.size()) {
      if (isTableStart(lines, i)) {
        int end = i + 3;
        while (end < lines.size() && lines.get(end).trim().startsWith("|")) {
          end++;
        }
        extraction.blocks.add(new int[]{i, end});
        i = end;
      } else {
        i++;
      }
    }
    return extraction;
  }

  private static boolean isTableStart(List<String> lines, int start) {
    for (int k = 0; k < 3; k++) {
      String line = lines.get(start + k).trim();
      if (!line.startsWith("|")) {
        return false;
      }
      if (k == 1 && !line.replace("|", "").replace("-", "")
          .replace(":", "").replace(" ", "").isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /** S3/S4/S10：列>3、数据行>4、单元格>12 字、残行、编造数字 → 整表丢弃；返回是否有表被丢。 */
  static boolean dropIllegalTables(TableExtraction extraction, QuestionPromptInput input) {
    Set<String> whitelist = numberWhitelist(input);
    boolean droppedAny = false;
    for (int b = extraction.blocks.size() - 1; b >= 0; b--) {
      int[] range = extraction.blocks.get(b);
      if (!tableLegal(extraction.lines.subList(range[0], range[1]), whitelist)) {
        extraction.lines.subList(range[0], range[1]).clear();
        extraction.blocks.remove(b);
        droppedAny = true;
      }
    }
    return droppedAny;
  }

  private static boolean tableLegal(List<String> blockLines, Set<String> whitelist) {
    int headerCols = columnCount(blockLines.get(0));
    if (headerCols < 2 || headerCols > 3) {
      return false;
    }
    int dataRows = blockLines.size() - 2;
    if (dataRows < 1 || dataRows > 4) {
      return false;
    }
    for (String line : blockLines) {
      if (columnCount(line) != headerCols) {
        return false; // S4 残行
      }
      for (String cell : TABLE_CELL_SPLIT.split(line.trim())) {
        String cellText = cell.trim();
        if (cellText.isEmpty()) {
          continue;
        }
        if (cellText.length() > 12) {
          return false; // S3 单元格超 12 字
        }
        if (!cellNumbersWhitelisted(cellText, whitelist)) {
          return false; // S10 编造数字
        }
      }
    }
    return true;
  }

  private static int columnCount(String line) {
    String t = line.trim();
    int count = 0;
    for (String cell : TABLE_CELL_SPLIT.split(t)) {
      if (!cell.trim().isEmpty()) {
        count++;
      }
    }
    return count;
  }

  /** 题干/选项/答案中的数字集合（含小数、百分数）；单个 0~9 不强制校验。 */
  static Set<String> numberWhitelist(QuestionPromptInput input) {
    Set<String> whitelist = new LinkedHashSet<>();
    String source = nullToEmpty(input.stem()) + "\n" + nullToEmpty(input.optionsText())
        + "\n" + nullToEmpty(input.correctAnswerText());
    Matcher matcher = NUMBER_TOKEN.matcher(source);
    while (matcher.find()) {
      whitelist.add(matcher.group());
    }
    return whitelist;
  }

  private static boolean cellNumbersWhitelisted(String cellText, Set<String> whitelist) {
    Matcher matcher = NUMBER_TOKEN.matcher(cellText);
    while (matcher.find()) {
      String token = matcher.group();
      // 单个 0~9 序号不纳入校验，避免误伤
      if (token.length() == 1 && Character.isDigit(token.charAt(0))) {
        continue;
      }
      if (!whitelist.contains(token)) {
        return false;
      }
    }
    return true;
  }

  // ──────────────────────── S6 ────────────────────────

  /** S6：仅剥 "# "/"* "/"- " 等带空格行首标记；"-5" 这类负数不受影响。 */
  static String stripListMarkers(String text) {
    String[] lines = text.split("\n", -1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (!line.trim().startsWith("|")) {
        line = LIST_MARKER.matcher(line).replaceFirst("");
      }
      sb.append(line);
      if (i + 1 < lines.length) {
        sb.append('\n');
      }
    }
    return sb.toString();
  }

  // ──────────────────────── S8 / S9 ────────────────────────

  /** 小节：行首【名称】；content 为该行余文 + 后续非小节行。 */
  record Section(String marker, String content, int lineIndex) {
  }

  static List<Section> parseSections(String text) {
    String[] lines = text.split("\n", -1);
    List<Section> sections = new ArrayList<>();
    Section current = null;
    StringBuilder content = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String marker = sectionMarkerOf(line);
      if (marker != null) {
        if (current != null) {
          sections.add(new Section(current.marker(), content.toString().trim(), current.lineIndex()));
        }
        current = new Section(marker, "", i);
        content = new StringBuilder(line.substring(marker.length()).trim());
      } else if (current != null) {
        if (content.length() > 0) {
          content.append('\n');
        }
        content.append(line);
      }
    }
    if (current != null) {
      sections.add(new Section(current.marker(), content.toString().trim(), current.lineIndex()));
    }
    return sections;
  }

  private static String sectionMarkerOf(String line) {
    String t = line.trim();
    for (String marker : SECTION_OBJECTIVE) {
      if (t.startsWith(marker)) {
        return marker;
      }
    }
    return null;
  }

  /** S8：必需小节集合与顺序；主观省【干扰】。 */
  static String checkSections(List<Section> sections, String normalizedType) {
    String[] required = QuestionTypes.isSubjective(normalizedType)
        ? new String[]{"【考点】", "【正确】", "【记法】"}
        : SECTION_OBJECTIVE;
    int cursor = -1;
    for (String marker : required) {
      int found = -1;
      for (int i = 0; i < sections.size(); i++) {
        if (sections.get(i).marker().equals(marker) && sections.get(i).lineIndex() > cursor) {
          found = i;
          break;
        }
      }
      if (found < 0) {
        return "missing_section_" + marker.replace("【", "").replace("】", "");
      }
      cursor = sections.get(found).lineIndex();
    }
    return null;
  }

  /** S9：答案硬结构与题库完全一致。 */
  static String checkAnswerPrefix(List<Section> sections, QuestionPromptInput input) {
    Section correct = null;
    for (Section section : sections) {
      if (section.marker().equals("【正确】")) {
        correct = section;
        break;
      }
    }
    if (correct == null) {
      return "missing_section_正确";
    }
    String content = correct.content().trim();
    String type = input.normalizedType();
    if (QuestionTypes.isSubjective(type)) {
      if (!content.startsWith("答案要点：")) {
        return "answer_prefix_mismatch";
      }
      return null;
    }
    String expectedPrefix = "答案：" + nullToEmpty(input.correctAnswerText()) + "。";
    if (!content.startsWith(expectedPrefix)) {
      return "answer_prefix_mismatch";
    }
    return null;
  }

  // ──────────────────────── S7 ────────────────────────

  /**
   * S7：纯文本（非表格行）>800 字时，从尾部按完整行截断到 ≤800；只截小节内容行，
   * 不截小节标题行与表格行。单行本身超限无法按行截断时返回 null（由调用方拒绝）。
   */
  static String truncatePlainText(String text, TableExtraction tables) {
    int plain = plainLength(List.of(text.split("\n", -1)), tables);
    if (plain <= 800) {
      return text;
    }
    List<String> kept = new ArrayList<>(List.of(text.split("\n", -1)));
    while (plainLength(kept, tables) > 800) {
      int lastIndex = kept.size() - 1;
      String last = kept.get(lastIndex);
      if (inTableBlock(tables, lastIndex) || sectionMarkerOf(last) != null) {
        // 单行内容超预算且无法截断（小节标题或表格行）→ 交给调用方拒绝
        return null;
      }
      kept.remove(lastIndex);
    }
    return String.join("\n", kept);
  }

  private static int plainLength(List<String> lines, TableExtraction tables) {
    int total = 0;
    for (int i = 0; i < lines.size(); i++) {
      if (inTableBlock(tables, i)) {
        continue;
      }
      total += lines.get(i).replaceAll("\\s", "").length();
    }
    return total;
  }

  private static boolean inTableBlock(TableExtraction tables, int lineIndex) {
    for (int[] range : tables.blocks) {
      if (lineIndex >= range[0] && lineIndex < range[1]) {
        return true;
      }
    }
    return false;
  }

  // ──────────────────────── S12 ────────────────────────

  /** S12：【正确】+【干扰】合并（主观只看【正确】）去空白后 <30 字，或与题干完全相同 → 拒绝。 */
  static String checkContentFloor(List<Section> sections, QuestionPromptInput input) {
    StringBuilder body = new StringBuilder();
    for (Section section : sections) {
      boolean count = section.marker().equals("【正确】")
          || (!QuestionTypes.isSubjective(input.normalizedType()) && section.marker().equals("【干扰】"));
      if (count) {
        body.append(section.content());
      }
    }
    String plain = body.toString().replaceAll("\\s", "");
    if (plain.length() < 30) {
      return "content_too_short";
    }
    String fullNoWs = nullToEmpty(input.stem()).replaceAll("\\s", "");
    String textNoWs = sections.stream()
        .map(s -> s.marker().replace("【", "").replace("】", "") + s.content())
        .reduce(new StringBuilder(), StringBuilder::append, StringBuilder::append)
        .toString().replaceAll("\\s", "");
    if (!fullNoWs.isEmpty() && textNoWs.equals(fullNoWs)) {
      return "same_as_stem";
    }
    return null;
  }

  // ──────────────────────── helpers ────────────────────────

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
