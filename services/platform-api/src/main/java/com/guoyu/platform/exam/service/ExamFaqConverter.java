package com.guoyu.platform.exam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * 按题型生成 standard_question / answers / tag_name（确定性规则，不调 LLM）。
 * similar_questions / negative_questions 由 LLM 生成，不在此处处理。
 */
@Component
public class ExamFaqConverter {

  private final ObjectMapper objectMapper;

  /** answer JSON 中的选项 */
  private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

  public ExamFaqConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 将试题题干去除 HTML 标签后的纯文本。
   */
  public String stripHtml(String html) {
    if (html == null) return "";
    return Jsoup.parse(html).text().trim();
  }

  /**
   * 生成标准问（standard_question）。
   */
  public String buildStandardQuestion(ExamExamDetailEntity exam) {
    String stripped = stripHtml(exam.getContent());
    String type = exam.getType();

    if ("judge".equals(type)) {
      // 判断题：题干补问号
      String cleaned = stripped.replaceFirst("（\\s*）", "").trim();
      if (cleaned.endsWith("。")) {
        cleaned = cleaned.substring(0, cleaned.length() - 1);
      }
      return cleaned + "吗？";
    }

    // 单选/多选：已经是问句则保留，否则补"是什么？"
    if (stripped.endsWith("？") || stripped.endsWith("?")) {
      return stripped;
    }
    // 题干含 "?）" 或 "（？" 等半截问号，也是问句
    if (stripped.contains("？") || stripped.contains("?")) {
      return stripped;
    }
    // 陈述句补问号
    return stripped + "是什么？";
  }

  /**
   * 生成 answers 数组（JSON 字符串）。
   */
  public String buildAnswers(ExamExamDetailEntity exam) {
    String type = exam.getType();
    String stripped = stripHtml(exam.getContent());
    List<String> answers = new ArrayList<>();

    try {
      if ("judge".equals(type)) {
        answers.add(buildJudgeAnswer(exam, stripped));
      } else if ("multi".equals(type)) {
        answers.add(buildMultiAnswer(exam));
      } else {
        // single
        answers.add(buildSingleAnswer(exam));
      }
    } catch (Exception e) {
      answers.clear();
      answers.add(stripped);
    }

    // 拼接 resolve（解析）
    if (exam.getResolve() != null && !exam.getResolve().isBlank()) {
      answers.add("解析：" + exam.getResolve());
    }

    try {
      return objectMapper.writeValueAsString(answers);
    } catch (JsonProcessingException e) {
      return "[\"" + stripped.replace("\"", "\\\"") + "\"]";
    }
  }

  /**
   * 生成 tag_name: {题型}|{知识点路径}|{难度}
   */
  public String buildTagName(String typeName, String knowledgePath, Integer difficulty) {
    String typeLabel = switch (typeName) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      default -> typeName;
    };

    String path = (knowledgePath != null && !knowledgePath.isBlank()) ? knowledgePath : "未分类";

    String diffLabel;
    if (difficulty == null) {
      diffLabel = "未分类";
    } else {
      diffLabel = switch (difficulty) {
        case 0, 1 -> "简单";
        case 2 -> "中等";
        default -> "困难";
      };
    }

    return typeLabel + "|" + path + "|" + diffLabel;
  }

  /**
   * 生成 similar/negative 题目的 prompt 上下文供 LLM 使用。
   * 返回 Map 包含所有 prompt 变量。
   */
  public Map<String, String> buildLlmPromptContext(ExamExamDetailEntity exam, String knowledgePath) {
    String stripped = stripHtml(exam.getContent());
    List<Map<String, Object>> options = parseOptions(exam.getAnswer());

    StringBuilder correctOpts = new StringBuilder();
    StringBuilder wrongOpts = new StringBuilder();
    for (Map<String, Object> opt : options) {
      String content = String.valueOf(opt.getOrDefault("content", ""));
      Object isCorrect = opt.get("isCorrect");
      boolean correct = isCorrect instanceof Boolean b ? b : "true".equals(String.valueOf(isCorrect));
      if (correct) {
        if (!correctOpts.isEmpty()) correctOpts.append("、");
        correctOpts.append(content);
      } else {
        if (!wrongOpts.isEmpty()) wrongOpts.append("、");
        wrongOpts.append(content);
      }
    }

    String typeLabel = switch (exam.getType()) {
      case "single" -> "单选题";
      case "multi" -> "多选题";
      case "judge" -> "判断题";
      default -> exam.getType();
    };

    String diffLabel = exam.getDifficulty() != null ? switch (exam.getDifficulty()) {
      case 0, 1 -> "简单";
      case 2 -> "中等";
      default -> "困难";
    } : "未分类";

    String keyword = exam.getKeyword();
    String keyConcepts = keyword != null ? keyword.replace("|", "、") : "无";

    return Map.of(
        "typeLabel", typeLabel,
        "knowledgePath", knowledgePath != null ? knowledgePath : "未分类",
        "diffLabel", diffLabel,
        "contentStripped", stripped,
        "correctOptions", correctOpts.toString(),
        "wrongOptions", wrongOpts.toString(),
        "keyConcepts", keyConcepts
    );
  }

  // ──────────────────────── private ────────────────────────

  private String buildSingleAnswer(ExamExamDetailEntity exam) throws JsonProcessingException {
    List<Map<String, Object>> options = parseOptions(exam.getAnswer());
    for (Map<String, Object> opt : options) {
      Object isCorrect = opt.get("isCorrect");
      boolean correct = isCorrect instanceof Boolean b ? b : "true".equals(String.valueOf(isCorrect));
      if (correct) {
        return opt.get("content") + "是正确答案。";
      }
    }
    return "（无法解析正确答案）";
  }

  private String buildMultiAnswer(ExamExamDetailEntity exam) throws JsonProcessingException {
    List<Map<String, Object>> options = parseOptions(exam.getAnswer());
    StringBuilder sb = new StringBuilder("正确答案包括：");
    List<String> corrects = new ArrayList<>();
    for (Map<String, Object> opt : options) {
      Object isCorrect = opt.get("isCorrect");
      boolean correct = isCorrect instanceof Boolean b ? b : "true".equals(String.valueOf(isCorrect));
      if (correct) {
        corrects.add(String.valueOf(opt.get("content")));
      }
    }
    sb.append(String.join("、", corrects)).append("。");
    return sb.toString();
  }

  private String buildJudgeAnswer(ExamExamDetailEntity exam, String stripped) throws JsonProcessingException {
    List<Map<String, Object>> options = parseOptions(exam.getAnswer());
    for (Map<String, Object> opt : options) {
      Object isCorrect = opt.get("isCorrect");
      boolean correct = isCorrect instanceof Boolean b ? b : "true".equals(String.valueOf(isCorrect));
      String content = String.valueOf(opt.getOrDefault("content", ""));
      if (correct) {
        if ("对".equals(content) || "正确".equals(content) || "是".equals(content)) {
          String cleaned = stripped.replaceFirst("（\\s*）", "").trim();
          if (cleaned.endsWith("。")) cleaned = cleaned.substring(0, cleaned.length() - 1);
          return "是的，" + cleaned + "。";
        } else {
          return cleaned(stripped) + "是错误的。";
        }
      }
    }
    return "（无法解析正确答案）";
  }

  private String cleaned(String stripped) {
    String c = stripped.replaceFirst("（\\s*）", "").trim();
    if (c.endsWith("。")) c = c.substring(0, c.length() - 1);
    return c;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> parseOptions(String answerJson) {
    if (answerJson == null || answerJson.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(answerJson, new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }
}
