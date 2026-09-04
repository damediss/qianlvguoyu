package com.guoyu.platform.exam.theorygen.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 《理论知识考评要素细目表》单元格文本解析辅助。
 *
 * <p>兼容两种口径：
 * <pre>
 *   指引模板顺序：代码⏎名称⏎（X:Y:Z）
 *   样例文件顺序：名称⏎代码⏎（X:Y:Z）
 * </pre>
 * 解析策略：按行拆分后，含括号比例的行取 xyz；纯字母（A/B/…）的行取 code；
 * 其余非空行拼接为 name。两级节点 full_code 由调用方拼接。
 */
public final class BlueprintCellTextParser {

  private static final Pattern XYZ_PATTERN = Pattern.compile("[（(]\\s*(\\d+)\\s*[:：]\\s*(\\d+)\\s*[:：]\\s*(\\d+)\\s*[）)]");
  private static final Pattern LEVEL_CODE_PATTERN = Pattern.compile("^([A-Z])$");
  private static final Pattern POINT_CODE_PATTERN = Pattern.compile("^(\\d{3})$");

  private BlueprintCellTextParser() {
  }

  /** 解析结果 */
  public record CellParts(String name, String code, String xyzStat) {
  }

  public static CellParts parse(String raw) {
    if (raw == null || raw.isBlank()) {
      return new CellParts("", "", null);
    }
    String name = "";
    String code = "";
    String xyz = null;
    for (String line : raw.replace("\r\n", "\n").split("\n")) {
      String t = line.trim();
      if (t.isEmpty()) {
        continue;
      }
      Matcher m = XYZ_PATTERN.matcher(t);
      if (m.find()) {
        xyz = m.group(1) + ":" + m.group(2) + ":" + m.group(3);
        continue;
      }
      Matcher lm = LEVEL_CODE_PATTERN.matcher(t);
      if (lm.matches()) {
        code = lm.group(1);
        continue;
      }
      Matcher pm = POINT_CODE_PATTERN.matcher(t);
      if (pm.matches()) {
        code = pm.group(1);
        continue;
      }
      // 名称行：去掉行内尾部的（X:Y:Z）后保留
      t = XYZ_PATTERN.matcher(t).replaceAll("").trim();
      if (!t.isEmpty()) {
        name = name.isEmpty() ? t : name + t;
      }
    }
    return new CellParts(name, code, xyz);
  }

  /** 仅解析重要程度比例（如 "（80:7:3）" → "80:7:3"） */
  public static String parseXyzOnly(String raw) {
    if (raw == null) {
      return null;
    }
    Matcher m = XYZ_PATTERN.matcher(raw);
    return m.find() ? m.group(1) + ":" + m.group(2) + ":" + m.group(3) : null;
  }

  /** 相关知识要求多行文本 → 分号分隔单行（去空行，保留序号） */
  public static String normalizeKnowledge(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    List<String> parts = new ArrayList<>();
    for (String line : raw.replace("\r\n", "\n").split("\n")) {
      String t = line.trim();
      if (!t.isEmpty()) {
        parts.add(t);
      }
    }
    return String.join("；", parts);
  }

  /** 权重小数/百分比 → 百分比数值（0.45 → 45.00；"45%" → 45.00；"45" → 45.00） */
  public static java.math.BigDecimal parseWeight(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim().replace("%", "").replace("％", "").trim();
    try {
      java.math.BigDecimal v = new java.math.BigDecimal(t);
      if (v.compareTo(java.math.BigDecimal.ONE) < 0 && raw.contains("%")) {
        return v.setScale(2, java.math.RoundingMode.HALF_UP);
      }
      if (v.compareTo(java.math.BigDecimal.ONE) < 0) {
        // 小数口径（0.45=45%）
        return v.multiply(new java.math.BigDecimal(100)).setScale(2, java.math.RoundingMode.HALF_UP);
      }
      return v.setScale(2, java.math.RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 统计字符串中某字符出现次数（X/Y/Z 计数） */
  public static Map<String, Integer> newCounter() {
    return new LinkedHashMap<>();
  }
}
