package com.guoyu.platform.exam.theorygen.service;

import com.guoyu.platform.exam.theorygen.dto.TheoryQuestionItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * LLM 产出试题的确定性校验器（方案 §T3，指引 §2.3 全部可编码项）。
 * 校验失败 = 该考评点块失败，不猜不硬兜底。
 */
@Component
public class TheoryQuestionValidator {

  private static final Set<String> OBJECTIVE_TYPES = Set.of("single", "multi", "judge");
  private static final Set<String> SUBJECTIVE_TYPES = Set.of("essay", "cal", "draw", "discuss", "case_analysis");
  private static final Set<Character> OPTION_LABELS = Set.of('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H');

  /** 单题校验，返回错误列表（空 = 通过） */
  public List<String> validate(TheoryQuestionItem q) {
    List<String> errors = new ArrayList<>();
    if (q.getContent() == null || q.getContent().isBlank()) {
      errors.add("题干为空");
      return errors;
    }
    if (q.getQuestionType() == null || q.getTypeCode() == null) {
      errors.add("缺少题型");
      return errors;
    }
    if (OBJECTIVE_TYPES.contains(q.getQuestionType())) {
      validateObjective(q, errors);
    } else if (SUBJECTIVE_TYPES.contains(q.getQuestionType())) {
      validateSubjective(q, errors);
    } else {
      errors.add("未知题型: " + q.getQuestionType());
    }
    if (q.getDifficulty() == null || q.getDifficulty() < 1 || q.getDifficulty() > 5) {
      errors.add("难度必须为 1~5");
    }
    return errors;
  }

  private void validateObjective(TheoryQuestionItem q, List<String> errors) {
    String answer = q.getAnswer() == null ? "" : q.getAnswer().trim();
    switch (q.getQuestionType()) {
      case "single" -> {
        List<TheoryQuestionItem.Option> options = q.getOptionList();
        if (options == null || options.size() != 4) {
          errors.add("单选题必须 4 个选项，实际 " + (options == null ? 0 : options.size()));
        } else {
          checkOptionLabels(options, 4, errors);
        }
        if (!answer.matches("^[A-D]$")) {
          errors.add("单选题答案必须为 A~D 单字母，实际: " + answer);
        }
      }
      case "multi" -> {
        List<TheoryQuestionItem.Option> options = q.getOptionList();
        if (options == null || options.size() != 5) {
          errors.add("多选题必须 5 个选项，实际 " + (options == null ? 0 : options.size()));
        } else {
          checkOptionLabels(options, 5, errors);
        }
        if (answer.matches("^[A-E]{2,5}$")) {
          Set<Character> distinct = new HashSet<>();
          for (char c : answer.toCharArray()) {
            distinct.add(c);
          }
          if (distinct.size() < 2) {
            errors.add("多选题至少 2 个正确答案");
          }
        } else {
          errors.add("多选题答案必须为 2~5 个字母组合，实际: " + answer);
        }
      }
      case "judge" -> {
        if (!answer.equals("√") && !answer.equals("×") && !answer.equalsIgnoreCase("true")
            && !answer.equalsIgnoreCase("false")) {
          errors.add("判断题答案必须为 √/×，实际: " + answer);
        }
      }
      default -> errors.add("未知客观题型: " + q.getQuestionType());
    }
    // 题干格式：客观题应含空缺句"（ ）"或为判断陈述句
    if ("single".equals(q.getQuestionType()) || "multi".equals(q.getQuestionType())) {
      String content = q.getContent();
      if (!content.contains("（ ）") && !content.contains("( )") && !content.contains("（）")) {
        errors.add("选择题题干缺少空缺句（ ）");
      }
    }
  }

  private void validateSubjective(TheoryQuestionItem q, List<String> errors) {
    if (q.getReferenceAnswer() == null || q.getReferenceAnswer().isBlank()) {
      errors.add("主观题缺少参考答案");
    }
    if (q.getScorePoints() == null || q.getScorePoints().isBlank()) {
      errors.add("主观题缺少评分要点");
    }
    if (q.getScoringStandard() == null || q.getScoringStandard().isBlank()) {
      errors.add("主观题缺少评分标准");
    }
    // 建议分值与要点分值之和
    if (q.getScore() != null) {
      double sum = sumPointScores(q.getScorePoints());
      if (sum > 0 && Math.abs(sum - q.getScore().doubleValue()) > 0.01) {
        errors.add(String.format("评分要点分值之和 %.1f ≠ 建议分值 %.1f", sum, q.getScore().doubleValue()));
      }
    }
    // 要点分值上限：5 分制题型要点 ≤2 分；10/15 分制要点 ≤3 分
    if (q.getScore() != null) {
      double cap = q.getScore().doubleValue() <= 5 ? 2.0 : 3.0;
      List<Double> pointScores = extractPointScores(q.getScorePoints());
      for (Double ps : pointScores) {
        if (ps > cap) {
          errors.add(String.format("评分要点分值 %.1f 超过上限 %.0f 分", ps, cap));
          break;
        }
      }
    }
  }

  /** 从评分要点文本提取分值（匹配 "（2分）" "(2.5分)" 等） */
  private List<Double> extractPointScores(String scorePoints) {
    List<Double> scores = new ArrayList<>();
    if (scorePoints == null) {
      return scores;
    }
    java.util.regex.Matcher m = java.util.regex.Pattern
        .compile("[（(]\\s*(\\d+(?:\\.\\d+)?)\\s*分\\s*[）)]").matcher(scorePoints);
    while (m.find()) {
      try {
        scores.add(Double.parseDouble(m.group(1)));
      } catch (NumberFormatException ignored) {
        // 非法分值忽略，由和校验兜底
      }
    }
    return scores;
  }

  private double sumPointScores(String scorePoints) {
    return extractPointScores(scorePoints).stream().mapToDouble(Double::doubleValue).sum();
  }

  private void checkOptionLabels(List<TheoryQuestionItem.Option> options, int expected,
      List<String> errors) {
    for (int i = 0; i < options.size(); i++) {
      TheoryQuestionItem.Option o = options.get(i);
      char expectedLabel = (char) ('A' + i);
      if (o.getLabel() == null || o.getLabel().isBlank()) {
        errors.add("选项 " + expectedLabel + " 缺少标号");
      } else if (Character.toUpperCase(o.getLabel().trim().charAt(0)) != expectedLabel) {
        errors.add("选项标号错位: 期望 " + expectedLabel + " 实际 " + o.getLabel());
      }
      if (o.getContent() == null || o.getContent().isBlank()) {
        errors.add("选项 " + expectedLabel + " 内容为空");
      }
    }
  }

  /** 批次内题干查重（字面完全一致） */
  public List<String> checkDuplicates(List<TheoryQuestionItem> questions) {
    List<String> errors = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (TheoryQuestionItem q : questions) {
      String key = q.getContent() == null ? "" : q.getContent().trim();
      if (!seen.add(key)) {
        errors.add("批次内题干重复: " + abbreviate(key));
      }
    }
    return errors;
  }

  private String abbreviate(String s) {
    return s.length() <= 30 ? s : s.substring(0, 29) + "…";
  }
}
