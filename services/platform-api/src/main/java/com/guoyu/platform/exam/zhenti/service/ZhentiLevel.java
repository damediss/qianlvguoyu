package com.guoyu.platform.exam.zhenti.service;

/**
 * 等级码常量与解析（方案 v2.1 §3.2，Q2 已拍板沿用库内 0~4）。
 *
 * <p>train_exam_work_type_list.level 库为 tinyint、实体为 String，解析必须容错；
 * 与 reviewsync 的 parseLevel 同口径：数字串/数值均可，空与未知返回 null。
 */
public final class ZhentiLevel {

  public static final int LEVEL_5 = 0;
  public static final int LEVEL_4 = 1;
  public static final int LEVEL_3 = 2;
  public static final int LEVEL_2 = 3;
  public static final int LEVEL_1 = 4;

  private static final String[] LABELS = {
      "五级/初级工", "四级/中级工", "三级/高级工", "二级/技师", "一级/高级技师"
  };

  private ZhentiLevel() {
  }

  /** 展示标签；level 为空或越界返回 null。 */
  public static String labelOf(Integer level) {
    if (level == null || level < LEVEL_5 || level > LEVEL_1) {
      return null;
    }
    return LABELS[level];
  }

  /**
   * 容错解析：Integer / Number / 数字串 → int；null、空白、非数字 → null。
   * train_exam_work_type_list.level 是 tinyint，JDBC 读出来通常是 Integer；
   * 实体映射为 String 时也会走到 String 分支。
   *
   * <p>Boolean 兜底（踩坑 #23）：测试库该列是 tinyint(1)，mysql-connector tinyInt1isBit
   * 默认把 JdbcTemplate 直读映射成 Boolean；MyBatis String 实体也可能拿到 "true"/"false"。
   * 生产库该列是无宽度 tinyint 不受影响。布尔只能承载 0/1，2~4 在 tinyint(1) 库上
   * 驱动层即丢失，属历史列类型分叉，不在代码层强修。
   */
  public static Integer parseLevel(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Boolean bool) {
      return bool ? 1 : 0;
    }
    if (raw instanceof Number number) {
      return number.intValue();
    }
    String text = String.valueOf(raw).trim();
    if (text.isEmpty()) {
      return null;
    }
    if ("true".equalsIgnoreCase(text)) {
      return 1;
    }
    if ("false".equalsIgnoreCase(text)) {
      return 0;
    }
    try {
      return Integer.valueOf(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
