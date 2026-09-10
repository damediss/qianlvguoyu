package com.guoyu.platform.exam.skillgen.parser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 《操作技能考评要素细目表》单元格文本解析辅助。
 *
 * <p>一级/二级代码列写法：{@code 代码⏎（X:Y:Z）}（如 "A⏎（6:0:0）"），
 * 也兼容同行写法与倒序；名称/权重/选考方式按列直读，不拆格。
 */
public final class SkillCellTextParser {

  private static final Pattern XYZ_PATTERN =
      Pattern.compile("[（(]\\s*(\\d+)\\s*[:：]\\s*(\\d+)\\s*[:：]\\s*(\\d+)\\s*[）)]");
  private static final Pattern CODE_PATTERN = Pattern.compile("^([A-Z]{1,2})$");
  private static final Pattern POINT_CODE_PATTERN = Pattern.compile("^(\\d{1,4})$");
  /** 两个中文之间多余的空格（数据源常见 quirk："能对 生产 系统 开启" → "能对生产系统开启"） */
  private static final Pattern CJK_INNER_SPACE =
      Pattern.compile("(?<=[\\u4e00-\\u9fa5])\\s+(?=[\\u4e00-\\u9fa5])");

  private SkillCellTextParser() {
  }

  /** 代码列解析结果 */
  public record CodeParts(String code, String xyzStat) {
  }

  /** 解析 "A⏎（6:0:0）" 式单元格 → 代码 + 重要程度比例 */
  public static CodeParts parseCodeXyz(String raw) {
    if (raw == null || raw.isBlank()) {
      return new CodeParts("", null);
    }
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
      }
      String noXyz = XYZ_PATTERN.matcher(t).replaceAll("").trim();
      if (code.isEmpty()) {
        Matcher cm = CODE_PATTERN.matcher(noXyz);
        if (cm.matches()) {
          code = cm.group(1);
        }
      }
    }
    return new CodeParts(code, xyz);
  }

  /** 仅解析重要程度比例（"（6:0:0）" → "6:0:0"） */
  public static String parseXyzOnly(String raw) {
    if (raw == null) {
      return null;
    }
    Matcher m = XYZ_PATTERN.matcher(raw);
    return m.find() ? m.group(1) + ":" + m.group(2) + ":" + m.group(3) : null;
  }

  /** 考评点代码（001 式） */
  public static String parsePointCode(String raw) {
    if (raw == null) {
      return "";
    }
    String t = raw.trim();
    Matcher m = POINT_CODE_PATTERN.matcher(t);
    if (m.matches()) {
      return t.length() < 3 ? String.format("%03d", Integer.parseInt(t)) : t;
    }
    return t;
  }

  /** 试题量列：空 → null；整数 → 值；非数字/负数 → 抛 IllegalArgumentException 由调用方记警告 */
  public static Integer parseQuota(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim();
    if (t.endsWith(".0")) {
      t = t.substring(0, t.length() - 2);
    }
    try {
      int v = Integer.parseInt(t);
      if (v <= 0) {
        throw new IllegalArgumentException("试题量必须为正整数，实际: " + raw);
      }
      return v;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("试题量不是整数: " + raw);
    }
  }

  /** 权重小数/百分比 → 百分比数值（0.45 → 45.00；"45%" → 45.00；"45" → 45.00） */
  public static BigDecimal parseWeight(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim().replace("%", "").replace("％", "").trim();
    try {
      BigDecimal v = new BigDecimal(t);
      if (v.compareTo(BigDecimal.ONE) < 0 && raw.contains("%")) {
        return v.setScale(2, RoundingMode.HALF_UP);
      }
      if (v.compareTo(BigDecimal.ONE) < 0) {
        // 小数口径（0.45=45%）
        return v.multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP);
      }
      return v.setScale(2, RoundingMode.HALF_UP);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 多行文本规范化：逐行 trim + 折叠行内空白 + 去掉中文间多余空格；保留换行结构 */
  public static String normalizeMultiline(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (String line : raw.replace("\r\n", "\n").split("\n")) {
      String t = line.trim().replaceAll("[ \t]+", " ");
      t = CJK_INNER_SPACE.matcher(t).replaceAll("");
      if (t.isEmpty()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(t);
    }
    return sb.toString();
  }

  /** 重要程度：取首字母并校验 ∈ {X,Y,Z}；非法返回 null */
  public static String parseImportance(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    String t = raw.trim().toUpperCase();
    char c = t.charAt(0);
    return (c == 'X' || c == 'Y' || c == 'Z') ? String.valueOf(c) : null;
  }
}
