package com.guoyu.platform.courseware.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 固定槽位模板注册表(方案 §9.1/§9.3):槽位契约(类型/必填/容量)与几何同源,
 * prompt、校验器、渲染三处共用,严禁各自为政。
 */
public final class CoursewareLayout {

  private CoursewareLayout() {
  }

  /** 槽位矩形(pt,画布 960x540) */
  public record Rect(double x, double y, double w, double h) {
  }

  /** 槽位文字样式 */
  public record TextStyle(int fontSizePt, boolean bold, String colorHex, String align) {
  }

  /**
   * 槽位规格。capacity 语义:maxChars 总字数;text 用 maxChars;bullets 用 maxItems+maxCharsPerItem;
   * table 用 maxRows+maxCols+maxCharsPerItem(单元格);kpi 用 maxChars(value)/label/note 另有上限常量。
   */
  public record SlotSpec(String id, Set<String> accepts, boolean required, Rect rect, TextStyle style,
                         Integer maxChars, Integer maxItems, Integer maxCharsPerItem,
                         Integer maxRows, Integer maxCols) {
  }

  public record Layout(String id, String name, List<SlotSpec> slots) {

    public SlotSpec slot(String slotId) {
      return slots.stream().filter(s -> s.id().equals(slotId)).findFirst().orElse(null);
    }
  }

  public static final String COVER = "cover";
  public static final String TOC = "toc";
  public static final String SECTION = "section";
  public static final String CONTENT_BULLETS = "content_bullets";
  public static final String CONTENT_TABLE = "content_table";
  public static final String CONTENT_KPI = "content_kpi";
  public static final String SUMMARY = "summary";

  /** kpi 块字段级容量(方案 §9.3) */
  public static final int KPI_VALUE_MAX = 12;
  public static final int KPI_LABEL_MAX = 10;
  public static final int KPI_NOTE_MAX = 30;

  private static final String T = CoursewareModels.SlideBlock.TYPE_TEXT;
  private static final String B = CoursewareModels.SlideBlock.TYPE_BULLETS;
  private static final String TB = CoursewareModels.SlideBlock.TYPE_TABLE;
  private static final String K = CoursewareModels.SlideBlock.TYPE_KPI;

  private static SlotSpec slot(String id, Set<String> accepts, boolean required,
                               double x, double y, double w, double h,
                               int fontSize, boolean bold, String color, String align,
                               Integer maxChars, Integer maxItems, Integer maxCharsPerItem,
                               Integer maxRows, Integer maxCols) {
    return new SlotSpec(id, accepts, required, new Rect(x, y, w, h),
        new TextStyle(fontSize, bold, color, align),
        maxChars, maxItems, maxCharsPerItem, maxRows, maxCols);
  }

  private static final Map<String, Layout> REGISTRY = Map.of(
      COVER, new Layout(COVER, "封面", List.of(
          slot("title", Set.of(T), true, 60, 190, 840, 90, 40, true, CoursewareTheme.PRIMARY, "center",
              40, null, null, null, null),
          slot("subtitle", Set.of(T), false, 120, 300, 720, 50, 20, false, CoursewareTheme.SUBTITLE,
              "center", 50, null, null, null, null))),
      TOC, new Layout(TOC, "目录", List.of(
          slot("title", Set.of(T), true, 60, 40, 840, 56, 30, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("body", Set.of(B), true, 90, 130, 780, 340, 18, false, CoursewareTheme.TEXT, "left",
              null, 8, 45, null, null))),
      SECTION, new Layout(SECTION, "章节页", List.of(
          slot("title", Set.of(T), true, 60, 210, 840, 70, 34, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("text", Set.of(T), false, 60, 300, 840, 50, 16, false, CoursewareTheme.TEXT, "left",
              60, null, null, null, null))),
      CONTENT_BULLETS, new Layout(CONTENT_BULLETS, "要点页", List.of(
          slot("title", Set.of(T), true, 60, 30, 840, 52, 26, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("body", Set.of(B), true, 80, 105, 800, 355, 15, false, CoursewareTheme.TEXT, "left",
              null, 5, 45, null, null))),
      CONTENT_TABLE, new Layout(CONTENT_TABLE, "表格页", List.of(
          slot("title", Set.of(T), true, 60, 30, 840, 52, 26, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("body", Set.of(TB), true, 60, 105, 840, 355, 12, false, CoursewareTheme.TEXT, "left",
              null, null, 20, 8, 5))),
      CONTENT_KPI, new Layout(CONTENT_KPI, "数据页", List.of(
          slot("title", Set.of(T), true, 60, 30, 840, 52, 26, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("kpi_1", Set.of(K), true, 60, 150, 260, 180, 15, false, CoursewareTheme.TEXT, "center",
              null, null, null, null, null),
          slot("kpi_2", Set.of(K), false, 350, 150, 260, 180, 15, false, CoursewareTheme.TEXT, "center",
              null, null, null, null, null),
          slot("kpi_3", Set.of(K), false, 640, 150, 260, 180, 15, false, CoursewareTheme.TEXT, "center",
              null, null, null, null, null))),
      SUMMARY, new Layout(SUMMARY, "复习思考题页", List.of(
          slot("title", Set.of(T), true, 60, 30, 840, 52, 26, true, CoursewareTheme.PRIMARY, "left",
              40, null, null, null, null),
          slot("body", Set.of(B), true, 80, 105, 800, 355, 15, false, CoursewareTheme.TEXT, "left",
              null, 6, 60, null, null)))
  );

  public static Layout get(String layoutId) {
    return layoutId == null ? null : REGISTRY.get(layoutId);
  }

  public static Layout require(String layoutId) {
    Layout layout = get(layoutId);
    if (layout == null) {
      throw new IllegalArgumentException("未知版式: " + layoutId);
    }
    return layout;
  }

  /** page_role → 固定映射(方案 §9.1);content 可带 layout_hint 白名单 */
  public static String layoutForRole(String pageRole, String layoutHint) {
    if (pageRole == null) {
      return CONTENT_BULLETS;
    }
    return switch (pageRole) {
      case "cover" -> COVER;
      case "toc" -> TOC;
      case "section" -> SECTION;
      case "review", "summary" -> SUMMARY;
      default -> {
        if (CONTENT_TABLE.equals(layoutHint) || CONTENT_KPI.equals(layoutHint)) {
          yield layoutHint;
        }
        yield CONTENT_BULLETS;
      }
    };
  }

  /** 内容页可选版式(大纲确认界面下拉) */
  public static List<String> contentLayoutChoices() {
    return List.of(CONTENT_BULLETS, CONTENT_TABLE, CONTENT_KPI);
  }
}
