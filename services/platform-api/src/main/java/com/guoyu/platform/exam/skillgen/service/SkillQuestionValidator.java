package com.guoyu.platform.exam.skillgen.service;

import com.guoyu.platform.exam.skillgen.dto.SkillQuestionItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * LLM 产出的操作技能试题确定性校验器（方案 §7.3 V1~V9）。
 * 校验失败 = 该考评点块失败，不猜不硬兜底。
 */
@Component
public class SkillQuestionValidator {

  /** 整块校验：题数精确 = expectedCount，且逐题过规则 */
  public List<String> validateAll(List<SkillQuestionItem> questions, int expectedCount) {
    List<String> errors = new ArrayList<>();
    if (questions == null || questions.size() != expectedCount) {
      errors.add("题数必须为 " + expectedCount + " 道，实际 "
          + (questions == null ? 0 : questions.size()) + " 道");
      return errors;
    }
    for (int i = 0; i < questions.size(); i++) {
      List<String> itemErrors = validate(questions.get(i));
      for (String e : itemErrors) {
        errors.add("第 " + (i + 1) + " 题：" + e);
      }
    }
    errors.addAll(checkDuplicates(questions));
    return errors;
  }

  /** 单题校验，返回错误列表（空 = 通过） */
  public List<String> validate(SkillQuestionItem q) {
    List<String> errors = new ArrayList<>();
    // V2 五要素非空
    if (blank(q.getName())) {
      errors.add("试题名称为空");
    }
    if (q.getSitePrepare() == null || q.getSitePrepare().getEquipment() == null
        || q.getSitePrepare().getEquipment().isEmpty()) {
      errors.add("考场准备缺少设备设施清单");
    } else {
      boolean anyBad = q.getSitePrepare().getEquipment().stream()
          .anyMatch(e -> e == null || blank(e.getName()));
      if (anyBad) {
        errors.add("设备设施清单存在缺名称的行");
      }
    }
    if (blank(q.getExamineePrepare())) {
      errors.add("考生准备为空");
    }
    if (blank(q.getRequirements())) {
      errors.add("具体考核要求为空");
    }
    if (blank(q.getNegatives())) {
      errors.add("否定项说明为空");
    } else if (!q.getNegatives().contains("终止其考试") || !q.getNegatives().contains("零分")) {
      // V3 否定项标准表述
      errors.add("否定项说明未按标准表述（需含「终止其考试」与「零分」）");
    }
    // V7 考核时间
    if (q.getDurationMinutes() == null || q.getDurationMinutes() < 5 || q.getDurationMinutes() > 240) {
      errors.add("考核时间必须为 5~240 分钟");
    }
    // V4~V6 评分记录表
    List<SkillQuestionItem.ScoringItem> scoring = q.getScoring();
    if (scoring == null || scoring.size() < 3 || scoring.size() > 10) {
      errors.add("评分要素行数必须为 3~10 行，实际 " + (scoring == null ? 0 : scoring.size()));
      return errors;
    }
    double sum = 0;
    for (int i = 0; i < scoring.size(); i++) {
      SkillQuestionItem.ScoringItem s = scoring.get(i);
      String tag = "第 " + (i + 1) + " 行评分要素";
      if (s == null || blank(s.getItem())) {
        errors.add(tag + "缺要素名");
        continue;
      }
      if (blank(s.getScoreDesc())) {
        errors.add(tag + "缺评分标准");
      }
      Double score = parseNumber(s.getScore());
      Double maxDeduct = parseNumber(s.getMaxDeduct());
      Double step = parseNumber(s.getStep());
      if (score == null || score <= 0) {
        errors.add(tag + "配分非法：" + s.getScore());
      } else {
        sum += score;
      }
      if (maxDeduct == null || maxDeduct <= 0) {
        errors.add(tag + "最大扣分非法：" + s.getMaxDeduct());
      } else if (score != null && maxDeduct > score) {
        errors.add(tag + "最大扣分 " + trim(maxDeduct) + " 超过配分 " + trim(score));
      }
      if (step == null || step <= 0) {
        errors.add(tag + "步长非法：" + s.getStep());
      } else if (maxDeduct != null && step > maxDeduct) {
        errors.add(tag + "步长 " + trim(step) + " 超过最大扣分 " + trim(maxDeduct));
      }
    }
    // V5 配分合计 = 100
    if (Math.abs(sum - 100.0) > 0.01) {
      errors.add(String.format("评分配分合计 %.1f ≠ 100", sum));
    }
    return errors;
  }

  /** V8 同点内试题名称不得重复 + 名称/要求组合字面查重 */
  public List<String> checkDuplicates(List<SkillQuestionItem> questions) {
    List<String> errors = new ArrayList<>();
    Set<String> nameSeen = new HashSet<>();
    Set<String> comboSeen = new HashSet<>();
    for (SkillQuestionItem q : questions) {
      String name = q.getName() == null ? "" : q.getName().trim();
      if (!name.isEmpty() && !nameSeen.add(name)) {
        errors.add("同点内试题名称重复: " + abbreviate(name));
      }
      String combo = name + "|" + (q.getRequirements() == null ? "" : q.getRequirements().trim());
      if (!comboSeen.add(combo)) {
        errors.add("试题名称与考核要求组合重复: " + abbreviate(name));
      }
    }
    return errors;
  }

  private boolean blank(String s) {
    return s == null || s.isBlank();
  }

  /** 数字字符串解析：容忍 "40"、"40分"、"40.0"；非法返回 null */
  private Double parseNumber(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim().replace("分", "").trim();
    try {
      return Double.parseDouble(t);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String trim(double v) {
    return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
  }

  private String abbreviate(String s) {
    return s.length() <= 30 ? s : s.substring(0, 29) + "…";
  }
}
