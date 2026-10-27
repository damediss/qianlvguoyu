package com.guoyu.platform.exam.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.exam.entity.ExamKnowledgeStructureEntity;
import com.guoyu.platform.exam.mapper.ExamKnowledgeStructureMapper;
import com.guoyu.platform.exam.reviewsync.support.QuestionTypes;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import com.guoyu.platform.trainingdepartment.support.FollowTaskBankQuestionConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析生成输入组装 + sourceHash（《试题解析Markdown表格化_一期实施方案》§5.5）。
 *
 * <p>一条题目 → 一个 {@link QuestionPromptInput}；输入资格不合格（缺题干/缺选项/缺答案/
 * 答案不属于选项/判断对错无法归一）返回 null，调用方按 empty/invalid_input 处理，不调 LLM。
 * 先剥 HTML 再截断（题干 800 / 选项块 600 / 客观答案 200 / 主观答案 1200）。
 *
 * <p>sourceHash 覆盖 normalizedType/stem/optionsText/correctAnswerText/knowledgeTitle，
 * 题面、答案、知识点任一变化即变化；成功 draft 的 prompt 存
 * {@code question_resolve_v2#{sourceHash}}（§5.10）。无新表、无新列。
 */
@Slf4j
@Component
public class QuestionResolveInputFactory {

  private static final int STEM_MAX = 800;
  private static final int OPTIONS_MAX = 600;
  private static final int OBJECTIVE_ANSWER_MAX = 200;
  private static final int SUBJECTIVE_ANSWER_MAX = 1200;

  private final FollowTaskBankQuestionConverter bankQuestionConverter;
  private final ExamKnowledgeStructureMapper knowledgeStructureMapper;
  private final ObjectMapper objectMapper;

  public QuestionResolveInputFactory(
      FollowTaskBankQuestionConverter bankQuestionConverter,
      ExamKnowledgeStructureMapper knowledgeStructureMapper,
      ObjectMapper objectMapper) {
    this.bankQuestionConverter = bankQuestionConverter;
    this.knowledgeStructureMapper = knowledgeStructureMapper;
    this.objectMapper = objectMapper;
  }

  /** 解析生成输入；资格不合格返回 null。 */
  public QuestionPromptInput build(ExamExamDetailEntity detail) {
    if (detail == null) {
      return null;
    }
    String normalizedType = QuestionTypes.normalize(detail.getType());
    String stem;
    String optionsText;
    String correctAnswerText;
    if (QuestionTypes.isSubjective(normalizedType)) {
      stem = stripHtml(detail.getContent());
      optionsText = "";
      correctAnswerText = truncate(resolveSubjectiveAnswer(detail.getAnswer()), SUBJECTIVE_ANSWER_MAX);
    } else {
      FollowTaskQuestionReq snap = convertObjective(detail, normalizedType);
      if (snap == null) {
        // 未知题型（非客观三型/主观 8 白名单）：能解析出合法题干+选项+答案走通用分支
        snap = tryParseUnknownLegacy(detail);
      }
      if (snap == null) {
        return null;
      }
      stem = stripHtml(snap.getStem());
      List<?> options = snap.getOptions();
      if (options == null || options.isEmpty()) {
        return null;
      }
      optionsText = truncate(buildOptionsText(options), OPTIONS_MAX);
      // 选项列表非空但拼接后为空（极端脏数据）→ invalid_input
      if (!StringUtils.hasText(optionsText)) {
        return null;
      }
      correctAnswerText = truncate(
          resolveObjectiveAnswer(normalizedType, snap.getAnswer(), options), OBJECTIVE_ANSWER_MAX);
    }
    if (!StringUtils.hasText(stem)) {
      return null;
    }
    stem = truncate(stem, STEM_MAX);
    if (!StringUtils.hasText(correctAnswerText)) {
      return null;
    }

    String knowledgeTitle = resolveKnowledgeTitle(detail.getKnowledgeId());
    String sourceHash = sourceHash(normalizedType, stem, optionsText, correctAnswerText, knowledgeTitle);
    return new QuestionPromptInput(
        normalizedType, QuestionTypes.toLabel(normalizedType), stem, optionsText,
        correctAnswerText, knowledgeTitle, sourceHash);
  }

  private static final java.util.regex.Pattern UNKNOWN_OPTION_LINE =
      java.util.regex.Pattern.compile("^\\s*([A-Z])\\s*[.、．]\\s*(.+)$");

  /**
   * 未知题型兜底解析（§5.6 通用四行分支的输入侧）：content 首行为题干，后续
   * "A. xxx" 形态行为选项，answer 为字母（支持 A,C / A、C / AC）；任一不满足返回 null。
   */
  private FollowTaskQuestionReq tryParseUnknownLegacy(ExamExamDetailEntity detail) {
    if (!StringUtils.hasText(detail.getContent()) || !StringUtils.hasText(detail.getAnswer())) {
      return null;
    }
    String[] lines = detail.getContent().split("\n");
    if (lines.length < 3 || !StringUtils.hasText(stripHtml(lines[0]))) {
      return null;
    }
    List<Object> options = new ArrayList<>();
    List<String> keys = new ArrayList<>();
    for (int i = 1; i < lines.length; i++) {
      java.util.regex.Matcher matcher = UNKNOWN_OPTION_LINE.matcher(lines[i].trim());
      if (matcher.matches()) {
        String key = matcher.group(1);
        String text = stripHtml(matcher.group(2).trim());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("text", text == null ? "" : text);
        options.add(row);
        keys.add(key);
      }
    }
    if (options.size() < 2) {
      return null;
    }
    List<String> answerKeys = new ArrayList<>();
    String rawAnswer = detail.getAnswer().trim().toUpperCase(Locale.ROOT);
    if (rawAnswer.matches("[A-Z]([,，、\\s]*[A-Z])*") || rawAnswer.matches("[A-Z]+")) {
      for (char c : rawAnswer.toCharArray()) {
        if (c >= 'A' && c <= 'Z' && keys.contains(String.valueOf(c))
            && !answerKeys.contains(String.valueOf(c))) {
          answerKeys.add(String.valueOf(c));
        }
      }
    }
    if (answerKeys.isEmpty()) {
      return null;
    }
    FollowTaskQuestionReq req = new FollowTaskQuestionReq();
    req.setStem(lines[0]);
    req.setOptions(options);
    req.setAnswer(answerKeys.size() == 1 ? answerKeys.get(0) : answerKeys);
    return req;
  }

  /**
   * 客观题快照：优先直接转换；题库历史题型（单选题/多选题/判断题等）转换器不认时，
   * 用 QuestionTypes 归一码浅拷贝重试一次（§5.2 映射表存在的原因）。
   */
  private FollowTaskQuestionReq convertObjective(ExamExamDetailEntity detail, String normalizedType) {
    try {
      return bankQuestionConverter.convert(detail, 0);
    } catch (Exception first) {
      String rawType = detail.getType();
      if (normalizedType == null || rawType == null
          || normalizedType.equals(rawType.trim())) {
        log.debug("[题目解析] 客观题快照转换失败 questionId={} err={}",
            detail.getId(), first.getMessage());
        return null;
      }
      try {
        ExamExamDetailEntity copy = new ExamExamDetailEntity();
        copy.setId(detail.getId());
        copy.setDeleted(detail.getDeleted());
        copy.setType(normalizedType);
        copy.setContent(detail.getContent());
        copy.setAnswer(detail.getAnswer());
        copy.setResolve(detail.getResolve());
        copy.setScore(detail.getScore());
        return bankQuestionConverter.convert(copy, 0);
      } catch (Exception second) {
        log.debug("[题目解析] 客观题快照转换失败（归一重试后）questionId={} err={}",
            detail.getId(), second.getMessage());
        return null;
      }
    }
  }

  /** §5.5 sourceHash：SHA-256(canonical UTF-8) 64 位小写 hex；canonical 以 \u0001 分隔五段。 */
  public static String sourceHash(
      String normalizedType, String stem, String optionsText,
      String correctAnswerText, String knowledgeTitle) {
    String canonical = String.join("\u0001",
        nullToEmpty(normalizedType), nullToEmpty(stem), nullToEmpty(optionsText),
        nullToEmpty(correctAnswerText), nullToEmpty(knowledgeTitle));
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 不可用", e);
    }
  }

  /** 题干/选项/答案共用的 HTML 剥离规则（&lt;br&gt;/&lt;/p&gt; 转行、剥标签、实体解码、压缩空白）。 */
  public static String stripHtml(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw
        .replaceAll("(?is)<br\\s*/?>", "\n")
        .replaceAll("(?is)</p>", "\n")
        .replaceAll("(?is)<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
    return StringUtils.hasText(t) ? t : null;
  }

  private static String buildOptionsText(List<?> options) {
    StringBuilder sb = new StringBuilder();
    for (Object rawOpt : options) {
      if (!(rawOpt instanceof Map<?, ?> opt)) {
        continue;
      }
      Object key = opt.get("key");
      Object text = opt.get("text");
      if (key == null) {
        continue;
      }
      // 选项文本统一再剥一次 HTML（转换器快照可能残留标签）
      String textStr = text == null ? "" : String.valueOf(text).trim();
      String stripped = stripHtml(textStr);
      sb.append(String.valueOf(key).trim()).append(". ")
          .append(stripped == null ? "" : stripped).append('\n');
    }
    return sb.toString().trim();
  }

  /** 客观题答案：单选 "A"；多选字母升序 "A、C"；判断归一 "对/错"；不属于选项 → null。 */
  private static String resolveObjectiveAnswer(
      String normalizedType, Object snapAnswer, List<?> options) {
    if (snapAnswer == null) {
      return null;
    }
    if ("judge".equals(normalizedType)) {
      String j = String.valueOf(snapAnswer).trim().toLowerCase(Locale.ROOT);
      return switch (j) {
        case "true", "1", "t", "对", "正确" -> "对";
        case "false", "0", "f", "错", "错误" -> "错";
        default -> {
          // 兜底：按选项文本语义归一（A.对 B.错 形态）
          for (Object rawOpt : options) {
            if (!(rawOpt instanceof Map<?, ?> opt)) {
              continue;
            }
            if (String.valueOf(opt.get("key")).trim().equalsIgnoreCase(j)) {
              String text = opt.get("text") == null ? "" : String.valueOf(opt.get("text")).trim();
              if (text.contains("对") || text.contains("正确")) {
                yield "对";
              }
              if (text.contains("错") || text.contains("错误")) {
                yield "错";
              }
            }
          }
          yield null;
        }
      };
    }
    List<String> validKeys = new ArrayList<>();
    for (Object rawOpt : options) {
      if (rawOpt instanceof Map<?, ?> opt && opt.get("key") != null) {
        validKeys.add(String.valueOf(opt.get("key")).trim());
      }
    }
    List<String> keys = new ArrayList<>();
    if (snapAnswer instanceof List<?> list) {
      for (Object item : list) {
        if (item != null) {
          keys.add(String.valueOf(item).trim());
        }
      }
    } else {
      String raw = String.valueOf(snapAnswer).trim();
      if (raw.length() > 1 && raw.matches("[A-Za-z]+")) {
        for (char c : raw.toUpperCase(Locale.ROOT).toCharArray()) {
          keys.add(String.valueOf(c));
        }
      } else {
        for (String piece : raw.split("[,，、\\s]+")) {
          if (!piece.isEmpty()) {
            keys.add(piece);
          }
        }
      }
    }
    if (keys.isEmpty()) {
      return null;
    }
    for (String key : keys) {
      if (!validKeys.contains(key)) {
        return null;
      }
    }
    List<String> sorted = new ArrayList<>(keys);
    sorted.sort(String::compareTo);
    // 多选与多答案未知题型按字母升序 "A、C"；单答案一律取第一个
    return (keys.size() > 1 || "multi".equals(normalizedType))
        ? String.join("、", sorted) : sorted.get(0);
  }

  /** 主观题答案：answer JSON 数组取字符串元素或 content/text/answer/value 字段，按原序 "；" 连接。 */
  private String resolveSubjectiveAnswer(String answerJson) {
    if (!StringUtils.hasText(answerJson)) {
      return null;
    }
    String raw = answerJson.trim();
    if (raw.startsWith("[") ) {
      try {
        JsonNode node = objectMapper.readTree(raw);
        if (node.isArray() && !node.isEmpty()) {
          List<String> parts = new ArrayList<>();
          for (JsonNode item : node) {
            String text = null;
            if (item.isTextual()) {
              text = item.asText(null);
            } else if (item.isObject()) {
              for (String field : new String[]{"content", "text", "answer", "value"}) {
                JsonNode fieldNode = item.get(field);
                if (fieldNode != null && fieldNode.isTextual()
                    && StringUtils.hasText(fieldNode.asText())) {
                  text = fieldNode.asText();
                  break;
                }
              }
            }
            if (StringUtils.hasText(text)) {
              // 逐元素剥 HTML 后再拼接（先拼后剥会让 </p> 转行吃掉分隔符）
              String stripped = stripHtml(text.trim());
              parts.add(stripped == null ? text.trim() : stripped);
            }
          }
          if (!parts.isEmpty()) {
            return String.join("；", parts);
          }
        }
      } catch (Exception e) {
        log.debug("[题目解析] 主观题答案 JSON 解析失败，退回原始文本 err={}", e.getMessage());
      }
    }
    return stripHtml(raw);
  }

  private String resolveKnowledgeTitle(Integer knowledgeId) {
    if (knowledgeId == null) {
      return "";
    }
    try {
      ExamKnowledgeStructureEntity knowledge = knowledgeStructureMapper.selectById(knowledgeId);
      if (knowledge != null && StringUtils.hasText(knowledge.getTitle())) {
        return knowledge.getTitle().trim();
      }
    } catch (Exception e) {
      log.debug("[题目解析] 知识点查询失败 knowledgeId={} err={}", knowledgeId, e.getMessage());
    }
    return "";
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    String t = s.trim();
    return t.length() <= max ? t : t.substring(0, max);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  /**
   * 解析生成输入（§5.5）。
   *
   * @param normalizedType    归一题型码（QuestionTypes.normalize）
   * @param typeLabel         中文题型标签
   * @param stem              剥 HTML 并截断后的题干
   * @param optionsText       "A. …\nB. …" 形态；主观题为 ""
   * @param correctAnswerText 单选 "A" / 多选 "A、C" / 判断 "对|错"
   * @param knowledgeTitle    知识点名；无则 ""
   * @param sourceHash        五段 canonical 的 SHA-256
   */
  public record QuestionPromptInput(
      String normalizedType, String typeLabel, String stem, String optionsText,
      String correctAnswerText, String knowledgeTitle, String sourceHash) {
  }
}
