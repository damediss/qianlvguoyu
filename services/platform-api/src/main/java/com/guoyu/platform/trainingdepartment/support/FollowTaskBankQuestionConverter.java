package com.guoyu.platform.trainingdepartment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamExamDetailEntity;
import com.guoyu.platform.trainingdepartment.dto.FollowTaskQuestionReq;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 题库 exam_exam_detail → 跟班作业题目快照（B 档）。
 */
@Component
public class FollowTaskBankQuestionConverter {

  private final ObjectMapper objectMapper;

  public FollowTaskBankQuestionConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public static String normalizeType(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "single", "danxuan", "单选" -> "single";
      case "multi", "duoxuan", "多选" -> "multi";
      case "judge", "pandan", "判断" -> "judge";
      default -> t;
    };
  }

  public static boolean isObjective(String raw) {
    String n = normalizeType(raw);
    return "single".equals(n) || "multi".equals(n) || "judge".equals(n);
  }

  public FollowTaskQuestionReq convert(ExamExamDetailEntity detail, int sortNumber) {
    String qType = normalizeType(detail.getType());
    if (!isObjective(detail.getType())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "题库题 #" + detail.getId() + " 非客观题，无法布置到跟班作业");
    }

    String[] parts = splitStemAndOptions(detail.getContent());
    String stem = parts[0] != null ? parts[0].trim() : "";
    if (!StringUtils.hasText(stem)) {
      stem = "（题库题 #" + detail.getId() + "）";
    }

    List<Map<String, Object>> options = new ArrayList<>();
    List<String> correctKeys = new ArrayList<>();

    List<Map<String, Object>> answerOpts = parseAnswerOptionsJson(detail.getAnswer());
    if (!answerOpts.isEmpty()) {
      // answer JSON 含选项内容 + isCorrect；content 行可能也有 A./B. 标签
      for (int i = 0; i < answerOpts.size(); i++) {
        Map<String, Object> opt = answerOpts.get(i);
        String key = letterKey(i);
        String text = optionContentText(opt.get("content"));
        // 若 content 分行带有 A.xxx，优先用标签
        if (parts.length > i + 1) {
          String line = parts[i + 1].trim();
          ParsedOption po = parseOptionLine(line, key);
          key = po.key();
          if (!StringUtils.hasText(text)) {
            text = po.text();
          }
        }
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("key", key);
        row.put("text", text);
        options.add(row);
        if (isOptionCorrect(opt.get("isCorrect"))) {
          correctKeys.add(key);
        }
      }
    } else {
      // 旧格式：content 拆选项 + answer 字母
      int optCount = Math.max(0, parts.length - 1);
      if ("judge".equals(qType) && optCount == 0) {
        options.add(Map.of("key", "true", "text", "正确"));
        options.add(Map.of("key", "false", "text", "错误"));
      } else {
        for (int i = 0; i < optCount; i++) {
          ParsedOption po = parseOptionLine(parts[i + 1], letterKey(i));
          Map<String, Object> row = new LinkedHashMap<>();
          row.put("key", po.key());
          row.put("text", po.text());
          options.add(row);
        }
      }
      correctKeys.addAll(parseChoiceAnswerKeys(detail.getAnswer(), qType, options));
    }

    if ("judge".equals(qType)) {
      normalizeJudgeOptions(options, correctKeys);
    }

    if (options.isEmpty() && !"judge".equals(qType)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "题库题 #" + detail.getId() + " 缺少选项，无法布置");
    }
    if (correctKeys.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST,
          "题库题 #" + detail.getId() + " 缺少标准答案，无法布置");
    }

    FollowTaskQuestionReq req = new FollowTaskQuestionReq();
    req.setSortNumber(sortNumber);
    req.setQuestionType(qType);
    req.setStem(stem);
    req.setOptions(new ArrayList<>(options));
    if ("multi".equals(qType)) {
      req.setAnswer(correctKeys);
    } else {
      req.setAnswer(correctKeys.get(0));
    }
    req.setScore(detail.getScore() != null ? detail.getScore() : BigDecimal.ONE);
    req.setAnalysis(StringUtils.hasText(detail.getResolve()) ? detail.getResolve().trim() : null);
    req.setSourceExamDetailId(detail.getId() == null ? null : detail.getId().longValue());
    return req;
  }

  /**
   * 题库常把判断题做成 A.对 B.错 C.C D.D；快照只保留两个有效选项。
   */
  private static void normalizeJudgeOptions(
      List<Map<String, Object>> options, List<String> correctKeys) {
    List<Map<String, Object>> kept = new ArrayList<>();
    for (Map<String, Object> opt : options) {
      String text = optionContentText(opt.get("text"));
      String key = opt.get("key") == null ? "" : String.valueOf(opt.get("key")).trim();
      if (!StringUtils.hasText(text)) {
        continue;
      }
      // 纯字母占位（C / D / text==key）丢掉
      if (text.matches("(?i)^[A-D]$")) {
        continue;
      }
      if (StringUtils.hasText(key) && text.equalsIgnoreCase(key) && key.matches("(?i)^[A-D]$")) {
        continue;
      }
      kept.add(opt);
      if (kept.size() >= 2) {
        break;
      }
    }
    if (kept.isEmpty()) {
      kept.add(Map.of("key", "true", "text", "正确"));
      kept.add(Map.of("key", "false", "text", "错误"));
      // 原 correctKeys 可能是 A/B，按语义重映射
      List<String> remapped = new ArrayList<>();
      for (String k : correctKeys) {
        String n = k == null ? "" : k.trim().toLowerCase(Locale.ROOT);
        if ("a".equals(n) || "true".equals(n) || "对".equals(n) || "正确".equals(n) || "1".equals(n)) {
          remapped.add("true");
        } else if ("b".equals(n)
            || "false".equals(n)
            || "错".equals(n)
            || "错误".equals(n)
            || "0".equals(n)) {
          remapped.add("false");
        }
      }
      correctKeys.clear();
      if (!remapped.isEmpty()) {
        correctKeys.add(remapped.get(0));
      } else {
        correctKeys.add("true");
      }
    } else {
      options.clear();
      options.addAll(kept);
      correctKeys.removeIf(
          k ->
              kept.stream()
                  .noneMatch(o -> String.valueOf(o.get("key")).equalsIgnoreCase(String.valueOf(k))));
      if (correctKeys.isEmpty() && !kept.isEmpty()) {
        // 保留第一个选项为兜底（不应常态发生；isCorrect 通常在 A/对）
        correctKeys.add(String.valueOf(kept.get(0).get("key")));
      }
      return;
    }
    options.clear();
    options.addAll(kept);
  }

  private List<String> parseChoiceAnswerKeys(
      String answer, String qType, List<Map<String, Object>> options) {
    List<String> keys = new ArrayList<>();
    if (!StringUtils.hasText(answer)) {
      return keys;
    }
    String raw = answer.trim();
    if ("judge".equals(qType)) {
      String j = raw.toLowerCase(Locale.ROOT);
      if (j.contains("对") || j.equals("true") || j.equals("1") || j.equals("t") || j.equals("正确")) {
        keys.add("true");
      } else if (j.contains("错") || j.equals("false") || j.equals("0") || j.equals("f") || j.equals("错误")) {
        keys.add("false");
      } else {
        // 尝试匹配选项内容
        for (Map<String, Object> o : options) {
          if (String.valueOf(o.get("text")).equalsIgnoreCase(raw)) {
            keys.add(String.valueOf(o.get("key")));
          }
        }
      }
      return keys;
    }
    // 字母：A / A,C / AC
    String upper = raw.toUpperCase(Locale.ROOT).replace("，", ",");
    if (upper.matches("[A-Z]([,\\s]*[A-Z])*") || upper.matches("[A-Z]+")) {
      for (char c : upper.toCharArray()) {
        if (c >= 'A' && c <= 'Z') {
          String k = String.valueOf(c);
          if (!keys.contains(k)) {
            keys.add(k);
          }
        }
      }
      return keys;
    }
    // 内容精确匹配
    for (Map<String, Object> o : options) {
      if (String.valueOf(o.get("text")).trim().equalsIgnoreCase(raw)) {
        keys.add(String.valueOf(o.get("key")));
      }
    }
    return keys;
  }

  private List<Map<String, Object>> parseAnswerOptionsJson(String answerJson) {
    if (!StringUtils.hasText(answerJson)) {
      return List.of();
    }
    String t = answerJson.trim();
    if (!t.startsWith("[")) {
      return List.of();
    }
    try {
      return objectMapper.readValue(t, new TypeReference<>() {});
    } catch (Exception e) {
      return List.of();
    }
  }

  private static String optionContentText(Object content) {
    return content == null ? "" : String.valueOf(content).trim();
  }

  private static boolean isOptionCorrect(Object isCorrect) {
    if (isCorrect == null) {
      return false;
    }
    if (isCorrect instanceof Boolean b) {
      return b;
    }
    String s = String.valueOf(isCorrect).trim().toLowerCase(Locale.ROOT);
    return "true".equals(s) || "1".equals(s) || "yes".equals(s);
  }

  private static String[] splitStemAndOptions(String content) {
    if (content == null || content.isEmpty()) {
      return new String[]{""};
    }
    return content.split("\n");
  }

  private static String letterKey(int index) {
    return String.valueOf((char) ('A' + index));
  }

  private static ParsedOption parseOptionLine(String line, String defaultKey) {
    String s = line == null ? "" : line.trim();
    if (s.length() >= 2 && s.charAt(1) == '.') {
      return new ParsedOption(String.valueOf(s.charAt(0)).toUpperCase(Locale.ROOT), s.substring(2).trim());
    }
    if (s.length() >= 2 && (s.charAt(1) == '、' || s.charAt(1) == '．')) {
      return new ParsedOption(String.valueOf(s.charAt(0)).toUpperCase(Locale.ROOT), s.substring(2).trim());
    }
    return new ParsedOption(defaultKey, s);
  }

  private record ParsedOption(String key, String text) {}
}
