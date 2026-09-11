package com.guoyu.platform.trainingdepartment.support;

import org.springframework.util.StringUtils;

/**
 * {@code train_sign_up_people_self.level} 为 varchar：合并库同时存在数字串（{@code "0"…"4"}）
 * 与中文标签（{@code "五级/初级工"}）。实体按 String 读写，对外 Integer 口径用本工具互转。
 */
public final class SignUpLevelSupport {

  /** 与报名模板 / 扫码端一致：0=五级/初级工 … 4=一级/高级技师 */
  private static final String[] INDEX_LABELS = {
      "五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"
  };

  private SignUpLevelSupport() {}

  /** 入库：API Integer → varchar */
  public static String fromIndex(Integer index) {
    return index == null ? null : String.valueOf(index);
  }

  /**
   * 出库：varchar → Integer（供 VO / 认定导入等沿用零基下标）。
   * 纯数字直接解析；中文标签按工种等级关键字匹配；无法识别返回 null。
   */
  public static Integer toIndex(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    String t = raw.trim();
    if (t.matches("^-?\\d+$")) {
      try {
        return Integer.parseInt(t);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    // 先匹配更具体的「高级技师/一级」，避免被「技师」误伤
    if (t.contains("高级技师") || (t.contains("一级") && !t.contains("五级"))) {
      return 4;
    }
    if (t.contains("技师") || t.contains("二级")) {
      return 3;
    }
    if (t.contains("高级工") || t.contains("三级") || (t.contains("高级") && !t.contains("高级技师"))) {
      return 2;
    }
    if (t.contains("中级") || t.contains("四级")) {
      return 1;
    }
    if (t.contains("初级") || t.contains("五级")) {
      return 0;
    }
    for (int i = 0; i < INDEX_LABELS.length; i++) {
      if (INDEX_LABELS[i].equals(t)) {
        return i;
      }
    }
    return null;
  }

  /** 展示文案：已是中文则原样；数字串转标准标签 */
  public static String toLabel(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    String t = raw.trim();
    if (!t.matches("^-?\\d+$")) {
      return t;
    }
    Integer idx = toIndex(t);
    if (idx == null || idx < 0 || idx >= INDEX_LABELS.length) {
      return t;
    }
    return INDEX_LABELS[idx];
  }

  public static String toLabel(Integer index) {
    if (index == null || index < 0 || index >= INDEX_LABELS.length) {
      return "";
    }
    return INDEX_LABELS[index];
  }
}
