package com.guoyu.platform.courseware.domain;

/** 内置企业主题(方案 §9.4):一期单套,常量集中,后续多主题只需数据化。 */
public final class CoursewareTheme {

  private CoursewareTheme() {
  }

  public static final String THEME_ID = "enterprise";

  /** 主色(标题/强调) */
  public static final String PRIMARY = "1F5C99";
  /** 辅助色(浅蓝底) */
  public static final String SECONDARY = "EAF2FA";
  /** 正文色 */
  public static final String TEXT = "333333";
  /** 页脚灰 */
  public static final String FOOTER_GRAY = "999999";
  /** 封面副标 */
  public static final String SUBTITLE = "5A7A9E";

  /** 中文字体(标题/正文统一;Office 环境必装) */
  public static final String FONT_FAMILY = "微软雅黑";

  /** 画布尺寸(pt,16:9) */
  public static final double CANVAS_W_PT = 960.0;
  public static final double CANVAS_H_PT = 540.0;

  /** 页脚矩形与字号 */
  public static final double FOOTER_X = 60, FOOTER_Y = 508, FOOTER_W = 840, FOOTER_H = 24;
  public static final int FOOTER_FONT_SIZE = 9;

  /** 行高系数(溢出估算与渲染对齐) */
  public static final double LINE_HEIGHT_FACTOR = 1.5;
  /** 要点项间距(pt) */
  public static final double BULLET_GAP_PT = 6.0;
  /** 要点缩进(pt,占宽度) */
  public static final double BULLET_INDENT_PT = 18.0;
}
