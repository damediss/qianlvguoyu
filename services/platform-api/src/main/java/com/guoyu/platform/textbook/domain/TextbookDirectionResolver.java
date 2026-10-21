package com.guoyu.platform.textbook.domain;

import com.guoyu.platform.exam.standard.parser.model.ParsedStandard;
import com.guoyu.platform.exam.standard.parser.model.StandardLevelDoc;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 方向 → 职业功能序号解析（设计 §2.4-②）。
 *
 * <p>从等级正文首段（3.x 标题之后、工作要求表之前）抽取「X工考核职业功能 N、M、K 项」句式：
 * 铝电解工 10 方向以此映射过滤职业功能。解析不出时由创建表单人工登记 functionNos 兜底；
 * 单方向工种（directions=["默认"]）全部职业功能生效。
 *
 * <p>纯规则、无状态，可单测。
 */
public final class TextbookDirectionResolver {

  /** 「铝电解槽操作工考核职业功能 1、2、12 项」；序号分隔容忍 顿号/逗号/全角/空格混排 */
  private static final Pattern MAPPING_LINE = Pattern.compile(
      "([^\\s，,。；;、]+?)\\s*考核职业功能\\s*([0-9０-９\\s、，,]+?)\\s*项");
  /** 全角数字 → 半角 */
  private static final Pattern FULL_WIDTH_DIGIT = Pattern.compile("[０-９]");

  private TextbookDirectionResolver() {
  }

  /**
   * @return 方向 → 升序职业功能序号（保持文中出现顺序）；多方向标准解析不出返回空 Map，
   *         单方向工种返回 {"默认" → 全部序号}
   */
  public static Map<String, List<Integer>> resolve(StandardLevelDoc level,
      List<String> parsedDirections) {
    Map<String, List<Integer>> mapping = parseMapping(level.getIntroText());
    if (!mapping.isEmpty()) {
      return mapping;
    }
    boolean singleDirection = parsedDirections != null && parsedDirections.size() == 1
        && ParsedStandard.DEFAULT_DIRECTION.equals(parsedDirections.get(0));
    if (singleDirection) {
      List<Integer> all = new ArrayList<>();
      for (int i = 1; i <= level.getWorkFunctions().size(); i++) {
        all.add(i);
      }
      Map<String, List<Integer>> result = new LinkedHashMap<>();
      result.put(ParsedStandard.DEFAULT_DIRECTION, all);
      return result;
    }
    return new LinkedHashMap<>();
  }

  /** 是否解析出多方向映射（供上层决定"人工登记"兜底提示） */
  public static boolean hasDirectionMapping(StandardLevelDoc level) {
    return !parseMapping(level.getIntroText()).isEmpty();
  }

  /** 抽取「X工考核职业功能 n、m、k 项」→ {方向: [序号]}；容忍无空格/全角逗号/全角数字变体 */
  static Map<String, List<Integer>> parseMapping(String introText) {
    Map<String, List<Integer>> result = new LinkedHashMap<>();
    if (introText == null || introText.isBlank()) {
      return result;
    }
    Matcher m = MAPPING_LINE.matcher(introText);
    while (m.find()) {
      String direction = m.group(1).trim();
      String numbers = FULL_WIDTH_DIGIT.matcher(m.group(2))
          .replaceAll(r -> String.valueOf(r.group().charAt(0) - '０'));
      List<Integer> nos = new ArrayList<>();
      Matcher dm = Pattern.compile("\\d+").matcher(numbers);
      while (dm.find()) {
        int no = Integer.parseInt(dm.group());
        if (!nos.contains(no)) {
          nos.add(no);
        }
      }
      if (!direction.isEmpty() && !nos.isEmpty()) {
        result.put(direction, List.copyOf(nos));
      }
    }
    return result;
  }
}
