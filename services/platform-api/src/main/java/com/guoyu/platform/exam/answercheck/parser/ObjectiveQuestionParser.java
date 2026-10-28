package com.guoyu.platform.exam.answercheck.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 严格解析 + 确定性预检（方案 §8）。
 *
 * <p>禁止兜底编造答案：判断占位、答案1/答案2 等无法识别的内容必须标结构异常，不得猜测。
 * 本类不写数据库、不调用模型。
 */
@Slf4j
@Component
public class ObjectiveQuestionParser {

  public static final int MIN_OPTIONS = 2;
  public static final int MAX_OPTIONS = 16;

  private static final Pattern IMG_TAG = Pattern.compile("<img\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern DATA_IMAGE = Pattern.compile("data:image", Pattern.CASE_INSENSITIVE);
  private static final Pattern AUDIO_TAG = Pattern.compile("<audio\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern VIDEO_TAG = Pattern.compile("<video\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern PLACEHOLDER_LETTER = Pattern.compile("^[A-Ha-h]$");

  private static final List<String> POSITIVE = List.of("对", "正确", "是", "t", "true", "√");
  private static final List<String> NEGATIVE = List.of("错", "错误", "否", "f", "false", "×", "x");

  private final ObjectMapper objectMapper;

  public ObjectiveQuestionParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ParseOutcome parse(ObjectiveQuestionSource source, int maxStemChars, int maxOptionChars) {
    String type = source.type();
    if (!"single".equals(type) && !"multi".equals(type) && !"judge".equals(type)) {
      return ParseOutcome.invalid("TYPE_UNSUPPORTED");
    }
    String rawContent = source.contentHtml() == null ? "" : source.contentHtml();
    if (IMG_TAG.matcher(rawContent).find() || DATA_IMAGE.matcher(rawContent).find()
        || AUDIO_TAG.matcher(rawContent).find() || VIDEO_TAG.matcher(rawContent).find()
        || StringUtils.hasText(source.audio()) || StringUtils.hasText(source.video())) {
      return ParseOutcome.invalid("MEDIA_UNSUPPORTED");
    }

    String stem = toPlainText(rawContent);
    if (!StringUtils.hasText(stem)) {
      return ParseOutcome.invalid("STEM_BLANK");
    }
    if (stem.length() > maxStemChars) {
      return ParseOutcome.invalid("TEXT_TOO_LONG");
    }

    String answerJson = source.answerJson();
    if (!StringUtils.hasText(answerJson)) {
      return ParseOutcome.invalid("ANSWER_JSON_INVALID");
    }
    JsonNode root;
    try {
      root = objectMapper.readTree(answerJson);
    } catch (Exception e) {
      return ParseOutcome.invalid("ANSWER_JSON_INVALID");
    }
    if (root == null || !root.isArray()) {
      return ParseOutcome.invalid("ANSWER_JSON_INVALID");
    }
    if (root.isEmpty()) {
      return ParseOutcome.invalid("OPTIONS_EMPTY");
    }
    if (root.size() < MIN_OPTIONS || root.size() > MAX_OPTIONS) {
      return ParseOutcome.invalid("OPTION_COUNT_INVALID");
    }

    List<RawOption> rawOptions = new ArrayList<>();
    for (int i = 0; i < root.size(); i++) {
      JsonNode node = root.get(i);
      String text = node.path("content").asText("");
      String plain = toPlainText(text).trim();
      boolean correct = isCorrectFlag(node.path("isCorrect"));
      rawOptions.add(new RawOption(i, plain, correct));
    }

    List<RawOption> effective = rawOptions;
    if ("judge".equals(type)) {
      effective = normalizeJudge(rawOptions);
      if (effective == null) {
        return ParseOutcome.invalid("JUDGE_OPTIONS_INVALID");
      }
    } else {
      for (RawOption option : rawOptions) {
        if (!StringUtils.hasText(option.text())) {
          return ParseOutcome.invalid("OPTION_TEXT_BLANK");
        }
      }
      if (hasDuplicateText(rawOptions)) {
        return ParseOutcome.invalid("OPTION_TEXT_DUPLICATE");
      }
      for (RawOption option : rawOptions) {
        if (option.text().length() > maxOptionChars) {
          return ParseOutcome.invalid("TEXT_TOO_LONG");
        }
      }
    }

    long correctCount = effective.stream().filter(RawOption::correct).count();
    if (correctCount == 0) {
      return ParseOutcome.invalid("CORRECT_OPTION_MISSING");
    }
    if ("single".equals(type) && correctCount != 1) {
      return ParseOutcome.invalid("SINGLE_CORRECT_COUNT_INVALID");
    }
    if ("multi".equals(type) && correctCount < 2) {
      return ParseOutcome.invalid("MULTI_CORRECT_COUNT_INVALID");
    }

    Map<Integer, String> judgeLabels = judgeLabelMap(effective, type);
    List<NormalizedOption> options = new ArrayList<>();
    for (RawOption option : effective) {
      String optionId = "opt_" + option.index();
      String label = judgeLabels.containsKey(option.index()) ? judgeLabels.get(option.index())
          : String.valueOf((char) ('A' + options.size()));
      options.add(new NormalizedOption(optionId, option.index(), label, option.text(),
          option.correct()));
    }

    String hash = sourceHash(type, rawContent, answerJson);
    NormalizedObjectiveQuestion question = new NormalizedObjectiveQuestion(source.detailId(), type,
        stem, source.contextPath(), List.copyOf(options), hash);
    return ParseOutcome.ok(question);
  }

  private record RawOption(int index, String text, boolean correct) {
  }

  private boolean isCorrectFlag(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return false;
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    String text = node.asText("").trim().toLowerCase(Locale.ROOT);
    return "true".equals(text) || "1".equals(text) || "yes".equals(text);
  }

  private List<RawOption> normalizeJudge(List<RawOption> raw) {
    List<RawOption> candidates = new ArrayList<>();
    for (RawOption option : raw) {
      if (!StringUtils.hasText(option.text())) {
        continue;
      }
      candidates.add(option);
    }
    // 去掉第二个有效正反项之后、文本恰好等于自身字母的 C/D 等占位项
    boolean positiveSeen = false;
    boolean negativeSeen = false;
    List<RawOption> filtered = new ArrayList<>();
    for (RawOption option : candidates) {
      String normalized = normalizeText(option.text());
      boolean positive = isPositive(normalized);
      boolean negative = isNegative(normalized);
      if (!positive && !negative && positiveSeen && negativeSeen
          && PLACEHOLDER_LETTER.matcher(option.text().trim()).matches()) {
        continue;
      }
      filtered.add(option);
      positiveSeen = positiveSeen || positive;
      negativeSeen = negativeSeen || negative;
    }
    if (filtered.size() != 2) {
      return null;
    }
    boolean firstPositive = isPositive(normalizeText(filtered.get(0).text()));
    boolean firstNegative = isNegative(normalizeText(filtered.get(0).text()));
    boolean secondPositive = isPositive(normalizeText(filtered.get(1).text()));
    boolean secondNegative = isNegative(normalizeText(filtered.get(1).text()));
    boolean exactlyOnePositive = (firstPositive && !secondPositive) || (!firstPositive && secondPositive);
    boolean exactlyOneNegative = (firstNegative && !secondNegative) || (!firstNegative && secondNegative);
    if (!exactlyOnePositive || !exactlyOneNegative || firstPositive == secondPositive) {
      return null;
    }
    if (filtered.stream().filter(RawOption::correct).count() != 1) {
      return null;
    }
    return filtered;
  }

  private Map<Integer, String> judgeLabelMap(List<RawOption> options, String type) {
    Map<Integer, String> labels = new LinkedHashMap<>();
    if (!"judge".equals(type)) {
      return labels;
    }
    for (RawOption option : options) {
      labels.put(option.index(),
          isPositive(normalizeText(option.text())) ? "T" : "F");
    }
    return labels;
  }

  private boolean isPositive(String normalized) {
    return POSITIVE.contains(normalized);
  }

  private boolean isNegative(String normalized) {
    return NEGATIVE.contains(normalized);
  }

  private String normalizeText(String text) {
    return text == null ? "" : text.replace('\u00A0', ' ').trim().toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", "");
  }

  private boolean hasDuplicateText(List<RawOption> options) {
    Map<String, Boolean> seen = new LinkedHashMap<>();
    for (RawOption option : options) {
      String key = normalizeText(option.text());
      if (seen.putIfAbsent(key, Boolean.TRUE) != null) {
        return true;
      }
    }
    return false;
  }

  /** HTML → 纯文本；先做媒体检测再 strip（调用方已检测），不静默截断。 */
  public String toPlainText(String html) {
    if (!StringUtils.hasText(html)) {
      return "";
    }
    String text = html.contains("<") ? Jsoup.parse(html).text() : html;
    return text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
  }

  /**
   * sourceHash：type/content/answer 三段长度前缀后 SHA-256；NULL 用 {@code -1:}，空串用 {@code 0:}。
   * 禁止直接字符串拼接，避免 NULL/空串或边界碰撞。
   */
  public String sourceHash(String type, String content, String answer) {
    StringBuilder sb = new StringBuilder();
    appendSegment(sb, type);
    appendSegment(sb, content);
    appendSegment(sb, answer);
    return sha256(sb.toString());
  }

  private void appendSegment(StringBuilder sb, String value) {
    if (value == null) {
      sb.append("-1:");
    } else {
      sb.append(value.getBytes(StandardCharsets.UTF_8).length).append(':').append(value);
    }
  }

  private String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 不可用", e);
    }
  }
}
